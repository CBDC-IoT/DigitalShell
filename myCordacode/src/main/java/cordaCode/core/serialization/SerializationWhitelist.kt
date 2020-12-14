package cordaCode.core.serialization

import cordaCode.core.KeepForDJVM

/**
 * Provide a subclass of this via the [java.util.ServiceLoader] mechanism to be able to whitelist types for
 * serialisation that you cannot otherwise annotate. The name of the class must appear in a text file on the
 * classpath under the path META-INF/services/cordaCode.core.serialization.SerializationWhitelist
 */
@KeepForDJVM
interface SerializationWhitelist {
    /**
     * Optionally whitelist types for use in object serialization, as we lock down the types that can be serialized.
     *
     * For example, if you add a new [cordaCode.core.contracts.ContractState] it needs to be whitelisted.  You can do that
     * either by adding the [cordaCode.core.serialization.CordaSerializable] annotation or via this method.
     */
    val whitelist: List<Class<*>>
}