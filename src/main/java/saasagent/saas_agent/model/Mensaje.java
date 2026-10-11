package saasagent.saas_agent.model;

import jakarta.persistence.*;
import lombok.*;
import saasagent.saas_agent.model.enums.Rol;

import java.time.LocalDateTime;

@Builder
@Getter
@Entity
@Table(name = "mensajes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private Conversacion conversacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false, columnDefinition = "Text")
    private String contenido;

    @Column(unique = true)
    private String idMensajeMeta;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void alCrear(){
        this.fechaCreacion = LocalDateTime.now();
    }
}
