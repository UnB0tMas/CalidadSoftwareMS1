package com.upsjb.ms1.controller;

import com.upsjb.ms1.config.SwaggerSecurityConfig;
import com.upsjb.ms1.dto.auditoria.filter.AuditoriaSeguridadFilterDto;
import com.upsjb.ms1.dto.auditoria.filter.LoginAttemptFilterDto;
import com.upsjb.ms1.dto.auditoria.filter.UsuarioIpHistorialFilterDto;
import com.upsjb.ms1.dto.auditoria.response.AuditoriaSeguridadResponseDto;
import com.upsjb.ms1.dto.auditoria.response.LoginAttemptResponseDto;
import com.upsjb.ms1.dto.auditoria.response.UsuarioIpHistorialResponseDto;
import com.upsjb.ms1.dto.shared.ApiResponseDto;
import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.service.contract.AuditoriaSeguridadService;
import com.upsjb.ms1.service.contract.UsuarioIpHistorialService;
import com.upsjb.ms1.shared.response.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditoria")
@SecurityRequirement(name = SwaggerSecurityConfig.BEARER_AUTH_SCHEME)
@Tag(
        name = "Auditoría",
        description = "Operaciones de consulta de auditoría de seguridad, intentos de login e historial de IP."
)
public class AuditoriaController {

    private final AuditoriaSeguridadService auditoriaSeguridadService;
    private final UsuarioIpHistorialService usuarioIpHistorialService;
    private final ApiResponseFactory apiResponseFactory;

    public AuditoriaController(
            AuditoriaSeguridadService auditoriaSeguridadService,
            UsuarioIpHistorialService usuarioIpHistorialService,
            ApiResponseFactory apiResponseFactory
    ) {
        this.auditoriaSeguridadService = auditoriaSeguridadService;
        this.usuarioIpHistorialService = usuarioIpHistorialService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/seguridad")
    @Operation(
            summary = "Listar auditoría de seguridad",
            description = "Lista eventos de auditoría de seguridad con filtros y paginación."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auditoría de seguridad listada correctamente."),
            @ApiResponse(responseCode = "400", description = "Filtros o paginación inválidos."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para consultar auditoría.")
    })
    public ResponseEntity<ApiResponseDto<PageResponseDto<AuditoriaSeguridadResponseDto>>> findSecurityAudit(
            AuthenticatedUserContext actor,
            @ParameterObject @ModelAttribute AuditoriaSeguridadFilterDto filter,
            @Valid @ParameterObject @ModelAttribute PageRequestDto pageRequest
    ) {
        PageResponseDto<AuditoriaSeguridadResponseDto> response = auditoriaSeguridadService.findSecurityAudit(
                actor,
                filter,
                pageRequest
        );

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Auditoría de seguridad listada correctamente.",
                response
        ));
    }

    @GetMapping("/seguridad/{idAuditoria}")
    @Operation(
            summary = "Obtener registro de auditoría",
            description = "Obtiene un registro de auditoría de seguridad por ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro de auditoría encontrado correctamente."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para consultar este registro."),
            @ApiResponse(responseCode = "404", description = "Registro de auditoría no encontrado.")
    })
    public ResponseEntity<ApiResponseDto<AuditoriaSeguridadResponseDto>> findSecurityAuditById(
            AuthenticatedUserContext actor,
            @Parameter(description = "ID del registro de auditoría.", required = true)
            @PathVariable Long idAuditoria
    ) {
        AuditoriaSeguridadResponseDto response = auditoriaSeguridadService.findById(actor, idAuditoria);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Registro de auditoría encontrado correctamente.",
                response
        ));
    }

    @GetMapping("/login-attempts")
    @Operation(
            summary = "Listar intentos de login",
            description = "Lista intentos de login con filtros y paginación."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Intentos de login listados correctamente."),
            @ApiResponse(responseCode = "400", description = "Filtros o paginación inválidos."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para consultar intentos de login.")
    })
    public ResponseEntity<ApiResponseDto<PageResponseDto<LoginAttemptResponseDto>>> findLoginAttempts(
            AuthenticatedUserContext actor,
            @ParameterObject @ModelAttribute LoginAttemptFilterDto filter,
            @Valid @ParameterObject @ModelAttribute PageRequestDto pageRequest
    ) {
        PageResponseDto<LoginAttemptResponseDto> response = auditoriaSeguridadService.findLoginAttempts(
                actor,
                filter,
                pageRequest
        );

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Intentos de login listados correctamente.",
                response
        ));
    }

    @GetMapping("/ips")
    @Operation(
            summary = "Listar historial de IP",
            description = "Lista historial de IP de usuarios con filtros y paginación."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de IP listado correctamente."),
            @ApiResponse(responseCode = "400", description = "Filtros o paginación inválidos."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para consultar historial de IP.")
    })
    public ResponseEntity<ApiResponseDto<PageResponseDto<UsuarioIpHistorialResponseDto>>> findIpHistory(
            AuthenticatedUserContext actor,
            @ParameterObject @ModelAttribute UsuarioIpHistorialFilterDto filter,
            @Valid @ParameterObject @ModelAttribute PageRequestDto pageRequest
    ) {
        PageResponseDto<UsuarioIpHistorialResponseDto> response = auditoriaSeguridadService.findIpHistory(
                actor,
                filter,
                pageRequest
        );

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Historial de IP listado correctamente.",
                response
        ));
    }

    @GetMapping("/ips/mis-ips")
    @Operation(
            summary = "Listar mi historial de IP",
            description = "Lista el historial de IP del usuario autenticado. El service valida autorización contextual."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de IP listado correctamente."),
            @ApiResponse(responseCode = "400", description = "Paginación inválida."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para consultar este historial.")
    })
    public ResponseEntity<ApiResponseDto<PageResponseDto<UsuarioIpHistorialResponseDto>>> findMyIpHistory(
            AuthenticatedUserContext actor,
            @Valid @ParameterObject @ModelAttribute PageRequestDto pageRequest
    ) {
        PageResponseDto<UsuarioIpHistorialResponseDto> response = usuarioIpHistorialService.findByUsuario(
                actor,
                actor.idUsuario(),
                pageRequest
        );

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Historial de IP listado correctamente.",
                response
        ));
    }
}