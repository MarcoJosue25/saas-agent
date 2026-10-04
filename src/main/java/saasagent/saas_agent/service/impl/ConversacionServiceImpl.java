package saasagent.saas_agent.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import saasagent.saas_agent.dto.MensajeRequest;
import saasagent.saas_agent.model.Conversacion;
import saasagent.saas_agent.model.Empresa;
import saasagent.saas_agent.model.Mensaje;
import saasagent.saas_agent.model.enums.Rol;
import saasagent.saas_agent.repository.ConversacionRepository;
import saasagent.saas_agent.repository.EmpresaRepository;
import saasagent.saas_agent.repository.MensajeRepository;
import saasagent.saas_agent.service.AgenteService;
import saasagent.saas_agent.service.ConversacionService;
import saasagent.saas_agent.util.FiltroIntencionRapida;
import saasagent.saas_agent.util.RespuestaSaludoProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversacionServiceImpl implements ConversacionService {

    private final EmpresaRepository empresaRepository;
    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final FiltroIntencionRapida filtroIntencionRapida;
    private final RespuestaSaludoProvider respuestaSaludoProvider;
    private final AgenteService agenteService;


    @Override
    public String procesarMensaje(MensajeRequest request){
        Empresa empresa = empresaRepository.findByIdNumeroMetaAndActivoTrue(request.getIdNumeroMeta())
                .orElseThrow(() -> new IllegalArgumentException("No existe una empresa activa con ese número"));

        Conversacion conversacion = obtenerConversacion(empresa, request.getTelefonoCliente());
        List<Mensaje> historial = obtenerHistorial(conversacion);
        guardarMensaje(conversacion, Rol.CLIENTE, request.getTexto());

        String respuesta = filtroIntencionRapida.esSaludo(request.getTexto())
                ? respuestaSaludoProvider.obtenerRespuesta() : agenteService.responder(empresa, request.getTexto(), historial);
        guardarMensaje(conversacion, Rol.AGENTE, respuesta);
        return respuesta;
    }


    //Buscamos una conversacion anterior con el cliente, si no existe se crea una nueva
    private Conversacion obtenerConversacion(Empresa empresa, String telefonoCliente){
        Conversacion conversacion = conversacionRepository.findByEmpresa_IdAndTelefonoCliente(empresa.getId(),
                telefonoCliente).orElseGet(() -> Conversacion.builder().empresa(empresa).telefonoCliente(telefonoCliente).build());
        if(conversacion.getId() != null) {
            conversacion.registrarMensaje();
        }
        // Se guarda antes de crear el mensaje, que necesita el id de la conversación
        return conversacionRepository.save(conversacion);
    }

    private void guardarMensaje(Conversacion conversacion, Rol rol, String contenido){
        mensajeRepository.save(Mensaje.builder()
                .conversacion(conversacion).rol(rol).contenido(contenido).build());
    }

    private List<Mensaje> obtenerHistorial(Conversacion conversacion) {
        List<Mensaje> historial = new ArrayList<>(
                mensajeRepository.findTop10ByConversacion_IdOrderByIdDesc(conversacion.getId()));
        Collections.reverse(historial); // llegan del más nuevo al más antiguo
        return historial;
    }
}
