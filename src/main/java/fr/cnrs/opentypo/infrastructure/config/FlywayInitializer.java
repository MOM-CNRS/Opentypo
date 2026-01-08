package fr.cnrs.opentypo.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Initialiseur Flyway pour forcer l'exécution des migrations après Hibernate
 * S'exécute après Hibernate pour garantir que les tables sont créées avant l'insertion des données
 */
@Component
@Slf4j
public class FlywayInitializer {

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private Flyway flyway;

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String flywayLocations;

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.baseline-version:0}")
    private String baselineVersion;

    @Value("${spring.flyway.schemas:public}")
    private String schemas;

    @EventListener
    @Order(1000) // S'exécute après le démarrage complet de l'application
    public void initializeFlyway(ContextRefreshedEvent event) {
        log.info("Initialisation manuelle de Flyway après Hibernate...");

        if (flyway != null) {
            // Flyway est déjà configuré par Spring Boot, on vérifie juste l'état
            try {
                int pending = flyway.info().pending().length;
                int applied = flyway.info().applied().length;
                log.info("Flyway - Migrations appliquées: {}, en attente: {}", applied, pending);
                
                if (pending > 0) {
                    log.info("Exécution des migrations Flyway en attente...");
                    try {
                        flyway.migrate();
                        log.info("✓ Migrations Flyway exécutées avec succès");
                    } catch (FlywayValidateException e) {
                        log.warn("Erreur de validation Flyway détectée: {}", e.getMessage());
                        log.info("Tentative de réparation automatique des checksums...");
                        flyway.repair();
                        log.info("Réparation terminée, nouvelle tentative de migration...");
                        flyway.migrate();
                        log.info("✓ Migrations Flyway exécutées avec succès après réparation");
                    }
                } else {
                    log.info("Aucune migration en attente");
                }
            } catch (Exception e) {
                log.error("Erreur lors de l'exécution de Flyway: {}", e.getMessage(), e);
            }
        } else if (dataSource != null) {
            // Flyway n'est pas configuré, on le configure manuellement
            log.warn("Flyway bean non disponible, initialisation manuelle...");
            try {
                Flyway manualFlyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations(flywayLocations)
                    .baselineOnMigrate(baselineOnMigrate)
                    .baselineVersion(baselineVersion)
                    .schemas(schemas.split(","))
                    .validateOnMigrate(true)
                    .outOfOrder(false)
                    .load();

                int pending = manualFlyway.info().pending().length;
                int applied = manualFlyway.info().applied().length;
                log.info("Flyway manuel - Migrations appliquées: {}, en attente: {}", applied, pending);

                if (pending > 0) {
                    log.info("Exécution des migrations Flyway...");
                    try {
                        manualFlyway.migrate();
                        log.info("✓ Migrations Flyway exécutées avec succès");
                    } catch (FlywayValidateException e) {
                        log.warn("Erreur de validation Flyway détectée: {}", e.getMessage());
                        log.info("Tentative de réparation automatique des checksums...");
                        manualFlyway.repair();
                        log.info("Réparation terminée, nouvelle tentative de migration...");
                        manualFlyway.migrate();
                        log.info("✓ Migrations Flyway exécutées avec succès après réparation");
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de l'initialisation manuelle de Flyway: {}", e.getMessage(), e);
            }
        } else {
            log.warn("DataSource non disponible, impossible d'initialiser Flyway");
        }
    }
}

