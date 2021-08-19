package com.appdynamics.extensions.wmb;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.AssertUtils;
import com.appdynamics.extensions.util.PathResolver;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WMBMonitor extends ABaseMonitor {

    private MonitorContextConfiguration monitorContextConfiguration;
    private Map<String, ?> configYml;
    private static final String DEFAULT_METRIC_PREFIX = "Custom Metrics|WMB";
    private static final String MONITOR_NAME = "WMBMonitor";
    private static final String MA_PID = "MA-PID";
    private static final String CONFIG_FILE = "config-file";
    private static final String NAME = "name";
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static CountDownLatch sharedLatch = new CountDownLatch(1);
    private static String pid;
    private static final Logger logger = ExtensionsLoggerFactory.getLogger(WMBMonitor.class);


    @Override
    protected String getDefaultMetricPrefix() {
        return DEFAULT_METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return MONITOR_NAME;
    }

    @Override
    protected void initializeMoreStuff(Map<String, String> args) {
        monitorContextConfiguration = getContextConfiguration();
        configYml = monitorContextConfiguration.getConfigYml();
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        List<Map> queueManagers = (List<Map>) configYml.get("queueManagers");
        AssertUtils.assertNotNull(queueManagers, "QueueManagers cannot be null");
        for (Map queueManager : queueManagers) {
            AssertUtils.assertNotNull(queueManager, "QueueManager cannot be null");
            WMBMonitorTask task = createTask(queueManager, tasksExecutionServiceProvider.getMetricWriteHelper());
            tasksExecutionServiceProvider.submit((String) queueManager.get(NAME), task);
        }
        Map watchDogProperties = (Map) configYml.get("machineAgentWatchDog");
        /*
         * This extension works in continuous mode and starts a JVM by invoking a script. When the MA dies, the extension becomes
         * an orphanned process. To better manage the extension, we watch the MA process using MA PID passed through the script.
         */
        scheduler.scheduleAtFixedRate(new ProcessWatchDog(pid, sharedLatch, PathResolver.resolveDirectory(this.getClass())), ((Integer) watchDogProperties.get("initialDelay")).longValue(), ((Integer) watchDogProperties.get("period")).longValue(), TimeUnit.SECONDS);
    }

    private WMBMonitorTask createTask(Map queueManager, MetricWriteHelper metricWriteHelper) {
        return new WMBMonitorTask.Builder()
                .metricPrefix(monitorContextConfiguration.getMetricPrefix())
                .metricWriter(metricWriteHelper)
                .manager(queueManager)
                .build();
    }

    @Override
    protected List<Map<String, ?>> getServers() {
        return (List<Map<String, ?>>) configYml.get("queueManagers");
    }

    public static void main(String[] args) throws TaskExecutionException {

        if (args == null || args.length == 0) {
            logger.error("MA PID was not passed as an argument to WMBMonitor.");
            return;
        }
        if (args.length > 1) {
            logger.error("Incorrect number of arguments were passed to WMBMonitor.");
            return;
        }

        logger.info("MA pid = [{}]", args[0]);
        pid = args[0];
        final WMBMonitor monitor = new WMBMonitor();
        String configFile = System.getProperty("extension.configuration");
        final Map<String, String> taskArgs = new HashMap<>();
        taskArgs.put(CONFIG_FILE, configFile);
        taskArgs.put(MA_PID, args[0]);

        try {
            monitor.execute(taskArgs, null);
            logger.info("Connection started. Wait Indefinitely...");
            sharedLatch.await();
            logger.info("My parent has died. I have to die as well.");
            System.exit(0);
        } catch (Exception e) {
            logger.error("Error in Execution");
        } finally {
            if (scheduler != null) {
                scheduler.shutdown();
            }
        }
    }
}
