package com.fatec.easy_rag.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever; // Usando a implementação específica
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Serviço responsável pela recuperação de dados e geração de respostas
 * usando o modelo RAG.
 */
@Service
public class RagQueryService {

    private static final Logger logger = LogManager.getLogger(RagQueryService.class);

    private final Assistente assistant; // Usamos a interface Assistente
    private final ContentRetriever contentRetriever;

    /**
     * Construtor do RagQueryService.
     * 
     * @param chatLanguageModel O modelo de linguagem de chat para gerar respostas.
     * @param embeddingModel    O modelo de embedding usado para consultas.
     * @param embeddingStore    O store onde os embeddings dos documentos estão
     *                          armazenados.
     */
    public RagQueryService(ChatLanguageModel chatLanguageModel, EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore) {
        logger.info(
                ">>>>>> RagQueryService - parametrizacao do modelo busca 3 chunks mais relevantes com score minimo de similaridade.");
        // Configuração do ContentRetriever como na sua classe original
        this.contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3) // Mantido o valor da sua implementação
                .minScore(0.75) // Mantido o valor da sua implementação
                .build();

        // Instancia o AiServices com RAG e memória de chat, usando a interface
        // Assistente
        this.assistant = AiServices.builder(Assistente.class)
                .chatLanguageModel(chatLanguageModel) // Define o modelo de chat a ser usado
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10)) // Adiciona memória de chat
                // Note: O contentRetriever é passado para o AiServices, permitindo que ele
                // gerencie a recuperação
                // antes de chamar o LLM, usando o @SystemMessage com {context}.
                .contentRetriever(this.contentRetriever)
                .build();
    }

    /**
     * Consulta o modelo RAG com uma pergunta do usuário e retorna uma resposta.
     *
     * @param userQuery A pergunta do usuário.
     * @return A resposta gerada pelo LLM com base nas informações recuperadas.
     */
    /**
     * Consulta o modelo RAG com uma pergunta do usuário e retorna uma resposta.
     *
     * @param userQuery A pergunta do usuário.
     * @return A resposta gerada pelo LLM com base nas informações recuperadas.
     */
    public String queryModel(String userQuery) {
        try {
            logger.info(">>>>>> RagQueryService - Recebida consulta do usuário: " + userQuery);

            // O contentRetriever foi configurado no AiServices (no construtor),
            // então o LangChain4j busca automaticamente o contexto relevante
            // e o injeta no prompt antes de chamar o LLM.
            return assistant.chat(userQuery);

        } catch (Exception e) {
            logger.error(
                    ">>>>>> RagQueryService - Ocorreu um erro durante a consulta do modelo RAG: " + e.getMessage());
            e.printStackTrace();
            return "Desculpe, ocorreu um erro ao processar sua consulta.";
        }
    }

}
