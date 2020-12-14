package cordaCode.core.flows

import cordaCode.core.CordaRuntimeException

/**
 * An exception that is thrown when a flow has been killed.
 *
 * This exception can be returned and thrown to RPC clients waiting for the result of a flow's future.
 *
 * It can also be used in conjunction with [FlowLogic.isKilled] to escape long-running computation loops when a flow has been killed.
 */
class KilledFlowException(val id: StateMachineRunId, message: String) : CordaRuntimeException(message) {
    constructor(id: StateMachineRunId) : this(id, "The flow $id was killed")
}