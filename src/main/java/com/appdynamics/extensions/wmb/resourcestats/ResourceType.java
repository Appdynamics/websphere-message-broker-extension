package com.appdynamics.extensions.wmb.resourcestats;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceType {

    @XmlAttribute(name="name")
    private String name;

    @XmlElement(name = "resourceIdentifier")
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
