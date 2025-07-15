package com.fatec.easy_rag.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever; // Usando a implementação específica
import dev.langchain4j.rag.query.Query;
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
     * @param chatLanguageModel O modelo de linguagem de chat para gerar respostas.
     * @param embeddingModel O modelo de embedding usado para consultas.
     * @param embeddingStore O store onde os embeddings dos documentos estão armazenados.
     */
    public RagQueryService(ChatLanguageModel chatLanguageModel, EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        logger.info(">>>>>> RagQueryService - parametrizacao do modelo busca 3 chunks mais relevantes com score minimo de similaridade.");
        // Configuração do ContentRetriever como na sua classe original
        this.contentRetriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(3) // Mantido o valor da sua implementação
            .minScore(0.75) // Mantido o valor da sua implementação
            .build();

        // Instancia o AiServices com RAG e memória de chat, usando a interface Assistente
        this.assistant = AiServices.builder(Assistente.class)
                .chatLanguageModel(chatLanguageModel) // Define o modelo de chat a ser usado
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10)) // Adiciona memória de chat
                // Note: O contentRetriever é passado para o AiServices, permitindo que ele gerencie a recuperação
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
    public String queryModel(String userQuery) {
        try {
            logger.info(">>>>>> RagQueryService - Recebida consulta do usuário: " + userQuery);

            // O AiServices com ContentRetriever configurado já cuida da recuperação do contexto
            // e da sua injeção no SystemMessage.
            // A chamada para assistant.chat(userQuery) agora é mais limpa.
            //String response = assistant.chat(userQuery, ""); // O segundo parâmetro é o contexto, que será preenchido pelo AiServices
            String response = chatWithAssistant(userQuery);
            // Sua lógica original de verificar se o conteúdo relevante está vazio
            // foi movida para o @SystemMessage da interface Assistente.
            // Se o LLM não encontrar resposta com o contexto, ele seguirá a instrução do SystemMessage.
            return response;

        } catch (Exception e) {
            logger.error(">>>>>> RagQueryService - Ocorreu um erro durante a consulta do modelo RAG: " + e.getMessage());
            e.printStackTrace();
            return "Desculpe, ocorreu um erro ao processar sua consulta.";
        }
    }
 // Método para avaliar o resultado da interacao e responder
 	public String chatWithAssistant(String userMessage) {
 		logger.info(">>>>>> RagQueryService - avaliar o resultado da interacao e responder. ");
 		// 1. Recuperar o contexto
 		List<Content> relevantContents = contentRetriever.retrieve(Query.from(userMessage));

 		// 2. Verificar se algum conteúdo relevante foi recuperado
 		if (relevantContents.isEmpty()) {
 			// Se nenhum conteúdo relevante foi encontrado, retornar a mensagem padrão
 			return "Não consigo responder a esta pergunta com as informações disponíveis no documento.";
 		} else {
 			// Se houver conteúdo relevante, construir o prompt com o contexto
 			String context = relevantContents.stream().map(content -> content.textSegment().text())
 					.collect(java.util.stream.Collectors.joining("\n\n"));

 			// 3. Chamar o LLM com o contexto e a pergunta
 			// Aqui, a SystemMessage na interface Assistant ainda é importante para guiar o
 			// LLM
 			// a usar o contexto fornecido e não "alucinar" mesmo quando o contexto é dado.
 			return assistant.chat(userMessage + "\n\nContexto: " + context);
 		}
 	}

}
