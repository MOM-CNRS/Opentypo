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

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Exécute Flyway après l'initialisation JPA ({@code spring.flyway.enabled=false}).
 */
@Component
@Order(1)
@Slf4j
public class FlywayMigrationRunner implements ApplicationRunner {

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

    @Value("${spring.flyway.out-of-order:true}")
    private boolean outOfOrder;

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

        releasePoolConnectionsBeforeMigrate(dataSource);
        migrateWithRepairOnValidateError(runner);

        var finalInfo = resolveFlyway().info();
        log.info("Flyway — état final : {} appliquée(s), {} en attente",
                finalInfo.applied().length, finalInfo.pending().length);
    }

    /**
     * Libère les connexions du pool ouvertes par Hibernate avant les migrations DDL,
     * afin d'éviter les blocages (ex. V61) sur ALTER TABLE.
     */
    private static void releasePoolConnectionsBeforeMigrate(DataSource dataSource) {
        if (!(dataSource instanceof HikariDataSource hikari)) {
            return;
        }
        try {
            if (hikari.getHikariPoolMXBean() != null) {
                hikari.getHikariPoolMXBean().softEvictConnections();
                log.debug("Pool Hikari : connexions évincées avant migration Flyway");
            }
        } catch (Exception e) {
            log.warn("Impossible d'évincer le pool Hikari avant Flyway : {}", e.getMessage());
        }
    }

    private Flyway resolveFlyway() {
        if (dataSource == null) {
            return null;
        }
        log.debug("Configuration Flyway manuelle (migrations post-JPA)");
        return buildFlyway(outOfOrder);
    }

    private Flyway buildFlyway(boolean allowOutOfOrder) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(flywayLocations)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .schemas(schemas.split(","))
                .validateOnMigrate(true)
                .outOfOrder(allowOutOfOrder)
                .load();
    }

    private void migrateWithRepairOnValidateError(Flyway runner) {
        try {
            runner.migrate();
        } catch (FlywayValidateException e) {
            log.warn("Validation Flyway : {} — repair puis nouvelle migration", e.getMessage());
            runner.repair();
            try {
                runner.migrate();
            } catch (FlywayValidateException retryError) {
                if (!outOfOrder) {
                    log.warn(
                            "Nouvelle tentative avec out-of-order=true (migration manquante dans l'historique ?)");
                    buildFlyway(true).repair();
                    buildFlyway(true).migrate();
                } else {
                    throw retryError;
                }
            }
        }
    }
}
