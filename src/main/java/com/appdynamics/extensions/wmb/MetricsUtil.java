package com.appdynamics.extensions.wmb;


import com.appdynamics.extensions.wmb.config.Configuration;
import com.appdynamics.extensions.wmb.resourcestats.json.ResourceStatsObj;

import java.util.HashMap;
import java.util.Map;

public class MetricsUtil {

    public Map<String,String> buildMetrics(Configuration config,ResourceStatsObj resourceStatsObj){
        Map<String,String> metrics = new HashMap<String, String>();

        return metrics;
    }

    public void postMetrics(Configuration config,Map<String,String> metrics){

    }

}
