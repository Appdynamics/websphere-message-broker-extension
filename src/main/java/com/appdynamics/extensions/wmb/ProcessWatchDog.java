package com.appdynamics.extensions.wmb;


import com.appdynamics.extensions.PathResolver;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.CountDownLatch;

class ProcessWatchDog implements Runnable{

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ProcessWatchDog.class);
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
            countDownLatch.countDown();
        }
    }

    private String[] getCommand() {
        if(Util.isWindows()){
            return new String[]{PathResolver.getFile("process_checker.bat",installDir).getPath(),pid};
        }
        else{
            return new String[] {PathResolver.getFile("process_checker.sh",installDir).getPath(),pid};
        }
    }
}
