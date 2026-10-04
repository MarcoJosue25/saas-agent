package saasagent.saas_agent.service;

import saasagent.saas_agent.model.Empresa;

public interface AgenteService {

    String responder(Empresa empresa, String mensajeCliente);
}
