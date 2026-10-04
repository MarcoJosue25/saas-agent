package saasagent.saas_agent.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import saasagent.saas_agent.client.GeminiClient;
import saasagent.saas_agent.exception.GeminiException;
import saasagent.saas_agent.model.Empresa;
import saasagent.saas_agent.model.Mensaje;
import saasagent.saas_agent.model.enums.Rol;
import saasagent.saas_agent.service.AgenteService;
import saasagent.saas_agent.service.ProductoService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
public class AgenteServiceImpl implements AgenteService {

    private static final String MENSAJE_RESPALDO = "Disculpa, dame unos minutos y te atiendo";
    
    private final ProductoService productoService;
    private final GeminiClient geminiClient;
    private final String plantillaPrompt;

    public AgenteServiceImpl(GeminiClient geminiClient, ProductoService productoService) {
        this.geminiClient = geminiClient;
        this.productoService = productoService;
        this.plantillaPrompt = leerPlantilla();
    }

    @Override
    public String responder(Empresa empresa, String mensajeCliente, List<Mensaje> historial) {
        String catalogo = productoService.catalogoPrompt(empresa.getId());
        String instrucciones = plantillaPrompt.replace("{empresa}", empresa.getNombre())
                .replace("{catalogo}", catalogo).replace("{historial}", formatearHistorial(historial));

        try {
            return geminiClient.generarRespuesta(instrucciones, mensajeCliente);
        } catch (GeminiException e){
            log.warn("Gemini falló, se envía el mensaje de respaldo: {}",e.getMessage());
            return MENSAJE_RESPALDO;
        }
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

    //Convierte a String y guarda el archivo con instrucciones de IA en la variable plantillaPrompt
    // Si hay algun error el fallo es al arrancar y no cuando se envía algún mensaje
    private String leerPlantilla(){
        try{
            return new ClassPathResource("prompts/sistema.txt").getContentAsString(StandardCharsets.UTF_8);
        }catch (IOException e){
            throw new UncheckedIOException("No se pudo leer prompts/sistema.txt",e);
        }
    }

}
