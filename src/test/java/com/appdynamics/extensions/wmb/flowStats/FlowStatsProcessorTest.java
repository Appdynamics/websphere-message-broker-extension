package com.appdynamics.extensions.wmb.flowStats;


import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.wmb.ParserFactory;
import com.appdynamics.extensions.wmb.flowstats.FlowStatistics;
import com.appdynamics.extensions.wmb.flowstats.FlowStatsProcessor;
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

public class FlowStatsProcessorTest {

    ParserFactory parserFactory = new ParserFactory();

    @Test
    public void canParseXmlMessageSuccessfully() throws IOException, JMSException, JAXBException {
        Map<String,String> metricMap = Maps.newHashMap();
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/config.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m : (List<Metric>)pathCaptor.getValue()){
            metricMap.put(m.getMetricPath(),m.getMetricValue());
        }
        Assert.assertTrue(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|TotalElapsedTime"));
        Assert.assertTrue(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Threads|ThreadStatistics|16375|TotalSizeOfInputMessages"));
        Assert.assertTrue(metricMap.containsValue("1289"));
    }

    @Test
    public void shouldNotThrowErrorWhenMessageIsNull() throws IOException, JMSException, JAXBException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/config.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(null);
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer,never()).transformAndPrintMetrics(pathCaptor.capture());
    }

    @Test
    public void shouldCalculateCorrectDerivedMetricWhenDerivedMetricConfigured() throws IOException, JMSException, JAXBException {
        Map<String,String> metricMap = Maps.newHashMap();
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/config.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            metricMap.put(m.getMetricPath(),m.getMetricValue());
        }
        Assert.assertTrue(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|AverageCPUTimeWaitingForInputMessage"));
        Assert.assertTrue(metricMap.get("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|AverageCPUTimeWaitingForInputMessage").equals("26"));
        Assert.assertTrue(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Threads|ThreadStatistics|16375|AverageElapsedTimeWaitingForInputMessage"));
        Assert.assertTrue(metricMap.get("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Threads|ThreadStatistics|16375|AverageElapsedTimeWaitingForInputMessage").equals("200034"));
        Assert.assertTrue(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Nodes|NodeStatistics|HTTP Input|AverageCPUTime"));
        Assert.assertTrue(metricMap.get("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Nodes|NodeStatistics|HTTP Input|AverageCPUTime").equals("2"));
    }


    @Test
    public void shouldNotEmitDerivedStatsWhenDerivedAreNotConfigured() throws IOException, JMSException, JAXBException {
        Map<String,String> metricMap = Maps.newHashMap();
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithNoDerivedMetrics.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            metricMap.put(m.getMetricPath(),m.getMetricValue());
        }
        Assert.assertFalse(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Threads|ThreadStatistics|16375|AverageElapsedTimeWaitingForInputMessage"));
        Assert.assertFalse(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Nodes|NodeStatistics|HTTP Input|AverageCPUTime"));
    }

    @Test
    public void shouldNotEmitFlowStatsWhenFlowStatsAreNotConfigured() throws IOException, JMSException, JAXBException {
        Map<String,String> metricMap = Maps.newHashMap();
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithNoFlowStatsConfiguration.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            metricMap.put(m.getMetricPath(),m.getMetricValue());
        }
        Assert.assertFalse(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|TotalElapsedTime"));
        Assert.assertFalse(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Threads|ThreadStatistics|16375|TotalSizeOfInputMessages"));
    }

    @Test
    public void shouldEmitFlowStatsIncludedWhenIncludeClausesAreConfigured() throws IOException, JMSException, JAXBException {
        Map<String,String> metricMap = Maps.newHashMap();
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithSomeFlowStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            metricMap.put(m.getMetricPath(),m.getMetricValue());
        }
        Assert.assertTrue(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|TotalElapsedTime"));
        Assert.assertTrue(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Nodes|NodeStatistics|FAILQueue|TerminalStatistics|failure|CountOfInvocations"));
        Assert.assertFalse(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Threads|ThreadStatistics|16375|TotalSizeOfInputMessages"));
    }

    @Test
    public void shouldEmitMetricAliasWhenMetricsAreIncluded() throws JAXBException, JMSException, IOException {
        Map<String,String> metricMap = Maps.newHashMap();
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithSomeFlowStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            metricMap.put(m.getMetricPath(),m.getMetricValue());
        }
        Assert.assertTrue(metricMap.containsKey("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|Minimum Elapsed Time"));
    }

    @Test
    public void shouldEmitRightMetricTypeWhenMetricTypeIsConfigured() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithSomeFlowStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            if(m.getMetricPath().equals("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|AverageCPUTime")){
                Assert.assertTrue(m.getMetricValue().equals("0"));
                Assert.assertTrue(m.getAggregationType().equals("OBSERVATION"));
                Assert.assertTrue(m.getTimeRollUpType().equals("CURRENT"));
                Assert.assertTrue(m.getClusterRollUpType().equals("INDIVIDUAL"));
            }
        }
    }


    @Test
    public void shouldEmitRightMultiplierValueWhenMultiplierIsConfigured() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithSomeFlowStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        processor.onMessage(mockMsg);
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            if(m.getMetricPath().equals("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|TotalElapsedTime")){
                Assert.assertTrue(m.getMetricProperties().getMultiplier().equals(new BigDecimal("0.001")));
            }
        }
    }


    @Test
    public void shouldEmitDeltaMetricValueWhenDeltaIsConfigured() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithSomeFlowStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        processor.onMessage(mockMsg);
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m:(List<Metric>)pathCaptor.getValue()){
            if(m.getMetricPath().equals("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|TotalNumberOfMQErrors")){
                Assert.assertTrue(m.getMetricProperties().getDelta());
            }
        }
    }

    @Test
    public void shouldEmitConvertedValueWhenConvertIsConfigured() throws JAXBException, JMSException, IOException {
        Map<Object,Object> expectedConversionMap = Maps.newHashMap();
        expectedConversionMap.put("WSInputNode",1);
        expectedConversionMap.put("WSReplyNode",2);
        expectedConversionMap.put("MSLMappingNode",3);
        expectedConversionMap.put("$default",4);

        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithSomeFlowStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        processor.onMessage(mockMsg);
        ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);
        verify(writer,atLeastOnce()).transformAndPrintMetrics(pathCaptor.capture());
        for(Metric m: (List<Metric>)pathCaptor.getValue()){
            if(m.getMetricPath().equals("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Nodes|NodeStatistics|FAILQueue|Type")){
                Assert.assertTrue(m.getMetricProperties().getConversionValues().equals(expectedConversionMap));
            }
        }
    }


    private String getFileContents(String filepath) throws IOException {
        String filename = this.getClass().getResource(filepath).getFile();
        String text = Files.toString(new File(filename), Charsets.UTF_8);
        return text;
    }

    private FlowStatsProcessor<FlowStatistics> getFlowStatProcessor(String configFile, MetricWriteHelper writer) throws JAXBException {
        Map configMap = YmlReader.readFromFileAsMap(new File(this.getClass().getResource(configFile).getFile()));
        List<Map> qMgrs = (List<Map>)configMap.get("queueManagers");
        Map qMgrConfig = qMgrs.get(0);
        FlowStatsProcessor<FlowStatistics> processor = new FlowStatsProcessor<FlowStatistics>(qMgrConfig,parserFactory.getFlowStatisticsParser(),writer,"Custom Metrics|WMB|QMgr1");
        return processor;
    }
}
