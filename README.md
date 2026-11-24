```
Integrantes: Ignacio Paredes, Marthin Celedon, Facundo Benítez · Grupo 13
```

<img width="120" height="120" alt="Logotipo GameHub" src="https://github.com/user-attachments/assets/a6b356aa-b4b6-4494-a9d5-ac19ece491d3" />

GameHub Store es el backend de una tienda gamer online, construido con microservicios. Gestiona cuentas, catálogo, inventario, promociones, órdenes, pagos, despachos y reseñas. El frontend está en [V2Gamehub_Frontend_Grupo13](https://github.com/Rnachoo/V2Gamehub_Frontend_Grupo13).

## Stack

* **Java 21 · Spring Boot 4.0.6 · Spring Cloud 2025.1**
* **Spring Cloud Gateway + Eureka**: punto de entrada único y registro de servicios
* **Spring Security + JWT (HS256)**: roles `ROLE_USER`, `ROLE_OPERADOR` y `ROLE_ADMIN`
* **OpenFeign**: llamadas entre servicios con token de servicio
* **Spring Data JPA + H2**: una base por servicio, guardada en archivo
* **HATEOAS · Bean Validation · springdoc (Swagger)**
* **JUnit + Mockito + Datafaker**: pruebas unitarias

## Arquitectura

```
Frontend (React)
      │  HTTP + JWT
      ▼
API Gateway :8080 ── Eureka :8761
      │
      ├── auth :8000        ├── order :8001
      ├── user :8002        ├── payment :8003
      ├── category :8004    ├── shipping :8090
      ├── product :8091     ├── review :8040
      ├── inventory :8005   └── promotion :8009
```

| Servicio | Ruta base (`/api/v1`) | Responsabilidad |
|---|---|---|
| auth | `/auths` | Registro, login, renovación del token y roles |
| user | `/users` | Perfil, teléfono y direcciones |
| category | `/categories` | Categorías del catálogo |
| product | `/productos` | Productos, ofertas y destacados |
| inventory | `/inventories` | Stock, reservas y movimientos |
| promotion | `/promociones` | Cupones con vigencia, tope y usos |
| order | `/ordenes` | Órdenes, descuentos y estados |
| payment | `/payments` | Pagos aprobados o rechazados |
| shipping | `/despachos` | Despachos y seguimiento |
| review | `/resenas` | Reseñas de productos comprados |


## Docker

Con Docker Desktop abierto, desde la raíz del backend:

```sh
docker compose up --build -d --wait   # primera vez
docker compose up -d --wait           # siguientes veces
docker compose down                   # detener
```

Levanta Eureka, el Gateway y los 10 servicios. Solo el Gateway queda expuesto, en `http://localhost:8080`, y cada base H2 vive en su propio volumen. El frontend en Vercel o en `localhost:5173` se conecta a ese Gateway. Swagger queda en `http://localhost:8080/docs/swagger-ui.html`.
