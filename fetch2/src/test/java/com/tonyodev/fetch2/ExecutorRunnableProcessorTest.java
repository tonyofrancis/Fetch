package com.tonyodev.fetch2;

import com.tonyodev.fetch2.core.ExecutorRunnableProcessor;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by tonyofrancis on 6/30/17.
 */

public class ExecutorRunnableProcessorTest {

    @Test
    public void testClearQueue() throws Exception {
        ExecutorRunnableProcessor processor = new ExecutorRunnableProcessor();
        processor.queue(getLongRunnable());
        processor.queue(getLongRunnable());
        processor.queue(getLongRunnable());

        processor.clear();
        Assert.assertTrue(processor.isEmpty());
    }

    @Test
    public void testIsNotEmpty() throws Exception {
        ExecutorRunnableProcessor processor = new ExecutorRunnableProcessor();
        processor.queue(getLongRunnable());
        processor.queue(getLongRunnable());
        processor.queue(getLongRunnable());

        Assert.assertFalse(processor.isEmpty());
    }

    @Test
    public void testNotNullInsert() throws Exception {
        ExecutorRunnableProcessor processor = new ExecutorRunnableProcessor();
        processor.queue(getLongRunnable());
    }

    @Test
    public void testNext() throws Exception {
        ExecutorRunnableProcessor processor = new ExecutorRunnableProcessor();
        processor.queue(getLongRunnable());
        processor.queue(getLongRunnable());

        processor.next();
        processor.next();
    }

    private Runnable getLongRunnable() {
        return new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.currentThread().wait(10_000);
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

}
