package com.upsjb.ms1.domain.entity;

import com.upsjb.ms1.domain.enums.EstadoSesion;
import com.upsjb.ms1.domain.enums.MotivoRevocacionSesion;
import com.upsjb.ms1.domain.enums.TipoLogin;
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
        name = "usuario_sesion",
        indexes = {
                @Index(name = "ix_sesion_usuario_estado", columnList = "id_usuario, estado"),
                @Index(name = "ix_sesion_refresh_hash", columnList = "refresh_token_hash"),
                @Index(name = "ix_sesion_expira", columnList = "expires_at"),
                @Index(name = "ix_sesion_ip", columnList = "ip_address")
        }
)
public class UsuarioSesion extends AuditableEntity {

    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario_sesion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_usuario",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sesion_usuario")
    )
    private Usuario usuario;

    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    private String refreshTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_login", nullable = false, length = 40)
    private TipoLogin tipoLogin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoSesion estado;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "ip_address", nullable = false, length = 45)
    )
    private IpAddressValue ipAddress;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "user_agent", nullable = false, length = 512)
    )
    private UserAgentValue userAgent;

    @Column(name = "device_fingerprint", length = 160)
    private String deviceFingerprint;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_revocacion", length = 60)
    private MotivoRevocacionSesion motivoRevocacion;

    @Column(name = "revocation_reason_detail", length = 250)
    private String revocationReasonDetail;

    public boolean estaActiva() {
        return EstadoSesion.ACTIVA.equals(estado);
    }

    public boolean estaRevocada() {
        return EstadoSesion.REVOCADA.equals(estado);
    }

    public boolean estaExpirada() {
        return EstadoSesion.EXPIRADA.equals(estado);
    }

    public boolean estaCerrada() {
        return EstadoSesion.CERRADA.equals(estado);
    }

    public boolean estaVigente(Instant ahora) {
        return estaActiva() && expiresAt != null && expiresAt.isAfter(ahora);
    }

    public void registrarUso(Instant fechaUso) {
        this.lastUsedAt = fechaUso;
    }

    public void rotarRefreshToken(String nuevoRefreshTokenHash, Instant fechaUso) {
        this.refreshTokenHash = nuevoRefreshTokenHash;
        this.lastUsedAt = fechaUso;
    }

    public void cerrar(Instant fechaCierre) {
        this.estado = EstadoSesion.CERRADA;
        this.revokedAt = fechaCierre;
        this.motivoRevocacion = MotivoRevocacionSesion.LOGOUT;
    }

    public void revocar(
            MotivoRevocacionSesion motivoRevocacion,
            String detalle,
            Instant fechaRevocacion
    ) {
        this.estado = EstadoSesion.REVOCADA;
        this.motivoRevocacion = motivoRevocacion;
        this.revocationReasonDetail = detalle;
        this.revokedAt = fechaRevocacion;
    }

    public void marcarExpirada(Instant fechaExpiracion) {
        this.estado = EstadoSesion.EXPIRADA;
        this.revokedAt = fechaExpiracion;
        this.motivoRevocacion = MotivoRevocacionSesion.EXPIRED;
    }
}