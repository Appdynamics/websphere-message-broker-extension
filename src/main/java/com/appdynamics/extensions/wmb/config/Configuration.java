package com.appdynamics.extensions.wmb.config;


import java.util.List;

public class Configuration {

    private String host;
    private int port;
    private String clientId;
    private String metricPrefix;
    private int sleepTime;
    private String machineAgentUrl;
    private int numberThreads;
    private int threadTimeout;
    private List<ResourceStatTopic> resourceStatTopics;


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }


    public List<ResourceStatTopic> getResourceStatTopics() {
        return resourceStatTopics;
    }

    public void setResourceStatTopics(List<ResourceStatTopic> resourceStatTopics) {
        this.resourceStatTopics = resourceStatTopics;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    public int getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }

    public String getMachineAgentUrl() {
        return machineAgentUrl;
    }

    public void setMachineAgentUrl(String machineAgentUrl) {
        this.machineAgentUrl = machineAgentUrl;
    }

    public int getNumberThreads() {
        return numberThreads;
    }

    public void setNumberThreads(int numberThreads) {
        this.numberThreads = numberThreads;
    }

    public int getThreadTimeout() {
        return threadTimeout;
    }

    public void setThreadTimeout(int threadTimeout) {
        this.threadTimeout = threadTimeout;
    }
}
