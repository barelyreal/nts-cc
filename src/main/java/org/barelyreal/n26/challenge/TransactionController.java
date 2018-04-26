package org.barelyreal.n26.challenge;

import org.barelyreal.n26.challenge.model.SummaryStats;
import org.barelyreal.n26.challenge.model.Transaction;
import spark.Request;
import spark.Response;


/**
 * Handles transaction-related HTTP requests
 */
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController (TransactionService service) {
        this.transactionService = service;
    }

    public Object handleTransactionPost(Request request, Response response) {

        String requestBody = request.body();
        Transaction transaction = Transaction.fromJson(requestBody);

        if (! transactionService.transactionWillSucceed(transaction)) {
            response.status(204);
            return "";
        }

        transactionService.submitTransaction(transaction);
        response.status(200);
        return "";
    }

    public Object handleSummaryGet(Request request, Response response) {
        SummaryStats summary = transactionService.getSummary();
        return summary.toJson();
    }
}
