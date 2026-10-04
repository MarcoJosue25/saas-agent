package saasagent.saas_agent.service;

import saasagent.saas_agent.dto.ItemPedidoRequest;
import saasagent.saas_agent.dto.ResultadoValidacion;

import java.util.List;

public interface ValidadorService {

    ResultadoValidacion validar(Long empresaId, List<ItemPedidoRequest> pedidos);
}
