package cordaCode.core.serialization

import cordaCode.core.CordaException
import cordaCode.core.KeepForDJVM
import cordaCode.core.crypto.SecureHash

/** Thrown during deserialization to indicate that an attachment needed to construct the [WireTransaction] is not found. */
@KeepForDJVM
@CordaSerializable
class MissingAttachmentsException(val ids: List<SecureHash>, message: String?) : CordaException(message) {

    constructor(ids: List<SecureHash>) : this(ids, null)
}