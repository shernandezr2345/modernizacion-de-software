# Experimento: API REST segura para consulta de veterinarios

## 1. Objetivo

Implementar y validar un endpoint REST versionado y protegido mediante JWT dentro del monolito Spring PetClinic existente:

```http
GET /api/v1/vets
Authorization: Bearer <JWT con VETS_READ>
```

El experimento evalúa si es viable agregar una frontera REST segura al dominio Vet sin extraer un microservicio, sin modificar el esquema de base de datos y sin romper el comportamiento legado.

---

## 2. Alcance

### Incluido

- Endpoint `GET /api/v1/vets` con autenticación JWT y autorización `VETS_READ`.
- DTOs explícitos `VetResponse` y `SpecialtyResponse`.
- Capa de servicio de consulta `VetQueryService`.
- Configuración de seguridad con dos `SecurityFilterChain`.
- Pruebas automatizadas para `200`, `401`, `403`, equivalencia funcional y compatibilidad legada.
- Scripts k6 reproducibles para comparación de carga.

### Excluido

- Extracción de microservicio.
- Modificación de `VetController`, `schema.sql`, `data.sql`.
- Eliminación de Thymeleaf o `GET /vets`.
- Integración con Keycloak, Auth0 u otro Identity Provider real.
- SPA, API Gateway, base de datos separada.

---

## 3. Arquitectura implementada

```
Cliente REST
    │
    │ GET /api/v1/vets
    │ Authorization: Bearer <JWT>
    ▼
SecurityFilterChain (Order 1) — /api/v1/**
    │ valida JWT con clave pública RSA
    │ verifica autoridad VETS_READ
    ▼
VetsApiController  (@RestController)
    │
    ▼
VetQueryService  (@Service, readOnly=true)
    │
    ▼
VetRepository  (Spring Data JPA, sin modificar)
    │
    ▼
H2 / MySQL / PostgreSQL

────────────────────────────────────────────
GET /vets → SecurityFilterChain (Order 2) → VetController (Thymeleaf, sin cambios)
```

---

## 4. Archivos creados

| Archivo | Propósito |
|---------|-----------|
| `src/main/java/.../vet/api/dto/SpecialtyResponse.java` | DTO de especialidad (record) |
| `src/main/java/.../vet/api/dto/VetResponse.java` | DTO de veterinario (record) |
| `src/main/java/.../vet/service/VetQueryService.java` | Servicio de consulta readOnly |
| `src/main/java/.../vet/api/VetsApiController.java` | Controlador REST `/api/v1/vets` |
| `src/main/java/.../system/SecurityConfiguration.java` | Dos SecurityFilterChain |
| `src/main/resources/keys/test-public.pem` | Clave pública RSA-2048 (solo pruebas locales) |
| `src/test/java/.../vet/service/VetQueryServiceTest.java` | Pruebas unitarias del servicio (6) |
| `src/test/java/.../vet/api/VetsApiControllerIntegrationTest.java` | Pruebas de integración de seguridad (8) |
| `src/test/java/.../vet/api/VetApiEquivalenceTest.java` | Prueba de equivalencia funcional (1) |
| `performance/modern-vets.js` | Script k6 para API modernizada |
| `performance/legacy-vets.js` | Script k6 para endpoint legado |
| `performance/README.md` | Instrucciones de ejecución de carga |
| `performance/results/` | Directorio para resultados (vacío, gitignored) |
| `docs/experiment-secure-vets-api.md` | Este documento |

---

## 5. Archivos modificados

| Archivo | Cambio |
|---------|--------|
| `pom.xml` | Dependencias: `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-security-test` (test) |
| `src/main/resources/application.properties` | Propiedad `spring.security.oauth2.resourceserver.jwt.public-key-location` |
| `.gitignore` | Exclusión de `test-private-key.pem` |
| `src/test/java/.../vet/VetControllerTests.java` | Exclusión de `OAuth2ResourceServerWebSecurityAutoConfiguration` en `@WebMvcTest` |
| `src/test/java/.../owner/OwnerControllerTests.java` | Idem |
| `src/test/java/.../owner/VisitControllerTests.java` | Idem |
| `src/test/java/.../system/WelcomeControllerTests.java` | Idem |

---

## 6. Dependencias agregadas

Todas heredan su versión del BOM de Spring Boot 4.1.0:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<!-- scope test -->
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-test</artifactId>
  <scope>test</scope>
</dependency>
```

---

## 7. Configuración JWT

```properties
spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:keys/test-public.pem
```

Para producción, sobreescribir con la variable de entorno:

```bash
export JWT_PUBLIC_KEY_LOCATION=file:/path/to/production-public.pem
```

O como propiedad:

```
-Dspring.security.oauth2.resourceserver.jwt.public-key-location=file:/path/to/key.pem
```

---

## 8. Claim utilizado

```json
{
  "sub": "usuario",
  "authorities": ["VETS_READ"],
  "iat": 1234567890,
  "exp": 1234571490
}
```

Configuración del converter:

```java
JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
converter.setAuthoritiesClaimName("authorities");
converter.setAuthorityPrefix("");
```

El prefijo vacío hace que `VETS_READ` en el claim se mapee directamente a la autoridad `VETS_READ`, sin agregar `ROLE_` ni `SCOPE_`.

---

## 9. Autoridad utilizada

`VETS_READ` — autoridad personalizada, sin prefijo automático.

---

## 10. Cómo compilar

```bash
# Linux / macOS
./mvnw compile

# Windows
.\mvnw.cmd compile
```

Con Maven propio:

```bash
mvn compile
```

Requisitos: Java 17+ (validado con Java 21.0.10).

---

## 11. Cómo ejecutar pruebas

```bash
# Todas las pruebas (omite docker-compose en CI)
./mvnw test -Dspring.docker.compose.lifecycle-management=none

# Solo pruebas de la API
./mvnw test -Dtest="VetQueryServiceTest,VetsApiControllerIntegrationTest,VetApiEquivalenceTest"

# Solo prueba de equivalencia
./mvnw test -Dtest=VetApiEquivalenceTest
```

Resultados esperados: 15 pruebas nuevas — 6 unitarias + 8 integración + 1 equivalencia — todas verdes.

---

## 12. Cómo iniciar la aplicación

```bash
./mvnw spring-boot:run

# Con perfil MySQL
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql

# Con perfil PostgreSQL
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

La aplicación escucha en `http://localhost:8080` por defecto.

---

## 13. Cómo generar o proporcionar tokens de prueba

### Con Python (pyjwt + cryptography)

```bash
pip install pyjwt cryptography

python3 - <<'EOF'
import jwt, time
from cryptography.hazmat.primitives.serialization import load_pem_private_key

with open("test-private-key.pem", "rb") as f:
    key = load_pem_private_key(f.read(), password=None)

token = jwt.encode(
    {
        "sub": "test-user",
        "authorities": ["VETS_READ"],
        "iat": int(time.time()),
        "exp": int(time.time()) + 3600
    },
    key,
    algorithm="RS256"
)
print(token)
EOF
```

### Con openssl + bash (manual)

```bash
# Header + payload en base64url, firma con clave privada RSA
# Usar una librería o herramienta; no construir JWT manualmente en producción.
```

> **Nota:** La clave privada `test-private-key.pem` está en `.gitignore` y NO debe versionarse. Solo existe localmente.

---

## 14. Cómo probar 200, 401 y 403

### 200 — Token válido con VETS_READ

```bash
export TOKEN="<JWT generado con VETS_READ>"

curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  http://localhost:8080/api/v1/vets
# → 200

curl -s \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  http://localhost:8080/api/v1/vets | python3 -m json.tool
```

Respuesta esperada:

```json
[
  {
    "id": 1,
    "firstName": "James",
    "lastName": "Carter",
    "specialties": []
  },
  ...
]
```

### 401 — Sin token

```bash
curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8080/api/v1/vets
# → 401
```

### 401 — Token inválido

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer this.is.not.a.valid.jwt" \
  http://localhost:8080/api/v1/vets
# → 401
```

### 403 — Token válido sin VETS_READ

```bash
export TOKEN_NO_AUTH="<JWT sin la autoridad VETS_READ>"

curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN_NO_AUTH" \
  http://localhost:8080/api/v1/vets
# → 403
```

---

## 15. Cómo verificar GET /vets

```bash
# HTML (comportamiento por defecto)
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/vets
# → 200

# JSON
curl -s -H "Accept: application/json" http://localhost:8080/vets
# → {"vetList":[...]}

# HTML directo
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/vets.html
# → 200
```

El endpoint legado no requiere autenticación.

---

## 16. Cómo ejecutar k6

### Prerequisito

Instalar k6: https://k6.io/docs/get-started/installation/

### API modernizada

```bash
export JWT_WITH_VETS_READ="<token generado>"

# Corrida única
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e JWT_TOKEN="$JWT_WITH_VETS_READ" \
  performance/modern-vets.js

# 5 corridas con exportación de resultados
for i in 1 2 3 4 5; do
  k6 run \
    --summary-export=performance/results/modern-run-${i}.json \
    -e BASE_URL=http://localhost:8080 \
    -e JWT_TOKEN="$JWT_WITH_VETS_READ" \
    performance/modern-vets.js
done
```

### Endpoint legado

```bash
for i in 1 2 3 4 5; do
  k6 run \
    --summary-export=performance/results/legacy-run-${i}.json \
    -e BASE_URL=http://localhost:8080 \
    performance/legacy-vets.js
done
```

---

## 17. Cómo interpretar resultados

Métricas clave en la salida de k6:

| Métrica | Significado |
|---------|-------------|
| `http_req_duration` | Tiempo total de solicitud (p50, p95, p99, máximo) |
| `http_req_failed` | Tasa de solicitudes fallidas (debe ser 0) |
| `checks` | Porcentaje de validaciones pasadas (status 200, JSON válido) |
| `http_reqs` | Total de solicitudes enviadas |

### Comparación

```
diferencia_porcentual = ((p95_modernizado - p95_legado) / p95_legado) * 100
```

- Si `diferencia_porcentual ≤ 20%` → umbral cumplido.
- Si `diferencia_porcentual > 20%` → investigar causas (overhead JWT, serialización, falta de caché).

### Amenazas a la validez

1. `GET /vets` renderiza Thymeleaf (HTML) por defecto; `GET /api/v1/vets` serializa JSON puro. La diferencia en tiempo de renderizado no es atribuible únicamente a la capa de seguridad.
2. El endpoint legado usa `@Cacheable("vets")` vía `VetController`; la API moderna también accede a la misma caché a través de `VetRepository`. El primer request de cada corrida puede ser más lento (cache miss).
3. La validación JWT agrega una operación criptográfica RSA por solicitud. Este costo es esperado y no es una regresión funcional.

---

## 18. Características técnicas del entorno de pruebas

Todas las corridas de carga y pruebas automatizadas se ejecutaron en la siguiente máquina, sin cargas externas concurrentes.

### Hardware

| Componente | Detalle |
|------------|---------|
| Equipo | ASUS ROG Zephyrus G16 GU605MI |
| CPU | Intel Core Ultra 9 185H — 16 núcleos físicos / 22 hilos lógicos — base 2.5 GHz |
| RAM | 32 GB DDR5 |
| Almacenamiento | SSD NVMe Micron MTFDKBA1T0QFM — 1 TB |

### Software

| Componente | Versión |
|------------|---------|
| Sistema operativo | Windows 11 Pro 25H2 (build 26200) — 64 bits |
| JDK | Oracle JDK 21.0.10 (build 21.0.10+8-LTS-217, HotSpot Server VM) |
| Spring Boot | 4.1.0 |
| k6 | v2.0.0 (go1.26.3, windows/amd64) |
| Maven | 3.9.11 |
| Base de datos (pruebas) | H2 2.4.240 (in-memory, perfil `default`) |

### Consideraciones de validez

- La JVM y el servidor Tomcat se ejecutaron en el mismo proceso que las pruebas de carga k6, lo que puede inflar ligeramente la latencia en comparación con un entorno aislado.
- Las corridas se realizaron con la JVM ya calentada (una corrida previa de 1 minuto no incluida en los resultados).
- No se ejecutaron otras cargas de trabajo significativas durante las corridas.
- Los resultados son representativos del entorno de desarrollo local, no de producción.

---

## 19. Riesgos y limitaciones del experimento

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Clave privada de prueba comprometida | Bajo (solo afecta entorno local) | La clave está en `.gitignore`; rotar generando un nuevo par RSA |
| Token JWT con larga expiración en logs | Medio | No registrar tokens en logs de aplicación |
| Comparación HTML vs JSON no equivalente | Medio | Documentado como amenaza a la validez; comparar `GET /vets` con `Accept: application/json` |
| `PetClinicConcurrencyTests` flaky | Bajo | Es una prueba pre-existente con condición de carrera temporal; no está relacionada con este cambio |
| Todos los `@WebMvcTest` deben excluir `OAuth2ResourceServerWebSecurityAutoConfiguration` | Medio | Aplicado a todos los archivos afectados; nuevas pruebas @WebMvcTest deben incluir la exclusión |

---

## 20. Decisión final del experimento

**CONTINUAR** — todos los criterios verificables se cumplen.

### Resultados de carga (10 corridas, 20 VUs, 3 min c/u)

| Escenario | Corrida | p50 (ms) | p95 (ms) | Máximo (ms) | Errores |
|-----------|---------|----------|----------|-------------|---------|
| Legado | 1 | 0.726 | 1.052 | 61.058 | 0% |
| Legado | 2 | 0.682 | 1.147 | 4.139 | 0% |
| Legado | 3 | 0.737 | 1.138 | 3.361 | 0% |
| Legado | 4 | 0.716 | 1.150 | 3.041 | 0% |
| Legado | 5 | 0.783 | 1.215 | 5.103 | 0% |
| Modernizado | 1 | 0.775 | 1.343 | 12.927 | 0% |
| Modernizado | 2 | 0.768 | 1.177 | 13.479 | 0% |
| Modernizado | 3 | 0.772 | 1.303 | 12.654 | 0% |
| Modernizado | 4 | 0.812 | 1.298 | 14.126 | 0% |
| Modernizado | 5 | 0.786 | 1.186 | 15.986 | 0% |

| Métrica | Valor |
|---------|-------|
| Mediana p95 legado | 1.147 ms |
| Mediana p95 modernizado | 1.298 ms |
| Diferencia porcentual | +13.2% |
| Umbral (≤ 20%) | ✅ CUMPLIDO |
| p95 < 500 ms | ✅ CUMPLIDO |
| Tasa de error | 0% |

Estado de los criterios verificables:

| Criterio | Estado |
|----------|--------|
| `GET /api/v1/vets` retorna JSON | ✅ |
| Respuesta contiene id, firstName, lastName, specialties | ✅ |
| Sin token → 401 | ✅ |
| Token inválido → 401 | ✅ |
| Token sin VETS_READ → 403 | ✅ |
| Token con VETS_READ → 200 | ✅ |
| `GET /vets` conserva comportamiento | ✅ |
| Requests rechazados no invocan VetQueryService | ✅ |
| Equivalencia funcional con datos reales | ✅ |
| Suite completa sin regresiones | ✅ |
| Desempeño (k6) | ✅ mediana p95 modernizado = 1.298 ms (+13.2% vs legado) |

---

## 21. Procedimiento de reversión

Para revertir completamente el experimento sin afectar el comportamiento legado:

```bash
# 1. Revertir archivos modificados
git checkout HEAD -- pom.xml
git checkout HEAD -- src/main/resources/application.properties
git checkout HEAD -- .gitignore
git checkout HEAD -- src/test/java/org/springframework/samples/petclinic/vet/VetControllerTests.java
git checkout HEAD -- src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java
git checkout HEAD -- src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java
git checkout HEAD -- src/test/java/org/springframework/samples/petclinic/system/WelcomeControllerTests.java

# 2. Eliminar archivos nuevos
git rm -r src/main/java/org/springframework/samples/petclinic/vet/api/
git rm -r src/main/java/org/springframework/samples/petclinic/vet/service/
git rm src/main/java/org/springframework/samples/petclinic/system/SecurityConfiguration.java
git rm src/main/resources/keys/test-public.pem
git rm -r src/test/java/org/springframework/samples/petclinic/vet/api/
git rm src/test/java/org/springframework/samples/petclinic/vet/service/VetQueryServiceTest.java
git rm -r performance/
git rm -r docs/

# 3. Compilar y verificar
./mvnw test

# 4. Hacer commit
git commit -m "revert: remove secure vets API experiment"
```

> Todos los cambios están aislados en archivos nuevos o en secciones claramente delimitadas de archivos existentes. El reverso no afecta `VetController`, `VetRepository`, `Vet`, `Specialty` ni ningún esquema de base de datos.
