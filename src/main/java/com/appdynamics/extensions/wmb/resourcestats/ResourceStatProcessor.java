package com.appdynamics.extensions.wmb.resourcestats;

import com.appdynamics.extensions.wmb.MetricPrinter;
import com.appdynamics.extensions.wmb.XmlParser;
import com.appdynamics.extensions.wmb.metrics.DefaultMetricProperties;
import com.appdynamics.extensions.wmb.metrics.Metric;
import com.appdynamics.extensions.wmb.metrics.MetricProperties;
import com.appdynamics.extensions.wmb.metrics.MetricValueTransformer;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceIdentifier;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceStatistics;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceType;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.wmb.Util.convertToString;

public class ResourceStatProcessor implements MessageListener {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ResourceStatProcessor.class);
    private static final String BROKER_LABEL = "brokerLabel";
    private static final String EXECUTION_GROUP_NAME = "executionGroupName";

    private Map config;
    private XmlParser<ResourceStatistics> parser;
    private MetricPrinter printer;
    private final MetricValueTransformer valueTransformer = new MetricValueTransformer();

    public ResourceStatProcessor(Map config, XmlParser parser, MetricPrinter printer) {
        this.config = config;
        this.printer = printer;
        this.parser = parser;
    }

    public void subscribe(Connection conn) throws JMSException {
        Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Object metricsObj = config.get("metrics");
        if(metricsObj != null){
            Map metrics = (Map)metricsObj;
            if(metrics.get("resourceStatistics") != null){
                List<Map> resourceStats = (List<Map>) metrics.get("resourceStatistics");
                for(Map resourceStat : resourceStats){
                    Topic topic = session.createTopic(convertToString(resourceStat.get("name"),""));
                    TopicSubscriber topicSub = session.createDurableSubscriber(topic,convertToString(resourceStat.get("subscriberName"),""));
                    topicSub.setMessageListener(this);
                }
            }
        }
        logger.info("Resource Statistic Subscribers are registered.");
    }

    public void onMessage(Message message) {
    	long startTime = System.currentTimeMillis();
        String text = null;
        try {
        	text = getMessageString(message);
            if(text != null) {
                try {
                    ResourceStatistics resourceStatistics = parser.parse(text);
                    if (resourceStatistics != null) {
                        List<Metric> metrics = buildMetrics(resourceStatistics);
                        printer.reportMetrics(metrics);
                    }
                } catch (JAXBException e) {
                    logger.error("Unable to unmarshal XML message {}", text,e);
                }
            }
            else{
                logger.error("Message received is null.");
            }
        } catch(JMSException e){
            logger.error("Unable to process message {}", e);
        }
        logger.debug("Time taken to process one message = {}" ,Long.toString(System.currentTimeMillis() - startTime));
    }

    private List<Metric> buildMetrics(ResourceStatistics resourceStatistics){
        List<Metric> metrics = new ArrayList<Metric>();
        if(resourceStatistics != null){
            String brokerName = resourceStatistics.getAttributes().get(new QName(BROKER_LABEL));
            String executionGroupName = resourceStatistics.getAttributes().get(new QName(EXECUTION_GROUP_NAME));
            if(resourceStatistics.getResourceType() != null){
                for(ResourceType resourceType : resourceStatistics.getResourceType()){
                    String resourceTypeName = resourceType.getName();
                    if(resourceType.getResourceIdentifiers() != null){
                        for(ResourceIdentifier resourceIdentifier : resourceType.getResourceIdentifiers()){
                            String resourceIdName = resourceIdentifier.getName();
                            for (QName key: resourceIdentifier.getAttributes().keySet()) {
                                String value = resourceIdentifier.getAttributes().get(key);
                                String metricPath = formMetricPath(brokerName,executionGroupName,resourceTypeName,resourceIdName,key.toString());
                                MetricProperties metricProps = new DefaultMetricProperties();
                                BigDecimal val = valueTransformer.transform(metricPath,value,metricProps);
                                if(val != null){
                                    Metric m = new Metric();
                                    m.setMetricName(key.toString());
                                    m.setMetricKey(metricPath);
                                    m.setProperties(metricProps);
                                    m.setMetricValue(val);
                                    metrics.add(m);
                                }
                            }
                        }
                    }
                }
            }
        }
        return metrics;
    }

    private String formMetricPath(String brokerName, String executionGroupName, String resourceTypeName, String resourceIdName, String key) {
        StringBuilder metricBuilder = new StringBuilder();
        metricBuilder.append(brokerName != null ? brokerName : "")
                .append(executionGroupName != null ? "|" + executionGroupName : "")
                .append(resourceTypeName != null ? "|" + resourceTypeName : "")
                .append(resourceIdName != null ? "|" + resourceIdName : "")
                .append(key != null ? "|" + key : "");
        return metricBuilder.toString();
    }

    private String getMessageString(Message message) throws JMSException {
		if(message != null) {
            if (message instanceof TextMessage) {
                TextMessage tm = (TextMessage) message;
                return tm.getText();
            } else if (message instanceof BytesMessage) {
                BytesMessage bm = (BytesMessage) message;
                byte data[] = new byte[(int) bm.getBodyLength()];
                bm.readBytes(data);
                return new String(data);
            }
        }
		throw new JMSException("Message is not of TextMessage/BytesMessage.");
	}
}
