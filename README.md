# SaaS Agent

![CI](https://github.com/MarcoJosue25/saas-agent/actions/workflows/ci.yml/badge.svg)

Agente de ventas con IA para pequeños negocios. Responde preguntas sobre los
productos de un negocio consultando su base de datos real, en lugar de inventar
las respuestas, y registra pedidos. Es multi-tenant: varias empresas comparten la
misma base de datos y cada una ve solo sus datos. Se puede probar de punta a punta
con `curl` o Postman, sin depender de ningún canal externo.

Lo hago paso a paso, una pieza a la vez. Mi objetivo es practicar y mostrar mis
conocimientos de desarrollo backend a nivel junior-middle.

## Cómo funciona

```
POST /api/v1/mensajes
  → ConversacionService: busca la empresa, la conversación y los últimos 10 mensajes
      → saludo simple: responde una plantilla fija, sin llamar al modelo
      → otro mensaje: AgenteService
          → arma las instrucciones (empresa, catálogo e historial) y llama a Gemini
          → si el modelo responde texto, se devuelve tal cual
          → si pide la función crear_pedido:
              ValidadorService  comprueba producto, stock y precio contra la base
              PedidoService     registra el pedido y descuenta el stock
```

**Que el modelo no invente.** El modelo solo propone el pedido; quien decide es el código:

- El validador comprueba contra la base que cada producto exista, esté activo y sea
  de esa empresa, y que haya stock. El total se calcula con el precio real.
- El pedido y el descuento de stock van en una transacción. El descuento es un
  `update ... where stock >= cantidad`, así que dos pedidos a la vez no dejan el
  stock en negativo.
- La confirmación que recibe el cliente la arma el código con lo guardado, no el modelo.

**Stack:** Java 21, Spring Boot 4, MySQL 8 con Flyway, Maven, Lombok, Gemini (Vertex AI)
con `RestClient`, JUnit 5 con Mockito, Docker Compose y GitHub Actions.

## Cómo ejecutarlo

Necesitas Java 21, Docker y un proyecto de Google Cloud con Vertex AI activado.

```bash
gcloud auth application-default login   # credenciales para Gemini
cp .env.example .env                    # cambia las contraseñas
docker compose up -d                    # MySQL en el puerto 3310
```

Define `DB_USER`, `DB_PASSWORD`, `DB_URL` (con el puerto 3310) y `GCP_PROJECT_ID`
en tu terminal o en el IDE. Después:

```bash
./mvnw spring-boot:run    # Flyway crea las tablas
# datos de prueba: dos tiendas de ropa inventadas
mysql -h 127.0.0.1 -P 3310 -u saas_agent -p --default-character-set=utf8mb4 negocios_saasagent < scripts/seed/basics_moda.sql
mysql -h 127.0.0.1 -P 3310 -u saas_agent -p --default-character-set=utf8mb4 negocios_saasagent < scripts/seed/urbano_jeans.sql
```

## Ejemplo de uso

```bash
curl -X POST http://localhost:8080/api/v1/mensajes \
  -H "Content-Type: application/json" \
  -d '{"idNumeroMeta": "100000000000001", "telefonoCliente": "51999999999", "texto": "¿cuánto cuesta el polo básico negro?"}'
```

```json
{ "respuesta": "El Polo básico negro cuesta S/ 35.00 y tenemos stock en varias tallas." }
```

`idNumeroMeta` es el identificador del número de WhatsApp del negocio y sirve para
saber de qué empresa es el mensaje. Con `100000000000002` (la otra tienda) el mismo
polo cuesta S/ 29.90. Si la empresa no existe responde `404`, y si falta algún campo, `400`.

Cuando el cliente confirma una compra, el agente registra el pedido y responde, por
ejemplo: "Listo, tu pedido #1 quedó registrado: 2 x Polo básico negro (talla M).
Total: S/ 70.00".

## Pruebas

`./mvnw clean test` corre pruebas unitarias con Mockito (filtro de saludos, cliente de
Gemini con un servidor simulado, agente y servicio de conversación) y una prueba
contra MySQL real que comprueba el aislamiento entre empresas, por lo que necesita
la base de datos y las variables `DB_USER` y `DB_PASSWORD`. El CI de GitHub Actions
las corre en cada push, con una MySQL temporal.

## Alcance y limitaciones

- No tiene inicio de sesión ni CRUD de productos: el catálogo se carga con datos de prueba.
- Está pensado para conectarse a WhatsApp y por eso se identifica la empresa por su idNumeroMeta. 
  Sin embargo el endpoint es independiente del canal y recibe la empresa, el teléfono y el texto.
- El validador protege y confirma los datos de la empresa (producto, stock y precio), 
  Las respuestas despenden del catálogo que se le de al modelo.
- El catálogo completo viaja en cada mensaje. Con cientos de productos habría que 
  buscar solo los relevantes.
- Más adelante: Integracion real con Whatsapp API.