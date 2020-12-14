package cordaCode.core.flows

import kotlin.annotation.AnnotationTarget.CLASS

/**
 * Any [FlowLogic] which is to be started by the RPC interface ([cordaCode.core.messaging.CordaRPCOps.startFlowDynamic]
 * and [cordaCode.core.messaging.CordaRPCOps.startTrackedFlowDynamic]) must have this annotation. If it's missing the
 * flow will not be allowed to start and an exception will be thrown.
 */
@Target(CLASS)
@MustBeDocumented
// TODO Consider a different name, something along the lines of SchedulableFlow
annotation class StartableByRPC