package cordaCode.core.internal

import co.paralleluniverse.fibers.Suspendable
import cordaCode.core.internal.AttachmentTrustCalculator
import cordaCode.core.DeleteForDJVM
import cordaCode.core.internal.notary.NotaryService
import cordaCode.core.node.ServiceHub
import cordaCode.core.node.StatesToRecord
import java.util.concurrent.ExecutorService

// TODO: This should really be called ServiceHubInternal but that name is already taken by cordaCode.node.services.api.ServiceHubInternal.
@DeleteForDJVM
interface ServiceHubCoreInternal : ServiceHub {

    val externalOperationExecutor: ExecutorService

    val attachmentTrustCalculator: AttachmentTrustCalculator

    /**
     * Optional `NotaryService` which will be `null` for all non-Notary nodes.
     */
    val notaryService: NotaryService?

    fun createTransactionsResolver(flow: ResolveTransactionsFlow): TransactionsResolver
}

interface TransactionsResolver {
    @Suspendable
    fun downloadDependencies(batchMode: Boolean)

    fun recordDependencies(usedStatesToRecord: StatesToRecord)
}