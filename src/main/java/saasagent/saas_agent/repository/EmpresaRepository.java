package saasagent.saas_agent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import saasagent.saas_agent.model.Empresa;

import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findByIdNumeroMeta(String idNumeroMeta);

    Optional<Empresa> findByIdNumeroMetaAndActivoTrue(String idNumeroMeta);

}
