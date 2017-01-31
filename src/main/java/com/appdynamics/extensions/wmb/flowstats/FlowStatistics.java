package com.appdynamics.extensions.wmb.flowstats;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

@XmlRootElement(name = "WMQIStatisticsAccounting")
@XmlAccessorType(XmlAccessType.FIELD)
public class FlowStatistics {

	@XmlAnyAttribute
	private Map<QName, String> attributes;

	@XmlElement(name = "MessageFlow")
	private MessageFlow messageFlow;

	@XmlElement(name = "ThreadStatistics")
	@XmlElementWrapper(name = "Threads")
	private List<Thread> threadStatistics;

	@XmlElement(name = "NodeStatistics")
	@XmlElementWrapper(name = "Nodes")
	private List<Node> nodeStatistics;

	public Map<QName, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<QName, String> attributes) {
		this.attributes = attributes;
	}

	public MessageFlow getMessageFlow() {
		return messageFlow;
	}

	public void setMessageFlow(MessageFlow messageFlow) {
		this.messageFlow = messageFlow;
	}

	public List<Thread> getThreadStatistics() {
		return threadStatistics;
	}

	public void setThreadStatistics(List<Thread> threadStatistics) {
		this.threadStatistics = threadStatistics;
	}

	public List<Node> getNodeStatistics() {
		return nodeStatistics;
	}

	public void setNodeStatistics(List<Node> nodeStatistics) {
		this.nodeStatistics = nodeStatistics;
	}
}