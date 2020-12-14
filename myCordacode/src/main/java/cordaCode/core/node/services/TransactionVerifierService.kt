package cordaCode.core.node.services

import cordaCode.core.DeleteForDJVM
import cordaCode.core.DoNotImplement
import cordaCode.core.concurrent.CordaFuture
import cordaCode.core.transactions.LedgerTransaction

/**
 * Provides verification service. The implementation may be a simple in-memory verify() call or perhaps an IPC/RPC.
 * @suppress
 */
@DoNotImplement
@DeleteForDJVM
interface TransactionVerifierService {
    /**
     * @param transaction The transaction to be verified.
     * @return A future that completes successfully if the transaction verified, or sets an exception the verifier threw.
     */
    fun verify(transaction: LedgerTransaction): CordaFuture<*>
}