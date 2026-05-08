package com.upsjb.ms1.service.contract;

import com.upsjb.ms1.dto.auth.request.RegisterClienteRequestDto;
import com.upsjb.ms1.dto.auth.response.AuthTokenResponseDto;

public interface ClienteRegistrationService {

    AuthTokenResponseDto register(RegisterClienteRequestDto request);
}