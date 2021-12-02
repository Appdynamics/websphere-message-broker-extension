package com.appdynamics.extensions.wmb;



import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.wmb.metricUtils.MetricPrinter;
import org.slf4j.Logger;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.xml.bind.JAXBException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.appdynamics.extensions.wmb.Util.convertToString;

class WMBMonitorTask implements AMonitorTaskRunnable{

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(WMBMonitorTask.class);
    private String displayName;
    private String metricPrefix;
    private Map queueManagerConfig;
    private MetricWriteHelper metricWriter;
    private CountDownLatch countDownLatch;

    @Override
    public void run() {
        try {
            logger.info("Executing a run of WMBMonitor.");
            displayName = convertToString(queueManagerConfig.get("name"),"");
            MetricPrinter metricPrinter = new MetricPrinter(metricPrefix,displayName,metricWriter);
            Connection conn = new ConnectionFactory().createConnection(queueManagerConfig);
            //subscribe subscribers
            StatsSubscription sub = new StatsSubscription(queueManagerConfig,metricPrinter);
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
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            logger.error("An unexpected error occurred while completing task",e);
        }
        logger.info("WMB monitor run completed successfully.");
    }

    static class Builder {
        private WMBMonitorTask task = new WMBMonitorTask();

        Builder metricPrefix(String metricPrefix) {
            task.metricPrefix = metricPrefix;
            return this;
        }

        Builder metricWriter(MetricWriteHelper metricWriter) {
            task.metricWriter = metricWriter;
            return this;
        }

        Builder manager(Map manager){
            task.queueManagerConfig = manager;
            return this;
        }

        Builder countdownLatch(CountDownLatch countDownLatch){
            task.countDownLatch = countDownLatch;
            return this;
        }

        WMBMonitorTask build() {
            return task;
        }
    }
}