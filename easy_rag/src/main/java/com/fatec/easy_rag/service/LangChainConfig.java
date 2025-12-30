package com.fatec.easy_rag.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Toda a configuração do LangChain4j é feita em um unico lugar. Facilita a
 * modificação
 * para outros modelos de IA.
 */
@Configuration
public class LangChainConfig {

    private final Logger logger = LogManager.getLogger(this.getClass());

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String chatModelName;

    @Value("${langchain4j.open-ai.embedding-model.model-name}")
    private String embeddingModelName;

    // 1. Configuração do Modelo de Chat
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        logger.info(">>>>>> Configurando o modelo de chat: {}", chatModelName);
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModelName)
                .temperature(0.0) // Essencial para RAG: evita que a IA invente factos
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    // 2. Configuração do Modelo de Embedding
    @Bean
    public EmbeddingModel embeddingModel() {
        logger.info(">>>>>> Configurando o modelo de embedding: {}", embeddingModelName);
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    // 3. Store em Memória
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    // 4. Ingestor (Leva os documentos para a Store)
    @Bean
    public EmbeddingStoreIngestor embeddingStoreIngestor(EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore) {
        logger.info(">>>>>> Configurando o ingestor de embeddings...");
        return EmbeddingStoreIngestor.builder()
                .documentSplitter(dev.langchain4j.data.document.splitter.DocumentSplitters.recursive(500, 50))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }

    // 5. Retriever (Busca os documentos relevantes)
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        logger.info(">>>>> Configurando o retriever de conteúdo...");
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.5)
                .build();
    }

    // 6. O Assistente configurado (Conecta Chat + RAG + Memória)
    @Bean
    public Assistente assistant(ChatLanguageModel chatLanguageModel, ContentRetriever contentRetriever) {
        logger.info(">>>>>> Configurando o Assistant com memória e retriever...");
        return AiServices.builder(Assistente.class)
                .chatLanguageModel(chatLanguageModel)
                .contentRetriever(contentRetriever)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
