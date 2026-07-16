package br.com.ecofy.ms_ingestion.core.port.out;


import br.com.ecofy.ms_ingestion.core.domain.ImportJob;

public interface ParseCsvPort {

    /**
     * Faz o parse do CSV retornando transações válidas + erros por linha.
     * Erros de linha NÃO derrubam o job; erros estruturais do arquivo lançam exceção.
     */
    ParseResult parse(ImportJob job, String csvContent);

}