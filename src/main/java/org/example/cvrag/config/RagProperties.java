package org.example.cvrag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private int chunkSize = 800;
    private int chunkOverlap = 120;
    private int retrieveTopK = 6;
    private int retrievalCandidateMultiplier = 4;
    private double hybridAlpha = 0.7;
    private int shortMemoryWindow = 8;
    private int summaryTriggerTurns = 24;
    private String kbNamespace = "cv-rag-kb";
    private String memoryNamespace = "cv-rag-memory";

    private Vector vector = new Vector();
    private Memory memory = new Memory();
    private Model model = new Model();
    private Pinecone pinecone = new Pinecone();

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public int getRetrieveTopK() {
        return retrieveTopK;
    }

    public void setRetrieveTopK(int retrieveTopK) {
        this.retrieveTopK = retrieveTopK;
    }

    public int getRetrievalCandidateMultiplier() {
        return retrievalCandidateMultiplier;
    }

    public void setRetrievalCandidateMultiplier(int retrievalCandidateMultiplier) {
        this.retrievalCandidateMultiplier = retrievalCandidateMultiplier;
    }

    public double getHybridAlpha() {
        return hybridAlpha;
    }

    public void setHybridAlpha(double hybridAlpha) {
        this.hybridAlpha = hybridAlpha;
    }

    public int getShortMemoryWindow() {
        return shortMemoryWindow;
    }

    public void setShortMemoryWindow(int shortMemoryWindow) {
        this.shortMemoryWindow = shortMemoryWindow;
    }

    public int getSummaryTriggerTurns() {
        return summaryTriggerTurns;
    }

    public void setSummaryTriggerTurns(int summaryTriggerTurns) {
        this.summaryTriggerTurns = summaryTriggerTurns;
    }

    public String getKbNamespace() {
        return kbNamespace;
    }

    public void setKbNamespace(String kbNamespace) {
        this.kbNamespace = kbNamespace;
    }

    public String getMemoryNamespace() {
        return memoryNamespace;
    }

    public void setMemoryNamespace(String memoryNamespace) {
        this.memoryNamespace = memoryNamespace;
    }

    public Vector getVector() {
        return vector;
    }

    public void setVector(Vector vector) {
        this.vector = vector;
    }

    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Pinecone getPinecone() {
        return pinecone;
    }

    public void setPinecone(Pinecone pinecone) {
        this.pinecone = pinecone;
    }

    public static class Vector {
        private String provider = "in-memory";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }
    }

    public static class Memory {
        private boolean redisEnabled = false;
        private long ttlHours = 72;

        public boolean isRedisEnabled() {
            return redisEnabled;
        }

        public void setRedisEnabled(boolean redisEnabled) {
            this.redisEnabled = redisEnabled;
        }

        public long getTtlHours() {
            return ttlHours;
        }

        public void setTtlHours(long ttlHours) {
            this.ttlHours = ttlHours;
        }
    }

    public static class Model {
        private boolean mockEnabled = true;
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String apiKey = "";
        private String chatModel = "qwen-plus";
        private String embeddingModel = "text-embedding-v3";
        private String clipModel = "clip-vit-large-patch14";
        private double temperature = 0.2;

        public boolean isMockEnabled() {
            return mockEnabled;
        }

        public void setMockEnabled(boolean mockEnabled) {
            this.mockEnabled = mockEnabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getChatModel() {
            return chatModel;
        }

        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }

        public String getEmbeddingModel() {
            return embeddingModel;
        }

        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        public String getClipModel() {
            return clipModel;
        }

        public void setClipModel(String clipModel) {
            this.clipModel = clipModel;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
    }

    public static class Pinecone {
        private String apiKey = "";
        private String host = "";
        private String index = "";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }
    }
}
