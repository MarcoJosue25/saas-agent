package saasagent.saas_agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import saasagent.saas_agent.model.Mensaje;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    boolean existsByIdMensajeMeta(String idMensajeMeta);
}
