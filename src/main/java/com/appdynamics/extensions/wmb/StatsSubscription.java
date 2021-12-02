package com.appdynamics.extensions.wmb;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.wmb.flowstats.FlowStatistics;
import com.appdynamics.extensions.wmb.flowstats.FlowStatsProcessor;
import com.appdynamics.extensions.wmb.metricUtils.MetricPrinter;
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
    private MetricPrinter printer;
    final ParserFactory parserFactory = new ParserFactory();

    StatsSubscription(Map queueManagerConfig, MetricPrinter metricPrinter) {
        this.config = queueManagerConfig;
        this.printer = metricPrinter;
    }

    void subscribe(Connection conn) throws JMSException, JAXBException {
        Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        // Subscribe to resource statistics
        if (config.get("resourceStatisticsSubscribers") != null) {
            StatsProcessor<ResourceStatistics> resourceStatsProcessor = new ResourceStatsProcessor<ResourceStatistics>(config,parserFactory.getResourceStatisticsParser(),printer);
            resourceStatsProcessor.subscribe(session);
        }
        // Subscribe to message flow statistics
        if (config.get("flowStatisticsSubscribers") != null) {
            StatsProcessor<FlowStatistics> flowStatsProcessor = new FlowStatsProcessor(config,parserFactory.getFlowStatisticsParser(),printer);
            flowStatsProcessor.subscribe(session);
        }

    }
}