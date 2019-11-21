package org.folio.ncip.domain;

import java.util.HashMap;
import java.util.Map;


public class DroolsExchange {

    private Map<String, Object> context;

    public void addToContext(String parameter, Object value) {
        getContext().put(parameter, value);
    }

    public Object getFromContext(String parameter){
        return getContext().get(parameter);
    }

    public Map<String,Object> getContext() {
        if(null == context){
            context = new HashMap<>();
        }
        return context;
    }
}
