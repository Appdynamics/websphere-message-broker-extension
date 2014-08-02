package com.appdynamics.extensions.wmb.resourcestats.json;


import java.util.List;

public class ResourceStatistics {

    private List<ResourceType> resourceTypes;


    public List<ResourceType> getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(List<ResourceType> resourceTypes) {
        this.resourceTypes = resourceTypes;
    }
}
