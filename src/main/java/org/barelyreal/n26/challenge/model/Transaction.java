package org.barelyreal.n26.challenge.model;

import com.google.gson.Gson;


public class Transaction {

    private static final Gson gson = new Gson();

    public final double amount;
    public final long timestamp;

    public Transaction(double amount, long timestamp) {
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public static Transaction fromJson(String json) {
        return gson.fromJson(json, Transaction.class);
    }
}
