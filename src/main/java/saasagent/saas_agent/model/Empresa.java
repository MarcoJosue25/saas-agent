package saasagent.saas_agent.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Entity
@Getter
@Table(name = "empresa")
@NoArgsConstructor(access = AccessLevel.PROTECTED) //Evita que se use por error
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Nadie de afuera puede crear una nueva empresa
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nombre;

    @Column(name = "id_numero_meta", nullable = false, unique = true)
    private String idNumeroMeta;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @Column(updatable = false, nullable = false)
    private LocalDateTime fechaCreacion;

    //Se ejecuta al crear una empresa
    @PrePersist
    void alCrear(){
        this.fechaCreacion = LocalDateTime.now();
    }
}
