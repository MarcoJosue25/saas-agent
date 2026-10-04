package saasagent.saas_agent.exception;

public class GeminiException extends RuntimeException {
    //
    public GeminiException(String message) {
        super(message);
    }

    // Recibe la causa del error con el tipo de respuesta http 429, 400. etc
    public GeminiException(String message, Throwable causa) {
        super(message, causa);
    }
}
