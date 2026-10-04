package saasagent.saas_agent.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

@Component
public class FiltroIntencionRapida {

    // Lista de mensajes esperados del cliente para responder automáticamente
    private static final Set<String> SALUDOS = Set.of(
            "hola", "buenos dias", "buenas tardes", "buenas noches");

    public boolean esSaludo(String mensaje) {
        if (mensaje == null) {
            return false;
        }
        return SALUDOS.contains(normalizar(mensaje));
    }

    private String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
