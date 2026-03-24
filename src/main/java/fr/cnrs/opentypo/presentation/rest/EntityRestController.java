package fr.cnrs.opentypo.presentation.rest;

import fr.cnrs.opentypo.application.dto.api.EntityCreateRequest;
import fr.cnrs.opentypo.application.dto.api.EntityResponseDto;
import fr.cnrs.opentypo.application.dto.api.EntitySearchCriteria;
import fr.cnrs.opentypo.application.dto.api.EntityUpdateRequest;
import fr.cnrs.opentypo.application.service.EntityApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST API for typology {@link fr.cnrs.opentypo.domain.entity.Entity} resources.
 * <p>
 * Conventions: plural collection path {@code /api/v1/entities}, numeric id in path for item resource,
 * {@code GET} on collection with query parameters for filtering (no RPC-style subpaths),
 * {@code POST} returns {@code 201 Created} with {@code Location}, partial updates via {@code PATCH},
 * {@code DELETE} returns {@code 204 No Content}.
 * </p>
 */
@Tag(
        name = "Entités typologiques",
        description = "Recherche, liste, création, mise à jour partielle et suppression (cascade) des entités "
                + "de la typologie CNRS OpenTypo. Authentification par session HTTP (cookie `JSESSIONID`), "
                + "alignée sur l'interface JSF.")
@SecurityRequirement(name = "sessionCookie")
@RestController
@RequestMapping(path = "/api/v1/entities", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class EntityRestController {

    private final EntityApiService entityApiService;

    @Operation(
            operationId = "searchEntities",
            summary = "Rechercher ou lister des entités",
            description = "Filtres combinés avec une conjonction **ET**. Tous les critères sont optionnels. "
                    + "Pagination et tri via les paramètres Spring Data (`page`, `size`, `sort`). "
                    + "Tri par défaut : `createDate` décroissant (20 éléments par page).")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Page Spring Data (`content`, `totalElements`, `number`, `size`, etc.) contenant des `EntityResponseDto`."),
            @ApiResponse(responseCode = "401", description = "Non authentifié (pas de session valide).", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé.", content = @Content)
    })
    @GetMapping
    public Page<EntityResponseDto> getEntities(
            @Parameter(
                    name = "typeCode",
                    in = ParameterIn.QUERY,
                    description = "Code du type d'entité (ex. TYPE, SERIE, REFERENCE).",
                    example = "TYPE")
            @RequestParam(required = false) String typeCode,
            @Parameter(
                    name = "statut",
                    in = ParameterIn.QUERY,
                    description = "Statut métier : PROPOSITION, PUBLIQUE, PRIVEE, REFUSE.",
                    example = "PUBLIQUE")
            @RequestParam(required = false) String statut,
            @Parameter(
                    name = "code",
                    in = ParameterIn.QUERY,
                    description = "Code métier exact (égalité).",
                    example = "DECOCER")
            @RequestParam(required = false) String code,
            @Parameter(
                    name = "codeContains",
                    in = ParameterIn.QUERY,
                    description = "Sous-chaîne dans le code (recherche « contient »).")
            @RequestParam(required = false) String codeContains,
            @Parameter(
                    name = "idArk",
                    in = ParameterIn.QUERY,
                    description = "Identifiant ARK (égalité).")
            @RequestParam(required = false) String idArk,
            @Parameter(
                    name = "q",
                    in = ParameterIn.QUERY,
                    description = "Texte libre : recherche dans le code ou les libellés (selon `labelLang` si fourni).")
            @RequestParam(required = false) String q,
            @Parameter(
                    name = "labelLang",
                    in = ParameterIn.QUERY,
                    description = "Code langue ISO (ex. fr, en) : restreint la partie « libellés » de la recherche `q`.",
                    example = "fr")
            @RequestParam(required = false) String labelLang,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable) {
        EntitySearchCriteria criteria = EntitySearchCriteria.normalize(
                typeCode, statut, code, codeContains, idArk, q, labelLang);
        return entityApiService.search(criteria, pageable);
    }

    @Operation(
            operationId = "getEntityById",
            summary = "Obtenir une entité par identifiant",
            description = "Ressource unitaire : détail d'une entité par clé primaire numérique.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Entité trouvée.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EntityResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Entité introuvable.", content = @Content)
    })
    @GetMapping("/{id}")
    public EntityResponseDto getEntity(
            @Parameter(
                    name = "id",
                    in = ParameterIn.PATH,
                    description = "Identifiant technique de l'entité.",
                    required = true,
                    example = "42",
                    schema = @Schema(type = "integer", format = "int64"))
            @PathVariable Long id) {
        return entityApiService.getById(id);
    }

    @Operation(
            operationId = "createEntity",
            summary = "Créer une entité",
            description = "Crée une entité avec métadonnées et un libellé principal. Statut par défaut `PUBLIQUE` si omis. "
                    + "Optionnellement rattache l'entité à un parent (arbre typologique).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Code unique, type d'entité, libellé principal, langue du libellé, statut optionnel, parent optionnel.",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EntityCreateRequest.class))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Entité créée. L'en-tête `Location` pointe vers `GET /api/v1/entities/{id}`.",
                    headers = @Header(
                            name = "Location",
                            description = "URI absolue de la ressource créée.",
                            schema = @Schema(type = "string", format = "uri")),
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EntityResponseDto.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requête invalide : type ou langue inconnue, parent introuvable, validation Bean Validation.",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Non authentifié.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé.", content = @Content),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflit : un code d'entité identique existe déjà.",
                    content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityResponseDto> createEntity(
            @Valid @RequestBody EntityCreateRequest request) {
        EntityResponseDto body = entityApiService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/entities/{id}")
                .buildAndExpand(body.id())
                .toUri();
        return ResponseEntity.created(location).body(body);
    }

    @Operation(
            operationId = "patchEntity",
            summary = "Mise à jour partielle d'une entité",
            description = "Style RFC 5789 : seuls les champs **non null** du corps sont appliqués.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Champs optionnels : statut, code, idArk, ordre d'affichage, appellation, bibliographie, commentaire métadonnées.",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EntityUpdateRequest.class))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Entité mise à jour.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EntityResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide.", content = @Content),
            @ApiResponse(responseCode = "401", description = "Non authentifié.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Entité introuvable.", content = @Content),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflit : le nouveau code est déjà utilisé par une autre entité.",
                    content = @Content)
    })
    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EntityResponseDto patchEntity(
            @Parameter(
                    name = "id",
                    in = ParameterIn.PATH,
                    description = "Identifiant technique de l'entité à modifier.",
                    required = true,
                    schema = @Schema(type = "integer", format = "int64"))
            @PathVariable Long id,
            @RequestBody EntityUpdateRequest request) {
        return entityApiService.update(id, request);
    }

    @Operation(
            operationId = "deleteEntity",
            summary = "Supprimer une entité (cascade)",
            description = "Suppression récursive alignée sur l'interface (descendants et relations). Réponse sans corps.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Suppression effectuée."),
            @ApiResponse(responseCode = "401", description = "Non authentifié.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Entité introuvable.", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntity(
            @Parameter(
                    name = "id",
                    in = ParameterIn.PATH,
                    description = "Identifiant technique de l'entité à supprimer.",
                    required = true,
                    schema = @Schema(type = "integer", format = "int64"))
            @PathVariable Long id) {
        entityApiService.delete(id);
    }
}
