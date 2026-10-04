package saasagent.saas_agent.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import saasagent.saas_agent.client.GeminiClient;
import saasagent.saas_agent.exception.GeminiException;
import saasagent.saas_agent.model.Empresa;
import saasagent.saas_agent.service.ProductoService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgenteServiceImplTest {

    private GeminiClient geminiClient;
    private ProductoService productoService;
    private AgenteServiceImpl agente;
    private Empresa empresa;

    @BeforeEach
    void preparar() {
        geminiClient = mock(GeminiClient.class);
        productoService = mock(ProductoService.class);
        agente = new AgenteServiceImpl(geminiClient, productoService);
        empresa = Empresa.builder().id(1L).nombre("Moda Norte").idNumeroMeta("100000000000001").build();
    }

    //Comprueba que el catálogo llegue bien a Gemini
    @Test
    void lasInstruccionesLlevanElNombreDeLaEmpresaYElCatalogo() {
        String catalogo = "- Polo básico negro: Precio: S/ 35.00. Stock: 40 unidades.";
        when(productoService.catalogoPrompt(1L)).thenReturn(catalogo);
        when(geminiClient.generarRespuesta(anyString(), eq("¿cuánto cuesta el polo negro?")))
                .thenReturn("Cuesta S/ 35.00");

        String respuesta = agente.responder(empresa, "¿cuánto cuesta el polo negro?");

        assertThat(respuesta).isEqualTo("Cuesta S/ 35.00");

        ArgumentCaptor<String> instrucciones = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generarRespuesta(instrucciones.capture(), eq("¿cuánto cuesta el polo negro?"));
        assertThat(instrucciones.getValue())
                .contains("Moda Norte")
                .contains(catalogo)
                .doesNotContain("{empresa}")
                .doesNotContain("{catalogo}");
    }

    //Creamos una falsa respuesta para esta prueba, y se espera el mensaje de respaldo
    @Test
    void siGeminiFallaSeRespondeConElMensajeDeRespaldo() {
        when(productoService.catalogoPrompt(1L)).thenReturn("catálogo de prueba");
        when(geminiClient.generarRespuesta(anyString(), anyString())).thenThrow(new GeminiException("falló"));

        String respuesta = agente.responder(empresa, "hola");

        assertThat(respuesta).isNotBlank();
    }
}
