package com.fatec.easy_rag.service;

import dev.langchain4j.service.SystemMessage;

/**
 * Define a interface para o assistente de IA.
 * O LangChain4j cria automaticamente uma implementação dessa interface (Proxy).
 */
public interface Assistente {

    @SystemMessage({
            "Você é um assistente prestativo. Use as infos fornecidas para responder.",
            "Baseie-se EXCLUSIVAMENTE no contexto fornecido pelo sistema.",
            "Se a informação não for suficiente, diga que não sabe a resposta.",
            "É CRÍTICO que você NÃO use seu conhecimento pré-treinado externo."
    })
    String chat(String userMessage);
}
