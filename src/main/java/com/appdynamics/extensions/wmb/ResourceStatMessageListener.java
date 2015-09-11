package com.appdynamics.extensions.wmb;

import com.appdynamics.extensions.wmb.config.Configuration;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceStatistics;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

public class ResourceStatMessageListener implements MessageListener {

    public static final Logger logger = Logger.getLogger(ResourceStatMessageListener.class);
    private Configuration config;
    private Unmarshaller parser;
    private static MetricsUtil metricsUtil = new MetricsUtil();
    private static CloseableHttpClient httpclient = HttpClients.createDefault();

    public ResourceStatMessageListener(Configuration config,Unmarshaller parser) {
        this.config = config;
        this.parser = parser;
    }

    public void onMessage(Message message) {
    	long startTime = System.currentTimeMillis();
        try {
        	String text = getMessageString(message);
            if(text != null) {
                StringReader reader = new StringReader(text);
                try {
                    ResourceStatistics resourceStatistics = (ResourceStatistics) parser.unmarshal(reader);
                    if (resourceStatistics != null) {
                        Map<String, String> metricsMap = metricsUtil.buildMetrics(config, resourceStatistics);
                        metricsUtil.postMetrics(config,metricsMap,httpclient);
                    }
                } catch (JAXBException e) {
                    logger.error("Unable to unmarshal XML message", e);
                } catch (IOException e) {
                    logger.error("Unable to unmarshal XML message", e);
                }
            }
            else{
                logger.error("Message received is null.");
            }
        } catch(JMSException e){
            logger.error("Unable to process message", e);
        }
        if(logger.isDebugEnabled()){
            logger.debug("Time taken to process one message : " + Long.toString(System.currentTimeMillis() - startTime));
        }
    }

	private String getMessageString(Message message) throws JMSException {
		if(message instanceof TextMessage){
			TextMessage tm = (TextMessage)message;
			return tm.getText();
		}
		else if(message instanceof BytesMessage){
			BytesMessage bm=(BytesMessage)message;
		    byte data[]=new byte[(int)bm.getBodyLength()];
		    bm.readBytes(data);
		    return new String(data);
		}
		throw new JMSException("Message is not of TextMessage/BytesMessage.");
	}
}
