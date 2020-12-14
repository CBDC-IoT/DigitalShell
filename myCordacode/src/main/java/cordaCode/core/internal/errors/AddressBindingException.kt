package cordaCode.core.internal.errors

import cordaCode.core.CordaRuntimeException
import cordaCode.core.utilities.NetworkHostAndPort

class AddressBindingException(val addresses: Set<NetworkHostAndPort>) : CordaRuntimeException(message(addresses)) {

    constructor(address: NetworkHostAndPort) : this(setOf(address))

    private companion object {
        private fun message(addresses: Set<NetworkHostAndPort>): String {
            require(addresses.isNotEmpty()) { "Address list must not be empty" }
            return if (addresses.size > 1) {
                "Failed to bind on an address in ${addresses.joinToString(", ", "[", "]")}."
            } else {
                "Failed to bind on address ${addresses.single()}."
            }
        }
    }
}