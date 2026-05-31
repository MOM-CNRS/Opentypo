package fr.cnrs.opentypo.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Exécute Flyway après l'initialisation JPA ({@code spring.flyway.enabled=false}).
 */
@Component
@Order(1)
@Slf4j
public class FlywayMigrationRunner implements ApplicationRunner {

    @Autowired(required = false)
    private Flyway flyway;

    @Autowired(required = false)
    private DataSource dataSource;

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String flywayLocations;

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.baseline-version:0}")
    private String baselineVersion;

    @Value("${spring.flyway.schemas:public}")
    private String schemas;

    @Value("${spring.jpa.hibernate.ddl-auto:validate}")
    private String ddlAuto;

    @Override
    public void run(ApplicationArguments args) {
        if ("create".equalsIgnoreCase(ddlAuto) || "create-drop".equalsIgnoreCase(ddlAuto)) {
            log.warn(
                    "spring.jpa.hibernate.ddl-auto={} : risque de schéma recréé par Hibernate. "
                            + "Utilisez 'update' ou 'validate' en local.",
                    ddlAuto);
        }

        Flyway runner = resolveFlyway();
        if (runner == null) {
            log.warn("Flyway indisponible — migrations ignorées");
            return;
        }

        int pending = runner.info().pending().length;
        int applied = runner.info().applied().length;
        log.info("Flyway — migrations appliquées: {}, en attente: {}", applied, pending);

        migrateWithRepairOnValidateError(runner);

        log.info("Flyway — état final : {} appliquée(s), {} en attente",
                runner.info().applied().length, runner.info().pending().length);
    }

    private Flyway resolveFlyway() {
        if (dataSource == null) {
            return null;
        }
        if (flyway != null) {
            return flyway;
        }
        log.debug("Création manuelle de Flyway (spring.flyway.enabled=false)");
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(flywayLocations)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .schemas(schemas.split(","))
                .validateOnMigrate(true)
                .outOfOrder(false)
                .load();
    }

    private static void migrateWithRepairOnValidateError(Flyway runner) {
        try {
            runner.migrate();
        } catch (FlywayValidateException e) {
            log.warn("Validation Flyway : {} — repair puis nouvelle migration", e.getMessage());
            runner.repair();
            runner.migrate();
        }
    }
}
