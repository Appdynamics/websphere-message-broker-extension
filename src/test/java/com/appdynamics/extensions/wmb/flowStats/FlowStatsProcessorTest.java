package com.appdynamics.extensions.wmb.flowStats;


import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.wmb.ParserFactory;
import com.appdynamics.extensions.wmb.flowstats.FlowStatistics;
import com.appdynamics.extensions.wmb.flowstats.FlowStatsProcessor;
import com.appdynamics.extensions.wmb.metricUtils.MetricPrinter;
import com.appdynamics.extensions.wmb.resourcestats.ResourceStatsProcessor;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class FlowStatsProcessorTest {

    ParserFactory parserFactory = new ParserFactory();

    @Test
    public void canParseXmlMessageSuccessfully() throws IOException, JMSException, JAXBException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/config.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|TotalElapsedTime"));
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Threads|ThreadStatistics|16375|TotalSizeOfInputMessages"));
        Assert.assertTrue(valueCaptor.getAllValues().contains("1289"));
    }

    @Test
    public void shouldNotThrowErrorWhenMessageIsNull() throws IOException, JMSException, JAXBException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/config.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(null);
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        processor.onMessage(mockMsg);
        verify(writer,never()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
    }


    @Test
    public void shouldCalculateCorrectDerivedMetricWhenDerivedMetricConfigured() throws IOException, JMSException, JAXBException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/config.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|AverageCPUTimeWaitingForInputMessage"));
        Assert.assertTrue(valueCaptor.getAllValues().contains("26"));
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Threads|ThreadStatistics|16375|AverageElapsedTimeWaitingForInputMessage"));
        Assert.assertTrue(valueCaptor.getAllValues().contains("200034"));
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Nodes|NodeStatistics|HTTP Input|AverageCPUTime"));
        Assert.assertTrue(valueCaptor.getAllValues().contains("2"));
    }


    @Test
    public void shouldNotEmitDerivedStatsWhenDerivedAreNotConfigured() throws IOException, JMSException, JAXBException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithNoDerivedMetrics.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
        Assert.assertFalse(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Threads|ThreadStatistics|16375|AverageElapsedTimeWaitingForInputMessage"));
        Assert.assertFalse(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Nodes|NodeStatistics|HTTP Input|AverageCPUTime"));

    }


    @Test
    public void shouldNotEmitFlowStatsWhenFlowStatsAreNotConfigured() throws IOException, JMSException, JAXBException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithNoFlowStatsConfiguration.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
        Assert.assertFalse(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|TotalElapsedTime"));
        Assert.assertFalse(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Threads|ThreadStatistics|16375|TotalSizeOfInputMessages"));

    }

    @Test
    public void shouldEmitFlowStatsIncludedWhenIncludeClausesAreConfigured() throws IOException, JMSException, JAXBException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithSomeFlowStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|TotalElapsedTime"));
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Nodes|NodeStatistics|FAILQueue|TerminalStatistics|failure|CountOfInvocations"));
        Assert.assertFalse(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Threads|ThreadStatistics|16375|TotalSizeOfInputMessages"));
    }

    @Test
    public void shouldEmitMetricAliasWhenMetricsAreIncluded() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithSomeFlowStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|Minimum CPU Time"));
    }

    @Test
    public void shouldEmitRightMetricTypeWhenMetricTypeIsConfigured() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithSomeFlowStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aggCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> timeRollupCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clusterRollupCaptor = ArgumentCaptor.forClass(String.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),anyString(),aggCaptor.capture(),timeRollupCaptor.capture(),clusterRollupCaptor.capture());
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|AverageCPUTime"));
        Assert.assertTrue(aggCaptor.getAllValues().contains("OBSERVATION"));
        Assert.assertTrue(timeRollupCaptor.getAllValues().contains("CURRENT"));
        Assert.assertTrue(clusterRollupCaptor.getAllValues().contains("INDIVIDUAL"));
    }


    @Test
    public void shouldEmitRightMetricValueWhenMultiplierIsConfigured() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithSomeFlowStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        processor.onMessage(mockMsg);
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|TotalElapsedTime"));
        Assert.assertTrue(valueCaptor.getAllValues().contains("1000"));
    }


    @Test
    public void shouldEmitDeltaMetricValueWhenDeltaIsConfigured() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithSomeFlowStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        TextMessage mockMsg1 = mock(TextMessage.class);
        when(mockMsg1.getText()).thenReturn(getFileContents("/flowStats1.xml"));
        processor.onMessage(mockMsg);
        processor.onMessage(mockMsg1);
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|MessageFlow|TotalNumberOfMQErrors"));
        Assert.assertTrue(valueCaptor.getAllValues().contains("100"));
    }

    @Test
    public void shouldEmitConvertedValueWhenConvertIsConfigured() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        FlowStatsProcessor processor = getFlowStatProcessor("/conf/configWithSomeFlowStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/flowStats.xml"));
        processor.onMessage(mockMsg);
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Flow Statistics|Nodes|NodeStatistics|FAILQueue|Type"));
        Assert.assertTrue(valueCaptor.getAllValues().contains("4"));
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
        MetricPrinter printer = new MetricPrinter("Custom Metrics|WMB","QMgr1",writer);
        FlowStatsProcessor<FlowStatistics> processor = new FlowStatsProcessor<FlowStatistics>(qMgrConfig,parserFactory.getFlowStatisticsParser(),printer);
        return processor;
    }
}
