package cordaCode.core.utilities

import cordaCode.core.DeleteForDJVM

@DeleteForDJVM
object SgxSupport {
    @JvmStatic
    val isInsideEnclave: Boolean by lazy {
        (System.getProperty("os.name") == "Linux") && (System.getProperty("java.vm.name") == "Avian (Corda)")
    }
}
