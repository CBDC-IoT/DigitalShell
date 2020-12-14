package cordaCode.core.internal

import co.paralleluniverse.fibers.Suspendable
import cordaCode.core.concurrent.CordaFuture
import cordaCode.core.flows.FlowLogic
import cordaCode.core.serialization.CordaSerializable

/**
 * Interface for arbitrary operations that can be invoked in a flow asynchronously - the flow will suspend until the
 * operation completes. Operation parameters are expected to be injected via constructor.
 */
@CordaSerializable
interface FlowAsyncOperation<R : Any> {
    /**
     * Performs the operation in a non-blocking fashion.
     * @param deduplicationId  If the flow restarts from a checkpoint (due to node restart, or via a visit to the flow
     * hospital following an error) the execute method might be called more than once by the Corda flow state machine.
     * For each duplicate call, the deduplicationId is guaranteed to be the same allowing duplicate requests to be
     * de-duplicated if necessary inside the execute method.
     */
    fun execute(deduplicationId: String): CordaFuture<R>
}

/** Executes the specified [operation] and suspends until operation completion. */
@Deprecated(
    "This has been replaced by [FlowLogic.await] that provides an improved and public API",
    ReplaceWith("cordaCode.core.flows.FlowLogic.await")
)
@Suspendable
fun <T, R : Any> FlowLogic<T>.executeAsync(operation: FlowAsyncOperation<R>, maySkipCheckpoint: Boolean = false): R {
    val request = FlowIORequest.ExecuteAsyncOperation(operation)
    return stateMachine.suspend(request, maySkipCheckpoint)
}
