package com.appdynamics.extensions.wmb.resourcestats;


import com.appdynamics.extensions.wmb.config.ResourceStatTopic;

import java.util.concurrent.Callable;

public class ResourceStatsSubscriber implements Callable<Void> {

    private String host;
    private int port;
    private ResourceStatTopic resourceStat;
    private String metricPrefix;

    public ResourceStatsSubscriber(String host, int port, ResourceStatTopic resourceStat, String metricPrefix) {
        this.host = host;
        this.port = port;
        this.resourceStat = resourceStat;
        this.metricPrefix = metricPrefix;
    }

    public Void call() throws Exception {
        //connect to the broker

        //
        return null;
    }

}
