package com.appdynamics.extensions.wmb;

import com.appdynamics.extensions.wmb.metricUtils.*;
import com.google.common.collect.Maps;

import javax.jms.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.wmb.Util.convertToString;
import static com.appdynamics.extensions.wmb.Util.join;

public abstract class StatsProcessor<T> {

    protected static final String SEPARATOR = "|";

    protected Map config;
    protected XmlParser<T> parser;
    protected MetricPrinter printer;
    protected Map<String,MetricProperties> metricPropsHolder;
    protected final MetricValueTransformer valueTransformer = new MetricValueTransformer();

    public StatsProcessor(Map config, XmlParser<T> parser, MetricPrinter printer) {
        this.config = config;
        this.printer = printer;
        this.parser = parser;
    }

    public abstract void subscribe(Session session) throws JMSException;

    protected MetricProperties createMetricProperties(Map metadata, String metricName, String alias) {
        MetricProperties props = new DefaultMetricProperties();
        props.setAlias(alias);
        props.setMetricName(metricName);
        if(metadata.get("metricType") != null){
            props.setAggregationFields(metadata.get("metricType").toString());
        }
        if(metadata.get("multiplier") != null){
            props.setMultiplier(Double.parseDouble(metadata.get("multiplier").toString()));
        }
        if(metadata.get("convert") != null){
            props.setConversionValues((Map)metadata.get("convert"));
        }
        if(metadata.get("delta") != null){
            props.setDelta(Boolean.parseBoolean(metadata.get("delta").toString()));
        }
        return props;
    }

    protected Metric createMetricPoint(String metricPath, String value, MetricProperties properties, String metricName) {
        BigDecimal decimalValue = valueTransformer.transform(metricPath + SEPARATOR + metricName, value, properties);
        if (decimalValue != null) {
            Metric m = new Metric();
            m.setMetricName(metricName);
            m.setMetricPath(metricPath);
            m.setProperties(properties);
            m.setMetricValue(decimalValue);
            return m;
        } else {
            return null;
        }
    }

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
    protected Map<String, MetricProperties> buildMetricProperties(String name,String typeOfStatistics) {
        Map<String,MetricProperties> propertiesMap = Maps.newHashMap();
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
                                    Map.Entry entry = (Map.Entry)metadata.entrySet().iterator().next();
                                    String metricName = entry.getKey().toString();
                                    String alias = entry.getValue().toString();
                                    MetricProperties props = createMetricProperties(metadata, metricName, alias);
                                    propertiesMap.put(join(SEPARATOR,type,identifier,metricName),props);
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