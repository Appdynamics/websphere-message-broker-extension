package com.appdynamics.extensions.wmb;


import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.util.PathResolver;
import org.slf4j.Logger;

import java.io.File;
import java.util.concurrent.CountDownLatch;

/**
 * Given a PID, watch if the PID process is still running
 */
class ProcessWatchDog implements Runnable{

    private static final Logger logger = ExtensionsLoggerFactory.getLogger(ProcessWatchDog.class);
    private String pid;
    private CountDownLatch countDownLatch;
    private File installDir;
    private final ProcessExecutor processExecutor = new ProcessExecutor();

    ProcessWatchDog(String pid, CountDownLatch countDownLatch, File installDir) {
        this.pid = pid;
        this.countDownLatch = countDownLatch;
        this.installDir = installDir;
    }

    public void run() {
        boolean isProcessRunning = false;
        String[] command = getCommand();
        String output = processExecutor.execute(command).trim();
        isProcessRunning = pid.equals(output);
        if(!isProcessRunning){
            logger.debug("The process with PID {} is no longer running.", pid);
            countDownLatch.countDown();
        }
    }

    private String[] getCommand() {
        if(Util.isWindows()){
            return new String[]{PathResolver.getFile("process_checker.bat",installDir).getPath(),pid};
        }
        else{
            return new String[] {PathResolver.getFile("./process_checker.sh",installDir).getPath(),pid};
        }
    }
}
