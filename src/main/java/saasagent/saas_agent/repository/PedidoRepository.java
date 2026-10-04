package saasagent.saas_agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import saasagent.saas_agent.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
