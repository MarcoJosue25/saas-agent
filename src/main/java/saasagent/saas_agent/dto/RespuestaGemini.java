package saasagent.saas_agent.dto;

import tools.jackson.databind.JsonNode;

public record RespuestaGemini(String texto, String funcion, JsonNode argumentos) {

    public boolean esLlamadaAFuncion() {
        return funcion != null;
    }
}
