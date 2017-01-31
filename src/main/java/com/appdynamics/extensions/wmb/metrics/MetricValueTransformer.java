package com.appdynamics.extensions.wmb.metrics;


import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class MetricValueTransformer {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MetricValueTransformer.class);

    public BigDecimal transform(String metricPath,Object metricValue,MetricProperties props){
        if(metricValue == null){
            logger.error("Metric value for {} is null",metricPath);
            throw new IllegalArgumentException("Metric value cannot be null");
        }
        Object convertedValue = applyConvert(metricPath,metricValue,props);
        BigDecimal val = applyMultiplier(metricPath,convertedValue,props);
        return val;
    }


    private BigDecimal applyMultiplier(String metricName, Object metricValue, MetricProperties props) {
        try {
            BigDecimal bigD = new BigDecimal(metricValue.toString());
            double multiplier = props.getMultiplier();
            bigD = bigD.multiply(new BigDecimal(multiplier));
            return bigD;
        }
        catch(NumberFormatException nfe){
        	// debug, not warning, as this is the expected behavior for many attributes
            logger.debug("Cannot convert into BigDecimal {} value for metric {}.",metricValue,metricName);
        }
        return null;
    }

    private Object applyConvert(String metricName,Object metricValue,MetricProperties props){
        //get converted values if configured
        if(props.getConversionValues() != null && !props.getConversionValues().isEmpty()) {
            Object convertedValue = props.getConversionValues().get(metricValue);
            if (convertedValue != null) {
                logger.debug("Applied conversion on {} and replaced value {} with {}", metricName, metricValue, convertedValue);
                return convertedValue;
            }
            else{
                if(props.getConversionValues().get("$default") != null){
                    logger.debug("Choosing the $default value to go with {} for conversion",metricValue);
                    return props.getConversionValues().get("$default");
                }
            }
        }
        return metricValue;
    }
}

