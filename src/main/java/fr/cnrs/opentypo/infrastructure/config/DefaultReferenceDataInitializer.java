package fr.cnrs.opentypo.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Recharge types d'entité, groupes, langues et admin si des données manquent alors que
 * Flyway considère déjà les migrations comme appliquées.
 */
@Component
@Order(2)
@Slf4j
public class DefaultReferenceDataInitializer implements ApplicationRunner {

    private static final String SEED_SCRIPT = "db/reference/default-reference-data.sql";
    private static final int EXPECTED_ENTITY_TYPE_COUNT = 6;
    private static final int EXPECTED_GROUP_COUNT = 3;
    private static final int EXPECTED_LANGUAGE_COUNT = 2;

    private final DataSource dataSource;

    public DefaultReferenceDataInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!needsSeed()) {
            log.debug("Données de référence déjà présentes — seed ignoré");
            return;
        }

        log.warn(
                "Données de référence incomplètes (types d'entité, groupes, langues ou admin) — "
                        + "exécution de {}",
                SEED_SCRIPT);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(SEED_SCRIPT));
        populator.setContinueOnError(false);
        populator.execute(dataSource);

        log.info("Données de référence chargées — connexion : email=admin, mot de passe=admin");
    }

    private boolean needsSeed() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            if (!tableExists(statement, "entity_type")
                    || !tableExists(statement, "groupe")
                    || !tableExists(statement, "langue")
                    || !tableExists(statement, "utilisateur")) {
                log.debug("Tables de référence absentes — seed reporté");
                return false;
            }

            int entityTypeCount = count(statement, "SELECT COUNT(*) FROM entity_type");
            int groupCount = count(statement, "SELECT COUNT(*) FROM groupe");
            int languageCount = count(statement, "SELECT COUNT(*) FROM langue");
            boolean adminPresent = adminExists(statement);

            return entityTypeCount < EXPECTED_ENTITY_TYPE_COUNT
                    || groupCount < EXPECTED_GROUP_COUNT
                    || languageCount < EXPECTED_LANGUAGE_COUNT
                    || !adminPresent;
        } catch (Exception e) {
            log.warn("Impossible de vérifier les données de référence : {}", e.getMessage());
            return false;
        }
    }

    private static boolean tableExists(Statement statement, String tableName) throws Exception {
        try (ResultSet rs = statement.executeQuery(
                "SELECT 1 FROM information_schema.tables "
                        + "WHERE table_schema = current_schema() AND table_name = '" + tableName + "'")) {
            return rs.next();
        }
    }

    private static int count(Statement statement, String sql) throws Exception {
        try (ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static boolean adminExists(Statement statement) throws Exception {
        try (ResultSet rs = statement.executeQuery(
                "SELECT 1 FROM utilisateur WHERE LOWER(email) = 'admin' LIMIT 1")) {
            return rs.next();
        }
    }
}
