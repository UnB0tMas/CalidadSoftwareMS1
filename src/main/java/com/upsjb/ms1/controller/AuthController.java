package com.upsjb.ms1.controller;

import com.upsjb.ms1.config.SwaggerSecurityConfig;
import com.upsjb.ms1.dto.auth.request.LoginRequestDto;
import com.upsjb.ms1.dto.auth.request.LogoutRequestDto;
import com.upsjb.ms1.dto.auth.request.RefreshTokenRequestDto;
import com.upsjb.ms1.dto.auth.response.AuthTokenResponseDto;
import com.upsjb.ms1.dto.auth.response.AuthUserResponseDto;
import com.upsjb.ms1.dto.auth.response.SessionResponseDto;
import com.upsjb.ms1.dto.shared.ApiResponseDto;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.service.contract.AuthService;
import com.upsjb.ms1.shared.response.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Autenticación",
        description = "Operaciones de autenticación, refresh token, cierre de sesión y usuario autenticado."
)
public class AuthController {

    private final AuthService authService;
    private final ApiResponseFactory apiResponseFactory;

    public AuthController(
            AuthService authService,
            ApiResponseFactory apiResponseFactory
    ) {
        this.authService = authService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica al usuario mediante username/email y contraseña. Retorna access token, refresh token, usuario y sesión."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida."),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas o usuario no autorizado."),
            @ApiResponse(responseCode = "423", description = "Cuenta bloqueada.")
    })
    public ResponseEntity<ApiResponseDto<AuthTokenResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request
    ) {
        AuthTokenResponseDto response = authService.login(request);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Login realizado correctamente.",
                response
        ));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar token",
            description = "Renueva el access token usando un refresh token válido y vigente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token renovado correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida."),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado o revocado.")
    })
    public ResponseEntity<ApiResponseDto<AuthTokenResponseDto>> refresh(
            @Valid @RequestBody RefreshTokenRequestDto request
    ) {
        AuthTokenResponseDto response = authService.refresh(request);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Token renovado correctamente.",
                response
        ));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = SwaggerSecurityConfig.BEARER_AUTH_SCHEME)
    @Operation(
            summary = "Cerrar sesión actual",
            description = "Revoca la sesión actual o la sesión indicada por refresh token/id según el contrato del servicio."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión cerrada correctamente."),
            @ApiResponse(responseCode = "400", description = "Identificador de sesión inválido."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para cerrar esta sesión."),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada.")
    })
    public ResponseEntity<ApiResponseDto<SessionResponseDto>> logout(
            AuthenticatedUserContext actor,
            @Valid @RequestBody(required = false) LogoutRequestDto request
    ) {
        SessionResponseDto response = authService.logout(actor, request);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Sesión cerrada correctamente.",
                response
        ));
    }

    @PostMapping("/logout-all")
    @SecurityRequirement(name = SwaggerSecurityConfig.BEARER_AUTH_SCHEME)
    @Operation(
            summary = "Cerrar todas las sesiones propias",
            description = "Revoca todas las sesiones activas del usuario autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesiones cerradas correctamente."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para ejecutar esta acción.")
    })
    public ResponseEntity<ApiResponseDto<Integer>> logoutAll(
            AuthenticatedUserContext actor
    ) {
        int response = authService.logoutAll(actor);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Sesiones cerradas correctamente.",
                response
        ));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = SwaggerSecurityConfig.BEARER_AUTH_SCHEME)
    @Operation(
            summary = "Obtener usuario autenticado",
            description = "Retorna información segura del usuario autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado obtenido correctamente."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado.")
    })
    public ResponseEntity<ApiResponseDto<AuthUserResponseDto>> me(
            AuthenticatedUserContext actor
    ) {
        AuthUserResponseDto response = authService.me(actor);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Usuario autenticado obtenido correctamente.",
                response
        ));
    }
}