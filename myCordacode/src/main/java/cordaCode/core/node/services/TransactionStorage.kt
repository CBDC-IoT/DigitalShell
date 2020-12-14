package cordaCode.core.node.services

import cordaCode.core.DeleteForDJVM
import cordaCode.core.DoNotImplement
import cordaCode.core.concurrent.CordaFuture
import cordaCode.core.crypto.SecureHash
import cordaCode.core.messaging.DataFeed
import cordaCode.core.transactions.SignedTransaction
import rx.Observable

/**
 * Thread-safe storage of transactions.
 */
@DeleteForDJVM
@DoNotImplement
interface TransactionStorage {
    /**
     * Return the transaction with the given [id], or null if no such transaction exists.
     */
    fun getTransaction(id: SecureHash): SignedTransaction?

    /**
     * Get a synchronous Observable of updates.  When observations are pushed to the Observer, the vault will already
     * incorporate the update.
     */
    val updates: Observable<SignedTransaction>

    /**
     * Returns all currently stored transactions and further fresh ones.
     */
    fun track(): DataFeed<List<SignedTransaction>, SignedTransaction>

    /**
     * Returns a future that completes with the transaction corresponding to [id] once it has been committed
     */
    fun trackTransaction(id: SecureHash): CordaFuture<SignedTransaction>
}