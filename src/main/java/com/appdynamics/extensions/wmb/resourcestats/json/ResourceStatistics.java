package com.appdynamics.extensions.wmb.resourcestats.json;


import java.util.List;

public class ResourceStatistics {

    private List<ResourceType> ResourceType;

    public List<ResourceType> getResourceType() {
        return ResourceType;
    }

    public void setResourceType(List<ResourceType> resourceType) {
        ResourceType = resourceType;
    }
}
