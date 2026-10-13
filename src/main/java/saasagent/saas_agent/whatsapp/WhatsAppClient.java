package saasagent.saas_agent.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Slf4j
@Component
public class WhatsAppClient {

    private static final String URL_MENSAJES = "https://graph.facebook.com/v21.0/{phoneNumberId}/messages";

    private final RestClient restClient;
    private final String token;

    public WhatsAppClient(RestClient.Builder restClientBuilder, @Value("${app.whatsapp.token:}") String token) {
        this.restClient = restClientBuilder.build();
        this.token = token;
    }

    // Si el mensaje falla solo lo registra, porque el webhook igual debe responder 200
    public void enviarTexto(String phoneNumberId, String telefono, String texto) {
        Map<String, Object> cuerpo = Map.of(
                "messaging_product", "whatsapp",
                "to", telefono,
                "type", "text",
                "text", Map.of("body", texto));
        try {
            restClient.post()
                    .uri(URL_MENSAJES, phoneNumberId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("No se pudo enviar el mensaje a {}: {}", telefono, e.getMessage());
        }
    }
}