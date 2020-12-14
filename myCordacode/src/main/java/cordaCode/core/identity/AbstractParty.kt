package cordaCode.core.identity

import cordaCode.core.DoNotImplement
import cordaCode.core.contracts.PartyAndReference
import cordaCode.core.flows.Destination
import cordaCode.core.serialization.CordaSerializable
import cordaCode.core.utilities.OpaqueBytes
import java.security.PublicKey

/**
 * An [AbstractParty] contains the common elements of [Party] and [AnonymousParty], specifically the owning key of
 * the party. In most cases [Party] or [AnonymousParty] should be used, depending on use-case.
 */
@CordaSerializable
@DoNotImplement
abstract class AbstractParty(val owningKey: PublicKey): Destination {
    /** Anonymised parties do not include any detail apart from owning key, so equality is dependent solely on the key */
    override fun equals(other: Any?): Boolean = other === this || other is AbstractParty && other.owningKey == owningKey

    override fun hashCode(): Int = owningKey.hashCode()
    abstract fun nameOrNull(): CordaX500Name?

    /**
     * Build a reference to something being stored or issued by a party e.g. in a vault or (more likely) on their normal
     * ledger.
     */
    abstract fun ref(bytes: OpaqueBytes): PartyAndReference

    /**
     * Build a reference to something being stored or issued by a party e.g. in a vault or (more likely) on their normal
     * ledger.
     */
    fun ref(vararg bytes: Byte) = ref(OpaqueBytes.of(*bytes))
}