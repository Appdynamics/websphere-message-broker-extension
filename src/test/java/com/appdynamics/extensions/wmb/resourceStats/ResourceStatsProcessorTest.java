package com.appdynamics.extensions.wmb.resourceStats;



import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.wmb.metricUtils.MetricPrinter;
import com.appdynamics.extensions.wmb.ParserFactory;
import com.appdynamics.extensions.wmb.resourcestats.ResourceStatistics;
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

public class ResourceStatsProcessorTest {

    ParserFactory parserFactory = new ParserFactory();

    @Test
    public void canParseXmlMessageSuccessfully() throws IOException, JMSException, JAXBException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/config.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Resource Statistics|JVM|summary|InitialMemoryInMB"));
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Resource Statistics|Sockets|summary|TotalDataSent_KB"));
        Assert.assertTrue(valueCaptor.getAllValues().contains("256"));
    }

    @Test
    public void shouldNotThrowErrorWhenMessageIsNull() throws IOException, JMSException, JAXBException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/config.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(null);
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        processor.onMessage(mockMsg);
        verify(writer,never()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
    }

    @Test
    public void shouldNotEmitResourceStatsWhenResourceStatsAreNotConfigured() throws IOException, JMSException, JAXBException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/configWithNoResourceStatsConfiguration.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        processor.onMessage(mockMsg);
        verify(writer,never()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
    }


    @Test
    public void shouldEmitResourceStatsIncludedWhenIncludeClausesAreConfigured() throws IOException, JMSException, JAXBException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/configWithSomeResourceStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Resource Statistics|JVM|summary|InitialMemoryInMB"));
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Resource Statistics|JVM|Garbage Collection - MarkSweepCompact|CumulativeNumberOfGCCollections"));
        Assert.assertFalse(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Resource Statistics|Sockets|summary|TotalDataSent_KB"));
        Assert.assertTrue(valueCaptor.getAllValues().contains("256"));
        Assert.assertFalse(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Resource Statistics|Parsers|summary|ApproxMemKB"));
    }

    @Test
    public void shouldEmitMetricAliasWhenMetricsAreIncluded() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/configWithSomeResourceStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Resource Statistics|JDBCConnectionPools|summary|This is max pool"));
    }

    @Test
    public void shouldEmitRightMetricTypeWhenMetricTypeIsConfigured() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/configWithSomeResourceStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aggCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> timeRollupCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clusterRollupCaptor = ArgumentCaptor.forClass(String.class);
        processor.onMessage(mockMsg);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),anyString(),aggCaptor.capture(),timeRollupCaptor.capture(),clusterRollupCaptor.capture());
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Resource Statistics|JDBCConnectionPools|summary|CumulativeTimedOutRequests"));
        Assert.assertTrue(aggCaptor.getAllValues().contains("OBSERVATION"));
        Assert.assertTrue(timeRollupCaptor.getAllValues().contains("CURRENT"));
        Assert.assertTrue(clusterRollupCaptor.getAllValues().contains("INDIVIDUAL"));
    }


    @Test
    public void shouldEmitRightMetricValueWhenMultiplierIsConfigured() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/configWithSomeResourceStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
        processor.onMessage(mockMsg);
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Resource Statistics|JDBCConnectionPools|summary|CumulativeTimedOutRequests"));
        Assert.assertTrue(valueCaptor.getAllValues().contains("500000"));
    }


    @Test
    public void shouldEmitDeltaMetricValueWhenDeltaIsConfigured() throws JAXBException, JMSException, IOException {
        MetricWriteHelper writer = mock(MetricWriteHelper.class);
        ResourceStatsProcessor processor = getResourceStatProcessor("/conf/configWithSomeResourceStatsIncludesMissing.yml",writer);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.xml"));
        TextMessage mockMsg1 = mock(TextMessage.class);
        when(mockMsg1.getText()).thenReturn(getFileContents("/resourceStats2.xml"));
        processor.onMessage(mockMsg);
        processor.onMessage(mockMsg1);
        ArgumentCaptor<String> metricPathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(writer,atLeastOnce()).printMetric(metricPathCaptor.capture(),valueCaptor.capture(),anyString(),anyString(),anyString());
        Assert.assertTrue(metricPathCaptor.getAllValues().contains("Custom Metrics|WMB|QMgr1|default|Resource Statistics|JDBCConnectionPools|summary|CumulativeDelayedRequests"));
        Assert.assertTrue(valueCaptor.getAllValues().contains("750"));
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
        MetricPrinter printer = new MetricPrinter("Custom Metrics|WMB","QMgr1",writer);
        ResourceStatsProcessor<ResourceStatistics> processor = new ResourceStatsProcessor<ResourceStatistics>(qMgrConfig,parserFactory.getResourceStatisticsParser(),printer);
        return processor;
    }

}