package com.appdynamics.extensions.wmb.metrics;


import com.appdynamics.extensions.util.MetricWriteHelper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import static com.appdynamics.extensions.wmb.Util.toBigIntString;

public class MetricPrinter {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MetricPrinter.class);

    private int totalMetricsReported;
    private String metricPrefix;
    private String displayName;
    private MetricWriteHelper metricWriter;

    @VisibleForTesting
    public MetricPrinter(String metricPrefix, String displayName, MetricWriteHelper metricWriter){
        this.metricPrefix = metricPrefix;
        this.displayName = displayName;
        this.metricWriter = metricWriter;
    }

    public void reportMetrics(final List<Metric> metrics) {
        totalMetricsReported = 0;
        if(metrics == null || metrics.isEmpty()){
            return;
        }
        for(Metric metric : metrics){
            MetricProperties props = metric.getProperties();
            String fullMetricPath = formMetricPath(metric.getMetricKey());
            printMetric(fullMetricPath,metric.getMetricValue(), props.getAggregationType(),props.getTimeRollupType(),props.getClusterRollupType());
        }
        logger.debug("Total number of metrics reported by WMBMonitor {}",getTotalMetricsReported());
    }

    @VisibleForTesting
    public void printMetric(String metricPath, BigDecimal metricValue, String aggType, String timeRollupType, String clusterRollupType) {
        try{
            String metricValStr = toBigIntString(metricValue);
            if(metricValStr != null) {
                metricWriter.printMetric(metricPath,metricValStr,aggType,timeRollupType,clusterRollupType);
                  //System.out.println("Sending [" + aggType + "|" + timeRollupType + "|" + clusterRollupType
                  //		+ "] metric = " + metricPath + " = " + metricValStr);
                logger.debug("Sending [{}|{}|{}] metric= {},value={}", aggType, timeRollupType, clusterRollupType, metricPath, metricValStr);
                totalMetricsReported++;
            }
        }
        catch (Exception e){
            logger.error("Error reporting metric {} with value {}",metricPath,metricValue,e);
        }
    }

    private String formMetricPath(String metricKey) {
        if(!Strings.isNullOrEmpty(displayName)){
            return metricPrefix + "|" + displayName + "|" + metricKey;
        }
        return metricPrefix + "|" + metricKey;
    }

    public int getTotalMetricsReported() {
        return totalMetricsReported;
    }
}
