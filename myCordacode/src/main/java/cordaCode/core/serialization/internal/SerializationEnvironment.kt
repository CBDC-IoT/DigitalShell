@file:KeepForDJVM
package cordaCode.core.serialization.internal

import cordaCode.core.KeepForDJVM
import cordaCode.core.internal.InheritableThreadLocalToggleField
import cordaCode.core.internal.SimpleToggleField
import cordaCode.core.internal.ThreadLocalToggleField
import cordaCode.core.serialization.SerializationContext
import cordaCode.core.serialization.SerializationFactory

@KeepForDJVM
interface SerializationEnvironment {

    companion object {
        fun with(
                serializationFactory: SerializationFactory,
                p2pContext: SerializationContext,
                rpcServerContext: SerializationContext? = null,
                rpcClientContext: SerializationContext? = null,
                storageContext: SerializationContext? = null,

                checkpointContext: CheckpointSerializationContext? = null,
                checkpointSerializer: CheckpointSerializer? = null
        ): SerializationEnvironment =
                SerializationEnvironmentImpl(
                        serializationFactory = serializationFactory,
                        p2pContext = p2pContext,
                        optionalRpcServerContext = rpcServerContext,
                        optionalRpcClientContext = rpcClientContext,
                        optionalStorageContext = storageContext,
                        optionalCheckpointContext = checkpointContext,
                        optionalCheckpointSerializer = checkpointSerializer
                )
    }

    val serializationFactory: SerializationFactory
    val p2pContext: SerializationContext
    val rpcServerContext: SerializationContext
    val rpcClientContext: SerializationContext
    val storageContext: SerializationContext

    val checkpointSerializer: CheckpointSerializer
    val checkpointContext: CheckpointSerializationContext
}

@KeepForDJVM
private class SerializationEnvironmentImpl(
        override val serializationFactory: SerializationFactory,
        override val p2pContext: SerializationContext,
        private val optionalRpcServerContext: SerializationContext? = null,
        private val optionalRpcClientContext: SerializationContext? = null,
        private val optionalStorageContext: SerializationContext? = null,
        private val optionalCheckpointContext: CheckpointSerializationContext? = null,
        private val optionalCheckpointSerializer: CheckpointSerializer? = null) : SerializationEnvironment {

    override val rpcServerContext: SerializationContext get() = optionalRpcServerContext ?:
            throw UnsupportedOperationException("RPC server serialization not supported in this environment")

    override val rpcClientContext: SerializationContext get() = optionalRpcClientContext ?:
        throw UnsupportedOperationException("RPC client serialization not supported in this environment")

    override val storageContext: SerializationContext get() = optionalStorageContext ?:
        throw UnsupportedOperationException("Storage serialization not supported in this environment")

    override val checkpointContext: CheckpointSerializationContext get() = optionalCheckpointContext ?:
        throw UnsupportedOperationException("Checkpoint serialization not supported in this environment")

    override val checkpointSerializer: CheckpointSerializer get() = optionalCheckpointSerializer ?:
        throw UnsupportedOperationException("Checkpoint serialization not supported in this environment")
}

private val _nodeSerializationEnv = SimpleToggleField<SerializationEnvironment>("nodeSerializationEnv", true)
/** Should be set once in main. */
var nodeSerializationEnv by _nodeSerializationEnv

val _driverSerializationEnv = SimpleToggleField<SerializationEnvironment>("driverSerializationEnv")

val _rpcClientSerializationEnv = SimpleToggleField<SerializationEnvironment>("rpcClientSerializationEnv")

val _contextSerializationEnv = ThreadLocalToggleField<SerializationEnvironment>("contextSerializationEnv")

val _inheritableContextSerializationEnv = InheritableThreadLocalToggleField<SerializationEnvironment>("inheritableContextSerializationEnv") { stack ->
    stack.fold(false) { isAGlobalThreadBeingCreated, e ->
        isAGlobalThreadBeingCreated ||
                (e.className == "io.netty.util.concurrent.GlobalEventExecutor" && e.methodName == "startThread") ||
                (e.className == "java.util.concurrent.ForkJoinPool\$DefaultForkJoinWorkerThreadFactory" && e.methodName == "newThread")
    }
}

private val serializationEnvFields = listOf(
        _nodeSerializationEnv,
        _driverSerializationEnv,
        _contextSerializationEnv,
        _inheritableContextSerializationEnv,
        _rpcClientSerializationEnv
)

val _allEnabledSerializationEnvs: List<Pair<String, SerializationEnvironment>>
    get() = serializationEnvFields.mapNotNull { it.get()?.let { env -> Pair(it.name, env) } }

val effectiveSerializationEnv: SerializationEnvironment
    get() {
        return _allEnabledSerializationEnvs.let {
            checkNotNull(it.singleOrNull()?.second) {
                "Expected exactly 1 of {${serializationEnvFields.joinToString(", ") { it.name }}} " +
                        "but got: {${it.joinToString(", ") { it.first }}}"
            }
        }
    }
