package cordaCode.core.internal

import co.paralleluniverse.fibers.Suspendable
import cordaCode.core.flows.FlowLogic
import cordaCode.core.DeleteForDJVM
import cordaCode.core.DoNotImplement
import cordaCode.core.concurrent.CordaFuture
import cordaCode.core.context.InvocationContext
import cordaCode.core.flows.*
import cordaCode.core.identity.Party
import cordaCode.core.node.ServiceHub
import cordaCode.core.serialization.SerializedBytes
import org.slf4j.Logger

/** This is an internal interface that is implemented by code in the node module. You should look at [FlowLogic]. */
@DeleteForDJVM
@DoNotImplement
interface FlowStateMachine<FLOWRETURN> {
    @Suspendable
    fun <SUSPENDRETURN : Any> suspend(ioRequest: FlowIORequest<SUSPENDRETURN>, maySkipCheckpoint: Boolean): SUSPENDRETURN

    fun serialize(payloads: Map<FlowSession, Any>): Map<FlowSession, SerializedBytes<Any>>

    @Suspendable
    fun initiateFlow(destination: Destination, wellKnownParty: Party): FlowSession

    fun checkFlowPermission(permissionName: String, extraAuditData: Map<String, String>)

    fun recordAuditEvent(eventType: String, comment: String, extraAuditData: Map<String, String>)

    @Suspendable
    fun <SUBFLOWRETURN> subFlow(currentFlow: FlowLogic<*>, subFlow: FlowLogic<SUBFLOWRETURN>): SUBFLOWRETURN

    @Suspendable
    fun flowStackSnapshot(flowClass: Class<out FlowLogic<*>>): FlowStackSnapshot?

    @Suspendable
    fun persistFlowStackSnapshot(flowClass: Class<out FlowLogic<*>>)

    fun updateTimedFlowTimeout(timeoutSeconds: Long)

    val logic: FlowLogic<FLOWRETURN>
    val serviceHub: ServiceHub
    val logger: Logger
    val id: StateMachineRunId
    val resultFuture: CordaFuture<FLOWRETURN>
    val context: InvocationContext
    val ourIdentity: Party
    val ourSenderUUID: String?
    val creationTime: Long
    val isKilled: Boolean
}
