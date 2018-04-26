package org.barelyreal.n26.challenge;

import org.barelyreal.n26.challenge.model.SummaryStats;
import org.barelyreal.n26.challenge.model.Transaction;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Keeps a buffer of summary stats for the last n milliseconds.
 *
 * objects are kept in array buckets that represent a fixed length of time relative to the last update
 * time.
 *
 * backingBuffer[0] would represent the time from lastUpdateTime to lastUpdateTime - bucketLengthMillis
 * backingBuffer[1] would represent the time from lastUpdateTime - (bucketLengthMillis * 2) to lastUpdateTime - bucketLengthMillis
 *
 * and so on
 *
 */

public class TransactionBuffer {

    // We use a ReadWriteLock because there should be very few (only one) writers and many readers.
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    private long lastUpdateTimeMillis = 0;

    private final int numBuckets;
    private final long bucketLengthMillis;
    private final long bufferLengthMillis;

    private final SummaryStats[] backingBuffer;
    private final SummaryStats[] zeroBuffer;

    public TransactionBuffer(long bucketLengthMillis, int numBuckets) {
        this.backingBuffer = new SummaryStats[numBuckets];
        this.zeroBuffer = new SummaryStats[numBuckets];
        this.numBuckets = numBuckets;
        this.bucketLengthMillis = bucketLengthMillis;
        this.bufferLengthMillis = bucketLengthMillis * numBuckets;
    }

    /**
     * Adds a value to the buffer.
     *
     * @param transaction the transaction to add
     * @return true if the value was added, false if dropped because it was too old
     * @throws TimestampInFutureException
     */
    public boolean addValue (Transaction transaction) throws TimestampInFutureException {

        rwl.writeLock().lock();
        try {
            updateCurrentTime();

            // Now get the bucket index for this timestamp-- if the timestamp is in the future, this will throw an
            // exception
            int index = bucketIndexForTimestamp(transaction.timestamp);
            if (index < 0) {
                return false;
            }

            SummaryStats curStats = backingBuffer[index];
            if (curStats == null) {
                curStats = new SummaryStats(transaction.amount);
            } else {
                curStats = curStats.add(transaction.amount);
            }

            backingBuffer[index] = curStats;
            return true;
        }
        finally {
            rwl.writeLock().unlock();
        }
    }

    /**
     * Single point to get the current time to make sure that this is consistent throughout the app.
     *
     * @return
     */
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    // Figure out if the transaction falls within the last bufferLengthMillis
    public boolean transactionWillSucceed(Transaction transaction) {
        long curTime = getCurrentTime();
        return (transaction.timestamp >= (curTime - bufferLengthMillis)) &&
                transaction.timestamp <= curTime;
    }

    /**
     * Updates the current time and purges any objects that are too old.
     */
    private void updateCurrentTime () {

        long curTime = getCurrentTime();
        // If it's our first run, don't do anything
        if (lastUpdateTimeMillis == 0) {
            lastUpdateTimeMillis = curTime;
            return;
        }

        // Calculate the difference between our last update and the new time
        long timeDiff = curTime - lastUpdateTimeMillis;

        assert (timeDiff >= 0); // if the last update time is greater than curTime, something is seriously wrong

        lastUpdateTimeMillis = curTime;

        // figure out how many buckets to purge
        int numBucketsToPurge = (int) (timeDiff / bucketLengthMillis);
        if (numBucketsToPurge == 0) {
            return;
        }

        int moveLength = numBuckets - numBucketsToPurge;

        // move still valid items to the end of the buffer
        System.arraycopy(backingBuffer, 0, backingBuffer, numBucketsToPurge, moveLength);

        // zero out the beginning of the buffer
        System.arraycopy(zeroBuffer, 0, backingBuffer, 0, numBucketsToPurge);
    }

    public SummaryStats getSummary() {
        rwl.readLock().lock();
        try {
            // Amount of time that has elapsed since we last updated
            long timeDiff = getCurrentTime() - lastUpdateTimeMillis;
            assert (timeDiff >= 0);

            SummaryStats stats = null;
            if (timeDiff > bufferLengthMillis) {
                return new SummaryStats(0,0,0,0,0);
            }

            int numBucketsToIgnore = (int) (timeDiff / bucketLengthMillis);

            // Iterate through all stat buckets and combine them.  This will be constant time,
            // since the number of buckets is fixed.
            for (int i = 0; i < numBuckets - numBucketsToIgnore; i++) {
                SummaryStats curStat = backingBuffer[i];
                if (stats == null) {
                    stats = curStat;
                }
                else {
                    stats = stats.add(curStat);
                }
            }

            if (stats == null) {
                return new SummaryStats(0,0,0,0,0);
            }

            return stats;
        }
        finally {
            rwl.readLock().unlock();
        }
    }

    private int bucketIndexForTimestamp (long timestampMillis) throws TimestampInFutureException {

        // If the timestamp is in the future, we definitely want the caller to know
        if (timestampMillis > lastUpdateTimeMillis) {
            throw new TimestampInFutureException();
        }

        // If the timestamp is older than the buffer start time, return -1
        if (timestampMillis <= lastUpdateTimeMillis - bufferLengthMillis) {
            return -1;
        }

        // timestamp is inside the buffer
        long offsetFromCurrentTime = lastUpdateTimeMillis - timestampMillis;

        return (int) (offsetFromCurrentTime / bucketLengthMillis);
    }
}
