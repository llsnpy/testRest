package by.mironenko.testRest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final Semaphore semaphore;
    private final ScheduledExecutorService executorService;
    private final int requestLimit;

    private final static String REQUEST_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    public CrptApi(final TimeUnit time, final int requestLimit) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("Request limit should be greater than 0");
        }
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
        this.executorService = Executors.newScheduledThreadPool(1);

        long interval = time.toMillis(1);
        executorService.scheduleAtFixedRate(() -> {
            semaphore.release(requestLimit - semaphore.availablePermits());
        }, interval, interval, TimeUnit.MILLISECONDS);
    }

    public void createDoc(final Document document, final String signature) {
        try {
            semaphore.acquire();
            sendRequestToApi(document, signature);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendRequestToApi(final Document document, final String signature) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(REQUEST_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonInputString = document.toJson();
            try (OutputStream stream = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                stream.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                System.out.println("Successfully!");
            } else {
                throw new RuntimeException("Exception during sending request");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void shutdown() {
        executorService.shutdown();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Document {
        private String participantInn;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private Product[] products;
        private String regDate;
        private String regNumber;

        public String toJson() {
            return "{\"description\":{\"participantInn\":\"" + participantInn + "\"},"
                    + "\"doc_id\":\"" + docId + "\","
                    + "\"doc_status\":\"" + docStatus + "\","
                    + "\"doc_type\":\"" + docType + "\","
                    + "\"importRequest\":" + importRequest + ","
                    + "\"owner_inn\":\"" + ownerInn + "\","
                    + "\"participant_inn\":\"" + participantInn + "\","
                    + "\"producer_inn\":\"" + producerInn + "\","
                    + "\"production_date\":\"" + productionDate + "\","
                    + "\"production_type\":\"" + productionType + "\","
                    + "\"products\":" + productsToJson() + ","
                    + "\"reg_date\":\"" + regDate + "\","
                    + "\"reg_number\":\"" + regNumber + "\"}";
        }

        private String productsToJson() {
            StringBuilder productsJson = new StringBuilder("[");
            for (Product product : products) {
                productsJson.append(product.toJson()).append(",");
            }
            if (productsJson.length() > 1) {
                productsJson.setLength(productsJson.length() - 1);
            }
            productsJson.append("]");
            return productsJson.toString();
        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Product {
            private String certificateDocument;
            private String certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            private String productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;

            public String toJson() {
                return "{\"certificate_document\":\"" + certificateDocument + "\","
                        + "\"certificate_document_date\":\"" + certificateDocumentDate + "\","
                        + "\"certificate_document_number\":\"" + certificateDocumentNumber + "\","
                        + "\"owner_inn\":\"" + ownerInn + "\","
                        + "\"producer_inn\":\"" + producerInn + "\","
                        + "\"production_date\":\"" + productionDate + "\","
                        + "\"tnved_code\":\"" + tnvedCode + "\","
                        + "\"uit_code\":\"" + uitCode + "\","
                        + "\"uitu_code\":\"" + uituCode + "\"}";
            }
        }
    }
}
