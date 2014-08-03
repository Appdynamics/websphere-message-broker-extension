package com.appdynamics.extensions.wmb.resourcestats.json;


import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceIdentifier {

    Map<String,String> resourceMetrics = new HashMap<String, String>();

    public Map<String, String> getResourceMetrics() {
        return resourceMetrics;
    }

    public void setResourceMetrics(Map<String, String> resourceMetrics) {
        this.resourceMetrics = resourceMetrics;
    }
}
