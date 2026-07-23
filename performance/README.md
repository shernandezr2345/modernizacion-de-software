# Performance Tests — PetClinic Vets

Scripts de carga k6 para comparar el endpoint legado `GET /vets` con la API modernizada `GET /api/v1/vets`.

## Requisitos

- [k6](https://k6.io/docs/get-started/installation/) instalado y disponible en el PATH.
- La aplicación en ejecución en `http://localhost:8080` (o valor de `BASE_URL`).
- Un JWT válido con la autoridad `VETS_READ` para el script moderno (variable `JWT_TOKEN`).

## Generar un JWT de prueba

Con la clave privada RSA disponible en `test-private-key.pem` (no versionada):

```bash
# Ejemplo con Python (pyjwt)
python3 - <<'EOF'
import jwt, time
from cryptography.hazmat.primitives.serialization import load_pem_private_key

with open("test-private-key.pem", "rb") as f:
    key = load_pem_private_key(f.read(), password=None)

token = jwt.encode(
    {"sub": "perf-user", "authorities": ["VETS_READ"], "iat": int(time.time()), "exp": int(time.time()) + 3600},
    key,
    algorithm="RS256"
)
print(token)
EOF
```

O use cualquier herramienta (openssl + bash, Postman, jwt.io con la clave privada) que genere un JWT RS256 firmado con la clave pública en `src/main/resources/keys/test-public.pem`.

## Escenario de carga

| Fase | Duración | VUs |
|------|----------|-----|
| Ramp-up | 30 s | 0 → 20 |
| Carga estable | 2 min | 20 |
| Ramp-down | 30 s | 20 → 0 |

Calentamiento recomendado: ejecutar una corrida previa no incluida en las métricas.

## Ejecución

### API modernizada (5 corridas)

```bash
export JWT_WITH_VETS_READ="<token generado>"

for i in 1 2 3 4 5; do
  k6 run \
    --summary-export=performance/results/modern-run-${i}.json \
    -e BASE_URL=http://localhost:8080 \
    -e JWT_TOKEN="$JWT_WITH_VETS_READ" \
    performance/modern-vets.js
done
```

### API legada (5 corridas)

```bash
for i in 1 2 3 4 5; do
  k6 run \
    --summary-export=performance/results/legacy-run-${i}.json \
    -e BASE_URL=http://localhost:8080 \
    performance/legacy-vets.js
done
```

## Umbrales

| Umbral | Criterio |
|--------|---------|
| `http_req_failed` | `rate == 0` (0% de errores) |
| `http_req_duration` | `p(95) < 500 ms` |
| Comparativa | p95 modernizado ≤ p95 legado × 1.20 |

## Tabla de resultados

Completar tras ejecutar las 10 corridas:

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

**Mediana p95 legado:** 1.147 ms  
**Mediana p95 modernizado:** 1.298 ms  
**Diferencia porcentual:** +13.2% — ✅ dentro del umbral de 20%  
**Tasa de errores:** 0% en ambos escenarios  
**VUs:** 20, **Duración:** ramp-up 30s + 2min + ramp-down 30s  
**Ambiente:** misma JVM, misma máquina, perfil default (H2 in-memory)

### Fórmula de comparación

```
diferencia_porcentual = ((p95_modernizado - p95_legado) / p95_legado) * 100
```

## Diferencias conocidas entre endpoints

| Aspecto | GET /vets | GET /api/v1/vets |
|---------|-----------|-----------------|
| Autenticación | Ninguna | JWT con VETS_READ |
| Formato respuesta | `{"vetList":[...]}` | `[...]` (array) |
| Renderizado | Puede devolver HTML (Thymeleaf) sin `Accept: application/json` | Siempre JSON |
| Caché | Habilitada (`@Cacheable("vets")`) | Habilitada via VetQueryService → VetRepository |

> **Amenaza a la validez:** la comparación de latencia entre un endpoint que serializa HTML y otro que serializa JSON no es perfectamente equivalente. Documentar esta diferencia en el análisis de resultados.
