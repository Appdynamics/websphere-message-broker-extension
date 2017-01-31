package com.appdynamics.extensions.wmb;

import static com.appdynamics.extensions.wmb.Util.convertToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.appdynamics.extensions.wmb.flowstats.FlowStatistics;
import com.appdynamics.extensions.wmb.flowstats.MessageFlow;
import com.appdynamics.extensions.wmb.flowstats.Node;
import com.appdynamics.extensions.wmb.flowstats.Terminal;
import com.appdynamics.extensions.wmb.flowstats.Thread;
import com.appdynamics.extensions.wmb.metrics.DefaultMetricProperties;
import com.appdynamics.extensions.wmb.metrics.Metric;
import com.appdynamics.extensions.wmb.metrics.MetricProperties;
import com.appdynamics.extensions.wmb.metrics.MetricValueTransformer;
import com.appdynamics.extensions.wmb.resourcestats.ResourceIdentifier;
import com.appdynamics.extensions.wmb.resourcestats.ResourceStatistics;
import com.appdynamics.extensions.wmb.resourcestats.ResourceType;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

public class StatsProcessor {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StatsProcessor.class);
	private static final String BROKER_LABEL = "brokerLabel";
	private static final String EXECUTION_GROUP_NAME = "executionGroupName";

	private Map config;
	private XmlParser<ResourceStatistics> resourceStatsParser;
	private XmlParser<FlowStatistics> flowStatsParser;
	private MetricPrinter printer;
	private final MetricValueTransformer valueTransformer = new MetricValueTransformer();
	private final ResourceSubscriber resourceSubscriber = new ResourceSubscriber();
	private final FlowSubscriber flowSubscriber = new FlowSubscriber();
	private final MetricProperties defaultMetricProps;
	private final MetricProperties sumAggregationMetricProps;

	public StatsProcessor(Map config, XmlParser resourceStatsParser, XmlParser flowStatsParser, MetricPrinter printer) {
		this.config = config;
		this.printer = printer;
		this.resourceStatsParser = resourceStatsParser;
		this.flowStatsParser = flowStatsParser;
		this.defaultMetricProps = new DefaultMetricProperties();
		this.sumAggregationMetricProps = new DefaultMetricProperties();
		this.sumAggregationMetricProps.setAggregationType(MetricWriter.METRIC_AGGREGATION_TYPE_SUM);
		this.sumAggregationMetricProps.setTimeRollupType(MetricWriter.METRIC_TIME_ROLLUP_TYPE_SUM);
	}

	public void subscribe(Connection conn) throws JMSException {
		Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Object metricsObj = config.get("metrics");
		if (metricsObj != null) {
			Map metrics = (Map) metricsObj;
			// Subscribe to resource statistics
			if (metrics.get("resourceStatistics") != null) {
				List<Map> resourceStats = (List<Map>) metrics.get("resourceStatistics");
				for (Map resourceStat : resourceStats) {
					Topic topic = session.createTopic(convertToString(resourceStat.get("name"), ""));
					TopicSubscriber topicSub = session.createDurableSubscriber(topic,
							convertToString(resourceStat.get("subscriberName"), ""));
					topicSub.setMessageListener(resourceSubscriber);
				}
				logger.info("Resource Statistic Subscribers are registered.");
			}
			// Subscribe to message flow statistics
			if (metrics.get("flowStatistics") != null) {
				List<Map> flowStats = (List<Map>) metrics.get("flowStatistics");
				for (Map flowStat : flowStats) {
					Topic topic = session.createTopic(convertToString(flowStat.get("name"), ""));
					TopicSubscriber topicSub = session.createDurableSubscriber(topic,
							convertToString(flowStat.get("subscriberName"), ""));
					topicSub.setMessageListener(flowSubscriber);
				}
				logger.info("Mesage Flow Statistic Subscribers are registered.");
			}
		}
	}

	private class ResourceSubscriber implements MessageListener {
		public void onMessage(Message message) {
			long startTime = System.currentTimeMillis();
			String text = null;
			try {
				text = getMessageString(message);
				if (text != null) {
					try {
						ResourceStatistics resourceStatistics = resourceStatsParser.parse(text);
						if (resourceStatistics != null) {
							List<Metric> metrics = buildResourceMetrics(resourceStatistics);
							printer.reportMetrics(metrics);
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
			logger.debug("Time taken to process one resource statistics message = {}",
					Long.toString(System.currentTimeMillis() - startTime));
		}
	}

	private class FlowSubscriber implements MessageListener {
		public void onMessage(Message message) {
			try {
				long startTime = System.currentTimeMillis();
				String text = null;
				try {
					text = getMessageString(message);
					if (text != null) {
						try {
							FlowStatistics flowStatistics = flowStatsParser.parse(text);
							if (flowStatistics != null) {
								List<Metric> metrics = buildFlowMetrics(flowStatistics);
								printer.reportMetrics(metrics);
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
	}

	private List<Metric> buildResourceMetrics(ResourceStatistics resourceStatistics) {
		List<Metric> metrics = new ArrayList<Metric>();
		if (resourceStatistics != null) {
			String brokerName = resourceStatistics.getAttributes().get(new QName(BROKER_LABEL));
			String executionGroupName = resourceStatistics.getAttributes().get(new QName(EXECUTION_GROUP_NAME));
			if (resourceStatistics.getResourceType() != null) {
				for (ResourceType resourceType : resourceStatistics.getResourceType()) {
					String resourceTypeName = resourceType.getName();
					if (resourceType.getResourceIdentifiers() != null) {
						for (ResourceIdentifier resourceIdentifier : resourceType.getResourceIdentifiers()) {
							String resourceIdName = resourceIdentifier.getName();
							for (QName key : resourceIdentifier.getAttributes().keySet()) {
								String value = resourceIdentifier.getAttributes().get(key);
								String metricPath = formMetricPath(Arrays.asList(brokerName, executionGroupName,
										"Resource Statistics", resourceTypeName, resourceIdName));
								MetricProperties metricProps = new DefaultMetricProperties();
								Metric metricPoint = createMetricPoint(metricPath, value, metricProps, key.toString());
								if (null != metricPoint) {
									metrics.add(metricPoint);
								}
							}
						}
					}
				}
			}
		}
		return metrics;
	}

	private List<Metric> buildFlowMetrics(FlowStatistics flowStatistics) {
		
		final List<Metric> metrics = new ArrayList<Metric>();
		
		if (null != flowStatistics) {
			
			Map enabledFields = (Map) config.get("flowMetricFields");
			Map derivedFields = (Map) config.get("derivedFlowMetricFields");
			List messageFlowFields = (List) enabledFields.get("messageFlowFields");
			List messageFlowDerivedFields = (List) derivedFields.get("messageFlowFields");
			MessageFlow messageFlow = flowStatistics.getMessageFlow();
			
			if (null != messageFlow) {
				
				String brokerName = messageFlow.getAttributes().get(new QName("BrokerLabel"));
				String executionGroupName = messageFlow.getAttributes().get(new QName("ExecutionGroupName"));
				String applicationName = messageFlow.getAttributes().get(new QName("ApplicationName"));
				String flowName = messageFlow.getAttributes().get(new QName("MessageFlowName"));
				// Metric Path:
				// <prefix>|<broker>|<execution group>|Message Flow Statistics|<application>|<message flow>|<metric name>
				// "Execution Group" is also called "Integration Server"
				String metricBasePath = formMetricPath(Arrays.asList(brokerName, executionGroupName,
						"Message Flow Statistics", applicationName, flowName));
				
				// Message Flow Metrics
				if (null != messageFlowFields) {
					
					for (Object field : messageFlowFields) {
						
						try {
							
							String metricName = (String) field;
							QName key = new QName(metricName);
							
							if (messageFlow.getAttributes().containsKey(key)) {
								
								String metricValue = messageFlow.getAttributes().get(key);								
								MetricProperties metricProps = metricName.startsWith("Total")
										|| metricName.equals("ElapsedTimeWaitingForInputMessage")
										|| metricName.equals("TimesMaximumNumberOfThreadsReached")
												? sumAggregationMetricProps : defaultMetricProps;
								Metric metricPoint = createMetricPoint(metricBasePath, metricValue, metricProps,
										metricName);
								
								if (metricPoint != null) {
									metrics.add(metricPoint);
								}
							}
						} catch (ClassCastException e) {
							logger.error("Configuration Error: Could not parse a \"Message Flow\" field");
						}
					}
				}
				
				// Derived Message Flow Metrics
				if (null != messageFlowDerivedFields) {
					
					for (Object field : messageFlowDerivedFields) {
						
						String metricName = null;
						
						try {
							
							metricName = (String) field;
							DerivedField derivedField = DerivedField.valueOf(metricName);
							Metric metricPoint = null;
							
							switch (derivedField) {
							
							case AverageElapsedTime:
								metricPoint = createFractionMetricPoint(messageFlow.getAttributes(), "TotalElapsedTime",
										"TotalInputMessages", "AverageElapsedTime", metricBasePath);
								break;
							case AverageCPUTime:
								metricPoint = createFractionMetricPoint(messageFlow.getAttributes(), "TotalCPUTime",
										"TotalInputMessages", "AverageCPUTime", metricBasePath);
								break;
							case AverageCPUTimeWaitingForInputMessage:
								metricPoint = createFractionMetricPoint(messageFlow.getAttributes(),
										"CPUTimeWaitingForInputMessage", "TotalInputMessages",
										"AverageCPUTimeWaitingForInputMessage", metricBasePath);
								break;
							case AverageElapsedTimeWaitingForInputMessage:
								metricPoint = createFractionMetricPoint(messageFlow.getAttributes(),
										"ElapsedTimeWaitingForInputMessage", "TotalInputMessages",
										"AverageElapsedTimeWaitingForInputMessage", metricBasePath);
								break;
							case AverageSizeOfInputMessages:
								metricPoint = createFractionMetricPoint(messageFlow.getAttributes(),
										"TotalSizeOfInputMessages", "TotalInputMessages", "AverageSizeOfInputMessages",
										metricBasePath);
								break;
							default:
								break;
							}
							
							if (null != metricPoint) {
								metrics.add(metricPoint);
							}
						} catch (ClassCastException e) {
							logger.error("Configuration Error: Could not parse a derived \"Message Flow\" field");
						} catch (IllegalArgumentException e) {
							logger.error("Configuration Error: Derived \"Message Flow\" field \"" + metricName
									+ "\" is invalid");
						}
					}
				}
				
				// Thread Metrics
				List<Thread> threads = flowStatistics.getThreadStatistics();
				List threadFields = (List) enabledFields.get("threadFields");
				List threadDerivedFields = (List) derivedFields.get("threadFields");
				
				if (null != threads && null != threadFields) {
					
					for (Thread thread : threads) {
						
						String threadNumber = thread.getAttributes().get(new QName("Number"));
						String metricPath = metricBasePath + "|Threads|" + threadNumber;
						
						if (null != threadFields) {
							
							for (Object field : threadFields) {
								
								try {
									
									String metricName = (String) field;
									QName key = new QName(metricName);
									
									if (thread.getAttributes().containsKey(key)) {
										
										String metricValue = thread.getAttributes().get(key);
										MetricProperties metricProps = key.toString().startsWith("Total")
												|| key.toString().equals("ElapsedTimeWaitingForInputMessage")
														? sumAggregationMetricProps : defaultMetricProps;										
										Metric metricPoint = createMetricPoint(metricPath, metricValue, metricProps,
												metricName);
										
										if (metricPoint != null) {
											metrics.add(metricPoint);
										}
									}
								} catch (ClassCastException e) {
									logger.error("Configuration Error: Could not parse a \"Thread\" field");
								}
							}
						}
						
						if (null != threadDerivedFields) {
							
							for (Object field : threadDerivedFields) {
								
								String metricName = null;
								
								try {
									
									metricName = (String) field;
									DerivedField derivedField = DerivedField.valueOf(metricName);
									Metric metricPoint = null;
									
									switch (derivedField) {
									
									case AverageElapsedTime:
										metricPoint = createFractionMetricPoint(thread.getAttributes(),
												"TotalElapsedTime", "TotalNumberOfInputMessages", "AverageElapsedTime",
												metricPath);
										break;
									case AverageCPUTime:
										metricPoint = createFractionMetricPoint(thread.getAttributes(), "TotalCPUTime",
												"TotalNumberOfInputMessages", "AverageCPUTime", metricPath);
										break;
									case AverageCPUTimeWaitingForInputMessage:
										metricPoint = createFractionMetricPoint(thread.getAttributes(),
												"CPUTimeWaitingForInputMessage", "TotalNumberOfInputMessages",
												"AverageCPUTimeWaitingForInputMessage", metricPath);
										break;
									case AverageElapsedTimeWaitingForInputMessage:
										metricPoint = createFractionMetricPoint(thread.getAttributes(),
												"ElapsedTimeWaitingForInputMessage", "TotalNumberOfInputMessages",
												"AverageElapsedTimeWaitingForInputMessage", metricPath);
										break;
									case AverageSizeOfInputMessages:
										metricPoint = createFractionMetricPoint(thread.getAttributes(),
												"TotalSizeOfInputMessages", "TotalNumberOfInputMessages",
												"AverageSizeOfInputMessages", metricPath);
										break;
									default:
										break;
									}
									
									if (null != metricPoint) {
										metrics.add(metricPoint);
									}
								} catch (ClassCastException e) {
									logger.error("Configuration Error: Could not parse a derived \"Thread\" field");
								} catch (IllegalArgumentException e) {
									logger.error("Configuration Error: Derived \"Thread\" field \"" + metricName
											+ "\" is invalid");
								}
							}
						}
					}
				}
				
				// Node Metrics
				List<Node> nodes = flowStatistics.getNodeStatistics();
				List nodeFields = (List) enabledFields.get("nodeFields");
				List nodeDerivedFields = (List) derivedFields.get("nodeFields");
				
				if (null != nodes && null != nodeFields) {
					
					for (Node node : nodes) {
						
						String nodeLabel = node.getAttributes().get(new QName("Label"));
						String nodeType = node.getAttributes().get(new QName("Type"));
						String metricPath = metricBasePath + "|Nodes|" + nodeLabel + " (" + nodeType + ")";
						
						if (null != nodeFields) {
							
							for (Object field : nodeFields) {
								
								try {
									
									String metricName = (String) field;
									QName key = new QName(metricName);
									
									if (node.getAttributes().containsKey(key)) {
										
										String metricValue = node.getAttributes().get(key);										
										MetricProperties metricProps = key.toString().startsWith("Total")
												|| key.toString().equals("CountOfInvocations")
														? sumAggregationMetricProps : defaultMetricProps;
										Metric metricPoint = createMetricPoint(metricPath, metricValue, metricProps,
												key.toString());
										
										if (metricPoint != null) {
											metrics.add(metricPoint);
										}
									}
								} catch (ClassCastException e) {
									logger.error("Configuration Error: Could not parse a \"Node\" field");
								}
							}
						}
						
						if (null != nodeDerivedFields) {
							
							for (Object field : nodeDerivedFields) {
								
								String metricName = null;
								
								try {
									
									metricName = (String) field;
									DerivedField derivedField = DerivedField.valueOf(metricName);
									Metric metricPoint = null;
									
									switch (derivedField) {
									
									case AverageElapsedTime:
										metricPoint = createFractionMetricPoint(node.getAttributes(),
												"TotalElapsedTime", "CountOfInvocations", "AverageElapsedTime",
												metricPath);
										break;
									case AverageCPUTime:
										metricPoint = createFractionMetricPoint(node.getAttributes(), "TotalCPUTime",
												"CountOfInvocations", "AverageCPUTime", metricPath);
										break;
									default:
										break;
									}
									
									if (null != metricPoint) {
										metrics.add(metricPoint);
									}
								} catch (ClassCastException e) {
									logger.error("Configuration Error: Could not parse a derived \"Node\" field");
								} catch (IllegalArgumentException e) {
									logger.error("Configuration Error: Derived \"Node\" field \"" + metricName
											+ "\" is invalid");
								}
							}
						}
						
						// Terminal Metrics, only relevant attribute is "CountOfInvocations"
						List<Terminal> terminals = node.getTerminalStatistics();
						List terminalFields = (List) enabledFields.get("terminalFields");
						
						if (null != terminals && null != terminalFields
								&& terminalFields.contains("CountOfInvocations")) {
							
							for (Terminal terminal : terminals) {
								
								String terminalLabel = terminal.getAttributes().get(new QName("Label"));
								String terminalType = terminal.getAttributes().get(new QName("Type"));
								String metricValue = terminal.getAttributes().get("CountOfInvocations");
								
								metricPath = metricBasePath + "|Nodes|Terminals|" + terminalLabel + " (" + terminalType + ")";
								Metric metricPoint = createMetricPoint(metricPath, metricValue,
										sumAggregationMetricProps, "CountOfInvocations");
								metrics.add(metricPoint);
							}
						}
					}
				}
			}
		}
		
		return metrics;
	}

	private Metric createMetricPoint(String path, String value, MetricProperties properties, String name) {
		BigDecimal decimalValue = valueTransformer.transform(path, value, properties);
		String processedName = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(name), " ");
		if (decimalValue != null) {
			Metric m = new Metric();
			m.setMetricName(processedName);
			m.setMetricKey(path + "|" + processedName);
			m.setProperties(properties);
			m.setMetricValue(decimalValue);
			return m;
		} else {
			return null;
		}
	}

	private Metric createFractionMetricPoint(Map<QName, String> attributes, String numeratorName,
			String denominatorName, String metricName, String metricBasePath) {
		Long numerator = new Long(attributes.get(new QName(numeratorName)));
		Long denominator = new Long(attributes.get(new QName(denominatorName)));
		Long factionValue;
		if (denominator == 0) {
			return null;
		} else {
			factionValue = numerator / denominator;
		}
		return createMetricPoint(metricBasePath, factionValue.toString(), sumAggregationMetricProps, metricName);
	}

	private String formMetricPath(List<String> pathElements) {
		StringBuilder metricBuilder = new StringBuilder();
		if (pathElements.get(0) != null) {
			metricBuilder.append(pathElements.get(0));
		}
		for (int i = 1; i < pathElements.size(); i++) {
			String pathElement = pathElements.get(i);
			if (pathElement != null) {
				metricBuilder.append("|" + pathElement);
			}
		}
		return metricBuilder.toString();
	}

	private String getMessageString(Message message) throws JMSException {
		if (message != null) {
			if (message instanceof TextMessage) {
				TextMessage tm = (TextMessage) message;
				return tm.getText();
			} else if (message instanceof BytesMessage) {
				BytesMessage bm = (BytesMessage) message;
				byte data[] = new byte[(int) bm.getBodyLength()];
				bm.readBytes(data);
				return new String(data);
			}
		}
		throw new JMSException("Message is not of TextMessage/BytesMessage.");
	}

	private enum DerivedField {
		AverageElapsedTime, AverageCPUTime, AverageCPUTimeWaitingForInputMessage, AverageElapsedTimeWaitingForInputMessage, AverageSizeOfInputMessages
	}
}
