package com.fatec.easy_rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RAG semantico com filtragem de metadados
 * usa vetores para achar similaridade mas aplica um filtro rigido para
 * selecionar os documentos
 */
@SpringBootApplication
public class EasyRagApplication {

	public static void main(String[] args) {
		SpringApplication.run(EasyRagApplication.class, args);
	}

}
