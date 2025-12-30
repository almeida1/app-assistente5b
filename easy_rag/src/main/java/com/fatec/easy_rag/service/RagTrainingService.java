package com.fatec.easy_rag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser; // Adicionado para parsing explícito
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor; // Adicionado para ingestão simplificada

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service; // Mantido para consistência, embora não seja um @Service direto aqui

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo treinamento do modelo RAG,
 * incluindo carregamento de documentos, extração de metadados,
 * criação de embeddings e ingestão no EmbeddingStore.
 */
@Service
public class RagTrainingService {

    private static final Logger logger = LogManager.getLogger(RagTrainingService.class);

    private final EmbeddingStoreIngestor embeddingStoreIngestor;

    public RagTrainingService(EmbeddingStoreIngestor embeddingStoreIngestor) {
        this.embeddingStoreIngestor = embeddingStoreIngestor;
    }

    /**
     * Treina o modelo RAG carregando documentos de um caminho,
     * extraindo metadados, criando embeddings e os armazenando.
     *
     * @param documentsPath O caminho para o arquivo ou diretório contendo os
     *                      documentos.
     * @return Uma mensagem de sucesso ou falha.
     */
    public String trainModel(Path documentsPath) {
        try {
            logger.info(">>>>>> RagTrainingService - inicia a preparação da base de conhecimento.");

            // 2. Carregar os documentos do sistema de arquivos
            // Usamos TextDocumentParser para garantir que o conteúdo seja tratado como
            // texto.
            logger.info(">>>>>> RagTrainingService - Carrega todos os arquivos armazenados neste path.");
            List<Document> loadedDocuments = FileSystemDocumentLoader.loadDocuments(documentsPath,
                    new TextDocumentParser());

            if (loadedDocuments.isEmpty()) {
                return "Nenhum documento encontrado no caminho: " + documentsPath;
            }

            logger.info(">>>>>> RagTrainingService documentos carregados: " + loadedDocuments.size());

            // 3. Processar cada documento para extrair metadados e criar um novo Document
            logger.info(">>>>>> RagTrainingService - Armazena documentos com metadados.");
            List<Document> processedDocuments = loadedDocuments.stream()
                    .map(this::processDocumentWithMetadata)
                    .collect(Collectors.toList());

            logger.info("RagTrainingService documentos processados com metadados:");
            for (Document doc : processedDocuments) {
                logger.debug("  Conteúdo: \"{}...\"", doc.text().substring(0, Math.min(doc.text().length(), 100)));
                logger.debug("  Metadados: {}", doc.metadata());
            }

            // 4. Ingestão dos documentos usando EmbeddingStoreIngestor
            // O Ingestor cuida de dividir (split), gerar embeddings e salvar no store.
            logger.info(">>>>>> RagTrainingService - Iniciando ingestão no EmbeddingStore.");

            // 4. Ingestão dos documentos usando EmbeddingStoreIngestor
            // O Ingestor já foi configurado no LangChainConfig e injetado aqui.
            logger.info(">>>>>> RagTrainingService - Iniciando ingestão no EmbeddingStore.");

            embeddingStoreIngestor.ingest(processedDocuments);

            logger.info(">>>>>> RagTrainingService - Treinamento concluído. Documentos ingeridos: "
                    + processedDocuments.size());
            return "Treinamento do modelo RAG concluído com sucesso. Documentos processados: "
                    + processedDocuments.size();

        } catch (Exception e) {
            logger.error(">>>>>> RagTrainingService - Erro inesperado: " + e.getMessage(), e);
            return "Falha no treinamento: " + e.getMessage();
        }
    }

    /**
     * Processa um documento existente para adicionar metadados personalizados.
     *
     * @param originalDocument O documento original carregado.
     * @return Um novo Document com os metadados adicionados.
     */
    private Document processDocumentWithMetadata(Document originalDocument) {
        // Extrair metadados do texto do documento
        Map<String, String> extractedMetadataMap = extrairMetadados(originalDocument.text());
        Metadata newMetadata = new Metadata(extractedMetadataMap);

        // Retornar um novo Document com o texto original e os novos metadados
        return Document.from(originalDocument.text(), newMetadata);
    }

    /**
     * Extrai metadados de uma string de texto.
     * Esta é a função fornecida pelo usuário, adaptada para o exemplo.
     *
     * @param texto O texto do qual extrair os metadados.
     * @return Um mapa de strings contendo os metadados extraídos.
     */
    private Map<String, String> extrairMetadados(String texto) {
        return Map.of(
                "fonte", "ISTQB CTAL-TA Syllabus v4.0",
                "ano", "2025",
                "tipo", "Referência Técnica",
                "nivel", "Avançado",
                "papel", texto.contains("Test Analyst") ? "Test Analyst" : "Outro",
                "topico", texto.contains("Risk-Based Testing") ? "Risk-Based Testing" : "Processo de Teste");
    }
}
