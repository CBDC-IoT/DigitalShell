package cordaCode.core.identity

import cordaCode.core.KeepForDJVM
import cordaCode.core.contracts.PartyAndReference
import cordaCode.core.crypto.toStringShort
import cordaCode.core.flows.Destination
import cordaCode.core.flows.FlowLogic
import cordaCode.core.utilities.OpaqueBytes
import java.security.PublicKey

/**
 * The [AnonymousParty] class contains enough information to uniquely identify a [Party] while excluding private
 * information such as name. It is intended to represent a party on the distributed ledger.
 *
 * ### Flow sessions
 *
 * Anonymous parties can be used to communicate using the [FlowLogic.initiateFlow] method. Message routing is simply routing to the well-known
 * [Party] the anonymous party belongs to. This mechanism assumes the party initiating the communication knows who the anonymous party is.
 */
@KeepForDJVM
class AnonymousParty(owningKey: PublicKey) : Destination, AbstractParty(owningKey) {
    override fun nameOrNull(): CordaX500Name? = null
    override fun ref(bytes: OpaqueBytes): PartyAndReference = PartyAndReference(this, bytes)
    override fun toString() = "Anonymous(${owningKey.toStringShort()})"
}