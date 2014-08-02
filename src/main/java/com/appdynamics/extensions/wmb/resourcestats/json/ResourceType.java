package com.appdynamics.extensions.wmb.resourcestats.json;


import java.util.List;

public class ResourceType {

    private String name;
    private List<ResourceIdentifier> resourceIdentifiers;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ResourceIdentifier> getResourceIdentifiers() {
        return resourceIdentifiers;
    }

    public void setResourceIdentifiers(List<ResourceIdentifier> resourceIdentifiers) {
        this.resourceIdentifiers = resourceIdentifiers;
    }
}
