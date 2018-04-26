package org.barelyreal.n26.challenge;

import org.barelyreal.n26.challenge.model.SummaryStats;
import org.barelyreal.n26.challenge.model.Transaction;
import org.junit.Test;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.junit.Assert.*;


public class TransactionBufferTest {

    @Test
    public void createValidAndInvalidTransactions() {
        // Test creating valid and invalid transactions
        TransactionBuffer transactionBuffer = new TransactionBuffer(1, 2000);

        try {
            long currentTime = transactionBuffer.getCurrentTime();
            boolean success = transactionBuffer.addValue(new Transaction(1.0, currentTime));
            assertEquals(true, success);

            try { Thread.sleep(50); } catch (Exception e) {}

            currentTime = transactionBuffer.getCurrentTime();
            success = transactionBuffer.addValue(new Transaction(2.0, currentTime));
            assertEquals(true, success);

            try { Thread.sleep(50); } catch (Exception e) {}

            currentTime = transactionBuffer.getCurrentTime();
            success = transactionBuffer.addValue(new Transaction(4.0, currentTime-2000));
            assertEquals(false, success);

            // check the summary data
            SummaryStats summary = transactionBuffer.getSummary();
            assertEquals(3.0, summary.sum, 0.0);
            assertEquals(1.5, summary.avg,  0.0);
            assertEquals(2, summary.count);
            assertEquals(2.0, summary.max,  0.0);
            assertEquals(1.0, summary.min, 0.0);

        }
        catch (TimestampInFutureException e) {
            assertTrue("Buffer reported incorrect timestamp in future", false);
        }
    }

    // Test creating valid and invalid transactions
    @Test
    public void createTransactionsMultithreaded() {

        TransactionBuffer transactionBuffer = new TransactionBuffer(20, 1000);

        class MyRunnable implements Runnable {
            final Transaction transaction;
            MyRunnable (Transaction transaction) {
                this.transaction = transaction;
            }

            @Override
            public void run() {
                try {
                    transactionBuffer.addValue(transaction);
                }
                catch (Exception e) { assertTrue("Buffer reported incorrect timestamp in future", false);}
            }
        }

        long currentTime = transactionBuffer.getCurrentTime();

        Transaction[] transactionList = {
                new Transaction(1.0, currentTime),
                new Transaction(1.0, currentTime),
                new Transaction(1.0, currentTime),
                new Transaction(1.0, currentTime),
                new Transaction(1.0, currentTime),
                new Transaction(1.0, currentTime),
                new Transaction(1.0, currentTime),
                new Transaction(1.0, currentTime),
                new Transaction(1.0, currentTime),
                new Transaction(1.0, currentTime)
        };

        ExecutorService executor = Executors.newFixedThreadPool(30);
        for (Transaction transaction : transactionList) {
            Runnable worker = new MyRunnable(transaction);
            executor.execute(worker);
        }
        executor.shutdown();
        // Wait until all threads are finished
        while (!executor.isTerminated()) {
        }

        SummaryStats summary = transactionBuffer.getSummary();
        assertEquals(10.0, summary.sum, 0.0);
        assertEquals(1.0, summary.avg,  0.0);
        assertEquals(10, summary.count);
        assertEquals(1.0, summary.max,  0.0);
        assertEquals(1.0, summary.min, 0.0);
    }
}
