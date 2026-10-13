package saasagent.saas_agent.whatsapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import saasagent.saas_agent.dto.MensajeRequest;
import saasagent.saas_agent.exception.EmpresaNoEncontradaException;
import saasagent.saas_agent.service.ConversacionService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = WhatsAppWebhookController.class, properties = "app.whatsapp.verify-token=token-de-prueba")
class WhatsAppWebhookControllerTest {

    // Mensaje de texto con el formato que manda Meta
    private static final String MENSAJE_DE_META = """
            {"entry": [{"changes": [{"value": {
              "metadata": {"phone_number_id": "123456"},
              "messages": [{"from": "51999999999", "id": "wamid.PRUEBA001", "type": "text", "text": {"body": "hola"}}]
            }}]}]}
            """;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ConversacionService conversacionService;
    @MockitoBean
    private WhatsAppClient whatsAppClient;

    @Test
    void laVerificacionDevuelveElChallengeSiElTokenCoincide() throws Exception {
        mockMvc.perform(get("/webhook/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "token-de-prueba")
                        .param("hub.challenge", "12345"))
                .andExpect(status().isOk())
                .andExpect(content().string("12345"));
    }

    @Test
    void laVerificacionRechazaUnTokenIncorrecto() throws Exception {
        mockMvc.perform(get("/webhook/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "otro-token")
                        .param("hub.challenge", "12345"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unMensajeDeTextoSeProcesaYSeEnviaLaRespuesta() throws Exception {
        when(conversacionService.procesarMensaje(any(MensajeRequest.class))).thenReturn("Hola, ¿en qué te ayudo?");

        mockMvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(MENSAJE_DE_META))
                .andExpect(status().isOk());

        verify(whatsAppClient).enviarTexto("123456", "51999999999", "Hola, ¿en qué te ayudo?");
    }

    @Test
    void unMensajeRepetidoSeIgnora() throws Exception {
        when(conversacionService.yaProcesado("wamid.PRUEBA001")).thenReturn(true);

        mockMvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(MENSAJE_DE_META))
                .andExpect(status().isOk());

        verify(conversacionService, never()).procesarMensaje(any());
    }

    // Aunque falle algo por dentro, Meta tiene que recibir 200 o reintentar el mensaje
    @Test
    void siAlgoFallaIgualResponde200() throws Exception {
        when(conversacionService.procesarMensaje(any())).thenThrow(new EmpresaNoEncontradaException("no existe"));

        mockMvc.perform(post("/webhook/whatsapp").contentType(MediaType.APPLICATION_JSON).content(MENSAJE_DE_META))
                .andExpect(status().isOk());

        verify(whatsAppClient, never()).enviarTexto(anyString(), anyString(), anyString());
    }
}
