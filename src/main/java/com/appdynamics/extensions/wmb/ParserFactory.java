package com.appdynamics.extensions.wmb;


import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceIdentifier;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceStatistics;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class ParserFactory {

    public XmlParser getResourceStatisticsParser() throws JAXBException {
        return new XmlParser<ResourceStatistics>(build(ResourceIdentifier.class,ResourceStatistics.class,ResourceType.class));
    }

    private Unmarshaller build(Class... classes) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(classes);
        return jaxbContext.createUnmarshaller();
    }
}
