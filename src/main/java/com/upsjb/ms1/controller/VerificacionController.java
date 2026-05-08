package com.upsjb.ms1.controller;

import com.upsjb.ms1.dto.shared.ApiResponseDto;
import com.upsjb.ms1.dto.verificacion.request.ResendVerificationCodeRequestDto;
import com.upsjb.ms1.dto.verificacion.request.SendVerificationCodeRequestDto;
import com.upsjb.ms1.dto.verificacion.request.VerifyCodeRequestDto;
import com.upsjb.ms1.dto.verificacion.response.VerificationCodeResponseDto;
import com.upsjb.ms1.dto.verificacion.response.VerificationStatusResponseDto;
import com.upsjb.ms1.service.contract.VerificacionCodigoService;
import com.upsjb.ms1.shared.response.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/verificaciones")
@Tag(
        name = "Verificaciones",
        description = "Operaciones de envío, validación y reenvío de códigos de verificación."
)
public class VerificacionController {

    private final VerificacionCodigoService verificacionCodigoService;
    private final ApiResponseFactory apiResponseFactory;

    public VerificacionController(
            VerificacionCodigoService verificacionCodigoService,
            ApiResponseFactory apiResponseFactory
    ) {
        this.verificacionCodigoService = verificacionCodigoService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/enviar")
    @Operation(
            summary = "Enviar código de verificación",
            description = "Genera y envía un código de verificación al correo indicado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código de verificación enviado correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida."),
            @ApiResponse(responseCode = "409", description = "Existe un conflicto con la solicitud.")
    })
    public ResponseEntity<ApiResponseDto<VerificationCodeResponseDto>> send(
            @Valid @RequestBody SendVerificationCodeRequestDto request
    ) {
        VerificationCodeResponseDto response = verificacionCodigoService.send(request);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Código de verificación enviado correctamente.",
                response
        ));
    }

    @PostMapping("/validar")
    @Operation(
            summary = "Validar código de verificación",
            description = "Valida un código de verificación según correo y tipo de código."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código de verificación validado correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o código incorrecto."),
            @ApiResponse(responseCode = "401", description = "Código inválido, expirado o bloqueado.")
    })
    public ResponseEntity<ApiResponseDto<VerificationStatusResponseDto>> verify(
            @Valid @RequestBody VerifyCodeRequestDto request
    ) {
        VerificationStatusResponseDto response = verificacionCodigoService.verify(request);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Código de verificación validado correctamente.",
                response
        ));
    }

    @PostMapping("/reenviar")
    @Operation(
            summary = "Reenviar código de verificación",
            description = "Revoca o reemplaza el código pendiente según reglas del service y envía un nuevo código."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código de verificación reenviado correctamente."),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida."),
            @ApiResponse(responseCode = "409", description = "Existe un conflicto con la solicitud.")
    })
    public ResponseEntity<ApiResponseDto<VerificationCodeResponseDto>> resend(
            @Valid @RequestBody ResendVerificationCodeRequestDto request
    ) {
        VerificationCodeResponseDto response = verificacionCodigoService.resend(request);

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Código de verificación reenviado correctamente.",
                response
        ));
    }
}