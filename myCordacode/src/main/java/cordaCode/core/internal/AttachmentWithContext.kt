package cordaCode.core.internal

import cordaCode.core.contracts.Attachment
import cordaCode.core.contracts.ContractAttachment
import cordaCode.core.contracts.ContractClassName
import cordaCode.core.node.services.AttachmentId

/**
 * Used only for passing to the Attachment constraint verification.
 */
class AttachmentWithContext(
        val contractAttachment: ContractAttachment,
        val contract: ContractClassName,
        /** Required for verifying [WhitelistedByZoneAttachmentConstraint] */
        val whitelistedContractImplementations: Map<String, List<AttachmentId>>
) : Attachment by contractAttachment {
    init {
        require(contract in contractAttachment.allContracts) {
            "This AttachmentWithContext was not initialised properly. Please ensure all Corda contracts extending existing Corda contracts also implement the Contract base class."
        }
    }
}