package com.fatec.easy_rag.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

// Define a interface para o assistente de IA.
// Anotações LangChain4j podem ser usadas para definir o comportamento do LLM.
public interface Assistente {

    // Define uma mensagem de sistema que o LLM sempre receberá como contexto inicial.
    // Isso guia o comportamento do assistente.
    @SystemMessage({"Você é um assistente prestativo. Use as informações fornecidas para responder às perguntas. Se a informação não for suficiente, diga que não sabe a resposta.",
    		"É CRÍTICO que você NÃO use seu conhecimento pré-treinado."})
    // Define o método de chat que recebe a mensagem do usuário.
    // A anotação @UserMessage indica que este é o prompt do usuário.
    // @V("userMessage") é um placeholder para a variável userMessage no prompt.
    String chat(@UserMessage("{{userMessage}}") @V("userMessage") String userMessage);
}
