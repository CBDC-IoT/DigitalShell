package cordaCode.core.node.services.vault

import cordaCode.core.DoNotImplement

@DoNotImplement
interface CordaTransactionSupport {
    /**
     * Executes given statement in the scope of transaction with default transaction isolation level.
     * @param statement to be executed in the scope of this transaction.
     */
    fun <T> transaction(statement: SessionScope.() -> T): T
}