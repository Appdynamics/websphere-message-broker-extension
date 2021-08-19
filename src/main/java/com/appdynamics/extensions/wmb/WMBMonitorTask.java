package com.appdynamics.extensions.wmb;


import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import org.slf4j.Logger;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.xml.bind.JAXBException;
import java.util.Map;

import static com.appdynamics.extensions.wmb.Util.convertToString;

class WMBMonitorTask implements AMonitorTaskRunnable {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(WMBMonitorTask.class);
    private static final String SEPARATOR = "|";
    private String displayName;

    /* metric prefix from the config.yaml to be applied to each metric path*/
    private String metricPrefix;

    /* a facade to report metricUtils to the machine agent.*/
    private MetricWriteHelper metricWriteHelper;

    private Map queueManagerConfig;

    private WMBMonitorTask(){
    }

    public void run() {
        try {
            logger.info("Executing a run of WMBMonitor.");
            displayName = convertToString(queueManagerConfig.get("name"),"");
            Connection conn = new ConnectionFactory().createConnection(queueManagerConfig);
            //subscribe subscribers
            StatsSubscription sub = new StatsSubscription(queueManagerConfig,metricWriteHelper,metricPrefix +SEPARATOR+displayName);
            sub.subscribe(conn);
            //start connection
            conn.start();

        } catch (JMSException e) {
            logger.error("Unable to connect to the queue manager with name={}",displayName,e);
        } catch (JAXBException e) {
            logger.error("Couldn't initialize the parser",e);
        }  catch (Exception e){
            logger.error("Something unforeseen has happened..",e);
        }
    }

    @Override
    public void onTaskComplete() {
        logger.info("WMB monitor run completed successfully.");
    }


    static class Builder {
        private WMBMonitorTask task = new WMBMonitorTask();

        Builder metricPrefix(String metricPrefix) {
            task.metricPrefix = metricPrefix;
            return this;
        }

        Builder metricWriter(MetricWriteHelper metricWriteHelper) {
            task.metricWriteHelper = metricWriteHelper;
            return this;
        }

        Builder manager(Map manager){
            task.queueManagerConfig = manager;
            return this;
        }

        WMBMonitorTask build() {
            return task;
        }
    }
}
