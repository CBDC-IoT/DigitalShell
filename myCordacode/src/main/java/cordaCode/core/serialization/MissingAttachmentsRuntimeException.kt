package cordaCode.core.serialization

import cordaCode.core.CordaRuntimeException
import cordaCode.core.KeepForDJVM
import cordaCode.core.node.services.AttachmentId

@KeepForDJVM
@CordaSerializable
class MissingAttachmentsRuntimeException(val ids: List<AttachmentId>, message: String?, cause: Throwable?)
    : CordaRuntimeException(message, cause) {

    @Suppress("unused")
    constructor(ids: List<AttachmentId>, message: String?) : this(ids, message, null)

    @Suppress("unused")
    constructor(ids: List<AttachmentId>) : this(ids, null, null)
}
