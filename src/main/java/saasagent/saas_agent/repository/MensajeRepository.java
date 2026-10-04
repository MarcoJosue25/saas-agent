package saasagent.saas_agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import saasagent.saas_agent.model.Mensaje;

import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    boolean existsByIdMensajeMeta(String idMensajeMeta);

    List<Mensaje> findTop10ByConversacion_IdOrderByIdDesc(Long conversacionId);
}
