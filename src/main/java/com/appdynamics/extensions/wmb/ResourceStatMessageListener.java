package com.appdynamics.extensions.wmb;

import com.appdynamics.extensions.wmb.config.Configuration;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

public class ResourceStatMessageListener implements MessageListener {

    private Configuration config;

    public ResourceStatMessageListener(Configuration config) {
        this.config = config;
    }

    public void onMessage(Message message) {
        // print out the incoming message in string format
        TextMessage tm = (TextMessage)message;
        try {
            System.out.println(tm.getText());
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
