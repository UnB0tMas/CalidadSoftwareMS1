package com.upsjb.ms1.domain.entity;

import com.upsjb.ms1.domain.enums.EstadoRegistro;
import com.upsjb.ms1.domain.value.EmailValue;
import com.upsjb.ms1.domain.value.UsernameValue;
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
        name = "usuario",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_usuario_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_usuario_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "ix_usuario_estado", columnList = "estado"),
                @Index(name = "ix_usuario_rol", columnList = "id_rol"),
                @Index(name = "ix_usuario_email_verificado", columnList = "email_verificado"),
                @Index(name = "ix_usuario_bloqueado_hasta", columnList = "bloqueado_hasta")
        }
)
public class Usuario extends AuditableEntity {

    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_rol",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_usuario_rol")
    )
    private Rol rol;

    @Embedded
    @ToString.Include
    @AttributeOverride(
            name = "value",
            column = @Column(name = "username", nullable = false, length = 60)
    )
    private UsernameValue username;

    @Embedded
    @ToString.Include
    @AttributeOverride(
            name = "value",
            column = @Column(name = "email", nullable = false, length = 180)
    )
    private EmailValue email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nombres", nullable = false, length = 120)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 120)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoRegistro estado;

    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado;

    @Column(name = "requiere_cambio_password", nullable = false)
    private boolean requiereCambioPassword;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "ultimo_login_at")
    private Instant ultimoLoginAt;

    @Column(name = "ultimo_cambio_password_at")
    private Instant ultimoCambioPasswordAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean estaActivo() {
        return EstadoRegistro.ACTIVO.equals(estado);
    }

    public boolean estaInactivo() {
        return EstadoRegistro.INACTIVO.equals(estado);
    }

    public boolean estaBloqueado() {
        return EstadoRegistro.BLOQUEADO.equals(estado);
    }

    public boolean estaEliminado() {
        return EstadoRegistro.ELIMINADO.equals(estado);
    }

    public boolean tieneBloqueoTemporalActivo(Instant ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    public void marcarEmailVerificado() {
        this.emailVerificado = true;
    }

    public void registrarLoginExitoso(Instant fechaLogin) {
        this.ultimoLoginAt = fechaLogin;
    }

    public void cambiarPasswordHash(String nuevoPasswordHash, Instant fechaCambio) {
        this.passwordHash = nuevoPasswordHash;
        this.ultimoCambioPasswordAt = fechaCambio;
        this.requiereCambioPassword = false;
    }

    public void exigirCambioPassword() {
        this.requiereCambioPassword = true;
    }

    public void activar() {
        this.estado = EstadoRegistro.ACTIVO;
        this.bloqueadoHasta = null;
        this.deletedAt = null;
    }

    public void inactivar() {
        this.estado = EstadoRegistro.INACTIVO;
    }

    public void bloquearTemporalmente(Instant bloqueadoHasta) {
        this.estado = EstadoRegistro.BLOQUEADO;
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public void desbloquear() {
        this.estado = EstadoRegistro.ACTIVO;
        this.bloqueadoHasta = null;
    }

    public void eliminarLogicamente(Instant fechaEliminacion) {
        this.estado = EstadoRegistro.ELIMINADO;
        this.deletedAt = fechaEliminacion;
    }
}