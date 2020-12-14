package cordaCode.core.node.services

import cordaCode.core.DeleteForDJVM
import cordaCode.core.contracts.TimeWindow
import java.time.Clock

/**
 * Checks if the current instant provided by the input clock falls within the provided time-window.
 */
@Deprecated("This class is no longer used")
@DeleteForDJVM
class TimeWindowChecker(val clock: Clock = Clock.systemUTC()) {
    fun isValid(timeWindow: TimeWindow): Boolean = clock.instant() in timeWindow
}
