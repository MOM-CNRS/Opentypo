package fr.cnrs.opentypo.conf;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class FlywayInitializer {

    private static final Logger logger = LoggerFactory.getLogger(FlywayInitializer.class);

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
        logger.info("Initialisation manuelle de Flyway après Hibernate...");

        if (flyway != null) {
            // Flyway est déjà configuré par Spring Boot, on vérifie juste l'état
            try {
                int pending = flyway.info().pending().length;
                int applied = flyway.info().applied().length;
                logger.info("Flyway - Migrations appliquées: {}, en attente: {}", applied, pending);
                
                if (pending > 0) {
                    logger.info("Exécution des migrations Flyway en attente...");
                    flyway.migrate();
                    logger.info("✓ Migrations Flyway exécutées avec succès");
                } else {
                    logger.info("Aucune migration en attente");
                }
            } catch (Exception e) {
                logger.error("Erreur lors de l'exécution de Flyway: {}", e.getMessage(), e);
            }
        } else if (dataSource != null) {
            // Flyway n'est pas configuré, on le configure manuellement
            logger.warn("Flyway bean non disponible, initialisation manuelle...");
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
                logger.info("Flyway manuel - Migrations appliquées: {}, en attente: {}", applied, pending);

                if (pending > 0) {
                    logger.info("Exécution des migrations Flyway...");
                    manualFlyway.migrate();
                    logger.info("✓ Migrations Flyway exécutées avec succès");
                }
            } catch (Exception e) {
                logger.error("Erreur lors de l'initialisation manuelle de Flyway: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("DataSource non disponible, impossible d'initialiser Flyway");
        }
    }
}

