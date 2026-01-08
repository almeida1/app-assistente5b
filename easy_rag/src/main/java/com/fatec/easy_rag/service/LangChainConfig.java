package com.fatec.easy_rag.service;

import dev.langchain4j.data.segment.TextSegment;
import java.util.List;
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
                .documentSplitter(dev.langchain4j.data.document.splitter.DocumentSplitters.recursive(1000, 150))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }

    // 5. Retriever (Busca os documentos relevantes) - recuperação semantica
    // converte a pergunta do usuario
    // e os documentos em vetores e busca os documentos mais similares baseando-se
    // no significado (semantica) com filtragem por metadados
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        logger.info(">>>>> Configurando o retriever de conteúdo com filtragem de metadados...");

        // Implementação da Estratégia 2: Recuperação por Metadados
        // Filtra dinamicamente os documentos baseando-se em palavras-chave na pergunta
        return query -> {
            dev.langchain4j.store.embedding.filter.Filter filter = null;
            String userQuery = query.text().toLowerCase();

            // Exemplo de regra de negócio para filtro de metadados atualizado
            if (userQuery.contains("sequencial")) {
                logger.info(">>>>>> Aplicando filtro: sdlc_type = sequencial");
                filter = dev.langchain4j.store.embedding.filter.MetadataFilterBuilder
                        .metadataKey("sdlc_type").isEqualTo("sequencial");
            } else if (userQuery.contains("analista")) {
                logger.info(">>>>>> Aplicando filtro: role = Analista de Teste");
                filter = dev.langchain4j.store.embedding.filter.MetadataFilterBuilder
                        .metadataKey("role").isEqualTo("Analista de Teste");
            }

            // Constrói o retriever sob demanda com o filtro apropriado
            List<dev.langchain4j.rag.content.Content> results = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(embeddingModel)
                    .filter(filter) // Aplica o filtro (pode ser null)
                    .maxResults(3)
                    .minScore(0.5)
                    .build()
                    .retrieve(query);

            logger.info(">>>>>> Contexto recuperado ({} segmentos):", results.size());
            results.forEach(content -> logger.info("   - {}", content.textSegment().text()));
            logger.info(">>>>>> LangChainConfig Filtro => " + filter);
            return results;
        };
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
