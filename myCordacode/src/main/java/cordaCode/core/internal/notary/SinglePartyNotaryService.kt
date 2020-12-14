package cordaCode.core.internal.notary

import co.paralleluniverse.fibers.Suspendable
import cordaCode.core.contracts.StateRef
import cordaCode.core.contracts.TimeWindow
import cordaCode.core.crypto.Crypto
import cordaCode.core.crypto.SecureHash
import cordaCode.core.crypto.SignableData
import cordaCode.core.crypto.SignatureMetadata
import cordaCode.core.crypto.TransactionSignature
import cordaCode.core.flows.FlowExternalAsyncOperation
import cordaCode.core.flows.FlowLogic
import cordaCode.core.flows.NotarisationRequestSignature
import cordaCode.core.identity.Party
import cordaCode.core.internal.notary.UniquenessProvider.Result
import cordaCode.core.serialization.CordaSerializable
import cordaCode.core.utilities.contextLogger
import org.slf4j.Logger
import java.time.Duration
import java.util.concurrent.CompletableFuture

/** Base implementation for a notary service operated by a singe party. */
abstract class SinglePartyNotaryService : NotaryService() {
    companion object {
        private val staticLog = contextLogger()
    }

    protected open val log: Logger get() = staticLog

    /** Handles input state uniqueness checks. */
    protected abstract val uniquenessProvider: UniquenessProvider

    /** Attempts to commit the specified transaction [txId]. */
    @Suspendable
    open fun commitInputStates(
            inputs: List<StateRef>,
            txId: SecureHash,
            caller: Party,
            requestSignature: NotarisationRequestSignature,
            timeWindow: TimeWindow?,
            references: List<StateRef>
    ): Result {
        // TODO: Log the request here. Benchmarking shows that logging is expensive and we might get better performance
        // when we concurrently log requests here as part of the flows, instead of logging sequentially in the
        // `UniquenessProvider`.

        val callingFlow = FlowLogic.currentTopLevel
                ?: throw IllegalStateException("This method should be invoked in a flow context.")

        val result = callingFlow.await(
                CommitOperation(
                        this,
                        inputs,
                        txId,
                        caller,
                        requestSignature,
                        timeWindow,
                        references
                )
        )

        if (result is Result.Failure) {
            throw NotaryInternalException(result.error)
        }

        return result
    }

    /**
     * Estimate the wait time to be notarised taking into account the new request size.
     *
     * @param numStates The number of states we're about to request be notarised.
     */
    fun getEstimatedWaitTime(numStates: Int): Duration = uniquenessProvider.getEta(numStates)

    /**
     * Required for the flow to be able to suspend until the commit is complete.
     * This object will be included in the flow checkpoint.
     */
    @CordaSerializable
    class CommitOperation(
            val service: SinglePartyNotaryService,
            val inputs: List<StateRef>,
            val txId: SecureHash,
            val caller: Party,
            val requestSignature: NotarisationRequestSignature,
            val timeWindow: TimeWindow?,
            val references: List<StateRef>
    ) : FlowExternalAsyncOperation<Result> {

        override fun execute(deduplicationId: String): CompletableFuture<Result> {
            return service.uniquenessProvider.commit(inputs, txId, caller, requestSignature, timeWindow, references).toCompletableFuture()
        }
    }

    /** Sign a single transaction. */
    fun signTransaction(txId: SecureHash): TransactionSignature {
        val signableData = SignableData(txId, SignatureMetadata(services.myInfo.platformVersion, Crypto.findSignatureScheme(notaryIdentityKey).schemeNumberID))
        return services.keyManagementService.sign(signableData, notaryIdentityKey)
    }

}
