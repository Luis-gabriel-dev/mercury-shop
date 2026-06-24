package dev.adastratech.mercuryshop.user.adapter.in.web;

import dev.adastratech.mercuryshop.user.adapter.in.web.dto.ChangePasswordRequest;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.MessageResponse;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.UpdateMeRequest;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.UserResponse;
import dev.adastratech.mercuryshop.user.application.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserWebMapper.toResponse(userService.get(currentUserId(jwt)));
    }

    @PatchMapping("/me")
    UserResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateMeRequest request) {
        return UserWebMapper.toResponse(userService.updateProfile(currentUserId(jwt), UserWebMapper.toCommand(request)));
    }

    @PostMapping("/me/change-password")
    MessageResponse changePassword(@AuthenticationPrincipal Jwt jwt,
                                   @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUserId(jwt), UserWebMapper.toCommand(request));
        return new MessageResponse("Senha alterada com sucesso");
    }

    private static UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
