package com.appdynamics.extensions.wmb.metrics;


import com.appdynamics.extensions.util.MetricWriteHelper;

public class CustomMetricWriter extends MetricWriteHelper {

    public void printMetric(String metricPath, String metricValue, String aggregationType, String timeRollup, String clusterRollup) {
        StringBuilder sb = new StringBuilder();
        sb.append("name").append("=").append(metricPath).append(",");
        sb.append("value").append("=").append(metricValue);
        if(aggregationType != null) {
            sb.append(",");
            sb.append("aggregator").append("=").append(aggregationType);
        }

        if(timeRollup != null) {
            sb.append(",");
            sb.append("time-rollup").append("=").append(timeRollup);
        }

        if(clusterRollup != null) {
            sb.append(",");
            sb.append("cluster-rollup").append("=").append(clusterRollup);
        }
        System.out.println(sb.toString());
    }

}
