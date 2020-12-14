package cordaCode.core.flows

import cordaCode.core.flows.AbstractStateReplacementFlow
import cordaCode.core.flows.InitiatingFlow
import cordaCode.core.contracts.ContractState
import cordaCode.core.contracts.StateAndRef
import cordaCode.core.contracts.StateRef
import cordaCode.core.crypto.Crypto
import cordaCode.core.crypto.SignableData
import cordaCode.core.crypto.SignatureMetadata
import cordaCode.core.identity.Party
import cordaCode.core.internal.NotaryChangeTransactionBuilder
import cordaCode.core.transactions.SignedTransaction
import cordaCode.core.utilities.ProgressTracker

/**
 * A flow to be used for changing a state's Notary. This is required since all input states to a transaction
 * must point to the same notary.
 *
 * This assembles the transaction for notary replacement and sends out change proposals to all participants
 * of that state. If participants agree to the proposed change, they each sign the transaction.
 * Finally, the transaction containing all signatures is sent back to each participant so they can record it and
 * use the new updated state for future transactions.
 */
@InitiatingFlow
class NotaryChangeFlow<out T : ContractState>(
        originalState: StateAndRef<T>,
        newNotary: Party,
        progressTracker: ProgressTracker = AbstractStateReplacementFlow.Instigator.tracker())
    : AbstractStateReplacementFlow.Instigator<T, T, Party>(originalState, newNotary, progressTracker) {

    override fun assembleTx(): AbstractStateReplacementFlow.UpgradeTx {
        val inputs = resolveEncumbrances(originalState)

        val tx = NotaryChangeTransactionBuilder(
                inputs.map { it.ref },
                originalState.state.notary,
                modification,
                serviceHub.networkParametersService.currentHash
        ).build()

        val participantKeys = inputs.flatMap { it.state.data.participants }.map { it.owningKey }.toSet()
        // TODO: We need a much faster way of finding our key in the transaction
        val myKey = serviceHub.keyManagementService.filterMyKeys(participantKeys).single()
        val signableData = SignableData(tx.id, SignatureMetadata(serviceHub.myInfo.platformVersion, Crypto.findSignatureScheme(myKey).schemeNumberID))
        val mySignature = serviceHub.keyManagementService.sign(signableData, myKey)
        val stx = SignedTransaction(tx, listOf(mySignature))

        return AbstractStateReplacementFlow.UpgradeTx(stx)
    }

    /** Resolves the encumbrance state chain for the given [state]. */
    private fun resolveEncumbrances(state: StateAndRef<T>): List<StateAndRef<T>> {
        val states = mutableSetOf(state)
        while (states.last().state.encumbrance != null) {
            val encumbranceStateRef = StateRef(states.last().ref.txhash, states.last().state.encumbrance!!)
            val encumbranceState = serviceHub.toStateAndRef<T>(encumbranceStateRef)
            if (!states.add(encumbranceState)) break // Stop if there is a cycle.
        }
        return states.toList()
    }
}
