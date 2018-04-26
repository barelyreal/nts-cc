package org.barelyreal.n26.challenge.model;

import com.google.gson.Gson;

public class SummaryStats {

    private static final Gson gson = new Gson();

    public final double sum;
    public final double avg;
    public final double max;
    public final double min;
    public final long count;

    public SummaryStats (double value) {
        this.sum = value;
        this.avg = value;
        this.max = value;
        this.min = value;
        this.count = 1;
    }

    public SummaryStats (double sum, double avg, double max, double min, long count) {
        this.sum = sum;
        this.avg = avg;
        this.max = max;
        this.min = min;
        this.count = count;
    }

    public SummaryStats add (SummaryStats other) {
        if (other == null) {
            return this;
        }
        double newSum = sum + other.sum;
        long newCount = count + other.count;
        double newMax = Math.max(max, other.max);
        double newMin = Math.min(min, other.min);
        double newAvg = newCount == 0 ? 0 : newSum / (double) newCount;
        return new SummaryStats(newSum, newAvg, newMax, newMin, newCount);
    }

    public SummaryStats add (double value) {
        double newSum = sum + value;
        long newCount = count + 1;
        double newMax = Math.max(max, value);
        double newMin = Math.min(min, value);
        double newAvg = newSum / (double) newCount;
        return new SummaryStats(newSum, newAvg, newMax, newMin, newCount);
    }

    public String toJson() {
        return gson.toJson(this);
    }
}
