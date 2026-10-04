package saasagent.saas_agent.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import saasagent.saas_agent.dto.RespuestaGemini;
import saasagent.saas_agent.exception.GeminiException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {

    private static final String URL = "https://aiplatform.googleapis.com/v1/projects/proyecto-prueba"
            + "/locations/global/publishers/google/models/gemini-prueba:generateContent";

    private MockRestServiceServer servidor;
    private GeminiClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        // Solo reemplazo el token para no depender de las credenciales de Google
        cliente = new GeminiClient(builder, JsonMapper.builder().build(), "gemini-prueba", "proyecto-prueba") {
            @Override
            String obtenerToken() {
                return "token-de-prueba";
            }
        };
    }

    @Test
    void mandaLasInstruccionesYElMensajeYDevuelveElTexto() {
        servidor.expect(requestTo(URL))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer token-de-prueba"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("Sé breve"))
                .andExpect(jsonPath("$.contents[0].role").value("user"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("hola"))
                .andRespond(withSuccess(
                        "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"¡Hola! ¿En qué te ayudo?\"}]}}]}",
                        MediaType.APPLICATION_JSON));

        String respuesta = cliente.generarRespuesta("Sé breve", "hola");

        assertThat(respuesta).isEqualTo("¡Hola! ¿En qué te ayudo?");
        servidor.verify();
    }

    @Test
    void siGeminiDevuelveUnErrorSeLanzaGeminiException() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> cliente.generarRespuesta("instrucciones", "hola"))
                .isInstanceOf(GeminiException.class)
                .hasMessageContaining("No se pudo obtener la respuesta");
    }

    // Un timeout o una caída de red no traen respuesta HTTP, pero también tienen que ser GeminiException
    @Test
    void siSeCaeLaConexionSeLanzaGeminiException() {
        servidor.expect(requestTo(URL)).andRespond(request -> {
            throw new IOException("sin conexión");
        });

        assertThatThrownBy(() -> cliente.generarRespuesta("instrucciones", "hola"))
                .isInstanceOf(GeminiException.class);
    }

    @Test
    void siLaRespuestaNoTraeTextoSeLanzaGeminiException() {
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess("{\"candidates\": []}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.generarRespuesta("instrucciones", "hola"))
                .isInstanceOf(GeminiException.class)
                .hasMessageContaining("no devolvió texto");
    }

    // Si Gemini llama a una función, la respuesta trae su nombre y sus argumentos en vez de texto
    @Test
    void siGeminiLlamaAUnaFuncionSeDevuelveSuNombreYSusArgumentos() {
        String json = """
                {"candidates": [{"content": {"parts": [{"functionCall": {"name": "crear_pedido",
                    "args": {"items": [{"producto": "Polo básico negro", "cantidad": 2}]}}}]}}]}
                """;
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.tools[0].functionDeclarations[0].name").value("crear_pedido"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        RespuestaGemini respuesta = cliente.consultar("instrucciones", "quiero 2 polos negros",
                "[{\"functionDeclarations\": [{\"name\": \"crear_pedido\"}]}]");

        assertThat(respuesta.esLlamadaAFuncion()).isTrue();
        assertThat(respuesta.funcion()).isEqualTo("crear_pedido");
        assertThat(respuesta.argumentos().path("items").path(0).path("cantidad").asInt()).isEqualTo(2);
    }
}
