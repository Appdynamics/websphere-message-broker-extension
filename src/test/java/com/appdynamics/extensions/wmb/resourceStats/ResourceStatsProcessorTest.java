package com.appdynamics.extensions.wmb.resourceStats;


import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.wmb.ParserFactory;
import com.appdynamics.extensions.wmb.resourcestats.ResourceStatistics;
import com.appdynamics.extensions.wmb.resourcestats.ResourceStatsProcessor;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

public class ResourceStatsProcessorTest {

    ParserFactory parserFactory = new ParserFactory();

    @Test
    public void canParseXmlMessageSuccessfully() throws IOException, JMSException, JAXBException {
        Map<String,String> metricMap = Maps.newHashMap();
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/config.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            metricMap.put(m.getMetricPath(),m.getMetricValue());
        }
        Assert.assertTrue(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Resource Statistics|JVM|summary|InitialMemoryInMB"));
        Assert.assertTrue(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Resource Statistics|Sockets|summary|TotalDataSent_KB"));
        Assert.assertTrue(metricMap.containsValue("256"));
    }

    @Test
    public void shouldNotThrowErrorWhenMessageIsNull() throws IOException, JMSException, JAXBException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/config.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(null);
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer,never()).transformAndPrintMetrics(pathCaptor.capture());
    }

    @Test
    public void shouldNotEmitResourceStatsWhenResourceStatsAreNotConfigured() throws IOException, JMSException, JAXBException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/configWithNoResourceStatsConfiguration.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer).transformAndPrintMetrics(pathCaptor.capture());
        List<Metric> metricList = (List<Metric>)pathCaptor.getValue();
        Assert.assertTrue(metricList.isEmpty());
    }


    @Test
    public void shouldEmitResourceStatsIncludedWhenIncludeClausesAreConfigured() throws IOException, JMSException, JAXBException {
        Map<String,String> metricMap = Maps.newHashMap();
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/configWithSomeResourceStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            metricMap.put(m.getMetricPath(),m.getMetricValue());
        }
        Assert.assertTrue(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Resource Statistics|JVM|summary|InitialMemoryInMB"));
        Assert.assertTrue(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Resource Statistics|JVM|Garbage Collection - MarkSweepCompact|CumulativeNumberOfGCCollections"));
        Assert.assertFalse(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Resource Statistics|Sockets|summary|TotalDataSent_KB"));
        Assert.assertTrue(metricMap.containsValue("256"));
        Assert.assertFalse(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Resource Statistics|TCPIPClientNodes|summary|BytesReceived"));
    }

    @Test
    public void shouldEmitMetricAliasWhenMetricsAreIncluded() throws JAXBException, JMSException, IOException {
        Map<String,String> metricMap = Maps.newHashMap();
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/configWithSomeResourceStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            metricMap.put(m.getMetricPath(),m.getMetricValue());
        }
        Assert.assertTrue(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Resource Statistics|JDBCConnectionPools|summary|This is max pool"));
    }

    @Test
    public void shouldEmitRightMetricTypeWhenMetricTypeIsConfigured() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/configWithSomeResourceStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            if(m.getMetricPath().equals("Custom Metrics|WMB|QMgr1|default|Resource Statistics|JDBCConnectionPools|summary|CumulativeTimedOutRequests")){
                Assert.assertTrue(m.getMetricValue().equals("1000000"));
                Assert.assertTrue(m.getAggregationType().equals("OBSERVATION"));
                Assert.assertTrue(m.getTimeRollUpType().equals("CURRENT"));
                Assert.assertTrue(m.getClusterRollUpType().equals("INDIVIDUAL"));
            }
        }
    }


    @Test
    public void shouldEmitRightMultiplierValueWhenMultiplierIsConfigured() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/configWithSomeResourceStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            if(m.getMetricPath().equals("Custom Metrics|WMB|QMgr1|default|Resource Statistics|JDBCConnectionPools|summary|CumulativeTimedOutRequests")){
                Assert.assertTrue(m.getMetricProperties().getMultiplier().equals(new BigDecimal("0.5")));
            }
        }
    }


    @Test
    public void shouldEmitDeltaMetricValueWhenDeltaIsConfigured() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/configWithSomeResourceStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
        processor.onMessage(mockMsg);
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            if(m.getMetricPath().equals("Custom Metrics|WMB|QMgr1|default|Resource Statistics|JDBCConnectionPools|summary|CumulativeDelayedRequests")){
                Assert.assertTrue(m.getMetricProperties().getDelta());
            }
        }
    }

    private String getFileContents(String filepath) throws IOException {
        String filename = this.getClass().getResource(filepath).getFile();
        String text = Files.toString(new File(filename), Charsets.UTF_8);
        return text;
    }

    private ResourceStatsProcessor<ResourceStatistics> getResourceStatProcessor(String configFile, MetricWriteHelper writer) throws JAXBException {
        Map configMap = YmlReader.readFromFileAsMap(new File(this.getClass().getResource(configFile).getFile()));
        List<Map> qMgrs = (List<Map>)configMap.get("queueManagers");
        Map qMgrConfig = qMgrs.get(0);
        ResourceStatsProcessor<ResourceStatistics> processor = new ResourceStatsProcessor<ResourceStatistics>(qMgrConfig,parserFactory.getResourceStatisticsParser(),writer,"Custom Metrics|WMB|QMgr1");
        return processor;
    }

}
