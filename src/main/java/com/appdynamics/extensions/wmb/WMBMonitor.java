package com.appdynamics.extensions.wmb;


import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class WMBMonitor extends AManagedMonitor{

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WMBMonitor.class);
    private static final String CONFIG_ARG = "config-file";
    private static final String METRIC_PREFIX = "Custom Metrics|WMB";

    private MonitorConfiguration configuration;
    private boolean initialized;

    public WMBMonitor(){
        System.out.println(logVersion());
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext out) throws TaskExecutionException {
        logVersion();
        if(!initialized){
            initialize(taskArgs);
        }
        logger.debug("The raw arguments are {}", taskArgs);
        configuration.executeTask();
        //logger.info("WMB monitor run completed successfully.");
        return new TaskOutput("WMB monitor run completed successfully.");
    }

    private void initialize(Map<String, String> taskArgs) {
        //read the config.
        final String configFilePath = taskArgs.get(CONFIG_ARG);
        MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
        MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);
        conf.setConfigYml(configFilePath);
        conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE,
                MonitorConfiguration.ConfItem.METRIC_PREFIX,MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER);
        this.configuration = conf;
        Map<String, ?> config = configuration.getConfigYml();
        logger.info("The config is {}",config);
        if (config != null) {
            List<Map> managers = (List<Map>) config.get("queueManagers");
            if (managers != null && !managers.isEmpty()) {
                for (Map manager : managers) {
                    WMBMonitorTask task = createTask(manager);
                    configuration.getExecutorService().execute(task);
                        /*try {
                            Thread.sleep(100000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }*/
                }
            } else {
                logger.error("There are no queue managers configured");
            }
        } else {
            logger.error("The config.yml is not loaded due to previous errors. The task will not run");
        }
        initialized = true;
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
            logger.info("Executing periodic run of WMBMonitor.");
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
}
