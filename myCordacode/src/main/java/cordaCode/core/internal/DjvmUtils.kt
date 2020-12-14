@file:KeepForDJVM

package cordaCode.core.internal

import cordaCode.core.KeepForDJVM
import cordaCode.core.contracts.Attachment
import cordaCode.core.contracts.StateRef
import cordaCode.core.contracts.TransactionState
import cordaCode.core.crypto.SecureHash
import cordaCode.core.node.NetworkParameters
import cordaCode.core.transactions.LedgerTransaction
import cordaCode.core.transactions.WireTransaction

fun WireTransaction.toLtxDjvmInternal(
        resolveAttachment: (SecureHash) -> Attachment?,
        resolveStateRef: (StateRef) -> TransactionState<*>?,
        resolveParameters: (SecureHash?) -> NetworkParameters?
): LedgerTransaction {
    return toLtxDjvmInternalBridge(resolveAttachment, resolveStateRef, resolveParameters)
}
