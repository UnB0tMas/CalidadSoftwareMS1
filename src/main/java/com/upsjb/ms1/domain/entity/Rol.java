package com.upsjb.ms1.domain.entity;

import com.upsjb.ms1.domain.enums.EstadoRegistro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
        name = "rol",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rol_codigo", columnNames = "codigo")
        },
        indexes = {
                @Index(name = "ix_rol_estado", columnList = "estado"),
                @Index(name = "ix_rol_nombre", columnList = "nombre")
        }
)
public class Rol extends AuditableEntity {

    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long id;

    @ToString.Include
    @Column(name = "codigo", nullable = false, length = 40)
    private String codigo;

    @ToString.Include
    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "descripcion", length = 250)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoRegistro estado;

    @Column(name = "es_rol_sistema", nullable = false)
    private boolean rolSistema;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean estaActivo() {
        return EstadoRegistro.ACTIVO.equals(estado);
    }

    public boolean estaInactivo() {
        return EstadoRegistro.INACTIVO.equals(estado);
    }

    public boolean estaEliminado() {
        return EstadoRegistro.ELIMINADO.equals(estado);
    }

    public void activar() {
        this.estado = EstadoRegistro.ACTIVO;
        this.deletedAt = null;
    }

    public void inactivar() {
        this.estado = EstadoRegistro.INACTIVO;
    }

    public void eliminarLogicamente(Instant fechaEliminacion) {
        this.estado = EstadoRegistro.ELIMINADO;
        this.deletedAt = fechaEliminacion;
    }
}