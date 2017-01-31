package com.appdynamics.extensions.wmb;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.appdynamics.extensions.wmb.flowstats.FlowStatistics;
import com.appdynamics.extensions.wmb.flowstats.MessageFlow;
import com.appdynamics.extensions.wmb.flowstats.Node;
import com.appdynamics.extensions.wmb.flowstats.Terminal;
import com.appdynamics.extensions.wmb.flowstats.Thread;
import com.appdynamics.extensions.wmb.resourcestats.ResourceIdentifier;
import com.appdynamics.extensions.wmb.resourcestats.ResourceStatistics;
import com.appdynamics.extensions.wmb.resourcestats.ResourceType;

public class ParserFactory {

    public XmlParser getResourceStatisticsParser() throws JAXBException {
        return new XmlParser<ResourceStatistics>(build(ResourceIdentifier.class,ResourceStatistics.class,ResourceType.class));
    }
    
    public XmlParser getFlowStatisticsParser() throws JAXBException {
        return new XmlParser<FlowStatistics>(build(FlowStatistics.class, MessageFlow.class, Node.class, Terminal.class, Thread.class));
    }

    private Unmarshaller build(Class... classes) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(classes);
        return jaxbContext.createUnmarshaller();
    }
}
