package saasagent.saas_agent.service;

import saasagent.saas_agent.dto.ResultadoValidacion;
import saasagent.saas_agent.model.Conversacion;
import saasagent.saas_agent.model.Empresa;
import saasagent.saas_agent.model.Pedido;

public interface PedidoService {

    Pedido registrar(Empresa empresa, Conversacion conversacion, ResultadoValidacion resultado);

}
