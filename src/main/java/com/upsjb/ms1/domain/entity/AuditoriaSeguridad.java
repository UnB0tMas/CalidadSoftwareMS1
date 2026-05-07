package com.upsjb.ms1.domain.entity;

import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
import com.upsjb.ms1.domain.value.IpAddressValue;
import com.upsjb.ms1.domain.value.UserAgentValue;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "auditoria_seguridad",
        indexes = {
                @Index(name = "ix_auditoria_tipo", columnList = "tipo"),
                @Index(name = "ix_auditoria_actor", columnList = "id_usuario_actor"),
                @Index(name = "ix_auditoria_afectado", columnList = "id_usuario_afectado"),
                @Index(name = "ix_auditoria_fecha", columnList = "event_at"),
                @Index(name = "ix_auditoria_request", columnList = "request_id")
        }
)
public class AuditoriaSeguridad extends AuditableEntity {

    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria_seguridad")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 80)
    private TipoAuditoriaSeguridad tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_usuario_actor",
            foreignKey = @ForeignKey(name = "fk_auditoria_usuario_actor")
    )
    private Usuario usuarioActor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_usuario_afectado",
            foreignKey = @ForeignKey(name = "fk_auditoria_usuario_afectado")
    )
    private Usuario usuarioAfectado;

    @Column(name = "username_actor", length = 80)
    private String usernameActor;

    @Column(name = "username_afectado", length = 80)
    private String usernameAfectado;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "ip_address", length = 45)
    )
    private IpAddressValue ipAddress;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "user_agent", length = 512)
    )
    private UserAgentValue userAgent;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "http_method", length = 20)
    private String httpMethod;

    @Column(name = "path", length = 300)
    private String path;

    @Column(name = "resultado", nullable = false, length = 40)
    private String resultado;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Lob
    @Column(name = "metadata_json", columnDefinition = "NVARCHAR(MAX)")
    private String metadataJson;

    @Column(name = "event_at", nullable = false)
    private Instant eventAt;
}