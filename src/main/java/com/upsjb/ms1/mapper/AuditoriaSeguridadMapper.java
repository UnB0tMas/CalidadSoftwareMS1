package com.upsjb.ms1.mapper;

import com.upsjb.ms1.domain.entity.AuditoriaSeguridad;
import com.upsjb.ms1.dto.auditoria.response.AuditoriaSeguridadResponseDto;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaSeguridadMapper {

    public AuditoriaSeguridadResponseDto toResponse(AuditoriaSeguridad auditoria) {
        if (auditoria == null) {
            return null;
        }

        return new AuditoriaSeguridadResponseDto(
                auditoria.getId(),
                auditoria.getTipo(),
                auditoria.getUsuarioActor() == null ? null : auditoria.getUsuarioActor().getId(),
                auditoria.getUsernameActor(),
                auditoria.getUsuarioAfectado() == null ? null : auditoria.getUsuarioAfectado().getId(),
                auditoria.getUsernameAfectado(),
                auditoria.getIpAddress() == null ? null : auditoria.getIpAddress().getValue(),
                auditoria.getUserAgent() == null ? null : auditoria.getUserAgent().getValue(),
                auditoria.getRequestId(),
                auditoria.getHttpMethod(),
                auditoria.getPath(),
                auditoria.getResultado(),
                auditoria.getDescripcion(),
                auditoria.getEventAt()
        );
    }
}