// ruta: src/main/java/com/upsjb/ms1/dto/internal/request/InternalSessionValidationRequestDto.java
package com.upsjb.ms1.dto.internal.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

public record InternalSessionValidationRequestDto(

        @NotNull(message = "El idUsuarioMs1 es obligatorio.")
        @Positive(message = "El idUsuarioMs1 debe ser mayor a 0.")
        Long idUsuarioMs1,

        @NotNull(message = "El sessionId es obligatorio.")
        @Positive(message = "El sessionId debe ser mayor a 0.")
        Long sessionId

) implements Serializable {
}