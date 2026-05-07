package com.upsjb.ms1.domain.entity;

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
        name = "login_attempt",
        indexes = {
                @Index(name = "ix_login_attempt_usuario", columnList = "id_usuario"),
                @Index(name = "ix_login_attempt_username", columnList = "username_or_email"),
                @Index(name = "ix_login_attempt_ip_fecha", columnList = "ip_address, attempted_at"),
                @Index(name = "ix_login_attempt_exitoso", columnList = "exitoso")
        }
)
public class LoginAttempt extends AuditableEntity {

    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_login_attempt")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_usuario",
            foreignKey = @ForeignKey(name = "fk_login_attempt_usuario")
    )
    private Usuario usuario;

    @ToString.Include
    @Column(name = "username_or_email", nullable = false, length = 180)
    private String usernameOrEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_login", nullable = false, length = 40)
    private TipoLogin tipoLogin;

    @Column(name = "exitoso", nullable = false)
    private boolean exitoso;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Column(name = "failure_reason", length = 250)
    private String failureReason;

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

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Column(name = "request_id", length = 80)
    private String requestId;
}