package com.appdynamics.extensions.wmb.metricUtils;

import com.google.common.base.Strings;

import java.math.BigDecimal;

public class Metric {
    private String metricName;
    private String metricPath;
    private String clusterKey;
    private BigDecimal metricValue;
    private MetricProperties properties;

    public String getMetricNameOrAlias() {
        if(properties == null || Strings.isNullOrEmpty(properties.getAlias())){
            return metricName;
        }
        return properties.getAlias();
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getMetricPath() {
        return metricPath;
    }

    public void setMetricPath(String metricPath) {
        this.metricPath = metricPath;
    }

    public BigDecimal getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(BigDecimal metricValue) {
        this.metricValue = metricValue;
    }

    public MetricProperties getProperties() {
        return properties;
    }

    public void setProperties(MetricProperties properties) {
        this.properties = properties;
    }

    public String getClusterKey() {
        return clusterKey;
    }

    public void setClusterKey(String clusterKey) {
        this.clusterKey = clusterKey;
    }
}