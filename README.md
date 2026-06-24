# API Gateway — SmartLogix

Puerta de entrada única (API Gateway) de SmartLogix. Enruta las peticiones HTTP hacia los microservicios `api-inventario`, `api-pedidos` y `api-usuario`, centraliza la configuración de CORS, agrega la documentación Swagger de todos los servicios y expone un endpoint BFF (Backend For Frontend) con estadísticas del dashboard. Construido con Spring Boot 4.0.5 y Spring Cloud Gateway (WebFlux, reactivo). Puerto: **9090**.

Swagger UI: `http://localhost:9090/swagger-ui.html`

---

## Responsabilidad

A diferencia de los demás microservicios, el gateway **no tiene base de datos ni lógica de dominio**. Sus responsabilidades son:

- **Enrutamiento** (Spring Cloud Gateway): reenvía cada ruta `/api/v1/**` al microservicio correspondiente.
- **CORS**: configuración centralizada de orígenes permitidos para el frontend.
- **Agregación de Swagger**: un único Swagger UI que reúne la documentación OpenAPI de los tres microservicios.
- **BFF** (`/bff/v1`): compone datos de varios servicios en una sola respuesta (ej. estadísticas del dashboard).

---

## Arquitectura

```
api-gateway
├── ApiGatewayApplication       Punto de entrada Spring Boot (reactivo)
├── GatewayCorsConfig           CorsWebFilter — CORS centralizado
└── bff/
    ├── BffController           /bff/v1 — agregación de datos (BFF)
    └── WebClientConfig         WebClients hacia cada microservicio
```

El enrutamiento se declara de forma **declarativa** en `application.yml` (sección `spring.cloud.gateway`), no en código Java.

### Enrutamiento

| Ruta entrante | Microservicio destino | Variable de host |
|---------------|-----------------------|------------------|
| `/api/v1/products/**` | api-inventario | `INVENTORY_IP` |
| `/api/v1/branches/**` | api-inventario | `INVENTORY_IP` |
| `/api/v1/warehouses/**` | api-inventario | `INVENTORY_IP` |
| `/api/v1/orders/**` | api-pedidos | `ORDERS_IP` |
| `/api/v1/users/**` | api-usuario | `USERS_IP` |
| `/api/v1/roles/**` | api-usuario | `USERS_IP` |
| `/swagger-ui/index.html` | panel swagger | N/A |
Todos los microservicios destino escuchan en el mismo puerto (`MICROSERVICE_PORT`, por defecto `8081`); se diferencian por su IP/host.

### Patrones de diseño aplicados

**1. API Gateway**
Un único punto de entrada concentra el acceso a todos los microservicios. El cliente (frontend) sólo conoce la dirección del gateway; la topología interna (IPs y puertos de cada servicio) queda oculta y puede cambiar sin afectar al cliente. Beneficio: simplifica CORS, autenticación y observabilidad al centralizarlas.

**2. BFF (Backend For Frontend)**
`BffController` ofrece endpoints diseñados a la medida del frontend. En lugar de que la UI haga seis llamadas para armar el dashboard, el gateway las realiza en paralelo (`Mono.zip`) y devuelve un único objeto agregado. Reduce el número de viajes de red desde el cliente.

---

## Estructura de directorios

```
api-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/example/api_gateway/
│   │   │   ├── ApiGatewayApplication.java
│   │   │   ├── GatewayCorsConfig.java
│   │   │   └── bff/
│   │   │       ├── BffController.java
│   │   │       └── WebClientConfig.java
│   │   └── resources/
│   │       ├── application.yml          # Rutas, CORS y Swagger
│   │       └── static/index.html
│   └── test/
│       └── java/com/example/api_gateway/
│           ├── GatewayCorsConfigTest.java
│           └── bff/
│               ├── BffControllerTest.java
│               └── WebClientConfigTest.java
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Endpoints propios del gateway

### BFF — `/bff/v1`

| Método | Ruta | Descripción | Respuesta |
|--------|------|-------------|-----------|
| GET | `/bff/v1/dashboard` | Conteos agregados de todos los servicios | 200 |

**Ejemplo respuesta `GET /bff/v1/dashboard`:**

```json
{
  "productos": 24,
  "sucursales": 3,
  "bodegas": 5,
  "ordenes": 18,
  "usuarios": 12,
  "roles": 3
}
```

Si un servicio no responde, su conteo se devuelve como `0` (degradación controlada vía `onErrorReturn`).

El resto de rutas (`/api/v1/**`) son **proxy transparente** hacia los microservicios; su documentación está en el README de cada servicio y en el Swagger UI agregado.

---

## Dependencias principales (`pom.xml`)

| Artefacto | Versión | Propósito |
|-----------|---------|-----------|
| spring-boot-starter-parent | 4.0.5 | BOM de Spring Boot |
| spring-cloud-starter-gateway-server-webflux | 2025.1.1 | Gateway reactivo (Spring Cloud) |
| spring-boot-starter-actuator | — | Health checks y métricas (`/actuator`) |
| springdoc-openapi-starter-webflux-ui | 3.0.3 | Swagger UI agregado |
| spring-boot-devtools | — | Recarga en caliente (desarrollo) |
| spring-boot-starter-test / reactor-test | — | Pruebas (incluye soporte reactivo) |

---

## Instalación y ejecución

**Requisitos previos:** Java 17, Maven 3.8+. Los microservicios destino deben estar accesibles (localmente o por IP).

```bash
# 1. Compilar
./mvnw clean package -DskipTests

# 2. Ejecutar
./mvnw spring-boot:run
```

Variables de entorno disponibles:

| Variable | Por defecto | Descripción |
|----------|-------------|-------------|
| `INVENTORY_IP` | `localhost` | Host de api-inventario |
| `ORDERS_IP` | `localhost` | Host de api-pedidos |
| `USERS_IP` | `localhost` | Host de api-usuario |
| `MICROSERVICE_PORT` | `8081` | Puerto común de los microservicios destino |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Orígenes permitidos (separados por coma) |

### Con Docker

```bash
docker build -t smartlogix-gateway .
docker run -p 9090:9090 \
  -e INVENTORY_IP=api-inventario \
  -e ORDERS_IP=api-pedidos \
  -e USERS_IP=api-usuario \
  -e MICROSERVICE_PORT=8081 \
  -e CORS_ALLOWED_ORIGINS=http://localhost:3000 \
  smartlogix-gateway
```

---

## Pruebas

```bash
# Ejecutar pruebas unitarias
./mvnw test

# Reporte de cobertura JaCoCo:
# target/site/jacoco/index.html

# Reportes de Surefire:
# target/surefire-reports/
```

La cobertura objetivo es ≥ 60% sobre las funcionalidades del servicio.

---

## Estrategia de branching

| Rama | Propósito |
|------|-----------|
| `main` | Código en producción |
| `develop` | Integración de cambios |
| `feature/*` | Nuevas funcionalidades |
| `fix/*` | Corrección de bugs |
