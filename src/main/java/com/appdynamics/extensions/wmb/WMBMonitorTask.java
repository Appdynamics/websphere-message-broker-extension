package com.appdynamics.extensions.wmb;


import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.wmb.flowstats.FlowStatistics;
import com.appdynamics.extensions.wmb.resourcestats.ResourceStatistics;

import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.xml.bind.JAXBException;
import java.util.Map;

import static com.appdynamics.extensions.wmb.Util.convertToString;

class WMBMonitorTask implements Runnable{

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WMBMonitorTask.class);

    private String displayName;

    /* metric prefix from the config.yaml to be applied to each metric path*/
    private String metricPrefix;

    /* a facade to report metrics to the machine agent.*/
    private MetricWriteHelper metricWriter;

    private Map queueManagerConfig;

    private WMBMonitorTask(){
    }

    public void run() {
        try {
            logger.info("Executing a run of WMBMonitor...");
            displayName = convertToString(queueManagerConfig.get("name"),"");
            MetricPrinter metricPrinter = new MetricPrinter(metricPrefix,displayName,metricWriter);
            Connection conn = new ConnectionFactory().createConnection(queueManagerConfig);
            ParserFactory parserFactory = new ParserFactory();
            XmlParser<ResourceStatistics> resourceStatParser = parserFactory.getResourceStatisticsParser();
            XmlParser<FlowStatistics> flowStatParser = parserFactory.getFlowStatisticsParser();
            //subscribe subscribers
            StatsProcessor processor = new StatsProcessor(queueManagerConfig, resourceStatParser, flowStatParser, metricPrinter);
            processor.subscribe(conn);
            //start connection
            conn.start();
            logger.info("Connection started. Wait Indefinitely.");
            //wait indefinitely
            Object obj = new Object();
            synchronized (obj) {
                obj.wait();
            }
        } catch (JMSException e) {
            logger.error("Unable to connect to the queue manager with name={}",displayName,e);
        } catch (JAXBException e) {
            logger.error("Couldn't initialize the parser",e);
        } catch (InterruptedException e) {
            logger.error("Indefinite wait is interrupted..",e);
        } catch (Exception e){
            logger.error("Something unforeseen has happened..",e);
        }
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

        WMBMonitorTask build() {
            return task;
        }
    }
}
