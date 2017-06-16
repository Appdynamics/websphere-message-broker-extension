package com.appdynamics.extensions.wmb;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ProcessWatchDogTest {

    @BeforeClass
    public static void init() throws IOException {
        Runtime.getRuntime().exec(new String[]{"chmod", "-R","+x",ProcessWatchDogTest.class.getResource("/watchdog").getFile()});
    }

    @Test
    public void latchShouldRemainsSameWhenRunningProcessFound(){
        CountDownLatch cdl = new CountDownLatch(1);
        ProcessWatchDog pwd = new ProcessWatchDog("123",cdl,new File(this.getClass().getResource("/watchdog/success").getFile()));
        pwd.run();
        Assert.assertTrue(cdl.getCount() == 1);
    }

    @Test
    public void latchShouldCountDownWhenRunningNotProcessFound(){
        CountDownLatch cdl = new CountDownLatch(1);
        ProcessWatchDog pwd = new ProcessWatchDog("123",cdl,new File(this.getClass().getResource("/watchdog/failure").getFile()));
        pwd.run();
        Assert.assertTrue(cdl.getCount() == 0);
    }


}
