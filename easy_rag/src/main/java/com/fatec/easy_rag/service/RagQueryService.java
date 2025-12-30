package com.fatec.easy_rag.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class RagQueryService {

    private final Logger logger = LogManager.getLogger(this.getClass());
    private final Assistente assistant;

    public RagQueryService(Assistente assistant) {
        this.assistant = assistant;
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
