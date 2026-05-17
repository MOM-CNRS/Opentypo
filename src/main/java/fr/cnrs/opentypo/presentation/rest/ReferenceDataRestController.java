package fr.cnrs.opentypo.presentation.rest;

import fr.cnrs.opentypo.application.dto.api.ApiErrorResponse;
import fr.cnrs.opentypo.application.dto.api.EntityTypeDto;
import fr.cnrs.opentypo.application.dto.api.LangueDto;
import fr.cnrs.opentypo.application.service.ReferenceDataApiService;
import fr.cnrs.opentypo.infrastructure.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Données de référence en lecture seule pour l'API REST.
 */
@Tag(name = "Données de référence", description = "Types d'entité et langues supportées.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ReferenceDataRestController {

    private final ReferenceDataApiService referenceDataApiService;

    @Operation(
            operationId = "listEntityTypes",
            summary = "Lister les types d'entité",
            description = "Codes de la hiérarchie typologique : COLLECTION, REFERENTIEL, CATEGORIE, GROUPE, SERIE, TYPE.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des types.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = EntityTypeDto.class)))),
            @ApiResponse(responseCode = "401", description = "Non authentifié.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/entity-types")
    public List<EntityTypeDto> listEntityTypes() {
        return referenceDataApiService.listEntityTypes();
    }

    @Operation(
            operationId = "listLanguages",
            summary = "Lister les langues",
            description = "Langues disponibles pour les libellés multilingues, triées par nom.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des langues.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = LangueDto.class)))),
            @ApiResponse(responseCode = "401", description = "Non authentifié.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/languages")
    public List<LangueDto> listLanguages() {
        return referenceDataApiService.listLanguages();
    }
}
