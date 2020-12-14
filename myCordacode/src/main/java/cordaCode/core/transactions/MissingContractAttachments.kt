package cordaCode.core.transactions

import cordaCode.core.KeepForDJVM
import cordaCode.core.contracts.ContractState
import cordaCode.core.contracts.TransactionState
import cordaCode.core.flows.FlowException
import cordaCode.core.internal.Version
import cordaCode.core.serialization.CordaSerializable

/**
 * A contract attachment was missing when trying to automatically attach all known contract attachments
 *
 * @property states States which have contracts that do not have corresponding attachments in the attachment store.
 */
@CordaSerializable
@KeepForDJVM
class MissingContractAttachments
@JvmOverloads
constructor(val states: List<TransactionState<ContractState>>, contractsClassName: String? = null, minimumRequiredContractClassVersion: Version? = null) : FlowException(
        "Cannot find contract attachments for " +
        "${contractsClassName ?: states.map { it.contract }.distinct()}${minimumRequiredContractClassVersion?.let { ", minimum required contract class version $minimumRequiredContractClassVersion"}}.")
