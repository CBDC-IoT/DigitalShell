package cordaCode.core.crypto

import cordaCode.core.KeepForDJVM
import cordaCode.core.serialization.CordaSerializable

/**
 * Custom class for holding signature data. This exists for later extension work to provide a standardised cross-platform
 * serialization format.
 */
@CordaSerializable
@KeepForDJVM
data class CompositeSignaturesWithKeys(val sigs: List<TransactionSignature>) {
    companion object {
        val EMPTY = CompositeSignaturesWithKeys(emptyList())
    }
}
