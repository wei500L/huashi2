package com.huashi.eftransfer.ai.modules.rag.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    @Valid
    private AppServer appServer = new AppServer();
    @Valid
    private Ingestion ingestion = new Ingestion();
    @Valid
    private Retrieval retrieval = new Retrieval();

    public AppServer getAppServer() {
        return appServer;
    }

    public void setAppServer(AppServer appServer) {
        this.appServer = appServer;
    }

    public Ingestion getIngestion() {
        return ingestion;
    }

    public void setIngestion(Ingestion ingestion) {
        this.ingestion = ingestion;
    }

    public Retrieval getRetrieval() {
        return retrieval;
    }

    public void setRetrieval(Retrieval retrieval) {
        this.retrieval = retrieval;
    }

    public static class AppServer {

        private String baseUrl = "http://localhost:8080";
        @NotBlank
        private String internalToken = "";
        private Duration connectTimeout = Duration.ofSeconds(3);
        private Duration readTimeout = Duration.ofSeconds(5);

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getInternalToken() {
            return internalToken;
        }

        public void setInternalToken(String internalToken) {
            this.internalToken = internalToken;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }

    public static class Ingestion {

        private int exportPageSize = 100;
        private int embeddingBatchSize = 32;
        private boolean failedRetryEnabled = true;
        private int failedRetryLimit = 64;

        public int getExportPageSize() {
            return exportPageSize;
        }

        public void setExportPageSize(int exportPageSize) {
            this.exportPageSize = exportPageSize;
        }

        public int getEmbeddingBatchSize() {
            return embeddingBatchSize;
        }

        public void setEmbeddingBatchSize(int embeddingBatchSize) {
            this.embeddingBatchSize = embeddingBatchSize;
        }

        public boolean isFailedRetryEnabled() {
            return failedRetryEnabled;
        }

        public void setFailedRetryEnabled(boolean failedRetryEnabled) {
            this.failedRetryEnabled = failedRetryEnabled;
        }

        public int getFailedRetryLimit() {
            return failedRetryLimit;
        }

        public void setFailedRetryLimit(int failedRetryLimit) {
            this.failedRetryLimit = failedRetryLimit;
        }
    }

    public static class Retrieval {

        private int recallTopK = 20;
        private double recallThreshold = 0.55d;
        private int rerankTopN = 8;
        private double rerankThreshold = 0.20d;
        private int finalTopK = 6;
        private int hnswEfSearch = 64;

        public int getRecallTopK() {
            return recallTopK;
        }

        public void setRecallTopK(int recallTopK) {
            this.recallTopK = recallTopK;
        }

        public double getRecallThreshold() {
            return recallThreshold;
        }

        public void setRecallThreshold(double recallThreshold) {
            this.recallThreshold = recallThreshold;
        }

        public int getRerankTopN() {
            return rerankTopN;
        }

        public void setRerankTopN(int rerankTopN) {
            this.rerankTopN = rerankTopN;
        }

        public double getRerankThreshold() {
            return rerankThreshold;
        }

        public void setRerankThreshold(double rerankThreshold) {
            this.rerankThreshold = rerankThreshold;
        }

        public int getFinalTopK() {
            return finalTopK;
        }

        public void setFinalTopK(int finalTopK) {
            this.finalTopK = finalTopK;
        }

        public int getHnswEfSearch() {
            return hnswEfSearch;
        }

        public void setHnswEfSearch(int hnswEfSearch) {
            this.hnswEfSearch = hnswEfSearch;
        }
    }
}
