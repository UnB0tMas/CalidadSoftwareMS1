package com.upsjb.ms1.shared.audit;

import com.upsjb.ms1.domain.entity.AuditoriaSeguridad;
import com.upsjb.ms1.domain.entity.Usuario;
import com.upsjb.ms1.domain.enums.TipoAuditoriaSeguridad;
import com.upsjb.ms1.domain.value.IpAddressValue;
import com.upsjb.ms1.domain.value.UserAgentValue;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class AuditEventFactory {

    private static final String RESULTADO_OK = "OK";
    private static final String RESULTADO_ERROR = "ERROR";
    private static final String RESULTADO_WARNING = "WARNING";

    private final Clock clock;

    public AuditEventFactory(Clock clock) {
        this.clock = clock;
    }

    public AuditoriaSeguridad success(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion,
            String metadataJson
    ) {
        return build(tipo, actor, afectado, RESULTADO_OK, descripcion, metadataJson);
    }

    public AuditoriaSeguridad failure(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion,
            String metadataJson
    ) {
        return build(tipo, actor, afectado, RESULTADO_ERROR, descripcion, metadataJson);
    }

    public AuditoriaSeguridad warning(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String descripcion,
            String metadataJson
    ) {
        return build(tipo, actor, afectado, RESULTADO_WARNING, descripcion, metadataJson);
    }

    private AuditoriaSeguridad build(
            TipoAuditoriaSeguridad tipo,
            Usuario actor,
            Usuario afectado,
            String resultado,
            String descripcion,
            String metadataJson
    ) {
        AuditContext context = AuditContextHolder.get();
        Instant now = Instant.now(clock);

        return AuditoriaSeguridad.builder()
                .tipo(tipo)
                .usuarioActor(actor)
                .usuarioAfectado(afectado)
                .usernameActor(actor != null && actor.getUsername() != null ? actor.getUsername().getValue() : context.username())
                .usernameAfectado(afectado != null && afectado.getUsername() != null ? afectado.getUsername().getValue() : null)
                .ipAddress(toIpAddress(context.ipAddress()))
                .userAgent(UserAgentValue.of(context.userAgent()))
                .requestId(context.requestId())
                .httpMethod(context.httpMethod())
                .path(context.path())
                .resultado(resultado)
                .descripcion(descripcion)
                .metadataJson(metadataJson)
                .eventAt(now)
                .build();
    }

    private IpAddressValue toIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }
        return IpAddressValue.of(ipAddress);
    }
}