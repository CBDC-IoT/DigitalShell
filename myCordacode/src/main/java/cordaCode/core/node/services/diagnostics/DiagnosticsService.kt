package cordaCode.core.node.services.diagnostics

import cordaCode.core.DoNotImplement

/**
 * A [DiagnosticsService] provides APIs that allow CorDapps to query information about the node that CorDapp is currently running on.
 */
@DoNotImplement
interface DiagnosticsService {

    /**
     * Retrieve information about the current node version.
     *
     * @return [NodeVersionInfo] A structure holding information about the current node version.
     */
    fun nodeVersionInfo() : NodeVersionInfo
}