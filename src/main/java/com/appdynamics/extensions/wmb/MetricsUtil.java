package com.appdynamics.extensions.wmb;


import com.appdynamics.extensions.wmb.config.Configuration;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceIdentifier;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceStatistics;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceType;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class MetricsUtil {

    public static final Logger logger = Logger.getLogger(MetricsUtil.class);
    public static final String BROKER_LABEL = "brokerLabel";
    public static final String EXECUTION_GROUP_NAME = "executionGroupName";
    public static final String SEPARATOR = "|";
    public static final String NAME_QUERY_PARAM = "name=";
    public static final String VALUE_QUERY_PARAM = "value=";
    public static final String TYPE_QUERY_PARAM = "type=";
    public static final String UTF_8 = "UTF-8";

    public Map<String,String> buildMetrics(Configuration config,ResourceStatistics resourceStatistics){
        Map<String,String> metrics = new HashMap<String, String>();
        if(resourceStatistics != null){
            String brokerName = resourceStatistics.getAttributes().get(new QName(BROKER_LABEL));
            String executionGroupName = resourceStatistics.getAttributes().get(new QName(EXECUTION_GROUP_NAME));
            String metricPrefix = config.getMetricPrefix();
            if(resourceStatistics.getResourceType() != null){
                for(ResourceType resourceType : resourceStatistics.getResourceType()){
                    String resourceTypeName = resourceType.getName();
                    if(resourceType.getResourceIdentifiers() != null){
                        for(ResourceIdentifier resourceIdentifier : resourceType.getResourceIdentifiers()){
                            String resourceIdName = resourceIdentifier.getName();
                            for (QName key: resourceIdentifier.getAttributes().keySet()) {
                                String value = resourceIdentifier.getAttributes().get(key);
                                if(isMetricValid(key.toString(),value)){
                                    String wholeValue = toWholeNumberString(Double.parseDouble(value));
                                    StringBuilder metricBuilder = new StringBuilder();
                                    metricBuilder.append(metricPrefix)
                                            .append(brokerName != null ? brokerName : "").append(SEPARATOR)
                                            .append(executionGroupName != null ? executionGroupName : "").append(SEPARATOR)
                                            .append(resourceTypeName != null ? resourceTypeName : "").append(SEPARATOR)
                                            .append(resourceIdName != null ? resourceIdName : "").append(SEPARATOR)
                                            .append(key);
                                    metrics.put(metricBuilder.toString(),wholeValue);
                                }
                            }
                        }
                    }
                }
            }
        }
        return metrics;
    }

    public void postMetrics(Configuration config,Map<String,String> metrics,final CloseableHttpClient httpClient) throws IOException {
        ExecutorService threadPool = Executors.newFixedThreadPool(config.getNumberThreads());
        try {
            for (String key : metrics.keySet()) {
                String value = metrics.get(key);
                final String url = formUrl(config, key, value);
                if (logger.isDebugEnabled()) {
                    logger.debug("Posting metric : " + url);
                }
                Future task = threadPool.submit(new Runnable() {
                    public void run() {
                        HttpGet getMethod = new HttpGet(url);
                        try {
                            CloseableHttpResponse response = httpClient.execute(getMethod);
                            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                                logger.error("Posting metrics to machine agent failed : " + url);
                            }
                        } catch (ClientProtocolException e) {
                            logger.error("Posting metrics to machine agent failed : " + url, e);
                        } catch (IOException e) {
                            logger.error("Posting metrics to machine agent failed : " + url, e);
                        } finally {
                            getMethod.releaseConnection();
                        }
                    }
                });
                try {
                    task.get(config.getThreadTimeout(), TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    logger.error("Thread interrupted : " + url, e);
                } catch (ExecutionException e) {
                    logger.error("Something unforeseen occurred : " + url, e);
                } catch (TimeoutException e) {
                    logger.error("Thread timed out : " + url, e);
                }

            }
        }
        finally {
            if(!threadPool.isShutdown()){
                threadPool.shutdown();
            }
        }
    }


    private boolean isMetricValid(String key, String value) {
        //value should be numerical whole number
        try{
            double d = Double.parseDouble(value);
            if(d < 0){
                return false;
            }
        } catch(NumberFormatException nfe){
            return false;
        }
        //value should be positive
        return true;
    }




    private String formUrl(Configuration config, String key, String value) throws UnsupportedEncodingException {
        StringBuilder urlBuilder = new StringBuilder(config.getMachineAgentUrl());
        urlBuilder.append(NAME_QUERY_PARAM).append(URLEncoder.encode(key, UTF_8));
        urlBuilder.append("&").append(VALUE_QUERY_PARAM).append(URLEncoder.encode(value,UTF_8));
        urlBuilder.append("&").append(TYPE_QUERY_PARAM).append("average");
        return urlBuilder.toString();
    }

    /**
     * Currently, appD controller only supports Integer values. This function will round all the decimals into integers and convert them into strings.
     * If number is less than 0.5, Math.round will round it to 0 which is not useful on the controller.
     * @param attribute
     * @return
     */
    private  String toWholeNumberString(Object attribute) {
        if(attribute instanceof Double){
            Double d = (Double) attribute;
            if(d > 0 && d < 1.0d){
                return "1";
            }
            return String.valueOf(Math.round(d));
        }
        else if(attribute instanceof Float){
            Float f = (Float) attribute;
            if(f > 0 && f < 1.0f){
                return "1";
            }
            return String.valueOf(Math.round((Float) attribute));
        }
        return attribute.toString();
    }
}
