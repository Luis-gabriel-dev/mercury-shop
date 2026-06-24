package dev.adastratech.mercuryshop.user.adapter.in.web;

import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.PageResponse;
import dev.adastratech.mercuryshop.user.adapter.in.web.dto.UserResponse;
import dev.adastratech.mercuryshop.user.application.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Endpoints administrativos de usuários (somente ADMIN — autorização no SecurityConfig). */
@RestController
@RequestMapping("/v1/users")
class AdminUserController {

    private final UserService userService;

    AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    PageResponse<UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "ASC") PageQuery.Direction direction) {
        PageQuery query = new PageQuery(page, size, sort, direction);
        return PageResponse.from(userService.list(query).map(UserWebMapper::toResponse));
    }

    @GetMapping("/{id}")
    UserResponse get(@PathVariable UUID id) {
        return UserWebMapper.toResponse(userService.get(id));
    }
}
