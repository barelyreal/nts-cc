package org.barelyreal.n26.challenge;

import static spark.Spark.*;

public class Application {

    // Declare dependencies

    public static void main(String[] args) {

        // Instantiate your dependencies
        // Obviously we could use DI here, but for the sake of keeping things simple, let's just
        // create some singletons
        TransactionService transactionManager = new TransactionService();
        TransactionController transactionController = new TransactionController(transactionManager);

        // Configure Spark
        port(4567);

        // Set up routes
        post("/transactions",  transactionController::handleTransactionPost);
        get("/statistics", transactionController::handleSummaryGet);

    }

}
