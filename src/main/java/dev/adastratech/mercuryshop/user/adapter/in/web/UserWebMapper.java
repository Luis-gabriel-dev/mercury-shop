package dev.adastratech.mercuryshop.user.adapter.in.web;

import dev.adastratech.mercuryshop.user.adapter.in.web.dto.ChangePasswordRequest;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.LoginRequest;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.RegisterRequest;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.ResetPasswordRequest;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.TokenResponse;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.UpdateMeRequest;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.UserResponse;
import dev.adastratech.mercuryshop.user.application.AuthTokens;
import dev.adastratech.mercuryshop.user.application.command.ChangePasswordCommand;
import dev.adastratech.mercuryshop.user.application.command.LoginCommand;
import dev.adastratech.mercuryshop.user.application.command.RegisterCommand;
import dev.adastratech.mercuryshop.user.application.command.ResetPasswordCommand;
import dev.adastratech.mercuryshop.user.application.command.UpdateProfileCommand;
import dev.adastratech.mercuryshop.user.domain.Role;
import dev.adastratech.mercuryshop.user.domain.User;

final class UserWebMapper {

    private UserWebMapper() {
    }

    static RegisterCommand toCommand(RegisterRequest request) {
        return new RegisterCommand(request.email(), request.password(), request.fullName(), request.phone());
    }

    static LoginCommand toCommand(LoginRequest request) {
        return new LoginCommand(request.email(), request.password());
    }

    static ResetPasswordCommand toCommand(ResetPasswordRequest request) {
        return new ResetPasswordCommand(request.token(), request.newPassword());
    }

    static ChangePasswordCommand toCommand(ChangePasswordRequest request) {
        return new ChangePasswordCommand(request.currentPassword(), request.newPassword());
    }

    static UpdateProfileCommand toCommand(UpdateMeRequest request) {
        return new UpdateProfileCommand(request.fullName(), request.phone());
    }

    static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                user.getStatus().name(), user.isEmailVerified(),
                user.getRoles().stream().map(Role::name).sorted().toList(), user.getCreatedAt());
    }

    static TokenResponse toTokenResponse(AuthTokens tokens) {
        return TokenResponse.bearer(tokens.accessToken(), tokens.accessTokenExpiresInSeconds());
    }
}
