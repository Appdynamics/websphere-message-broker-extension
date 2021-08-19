package com.appdynamics.extensions.wmb;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Maps;

import javax.jms.*;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.wmb.Util.convertToString;
import static com.appdynamics.extensions.wmb.Util.join;

public abstract class StatsProcessor<T> {

    protected static final String SEPARATOR = "|";

    protected Map config;
    protected XmlParser<T> parser;
    protected MetricWriteHelper metricWriteHelper;
    protected String metricPrefix;
    protected Map<String,Map> metricPropsHolder;

    public StatsProcessor(Map config, XmlParser<T> parser, MetricWriteHelper metricWriteHelper,String metricPrefix) {
        this.config = config;
        this.parser = parser;
        this.metricWriteHelper = metricWriteHelper;
        this.metricPrefix=metricPrefix;
    }

    public abstract void subscribe(Session session) throws JMSException;

    protected String getMessageString(Message message) throws JMSException {
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

    //creates a map from the config.yaml of all the configured metrics along with their configured metric properties.
    //protected Map<String, MetricProperties> buildMetricProperties(String name,String typeOfStatistics) {
    protected Map<String, Map> buildMetricProperties(String name,String typeOfStatistics) {
        Map<String,Map> propertiesMap = Maps.newHashMap();
        Object metricsObj = config.get(name);
        if(metricsObj != null){
            Map metrics = (Map)metricsObj;
            Object resourceStatsObj = metrics.get(typeOfStatistics);
            if(resourceStatsObj != null){
                List<Map> resourceStats = (List<Map>) resourceStatsObj;
                for(Map resourceStat : resourceStats){
                    String type= convertToString(resourceStat.get("type"),"");
                    Object identifiersObj = resourceStat.get("identifiers");
                    if(identifiersObj != null){
                        List<String> identifiers = (List<String>) identifiersObj;
                        for(String identifier : identifiers){
                            List includeMetrics = (List)resourceStat.get("include");
                            if(includeMetrics != null){
                                for(Object includeObj : includeMetrics){
                                    Map metadata = (Map)includeObj;
                                    String metricName = (String) metadata.get("name");
                                    propertiesMap.put(join(SEPARATOR,type,identifier,metricName),metadata);
                                }
                            }
                        }
                    }

                }
            }
        }
        return propertiesMap;
    }

    protected abstract List<Metric> buildMetrics(T stats);
}
