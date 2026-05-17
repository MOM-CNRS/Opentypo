package fr.cnrs.opentypo.application.service;

import fr.cnrs.opentypo.application.dto.api.EntityTypeDto;
import fr.cnrs.opentypo.application.dto.api.LangueDto;
import fr.cnrs.opentypo.domain.entity.EntityType;
import fr.cnrs.opentypo.domain.entity.Langue;
import fr.cnrs.opentypo.infrastructure.persistence.EntityTypeRepository;
import fr.cnrs.opentypo.infrastructure.persistence.LangueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Read-only reference data for the REST API (types d'entité, langues).
 */
@Service
@RequiredArgsConstructor
public class ReferenceDataApiService {

    private final EntityTypeRepository entityTypeRepository;
    private final LangueRepository langueRepository;

    @Transactional(readOnly = true)
    public List<EntityTypeDto> listEntityTypes() {
        return entityTypeRepository.findAll().stream()
                .sorted(Comparator.comparing(EntityType::getCode, String.CASE_INSENSITIVE_ORDER))
                .map(t -> new EntityTypeDto(t.getId(), t.getCode()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LangueDto> listLanguages() {
        return langueRepository.findAllByOrderByNomAsc().stream()
                .map(ReferenceDataApiService::toLangueDto)
                .toList();
    }

    private static LangueDto toLangueDto(Langue langue) {
        return new LangueDto(langue.getCode(), langue.getNom());
    }
}
