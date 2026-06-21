package fr.cnrs.opentypo.application.dto.api;

/**
 * Messages d'erreur en français pour l'API REST.
 */
public final class ApiErrorMessages {

    public static final String VALIDATION_FAILED = "Échec de la validation";
    public static final String UNAUTHORIZED = "Non authentifié";
    public static final String INVALID_CREDENTIALS = "Identifiants incorrects";
    public static final String INVALID_TOKEN = "Jeton invalide ou expiré";
    public static final String ACCOUNT_DISABLED = "Compte désactivé";
    public static final String FORBIDDEN = "Accès refusé";
    public static final String CREATE_REFERENTIEL_ADMIN_ONLY =
            "Seuls les administrateurs technique et fonctionnel peuvent créer un référentiel ou une collection";
    public static final String CREATE_ENTITY_FORBIDDEN =
            "Droits insuffisants pour créer cette entité : administrateur, gestionnaire du référentiel "
                    + "ou rédacteur du groupe requis";
    public static final String CREATE_PARENT_REQUIRED =
            "L'identifiant du parent est obligatoire pour créer ce type d'entité";
    public static final String DELETE_REFERENTIEL_ADMIN_ONLY =
            "Seuls les administrateurs technique et fonctionnel peuvent supprimer un référentiel ou une collection";
    public static final String DELETE_ENTITY_FORBIDDEN =
            "Droits insuffisants pour supprimer cette entité : administrateur, gestionnaire du référentiel "
                    + "ou rédacteur du groupe requis";
    public static final String UPDATE_REFERENCE_FORBIDDEN =
            "Droits insuffisants pour modifier ce référentiel";
    public static final String UPDATE_COLLECTION_FORBIDDEN =
            "Droits insuffisants pour modifier cette collection";
    public static final String UPDATE_ENTITY_FORBIDDEN =
            "Droits insuffisants pour modifier cette entité : administrateur, gestionnaire du référentiel ou rédacteur du groupe requis";
    public static final String VISIBILITY_CHANGE_FORBIDDEN =
            "Droits insuffisants pour modifier la visibilité de cette entité";
    public static final String VISIBILITY_PROPOSITION_FORBIDDEN =
            "Impossible de changer la visibilité d'une entité au statut PROPOSITION : validez-la d'abord";
    public static final String ENTITY_NOT_FOUND = "Entité introuvable";
    public static final String CODE_REQUIRED = "Le paramètre code est obligatoire";
    public static final String VALUE_REQUIRED = "Le paramètre value est obligatoire";
    public static final String FIELD_MUST_BE_CODE_OR_LABEL = "Le paramètre field doit valoir CODE ou LABEL";
    public static final String MATCH_MUST_BE_EXACT_OR_CONTAINS = "Le paramètre match doit valoir EXACT ou CONTAINS";
    public static final String LOOKUP_PARAMS_REQUIRED = "Les paramètres field, match et value sont obligatoires en mode recherche ciblée";
    public static final String LOOKUP_PARAMS_INCOMPLETE =
            "Les paramètres field, match et value doivent être fournis ensemble pour une recherche";
    public static final String REFERENTIEL_OR_PARENT_EXCLUSIVE =
            "Utilisez soit referentielId, soit parentId, pas les deux";
    public static final String CODE_ALREADY_EXISTS = "Une entité avec ce code existe déjà";
    public static final String CATEGORY_CODE_EXISTS_IN_REFERENCE =
            "Une catégorie avec ce code existe déjà dans ce référentiel";
    public static final String GROUP_CODE_EXISTS_IN_REFERENCE =
            "Un groupe avec ce code existe déjà dans ce référentiel";
    public static final String SERIE_CODE_EXISTS_IN_GROUP =
            "Une série avec ce code existe déjà dans ce groupe";
    public static final String TYPE_CODE_EXISTS_IN_GROUP =
            "Un type avec ce code existe déjà dans ce groupe";
    public static final String LIMIT_MIN_ONE = "Le paramètre limit doit être au moins égal à 1";
    public static final String BAD_REQUEST = "Requête invalide";
    public static final String NOT_FOUND = "Ressource introuvable";
    public static final String CONFLICT = "Conflit";
    public static final String INTERNAL_ERROR = "Erreur interne du serveur";

    private ApiErrorMessages() {
    }

    public static String unknownLanguageCode(String code) {
        return "Code langue inconnu : " + code;
    }

    public static String unknownEntityType(String typeCode) {
        return "Type d'entité inconnu : " + typeCode;
    }

    public static String invalidStatutFilter(String value) {
        return "Valeur de statut invalide : « " + value
                + " ». Utilisez PUBLIQUE, PROPOSITION, REFUSED ou tous";
    }

    public static String invalidListOrder(String value) {
        return "Valeur de order invalide : « " + value + " ». Utilisez date_desc, date, code ou code_desc";
    }

    public static String parentEntityNotFound(Long parentId) {
        return "Entité parente introuvable : " + parentId;
    }

    public static String entityNotFound(Long id) {
        return "Entité introuvable : " + id;
    }

    public static String httpStatusMessage(int status) {
        return switch (status) {
            case 400 -> BAD_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 409 -> CONFLICT;
            case 500 -> INTERNAL_ERROR;
            default -> "Erreur HTTP " + status;
        };
    }

    /** Corps JSON {@link ApiErrorResponse} pour les réponses d'erreur manuelles (sécurité). */
    public static String toJson(int status, String error) {
        String safeError = error == null ? "" : error.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"status\":" + status + ",\"error\":\"" + safeError + "\",\"details\":null}";
    }
}
