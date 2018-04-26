package org.barelyreal.n26.challenge;


import org.barelyreal.n26.challenge.model.SummaryStats;
import org.barelyreal.n26.challenge.model.Transaction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class TransactionService {

    // We are using an ExecutorService in order to decouple the ingest from the processing.  This will provide
    // better response times for the clients submitting transactions, especially if there are a lot
    // of incoming transactions, since otherwise we would need to get the write lock for each transaction.
    //
    // The challenge instructions specified a "real time" API, but did not define exactly what that meant.
    // If we wanted complete transactional integrity, we could simply call the TransactionBuffer directly,
    // but usually in such situations the possibility of a slight delay in processing is acceptable, so I
    // decided to optimize for better scaling.
    //
    // In a distributed environment this would be where we hand off to a clustered db or in-memory cache
    private final ExecutorService executorService;

    // We set this to 60000 buckets at 1 ms each, since we want to-the-millisecond accuracy for 60 seconds
    // If we wanted less accuracy and better performance we could increase the bucket size and reduce
    // the number of buckets
    private TransactionBuffer transactionBuffer = new TransactionBuffer(1, 60000);

    public TransactionService() {
        executorService = Executors.newSingleThreadExecutor();
    }

    public void submitTransaction (Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException();
        }
        executorService.submit(new UpdateStatsTask(transaction));
    }

    /**
     * In order to let the client know if the transaction will succeed or not, we need this, since
     * we aren't actually doing the processing immediately.
     *
     * @param transaction
     * @return
     */
    public boolean transactionWillSucceed(Transaction transaction) {
        return transactionBuffer.transactionWillSucceed(transaction);
    }

    public SummaryStats getSummary() {
        return transactionBuffer.getSummary();
    }

    private class UpdateStatsTask implements Runnable {
        final Transaction transaction;

        UpdateStatsTask(Transaction transaction) {
            this.transaction = transaction;
        }

        public void run() {
            try {
                transactionBuffer.addValue(transaction);
            }
            catch (TimestampInFutureException e) {
                // for now just ignore
            }
        }
    }
}
