package com.appdynamics.extensions.wmb.flowstats;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.namespace.QName;

@XmlAccessorType(XmlAccessType.FIELD)
public class Node {

	@XmlAnyAttribute
	private Map<QName, String> attributes;

	@XmlElement(name = "TerminalStatistics")
	private List<Terminal> terminalStatistics;

	public Map<QName, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<QName, String> attributes) {
		this.attributes = attributes;
	}

	public List<Terminal> getTerminalStatistics() {
		return terminalStatistics;
	}

	public void setTerminalStatistics(List<Terminal> terminalStatistics) {
		this.terminalStatistics = terminalStatistics;
	}
}