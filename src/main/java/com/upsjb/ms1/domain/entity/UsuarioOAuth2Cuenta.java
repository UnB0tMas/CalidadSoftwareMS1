package com.upsjb.ms1.domain.entity;

import com.upsjb.ms1.domain.enums.EstadoRegistro;
import com.upsjb.ms1.domain.enums.ProveedorOAuth2;
import com.upsjb.ms1.domain.value.EmailValue;
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
        name = "usuario_oauth2_cuenta",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_oauth2_provider_subject",
                        columnNames = {"proveedor", "provider_user_id"}
                ),
                @UniqueConstraint(
                        name = "uk_oauth2_usuario_proveedor",
                        columnNames = {"id_usuario", "proveedor"}
                )
        },
        indexes = {
                @Index(name = "ix_oauth2_usuario", columnList = "id_usuario"),
                @Index(name = "ix_oauth2_email", columnList = "email"),
                @Index(name = "ix_oauth2_estado", columnList = "estado")
        }
)
public class UsuarioOAuth2Cuenta extends AuditableEntity {

    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario_oauth2_cuenta")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_usuario",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_oauth2_usuario")
    )
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "proveedor", nullable = false, length = 40)
    private ProveedorOAuth2 proveedor;

    @ToString.Include
    @Column(name = "provider_user_id", nullable = false, length = 180)
    private String providerUserId;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "email", length = 180)
    )
    private EmailValue email;

    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado;

    @Column(name = "display_name", length = 160)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoRegistro estado;

    @Column(name = "ultimo_login_at")
    private Instant ultimoLoginAt;

    public boolean estaActiva() {
        return EstadoRegistro.ACTIVO.equals(estado);
    }

    public void registrarLogin(Instant fechaLogin) {
        this.ultimoLoginAt = fechaLogin;
    }

    public void activar() {
        this.estado = EstadoRegistro.ACTIVO;
    }

    public void inactivar() {
        this.estado = EstadoRegistro.INACTIVO;
    }
}