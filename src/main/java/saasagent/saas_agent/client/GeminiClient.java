package saasagent.saas_agent.client;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import saasagent.saas_agent.exception.GeminiException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;

@Slf4j
@Component
public class GeminiClient {

    private final RestClient restClient;
    private final JsonMapper mapper;
    private final String modelo;
    private final String proyecto;

    private GoogleCredentials credenciales;

    public GeminiClient(RestClient.Builder restClientBuilder,
                        JsonMapper mapper,
                        @Value("${app.gemini.model}") String modelo,
                        @Value("${app.gemini.project:}") String proyecto) {
        this.restClient = restClientBuilder.build();
        this.mapper = mapper;
        this.modelo = modelo;
        this.proyecto = proyecto;
    }
    //Manda el archivo esperado a la IA espera la respuesta generada y la devuelve como String
    public String generarRespuesta(String instrucciones, String mensajeCliente) {
        String url = "https://aiplatform.googleapis.com/v1/projects/" + proyecto
                + "/locations/global/publishers/google/models/" + modelo + ":generateContent";

        log.info("Llamando a Gemini (proyecto {}, modelo {})", proyecto, modelo);

        try {
            String respuesta = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + obtenerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(construirCuerpo(instrucciones, mensajeCliente))
                    .retrieve()
                    .body(String.class);

            return extraerTexto(respuesta);
        } catch (RestClientException e) {
            log.error("Falló la llamada a Gemini: {}", e.getMessage());
            throw new GeminiException("No se pudo obtener la respuesta de Gemini", e);
        }
    }
    //Arma el json con el mensaje del cliente y los datos de la empresa.
    private String construirCuerpo(String instrucciones, String mensajeCliente) {
        ObjectNode cuerpo = mapper.createObjectNode();

        cuerpo.putObject("systemInstruction")
                .putArray("parts")
                .addObject()
                .put("text", instrucciones);

        ObjectNode turno = cuerpo.putArray("contents").addObject();
        turno.put("role", "user");
        turno.putArray("parts").addObject().put("text", mensajeCliente);

        return mapper.writeValueAsString(cuerpo);
    }

    //Recorre el json con path hasta extraer la respuesta
    private String extraerTexto(String respuestaJson) {
        String texto = mapper.readTree(respuestaJson)
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asString("");

        if (texto.isBlank()) {
            throw new GeminiException("Gemini no devolvió texto");
        }
        return texto.trim();
    }

    // Pide un token temporal de las credenciales de tu cuenta de google
    String obtenerToken() {
        try {
            if (credenciales == null) {
                credenciales = GoogleCredentials.getApplicationDefault()
                        .createScoped("https://www.googleapis.com/auth/cloud-platform");
            }
            credenciales.refreshIfExpired();
            return credenciales.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new GeminiException("No se pudieron obtener las credenciales de Google", e);
        }
    }
}
