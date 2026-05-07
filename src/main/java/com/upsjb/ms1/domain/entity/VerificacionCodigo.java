package com.upsjb.ms1.domain.entity;

import com.upsjb.ms1.domain.enums.EstadoVerificacionCodigo;
import com.upsjb.ms1.domain.enums.TipoCodigoVerificacion;
import com.upsjb.ms1.domain.value.EmailValue;
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
        name = "verificacion_codigo",
        indexes = {
                @Index(name = "ix_codigo_usuario_tipo", columnList = "id_usuario, tipo_codigo"),
                @Index(name = "ix_codigo_email_tipo", columnList = "email, tipo_codigo"),
                @Index(name = "ix_codigo_estado_expira", columnList = "estado, expires_at"),
                @Index(name = "ix_codigo_hash", columnList = "codigo_hash")
        }
)
public class VerificacionCodigo extends AuditableEntity {

    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_verificacion_codigo")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_usuario",
            foreignKey = @ForeignKey(name = "fk_codigo_usuario")
    )
    private Usuario usuario;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "email", nullable = false, length = 180)
    )
    private EmailValue email;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_codigo", nullable = false, length = 50)
    private TipoCodigoVerificacion tipoCodigo;

    @Column(name = "codigo_hash", nullable = false, length = 255)
    private String codigoHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoVerificacionCodigo estado;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "max_intentos", nullable = false)
    private int maxIntentos;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

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

    public boolean estaValidado() {
        return EstadoVerificacionCodigo.VALIDADO.equals(estado);
    }

    public boolean estaExpirado(Instant ahora) {
        return EstadoVerificacionCodigo.EXPIRADO.equals(estado)
                || (expiresAt != null && !expiresAt.isAfter(ahora));
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

    public void marcarValidado(Instant fechaValidacion) {
        this.estado = EstadoVerificacionCodigo.VALIDADO;
        this.validatedAt = fechaValidacion;
    }

    public void marcarExpirado() {
        this.estado = EstadoVerificacionCodigo.EXPIRADO;
    }

    public void revocar(Instant fechaRevocacion) {
        this.estado = EstadoVerificacionCodigo.REVOCADO;
        this.revokedAt = fechaRevocacion;
    }
}