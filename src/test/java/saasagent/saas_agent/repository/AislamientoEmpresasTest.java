package saasagent.saas_agent.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import saasagent.saas_agent.model.Conversacion;
import saasagent.saas_agent.model.Empresa;
import saasagent.saas_agent.model.Producto;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Usa la MySQL real (no una base en memoria) y deshace los datos al terminar cada prueba
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AislamientoEmpresasTest {

    private static final String TELEFONO = "51999999999";

    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private ConversacionRepository conversacionRepository;

    private Empresa empresaA;
    private Empresa empresaB;

    @BeforeEach
    void preparar() {
        empresaA = empresaRepository.save(Empresa.builder().nombre("Tienda A").idNumeroMeta("900000000000001").build());
        empresaB = empresaRepository.save(Empresa.builder().nombre("Tienda B").idNumeroMeta("900000000000002").build());

        productoRepository.save(producto(empresaA, "Polo negro", "35.00"));
        productoRepository.save(producto(empresaB, "Polo negro", "29.90"));
        productoRepository.save(producto(empresaB, "Casaca de jean", "149.90"));
    }

    @Test
    void elMismoNombreDevuelveElProductoDeCadaEmpresa() {
        Producto deA = productoRepository
                .findByEmpresa_IdAndNombreIgnoreCaseAndActivoTrue(empresaA.getId(), "polo negro").orElseThrow();
        Producto deB = productoRepository
                .findByEmpresa_IdAndNombreIgnoreCaseAndActivoTrue(empresaB.getId(), "polo negro").orElseThrow();

        assertThat(deA.getPrecio()).isEqualByComparingTo("35.00");
        assertThat(deB.getPrecio()).isEqualByComparingTo("29.90");
    }

    @Test
    void elCatalogoDeUnaEmpresaNoIncluyeLosProductosDeOtra() {
        List<Producto> catalogoA = productoRepository.findByEmpresa_IdAndActivoTrueOrderByNombreAsc(empresaA.getId());

        assertThat(catalogoA).extracting(Producto::getNombre).containsExactly("Polo negro");
        // La casaca solo existe en la empresa B
        assertThat(productoRepository
                .findByEmpresa_IdAndNombreIgnoreCaseAndActivoTrue(empresaA.getId(), "Casaca de jean")).isEmpty();
    }

    @Test
    void laConversacionDeUnClienteNoSeVeDesdeOtraEmpresa() {
        conversacionRepository.save(Conversacion.builder().empresa(empresaA).telefonoCliente(TELEFONO).build());

        assertThat(conversacionRepository.findByEmpresa_IdAndTelefonoCliente(empresaA.getId(), TELEFONO)).isPresent();
        assertThat(conversacionRepository.findByEmpresa_IdAndTelefonoCliente(empresaB.getId(), TELEFONO)).isEmpty();
    }

    private static Producto producto(Empresa empresa, String nombre, String precio) {
        return Producto.builder().empresa(empresa).nombre(nombre).precio(new BigDecimal(precio)).stock(10).build();
    }
}