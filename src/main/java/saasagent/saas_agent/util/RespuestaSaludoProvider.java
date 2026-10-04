package saasagent.saas_agent.util;

import org.springframework.stereotype.Component;

@Component
public class RespuestaSaludoProvider {

    private static final String RESPUESTA = "¡Hola Cómo estás! ¿En qué puedo ayudarte?";

    public String obtenerRespuesta() {
        return RESPUESTA;
    }
}
