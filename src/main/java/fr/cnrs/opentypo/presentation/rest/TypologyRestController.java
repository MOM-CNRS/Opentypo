package fr.cnrs.opentypo.presentation.rest;

import fr.cnrs.opentypo.application.dto.api.ApiErrorResponse;
import fr.cnrs.opentypo.application.dto.api.EntityApiStatutFilter;
import fr.cnrs.opentypo.application.dto.api.EntityResponseDto;
import fr.cnrs.opentypo.application.service.EntityApiService;
import fr.cnrs.opentypo.infrastructure.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Collections (typologies OpenTypo) exposées par l'API REST.
 */
@Tag(
        name = "Typologies",
        description = "Collections de typologie archéologique (niveau COLLECTION de la hiérarchie).")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
@RestController
@RequestMapping(path = "/api/v1/typologies", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TypologyRestController {

    private final EntityApiService entityApiService;

    @Operation(
            operationId = "listCollections",
            summary = "Lister les collections",
            description = """
                    Retourne les **collections** (entités de type COLLECTION).
                    Paramètre **statut** :
                    - absent ou `PUBLIQUE` (défaut) : collections publiques ;
                    - `PROPOSITION`, `REFUSED` : filtre sur ce statut ;
                    - `tous` : toutes les collections, tous statuts.
                    Tri : ordre d'affichage personnalisé puis code (comme l'interface).""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des collections (vide si aucune ne correspond).",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = EntityResponseDto.class)))),
            @ApiResponse(responseCode = "400", description = "Paramètre statut invalide.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    public List<EntityResponseDto> listCollections(
            @Parameter(
                    name = "statut",
                    in = ParameterIn.QUERY,
                    description = "Filtre par statut. Défaut : PUBLIQUE.",
                    example = "PUBLIQUE",
                    schema = @Schema(allowableValues = {"PUBLIQUE", "PROPOSITION", "REFUSED", EntityApiStatutFilter.VALUE_TOUS}))
            @RequestParam(required = false) String statut,
            @Parameter(
                    name = "limit",
                    in = ParameterIn.QUERY,
                    description = "Nombre maximum de collections retournées (défaut 500, max 5000).",
                    example = "500")
            @RequestParam(required = false) Integer limit) {
        EntityApiStatutFilter filter = EntityApiStatutFilter.parse(statut);
        return entityApiService.listCollections(filter, limit);
    }
}
