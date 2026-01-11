# OpenTypo

OpenTypo est une plateforme web de recherche et de gestion de typologies archéologiques développée par le CNRS. Cette application permet aux chercheurs de consulter, gérer et enrichir une base de données complète de types archéologiques, facilitant ainsi la recherche scientifique et la documentation du patrimoine archéologique.

## 🎯 Fonctionnalités principales

- **Gestion des collections** : Organisation des typologies par collections
- **Gestion des référentiels** : Création et gestion de référentiels archéologiques
- **Gestion des catégories** : Organisation hiérarchique des types archéologiques
- **Gestion des groupes** : Classification détaillée au sein des catégories
- **Recherche avancée** : Outils de recherche puissants et précis
- **Navigation arborescente** : Visualisation hiérarchique des données via un arbre interactif
- **Gestion des utilisateurs** : Système d'authentification et de permissions
- **Bibliographie** : Gestion des références bibliographiques associées aux entités
- **Auteurs** : Association d'auteurs aux entités archéologiques

## 🛠️ Technologies utilisées

### Backend
- **Java 21** : Langage de programmation principal
- **Spring Boot 4.0.0** : Framework d'application
- **Spring Data JPA** : Accès aux données
- **JSF (JavaServer Faces)** : Framework de présentation
- **JoinFaces 6.0.0** : Intégration JSF avec Spring Boot
- **PrimeFaces 14.0.5** : Bibliothèque de composants JSF
- **Flyway** : Gestion des migrations de base de données
- **PostgreSQL** : Base de données (configurable)

### Frontend
- **Bootstrap 5.3.3** : Framework CSS
- **PrimeIcons** : Bibliothèque d'icônes
- **FontAwesome 6.7.2** : Bibliothèque d'icônes supplémentaire
- **JavaScript (ES6+)** : Interactivité côté client
- **CSS3** : Styles personnalisés avec variables CSS

## 📋 Prérequis

- Java 21 ou supérieur
- Maven 3.6+
- PostgreSQL (ou autre base de données compatible JPA)
- Un serveur d'application (embarqué avec Spring Boot)

## 🚀 Installation

### 1. Cloner le dépôt

```bash
git clone <url-du-depot>
cd Opentypo
```

### 2. Configurer la base de données

Modifier le fichier `src/main/resources/application.yaml` ou créer un fichier `application-local.yaml` :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/opentypo
    username: votre_utilisateur
    password: votre_mot_de_passe
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
```

### 3. Exécuter les migrations Flyway

Les migrations sont automatiquement exécutées au démarrage de l'application. Elles se trouvent dans `src/main/resources/db/migration/`.

### 4. Compiler et lancer l'application

```bash
mvn clean install
mvn spring-boot:run
```

L'application sera accessible à l'adresse : `http://localhost:8080`

## 📁 Structure du projet

```
Opentypo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── fr/cnrs/opentypo/
│   │   │       ├── application/          # Couche Application
│   │   │       │   ├── dto/              # Data Transfer Objects
│   │   │       │   ├── mapper/           # Mappers Entity <-> DTO
│   │   │       │   └── service/          # Services métier
│   │   │       ├── common/               # Code partagé
│   │   │       │   ├── constant/         # Constantes
│   │   │       │   ├── models/           # Modèles communs
│   │   │       │   └── util/             # Utilitaires
│   │   │       ├── domain/               # Couche Domaine
│   │   │       │   └── entity/           # Entités JPA
│   │   │       ├── infrastructure/       # Couche Infrastructure
│   │   │       │   ├── config/           # Configuration Spring
│   │   │       │   └── persistence/      # Repositories Spring Data JPA
│   │   │       ├── presentation/         # Couche Présentation
│   │   │       │   ├── bean/             # Managed Beans JSF
│   │   │       │   └── converter/        # JSF Converters
│   │   │       └── OpenTypoApplication.java
│   │   ├── resources/
│   │   │   ├── db/migration/             # Migrations Flyway
│   │   │   ├── application.yaml          # Configuration principale
│   │   │   └── logback-spring.xml        # Configuration des logs
│   │   └── webapp/
│   │       ├── candidats/                # Pages de gestion des candidats
│   │       ├── commun/                   # Templates et composants communs
│   │       ├── details/                  # Pages de détails (collection, référence, catégorie, etc.)
│   │       ├── dialogs/                  # Dialogs PrimeFaces
│   │       ├── search/                   # Pages de recherche
│   │       ├── tree/                     # Composant arbre
│   │       ├── user/                     # Pages d'authentification
│   │       ├── users/                    # Pages de gestion des utilisateurs
│   │       └── resources/
│   │           ├── css/                  # Feuilles de style
│   │           ├── js/                   # Scripts JavaScript
│   │           └── img/                  # Images
│   └── test/                             # Tests unitaires
├── pom.xml                               # Configuration Maven
├── ARCHITECTURE.md                       # Documentation de l'architecture
└── README.md                             # Ce fichier
```

## 🏗️ Architecture

Le projet suit une architecture en couches basée sur les principes du Domain-Driven Design (DDD) :

- **Domain Layer** : Entités métier et interfaces de repositories
- **Application Layer** : Services métier, DTOs et mappers
- **Infrastructure Layer** : Implémentations techniques (repositories, configuration)
- **Presentation Layer** : Beans JSF et composants d'interface utilisateur

Pour plus de détails, consultez le fichier [ARCHITECTURE.md](ARCHITECTURE.md).

## 🔐 Authentification et sécurité

L'application utilise Spring Security pour la gestion de l'authentification et des autorisations. Les utilisateurs peuvent avoir différents niveaux de permissions :

- **Consultation** : Accès en lecture seule
- **Création/Édition** : Permissions de modification des données
- **Administration** : Accès complet à toutes les fonctionnalités

## 📊 Entités principales

- **Collection** : Regroupe plusieurs référentiels
- **Référentiel (Reference)** : Contient des catégories de types archéologiques
- **Catégorie** : Classification des types au sein d'un référentiel
- **Groupe** : Sous-classification au sein d'une catégorie
- **Utilisateur** : Gestion des comptes utilisateurs
- **Auteur** : Auteurs associés aux entités
- **Bibliographie** : Références bibliographiques

## 🎨 Interface utilisateur

L'interface est conçue pour être :
- **Responsive** : Adaptée aux différentes tailles d'écran
- **Ergonomique** : Navigation intuitive avec arbre interactif
- **Moderne** : Design épuré respectant la charte graphique du CNRS
- **Accessible** : Support du clavier et des standards d'accessibilité

### Charte de couleurs

- **Couleur principale** : `#47624D` (Vert forêt foncé)
- **Couleur hover** : `#A7C1A8` (Vert clair)
- **Couleur texte secondaire** : `#819A91`
- **Fond global** : `#EEEFE0`

## 🔧 Configuration

### Profils Spring

- **local** : Configuration pour le développement local (`application-local.yaml`)
- **prod** : Configuration pour la production (`application-prod.yaml`)

### Variables d'environnement

Les configurations sensibles peuvent être externalisées via des variables d'environnement ou des fichiers de configuration spécifiques au profil.

## 📝 Migrations de base de données

Les migrations Flyway sont automatiquement exécutées au démarrage. Les fichiers de migration se trouvent dans `src/main/resources/db/migration/` et suivent la convention de nommage `V{version}__{description}.sql`.

## 🧪 Tests

Pour exécuter les tests :

```bash
mvn test
```

## 📦 Déploiement

### Build pour la production

```bash
mvn clean package -Pprod
```

Le fichier JAR sera généré dans le dossier `target/`.

### Exécution en production

```bash
java -jar target/opentypo-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 🤝 Contribution

Pour contribuer au projet :

1. Créer une branche depuis `main`
2. Effectuer vos modifications
3. Créer une pull request avec une description détaillée

## 📄 Licence

Consultez le fichier [LICENSE](LICENSE) pour plus d'informations.

## 👥 Équipe

Développé par le CNRS (Centre National de la Recherche Scientifique).

## 📞 Support

Pour toute question ou problème, veuillez ouvrir une issue sur le dépôt du projet.

## 🔄 Changelog

### Version 0.0.1-SNAPSHOT
- Version initiale du projet
- Gestion des collections, référentiels, catégories et groupes
- Système d'authentification et de permissions
- Interface utilisateur responsive et moderne
- Navigation arborescente interactive

---

**Note** : Ce projet est en développement actif. Certaines fonctionnalités peuvent être en cours d'implémentation.
