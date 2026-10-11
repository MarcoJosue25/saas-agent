package saasagent.saas_agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeRequest {
    @NotBlank(message = "El id de la empresa no puede estar vacío")
    private String idNumeroMeta;

    @NotBlank(message = "El telefono del cliente no puede estar vacío")
    private String telefonoCliente;

    @NotBlank(message = "El texto del mensaje no puede estar vacío")
    private String texto;
}
