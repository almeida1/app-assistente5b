package com.fatec.easy_rag.controller;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.easy_rag.service.AIService;
import com.fatec.easy_rag.service.PerguntaRequisicao;
import com.fatec.easy_rag.service.RagQueryService;
import com.fatec.easy_rag.service.RagTrainingService;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

@RestController
@RequestMapping("/rag")
public class RagController {

	private final AIService aiService;
	private final RagTrainingService trainingService;
	private final RagQueryService queryService;
	private static final Logger logger = LogManager.getLogger(RagController.class);

	public RagController(AIService aiService, RagTrainingService trainingService, RagQueryService queryService) {
        this.aiService = aiService;
        this.trainingService = trainingService;
        this.queryService = queryService;
    }

	@PostMapping("/consultar")
	public String ask(@RequestBody PerguntaRequisicao request) {
		// --- Adicione este log para depuração ---
		logger.info(">>>>>> Controller - Requisição recebida: " + request);
		if (request != null) {
			logger.info(">>>>>> Controller - Pergunta extraída: " + request.getQuestion());
		} else {
			logger.info(">>>>>> Controller - Objeto QuestionRequest é nulo!");
		}
		// --- Fim do log de depuração ---

		if (request == null || request.getQuestion() == null) {
			// Lidar com o caso de requisição ou pergunta nula
			return "Erro: A pergunta não foi fornecida corretamente no corpo da requisição JSON.";
		}

		// return aiService.chatWithAssistant(request.getQuestion());
		return queryService.queryModel(request.getQuestion());
	}

	@GetMapping("/treinamento")
	public String treinamento() {
		Path documentsPath = Paths.get("C:/edson/2025 graduacao 2s/documents");
		return trainingService.trainModel(documentsPath);

	}

	
}
