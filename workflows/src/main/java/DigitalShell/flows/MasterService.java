package DigitalShell.flows;

import cordaCode.core.identity.Party;
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
