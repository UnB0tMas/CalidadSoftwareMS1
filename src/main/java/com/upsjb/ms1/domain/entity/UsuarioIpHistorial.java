package com.upsjb.ms1.domain.entity;

import com.upsjb.ms1.domain.value.IpAddressValue;
import com.upsjb.ms1.domain.value.UserAgentValue;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "usuario_ip_historial",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_usuario_ip_historial",
                        columnNames = {"id_usuario", "ip_address"}
                )
        },
        indexes = {
                @Index(name = "ix_ip_usuario", columnList = "id_usuario"),
                @Index(name = "ix_ip_sospechosa", columnList = "sospechosa"),
                @Index(name = "ix_ip_ultimo_uso", columnList = "ultimo_uso_at")
        }
)
public class UsuarioIpHistorial extends AuditableEntity {

    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario_ip_historial")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_usuario",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ip_usuario")
    )
    private Usuario usuario;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "ip_address", nullable = false, length = 45)
    )
    private IpAddressValue ipAddress;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "ultimo_user_agent", length = 512)
    )
    private UserAgentValue ultimoUserAgent;

    @Column(name = "primer_uso_at", nullable = false)
    private Instant primerUsoAt;

    @Column(name = "ultimo_uso_at", nullable = false)
    private Instant ultimoUsoAt;

    @Column(name = "cantidad_usos", nullable = false)
    private int cantidadUsos;

    @Column(name = "sospechosa", nullable = false)
    private boolean sospechosa;

    @Column(name = "bloqueada", nullable = false)
    private boolean bloqueada;

    @Column(name = "motivo_sospecha", length = 250)
    private String motivoSospecha;

    public void registrarUso(UserAgentValue userAgent, Instant fechaUso) {
        this.ultimoUserAgent = userAgent;
        this.ultimoUsoAt = fechaUso;
        this.cantidadUsos++;
    }

    public void marcarSospechosa(String motivo) {
        this.sospechosa = true;
        this.motivoSospecha = motivo;
    }

    public void limpiarSospecha() {
        this.sospechosa = false;
        this.motivoSospecha = null;
    }

    public void bloquear() {
        this.bloqueada = true;
    }

    public void desbloquear() {
        this.bloqueada = false;
    }
}