package net.corda.DigitalShell.flows;

import net.corda.core.identity.Party;
import java.util.HashMap;
import java.util.Map;

public class MasterService {
    Map<Party, Boolean> notaryList;
    public MasterService(){
    notaryList = new HashMap<>();
    }

    public boolean isAvaiable(Party party){
        return notaryList.get(party);
    }
}
