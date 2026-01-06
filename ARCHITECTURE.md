# Architecture du Projet OpenTypo

## Structure des Packages

```
fr.cnrs.opentypo/
├── domain/                      # Couche Domaine (Domain-Driven Design)
│   ├── entity/                  # Entités JPA (Domain Entities)
│   ├── repository/               # Interfaces Repository (Domain Layer)
│   └── exception/                # Exceptions métier
│
├── application/                  # Couche Application (Use Cases)
│   ├── dto/                      # Data Transfer Objects
│   ├── mapper/                   # Mappers (Entity <-> DTO)
│   ├── service/                  # Services métier (Application Services)
│   └── usecase/                  # Cas d'utilisation (si nécessaire)
│
├── infrastructure/               # Couche Infrastructure
│   ├── persistence/              # Implémentations Repository (Spring Data JPA)
│   ├── security/                 # Configuration sécurité
│   ├── config/                   # Configuration Spring
│   ├── exception/               # Handlers d'exceptions
│   └── migration/                # Migrations Flyway
│
├── presentation/                 # Couche Présentation (JSF)
│   ├── bean/                     # Managed Beans (ViewModels)
│   ├── converter/                # JSF Converters
│   └── validator/                # JSF Validators
│
└── common/                        # Code partagé
    ├── util/                     # Utilitaires
    ├── constant/                  # Constantes
    └── exception/                # Exceptions communes
```

## Design Patterns Appliqués

### 1. **Repository Pattern**
- Interfaces dans `domain/repository/`
- Implémentations dans `infrastructure/persistence/`

### 2. **Service Layer Pattern**
- Services métier dans `application/service/`
- Séparation des responsabilités

### 3. **DTO Pattern**
- DTOs dans `application/dto/`
- Mappers dans `application/mapper/`

### 4. **Factory Pattern**
- Pour créer des objets complexes (si nécessaire)

### 5. **Strategy Pattern**
- Pour les algorithmes variables (authentification, etc.)

### 6. **Builder Pattern**
- Pour construire des objets complexes

## Principes

1. **Séparation des Couches** : Chaque couche ne dépend que des couches inférieures
2. **Dependency Inversion** : Les interfaces sont dans le domaine, les implémentations dans l'infrastructure
3. **Single Responsibility** : Chaque classe a une seule responsabilité
4. **DRY (Don't Repeat Yourself)** : Pas de duplication de code

