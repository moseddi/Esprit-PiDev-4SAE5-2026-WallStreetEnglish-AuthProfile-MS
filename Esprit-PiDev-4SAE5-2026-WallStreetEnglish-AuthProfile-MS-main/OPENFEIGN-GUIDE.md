# Guide: Communication Synchrone avec OpenFeign

## 1. Dependances Maven

Ajoutez ces dependances dans votre `pom.xml`:

```xml
<!-- OpenFeign -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- Eureka Client (optionnel si utilisation de service discovery) -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

## 2. Activation d'OpenFeign

Dans votre application principale:

```java
@SpringBootApplication
@EnableFeignClients
public class UserManagementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserManagementServiceApplication.class, args);
    }
}
```

## 3. Creation d'un Client Feign

### 3.1 Avec URL fixe

```java
@FeignClient(name = "AUTH-SERVICE", url = "http://localhost:8081")
public interface AuthServiceClient {

    @PostMapping("/api/auth/register")
    AuthResponse registerUser(@RequestBody AuthRegisterRequest request);

    @PostMapping("/api/auth/admin/create")
    AuthResponse createUserByAdmin(
            @RequestBody AuthRegisterRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @PutMapping("/api/auth/admin/role")
    AuthResponse updateUserRole(
            @RequestBody RoleUpdateRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
```

### 3.2 Avec Eureka (Service Discovery)

```java
@FeignClient(name = "auth-service")  // Nom du service dans Eureka
public interface AuthServiceClient {
    // ...
}
```

## 4. Methodes HTTP

| Annotation | Description |
|------------|-------------|
| `@GetMapping` | Requete GET |
| `@PostMapping` | Requete POST |
| `@PutMapping` | Requete PUT |
| `@DeleteMapping` | Requete DELETE |
| `@PatchMapping` | Requete PATCH |

## 5. Parametres de Requete

| Annotation | Description |
|------------|-------------|
| `@PathVariable` | Parametre dans l'URL (ex: `/users/{id}`) |
| `@RequestParam` | Parametre de requete (ex: `?page=1`) |
| `@RequestBody` | Corps de la requete (JSON) |
| `@RequestHeader` | En-tete HTTP |
| `@Headers` | Multiple en-tetes |

### Exemples:

```java
@GetMapping("/api/users/{id}")
User getUserById(@PathVariable("id") Long id);

@GetMapping("/api/users")
List<User> getUsers(@RequestParam("page") int page, 
                    @RequestParam("size") int size);

@PostMapping("/api/users")
User createUser(@RequestBody User user);

@PutMapping("/api/users/{id}")
User updateUser(@PathVariable("id") Long id, 
                @RequestBody User user);
```

## 6. Configuration Avancee

### 6.1 Configuration par defaut (application.yml)

```yaml
feign:
  client:
    config:
      default:
        connect-timeout: 5000
        read-timeout: 10000
        logger-level: basic
```

### 6.2 Configuration par client

```yaml
feign:
  client:
    config:
      AUTH-SERVICE:
        connect-timeout: 3000
        read-timeout: 5000
```

### 6.3 Circuit Breaker (Hystrix)

```java
@FeignClient(name = "AUTH-SERVICE", url = "http://localhost:8081",
             fallback = AuthServiceClientFallback.class)
public interface AuthServiceClient {
    // ...
}

@Component
public class AuthServiceClientFallback implements AuthServiceClient {
    @Override
    public AuthResponse registerUser(AuthRegisterRequest request) {
        return null; // or fallback response
    }
}
```

Ajoutez la dependance:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

### 6.4 Intercepteurs pour Authentification

```java
@Component
public class FeignAuthInterceptor implements RequestInterceptor {
    
    @Override
    public void apply(RequestTemplate template) {
        // Ajouter le token JWT
        String token = obtainToken();
        template.header("Authorization", "Bearer " + token);
    }
    
    private String obtainToken() {
        // Logique pour recuperer le token
    }
}
```

Activez dans la configuration:
```yaml
feign:
  client:
    config:
      default:
        request-interceptors:
          - tn.esprit.usermanagementservice.config.FeignAuthInterceptor
```

## 7. Gestion des Erreurs

```java
@FeignClient(name = "AUTH-SERVICE", url = "http://localhost:8081",
             errorDecoder = CustomErrorDecoder.class)
public interface AuthServiceClient {
    // ...
}

@Component
public class CustomErrorDecoder implements ErrorDecoder {
    
    private final ErrorDecoder defaultDecoder = new Default();
    
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new ResourceNotFoundException("Ressource non trouvee");
        }
        if (response.status() == 401) {
            return new UnauthorizedException("Non autorise");
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
```

## 8. Utilisation dans un Service

```java
@Service
@RequiredArgsConstructor
public class UserProfileService {
    
    private final AuthServiceClient authServiceClient;
    
    public AuthResponse registerUser(AuthRegisterRequest request) {
        return authServiceClient.registerUser(request);
    }
    
    public AuthResponse updateUserRole(String token, RoleUpdateRequest request) {
        return authServiceClient.updateUserRole(request, "Bearer " + token);
    }
}
```

## 9. Configuration Complete (application.yml)

```yaml
server:
  port: 8082

spring:
  application:
    name: user-management-service

feign:
  client:
    config:
      default:
        connect-timeout: 5000
        read-timeout: 10000
        logger-level: headers
        error-decoder: tn.esprit.usermanagementservice.config.CustomErrorDecoder
      AUTH-SERVICE:
        url: http://localhost:8081

# Eureka (optionnel)
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

## 10. Configuration Java (Alternative)

```java
@Configuration
public class FeignConfig {
    
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
    
    @Bean
    public Contract feignContract() {
        return new SpringMvcContract();  // Utilise les annotations Spring MVC
    }
}
```

Utilisez avec:
```java
@FeignClient(name = "AUTH-SERVICE", 
             url = "http://localhost:8081",
             configuration = FeignConfig.class)
public interface AuthServiceClient {
    // ...
}
```

## Resume des Points Cles

1. **Dependances**: `spring-cloud-starter-openfeign`
2. **Activation**: `@EnableFeignClients` sur la classe principale
3. **Definition**: Interface avec annotations `@FeignClient` et HTTP
4. **Injection**: Via constructeur dans les services
5. **Configuration**: `application.yml` ou classes Java
6. **Securite**: Intercepteurs pour JWT ou Circuit Breaker pour la resilience
