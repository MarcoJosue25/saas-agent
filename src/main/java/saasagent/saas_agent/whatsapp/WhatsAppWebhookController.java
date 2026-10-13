package saasagent.saas_agent.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import saasagent.saas_agent.dto.MensajeRequest;
import saasagent.saas_agent.service.ConversacionService;
import saasagent.saas_agent.whatsapp.WebhookPayload.MensajeEntrante;

@Slf4j
@RestController
public class WhatsAppWebhookController {

    private final ConversacionService conversacionService;
    private final WhatsAppClient whatsAppClient;
    private final String verifyToken;

    public WhatsAppWebhookController(ConversacionService conversacionService, WhatsAppClient whatsAppClient,
                                     @Value("${app.whatsapp.verify-token:}") String verifyToken) {
        this.conversacionService = conversacionService;
        this.whatsAppClient = whatsAppClient;
        this.verifyToken = verifyToken;
    }

    // Meta llama a este GET al configurar el webhook, para comprobar que el servidor es nuestro
    @GetMapping("/webhook/whatsapp")
    public ResponseEntity<String> verificar(@RequestParam("hub.mode") String modo,
                                            @RequestParam("hub.verify_token") String token,
                                            @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(modo) && !verifyToken.isBlank() && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // Siempre responde 200. Si Meta recibe otro código, reintenta el mensaje durante días
    @PostMapping("/webhook/whatsapp")
    public ResponseEntity<Void> recibir(@RequestBody WebhookPayload payload) {
        for (MensajeEntrante mensaje : payload.mensajesDeTexto()) {
            try {
                if (conversacionService.yaProcesado(mensaje.idMensaje())) {
                    continue;
                }
                MensajeRequest request = MensajeRequest.builder()
                        .idNumeroMeta(mensaje.phoneNumberId())
                        .telefonoCliente(mensaje.telefono())
                        .texto(mensaje.texto())
                        .idMensajeMeta(mensaje.idMensaje())
                        .build();
                String respuesta = conversacionService.procesarMensaje(request);
                whatsAppClient.enviarTexto(mensaje.phoneNumberId(), mensaje.telefono(), respuesta);
            } catch (Exception e) {
                log.error("No se pudo procesar el mensaje {}: {}", mensaje.idMensaje(), e.getMessage());
            }
        }
        return ResponseEntity.ok().build();
    }
}