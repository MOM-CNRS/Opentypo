package fr.cnrs.opentypo.presentation.bean.candidats;

import fr.cnrs.opentypo.domain.entity.Label;
import org.springframework.data.repository.Repository;

import java.util.List;

interface LabelRepository extends Repository<Label, Long> {

    /**
     * Retourne tous les labels associés à une entité à partir de son ID.
     */
    List<Label> findByEntity_Id(Long entityId);
}
