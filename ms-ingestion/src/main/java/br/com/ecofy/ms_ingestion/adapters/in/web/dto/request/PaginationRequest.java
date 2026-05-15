package br.com.ecofy.ms_ingestion.adapters.in.web.dto.request;

public record PaginationRequest(

        int page,
        int size

) {
}