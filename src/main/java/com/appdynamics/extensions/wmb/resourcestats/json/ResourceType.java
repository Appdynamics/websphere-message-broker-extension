package com.appdynamics.extensions.wmb.resourcestats.json;


import com.google.gson.annotations.Expose;

import java.beans.Transient;
import java.util.List;

public class ResourceType {

    private String name;

    private List<ResourceIdentifier> resourceIdentifier;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ResourceIdentifier> getResourceIdentifier() {
        return resourceIdentifier;
    }

    public void setResourceIdentifier(List<ResourceIdentifier> resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }
}
