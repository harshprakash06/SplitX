package com.xtemper.connectme.Helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ListDuplicateRemover {
    public static List<HashMap<String, Object>> removeDuplicate(List<HashMap<String, Object>>  list){
        List<HashMap<String, Object>> newList = new ArrayList<>();
        if(list.isEmpty())
            return newList;
        if(list.size()==1){
            newList.add(list.get(0));
            return newList;
        }
        for (HashMap<String, Object> stringObjectHashMap : list) {
            if (newList.contains(stringObjectHashMap))
                continue;
            else {
                newList.add(stringObjectHashMap);
            }
        }
        return newList;
    }

}
