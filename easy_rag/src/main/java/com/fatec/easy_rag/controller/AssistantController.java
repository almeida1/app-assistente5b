package com.fatec.easy_rag.controller;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fatec.easy_rag.service.ChatRequest;
import com.fatec.easy_rag.service.RagQueryService;
import com.fatec.easy_rag.service.DocumentIngestor;

@RestController
@RequestMapping("/api")
public class AssistantController {

	private final DocumentIngestor trainingService;
	private final RagQueryService queryService;
	private static final Logger logger = LogManager.getLogger(AssistantController.class);

	public AssistantController(DocumentIngestor trainingService, RagQueryService queryService) {
		this.trainingService = trainingService;
		this.queryService = queryService;
	}

	@PostMapping("/consultar")
	public String ask(@RequestBody ChatRequest request) {
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

		return queryService.queryModel(request.getQuestion());
	}

	@PostMapping("/upload")
	public ResponseEntity<String> treinamento() {
		Path documentsPath = Paths.get("C:/edson/2025 graduacao 2s/documents");
		String mensagem = trainingService.trainModel(documentsPath);
		return ResponseEntity.ok(mensagem);

	}

}
