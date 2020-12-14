package cordaCode.core.internal.cordapp

import cordaCode.core.DeleteForDJVM
import cordaCode.core.cordapp.Cordapp
import cordaCode.core.crypto.SecureHash
import cordaCode.core.flows.FlowLogic
import cordaCode.core.internal.PLATFORM_VERSION
import cordaCode.core.internal.VisibleForTesting
import cordaCode.core.internal.notary.NotaryService
import cordaCode.core.internal.toPath
import cordaCode.core.schemas.MappedSchema
import cordaCode.core.serialization.SerializationCustomSerializer
import cordaCode.core.serialization.SerializationWhitelist
import cordaCode.core.serialization.SerializeAsToken
import java.net.URL
import java.nio.file.Paths

@DeleteForDJVM
data class CordappImpl(
        override val contractClassNames: List<String>,
        override val initiatedFlows: List<Class<out FlowLogic<*>>>,
        override val rpcFlows: List<Class<out FlowLogic<*>>>,
        override val serviceFlows: List<Class<out FlowLogic<*>>>,
        override val schedulableFlows: List<Class<out FlowLogic<*>>>,
        override val services: List<Class<out SerializeAsToken>>,
        override val serializationWhitelists: List<SerializationWhitelist>,
        override val serializationCustomSerializers: List<SerializationCustomSerializer<*, *>>,
        override val customSchemas: Set<MappedSchema>,
        override val allFlows: List<Class<out FlowLogic<*>>>,
        override val jarPath: URL,
        override val info: Cordapp.Info,
        override val jarHash: SecureHash.SHA256,
        override val minimumPlatformVersion: Int,
        override val targetPlatformVersion: Int,
        val notaryService: Class<out NotaryService>? = null,
        /** Indicates whether the CorDapp is loaded from external sources, or generated on node startup (virtual). */
        val isLoaded: Boolean = true,
        private val explicitCordappClasses: List<String> = emptyList(),
        val isVirtual: Boolean = false
) : Cordapp {
    override val name: String = jarName(jarPath)

    // TODO: Also add [SchedulableFlow] as a Cordapp class
    override val cordappClasses: List<String> = run {
        val classList = rpcFlows + initiatedFlows + services + serializationWhitelists.flatMap { it.whitelist } + notaryService
        classList.mapNotNull { it?.name } + contractClassNames + explicitCordappClasses
    }

    companion object {
        fun jarName(url: URL): String = url.toPath().fileName.toString().removeSuffix(".jar")

        /** CorDapp manifest entries */
        const val CORDAPP_CONTRACT_NAME = "Cordapp-Contract-Name"
        const val CORDAPP_CONTRACT_VERSION = "Cordapp-Contract-Version"
        const val CORDAPP_CONTRACT_VENDOR = "Cordapp-Contract-Vendor"
        const val CORDAPP_CONTRACT_LICENCE = "Cordapp-Contract-Licence"

        const val CORDAPP_WORKFLOW_NAME = "Cordapp-Workflow-Name"
        const val CORDAPP_WORKFLOW_VERSION = "Cordapp-Workflow-Version"
        const val CORDAPP_WORKFLOW_VENDOR = "Cordapp-Workflow-Vendor"
        const val CORDAPP_WORKFLOW_LICENCE = "Cordapp-Workflow-Licence"

        const val TARGET_PLATFORM_VERSION = "Target-Platform-Version"
        const val MIN_PLATFORM_VERSION = "Min-Platform-Version"

        const val UNKNOWN_VALUE = "Unknown"
        const val DEFAULT_CORDAPP_VERSION = 1

        /** used for CorDapps that do not explicitly define attributes */
        val UNKNOWN_INFO = Cordapp.Info.Default(UNKNOWN_VALUE, UNKNOWN_VALUE, UNKNOWN_VALUE, UNKNOWN_VALUE)

        @VisibleForTesting
        val TEST_INSTANCE = CordappImpl(
                contractClassNames = emptyList(),
                initiatedFlows = emptyList(),
                rpcFlows = emptyList(),
                serviceFlows = emptyList(),
                schedulableFlows = emptyList(),
                services = emptyList(),
                serializationWhitelists = emptyList(),
                serializationCustomSerializers = emptyList(),
                customSchemas = emptySet(),
                jarPath = Paths.get("").toUri().toURL(),
                info = CordappImpl.UNKNOWN_INFO,
                allFlows = emptyList(),
                jarHash = SecureHash.allOnesHash,
                minimumPlatformVersion = 1,
                targetPlatformVersion = PLATFORM_VERSION,
                notaryService = null
        )
    }
}
