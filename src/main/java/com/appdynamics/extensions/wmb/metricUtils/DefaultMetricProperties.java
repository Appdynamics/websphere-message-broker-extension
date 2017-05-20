package com.appdynamics.extensions.wmb.metricUtils;

import com.singularity.ee.agent.systemagent.api.MetricWriter;

public class DefaultMetricProperties extends MetricProperties{

    private static final String DEFAULT_METRIC_TYPE = MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE + " " + MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE + " " + MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL;
    private static final boolean DEFAULT_DELTA = false;

    public DefaultMetricProperties(){
        setAggregationFields(DEFAULT_METRIC_TYPE);
        setMultiplier(DEFAULT_MULTIPLIER);
        setDelta(DEFAULT_DELTA);
    }

}
