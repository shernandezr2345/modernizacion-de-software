# Plan de implementación — Preexperimento PetClinic

## 1. Objetivo

Implementar en la base de código actual de Spring PetClinic una API REST versionada para consultar veterinarios:

```http
GET /api/v1/vets
```

La API debe:

1. Retornar los veterinarios y sus especialidades en JSON.
2. Requerir autenticación mediante JWT.
3. Permitir acceso únicamente a usuarios con la autoridad `VETS_READ`.
4. Retornar:
   - `200 OK` para un token válido con `VETS_READ`.
   - `401 Unauthorized` cuando no exista token, esté vencido o sea inválido.
   - `403 Forbidden` cuando el token sea válido pero no contenga `VETS_READ`.
5. Mantener funcionando sin modificaciones el endpoint legado `GET /vets`.
6. No modificar el esquema de base de datos.
7. No modificar el comportamiento público de `VetRepository`.
8. No exponer directamente las entidades JPA `Vet` y `Specialty`.

El cambio debe realizarse dentro del monolito actual. Este preexperimento no incluye extracción de microservicios, separación física de base de datos, API Gateway, SPA Angular ni integración con un proveedor de identidad real.

---

## 2. Alcance

### Incluido

- Nueva ruta REST `GET /api/v1/vets`.
- Respuesta JSON con DTOs explícitos.
- Nueva capa de servicio de consulta.
- Validación de JWT.
- Autorización mediante `VETS_READ`.
- Pruebas automatizadas para `200`, `401` y `403`.
- Prueba de equivalencia funcional con el flujo legado.
- Verificación de compatibilidad con `GET /vets`.
- Scripts reproducibles de carga.
- Documentación de ejecución.

### Excluido

- Extracción de un microservicio.
- Creación de una nueva base de datos.
- Modificación de `schema.sql` o `data.sql`.
- Eliminación de Thymeleaf.
- Eliminación o reemplazo de `VetController`.
- Implementación de Angular, React o cualquier SPA.
- Implementación de API Gateway.
- Integración con Keycloak, Auth0 u otro Identity Provider real.
- Protección de toda la aplicación legado.
- Refactorizaciones no relacionadas con el experimento.

---

## 3. Restricciones obligatorias

La implementación debe respetar las siguientes condiciones:

```text
- No convertir PetClinic en microservicios.
- No eliminar Thymeleaf.
- No eliminar ni reemplazar VetController.
- No cambiar la ruta GET /vets.
- No modificar schema.sql ni data.sql.
- No cambiar las entidades Vet o Specialty salvo que sea estrictamente necesario.
- No retornar entidades JPA directamente desde el controlador REST.
- No agregar Keycloak, Auth0 ni otro Identity Provider real.
- No usar sesiones HTTP para proteger /api/v1/vets.
- No proteger globalmente toda la aplicación legado.
- No cambiar endpoints distintos de /api/v1/vets.
- No generar claves privadas dentro del repositorio de producción.
- No registrar JWT completos, claves privadas ni secretos en logs.
- No agregar dependencias con versiones explícitas cuando puedan heredarse del BOM de Spring Boot.
- No introducir WebFlux únicamente para realizar pruebas.
```

---

## 4. Fase inicial: inspección del repositorio

Antes de modificar código, inspeccionar la base actual y documentar:

1. Versión real de Java.
2. Versión real de Spring Boot.
3. Sistema de construcción: Maven o Gradle.
4. Paquete base de la aplicación.
5. Ubicación de:
   - `VetController`
   - `VetRepository`
   - `Vet`
   - `Specialty`
   - `Vets`
   - configuración web
   - configuración de caché
   - pruebas relacionadas con veterinarios
   - `application.properties` o `application.yml`
6. Firma real de `VetRepository.findAll()`.
7. Forma en la que `GET /vets` obtiene actualmente sus datos.
8. Estructura actual de la respuesta JSON de `GET /vets`, si existe negociación de contenido.
9. Dependencias de seguridad existentes.
10. Presencia de:
    - `SecurityFilterChain`
    - `@EnableMethodSecurity`
    - OAuth2 Resource Server
    - `JwtDecoder`
    - Actuator
11. Convenciones existentes para:
    - paquetes
    - DTOs
    - records
    - servicios
    - pruebas
    - nombres de clases

No asumir firmas de métodos, tipos de identificadores ni estructuras de colecciones cuando puedan obtenerse directamente del código.

---

## 5. Crear la rama de trabajo

Crear una rama específica para el experimento:

```bash
git checkout -b experiment/secure-vets-rest-api
```

Antes de realizar cambios, ejecutar toda la suite:

### Linux/macOS

```bash
./mvnw test
```

### Windows

```powershell
.\mvnw.cmd test
```

Registrar:

- cantidad de pruebas ejecutadas;
- pruebas fallidas;
- tiempo total;
- estado del endpoint legado;
- datos de veterinarios cargados por el proyecto;
- respuesta actual de `GET /vets`;
- configuración de caché relevante.

No continuar si la línea base ya está fallando sin documentar primero cuáles fallos son preexistentes.

---

## 6. Dependencias

Verificar primero si ya existen. Agregar únicamente las faltantes.

### Maven

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

No declarar versiones individuales.

Después de modificar el archivo de construcción:

```bash
./mvnw dependency:tree
./mvnw test
```

En Windows:

```powershell
.\mvnw.cmd dependency:tree
.\mvnw.cmd test
```

---

## 7. Estructura propuesta

Adaptar los paquetes a la estructura real del repositorio.

```text
src/main/java/.../vet/
├── Vet.java
├── Specialty.java
├── VetRepository.java
├── VetController.java
├── api/
│   ├── VetsApiController.java
│   └── dto/
│       ├── VetResponse.java
│       └── SpecialtyResponse.java
└── service/
    └── VetQueryService.java

src/main/java/.../system/
└── SecurityConfiguration.java

src/test/java/.../vet/api/
├── VetsApiControllerIntegrationTest.java
├── VetsApiAuthorizationTest.java
└── VetApiEquivalenceTest.java

src/test/java/.../vet/service/
└── VetQueryServiceTest.java

performance/
├── legacy-vets.js
├── modern-vets.js
├── README.md
└── results/
```

La estructura exacta puede cambiar para respetar las convenciones existentes, pero deben existir claramente:

- DTOs;
- servicio de consulta;
- controlador REST;
- configuración de seguridad;
- pruebas funcionales;
- pruebas de seguridad;
- prueba de equivalencia;
- scripts de carga;
- documentación.

---

## 8. Contrato HTTP esperado

### Request

```http
GET /api/v1/vets
Authorization: Bearer <jwt>
Accept: application/json
```

### Response satisfactoria

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
[
  {
    "id": 1,
    "firstName": "James",
    "lastName": "Carter",
    "specialties": [
      {
        "id": 1,
        "name": "radiology"
      }
    ]
  }
]
```

### Reglas del contrato

- La raíz es un arreglo JSON.
- Cada veterinario contiene:
  - `id`
  - `firstName`
  - `lastName`
  - `specialties`
- `specialties` nunca debe ser `null`.
- Cada especialidad contiene:
  - `id`
  - `name`
- No deben aparecer:
  - propiedades internas de Hibernate;
  - referencias inversas a veterinarios;
  - proxies;
  - metadatos JPA;
  - campos no definidos por el contrato.

---

## 9. DTO `SpecialtyResponse`

Preferir un `record` cuando la versión real de Java y las convenciones del proyecto lo permitan.

```java
public record SpecialtyResponse(
        Integer id,
        String name
) {

    public static SpecialtyResponse from(Specialty specialty) {
        Objects.requireNonNull(specialty, "specialty must not be null");

        return new SpecialtyResponse(
                specialty.getId(),
                specialty.getName()
        );
    }
}
```

Adaptar:

- el tipo real de `id`;
- los nombres reales de getters;
- la visibilidad;
- el paquete.

### Criterios

- No incluir referencia a `Vet`.
- No exponer la entidad JPA.
- No usar mapeadores automáticos para este experimento.
- Mantener un contrato pequeño y explícito.
- Validar el comportamiento cuando la especialidad sea nula solo si el dominio actual lo permite.

---

## 10. DTO `VetResponse`

Implementación orientativa:

```java
public record VetResponse(
        Integer id,
        String firstName,
        String lastName,
        List<SpecialtyResponse> specialties
) {

    public VetResponse {
        specialties = specialties == null
                ? List.of()
                : List.copyOf(specialties);
    }

    public static VetResponse from(Vet vet) {
        Objects.requireNonNull(vet, "vet must not be null");

        List<SpecialtyResponse> specialties = vet.getSpecialties().stream()
                .map(SpecialtyResponse::from)
                .toList();

        return new VetResponse(
                vet.getId(),
                vet.getFirstName(),
                vet.getLastName(),
                specialties
        );
    }
}
```

Adaptar `getSpecialties()` a la API real de la entidad.

### Criterios

- No retornar la entidad `Vet`.
- No retornar la entidad `Specialty`.
- La lista debe ser inmutable o una copia defensiva.
- Mantener un orden determinista.
- Conservar el orden existente cuando el flujo legado ya lo defina.
- No incluir lógica HTTP.
- No acceder al repositorio desde el DTO.

---

## 11. Servicio `VetQueryService`

Crear un servicio de solo lectura que encapsule el repositorio.

```java
@Service
@Transactional(readOnly = true)
public class VetQueryService {

    private final VetRepository vetRepository;

    public VetQueryService(VetRepository vetRepository) {
        this.vetRepository = vetRepository;
    }

    public List<VetResponse> listAll() {
        return vetRepository.findAll().stream()
                .map(VetResponse::from)
                .toList();
    }
}
```

La implementación debe ajustarse si `findAll()` retorna:

- `Collection<Vet>`;
- `Page<Vet>`;
- `Vets`;
- un iterable;
- otro wrapper propio de PetClinic.

### Criterios

- Sin lógica HTTP.
- Sin dependencia de `HttpServletRequest`.
- Sin acceso directo a seguridad.
- Sin mutaciones.
- Sin retornar entidades JPA.
- Transacción de solo lectura.
- Resultado no nulo.
- No agregar un repositorio alternativo.
- No duplicar consultas existentes.
- No modificar `VetRepository` salvo que exista una necesidad técnica demostrable.

---

## 12. Controlador `VetsApiController`

Crear un controlador nuevo. No convertir el controlador Thymeleaf existente.

```java
@RestController
@RequestMapping(
        path = "/api/v1/vets",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class VetsApiController {

    private final VetQueryService vetQueryService;

    public VetsApiController(VetQueryService vetQueryService) {
        this.vetQueryService = vetQueryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VETS_READ')")
    public List<VetResponse> list() {
        return vetQueryService.listAll();
    }
}
```

### Criterios

- Ruta exacta: `/api/v1/vets`.
- Método HTTP: `GET`.
- Respuesta: JSON.
- No utilizar `Model`, `ModelMap` o `ModelAndView`.
- No retornar nombre de plantilla.
- No acceder directamente a `VetRepository`.
- No retornar entidades JPA.
- No duplicar lógica de consulta.
- No modificar `VetController`.
- No romper `GET /vets`.

---

## 13. Convención de autoridad

Utilizar internamente la autoridad exacta:

```text
VETS_READ
```

Implementar:

```java
@PreAuthorize("hasAuthority('VETS_READ')")
```

No anteponer automáticamente `ROLE_`, salvo que todo el diseño sea cambiado de manera consistente y documentada.

El contrato recomendado del JWT usa:

```json
{
  "sub": "experiment-user",
  "authorities": [
    "VETS_READ"
  ]
}
```

No mezclar múltiples convenciones como:

- `scope`;
- `scp`;
- `roles`;
- `realm_access.roles`;
- `authorities`.

Elegir únicamente `authorities` para este experimento.

---

## 14. Configuración de seguridad

La nueva seguridad debe proteger únicamente la API experimental y conservar pública la aplicación legado.

La estrategia preferida consiste en dos `SecurityFilterChain`.

### Cadena 1: API REST

```java
@Bean
@Order(1)
SecurityFilterChain vetsApiSecurity(
        HttpSecurity http,
        Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter
) throws Exception {

    http
        .securityMatcher("/api/v1/**")
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(HttpMethod.GET, "/api/v1/vets")
            .hasAuthority("VETS_READ")
            .anyRequest()
            .authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter)
            )
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .csrf(AbstractHttpConfigurer::disable);

    return http.build();
}
```

### Cadena 2: aplicación legado

```java
@Bean
@Order(2)
SecurityFilterChain legacySecurity(HttpSecurity http) throws Exception {

    http
        .authorizeHttpRequests(authorize -> authorize
            .anyRequest()
            .permitAll()
        );

    return http.build();
}
```

Adaptar el código a la versión real de Spring Security.

### Criterios

- `/api/v1/vets` requiere autenticación.
- `/api/v1/vets` requiere `VETS_READ`.
- `GET /vets` mantiene el comportamiento anterior.
- La API es stateless.
- No debe aparecer formulario de login.
- Las llamadas API sin token deben retornar `401`, no una redirección.
- No se deben crear sesiones.
- No proteger globalmente todas las rutas.
- No alterar innecesariamente CSRF para la aplicación MVC legado.
- Limitar la desactivación de CSRF a la cadena de la API cuando sea posible.

---

## 15. Conversión de authorities del JWT

Configurar la extracción del claim `authorities`.

```java
@Bean
Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {

    JwtGrantedAuthoritiesConverter authoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

    authoritiesConverter.setAuthoritiesClaimName("authorities");
    authoritiesConverter.setAuthorityPrefix("");

    JwtAuthenticationConverter authenticationConverter =
            new JwtAuthenticationConverter();

    authenticationConverter.setJwtGrantedAuthoritiesConverter(
            authoritiesConverter
    );

    return authenticationConverter;
}
```

Esto debe convertir:

```json
"authorities": ["VETS_READ"]
```

en una autoridad de Spring Security exactamente igual a:

```text
VETS_READ
```

---

## 16. Configuración del `JwtDecoder`

Para ejecución local, configurar una llave pública RSA externa.

Ejemplo:

```properties
spring.security.oauth2.resourceserver.jwt.public-key-location=${JWT_PUBLIC_KEY_LOCATION:classpath:keys/test-public.pem}
```

### Reglas

- No versionar una clave privada de producción.
- No incluir secretos reales.
- No registrar el token completo.
- No registrar el contenido completo de las claves.
- Documentar claramente que las claves locales son únicamente de prueba.
- Preferir configuración externa por variable de entorno.
- Mantener la generación de tokens separada del código productivo.

Para pruebas con `MockMvc`, utilizar `spring-security-test` y `jwt()` cuando no sea necesario validar criptografía real.

---

## 17. Pruebas unitarias del mapeo

Crear pruebas para `VetResponse`, `SpecialtyResponse` o `VetQueryService`.

### Casos mínimos

1. Mapea `id`, `firstName` y `lastName`.
2. Mapea todas las especialidades.
3. Retorna `specialties` como lista vacía cuando no hay especialidades.
4. No retorna entidades JPA.
5. Conserva un orden determinista.
6. `VetQueryService` delega exactamente una vez en `VetRepository`.
7. `VetQueryService` retorna DTOs.
8. No realiza operaciones de escritura.

Ejemplo conceptual:

```java
@ExtendWith(MockitoExtension.class)
class VetQueryServiceTest {

    @Mock
    private VetRepository repository;

    private VetQueryService service;

    @BeforeEach
    void setUp() {
        service = new VetQueryService(repository);
    }

    @Test
    void shouldMapVetsToResponseDtos() {
        // Arrange:
        // Construir Vet y Specialty usando la API real del dominio.
        // Configurar repository.findAll().

        // Act:
        List<VetResponse> result = service.listAll();

        // Assert:
        assertThat(result).hasSize(1);
        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }
}
```

---

## 18. Pruebas de integración

Usar `MockMvc` si la aplicación actual es Spring MVC.

### CP-01: token válido con `VETS_READ`

```java
@Test
void shouldReturnVetsForAuthorizedUser() throws Exception {

    mockMvc.perform(get("/api/v1/vets")
            .with(jwt().authorities(
                    new SimpleGrantedAuthority("VETS_READ")
            )))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON
        ))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].firstName").exists())
        .andExpect(jsonPath("$[0].lastName").exists())
        .andExpect(jsonPath("$[0].specialties").isArray());
}
```

### CP-02: sin token

```java
@Test
void shouldReturnUnauthorizedWithoutJwt() throws Exception {

    mockMvc.perform(get("/api/v1/vets"))
        .andExpect(status().isUnauthorized());
}
```

### CP-03: token inválido o expirado

Debe cubrirse mediante:

- un `JwtDecoder` que lance `JwtException`; o
- un token RSA realmente expirado o alterado en una prueba específica.

Resultado esperado:

```text
401 Unauthorized
```

### CP-04: token válido sin `VETS_READ`

```java
@Test
void shouldReturnForbiddenWithoutRequiredAuthority() throws Exception {

    mockMvc.perform(get("/api/v1/vets")
            .with(jwt().authorities(
                    new SimpleGrantedAuthority("OTHER_AUTHORITY")
            )))
        .andExpect(status().isForbidden());
}
```

### CP-05: equivalencia funcional

Comparar los datos normalizados de:

- `VetRepository`;
- respuesta de `/api/v1/vets`.

Comparar:

- cantidad;
- identificadores;
- nombres;
- apellidos;
- especialidades;
- identificadores de especialidades;
- nombres de especialidades.

No comparar HTML textual contra JSON.

### CP-06: compatibilidad legado

```java
@Test
void shouldKeepLegacyVetsPageAvailable() throws Exception {

    mockMvc.perform(get("/vets"))
        .andExpect(status().isOk());
}
```

Adaptar la expectativa al comportamiento real si el endpoint usa:

- redirección;
- paginación;
- negociación de contenido;
- vista Thymeleaf.

### CP-07: solicitudes rechazadas no llegan al servicio

Usar un spy:

```java
@SpyBean
private VetQueryService vetQueryService;
```

Para `401` y `403`:

```java
verifyNoInteractions(vetQueryService);
```

---

## 19. Prueba de equivalencia

Crear una prueba independiente que no genere el resultado esperado reutilizando exactamente el mismo método de mapeo que se está probando.

Ejemplo conceptual:

```java
@Test
void apiResponseShouldBeEquivalentToRepositoryData() throws Exception {

    List<Vet> repositoryVets = loadRepositoryVets();

    MvcResult result = mockMvc.perform(get("/api/v1/vets")
            .with(jwt().authorities(
                    new SimpleGrantedAuthority("VETS_READ")
            )))
        .andExpect(status().isOk())
        .andReturn();

    List<VetResponse> actual = objectMapper.readValue(
            result.getResponse().getContentAsByteArray(),
            new TypeReference<>() {}
    );

    List<ExpectedVet> expected = repositoryVets.stream()
            .map(this::mapIndependently)
            .toList();

    assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expected);
}
```

El criterio de aceptación es:

```text
Cero diferencias en cantidad, IDs, nombres, apellidos y especialidades.
```

---

## 20. Verificación manual

### Sin token

```bash
curl -i http://localhost:8080/api/v1/vets
```

Esperado:

```text
HTTP/1.1 401
```

### Token válido sin autoridad

```bash
curl -i \
  -H "Authorization: Bearer ${JWT_WITHOUT_ROLE}" \
  http://localhost:8080/api/v1/vets
```

Esperado:

```text
HTTP/1.1 403
```

### Token válido con `VETS_READ`

```bash
curl -i \
  -H "Authorization: Bearer ${JWT_WITH_VETS_READ}" \
  -H "Accept: application/json" \
  http://localhost:8080/api/v1/vets
```

Esperado:

```text
HTTP/1.1 200
Content-Type: application/json
```

### Endpoint legado

```bash
curl -i http://localhost:8080/vets
```

Esperado:

```text
El mismo comportamiento registrado en la línea base.
```

---

## 21. Pruebas de carga

Crear:

```text
performance/
├── legacy-vets.js
├── modern-vets.js
├── README.md
└── results/
```

### Configuración

- 20 usuarios virtuales.
- Incremento gradual: 30 segundos.
- Carga estable: 2 minutos.
- Descenso gradual: 30 segundos.
- Calentamiento: 1 minuto.
- Cinco corridas para el flujo legado.
- Cinco corridas para la API modernizada.
- Misma máquina.
- Misma JVM.
- Mismo perfil.
- Mismos datos.
- Misma configuración de caché.
- Reportar mediana de las cinco corridas.

### Script k6 para API modernizada

```javascript
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '2m', target: 20 },
    { duration: '30s', target: 0 }
  ],
  thresholds: {
    http_req_failed: ['rate==0'],
    http_req_duration: ['p(95)<500']
  }
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const token = __ENV.JWT_TOKEN;

export default function () {

  const response = http.get(`${baseUrl}/api/v1/vets`, {
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'application/json'
    }
  });

  check(response, {
    'status is 200': (r) => r.status === 200,
    'response is JSON': (r) =>
      String(r.headers['Content-Type']).includes('application/json')
  });
}
```

### Script legado

Usar la ruta real que represente el flujo anterior.

Documentar si:

- `GET /vets` renderiza HTML;
- `GET /vets` retorna JSON con `Accept: application/json`;
- existe otra ruta previa para obtener la lista;
- el caché influye en la medición.

No afirmar que la comparación es perfectamente equivalente cuando un endpoint renderiza HTML y el otro solamente serializa JSON. Registrar esta diferencia como amenaza a la validez.

### Umbrales

```text
p95 modernizado < 500 ms
```

```text
p95 modernizado <= p95 legado * 1.20
```

```text
100% de solicitudes válidas responden HTTP 200
```

---

## 22. Ejecución de las pruebas de carga

### Calentamiento

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e JWT_TOKEN="$JWT_WITH_VETS_READ" \
  performance/modern-vets.js
```

Crear un escenario de calentamiento de un minuto o documentar una ejecución previa no incluida en las métricas.

### Corridas

Ejecutar cinco veces cada escenario y guardar resultados:

```bash
k6 run \
  --summary-export=performance/results/modern-run-1.json \
  -e BASE_URL=http://localhost:8080 \
  -e JWT_TOKEN="$JWT_WITH_VETS_READ" \
  performance/modern-vets.js
```

Repetir para:

```text
modern-run-1.json
modern-run-2.json
modern-run-3.json
modern-run-4.json
modern-run-5.json
legacy-run-1.json
legacy-run-2.json
legacy-run-3.json
legacy-run-4.json
legacy-run-5.json
```

Crear una tabla final:

| Escenario | Corrida | p50 | p95 | Máximo | Errores |
|---|---:|---:|---:|---:|---:|
| Legado | 1 | | | | |
| Legado | 2 | | | | |
| Legado | 3 | | | | |
| Legado | 4 | | | | |
| Legado | 5 | | | | |
| Modernizado | 1 | | | | |
| Modernizado | 2 | | | | |
| Modernizado | 3 | | | | |
| Modernizado | 4 | | | | |
| Modernizado | 5 | | | | |

Calcular:

- mediana del p95 legado;
- mediana del p95 modernizado;
- diferencia porcentual;
- tasa de error;
- cumplimiento de umbrales.

Fórmula:

```text
diferencia_porcentual =
((p95_modernizado - p95_legado) / p95_legado) * 100
```

---

## 23. Documentación requerida

Crear o actualizar:

```text
docs/experiment-secure-vets-api.md
```

Debe contener:

1. Objetivo.
2. Alcance.
3. Arquitectura implementada.
4. Archivos creados.
5. Archivos modificados.
6. Dependencias agregadas.
7. Configuración JWT.
8. Claim utilizado.
9. Autoridad utilizada.
10. Cómo compilar.
11. Cómo ejecutar pruebas.
12. Cómo iniciar la aplicación.
13. Cómo generar o proporcionar tokens de prueba.
14. Cómo probar `200`, `401` y `403`.
15. Cómo verificar `GET /vets`.
16. Cómo ejecutar k6.
17. Cómo interpretar resultados.
18. Riesgos y limitaciones.
19. Decisión final del experimento.
20. Procedimiento de reversión.

---

## 24. Estrategia de commits

Realizar cambios pequeños y comprobables.

```text
1. test: capture baseline behavior for vets
2. build: add resource server security dependencies
3. feat: add vet API response DTOs
4. feat: add read-only vet query service
5. feat: expose versioned vets REST endpoint
6. feat: secure vets API with JWT authority
7. test: cover vets API authentication and authorization
8. test: verify legacy and REST data equivalence
9. perf: add reproducible k6 scenarios
10. docs: document secure vets experiment
```

Después de cada commit:

```bash
./mvnw test
```

En Windows:

```powershell
.\mvnw.cmd test
```

No continuar mientras existan pruebas nuevas fallando.

---

## 25. Criterios de aceptación

| ID | Requisito | Criterio |
|---|---|---|
| REQ-F01 | Consulta REST | `GET /api/v1/vets` devuelve veterinarios y especialidades en JSON. |
| REQ-F02 | Equivalencia funcional | Cantidad, IDs, nombres, apellidos y especialidades coinciden con el flujo legado. |
| REQ-S01 | Autenticación | Sin token o con token inválido se retorna `401`. |
| REQ-S02 | Autorización | Token sin `VETS_READ` retorna `403`. |
| REQ-Q01 | Desempeño | p95 menor a 500 ms y no más de 20% superior al legado. |
| REQ-Q02 | Compatibilidad | `GET /vets` conserva su comportamiento anterior. |
| REQ-T01 | Pruebas automatizadas | Existen pruebas para `200`, `401`, `403` y equivalencia. |

---

## 26. Regla de decisión

El experimento se considera viable cuando:

- todos los requisitos de prioridad alta se cumplen;
- no existen diferencias funcionales;
- no es posible evadir la autorización;
- `GET /vets` continúa funcionando;
- todas las solicitudes válidas responden `200`;
- el desempeño cumple el umbral o cualquier desviación está explicada y puede corregirse sin cambiar el enfoque arquitectónico.

### Resultado: continuar

Elegir `CONTINUAR` cuando:

- funcionalidad cumple;
- seguridad cumple;
- compatibilidad cumple;
- pruebas pasan;
- desempeño cumple.

### Resultado: ajustar

Elegir `AJUSTAR` cuando:

- funcionalidad y seguridad cumplen;
- el desempeño supera el umbral;
- existe una causa identificable y corregible;
- no se requiere descartar la frontera REST segura.

### Resultado: descartar

Elegir `DESCARTAR` cuando:

- no se logra equivalencia funcional;
- se puede evadir el control de acceso;
- la página legado deja de funcionar;
- se requiere modificar el esquema o extraer el servicio para lograr el objetivo;
- la solución introduce cambios fuera del alcance.

---

## 27. Definition of Done

La implementación solo puede declararse terminada cuando se cumpla todo:

```text
[ ] El proyecto compila.
[ ] Todas las pruebas anteriores continúan pasando.
[ ] GET /vets conserva su comportamiento anterior.
[ ] GET /api/v1/vets retorna JSON.
[ ] La respuesta contiene id, firstName, lastName y specialties.
[ ] Cada specialty contiene id y name.
[ ] specialties nunca es null.
[ ] Las entidades JPA no se retornan directamente.
[ ] VetsApiController depende de VetQueryService.
[ ] VetQueryService depende de VetRepository.
[ ] VetRepository conserva su contrato existente.
[ ] No se modificó el esquema de base de datos.
[ ] No se modificaron schema.sql ni data.sql.
[ ] Request sin JWT retorna 401.
[ ] JWT inválido o expirado retorna 401.
[ ] JWT válido sin VETS_READ retorna 403.
[ ] JWT válido con VETS_READ retorna 200.
[ ] Una petición rechazada no invoca VetQueryService.
[ ] Existe una prueba automatizada de equivalencia.
[ ] Existe una prueba de compatibilidad con GET /vets.
[ ] Existe un script reproducible de carga para cada escenario.
[ ] Se documentan cinco corridas por escenario.
[ ] No existen claves privadas reales o tokens válidos versionados.
[ ] La documentación contiene instrucciones reproducibles.
[ ] Se documentaron riesgos, desviaciones y pendientes.
[ ] ./mvnw test termina exitosamente.
```

---

# Instrucciones para Copilot Agent

## Prompt de ejecución

```text
Trabaja sobre la base de código actual de Spring PetClinic.

Objetivo:
Implementa el preexperimento de modernización del dominio Vet agregando una API REST
versionada y protegida mediante JWT:

GET /api/v1/vets

Antes de modificar código:

1. Inspecciona el pom.xml o build.gradle, la versión de Java, Spring Boot y la estructura
   del proyecto.
2. Localiza VetController, VetRepository, Vet, Specialty, Vets, la configuración web y las
   pruebas existentes.
3. Ejecuta toda la suite de pruebas y registra la línea base.
4. Resume los archivos que vas a modificar y crear.
5. No asumas firmas de clases o métodos: obténlas del repositorio.
6. Identifica el comportamiento actual de GET /vets.
7. Verifica si ya existen dependencias o configuraciones de Spring Security.

Requisitos funcionales:

- GET /api/v1/vets debe retornar todos los veterinarios y sus especialidades en JSON.
- La respuesta de cada veterinario debe contener id, firstName, lastName y specialties.
- Cada especialidad debe contener id y name, usando los tipos reales del proyecto.
- Los datos deben ser equivalentes a los que utiliza el flujo legado.
- GET /vets debe conservar exactamente su comportamiento anterior.
- specialties nunca debe ser null.

Seguridad:

- Agrega Spring Security y OAuth2 Resource Server únicamente si no existen.
- La API debe ser stateless.
- Sin Authorization header: HTTP 401.
- JWT inválido o expirado: HTTP 401.
- JWT válido sin VETS_READ: HTTP 403.
- JWT válido con VETS_READ: HTTP 200.
- Usa el claim authorities.
- Elimina el prefijo automático de authorities para producir exactamente VETS_READ.
- Protege solamente /api/v1/**.
- Mantén públicas las rutas legado durante este experimento.
- Configura dos SecurityFilterChain con @Order si es necesario para impedir que
  Spring Security rompa las páginas Thymeleaf existentes.
- No uses formulario de login.
- No crees sesiones HTTP para la API.
- No almacenes claves privadas, tokens válidos o secretos reales en el repositorio.

Diseño:

- No modifiques ni reemplaces VetController.
- Crea un VetsApiController nuevo con @RestController.
- Crea VetQueryService como servicio transaccional readOnly.
- Crea DTOs explícitos VetResponse y SpecialtyResponse.
- No retornes entidades JPA directamente.
- No modifiques VetRepository ni el esquema de base de datos.
- No extraigas un microservicio.
- No agregues SPA, API Gateway, Keycloak, Auth0 ni una base de datos nueva.
- Adapta los paquetes y firmas a las convenciones reales del repositorio.
- No realices refactorizaciones no relacionadas.

Pruebas:

- Prueba 200 con JWT y VETS_READ.
- Prueba 401 sin JWT.
- Prueba 401 para token inválido o expirado.
- Prueba 403 con JWT sin VETS_READ.
- Prueba que GET /vets continúa funcionando.
- Prueba la estructura JSON.
- Prueba equivalencia de IDs, nombres, apellidos y especialidades.
- Verifica que los requests 401 y 403 no invoquen VetQueryService.
- Usa MockMvc y spring-security-test si la aplicación es Spring MVC.
- No introduzcas WebFlux solamente para las pruebas.
- Ejecuta toda la suite después de cada bloque de cambios.
- Corrige cualquier error antes de continuar.

Prueba de carga:

- Crea scripts k6 versionados para el flujo legado y /api/v1/vets.
- Usa 20 usuarios virtuales.
- Usa ramp-up de 30 segundos.
- Mantén carga estable durante 2 minutos.
- Usa ramp-down de 30 segundos.
- Incluye una fase de calentamiento de 1 minuto.
- Documenta cómo ejecutar cinco corridas por escenario.
- El threshold de la API debe ser p95 < 500 ms.
- La tasa de errores debe ser 0.
- Agrega instrucciones para comparar que el p95 modernizado no sea más de 20% mayor
  que el p95 legado.
- Documenta el efecto de la caché y cualquier diferencia entre renderizar HTML y serializar JSON.

Documentación:

- Documenta cómo compilar.
- Documenta cómo iniciar la aplicación.
- Documenta cómo ejecutar pruebas.
- Documenta cómo generar o proporcionar tokens de prueba.
- Documenta cómo probar los casos 200, 401 y 403.
- Documenta cómo verificar GET /vets.
- Documenta cómo ejecutar k6.
- Documenta cómo interpretar los resultados.
- Documenta cualquier diferencia entre el diseño solicitado y la estructura real del proyecto.
- Documenta riesgos, limitaciones y procedimiento de reversión.

Modo de trabajo:

- Implementa en pasos pequeños.
- Realiza commits pequeños y coherentes.
- Después de cada paso ejecuta las pruebas pertinentes.
- No elimines pruebas existentes.
- No cambies versiones de dependencias salvo que sea imprescindible.
- No declares la tarea terminada mientras falle una prueba.
- No modifiques código fuera del alcance sin justificarlo.

Al finalizar, muestra:

1. Resumen de cambios.
2. Archivos creados.
3. Archivos modificados.
4. Decisiones técnicas.
5. Comandos ejecutados.
6. Resultados de pruebas.
7. Resultado de los casos 200, 401 y 403.
8. Resultado de la prueba de equivalencia.
9. Resultado de compatibilidad con GET /vets.
10. Resultados de carga disponibles.
11. Riesgos o pendientes.
12. Verificación completa de la Definition of Done.
```

---

## Resultado esperado del agente

Al terminar, Copilot debe entregar un reporte similar a:

```text
Estado: COMPLETADO | PARCIAL | BLOQUEADO

Resumen:
- ...

Archivos creados:
- ...

Archivos modificados:
- ...

Pruebas:
- Total:
- Exitosas:
- Fallidas:

Validación HTTP:
- 200 autorizado:
- 401 sin token:
- 401 token inválido:
- 403 sin autoridad:
- GET /vets:

Equivalencia:
- Cantidad:
- IDs:
- Nombres:
- Apellidos:
- Especialidades:

Carga:
- p95 legado:
- p95 modernizado:
- Diferencia:
- Tasa de errores:
- Cumple umbral:

Pendientes:
- ...

Definition of Done:
- [x] ...
- [ ] ...
```
