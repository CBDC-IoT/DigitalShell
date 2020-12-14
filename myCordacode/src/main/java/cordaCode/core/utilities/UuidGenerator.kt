package cordaCode.core.utilities

import cordaCode.core.DeleteForDJVM
import java.util.*

@DeleteForDJVM
class UuidGenerator {

    companion object {
        fun next(): UUID = UUID.randomUUID()
    }
}