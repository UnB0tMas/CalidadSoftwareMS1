package com.upsjb.ms1.service.contract;

import com.upsjb.ms1.dto.password.request.ChangePasswordRequestDto;
import com.upsjb.ms1.dto.password.request.ConfirmChangePasswordRequestDto;
import com.upsjb.ms1.dto.password.request.ForgotPasswordRequestDto;
import com.upsjb.ms1.dto.password.request.ResetPasswordRequestDto;
import com.upsjb.ms1.dto.password.response.ChangePasswordVerificationResponseDto;
import com.upsjb.ms1.dto.password.response.PasswordOperationResponseDto;
import com.upsjb.ms1.security.principal.AuthenticatedUserContext;

public interface PasswordResetService {

    ChangePasswordVerificationResponseDto requestPasswordChange(
            AuthenticatedUserContext actor,
            ChangePasswordRequestDto request
    );

    PasswordOperationResponseDto confirmPasswordChange(
            AuthenticatedUserContext actor,
            ConfirmChangePasswordRequestDto request
    );

    PasswordOperationResponseDto forgotPassword(ForgotPasswordRequestDto request);

    PasswordOperationResponseDto resetPassword(ResetPasswordRequestDto request);
}