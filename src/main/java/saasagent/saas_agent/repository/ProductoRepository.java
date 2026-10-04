package saasagent.saas_agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import saasagent.saas_agent.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
