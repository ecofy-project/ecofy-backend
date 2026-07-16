package br.com.ecofy.ms_ingestion.core.port.out;


import br.com.ecofy.ms_ingestion.core.domain.ImportJob;

public interface ParseOfxPort {

    /**
     * Faz o parse do OFX retornando transações válidas + erros por bloco.
     * Blocos inválidos NÃO derrubam o job; erros estruturais lançam exceção.
     */
    ParseResult parse(ImportJob job, String ofxContent);

}