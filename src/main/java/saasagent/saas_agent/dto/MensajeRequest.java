package saasagent.saas_agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeRequest {
    private String idNumeroMeta;
    private String telefonoCliente;
    private String texto;
}
