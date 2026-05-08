// src/main/java/com/upsjb/ms1/service/contract/VerificacionCodigoService.java
package com.upsjb.ms1.service.contract;

import com.upsjb.ms1.domain.enums.TipoCodigoVerificacion;
import com.upsjb.ms1.dto.verificacion.request.ResendVerificationCodeRequestDto;
import com.upsjb.ms1.dto.verificacion.request.SendVerificationCodeRequestDto;
import com.upsjb.ms1.dto.verificacion.request.VerifyCodeRequestDto;
import com.upsjb.ms1.dto.verificacion.response.VerificationCodeResponseDto;
import com.upsjb.ms1.dto.verificacion.response.VerificationStatusResponseDto;

public interface VerificacionCodigoService {

    VerificationCodeResponseDto send(SendVerificationCodeRequestDto request);

    VerificationCodeResponseDto resend(ResendVerificationCodeRequestDto request);

    VerificationStatusResponseDto verify(VerifyCodeRequestDto request);

    VerificationStatusResponseDto status(
            String email,
            TipoCodigoVerificacion tipoCodigo
    );

    int expirePendingCodes();

    int revokePendingByEmailAndType(
            String email,
            TipoCodigoVerificacion tipoCodigo
    );
}