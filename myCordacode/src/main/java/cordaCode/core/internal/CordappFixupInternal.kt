package cordaCode.core.internal

import cordaCode.core.DeleteForDJVM
import cordaCode.core.node.services.AttachmentId

@DeleteForDJVM
interface CordappFixupInternal {
    fun fixupAttachmentIds(attachmentIds: Collection<AttachmentId>): Set<AttachmentId>
}
