package fr.cnrs.opentypo.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentypo.application.dto.LangueEnum;
import fr.cnrs.opentypo.application.dto.pactols.*;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.Optional;

/**
 * Service pour interagir avec l'API PACTOLS
 */
@Service
@Named("pactolsService")
@Slf4j
public class PactolsService {

    private static final String PACTOLS_BASE_URL = "https://pactols.frantiq.fr/openapi/v1";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PactolsService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Récupère la liste des thésaurus disponibles
     * @param selectedLangue Langue sélectionnée pour les labels
     * @return Liste des thésaurus
     */
    public List<PactolsThesaurus> getThesaurusList(String selectedLangue) {
        try {
            String url = PACTOLS_BASE_URL + "/thesaurus";
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, null, List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<PactolsThesaurus> thesaurusList = new ArrayList<>();
                
                for (Object item : response.getBody()) {
                    Map<String, Object> thesaurusMap = objectMapper.convertValue(item, new TypeReference<>() {});
                    
                    PactolsThesaurus thesaurus = new PactolsThesaurus();
                    thesaurus.setIdTheso((String) thesaurusMap.get("idTheso"));
                    
                    // Extraire les labels
                    Object labelsObj = thesaurusMap.get("labels");
                    if (labelsObj instanceof List<?>) {
                        List<LinkedHashMap<String, String>> labels = objectMapper.convertValue(labelsObj, new TypeReference<List<LinkedHashMap<String, String>>>() {});
                        thesaurus.setLabels(labels);
                        
                        // Sélectionner le label selon la langue
                        Optional<LinkedHashMap<String, String>> labelOpt = labels.stream()
                            .filter(element -> element.containsKey("lang") && selectedLangue.equals(element.get("lang")))
                            .findFirst();
                        
                        if (labelOpt.isEmpty()) {
                            // Si la langue n'est pas trouvée, chercher "fr" par défaut
                            labelOpt = labels.stream()
                                .filter(element -> element.containsKey("lang") && "fr".equals(element.get("lang")))
                                .findFirst();
                        }
                        
                        if (labelOpt.isPresent() && labelOpt.get().containsKey("title")) {
                            thesaurus.setSelectedLabel(labelOpt.get().get("title"));
                        } else if (!labels.isEmpty() && labels.get(0).containsKey("title")) {
                            // Prendre le premier disponible si aucun ne correspond
                            thesaurus.setSelectedLabel(labels.get(0).get("title"));
                        } else {
                            thesaurus.setSelectedLabel("");
                        }
                    }
                    
                    thesaurusList.add(thesaurus);
                }
                
                log.info("Récupération de {} thésaurus", thesaurusList.size());
                return thesaurusList;
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des thésaurus", e);
        }
        return new ArrayList<>();
    }

    /**
     * Récupère la liste des langues disponibles pour un thésaurus
     * @param idThesaurus ID du thésaurus
     * @return Liste des langues
     */
    public List<PactolsLangue> getThesaurusLanguages(String idThesaurus) {
        try {
            String url = PACTOLS_BASE_URL + "/thesaurus/" + idThesaurus + "/listlang";
            ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                List.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<PactolsLangue> languages = new ArrayList<>();
                
                for (Object item : response.getBody()) {
                    Map<String, Object> langMap = objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {});

                    String idLang = (String) langMap.get("lang");

                    PactolsLangue langue = new PactolsLangue();
                    langue.setIdLang(idLang);
                    langue.setNom(LangueEnum.getLabelByCode(idLang));

                    languages.add(langue);
                }
                
                log.info("Récupération de {} langues pour le thésaurus {}", languages.size(), idThesaurus);
                return languages;
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des langues du thésaurus {}", idThesaurus, e);
        }
        return new ArrayList<>();
    }

    /**
     * Récupère la liste des collections d'un thésaurus
     * @param idThesaurus ID du thésaurus
     * @param selectedLangue Langue sélectionnée pour les labels
     * @return Liste des collections
     */
    public List<PactolsCollection> getThesaurusCollections(String idThesaurus, String selectedLangue) {
        try {
            String url = PACTOLS_BASE_URL + "/group/" + idThesaurus;
            ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                List.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<PactolsCollection> collections = new ArrayList<>();
                
                for (Object item : response.getBody()) {
                    Map<String, Object> collectionMap = objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {});
                    
                    PactolsCollection collection = new PactolsCollection();
                    collection.setIdGroup((String) collectionMap.get("idGroup"));
                    
                    // Extraire les labels
                    Object labelsObj = collectionMap.get("labels");
                    if (labelsObj instanceof ArrayList<?>) {
                        List<LinkedHashMap<String, String>> labels = objectMapper.convertValue(labelsObj, new TypeReference<List<LinkedHashMap<String, String>>>() {});
                        collection.setLabels(labels);

                        // Sélectionner le label selon la langue
                        Optional<LinkedHashMap<String, String>> labelOpt = labels.stream()
                                .filter(element -> element.containsKey("lang") && selectedLangue.equals(element.get("lang")))
                                .findFirst();

                        if (labelOpt.isEmpty()) {
                            // Si la langue n'est pas trouvée, chercher "fr" par défaut
                            labelOpt = labels.stream()
                                    .filter(element -> element.containsKey("lang") && "fr".equals(element.get("lang")))
                                    .findFirst();
                        }

                        if (labelOpt.isPresent() && labelOpt.get().containsKey("title")) {collection.setSelectedLabel(labelOpt.get().get("title"));
                        } else if (!labels.isEmpty() && labels.get(0).containsKey("title")) {
                            // Prendre le premier disponible si aucun ne correspond
                            collection.setSelectedLabel(labels.get(0).get("title"));
                        } else {
                            collection.setSelectedLabel("");
                        }
                    }
                    
                    collections.add(collection);
                }
                
                log.info("Récupération de {} collections pour le thésaurus {}", collections.size(), idThesaurus);
                return collections;
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des collections du thésaurus {}", idThesaurus, e);
        }
        return new ArrayList<>();
    }

    /**
     * Recherche des concepts dans un thésaurus
     * @param idThesaurus ID du thésaurus
     * @param searchValue Valeur recherchée
     * @param idLang ID de la langue
     * @param idCollection ID de la collection
     * @return Liste des concepts trouvés
     */
    public List<PactolsConcept> searchConcepts(String idThesaurus, String searchValue, String idLang, String idCollection) {
        try {
            String url = PACTOLS_BASE_URL + "/concept/" + idThesaurus + "/autocomplete/" + 
                        searchValue + "/full?lang=" + idLang + "&group=" + idCollection;
            
            ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                List.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<PactolsConcept> concepts = new ArrayList<>();
                
                for (Object item : response.getBody()) {
                    Map<String, Object> conceptMap = objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {});
                    
                    PactolsConcept concept = new PactolsConcept();
                    concept.setIdConcept((String) conceptMap.get("idConcept"));
                    
                    // Extraire les termes
                    Object termsObj = conceptMap.get("terms");
                    if (termsObj instanceof Map) {
                        Map<String, String> terms = objectMapper.convertValue(termsObj, new TypeReference<Map<String, String>>() {});
                        concept.setTerms(terms);
                        
                        // Sélectionner le terme selon la langue choisie
                        String term = terms.get(idLang);
                        if (term == null && !terms.isEmpty()) {
                            term = terms.values().iterator().next();
                        }
                        concept.setSelectedTerm(term != null ? term : "");
                    }
                    
                    concepts.add(concept);
                }
                
                log.info("Recherche de concepts: {} résultats trouvés", concepts.size());
                return concepts;
            }
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de concepts", e);
        }
        return new ArrayList<>();
    }
}
