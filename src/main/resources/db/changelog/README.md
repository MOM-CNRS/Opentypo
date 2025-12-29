# Liquibase - Gestion des migrations de base de données

## Structure

```
db/changelog/
├── db.changelog-master.xml    # Fichier master qui référence tous les changelogs
└── changes/                   # Répertoire contenant les changelogs individuels
```

## Utilisation

### Créer un nouveau changelog

1. Créez un nouveau fichier dans `changes/` avec le format : `YYYYMMDD-HHMMSS-description.xml`
   Exemple : `20240101-120000-create-users-table.xml`

2. Ajoutez le contenu du changelog (voir exemple ci-dessous)

3. Référencez le nouveau changelog dans `db.changelog-master.xml` :
   ```xml
   <include file="db/changelog/changes/20240101-120000-create-users-table.xml"/>
   ```

### Exemple de changelog

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="20240101-120000-1" author="votre-nom">
        <createTable tableName="users">
            <column name="id" type="BIGSERIAL">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="username" type="VARCHAR(255)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="email" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>
    </changeSet>

</databaseChangeLog>
```

## Commandes Maven (optionnel)

Si vous souhaitez utiliser Liquibase en ligne de commande, ajoutez le plugin Maven dans `pom.xml` :

```xml
<plugin>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-maven-plugin</artifactId>
    <configuration>
        <changeLogFile>src/main/resources/db/changelog/db.changelog-master.xml</changeLogFile>
        <url>jdbc:postgresql://localhost:5432/opentypo</url>
        <username>postgres</username>
        <password>postgres</password>
    </configuration>
</plugin>
```

Commandes disponibles :
- `mvn liquibase:update` - Applique les changements
- `mvn liquibase:status` - Affiche le statut des migrations
- `mvn liquibase:rollback` - Annule les changements

## Bonnes pratiques

1. **Un changelog par modification** : Créez un fichier séparé pour chaque modification de schéma
2. **Nommage** : Utilisez un format de date/heure pour garantir l'ordre d'exécution
3. **Idempotence** : Les changements doivent être idempotents (peuvent être exécutés plusieurs fois sans effet)
4. **Rollback** : Ajoutez des rollbacks quand c'est possible pour faciliter les retours en arrière
5. **Tests** : Testez toujours les migrations sur une base de données de test avant la production

## Configuration

La configuration Liquibase est définie dans :
- `application.yaml` : Configuration par défaut
- `application-local.yaml` : Configuration pour le profil local
- `application-prod.yaml` : Configuration pour le profil production

Propriétés principales :
- `spring.liquibase.change-log` : Chemin vers le fichier master
- `spring.liquibase.enabled` : Active/désactive Liquibase
- `spring.liquibase.drop-first` : Supprime la base avant d'appliquer les changements (⚠️ à utiliser avec précaution)

