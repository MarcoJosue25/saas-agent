package saasagent.saas_agent.whatsapp;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

//Spring convierte el json que manda meta en un objeto de esta clase
public record WebhookPayload(List<Entry> entry) {

    public WebhookPayload {
        entry = entry == null ? List.of() : entry;
    }

    // Devuelve solo los mensajes de texto; ignora los avisos de entrega, imágenes, etc.
    public List<MensajeEntrante> mensajesDeTexto() {
        List<MensajeEntrante> mensajes = new ArrayList<>();
        for (Entry e : entry) {
            for (Change cambio : e.changes()) {
                Value valor = cambio.value();
                for (MensajeMeta m : valor.messages()) {
                    if ("text".equals(m.type()) && m.text() != null) {
                        mensajes.add(new MensajeEntrante(
                                valor.metadata().phoneNumberId(), m.from(), m.id(), m.text().body()));
                    }
                }
            }
        }
        return mensajes;
    }

    public record Entry(List<Change> changes) {
        public Entry {
            changes = changes == null ? List.of() : changes;
        }
    }

    public record Change(Value value) {
    }

    public record Value(Metadata metadata, List<MensajeMeta> messages) {
        public Value {
            messages = messages == null ? List.of() : messages;
        }
    }

    public record Metadata(@JsonProperty("phone_number_id") String phoneNumberId) {
    }

    public record MensajeMeta(String from, String id, String type, Texto text) {
    }

    public record Texto(String body) {
    }

    public record MensajeEntrante(String phoneNumberId, String telefono, String idMensaje, String texto) {
    }
}