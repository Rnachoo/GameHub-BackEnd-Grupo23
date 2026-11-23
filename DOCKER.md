# Backend de GameHub con Docker Compose

Compose construye y ejecuta Eureka, el Gateway y los diez microservicios. Los servicios se comunican por nombres dentro de la red de Compose. Cada base H2 se guarda en su propio volumen.

## Arranque local

1. Instala y abre Docker con el plugin Docker Compose.
2. Copia `.env.docker.example` a `.env` en la raíz del backend. Define `JWT_SECRET` con un valor aleatorio de al menos 32 bytes y `DB_PASSWORD` con una contraseña propia. Para la demo, puedes definir `SEED_PASSWORD` antes del primer arranque para crear las cuentas locales.
3. Si el Gateway que arrancaste con Maven sigue en el puerto 8080, detenlo o elige otro valor de `GATEWAY_PORT`.
4. Desde la raíz del backend, ejecuta:

```sh
docker compose up --build -d --wait
docker compose ps
```

Prueba `http://localhost:8080/api/v1/productos`. Si el stack no queda disponible, revisa `docker compose logs msvc-eureka msvc-gateway`. Usa `docker compose down` para detenerlo conservando los volúmenes.

Con el frontend local, configura `VITE_API_URL=http://localhost:8080/api/v1` en el repositorio del frontend y vuelve a iniciar Vite. Ajusta el puerto de la URL si cambiaste `GATEWAY_PORT`.

## Conexión con Vercel

Ejecuta Compose en el servidor del equipo. En ese servidor, configura un proxy HTTPS que escuche en el dominio de la API y reenvíe `/api/v1/` al Gateway local. El puerto de Compose se vincula a `127.0.0.1` para que el proxy pueda acceder a él desde el mismo host. Mantén Eureka y los demás microservicios en la red interna.

Usa claves y contraseñas propias del servidor. Deja `SEED_PASSWORD` vacío en un despliegue público para omitir el seed de cuentas de prueba. Configura `CORS_ALLOWED_ORIGINS` con el dominio del frontend. En Vercel, asigna la URL HTTPS del Gateway con el sufijo `/api/v1` a `VITE_API_URL` de Production y vuelve a desplegar.
