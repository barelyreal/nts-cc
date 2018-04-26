package org.barelyreal.n26.challenge;

import org.barelyreal.n26.challenge.model.SummaryStats;
import org.barelyreal.n26.challenge.model.Transaction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransactionControllerTest {


    @Test
    public void createValidAndInvalidTransactions() {
        // Test creating valid and invalid transactions
        TransactionBuffer transactionBuffer = new TransactionBuffer(1, 2000);

        try {
            long currentTime = transactionBuffer.getCurrentTime();
            boolean success = transactionBuffer.addValue(new Transaction(1.0, currentTime));
            assertEquals(true, success);

            currentTime = transactionBuffer.getCurrentTime();
            success = transactionBuffer.addValue(new Transaction(2.0, currentTime));
            assertEquals(true, success);

            currentTime = transactionBuffer.getCurrentTime();
            success = transactionBuffer.addValue(new Transaction(4.0, currentTime-3000));
            assertEquals(false, success);

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

}
