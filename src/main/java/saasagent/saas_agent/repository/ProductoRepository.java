package saasagent.saas_agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import saasagent.saas_agent.model.Producto;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByEmpresa_IdAndActivoTrueOrderByNombreAsc(Long empresaId);

    Optional<Producto> findByEmpresa_IdAndNombreIgnoreCaseAndActivoTrue(Long empresaId, String nombre);

    @Modifying
    @Query("update Producto p set p.stock = p.stock - :cantidad where p.id = :id and p.stock >= :cantidad")
    int descontarStock(@Param("id") Long id, @Param("cantidad") int cantidad);

}
