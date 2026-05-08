package com.upsjb.ms1.controller;

import com.upsjb.ms1.config.SwaggerSecurityConfig;
import com.upsjb.ms1.dto.password.request.ChangePasswordRequestDto;
import com.upsjb.ms1.dto.password.request.ConfirmChangePasswordRequestDto;
import com.upsjb.ms1.dto.password.request.ForgotPasswordRequestDto;
import com.upsjb.ms1.dto.password.request.ResetPasswordRequestDto;
import com.upsjb.ms1.dto.password.response.ChangePasswordVerificationResponseDto;
import com.upsjb.ms1.dto.password.response.PasswordOperationResponseDto;
import com.upsjb.ms1.dto.shared.ApiResponseDto;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.service.contract.PasswordResetService;
import com.upsjb.ms1.shared.response.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/password")
@Tag(
        name = "Contraseña",
        description = "Operaciones de cambio y recuperación de contraseña."
)
public class PasswordController {

    private final PasswordResetService passwordResetService;
    private final ApiResponseFactory apiResponseFactory;

    public PasswordController(
            PasswordResetService passwordResetService,
            ApiResponseFactory apiResponseFactory
    ) {
        this.passwordResetService = passwordResetService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/change/request")
    @SecurityRequirement(name = SwaggerSecurityConfig.BEARER_AUTH_SCHEME)
    @Operation(
            summary = "Solicitar cambio de contraseña",
            description = "Genera y envía el código de verificación para que el usuario autenticado pueda cambiar su contraseña."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código para cambio de contraseña enviado correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para solicitar el cambio.")
    })
    public ResponseEntity<ApiResponseDto<ChangePasswordVerificationResponseDto>> requestPasswordChange(
            AuthenticatedUserContext actor,
            @Valid @RequestBody ChangePasswordRequestDto request
    ) {
        ChangePasswordVerificationResponseDto response = passwordResetService.requestPasswordChange(actor, request);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Código para cambio de contraseña enviado correctamente.",
                response
        ));
    }

    @PostMapping("/change/confirm")
    @SecurityRequirement(name = SwaggerSecurityConfig.BEARER_AUTH_SCHEME)
    @Operation(
            summary = "Confirmar cambio de contraseña",
            description = "Valida el código enviado al correo y cambia la contraseña del usuario autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña cambiada correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o código incorrecto."),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado."),
            @ApiResponse(responseCode = "403", description = "No tiene permiso para confirmar el cambio.")
    })
    public ResponseEntity<ApiResponseDto<PasswordOperationResponseDto>> confirmPasswordChange(
            AuthenticatedUserContext actor,
            @Valid @RequestBody ConfirmChangePasswordRequestDto request
    ) {
        PasswordOperationResponseDto response = passwordResetService.confirmPasswordChange(actor, request);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Contraseña cambiada correctamente.",
                response
        ));
    }

    @PostMapping("/forgot")
    @Operation(
            summary = "Solicitar recuperación de contraseña",
            description = "Solicita recuperación de contraseña. La respuesta no revela si el correo existe."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitud procesada correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida.")
    })
    public ResponseEntity<ApiResponseDto<PasswordOperationResponseDto>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDto request
    ) {
        PasswordOperationResponseDto response = passwordResetService.forgotPassword(request);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Si el correo está registrado, se enviaron instrucciones de recuperación.",
                response
        ));
    }

    @PostMapping("/reset")
    @Operation(
            summary = "Restablecer contraseña",
            description = "Restablece la contraseña usando un token o código de recuperación válido."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o token/código incorrecto."),
            @ApiResponse(responseCode = "401", description = "Token/código inválido, expirado o revocado.")
    })
    public ResponseEntity<ApiResponseDto<PasswordOperationResponseDto>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request
    ) {
        PasswordOperationResponseDto response = passwordResetService.resetPassword(request);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Contraseña restablecida correctamente.",
                response
        ));
    }
}