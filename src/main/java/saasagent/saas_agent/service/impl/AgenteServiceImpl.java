package saasagent.saas_agent.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import saasagent.saas_agent.client.GeminiClient;
import saasagent.saas_agent.dto.ItemPedidoRequest;
import saasagent.saas_agent.dto.RespuestaGemini;
import saasagent.saas_agent.dto.ResultadoValidacion;
import saasagent.saas_agent.exception.GeminiException;
import saasagent.saas_agent.model.Conversacion;
import saasagent.saas_agent.model.Empresa;
import saasagent.saas_agent.model.Mensaje;
import saasagent.saas_agent.model.Pedido;
import saasagent.saas_agent.model.enums.Rol;
import saasagent.saas_agent.service.AgenteService;
import saasagent.saas_agent.service.PedidoService;
import saasagent.saas_agent.service.ProductoService;
import saasagent.saas_agent.service.ValidadorService;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
public class AgenteServiceImpl implements AgenteService {

    private static final String MENSAJE_RESPALDO = "Disculpa, dame unos minutos y te atiendo";

    private final ProductoService productoService;
    private final GeminiClient geminiClient;
    private final ValidadorService validadorService;
    private final PedidoService pedidoService;
    private final String plantillaPrompt;
    private final String herramientasJson;

    public AgenteServiceImpl(GeminiClient geminiClient, ProductoService productoService,
                             ValidadorService validadorService, PedidoService pedidoService) {
        this.geminiClient = geminiClient;
        this.productoService = productoService;
        this.validadorService = validadorService;
        this.pedidoService = pedidoService;
        this.plantillaPrompt = leerArchivo("prompts/sistema.txt");
        this.herramientasJson = leerArchivo("prompts/herramientas.json");
    }

    @Override
    public String responder(Empresa empresa, Conversacion conversacion, String mensajeCliente, List<Mensaje> historial) {
        String catalogo = productoService.catalogoPrompt(empresa.getId());
        String instrucciones = plantillaPrompt.replace("{empresa}", empresa.getNombre())
                .replace("{catalogo}", catalogo).replace("{historial}", formatearHistorial(historial));

        try {
            RespuestaGemini respuesta = geminiClient.consultar(instrucciones, mensajeCliente, herramientasJson);
            if (respuesta.esLlamadaAFuncion()) {
                return "crear_pedido".equals(respuesta.funcion())
                        ? crearPedido(empresa, conversacion, respuesta.argumentos()) : MENSAJE_RESPALDO;
            }
            return respuesta.texto();
        } catch (GeminiException e) {
            log.warn("Gemini falló, se envía el mensaje de respaldo: {}", e.getMessage());
            return MENSAJE_RESPALDO;
        }
    }

    // Gemini propone el pedido, pero quien lo valida y lo registra es nuestro código
    private String crearPedido(Empresa empresa, Conversacion conversacion, JsonNode argumentos) {
        List<ItemPedidoRequest> pedidos = new ArrayList<>();
        for (JsonNode item : argumentos.path("items")) {
            pedidos.add(new ItemPedidoRequest(item.path("producto").asString(""),
                    item.path("cantidad").asInt(0), item.path("talla").asString(null)));
        }

        ResultadoValidacion resultado = validadorService.validar(empresa.getId(), pedidos);
        if (!resultado.esValido()) {
            return "No pude registrar tu pedido:\n- " + String.join("\n- ", resultado.errores());
        }

        try {
            Pedido pedido = pedidoService.registrar(empresa, conversacion, resultado);
            return "Listo, tu pedido #" + pedido.getId() + " quedó registrado:\n"
                    + resultado.items().stream().map(this::describirItem).collect(Collectors.joining("\n"))
                    + "\nTotal: S/ " + resultado.total().setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (IllegalStateException e) {
            log.warn("No se pudo registrar el pedido: {}", e.getMessage());
            return "No pude registrar tu pedido porque el stock cambió. ¿Lo intentamos de nuevo?";
        }
    }

    private String describirItem(ResultadoValidacion.Item item) {
        String talla = item.talla() == null ? "" : " (talla " + item.talla() + ")";
        return "- " + item.cantidad() + " x " + item.producto().getNombre() + talla;
    }

    //Devuelve la lista de mensajes anteriores en un solo String
    private String formatearHistorial(List<Mensaje> historial) {
        if (historial.isEmpty()) {
            return "(Todavía no hay mensajes anteriores.)";
        }
        return historial.stream()
                .map(m -> (m.getRol() == Rol.CLIENTE ? "Cliente: " : "Asistente: ") + m.getContenido())
                .collect(Collectors.joining("\n"));
    }

    // Lee un archivo de resources. Si hay algun error el fallo es al arrancar y no cuando se envía algún mensaje
    private String leerArchivo(String ruta) {
        try {
            return new ClassPathResource(ruta).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + ruta, e);
        }
    }
}
