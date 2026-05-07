package com.upsjb.ms1.domain.entity;

import com.upsjb.ms1.domain.enums.EstadoVerificacionCodigo;
import com.upsjb.ms1.domain.enums.TipoToken;
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
        name = "password_reset_token",
        indexes = {
                @Index(name = "ix_reset_usuario_estado", columnList = "id_usuario, estado"),
                @Index(name = "ix_reset_token_hash", columnList = "token_hash"),
                @Index(name = "ix_reset_expira", columnList = "expires_at")
        }
)
public class PasswordResetToken extends AuditableEntity {

    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_password_reset_token")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_usuario",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reset_usuario")
    )
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_token", nullable = false, length = 50)
    private TipoToken tipoToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoVerificacionCodigo estado;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "max_intentos", nullable = false)
    private int maxIntentos;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

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

    public boolean estaPendiente() {
        return EstadoVerificacionCodigo.PENDIENTE.equals(estado);
    }

    public boolean estaUsado() {
        return EstadoVerificacionCodigo.VALIDADO.equals(estado);
    }

    public boolean estaVigente(Instant ahora) {
        return estaPendiente() && expiresAt != null && expiresAt.isAfter(ahora);
    }

    public boolean tieneIntentosDisponibles() {
        return intentosFallidos < maxIntentos;
    }

    public void registrarIntentoFallido() {
        this.intentosFallidos++;

        if (this.intentosFallidos >= this.maxIntentos) {
            this.estado = EstadoVerificacionCodigo.BLOQUEADO;
        }
    }

    public void marcarUsado(Instant fechaUso) {
        this.estado = EstadoVerificacionCodigo.VALIDADO;
        this.usedAt = fechaUso;
    }

    public void marcarExpirado() {
        this.estado = EstadoVerificacionCodigo.EXPIRADO;
    }

    public void revocar(Instant fechaRevocacion) {
        this.estado = EstadoVerificacionCodigo.REVOCADO;
        this.revokedAt = fechaRevocacion;
    }
}