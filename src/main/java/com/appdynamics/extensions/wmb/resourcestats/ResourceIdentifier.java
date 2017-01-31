package com.appdynamics.extensions.wmb.resourcestats;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.namespace.QName;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceIdentifier {

    @XmlAttribute(name="name")
    private String name;

    @XmlAnyAttribute
    private Map<QName,String> attributes;

    public Map<QName, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<QName, String> attributes) {
        this.attributes = attributes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
