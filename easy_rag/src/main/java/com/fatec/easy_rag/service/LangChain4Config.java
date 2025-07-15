package com.fatec.easy_rag.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4Config {
	private static final Logger logger = LogManager.getLogger(LangChain4Config.class);
    // Configura e retorna uma instância de OpenAiChatModel.
    // As propriedades são injetadas do application.yml.
    @Bean
    OpenAiChatModel chatLanguageModel(
            @Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model.model-name}") String modelName,
            @Value("${langchain4j.open-ai.chat-model.temperature}") Double temperature ) 
    		{
    	logger.info(">>>>>> LangChainConfig - configura os parametros do modelo de chat.");
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .logRequests(true) // Habilita o log das requisições para depuração
                .logResponses(true) // Habilita o log das respostas para depuração
                .build();
    }

    // Configura e retorna uma instância de OpenAiEmbeddingModel considerando application properties.
    @Bean
    OpenAiEmbeddingModel embeddingModel(
            @Value("${langchain4j.open-ai.embedding-model.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.embedding-model.model-name}") String modelName) {
    	logger.info(">>>>>> LangChainConfig - configura os parametros do Embedding model.");
    	return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .logRequests(true) // Habilita o log das requisições para depuração
                .logResponses(true) // Habilita o log das respostas para depuração
                .build();
    }

    // Configura e retorna uma instância de InMemoryEmbeddingStore.
    // Este é um Vector Store em memória, simples para começar.
    // Em produção deve-se utilizar Vector Store persistente.
    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
    	logger.info(">>>>>> LangChainConfig - obtem uma instancia de Embedding store em memoria.");
        return new InMemoryEmbeddingStore<>();
    }
}
