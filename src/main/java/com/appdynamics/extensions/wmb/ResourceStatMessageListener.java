package com.appdynamics.extensions.wmb;

import com.appdynamics.extensions.wmb.config.Configuration;
import com.appdynamics.extensions.wmb.resourcestats.json.ResourceStatsObj;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.Map;

public class ResourceStatMessageListener implements MessageListener {

    public static final Logger logger = Logger.getLogger(ResourceStatMessageListener.class);
    private Configuration config;
    private Gson parser;
    private MetricsUtil metricsUtil = new MetricsUtil();

    public ResourceStatMessageListener(Configuration config,Gson parser) {
        this.config = config;
        this.parser = parser;
    }

    public void onMessage(Message message) {
        TextMessage tm = (TextMessage)message;
        try {
            String text = tm.getText();
            try {
                ResourceStatsObj resourceStatsObj = parser.fromJson(text, ResourceStatsObj.class);
                if(resourceStatsObj != null){
                    Map<String,String> metricsMap = metricsUtil.buildMetrics(config,resourceStatsObj);
                }
            } catch (JsonParseException e) {
                logger.error("Unable to parse json ::" + text, e);
            }
        } catch(JMSException e){
            logger.error("Unable to process message", e);
        }
    }
}
