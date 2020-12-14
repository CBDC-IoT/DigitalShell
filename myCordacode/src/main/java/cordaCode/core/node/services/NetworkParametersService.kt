package cordaCode.core.node.services

import cordaCode.core.DoNotImplement
import cordaCode.core.crypto.SecureHash
import cordaCode.core.node.NetworkParameters

/**
 * Service for retrieving network parameters used for resolving transactions according to parameters that were
 * historically in force in the network.
 */
@DoNotImplement
interface NetworkParametersService {
    /**
     * Hash of the current parameters for the network.
     */
    val currentHash: SecureHash

    /**
     * For backwards compatibility, this parameters hash will be used for resolving historical transactions in the chain.
     */
    val defaultHash: SecureHash

    /**
     * Return the network parameters with the given hash, or null if it doesn't exist.
     */
    fun lookup(hash: SecureHash): NetworkParameters?
}
