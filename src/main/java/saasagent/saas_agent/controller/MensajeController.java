package saasagent.saas_agent.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import saasagent.saas_agent.dto.MensajeRequest;
import saasagent.saas_agent.dto.MensajeResponse;
import saasagent.saas_agent.service.ConversacionService;

@Slf4j
@RestController
@RequestMapping("/api/v1/mensajes")
@RequiredArgsConstructor
public class MensajeController {
    private final ConversacionService conversacionService;

    @PostMapping
    public MensajeResponse recibirMensaje(@RequestBody MensajeRequest request){
        log.info("Endpoint hit: POST /api/v1/mensajes");
        String respuesta = conversacionService.procesarMensaje(request);
        return new MensajeResponse(respuesta);
    }


}
