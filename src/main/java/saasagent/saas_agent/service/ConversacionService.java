package saasagent.saas_agent.service;

import saasagent.saas_agent.dto.MensajeRequest;

public interface ConversacionService {

    String procesarMensaje(MensajeRequest request);
}
