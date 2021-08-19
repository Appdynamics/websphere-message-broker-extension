package com.appdynamics.extensions.wmb;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.wmb.flowstats.FlowStatistics;
import com.appdynamics.extensions.wmb.resourcestats.ResourceStatistics;
import com.appdynamics.extensions.wmb.resourcestats.ResourceStatsProcessor;
import org.slf4j.Logger;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.xml.bind.JAXBException;
import java.util.Map;

class StatsSubscription {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(StatsSubscription.class);
    private Map config;
    private MetricWriteHelper metricWriteHelper;
    private String metricPrefix;
    final ParserFactory parserFactory = new ParserFactory();

    StatsSubscription(Map queueManagerConfig, MetricWriteHelper metricWriteHelper,String metricPrefix) {
        this.config = queueManagerConfig;
        this.metricWriteHelper=metricWriteHelper;
        this.metricPrefix=metricPrefix;
    }

    void subscribe(Connection conn) throws JMSException, JAXBException {
        Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        // Subscribe to resource statistics
        if (config.get("resourceStatisticsSubscribers") != null) {
            logger.info("Subscribing to resourceStatisticsSubscribers");
            StatsProcessor<ResourceStatistics> resourceStatsProcessor = new ResourceStatsProcessor<ResourceStatistics>(config,parserFactory.getResourceStatisticsParser(),metricWriteHelper,metricPrefix);
            resourceStatsProcessor.subscribe(session);
        }
        // Subscribe to message flow statistics
        if (config.get("flowStatisticsSubscribers") != null) {
            logger.info("Subscribing to flowStatisticsSubscribers");
            StatsProcessor<FlowStatistics> flowStatsProcessor = new com.appdynamics.extensions.wmb.flowstats.FlowStatsProcessor(config,parserFactory.getFlowStatisticsParser(),metricWriteHelper,metricPrefix);
            flowStatsProcessor.subscribe(session);
        }

    }
}
