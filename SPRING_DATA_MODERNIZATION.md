# Spring Data JPA Modernization for QLink Server

## Overview

This document describes the modernization changes made to QLink Server's data access layer using Spring Data JPA. The goal is to reduce boilerplate code, improve type safety, and enable better separation of concerns.

## Changes Completed

### 1. Dependencies Added to pom.xml

The following Spring Data JPA and related dependencies have been added:

| Dependency | Version | Purpose |
|------------|---------|---------|
| spring-context | 5.3.39 | Spring core container (compatible with Spring Data JPA 2.7.x) |
| spring-orm | 5.3.39 | Spring ORM integration |
| spring-data-jpa | 2.7.18 | Spring Data JPA repositories |
| hibernate-core | 5.6.15.Final | JPA implementation (uses javax.persistence) |
| flyway-core | 10.18.0 | Database migrations |
| flyway-mysql | 10.18.0 | MySQL support for Flyway |

### 2. Database Migrations Created

Flyway migration files have been created in `src/main/resources/db/migration/`:

- **`V1__Initial_Schema.sql`** - Creates all core tables for QLinkServer with JPA-compatible columns
  - users, accounts, articles, auditorium_talks, bullets, emails
  - entry_types, gateways, menu_item_entries, qfiles, room_logs
  - toc_entries, vendor_rooms

- **`V2__Add_SpringData_JPA_Fields.sql`** - Adds version column for optimistic locking support
  - Adds `version` int(11) NOT NULL default '0' to all tables

### 3. JPA Entity Classes (with javax.persistence annotations)

Updated entity classes using Spring Data JPA annotations (compatible with Spring 5.x):

- [`User.java`](src/main/java/org/jbrain/qlink/db/entity/User.java:1) - User entity with JPA annotations
  - `@Entity` and `@Table(name = "users")`
  - `@Id` and `@GeneratedValue(strategy = GenerationType.IDENTITY)` for primary key
  - `@Column` annotations for all fields

- [`Account.java`](src/main/java/org/jbrain/qlink/db/entity/Account.java:1) - Account entity with relationship to User
  - `@Entity` and `@Table(name = "accounts")`
  - `@Id` and `@GeneratedValue(strategy = GenerationType.IDENTITY)` for primary key
  - `@Column` annotations for all fields

### 4. Spring Data JPA Repositories

Repository interfaces extending Spring Data's `JpaRepository`:

- [`UserRepository.java`](src/main/java/org/jbrain/qlink/db/repository/UserRepository.java:1) - User repository with custom query methods
  - `findByAccessCode(String accessCode)`
  - `findByEmail(String email)`
  - `existsByAccessCode(String accessCode)`
  - `existsByEmail(String email)`

- [`AccountRepository.java`](src/main/java/org/jbrain/qlink/db/repository/AccountRepository.java:1) - Account repository with custom query methods
  - `findByHandle(String handle)` - with custom @Query for handle matching
  - `findByUserIdOrderByCreateDate(int userId)`
  - `findByUserIdAndPrimaryIndTrue(int userId)`
  - `existsByHandle(String handle)`

### 5. Spring Data Configuration

- [`SpringDataJpaConfig.java`](src/main/java/org/jbrain/qlink/db/config/SpringDataJpaConfig.java:1) - Spring configuration class for JPA setup
  - Uses HikariCP for connection pooling
  - Configures EntityManagerFactory with Hibernate JPA provider
  - Enables transaction management
  - Scans repositories in `org.jbrain.qlink.db.repository` package

## Building and Testing

### Build Project with Maven

```bash
mvn clean compile
```

This will download all dependencies including Spring Data JPA and compile the entity classes.

### Expected Output

```
[INFO] BUILD SUCCESS
[INFO] Total time:  X.XXX s
```

The build should succeed with 401 source files compiled.

## Backward Compatibility

The existing `BaseDAO` and DAO classes in `org.jbrain.qlink.db.dao` remain unchanged and fully functional. They can coexist with Spring Data repositories during the migration period.

## Migration Path

1. **Phase 1** (Current): Add dependencies and create entity classes
   - [x] Add Spring Data JPA and Flyway dependencies to pom.xml
   - [x] Create JPA Entity classes (User, Account)
   - [x] Create Spring Data JPA Repositories (UserRepository, AccountRepository)
   - [x] Create Spring Data configuration class (SpringDataJpaConfig)
   - [x] Create Flyway database migrations (V1, V2)
   - [x] Build and verify

2. **Phase 2** (Optional): Create DTO layer and service layer
   - Create DTO classes for clean API boundaries
   - Create service layer using Spring Data repositories

3. **Phase 3** (Optional): Gradually refactor existing code
   - Refactor existing DAO classes to use Spring Data repositories
   - Deprecate old BaseDAO-based code

## Usage Example

After completing Phase 2, usage would look like:

```java
@Service
public class UserService {
    private final UserRepository userRepository;
    
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Transactional(readOnly = true)
    public User findUserByAccessCode(String accessCode) {
        return userRepository.findByAccessCode(accessCode);
    }
    
    @Transactional
    public User findUserById(int userId) {
        return userRepository.findById(userId).orElse(null);
    }
}
```

## Notes

- Spring Data JPA 2.7.x uses `javax.persistence` (JPA 2.x), not `jakarta.persistence` (JPA 3.x)
- The Spring version (5.3.39) was chosen for compatibility with Spring Data JPA 2.7.18
- Use `@Transactional` for write operations
- Consider using `@Query` for complex queries that can't be derived from method names
- Flyway migrations should be versioned and never modified after release
- The existing BaseDAO pattern can coexist with Spring Data repositories