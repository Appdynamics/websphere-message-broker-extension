package com.appdynamics.extensions.wmb.resourcestats;


import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

@XmlRootElement(name = "ResourceStatistics")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceStatistics {

    @XmlAnyAttribute
    private Map<QName,String> attributes;

    @XmlElement(name = "ResourceType")
    private List<ResourceType> resourceType;

    public Map<QName, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<QName, String> attributes) {
        this.attributes = attributes;
    }

    public List<ResourceType> getResourceType() {
        return resourceType;
    }

    public void setResourceType(List<ResourceType> resourceType) {
        this.resourceType = resourceType;
    }
}
