package com.appdynamics.extensions.wmb;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class StatsProcessorTest {

	ParserFactory parserFactory = new ParserFactory();
	MetricWriteHelper writer;
	StatsProcessor processor;
	ArgumentCaptor<String> metricPathCaptor;
	ArgumentCaptor<String> valueCaptor;
	TextMessage mockMsg;
	
	@Before
	public void setupHelperMocks () throws JAXBException {
		writer = mock(MetricWriteHelper.class);
		processor = null;		
		metricPathCaptor = ArgumentCaptor.forClass(String.class);
		valueCaptor = ArgumentCaptor.forClass(String.class);
		mockMsg = mock(TextMessage.class);
	}
	
	@Test
	public void canParseResourceStatsFromXmlMessageSuccessfully() throws Exception {
		// Prepare mock objects
		when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
		processor = getStatsProcessor("/conf/config.yml");
		
		// Feed resource stats to processor
		getResourceSubscriberFromProcessor(processor).onMessage(mockMsg);
		
		// See that metrics matching the input message are written
		verify(writer, atLeastOnce()).printMetric(metricPathCaptor.capture(), valueCaptor.capture(), anyString(),
				anyString(), anyString());
		
		List<String> metrics = metricPathCaptor.getAllValues();
		String prefix = "Custom Metrics|WMB|QMgr1|IB9NODE|default|Resource Statistics|";
		Assert.assertTrue(metrics.contains(prefix + "JVM|summary|Initial Memory In MB"));
		Assert.assertTrue(metrics.contains(prefix + "Parsers|[Administration]|Approx Mem KB"));
		Assert.assertTrue(valueCaptor.getAllValues().contains("72"));
	}
	
	@Test
	public void canParseFlowStatsFromXmlMessageSuccessfully() throws Exception {
		// Prepare mock objects
		when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
		processor = getStatsProcessor("/conf/config.yml");
		
		// Feed flow stats to processor
		getFlowSubscriberFromProcessor(processor).onMessage(mockMsg);		
		
		// See that metrics matching the input message are written
		verify(writer, atLeastOnce()).printMetric(metricPathCaptor.capture(), valueCaptor.capture(), anyString(),
				anyString(), anyString());
		
		List<String> metrics = metricPathCaptor.getAllValues();
		String prefix = "Custom Metrics|WMB|QMgr1|TESTNODE_wmb|default|Message Flow Statistics|Transformation_Map|Transformation_Map|";
		Assert.assertTrue(metrics.contains(prefix + "Elapsed Time Waiting For Input Message"));
		Assert.assertTrue(metrics.contains(prefix + "Nodes|HTTP Reply (WSReplyNode)|Number Of Output Terminals"));
		
		List<String> values = valueCaptor.getAllValues();
		Assert.assertTrue(values.contains("10001697"));
		Assert.assertTrue(values.contains("1289"));
	}

	@Test
	public void shouldNotThrowErrorWhenMessageIsNull() throws Exception {
		// Prepare mock objects
		when(mockMsg.getText()).thenReturn(null);
		processor = getStatsProcessor("/conf/config.yml");
		
		// Feed null message to processor
		getResourceSubscriberFromProcessor(processor).onMessage(mockMsg);		
		
		// See that no metrics are written
		verify(writer, never()).printMetric(metricPathCaptor.capture(), valueCaptor.capture(), anyString(), anyString(),
				anyString());
	}
	
	@Test
	public void shouldNotThrowErrorWhenSomeFieldsAreNotConfigured () throws Exception {
		// Prepare mock objects
		when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
		processor = getStatsProcessor("/conf/configWithNullFields.yml");
		
		// Feed flow stats to processor
		getFlowSubscriberFromProcessor(processor).onMessage(mockMsg);
		
		// See that metrics are written despite the config
		verify(writer, atLeastOnce()).printMetric(metricPathCaptor.capture(), valueCaptor.capture(), anyString(),
				anyString(), anyString());
	}
	
	@Test
	public void shouldNotThrowErrorWhenNoFlowMetricFieldsAreConfigured () throws Exception {
		// Prepare mock objects
		when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
		processor = getStatsProcessor("/conf/configWithNoFlowFieldsConfigured.yml");
		
		// Feed flow stats to processor
		getFlowSubscriberFromProcessor(processor).onMessage(mockMsg);
		
		// See that no metrics are written
		verify(writer, never()).printMetric(metricPathCaptor.capture(), valueCaptor.capture(), anyString(),
				anyString(), anyString());
	}

	private String getFileContents(String filepath) throws IOException {
		String filename = this.getClass().getResource(filepath).getFile();
		String text = Files.toString(new File(filename), Charsets.UTF_8);
		return text;
	}

	private StatsProcessor getStatsProcessor(String configPath) throws JAXBException {
		Map configMap = YmlReader
				.readFromFileAsMap(new File(this.getClass().getResource(configPath).getFile()));		
		Map qMgrConfig = ((List<Map>) configMap.get("queueManagers")).get(0);
		MetricPrinter printer = new MetricPrinter("Custom Metrics|WMB", "QMgr1", writer);
		StatsProcessor processor = new StatsProcessor(qMgrConfig, parserFactory.getResourceStatisticsParser(),
				parserFactory.getFlowStatisticsParser(), printer);
		return processor;
	}

	private MessageListener getResourceSubscriberFromProcessor(StatsProcessor processor) throws Exception {
		MessageListener listener = null;
		Field resourceSubsciberField;
		resourceSubsciberField = processor.getClass().getDeclaredField("resourceSubscriber");
		resourceSubsciberField.setAccessible(true);
		listener = (MessageListener) resourceSubsciberField.get(processor);
		return listener;
	}
	
	private MessageListener getFlowSubscriberFromProcessor(StatsProcessor processor) throws Exception {
		MessageListener listener = null;
		Field resourceSubsciberField;
		resourceSubsciberField = processor.getClass().getDeclaredField("flowSubscriber");
		resourceSubsciberField.setAccessible(true);
		listener = (MessageListener) resourceSubsciberField.get(processor);
		return listener;
	}
}