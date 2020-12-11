package com.template.webserver.Service;

import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class VendingMachineService {
    private HashMap<String, Integer> foodPrice = new HashMap<>();

    public VendingMachineService(){
        foodPrice.put("cola",3);
        foodPrice.put("biscuit",4);
        foodPrice.put("bread",5);
        foodPrice.put("water",2);

    }
    public Integer getFoodPrice(String foodName){
        return foodPrice.get(foodName);
    }
}
