import com.appdynamics.extensions.wmb.ParserBuilder;
import com.appdynamics.extensions.wmb.ResourceStatMessageListener;
import com.appdynamics.extensions.wmb.config.Configuration;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceIdentifier;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceStatistics;
import com.appdynamics.extensions.wmb.resourcestats.xml.ResourceType;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.Test;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ResourceStatMessageListenerTest {

    @Test
    public void canParseXmlMessageSuccessfully() throws IOException, JMSException, JAXBException {
        Unmarshaller unmarshaller = getParser();
        ResourceStatMessageListener listener = new ResourceStatMessageListener(getConfiguration(),unmarshaller);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats1.xml"));
        listener.onMessage(mockMsg);
    }

    @Test
    public void shouldNotThrowErrorWhenMessageIsNull() throws IOException, JMSException, JAXBException {
        Unmarshaller unmarshaller = getParser();
        ResourceStatMessageListener listener = new ResourceStatMessageListener(new Configuration(),unmarshaller);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(null);
        listener.onMessage(mockMsg);
    }

    @Test
    public void canPostMetricsSuccessfully() throws IOException, JMSException, JAXBException {
        Unmarshaller unmarshaller = getParser();
        Configuration config = getConfiguration();
        ResourceStatMessageListener listener = new ResourceStatMessageListener(config,unmarshaller);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats1.xml"));
        listener.onMessage(mockMsg);
    }

    private Configuration getConfiguration() {
        Configuration config = new Configuration();
        config.setMetricPrefix("Custom Metrics|WMB|");
        config.setMachineAgentUrl("http://localhost:8293/machineagent/metrics?");
        config.setNumberThreads(10);
        config.setThreadTimeout(2);
        return config;
    }

    private Unmarshaller getParser() throws JAXBException {
        return new ParserBuilder().getParser(ResourceStatistics.class, ResourceIdentifier.class, ResourceType.class);
    }


    private String getFileContents(String filepath) throws IOException {
        String filename = this.getClass().getResource(filepath).getFile();
        String text = Files.toString(new File(filename), Charsets.UTF_8);
        return text;
    }


}
