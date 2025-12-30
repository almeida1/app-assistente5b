package com.fatec.easy_rag.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChainConfig {
    private static final Logger logger = LogManager.getLogger(LangChainConfig.class);

    // 1. Configuração do Modelo de Chat (application.properties)
    // 2. Configuração do Modelo de Embedding (application.properties)
    // 3. Store em memoria - toda vez que a aplicação é reiniciada, os embeddings
    // são perdidos este bean deve ser definido manualmente
    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
        logger.info(">>>>>> LangChainConfig - obtem uma instancia de Embedding store em memoria.");
        return new InMemoryEmbeddingStore<>();
    }
}
