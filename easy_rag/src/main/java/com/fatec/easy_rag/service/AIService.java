package com.fatec.easy_rag.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel; // Alterado de ChatLanguageModel para ChatModel
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;

//https://docs.langchain4j.dev/tutorials/rag/#rag-stages
@Service
public class AIService {
	private static final Logger logger = LogManager.getLogger(AIService.class);
	private Assistente assistant; // Anotações LangChain4j podem ser usadas para definir o comportamento do LLM.
	private EmbeddingModel embeddingModel;
	private EmbeddingStore<TextSegment> embeddingStore;
	private ContentRetriever contentRetriever; // responsavel por buscar e retornar pedacos de inf mais relevantes

	// O construtor injeta os componentes de configuracao
	// que foram definidos como Beans na classe LangChain4jConfig.
	public AIService(ChatLanguageModel chatLanguageModel,EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
		this.embeddingModel = embeddingModel;
		this.embeddingStore = embeddingStore;
		logger.info(">>>>>> AIService - parametrizacao do modelo busca 3 chunks mais relevantes com score minimo de similaridade.");
		this.contentRetriever = EmbeddingStoreContentRetriever.builder().embeddingStore(embeddingStore).embeddingModel(embeddingModel).maxResults(3).minScore(0.75).build();
		// Instancia o AiServices com RAG e memória de chat
		this.assistant = AiServices.builder(Assistente.class).chatLanguageModel(chatLanguageModel) // Define o modelo de chat a ser usado
				.chatMemory(MessageWindowChatMemory.withMaxMessages(10)) // Adiciona memória de chat
				.contentRetriever(EmbeddingStoreContentRetriever.builder() // Configura o retriever para RAG
						.embeddingStore(embeddingStore) // Usa o EmbeddingStore para buscar
						.embeddingModel(embeddingModel) // Usa o EmbeddingModel para embeddar a pergunta
						.build())
				.build();
	}

//	// garante que quando do aiservice estiver pronto para receber perguntas o embedding já estará populado.
//	@PostConstruct
//	public void init() {
//		try {
//			logger.info(">>>>>> AIService - inicia a preparação da base de conhecimento.");
//			loadAndIngestDocuments();
//		} catch (IOException e) {
//			logger.error(">>>>>> AIService - Erro ao carregar e ingerir documentos: " + e.getMessage());
//		}
//	}

	/*
	 * A biblioteca Apache Tika, que suporta uma varidade de tipos de documentos, é
	 * usada para detectar o tipo de documento e analisa-lo (parse). Essa
	 * dependencia é carregada no starter project do spring boot langchain4j. É
	 * possivel tambem filtrar os documentos: 
	 * PathMatcher pathMatcher =  FileSystems.getDefault().getPathMatcher("glob:*.pdf"); 
	 * List<Document> documents = FileSystemDocumentLoader.loadDocuments("/home/langchain4j/documentation", pathMatcher);
	 */
	private void loadAndIngestDocuments() throws IOException {
		Path documentsPath = Paths.get("e:/documents");
		// 1. Gera um arquivo de exemplo se o path nao existir
		logger.info(">>>>>> AIService - Verifica a existencia do path de documentos => " + documentsPath.toString());
		if (!Files.exists(documentsPath)) {
			Files.createDirectories(documentsPath);
			logger.info(">>>>>> AIService - Cria o diretorio e um exemplo de documentos: " + documentsPath);
			Files.writeString(documentsPath.resolve("exemplo.txt"),
					"O céu é azul e o mar é profundo. O sol brilha forte. A Terra é um planeta maravilhoso. A capital do Brasil é Brasília.");
			logger.info(">>>>>> Arquivo de exemplo 'exemplo.txt' criado.");
		}

		// 2. Extrai metadados

		logger.info(">>>>>> AIService - Carrega todos os arquivos neste path.");
		List<Document> documents = FileSystemDocumentLoader.loadDocuments(documentsPath);
		logger.info(">>>>>> AIService - Armazena documentos com metadados.");
		armazenaDocumentosComMetadados(documents);
		logger.info(">>>>>> AIService - Divide o documento em segumentos de texto (chunks) .");
		var documentSplitter = DocumentSplitters.recursive(500, 50);
		List<TextSegment> segments = documentSplitter.splitAll(documents);
		logger.info(">>>>>> AIService - Processa o arquivo para armazenar a informação em um banco de dados vetorial.");
		List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
		embeddingStore.addAll(embeddings, segments);
		// InMemoryEmbeddingStore<TextSegment> embeddignStore = new
		// InMemoryEmbeddingStore<>();
		// EmbeddingStoreIngestor.ingest(documents, embeddingStore);
		logger.info(">>>>>> AIService - Documentos carregados e embeddings criados/armazenados. Total de segmentos: "
				+ segments.size());
	}

	// Método para avaliar o resultado da interacao e responder
	public String chatWithAssistant(String userMessage) {
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

	private Map<String, String> extrairMetadados(String texto) {
		return Map.of("fonte", "ISTQB CTAL-TA Syllabus v4.0", "ano", "2025", "tipo", "Referência Técnica", "nivel",
				"Avançado", "papel", texto.contains("Test Analyst") ? "Test Analyst" : "Outro", "topico",
				texto.contains("Risk-Based Testing") ? "Risk-Based Testing" : "Processo de Teste");
	}

	private List<Document> armazenaDocumentosComMetadados(List<Document> loadedDocuments) {
		System.out.println("\nDocumentos carregados:");
		loadedDocuments.forEach(doc -> System.out
				.println("  Conteúdo: \"" + doc.text().substring(0, Math.min(doc.text().length(), 100)) + "...\""));

		// Lista para armazenar os novos documentos com metadados
		List<Document> processedDocuments = loadedDocuments.stream().map(doc -> {
			// Passo 2: Criar os metadados a partir do texto do documento
			Map<String, String> extractedMetadataMap = extrairMetadados(doc.text());
			Metadata metadata = new Metadata(extractedMetadataMap);

			// Passo 3: Criar um novo Document com o texto extraído e os metadados
			// O Document.from() cria um novo objeto Document com o texto e os metadados
			// fornecidos.
			return Document.from(doc.text(), metadata);
		}).collect(Collectors.toList());

		System.out.println("\nDocumentos processados com metadados:");
		processedDocuments.forEach(doc -> {
			System.out
					.println("  Conteúdo: \"" + doc.text().substring(0, Math.min(doc.text().length(), 100)) + "...\"");
			System.out.println("  Metadados: " + doc.metadata());
			System.out.println("  --------------------------------------");
		});
		return null;
	}
}