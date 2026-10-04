package saasagent.saas_agent.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroIntencionRapidaTest {

    private final FiltroIntencionRapida filtro = new FiltroIntencionRapida();

    @ParameterizedTest
    @ValueSource(strings = {"hola", "buenos dias", "buenas tardes", "buenas noches"})
    void detectaLosSaludosBase(String saludo) {
        assertThat(filtro.esSaludo(saludo)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"¡HOLA!", "  Buenos DÍAS ", "buenas tardes!!!", "¿buenas   noches?"})
    void ignoraMayusculasTildesSignosYEspaciosDeMas(String mensaje) {
        assertThat(filtro.esSaludo(mensaje)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"hola cuanto cuesta el polo negro", "Hola, ¿tienen polos?", "buenas tardes quiero un pedido"})
    void unSaludoSeguidoDeUnaPreguntaNoSeTrataComoSaludo(String mensaje) {
        assertThat(filtro.esSaludo(mensaje)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"quiero un polo negro", "holaa", "hola hola"})
    void unMensajeQueNoEsUnSaludoBaseNoSeDetecta(String mensaje) {
        assertThat(filtro.esSaludo(mensaje)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "???"})
    void unMensajeNuloOSinTextoNoEsSaludo(String mensaje) {
        assertThat(filtro.esSaludo(mensaje)).isFalse();
    }
}
