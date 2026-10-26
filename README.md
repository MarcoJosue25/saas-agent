# SaaS Agent

![CI](https://github.com/MarcoJosue25/saas-agent/actions/workflows/ci.yml/badge.svg)

Agente de ventas con IA para pequeños negocios. Responde preguntas sobre los
productos de un negocio consultando su base de datos real, en lugar de inventar
las respuestas, y registra pedidos. Es multi-tenant: varias empresas comparten la
misma base de datos y cada una ve solo sus datos. Se puede probar de punta a punta
con `curl` o Postman, sin depender de ningún canal externo.


## Cómo funciona

```
POST /api/v1/mensajes    (endpoint de pruebas, con curl o Postman)
POST /webhook/whatsapp   (mensajes reales de WhatsApp: ignora repetidos y responde por WhatsApp)
  → ConversacionService: busca la empresa, la conversación y los últimos 10 mensajes
      → saludo simple: responde una plantilla fija, sin llamar al modelo
      → otro mensaje: AgenteService
          → arma las instrucciones (empresa, catálogo e historial) y llama a Gemini
          → si el modelo responde texto, se devuelve tal cual
          → si pide la función crear_pedido:
              ValidadorService  comprueba producto, stock y precio contra la base
              PedidoService     registra el pedido y descuenta el stock
```

Si Gemini no responde, el cliente recibe un mensaje de respaldo ("Disculpa, dame unos
minutos y te atiendo") en lugar de un error.

**Que el modelo no invente pedidos.** El modelo solo propone el pedido; quien decide es el código:

- El validador comprueba contra la base que cada producto exista, esté activo y sea
  de esa empresa, y que haya stock. El total se calcula con el precio real.
- El pedido y el descuento de stock van en una transacción. El descuento es un
  `update ... where stock >= cantidad` (Un @Query en el repository), así que dos pedidos a la vez no dejan el
  stock en negativo.
- La confirmación que recibe el cliente la arma el código con lo guardado, no el modelo.

**Stack:** Java 21, Spring Boot 4, MySQL 8 con Flyway, Maven, Lombok, Gemini (Vertex AI)
y WhatsApp Cloud API con `RestClient`, JUnit 5 con Mockito, Docker Compose y GitHub Actions.

## Cómo ejecutarlo

Necesitas Java 21, Docker y un proyecto de Google Cloud con Vertex AI activado.

```bash
gcloud auth application-default login   # credenciales para Gemini
cp .env.example .env                    # cambia las contraseñas
docker compose up -d                    # MySQL en el puerto 3310
```

Define `DB_USER`, `DB_PASSWORD`, `DB_URL` (con el puerto 3310) y `GCP_PROJECT_ID`
en tu terminal o en el IDE. Si vas a conectar WhatsApp, define también `WHATSAPP_TOKEN` y
`WHATSAPP_VERIFY_TOKEN` (más abajo); sin ellas todo lo demás funciona igual. Después:

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

`idNumeroMeta` es el identificador del número de WhatsApp del negocio (el `phone_number_id`
de Meta) y sirve para saber de qué empresa es el mensaje. Con `100000000000002` (la otra
tienda) el mismo polo cuesta S/ 29.90. Si la empresa no existe responde `404`, y si falta
algún campo, `400`.

Cuando el cliente confirma una compra, el agente registra el pedido y responde, por
ejemplo: "Listo, tu pedido #1 quedó registrado: 2 x Polo básico negro (talla M).
Total: S/ 70.00".

## Conectar con WhatsApp

El webhook queda en `/webhook/whatsapp` y habla con WhatsApp Cloud API. Para probarlo:

1. Define `WHATSAPP_TOKEN` (el token de acceso de tu app en Meta) y `WHATSAPP_VERIFY_TOKEN`
   (un texto que eliges tú).
2. Expón el puerto 8080 con una herramienta como ngrok.
3. En el panel de Meta, configura la URL `https://<tu-dominio>/webhook/whatsapp` y el mismo
   token de verificación.
4. Registra la empresa con el `phone_number_id` de tu número como `id_numero_meta`.

Meta reintenta durante días los mensajes que no reciben un 200, así que el webhook siempre
responde 200 y descarta los mensajes repetidos usando el id de cada mensaje. Por ahora solo
atiende mensajes de texto.

## Pruebas

`./mvnw clean test` corre más de 40 pruebas: unitarias con Mockito (filtro de saludos,
agente y servicio de conversación), el cliente de Gemini con un servidor simulado, los
controllers con MockMvc (endpoint de mensajes y webhook de WhatsApp) y una prueba contra
MySQL real que comprueba el aislamiento entre empresas. Esa prueba y el test de arranque
necesitan la base de datos y las variables `DB_USER` y `DB_PASSWORD`; para correr las demás
sin ellas: `./mvnw test -Dtest='!AislamientoEmpresasTest,!SaasAgentApplicationTests'`.
El CI de GitHub Actions corre todas en cada push, con una MySQL temporal.

Hay además una prueba manual contra Gemini real, que solo corre con `GEMINI_PRUEBA_REAL=true`
porque gasta crédito.

## Alcance y limitaciones

- No tiene inicio de sesión ni CRUD de productos a propósito porque este prototipo se enfoca en las reglas al agente:
el catálogo se carga con datos de prueba.
- Está conectado a WhatsApp Cloud API y por eso se identifica la empresa por su idNumeroMeta. 
  El endpoint `/api/v1/mensajes` es independiente del canal y recibe la empresa, el teléfono
  y el texto, por eso sirve para probar sin WhatsApp.
- El validador protege y confirma los datos de la empresa (producto, stock y precio). 
  Las respuestas dependen del catálogo que se le da al modelo.
- El catálogo completo viaja en cada mensaje. Con cientos de productos hace falta el uso de rag para traer los datos relevantes.
- El webhook comprueba el token de verificación de Meta, pero todavía no valida la firma
  de cada petición (`X-Hub-Signature-256`).
- Cada mensaje se procesa de forma síncrona: la respuesta de Gemini se espera dentro de la
  misma petición del webhook. Solo se atienden y se envían mensajes de texto.
- La talla no se valida aun contra el catálogo y el stock es por producto, no por talla.