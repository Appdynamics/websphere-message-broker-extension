import com.appdynamics.extensions.wmb.ParserBuilder;
import com.appdynamics.extensions.wmb.ResourceStatMessageListener;
import com.appdynamics.extensions.wmb.config.Configuration;
import com.appdynamics.extensions.wmb.resourcestats.json.ResourceStatsAdapter;
import com.appdynamics.extensions.wmb.resourcestats.json.ResourceStatsObj;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.jms.JMSTextMessage;
import com.ibm.msg.client.jms.internal.JmsTextMessageImpl;
import org.junit.Test;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ResourceStatMessageListenerTest {

    @Test
    public void canParseJsonMessageSuccessfully() throws IOException, JMSException {
        Gson gson = getParser();
        ResourceStatMessageListener listener = new ResourceStatMessageListener(new Configuration(),gson);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(getFileContents("/resourceStats.json"));
        listener.onMessage(mockMsg);
    }

    @Test
    public void canParseJsonMessageUnSuccessfully() throws IOException, JMSException {
        Gson gson = getParser();
        ResourceStatMessageListener listener = new ResourceStatMessageListener(new Configuration(),gson);
        TextMessage mockMsg = mock(TextMessage.class);
        when(mockMsg.getText()).thenReturn(null);
        listener.onMessage(mockMsg);
    }

    private Gson getParser() {
        return new ParserBuilder().getParser(ResourceStatsObj.class,new ResourceStatsAdapter());
    }


    private String getFileContents(String filepath) throws IOException {
        String filename = this.getClass().getResource(filepath).getFile();
        String text = Files.toString(new File(filename), Charsets.UTF_8);
        return text;
    }


}
