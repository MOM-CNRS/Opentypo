package fr.cnrs.opentypo.presentation.rest;

import fr.cnrs.opentypo.application.dto.api.ApiErrorResponse;
import fr.cnrs.opentypo.application.dto.api.EntityApiStatutFilter;
import fr.cnrs.opentypo.application.dto.api.EntityCreateRequest;
import fr.cnrs.opentypo.application.dto.api.EntityListOrder;
import fr.cnrs.opentypo.application.dto.api.EntityResponseDto;
import fr.cnrs.opentypo.application.dto.api.EntityUpdateRequest;
import fr.cnrs.opentypo.application.dto.api.EntityVisibilityRequest;
import fr.cnrs.opentypo.application.service.EntityApiService;
import fr.cnrs.opentypo.infrastructure.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import java.util.List;

/**
 * REST API for typology {@link fr.cnrs.opentypo.domain.entity.Entity} resources.
 */
@Tag(
        name = "Entités typologiques",
        description = "Recherche, création, mise à jour partielle et suppression (cascade) des entités typologiques.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
@RestController
@RequestMapping(path = "/api/v1/entities", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class EntityRestController {

    private final EntityApiService entityApiService;

    @Operation(
            operationId = "listEntities",
            summary = "Lister ou rechercher des entités",
            description = """
                    Recherche sur le **code métier** (`field=CODE`) ou le **libellé** (`field=LABEL`, avec `lang`, défaut fr).
                    `match` : `EXACT` ou `CONTAINS`. `value` : texte recherché.
                    Sans `field`/`match`/`value` : liste paginée filtrée par `statut` uniquement.
                    **statut** : `PUBLIQUE`, `PROPOSITION`, `REFUSED` (stocké REFUSE), ou `tous` (tous les statuts).
                    **order** : `date_desc` (défaut), `date`, `code`, `code_desc`.
                    **limit** : défaut 200, max 1000.""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste d'entités correspondantes (vide si aucun résultat).",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = EntityResponseDto.class)))),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides.", content = @Content(
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
    public List<EntityResponseDto> listEntities(
            @Parameter(
                    name = "field",
                    in = ParameterIn.QUERY,
                    description = "`CODE` ou `LABEL`.",
                    example = "CODE",
                    schema = @Schema(allowableValues = {"CODE", "LABEL"}))
            @RequestParam(required = false) String field,
            @Parameter(
                    name = "match",
                    in = ParameterIn.QUERY,
                    description = "`EXACT` ou `CONTAINS`.",
                    example = "EXACT",
                    schema = @Schema(allowableValues = {"EXACT", "CONTAINS"}))
            @RequestParam(required = false) String match,
            @Parameter(
                    name = "value",
                    in = ParameterIn.QUERY,
                    description = "Texte recherché.",
                    example = "CER")
            @RequestParam(required = false) String value,
            @Parameter(
                    name = "statut",
                    in = ParameterIn.QUERY,
                    description = "Filtre par statut métier.",
                    example = "PUBLIQUE",
                    schema = @Schema(allowableValues = {"PUBLIQUE", "PROPOSITION", "REFUSED", EntityApiStatutFilter.VALUE_TOUS}))
            @RequestParam(required = false) String statut,
            @Parameter(
                    name = "lang",
                    in = ParameterIn.QUERY,
                    description = "Code langue pour field=LABEL (défaut fr).",
                    example = "fr")
            @RequestParam(required = false) String lang,
            @Parameter(
                    name = "limit",
                    in = ParameterIn.QUERY,
                    description = "Nombre max de résultats (défaut 200, max 1000).",
                    example = "200")
            @RequestParam(required = false) Integer limit,
            @Parameter(
                    name = "order",
                    in = ParameterIn.QUERY,
                    description = "Tri des résultats.",
                    example = EntityListOrder.DEFAULT_VALUE,
                    schema = @Schema(allowableValues = {"date_desc", "date", "code", "code_desc"}))
            @RequestParam(required = false) String order) {
        return entityApiService.listEntities(field, match, value, statut, lang, limit, order);
    }

    @Operation(
            operationId = "getEntityByCode",
            summary = "Obtenir une entité par code métier",
            description = "Ressource unitaire par code métier (insensible à la casse), avec détail complet.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Entité trouvée.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EntityResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Code vide.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Entité introuvable.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/by-code/{code}")
    public EntityResponseDto getEntityByCode(
            @Parameter(
                    name = "code",
                    in = ParameterIn.PATH,
                    description = "Code métier unique.",
                    required = true,
                    example = "DECOCER")
            @PathVariable String code) {
        return entityApiService.getByCode(code);
    }

    @Operation(
            operationId = "getEntityChildren",
            summary = "Lister les enfants directs d'une entité",
            description = "Enfants directs, ordonnés par display_order puis code. Filtre **statut** optionnel.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des enfants (vide si aucun).",
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
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Entité parente introuvable.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}/children")
    public List<EntityResponseDto> getEntityChildren(
            @Parameter(
                    name = "id",
                    in = ParameterIn.PATH,
                    required = true,
                    schema = @Schema(type = "integer", format = "int64"))
            @PathVariable Long id,
            @Parameter(
                    name = "statut",
                    in = ParameterIn.QUERY,
                    schema = @Schema(allowableValues = {"PUBLIQUE", "PROPOSITION", "REFUSED", EntityApiStatutFilter.VALUE_TOUS}))
            @RequestParam(required = false) String statut) {
        return entityApiService.getChildren(id, EntityApiStatutFilter.parse(statut));
    }

    @Operation(
            operationId = "getEntityParents",
            summary = "Lister les parents directs d'une entité",
            description = "Parents directs dans l'arbre typologique. Filtre **statut** optionnel.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des parents (vide si aucun).",
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
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Entité introuvable.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}/parents")
    public List<EntityResponseDto> getEntityParents(
            @Parameter(
                    name = "id",
                    in = ParameterIn.PATH,
                    required = true,
                    schema = @Schema(type = "integer", format = "int64"))
            @PathVariable Long id,
            @Parameter(
                    name = "statut",
                    in = ParameterIn.QUERY,
                    schema = @Schema(allowableValues = {"PUBLIQUE", "PROPOSITION", "REFUSED", EntityApiStatutFilter.VALUE_TOUS}))
            @RequestParam(required = false) String statut) {
        return entityApiService.getParents(id, EntityApiStatutFilter.parse(statut));
    }

    @Operation(
            operationId = "getEntityById",
            summary = "Obtenir une entité par identifiant",
            description = "Détail complet incluant rubriques Description, Caractéristiques physiques et descriptions de présentation.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Entité trouvée.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EntityResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Entité introuvable.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public EntityResponseDto getEntity(
            @Parameter(
                    name = "id",
                    in = ParameterIn.PATH,
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
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflit : un code d'entité identique existe déjà.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
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
            description = "Style RFC 5789 : seuls les champs **non null** du corps sont appliqués. "
                    + "La publication (`PUBLIQUE`) peut déclencher l'attribution automatique d'un ARK si absent.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Entité mise à jour.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EntityResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Entité introuvable.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflit : le nouveau code est déjà utilisé par une autre entité.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EntityResponseDto patchEntity(
            @Parameter(
                    name = "id",
                    in = ParameterIn.PATH,
                    required = true,
                    schema = @Schema(type = "integer", format = "int64"))
            @PathVariable Long id,
            @RequestBody EntityUpdateRequest request) {
        return entityApiService.update(id, request);
    }

    @Operation(
            operationId = "updateEntityVisibility",
            summary = "Modifier la visibilité (publique / privée)",
            description = """
                    Bascule le statut de visibilité de l'entité :
                    - `publique: true` → statut **PUBLIQUE** ;
                    - `publique: false` → statut **PRIVEE**.
                    Une entité au statut **PROPOSITION** ne peut pas être modifiée via cet endpoint.
                    Le passage en **PUBLIQUE** peut déclencher l'attribution automatique d'un ARK si absent.""",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EntityVisibilityRequest.class))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Visibilité mise à jour.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EntityResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide ou statut PROPOSITION.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Droits insuffisants.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Entité introuvable.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping(path = "/{id}/visibility", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EntityResponseDto updateEntityVisibility(
            @Parameter(
                    name = "id",
                    in = ParameterIn.PATH,
                    required = true,
                    schema = @Schema(type = "integer", format = "int64"))
            @PathVariable Long id,
            @Valid @RequestBody EntityVisibilityRequest request) {
        return entityApiService.updateVisibility(id, request);
    }

    @Operation(
            operationId = "deleteEntity",
            summary = "Supprimer une entité (cascade)",
            description = "Suppression récursive alignée sur l'interface (descendants et relations). Réponse sans corps.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Suppression effectuée."),
            @ApiResponse(responseCode = "401", description = "Non authentifié.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Entité introuvable.", content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntity(
            @Parameter(
                    name = "id",
                    in = ParameterIn.PATH,
                    required = true,
                    schema = @Schema(type = "integer", format = "int64"))
            @PathVariable Long id) {
        entityApiService.delete(id);
    }
}
