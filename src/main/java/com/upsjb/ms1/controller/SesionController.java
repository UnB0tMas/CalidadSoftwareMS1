package com.upsjb.ms1.controller;

import com.upsjb.ms1.config.SwaggerSecurityConfig;
import com.upsjb.ms1.domain.enums.EstadoSesion;
import com.upsjb.ms1.dto.auth.response.SessionResponseDto;
import com.upsjb.ms1.dto.shared.ApiResponseDto;
import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.service.contract.SesionService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sesiones")
@SecurityRequirement(name = SwaggerSecurityConfig.BEARER_AUTH_SCHEME)
@Tag(
        name = "Sesiones",
        description = "Operaciones de consulta y revocación de sesiones."
)
public class SesionController {

    private final SesionService sesionService;
    private final ApiResponseFactory apiResponseFactory;

    public SesionController(
            SesionService sesionService,
            ApiResponseFactory apiResponseFactory
    ) {
        this.sesionService = sesionService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/mis-sesiones")
    @Operation(
            summary = "Listar mis sesiones",
            description = "Lista las sesiones del usuario autenticado con filtro opcional por estado y paginación."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesiones listadas correctamente."),
            @ApiResponse(responseCode = "400", description = "Filtros o paginación inválidos."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para consultar estas sesiones.")
    })
    public ResponseEntity<ApiResponseDto<PageResponseDto<SessionResponseDto>>> findOwnSessions(
            AuthenticatedUserContext actor,
            @Parameter(description = "Estado de sesión opcional.")
            @RequestParam(required = false) EstadoSesion estado,
            @Valid @ParameterObject @ModelAttribute PageRequestDto pageRequest
    ) {
        PageResponseDto<SessionResponseDto> response = sesionService.findOwnSessions(actor, estado, pageRequest);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Sesiones listadas correctamente.",
                response
        ));
    }

    @GetMapping("/usuario/{idUsuario}")
    @Operation(
            summary = "Listar sesiones de un usuario",
            description = "Lista sesiones de un usuario específico. La autorización contextual se valida en SesionService/SesionPolicy."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesiones listadas correctamente."),
            @ApiResponse(responseCode = "400", description = "Filtros o paginación inválidos."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para consultar sesiones de este usuario."),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    public ResponseEntity<ApiResponseDto<PageResponseDto<SessionResponseDto>>> findUserSessions(
            AuthenticatedUserContext actor,
            @Parameter(description = "ID del usuario.", required = true)
            @PathVariable Long idUsuario,
            @Parameter(description = "Estado de sesión opcional.")
            @RequestParam(required = false) EstadoSesion estado,
            @Valid @ParameterObject @ModelAttribute PageRequestDto pageRequest
    ) {
        PageResponseDto<SessionResponseDto> response = sesionService.findUserSessions(
                actor,
                idUsuario,
                estado,
                pageRequest
        );

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Sesiones listadas correctamente.",
                response
        ));
    }

    @GetMapping("/{idSesion}")
    @Operation(
            summary = "Obtener sesión por ID",
            description = "Obtiene una sesión por ID. El service valida si el actor puede verla."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión encontrada correctamente."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para consultar esta sesión."),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada.")
    })
    public ResponseEntity<ApiResponseDto<SessionResponseDto>> findById(
            AuthenticatedUserContext actor,
            @Parameter(description = "ID de la sesión.", required = true)
            @PathVariable Long idSesion
    ) {
        SessionResponseDto response = sesionService.findById(actor, idSesion);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Sesión encontrada correctamente.",
                response
        ));
    }

    @DeleteMapping("/{idSesion}")
    @Operation(
            summary = "Revocar sesión",
            description = "Revoca una sesión por ID usando el flujo centralizado de SesionService."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión revocada correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para revocar esta sesión."),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada."),
            @ApiResponse(responseCode = "409", description = "Conflicto de negocio.")
    })
    public ResponseEntity<ApiResponseDto<SessionResponseDto>> deleteSession(
            AuthenticatedUserContext actor,
            @Parameter(description = "ID de la sesión.", required = true)
            @PathVariable Long idSesion
    ) {
        SessionResponseDto response = sesionService.revokeSession(
                actor,
                idSesion,
                "Revocación solicitada desde endpoint DELETE."
        );

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Sesión revocada correctamente.",
                response
        ));
    }

    @PostMapping("/{idSesion}/revocar")
    @Operation(
            summary = "Revocar sesión con motivo",
            description = "Revoca una sesión por ID con motivo funcional opcional."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión revocada correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para revocar esta sesión."),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada."),
            @ApiResponse(responseCode = "409", description = "Conflicto de negocio.")
    })
    public ResponseEntity<ApiResponseDto<SessionResponseDto>> revokeSession(
            AuthenticatedUserContext actor,
            @Parameter(description = "ID de la sesión.", required = true)
            @PathVariable Long idSesion,
            @Parameter(description = "Motivo funcional de revocación.")
            @RequestParam(required = false) String motivo
    ) {
        SessionResponseDto response = sesionService.revokeSession(actor, idSesion, motivo);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Sesión revocada correctamente.",
                response
        ));
    }
}