package com.appdynamics.extensions.wmb.resourcestats;

import com.appdynamics.extensions.wmb.config.Configuration;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ResourceStatsScheduledTask implements Runnable{

    private static final int DEFAULT_NUMBER_OF_THREADS = 3;
    private Configuration config;
    public static final Logger logger = Logger.getLogger(ResourceStatsScheduledTask.class);
    private ExecutorService threadPool;

    public ResourceStatsScheduledTask(Configuration configuration){
        this.config = configuration;
        threadPool = Executors.newFixedThreadPool( DEFAULT_NUMBER_OF_THREADS );
    }

    public void run() {
        if(logger.isDebugEnabled()){
            logger.debug("ResourceStatistics Task started.");
        }
        List<Future<Void>> parallelTasks = createParallelTasks(config);


    }


    private List<Future<Void>> createParallelTasks(Configuration config) {
        List<Future<Void>> parallelTasks = new ArrayList<Future<Void>>();
//        if(config != null && config.getResourceStatistics() != null){
//            for(ResourceStatistics resStat : config.getResourceStatistics()){
//                ResourceStatsSubscriber resMonitorTask = new ResourceStatsSubscriber(config.getHost(),config.getPort(),resStat,config.getMetricPrefix());
//                parallelTasks.add(getThreadPool().submit(resMonitorTask));
//            }
//        }
        return parallelTasks;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }
}
