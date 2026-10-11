package saasagent.saas_agent.service;

import saasagent.saas_agent.model.Conversacion;
import saasagent.saas_agent.model.Empresa;
import saasagent.saas_agent.model.Mensaje;

import java.util.List;

public interface AgenteService {

    String responder(Empresa empresa, Conversacion conversacion, String mensajeCliente, List<Mensaje> historial);
}
