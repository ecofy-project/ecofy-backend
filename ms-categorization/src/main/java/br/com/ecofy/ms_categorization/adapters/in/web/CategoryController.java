package br.com.ecofy.ms_categorization.adapters.in.web;

import br.com.ecofy.ms_categorization.adapters.in.web.dto.request.CreateCategoryRequest;
import br.com.ecofy.ms_categorization.adapters.in.web.dto.response.CategoryResponse;
import br.com.ecofy.ms_categorization.core.application.command.CreateCategoryCommand;
import br.com.ecofy.ms_categorization.core.port.in.CreateCategoryUseCase;
import br.com.ecofy.ms_categorization.core.port.in.ListCategoriesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

// Centraliza os endpoints de criação e consulta de categorias.
@RestController
@RequestMapping(path = "/api/categorization/v1/categories", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Categories", description = "Gestão de categorias para categorização de transações")
@Slf4j
@RequiredArgsConstructor
public class CategoryController {

    private final CreateCategoryUseCase createUseCase;
    private final ListCategoriesUseCase listUseCase;

    // Registra uma categoria e retorna sua localização.
    @Operation(
            summary = "Cria uma nova categoria",
            description = """
                    Cria uma categoria ativa para uso no motor de regras de categorização.
                    O nome é obrigatório e não pode ser vazio.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria criada com sucesso",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido (ex.: nome vazio)"),
            @ApiResponse(responseCode = "409", description = "Categoria já existe (quando aplicável)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao criar categoria")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {

        log.debug("[CategoryController] - [create] -> Criando categoria name={} color={}",
                request.name(), request.color());

        var saved = createUseCase.create(new CreateCategoryCommand(request.name(), request.color()));

        log.info("[CategoryController] - [create] -> Categoria criada com sucesso categoryId={} name={}",
                saved.getId(), saved.getName());

        CategoryResponse body = toResponse(saved);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(body);
    }

    // Consulta as categorias disponíveis para categorização.
    @Operation(
            summary = "Lista categorias ativas",
            description = """
                    Retorna todas as categorias marcadas como ativas.
                    Útil para popular dropdowns e regras no painel.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno ao listar categorias")
    })
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list() {

        log.debug("[CategoryController] - [list] -> Listando categorias ativas");

        var result = listUseCase.listActive().stream()
                .map(CategoryController::toResponse)
                .toList();

        return ResponseEntity.ok(result);
    }

    private static CategoryResponse toResponse(br.com.ecofy.ms_categorization.core.domain.Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getColor(),
                c.isActive()
        );
    }
}
