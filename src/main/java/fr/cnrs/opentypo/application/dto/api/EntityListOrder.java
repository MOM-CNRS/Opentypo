package fr.cnrs.opentypo.application.dto.api;

import fr.cnrs.opentypo.domain.entity.Entity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Tri des listes d'entités pour {@code GET /api/v1/entities}.
 */
public enum EntityListOrder {

    DATE_DESC("date_desc", Sort.by(Sort.Direction.DESC, "createDate")),
    DATE_ASC("date", Sort.by(Sort.Direction.ASC, "createDate")),
    CODE_ASC("code", null),
    CODE_DESC("code_desc", null);

    public static final String DEFAULT_VALUE = "date_desc";

    private final String paramValue;
    private final Sort jpaSort;

    EntityListOrder(String paramValue, Sort jpaSort) {
        this.paramValue = paramValue;
        this.jpaSort = jpaSort;
    }

    public boolean usesJpaSort() {
        return jpaSort != null;
    }

    public Pageable toPageable(int limit) {
        return PageRequest.of(0, limit, jpaSort);
    }

    public static EntityListOrder parse(String value) {
        if (value == null || value.isBlank()) {
            return DATE_DESC;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (EntityListOrder order : values()) {
            if (order.paramValue.equals(normalized)) {
                return order;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                ApiErrorMessages.invalidListOrder(value.trim()));
    }

    public List<Entity> sortEntities(List<Entity> entities) {
        if (entities.isEmpty() || usesJpaSort()) {
            return entities;
        }
        Comparator<Entity> comparator = switch (this) {
            case CODE_ASC -> Comparator.comparing(
                    e -> safeCode(e), String.CASE_INSENSITIVE_ORDER);
            case CODE_DESC -> Comparator.comparing(
                    (Entity e) -> safeCode(e), String.CASE_INSENSITIVE_ORDER).reversed();
            default -> null;
        };
        if (comparator == null) {
            return entities;
        }
        return entities.stream().sorted(comparator).toList();
    }

    private static String safeCode(Entity entity) {
        return entity.getCode() != null ? entity.getCode() : "";
    }
}
