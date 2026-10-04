package saasagent.saas_agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import saasagent.saas_agent.model.Producto;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByEmpresa_IdAndActivoTrueOrderByNombreAsc(Long empresaId);

}
