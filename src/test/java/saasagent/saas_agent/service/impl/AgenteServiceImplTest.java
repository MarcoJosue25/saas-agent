package saasagent.saas_agent.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import saasagent.saas_agent.client.GeminiClient;
import saasagent.saas_agent.dto.RespuestaGemini;
import saasagent.saas_agent.dto.ResultadoValidacion;
import saasagent.saas_agent.exception.GeminiException;
import saasagent.saas_agent.model.Conversacion;
import saasagent.saas_agent.model.Empresa;
import saasagent.saas_agent.model.Mensaje;
import saasagent.saas_agent.model.Pedido;
import saasagent.saas_agent.model.Producto;
import saasagent.saas_agent.model.enums.Rol;
import saasagent.saas_agent.service.PedidoService;
import saasagent.saas_agent.service.ProductoService;
import saasagent.saas_agent.service.ValidadorService;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgenteServiceImplTest {

    private GeminiClient geminiClient;
    private ProductoService productoService;
    private ValidadorService validadorService;
    private PedidoService pedidoService;
    private AgenteServiceImpl agente;
    private Empresa empresa;
    private Conversacion conversacion;

    @BeforeEach
    void preparar() {
        geminiClient = mock(GeminiClient.class);
        productoService = mock(ProductoService.class);
        validadorService = mock(ValidadorService.class);
        pedidoService = mock(PedidoService.class);
        agente = new AgenteServiceImpl(geminiClient, productoService, validadorService, pedidoService);
        empresa = Empresa.builder().id(1L).nombre("Moda Norte").idNumeroMeta("100000000000001").build();
        conversacion = Conversacion.builder().empresa(empresa).telefonoCliente("51999999999").build();
    }

    //Comprueba que el catálogo llegue bien a Gemini
    @Test
    void lasInstruccionesLlevanElNombreDeLaEmpresaYElCatalogo() {
        String catalogo = "- Polo básico negro: Precio: S/ 35.00. Stock: 40 unidades.";
        when(productoService.catalogoPrompt(1L)).thenReturn(catalogo);
        when(geminiClient.consultar(anyString(), eq("¿cuánto cuesta el polo negro?"), anyString()))
                .thenReturn(new RespuestaGemini("Cuesta S/ 35.00", null, null));

        String respuesta = agente.responder(empresa, conversacion, "¿cuánto cuesta el polo negro?", List.of());

        assertThat(respuesta).isEqualTo("Cuesta S/ 35.00");

        ArgumentCaptor<String> instrucciones = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).consultar(instrucciones.capture(), eq("¿cuánto cuesta el polo negro?"), anyString());
        assertThat(instrucciones.getValue())
                .contains("Moda Norte")
                .contains(catalogo)
                .doesNotContain("{empresa}")
                .doesNotContain("{catalogo}")
                .doesNotContain("{historial}");
    }

    //Comprueba que la conversación anterior llegue a Gemini, del mensaje más antiguo al más reciente
    @Test
    void lasInstruccionesLlevanElHistorialDeLaConversacion() {
        when(productoService.catalogoPrompt(1L)).thenReturn("catálogo de prueba");
        when(geminiClient.consultar(anyString(), anyString(), anyString()))
                .thenReturn(new RespuestaGemini("De la S a la XL", null, null));
        List<Mensaje> historial = List.of(
                Mensaje.builder().rol(Rol.CLIENTE).contenido("cuanto cuesta el polo negro").build(),
                Mensaje.builder().rol(Rol.AGENTE).contenido("Cuesta S/ 35.00").build());

        agente.responder(empresa, conversacion, "y en que tallas esta?", historial);

        ArgumentCaptor<String> instrucciones = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).consultar(instrucciones.capture(), eq("y en que tallas esta?"), anyString());
        assertThat(instrucciones.getValue())
                .contains("Cliente: cuanto cuesta el polo negro\nAsistente: Cuesta S/ 35.00");
    }

    //Creamos una falsa respuesta para esta prueba, y se espera el mensaje de respaldo
    @Test
    void siGeminiFallaSeRespondeConElMensajeDeRespaldo() {
        when(productoService.catalogoPrompt(1L)).thenReturn("catálogo de prueba");
        when(geminiClient.consultar(anyString(), anyString(), anyString())).thenThrow(new GeminiException("falló"));

        String respuesta = agente.responder(empresa, conversacion, "hola", List.of());

        assertThat(respuesta).isNotBlank();
    }

    // El guardrail: si Gemini propone un producto que no existe, el pedido no se registra
    @Test
    void unPedidoConUnProductoInventadoSeRechazaSinRegistrarlo() {
        when(productoService.catalogoPrompt(1L)).thenReturn("catálogo de prueba");
        when(geminiClient.consultar(anyString(), anyString(), anyString())).thenReturn(llamadaCrearPedido("Camisa roja", 1));
        when(validadorService.validar(eq(1L), anyList())).thenReturn(new ResultadoValidacion(
                List.of("No encontré el producto \"Camisa roja\" en el catálogo."), List.of(), BigDecimal.ZERO));

        String respuesta = agente.responder(empresa, conversacion, "quiero una camisa roja", List.of());

        assertThat(respuesta).startsWith("No pude registrar tu pedido").contains("Camisa roja");
        verifyNoInteractions(pedidoService);
    }

    @Test
    void unPedidoValidoSeRegistraYSeConfirmaConElTotal() {
        Producto polo = Producto.builder().nombre("Polo básico negro").precio(new BigDecimal("35.00")).stock(40).build();
        ResultadoValidacion valido = new ResultadoValidacion(List.of(),
                List.of(new ResultadoValidacion.Item(polo, 2, "M")), new BigDecimal("70.00"));
        when(productoService.catalogoPrompt(1L)).thenReturn("catálogo de prueba");
        when(geminiClient.consultar(anyString(), anyString(), anyString())).thenReturn(llamadaCrearPedido("Polo básico negro", 2));
        when(validadorService.validar(eq(1L), anyList())).thenReturn(valido);
        when(pedidoService.registrar(empresa, conversacion, valido)).thenReturn(Pedido.builder().id(7L).build());

        String respuesta = agente.responder(empresa, conversacion, "si, confirmo", List.of());

        assertThat(respuesta).contains("#7").contains("2 x Polo básico negro (talla M)").contains("Total: S/ 70.00");
    }

    // --- Ayudas ---------------------------------------------------------------------------

    private static RespuestaGemini llamadaCrearPedido(String producto, int cantidad) {
        String args = "{\"items\": [{\"producto\": \"" + producto + "\", \"cantidad\": " + cantidad + "}]}";
        return new RespuestaGemini(null, "crear_pedido", JsonMapper.builder().build().readTree(args));
    }
}
