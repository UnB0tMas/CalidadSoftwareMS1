package com.upsjb.ms1.controller;

import com.upsjb.ms1.config.SwaggerSecurityConfig;
import com.upsjb.ms1.domain.enums.EstadoRegistro;
import com.upsjb.ms1.dto.rol.filter.RolFilterDto;
import com.upsjb.ms1.dto.rol.request.RolCreateRequestDto;
import com.upsjb.ms1.dto.rol.request.RolUpdateRequestDto;
import com.upsjb.ms1.dto.rol.response.RolLookupDto;
import com.upsjb.ms1.dto.rol.response.RolResponseDto;
import com.upsjb.ms1.dto.shared.ApiResponseDto;
import com.upsjb.ms1.dto.shared.PageRequestDto;
import com.upsjb.ms1.dto.shared.PageResponseDto;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.service.contract.RolService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@SecurityRequirement(name = SwaggerSecurityConfig.BEARER_AUTH_SCHEME)
@Tag(
        name = "Roles",
        description = "Operaciones de administración y consulta de roles."
)
public class RolController {

    private final RolService rolService;
    private final ApiResponseFactory apiResponseFactory;

    public RolController(
            RolService rolService,
            ApiResponseFactory apiResponseFactory
    ) {
        this.rolService = rolService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping
    @Operation(
            summary = "Listar roles",
            description = "Lista roles con filtros y paginación."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles listados correctamente."),
            @ApiResponse(responseCode = "400", description = "Filtros o paginación inválidos."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para listar roles.")
    })
    public ResponseEntity<ApiResponseDto<PageResponseDto<RolResponseDto>>> findAll(
            AuthenticatedUserContext actor,
            @ParameterObject @ModelAttribute RolFilterDto filter,
            @Valid @ParameterObject @ModelAttribute PageRequestDto pageRequest
    ) {
        PageResponseDto<RolResponseDto> response = rolService.findAll(actor, filter, pageRequest);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Roles listados correctamente.",
                response
        ));
    }

    @PostMapping
    @Operation(
            summary = "Crear rol",
            description = "Crea un rol. Validaciones de código, duplicidad, sistema y reglas de seguridad se realizan en service/validator/policy."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rol creado correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para crear roles."),
            @ApiResponse(responseCode = "409", description = "Rol duplicado o conflicto de negocio.")
    })
    public ResponseEntity<ApiResponseDto<RolResponseDto>> create(
            AuthenticatedUserContext actor,
            @Valid @RequestBody RolCreateRequestDto request
    ) {
        RolResponseDto response = rolService.create(actor, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(apiResponseFactory.created(
                        "Rol creado correctamente.",
                        response
                ));
    }

    @GetMapping("/lookup")
    @Operation(
            summary = "Listar roles para selección",
            description = "Lista roles en formato ligero para componentes de selección."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles para selección listados correctamente."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para consultar roles.")
    })
    public ResponseEntity<ApiResponseDto<List<RolLookupDto>>> findLookup(
            AuthenticatedUserContext actor
    ) {
        List<RolLookupDto> response = rolService.findLookup(actor);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Roles para selección listados correctamente.",
                response
        ));
    }

    @GetMapping("/{idRol}")
    @Operation(
            summary = "Obtener rol por ID",
            description = "Obtiene un rol por ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol encontrado correctamente."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para consultar este rol."),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado.")
    })
    public ResponseEntity<ApiResponseDto<RolResponseDto>> findById(
            AuthenticatedUserContext actor,
            @Parameter(description = "ID del rol.", required = true)
            @PathVariable Long idRol
    ) {
        RolResponseDto response = rolService.findById(actor, idRol);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Rol encontrado correctamente.",
                response
        ));
    }

    @PutMapping("/{idRol}")
    @Operation(
            summary = "Actualizar rol",
            description = "Actualiza datos editables de un rol."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol actualizado correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para actualizar este rol."),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflicto de negocio.")
    })
    public ResponseEntity<ApiResponseDto<RolResponseDto>> update(
            AuthenticatedUserContext actor,
            @Parameter(description = "ID del rol.", required = true)
            @PathVariable Long idRol,
            @Valid @RequestBody RolUpdateRequestDto request
    ) {
        RolResponseDto response = rolService.update(actor, idRol, request);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Rol actualizado correctamente.",
                response
        ));
    }

    @PatchMapping("/{idRol}/estado")
    @Operation(
            summary = "Cambiar estado de rol",
            description = "Cambia el estado de un rol usando el enum oficial EstadoRegistro."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de rol actualizado correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para cambiar el estado del rol."),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado."),
            @ApiResponse(responseCode = "409", description = "Conflicto de negocio.")
    })
    public ResponseEntity<ApiResponseDto<RolResponseDto>> changeEstado(
            AuthenticatedUserContext actor,
            @Parameter(description = "ID del rol.", required = true)
            @PathVariable Long idRol,
            @Parameter(description = "Nuevo estado del rol.", required = true)
            @RequestParam EstadoRegistro estado,
            @Parameter(description = "Motivo funcional del cambio de estado.")
            @RequestParam(required = false) String motivo
    ) {
        RolResponseDto response = rolService.changeEstado(actor, idRol, estado, motivo);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Estado de rol actualizado correctamente.",
                response
        ));
    }
}