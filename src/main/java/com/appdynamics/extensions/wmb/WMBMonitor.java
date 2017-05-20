package com.appdynamics.extensions.wmb;


import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.wmb.metricUtils.CustomMetricWriter;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WMBMonitor extends AManagedMonitor{

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WMBMonitor.class);
    private static final String CONFIG_ARG = "config-file";
    private static final String METRIC_PREFIX = "Custom Metrics|WMB";
    private static final String MA_PID = "MA-PID";
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static CountDownLatch sharedLatch = new CountDownLatch(1);
    private MonitorConfiguration configuration;


    public WMBMonitor(){
        System.out.println(logVersion());
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext out) throws TaskExecutionException {
        logVersion();
        initialize(taskArgs);
        logger.debug("The raw arguments are {}", taskArgs);
        Map<String, ?> config = configuration.getConfigYml();
        if (config != null) {
            List<Map> managers = (List<Map>) config.get("queueManagers");
            if (managers != null && !managers.isEmpty()) {
                for (Map manager : managers) {
                    WMBMonitorTask task = createTask(manager);
                    configuration.getExecutorService().execute(task);
                }
            } else {
                logger.error("There are no queue managers configured");
            }
            Map watchDogProperties = (Map)config.get("machineAgentWatchDog");
            /*
             * This extension works in continuous mode and starts a JVM by invoking a script. When the MA dies, the extension becomes
             * an orphanned process. To better manage the extension, we watch the MA process using MA PID passed through the script.
             */
            scheduler.scheduleAtFixedRate(new ProcessWatchDog(taskArgs.get(MA_PID), sharedLatch, PathResolver.resolveDirectory(this.getClass())), ((Integer)watchDogProperties.get("initialDelay")).longValue(),((Integer)watchDogProperties.get("period")).longValue(), TimeUnit.SECONDS);
        } else {
            logger.error("The config.yml is not loaded due to previous errors. The task will not run");
        }

        return new TaskOutput("WMB monitor run completed successfully.");
    }

    private void initialize(Map<String, String> taskArgs) {
        //read the config.
        final String configFilePath = taskArgs.get(CONFIG_ARG);
        MetricWriteHelper metricWriteHelper = new CustomMetricWriter();
        MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);
        conf.setConfigYml(configFilePath);
        conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE,
                MonitorConfiguration.ConfItem.METRIC_PREFIX,MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER);
        this.configuration = conf;
    }


    private WMBMonitorTask createTask(Map manager) {
        return new WMBMonitorTask.Builder()
                .metricPrefix(configuration.getMetricPrefix())
                .metricWriter(configuration.getMetricWriter())
                .manager(manager)
                .build();
    }

    private class TaskRunnable implements Runnable{
        public void run() {
        }
    }

    private static String getImplementationVersion() {
        return WMBMonitor.class.getPackage().getImplementationTitle();
    }

    private String logVersion() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        return msg;
    }


    public static void main(String[] args){
        if (args == null || args.length == 0) {
            logger.error("MA PID was not passed as an argument to WMBMonitor.");
            return;
        }
        if(args.length > 1){
            logger.error("Incorrect number of arguments were passed to WMBMonitor.");
            return;
        }
        logger.info("MA pid = {}",args[0]);
        final WMBMonitor wmbMonitor = new WMBMonitor();
        String configFile = System.getProperty("extension.configuration");
        Map<String,String> taskArgs = Maps.newHashMap();
        taskArgs.put(CONFIG_ARG,configFile);
        taskArgs.put(MA_PID,args[0]);
        try {
            wmbMonitor.execute(taskArgs,null);
            logger.info("Connection started. Wait Indefinitely...");
            sharedLatch.await();
            logger.info("My parent has died. I have to die as well.");
            System.exit(0);
        }
        catch(Exception e){
            logger.error("Error in execution",e);
        }
        finally{
            if(scheduler != null){
                scheduler.shutdown();
            }
        }
    }

}
