package com.appdynamics.extensions.wmb;


import com.appdynamics.extensions.wmb.config.ConfigUtil;
import com.appdynamics.extensions.wmb.config.Configuration;
import com.appdynamics.extensions.wmb.config.ResourceStatTopic;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceIdentifier;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceStatistics;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceType;
import com.google.common.base.Strings;
import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQConnectionFactory;

import org.apache.log4j.Logger;

import javax.jms.*;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.FileNotFoundException;

public class WmbMonitor {

    public static final Logger logger = Logger.getLogger(WmbMonitor.class);
    public static final String CONFIG_FILENAME = "conf/config.yml";
    private final static ConfigUtil<Configuration> configUtil = new ConfigUtil<Configuration>();

    public static void main(String[] args){
        logger.info("Starting the WMB Monitor");
        WmbMonitor wmbMonitor = new WmbMonitor();
        wmbMonitor.execute();
        logger.info("Terminating the WMB Monitor");
    }

    public void execute(){
        try {
            // load the config file
            Configuration configuration = configUtil.readConfig(CONFIG_FILENAME,Configuration.class);
            try {
                //create connection
                Connection conn = createConnection(configuration);
                //build parser
                Unmarshaller parser = new ParserBuilder().getParser(ResourceStatistics.class, ResourceIdentifier.class, ResourceType.class);
                //register subscribers
                registerSubscribers(conn,configuration,parser);
                logger.info("Message Listener is registered.");
                //start connection
                conn.start();
                logger.info("Connection started. Wait Indefinitely.");
                //wait indefinitely
//                while(true){
//                    Thread.sleep(configuration.getSleepTime());
//                }
                Object obj = new Object();
                synchronized (obj) {
                    obj.wait();
                }
            } catch (JMSException e) {
                logger.error("Unable to connect or subscribe ::" + configuration.getHost() + "," + configuration.getPort(), e);
            } catch (JAXBException e) {
                logger.error("Unable to initialize the XML unmarshaller " + e);
            } catch (InterruptedException e) {
                logger.error("Insomniac! " + e);
            }
        } catch (FileNotFoundException e) {
            logger.error("Config file not found :: " + CONFIG_FILENAME, e);
        }


    }

    private void registerSubscribers(Connection conn, Configuration config,Unmarshaller parser) throws JMSException {
        Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        if(config != null && config.getResourceStatTopics() != null){
            for(ResourceStatTopic resTopic : config.getResourceStatTopics()){
                Topic topic = session.createTopic(resTopic.getName());
                TopicSubscriber topicSub = session.createDurableSubscriber(topic,resTopic.getSubscriberName());
                topicSub.setMessageListener(new ResourceStatMessageListener(config,parser));
            }
        }
    }



    private Connection createConnection(Configuration config) throws JMSException {
        MQConnectionFactory cf = new MQConnectionFactory();
        cf.setHostName(config.getHost());
        cf.setPort(config.getPort());
        cf.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
        cf.setQueueManager(config.getQueueManager());
        if(!Strings.isNullOrEmpty(config.getChannelName())){
        	cf.setChannel(config.getChannelName());
        }
        
        Connection connection = null;
        if(!Strings.isNullOrEmpty(config.getUserID()) && !Strings.isNullOrEmpty(config.getPassword())){
        	connection = cf.createConnection(config.getUserID(), config.getPassword());
        }
        else{
        	connection =  cf.createConnection();
        }
        
        connection.setClientID(config.getClientId());
        return connection;
    }

}
