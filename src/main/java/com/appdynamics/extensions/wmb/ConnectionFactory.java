package com.appdynamics.extensions.wmb;


import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import java.util.Map;

import static com.appdynamics.extensions.wmb.Util.convertToString;

/**
 * ConnectionFactory to connect to the QM.
 */
class ConnectionFactory {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ConnectionFactory.class);

    Connection createConnection(Map queueManagerConfig) throws JMSException {
        logger.info("Connecting to the queue manager...");
        MQConnectionFactory cf = new MQConnectionFactory();
        cf.setHostName(convertToString(queueManagerConfig.get("host"),"localhost"));
        String portStr = convertToString(queueManagerConfig.get("port"),"");
        int port = portStr != null ? Integer.parseInt(portStr) : -1;
        cf.setPort(port);
        String transportType = convertToString(queueManagerConfig.get("transportType"),"Bindings");
        if(transportType.equalsIgnoreCase("Client")){
            cf.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        }
        else{
            cf.setTransportType(WMQConstants.WMQ_CM_BINDINGS);
        }
        if(queueManagerConfig.get("name") != null && !queueManagerConfig.get("name").equals("")){
            cf.setQueueManager(convertToString(queueManagerConfig.get("name"),""));
        }

        if(queueManagerConfig.get("channelName") != null && !queueManagerConfig.get("channelName").equals("")){
            cf.setChannel(convertToString(queueManagerConfig.get("channelName"),""));
        }
        if(queueManagerConfig.get("sslKeyRepository") != null && !queueManagerConfig.get("sslKeyRepository").equals("")){
            cf.setSSLCertStores(convertToString(queueManagerConfig.get("sslKeyRepository"),""));
        }
        if(queueManagerConfig.get("cipherSuite") != null && !queueManagerConfig.get("cipherSuite").equals("")){
            cf.setSSLCipherSuite(convertToString(queueManagerConfig.get("cipherSuite"),""));
        }

        Connection connection = null;
        String userId=convertToString(queueManagerConfig.get("userID"),"");
        String password=convertToString(queueManagerConfig.get("password"),"");
        connection = cf.createConnection(userId,password);
        connection.setClientID(convertToString(queueManagerConfig.get("clientID"),""));
        logger.info("Connection to QM {} is successful..",convertToString(queueManagerConfig.get("name"),""));
        return connection;
    }
}
