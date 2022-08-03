package util;

import net.corda.core.identity.Party;
import net.corda.core.node.services.IdentityService;

public class IdentityManager {

    public static Party getParty(IdentityService identityService, String name) {
        return identityService.partiesFromName(name,false).stream().findAny().orElseThrow(()-> new IllegalArgumentException(name + " party not found"));
    }
}
