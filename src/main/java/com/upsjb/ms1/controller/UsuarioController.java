package com.upsjb.ms1.controller;

import com.upsjb.ms1.config.SwaggerSecurityConfig;
import com.upsjb.ms1.dto.shared.ApiResponseDto;
import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.dto.usuario.filter.UsuarioFilterDto;
import com.upsjb.ms1.dto.usuario.request.UsuarioChangeEstadoRequestDto;
import com.upsjb.ms1.dto.usuario.request.UsuarioChangeRolRequestDto;
import com.upsjb.ms1.dto.usuario.request.UsuarioCreateRequestDto;
import com.upsjb.ms1.dto.usuario.request.UsuarioUpdateRequestDto;
import com.upsjb.ms1.dto.usuario.response.UsuarioDetailResponseDto;
import com.upsjb.ms1.dto.usuario.response.UsuarioLookupDto;
import com.upsjb.ms1.dto.usuario.response.UsuarioResponseDto;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.service.contract.UsuarioService;
import com.upsjb.ms1.shared.response.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@SecurityRequirement(name = SwaggerSecurityConfig.BEARER_AUTH_SCHEME)
@Tag(
        name = "Usuarios",
        description = "Operaciones de administración, consulta, estado y rol de usuarios."
)
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final ApiResponseFactory apiResponseFactory;

    public UsuarioController(
            UsuarioService usuarioService,
            ApiResponseFactory apiResponseFactory
    ) {
        this.usuarioService = usuarioService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping
    @Operation(
            summary = "Listar usuarios",
            description = "Lista usuarios con filtros y paginación. La autorización contextual se valida en el service/policy."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuarios listados correctamente."),
            @ApiResponse(responseCode = "400", description = "Filtros o paginación inválidos."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para listar usuarios.")
    })
    public ResponseEntity<ApiResponseDto<PageResponseDto<UsuarioResponseDto>>> findAll(
            AuthenticatedUserContext actor,
            @ParameterObject @ModelAttribute UsuarioFilterDto filter,
            @Valid @ParameterObject @ModelAttribute PageRequestDto pageRequest
    ) {
        PageResponseDto<UsuarioResponseDto> response = usuarioService.findAll(actor, filter, pageRequest);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Usuarios listados correctamente.",
                response
        ));
    }

    @PostMapping
    @Operation(
            summary = "Crear usuario",
            description = "Crea un usuario del sistema. Las reglas de rol, duplicados, password y auditoría se gestionan en el service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para crear usuarios."),
            @ApiResponse(responseCode = "409", description = "Usuario duplicado o conflicto de negocio.")
    })
    public ResponseEntity<ApiResponseDto<UsuarioResponseDto>> create(
            AuthenticatedUserContext actor,
            @Valid @RequestBody UsuarioCreateRequestDto request
    ) {
        UsuarioResponseDto response = usuarioService.create(actor, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(apiResponseFactory.created(
                        "Usuario creado correctamente.",
                        response
                ));
    }

    @GetMapping("/lookup")
    @Operation(
            summary = "Listar usuarios para selección",
            description = "Lista usuarios en formato ligero para componentes de selección."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuarios para selección listados correctamente."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para consultar usuarios.")
    })
    public ResponseEntity<ApiResponseDto<List<UsuarioLookupDto>>> findLookup(
            AuthenticatedUserContext actor
    ) {
        List<UsuarioLookupDto> response = usuarioService.findLookup(actor);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Usuarios para selección listados correctamente.",
                response
        ));
    }

    @GetMapping("/{idUsuario}")
    @Operation(
            summary = "Obtener usuario por ID",
            description = "Obtiene el detalle seguro de un usuario por ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado correctamente."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para consultar este usuario."),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    public ResponseEntity<ApiResponseDto<UsuarioDetailResponseDto>> findById(
            AuthenticatedUserContext actor,
            @Parameter(description = "ID del usuario.", required = true)
            @PathVariable Long idUsuario
    ) {
        UsuarioDetailResponseDto response = usuarioService.findById(actor, idUsuario);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Usuario encontrado correctamente.",
                response
        ));
    }

    @PutMapping("/{idUsuario}")
    @Operation(
            summary = "Actualizar usuario",
            description = "Actualiza datos editables del usuario. Las reglas de autorización y validación se aplican en service/policy/validator."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para actualizar este usuario."),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflicto de negocio.")
    })
    public ResponseEntity<ApiResponseDto<UsuarioResponseDto>> update(
            AuthenticatedUserContext actor,
            @Parameter(description = "ID del usuario.", required = true)
            @PathVariable Long idUsuario,
            @Valid @RequestBody UsuarioUpdateRequestDto request
    ) {
        UsuarioResponseDto response = usuarioService.update(actor, idUsuario, request);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Usuario actualizado correctamente.",
                response
        ));
    }

    @PatchMapping("/{idUsuario}/estado")
    @Operation(
            summary = "Cambiar estado de usuario",
            description = "Activa, inactiva, bloquea, desbloquea o elimina lógicamente un usuario según reglas del service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de usuario actualizado correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para cambiar el estado."),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflicto de negocio.")
    })
    public ResponseEntity<ApiResponseDto<UsuarioResponseDto>> changeEstado(
            AuthenticatedUserContext actor,
            @Parameter(description = "ID del usuario.", required = true)
            @PathVariable Long idUsuario,
            @Valid @RequestBody UsuarioChangeEstadoRequestDto request
    ) {
        UsuarioResponseDto response = usuarioService.changeEstado(actor, idUsuario, request);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Estado de usuario actualizado correctamente.",
                response
        ));
    }

    @PatchMapping("/{idUsuario}/rol")
    @Operation(
            summary = "Cambiar rol de usuario",
            description = "Cambia el rol de un usuario. El service debe revocar sesiones si corresponde."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol de usuario actualizado correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para cambiar el rol."),
            @ApiResponse(responseCode = "404", description = "Usuario o rol no encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflicto de negocio.")
    })
    public ResponseEntity<ApiResponseDto<UsuarioResponseDto>> changeRol(
            AuthenticatedUserContext actor,
            @Parameter(description = "ID del usuario.", required = true)
            @PathVariable Long idUsuario,
            @Valid @RequestBody UsuarioChangeRolRequestDto request
    ) {
        UsuarioResponseDto response = usuarioService.changeRol(actor, idUsuario, request);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Rol de usuario actualizado correctamente.",
                response
        ));
    }
}