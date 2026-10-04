package saasagent.saas_agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import saasagent.saas_agent.model.Conversacion;

import java.util.Optional;

public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    Optional<Conversacion> findByEmpresa_IdAndTelefonoCliente(Long empresaId, String telefonoCliente);
}
