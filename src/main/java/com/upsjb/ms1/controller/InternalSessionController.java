// ruta: src/main/java/com/upsjb/ms1/controller/InternalSessionController.java
package com.upsjb.ms1.controller;

import com.upsjb.ms1.config.SwaggerSecurityConfig;
import com.upsjb.ms1.dto.internal.request.InternalSessionValidationRequestDto;
import com.upsjb.ms1.dto.internal.response.InternalSessionValidationResponseDto;
import com.upsjb.ms1.dto.shared.ApiResponseDto;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;
import com.upsjb.ms1.service.contract.SesionService;
import com.upsjb.ms1.shared.response.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/api/internal/sesiones")
public class InternalSessionController {

    private final SesionService sesionService;
    private final ApiResponseFactory apiResponseFactory;

    public InternalSessionController(
            SesionService sesionService,
            ApiResponseFactory apiResponseFactory
    ) {
        this.sesionService = sesionService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponseDto<InternalSessionValidationResponseDto>> validateSession(
            AuthenticatedUserContext actor,
            @Valid @RequestBody InternalSessionValidationRequestDto request
    ) {
        InternalSessionValidationResponseDto response = sesionService.validateInternalSession(
                actor,
                request
        );

        return ResponseEntity.ok(apiResponseFactory.ok(
                "Sesión MS1 validada correctamente.",
                response
        ));
    }
}