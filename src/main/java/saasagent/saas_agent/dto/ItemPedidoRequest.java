package saasagent.saas_agent.dto;

public record ItemPedidoRequest(
        String producto,
        int cantidad,
        String talla) {
}
