package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class  MACRO_TIME_MANG {
    private boolean isUsed = false;
    private long startTime;
    private HashMap<String, Long> timeTable;

    public MACRO_TIME_MANG(){
        this.timeTable = new HashMap<>();
    }

    public void close(){
        this.isUsed = false;
    }

    public void start(){
        this.isUsed = true;
        if (this.isUsed){
            startTime = System.currentTimeMillis();
        }
    }

    public synchronized void cut(String cutPoint){
        if (this.isUsed){
            this.timeTable.put(cutPoint, System.currentTimeMillis());
        }
    }

    public void end(){
        if (this.isUsed){
            timeTable.put("end", System.currentTimeMillis());
        }
    }

    public String result(){
        if (isUsed){
            Long Last_point = startTime;
            ArrayList<String> arrayList = new ArrayList<>();
        for (Map.Entry<String, Long> entry: timeTable.entrySet()){
            arrayList.add(entry.getKey() + " Time: " + (entry.getValue() - Last_point) + "ms");
            Last_point = entry.getValue();
        }
        return arrayList.toString();
    }
    return "No result";
    }
}
