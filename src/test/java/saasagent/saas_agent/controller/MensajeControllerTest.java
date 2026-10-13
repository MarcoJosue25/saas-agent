package saasagent.saas_agent.controller;

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MensajeController.class)
class MensajeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ConversacionService conversacionService;

    @Test
    void unMensajeValidoDevuelveLaRespuestaDelAgente() throws Exception {
        when(conversacionService.procesarMensaje(any(MensajeRequest.class))).thenReturn("Cuesta S/ 35.00");

        mockMvc.perform(post("/api/v1/mensajes").contentType(MediaType.APPLICATION_JSON)
                        .content(json("100000000000001", "51999999999", "¿cuánto cuesta el polo?")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value("Cuesta S/ 35.00"));
    }

    @Test
    void unTextoVacioDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/mensajes").contentType(MediaType.APPLICATION_JSON)
                        .content(json("100000000000001", "51999999999", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El texto del mensaje no puede estar vacío"));

        verifyNoInteractions(conversacionService);
    }

    @Test
    void unaEmpresaInexistenteDevuelve404() throws Exception {
        when(conversacionService.procesarMensaje(any(MensajeRequest.class)))
                .thenThrow(new EmpresaNoEncontradaException("No existe una empresa activa con ese número"));

        mockMvc.perform(post("/api/v1/mensajes").contentType(MediaType.APPLICATION_JSON)
                        .content(json("999", "51999999999", "hola")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe una empresa activa con ese número"));
    }

    private static String json(String idNumeroMeta, String telefono, String texto) {
        return """
                {"idNumeroMeta": "%s", "telefonoCliente": "%s", "texto": "%s"}
                """.formatted(idNumeroMeta, telefono, texto);
    }
}
