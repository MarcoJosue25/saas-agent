package saasagent.saas_agent.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import saasagent.saas_agent.config.AppConfig;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * Prueba manual contra Gemini de verdad: gasta crédito, así que solo corre si defino
 * GEMINI_PRUEBA_REAL=true. Necesita GCP_PROJECT_ID y, si quiero otro modelo, GEMINI_MODEL.
 */
@EnabledIfEnvironmentVariable(named = "GEMINI_PRUEBA_REAL", matches = "true")
class GeminiClientRealTest {

    @Test
    void respondeConLaConfiguracionDelEntorno() {
        GeminiClient cliente = new GeminiClient(
                new AppConfig().restClientBuilder(),
                JsonMapper.builder().build(),
                valor("GEMINI_MODEL", "gemini-3.6-flash"),
                valor("GCP_PROJECT_ID", ""));

        String respuesta = cliente.generarRespuesta(
                "Eres un asistente de una tienda de ropa. Responde en una frase corta.",
                "Hola, ¿cómo estás?");

        System.out.println("Respuesta de Gemini: " + respuesta);
        assertThat(respuesta).isNotBlank();
    }

    private static String valor(String variable, String porDefecto) {
        String valor = System.getenv(variable);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }
}
