package com.appdynamics.extensions.wmb.flowstats;


import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.wmb.StatsProcessor;
import com.appdynamics.extensions.wmb.XmlParser;
import org.slf4j.Logger;

import javax.jms.*;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.wmb.Util.convertToString;
import static com.appdynamics.extensions.wmb.Util.join;

public class FlowStatsProcessor<T> extends StatsProcessor<T> implements MessageListener {

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(FlowStatsProcessor.class);
    private static final String SEPARATOR = "|";
    private static final String EXECUTION_GROUP_NAME = "ExecutionGroupName";

    private enum DerivedMetric {
        AverageElapsedTime, AverageCPUTime, AverageCPUTimeWaitingForInputMessage, AverageElapsedTimeWaitingForInputMessage, AverageSizeOfInputMessages
    }

    public FlowStatsProcessor(Map config, XmlParser<T> parser, MetricWriteHelper metricWriteHelper, String metricPrefix) {
        super(config, parser, metricWriteHelper, metricPrefix);
        this.metricPropsHolder = buildMetricProperties("metrics", "flowStatistics");
        Map<String, Map> derivedPropsHolder = buildMetricProperties("derivedMetrics", "flowStatistics");
        this.metricPropsHolder.putAll(derivedPropsHolder);
    }


    public void subscribe(Session session) throws JMSException {
        if (config.get("flowStatisticsSubscribers") != null) {
            List<Map> resourceSubscribers = (List<Map>) config.get("flowStatisticsSubscribers");
            for (Map resourceSub : resourceSubscribers) {
                Topic topic = session.createTopic(convertToString(resourceSub.get("topic"), ""));
                TopicSubscriber topicSub = session.createDurableSubscriber(topic,
                        convertToString(resourceSub.get("subscriberName"), ""));
                topicSub.setMessageListener(this);
            }
            logger.info("Message Flow Statistic Subscribers are registered.");
        }
    }

    /**
     * This method is called every time when there is a message on the topic.
     *
     * @param message
     */
    public void onMessage(Message message) {
        try {
            long startTime = System.currentTimeMillis();
            String text = null;
            try {
                text = getMessageString(message);
                if (text != null) {
                    try {
                        T flowStatistics = parser.parse(text);
                        if (flowStatistics != null) {
                            List<Metric> metrics = buildMetrics(flowStatistics);
                            metricWriteHelper.transformAndPrintMetrics(metrics);
                        }
                    } catch (JAXBException e) {
                        logger.error("Unable to unmarshal XML message {}", text, e);
                    }
                } else {
                    logger.error("Message received is null.");
                }
            } catch (JMSException e) {
                logger.error("Unable to process message {}", e);
            }
            logger.debug("Time taken to process one flow statistics message = {}",
                    Long.toString(System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            logger.error("Something unforeseen has happened while processing a flow statistics message.", e);
        }
    }

    protected List<Metric> buildMetrics(T stats) {
        FlowStatistics flowStatistics = (FlowStatistics) stats;
        List<Metric> metrics = new ArrayList<Metric>();
        if (flowStatistics != null && flowStatistics.getMessageFlow() != null) {

            //message flow statistics
            MessageFlow messageFlow = flowStatistics.getMessageFlow();
            String executionGroupName = messageFlow.getAttributes().get(new QName(EXECUTION_GROUP_NAME));
            for (QName key : messageFlow.getAttributes().keySet()) {
                String messageFlowMetric = join(SEPARATOR, "MessageFlow", key.toString());
                Map flowMetricProperties = metricPropsHolder.get(messageFlowMetric);
                if (flowMetricProperties != null) {
                    String value = messageFlow.getAttributes().get(key);
                    String metricPath = join(SEPARATOR, executionGroupName,
                            "Flow Statistics", "MessageFlow");
                    Metric metric = new Metric(key.toString(), value, metricPrefix + SEPARATOR + metricPath + SEPARATOR + flowMetricProperties.get("alias"), flowMetricProperties);
                    metrics.add(metric);
                }
            }
            derivedMetrics(metrics, messageFlow, executionGroupName);

            // thread statistics
            List<Thread> threadStats = flowStatistics.getThreadStatistics();
            if (threadStats != null) {
                for (Thread t : threadStats) {
                    String threadNumber = t.getAttributes().get(new QName("Number"));
                    for (QName key : t.getAttributes().keySet()) {
                        String threadFlowStatistics = join(SEPARATOR, "Threads", "ThreadStatistics", key.toString());
                        Map flowMetricProperties = metricPropsHolder.get(threadFlowStatistics);
                        if (flowMetricProperties != null) {
                            String metricPath = join(SEPARATOR, executionGroupName, "Flow Statistics", "Threads", "ThreadStatistics", threadNumber);
                            String value = t.getAttributes().get(key);
                            Metric metric = new Metric(key.toString(),value,metricPrefix+SEPARATOR+metricPath+SEPARATOR+flowMetricProperties.get("alias"),flowMetricProperties);
                            metrics.add(metric);
                        }
                    }

                    //derived metrics - thread statistics
                    derivedMetrics(metrics, executionGroupName, t, threadNumber);
                }
                //adding a metric for inserting the count of threads
                Metric metric = new Metric("Number",Integer.toString(threadStats.size()),metricPrefix+SEPARATOR+join(SEPARATOR, executionGroupName, "Flow Statistics", "Threads")+SEPARATOR+"Number");
                metrics.add(metric);
            }

            //node statistics
            List<Node> nodeStats = flowStatistics.getNodeStatistics();
            if (nodeStats != null) {
                for (Node n : nodeStats) {
                    String nodeLabel = n.getAttributes().get(new QName("Label"));
                    for (QName key : n.getAttributes().keySet()) {
                        String nodeFlowStatistics = join(SEPARATOR, "Nodes", "NodeStatistics", key.toString());
                        Map flowMetricProperties = metricPropsHolder.get(nodeFlowStatistics);
                        if (flowMetricProperties != null) {
                            String metricPath = join(SEPARATOR, executionGroupName, "Flow Statistics", "Nodes", "NodeStatistics", nodeLabel);
                            String value = n.getAttributes().get(key);
                            Metric metric = new Metric(key.toString(),value,metricPrefix+SEPARATOR+metricPath+flowMetricProperties.get("alias"),flowMetricProperties);
                            metrics.add(metric);
                        }
                    }
                    //terminal statistics
                    List<Terminal> terminalStats = n.getTerminalStatistics();
                    if (terminalStats != null) {
                        for (Terminal t : n.getTerminalStatistics()) {
                            String terminalLabel = t.getAttributes().get(new QName("Label"));
                            for (QName key : t.getAttributes().keySet()) {
                                String terminalFlowStatistics = join(SEPARATOR, "Nodes", "NodeStatistics", "TerminalStatistics", key.toString());
                                Map flowMetricProperties = metricPropsHolder.get(terminalFlowStatistics);
                                if (flowMetricProperties != null) {
                                    String metricPath = join(SEPARATOR, executionGroupName, "Flow Statistics", "Nodes", "NodeStatistics", nodeLabel, "TerminalStatistics", terminalLabel);
                                    String value = t.getAttributes().get(key);
                                    Metric metric = new Metric(key.toString(),value,metricPrefix+SEPARATOR+metricPath+SEPARATOR+flowMetricProperties.get("alias"),flowMetricProperties);
                                    metrics.add(metric);
                                }
                            }
                        }
                    }

                    derivedMetrics(metrics, executionGroupName, n, nodeLabel);
                }
                //adding a metric for inserting the count of threads
                Metric metric = new Metric("Number",Integer.toString(nodeStats.size()),metricPrefix+SEPARATOR+join(SEPARATOR, executionGroupName, "Flow Statistics", "Nodes")+SEPARATOR+"Number");
                metrics.add(metric);
            }
        }
        return metrics;
    }

    private void derivedMetrics(List<Metric> metrics, String executionGroupName, Node n, String nodeLabel) {
        for (DerivedMetric metric : DerivedMetric.values()) {
            String nodeStatistics = join(SEPARATOR, "Nodes", "NodeStatistics", metric.toString());
            Map flowMetricProperties = metricPropsHolder.get(nodeStatistics);
            try {
                if (flowMetricProperties != null) {
                    Metric metricPoint = null;
                    String metricPath = join(SEPARATOR, executionGroupName, "Flow Statistics", "Nodes", "NodeStatistics", nodeLabel);
                    switch (metric) {
                        case AverageElapsedTime:
                            metricPoint = createFractionMetricPoint(n.getAttributes(),
                                    "TotalElapsedTime", "CountOfInvocations", metric.toString(),
                                    metricPath, flowMetricProperties);
                            break;
                        case AverageCPUTime:
                            metricPoint = createFractionMetricPoint(n.getAttributes(), "TotalCPUTime",
                                    "CountOfInvocations", metric.toString(), metricPath, flowMetricProperties);
                            break;

                        default:
                            break;
                    }
                    if (metricPoint != null) {
                        metrics.add(metricPoint);
                    }
                }
            } catch (ClassCastException e) {
                logger.error("Configuration Error: Could not parse a derived \"Node\" field");
            } catch (IllegalArgumentException e) {
                logger.error("Configuration Error: Derived \"Node\" field \"" + metric.toString()
                        + "\" is invalid");
            }
        }
    }

    private void derivedMetrics(List<Metric> metrics, String executionGroupName, Thread t, String threadNumber) {
        for (DerivedMetric metric : DerivedMetric.values()) {
            String threadFlowStatistics = join(SEPARATOR, "Threads", "ThreadStatistics", metric.toString());
            Map flowMetricProperties = metricPropsHolder.get(threadFlowStatistics);
            try {
                if (flowMetricProperties != null) {
                    Metric metricPoint = null;
                    String metricPath = join(SEPARATOR, executionGroupName, "Flow Statistics", "Threads", "ThreadStatistics", threadNumber);
                    switch (metric) {
                        case AverageElapsedTime:
                            metricPoint = createFractionMetricPoint(t.getAttributes(),
                                    "TotalElapsedTime", "TotalNumberOfInputMessages", metric.toString(),
                                    metricPath, flowMetricProperties);
                            break;
                        case AverageCPUTime:
                            metricPoint = createFractionMetricPoint(t.getAttributes(), "TotalCPUTime",
                                    "TotalNumberOfInputMessages", metric.toString(), metricPath, flowMetricProperties);
                            break;
                        case AverageCPUTimeWaitingForInputMessage:
                            metricPoint = createFractionMetricPoint(t.getAttributes(),
                                    "CPUTimeWaitingForInputMessage", "TotalNumberOfInputMessages",
                                    metric.toString(), metricPath, flowMetricProperties);
                            break;

                        case AverageSizeOfInputMessages:
                            metricPoint = createFractionMetricPoint(t.getAttributes(),
                                    "TotalSizeOfInputMessages", "TotalNumberOfInputMessages",
                                    metric.toString(), metricPath, flowMetricProperties);
                            break;

                        case AverageElapsedTimeWaitingForInputMessage:
                            metricPoint = createFractionMetricPoint(t.getAttributes(), "ElapsedTimeWaitingForInputMessage", "TotalNumberOfInputMessages",
                                    metric.toString(), metricPath, flowMetricProperties);
                            break;
                        default:
                            break;
                    }
                    if (metricPoint != null) {
                        metrics.add(metricPoint);
                    }
                }
            } catch (ClassCastException e) {
                logger.error("Configuration Error: Could not parse a derived \"Threads\" field");
            } catch (IllegalArgumentException e) {
                logger.error("Configuration Error: Derived \"Threads\" field \"" + metric.toString()
                        + "\" is invalid");
            }
        }
    }


    private void derivedMetrics(List<Metric> metrics, MessageFlow messageFlow, String executionGroupName) {
        //derived metrics - message flow
        for (DerivedMetric metric : DerivedMetric.values()) {
            String messageFlowMetric = join(SEPARATOR, "MessageFlow", metric.toString());
            Map flowMetricProperties = metricPropsHolder.get(messageFlowMetric);
            try {
                if (flowMetricProperties != null) {
                    Metric metricPoint = null;
                    String metricPath = join(SEPARATOR, executionGroupName, "Flow Statistics", "MessageFlow");
                    switch (metric) {
                        case AverageElapsedTime:
                            metricPoint = createFractionMetricPoint(messageFlow.getAttributes(),
                                    "TotalElapsedTime", "TotalInputMessages", metric.toString(),
                                    metricPath, flowMetricProperties);
                            break;
                        case AverageCPUTime:
                            metricPoint = createFractionMetricPoint(messageFlow.getAttributes(), "TotalCPUTime",
                                    "TotalInputMessages", metric.toString(), metricPath, flowMetricProperties);
                            break;
                        case AverageCPUTimeWaitingForInputMessage:
                            metricPoint = createFractionMetricPoint(messageFlow.getAttributes(),
                                    "CPUTimeWaitingForInputMessage", "TotalInputMessages",
                                    metric.toString(), metricPath, flowMetricProperties);
                            break;

                        case AverageSizeOfInputMessages:
                            metricPoint = createFractionMetricPoint(messageFlow.getAttributes(),
                                    "TotalSizeOfInputMessages", "TotalInputMessages",
                                    metric.toString(), metricPath, flowMetricProperties);
                            break;
                        default:
                            break;
                    }
                    if (metricPoint != null) {
                        metrics.add(metricPoint);
                    }
                }
            } catch (ClassCastException e) {
                logger.error("Configuration Error: Could not parse a derived \"MessageFlow\" field");
            } catch (IllegalArgumentException e) {
                logger.error("Configuration Error: Derived \"MessageFlow\" field \"" + metric.toString()
                        + "\" is invalid");
            }
        }
    }


    //private Metric createFractionMetricPoint(Map<QName, String> attributes, String numeratorName, String denominatorName, String metricName, String metricPath, MetricProperties metricProperties) {
    private Metric createFractionMetricPoint(Map<QName, String> attributes, String numeratorName, String denominatorName, String metricName, String metricPath, Map metricProperties) {
        BigDecimal numerator = new BigDecimal(attributes.get(new QName(numeratorName)));
        BigDecimal denominator = new BigDecimal(attributes.get(new QName(denominatorName)));
        BigDecimal fractionValue;
        if (denominator.equals(BigDecimal.ZERO)) {
            return null;
        } else {
            fractionValue = numerator.divide(denominator, 0, RoundingMode.HALF_UP);
        }
        return new Metric(metricName,fractionValue.toString(),metricPrefix+SEPARATOR+metricPath+SEPARATOR+metricProperties.get("alias"),metricProperties);
    }


}
