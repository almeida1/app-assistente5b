package com.fatec.easy_rag.service;

public class ChatRequest {
    private String pergunta;

    // Construtor padrão (no-arg constructor) é **obrigatório** para que o Jackson
    // possa desserializar o JSON para um objeto Java.
    public ChatRequest() {
    }

    // Construtor com argumentos (opcional, mas útil para criar instâncias
    // programaticamente).
    public ChatRequest(String question) {
        this.pergunta = question;
    }

    // Getter para a propriedade 'question'.
    // O Jackson usa este getter para ler o valor da propriedade 'question' do JSON.
    public String getQuestion() {
        return pergunta;
    }

    // Setter para a propriedade 'question'.
    // O Jackson usa este setter para definir o valor da propriedade 'question' no
    // objeto Java
    // a partir do JSON recebido.
    public void setQuestion(String question) {
        this.pergunta = question;
    }

    // Sobrescrever toString() para facilitar a depuração.
    @Override
    public String toString() {
        return "QuestionRequest{" +
                "pergunta ='" + pergunta + '\'' +
                '}';
    }
}
