package saasagent.saas_agent.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import saasagent.saas_agent.dto.MensajeRequest;
import saasagent.saas_agent.model.Conversacion;
import saasagent.saas_agent.model.Empresa;
import saasagent.saas_agent.model.Mensaje;
import saasagent.saas_agent.model.enums.Rol;
import saasagent.saas_agent.repository.ConversacionRepository;
import saasagent.saas_agent.repository.EmpresaRepository;
import saasagent.saas_agent.repository.MensajeRepository;
import saasagent.saas_agent.service.AgenteService;
import saasagent.saas_agent.util.FiltroIntencionRapida;
import saasagent.saas_agent.util.RespuestaSaludoProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConversacionServiceImplTest {

    private static final String ID_NUMERO = "100000000000001";
    private static final String TELEFONO = "51999999999";

    private MensajeRepository mensajeRepository;
    private ConversacionRepository conversacionRepository;
    private AgenteService agenteService;
    private ConversacionServiceImpl servicio;
    private Empresa empresa;

    //Deja listo el caso de un cliente nuevo
    @BeforeEach
    void preparar() {
        EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
        conversacionRepository = mock(ConversacionRepository.class);
        mensajeRepository = mock(MensajeRepository.class);
        agenteService = mock(AgenteService.class);
        servicio = new ConversacionServiceImpl(empresaRepository, conversacionRepository, mensajeRepository,
                new FiltroIntencionRapida(), new RespuestaSaludoProvider(), agenteService);

        empresa = Empresa.builder().id(1L).nombre("Moda Norte").idNumeroMeta(ID_NUMERO).build();
        when(empresaRepository.findByIdNumeroMetaAndActivoTrue(ID_NUMERO)).thenReturn(Optional.of(empresa));
        when(conversacionRepository.findByEmpresa_IdAndTelefonoCliente(1L, TELEFONO)).thenReturn(Optional.empty());
        when(conversacionRepository.save(any(Conversacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void unSaludoSeResponderConLaPlantillaSinLlamarAlAgente() {
        String respuesta = servicio.procesarMensaje(solicitud("hola"));

        assertThat(respuesta).isEqualTo(new RespuestaSaludoProvider().obtenerRespuesta());
        verifyNoInteractions(agenteService);

        // Confirma que se  guarden los dos mensajes: el del cliente y la respuesta
        ArgumentCaptor<Mensaje> mensajes = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeRepository, times(2)).save(mensajes.capture());
        assertThat(mensajes.getAllValues()).extracting(Mensaje::getRol)
                .containsExactly(Rol.CLIENTE, Rol.AGENTE);
    }

    @Test
    void unMensajeNormalLlamaAlAgenteYLaConversacionNuevaLlevaLaEmpresa() {
        when(agenteService.responder(empresa, "¿cuánto cuesta el polo negro?")).thenReturn("Cuesta S/ 35.00");

        String respuesta = servicio.procesarMensaje(solicitud("¿cuánto cuesta el polo negro?"));

        assertThat(respuesta).isEqualTo("Cuesta S/ 35.00");

        ArgumentCaptor<Conversacion> conversacion = ArgumentCaptor.forClass(Conversacion.class);
        verify(conversacionRepository).save(conversacion.capture());
        assertThat(conversacion.getValue().getEmpresa()).isSameAs(empresa);
        assertThat(conversacion.getValue().getTelefonoCliente()).isEqualTo(TELEFONO);
    }

    private static MensajeRequest solicitud(String texto) {
        return MensajeRequest.builder()
                .idNumeroMeta(ID_NUMERO)
                .telefonoCliente(TELEFONO)
                .texto(texto)
                .build();
    }
}
