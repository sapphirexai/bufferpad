package cn.tpl.opc.auth;

import cn.tpl.opc.commons.dto.ResultDTO;
import lombok.Data;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.validation.constraints.*;

@RestController
@RequestMapping("/users")
public class UserController {
    private final AuthService service;
    public UserController(AuthService service) { this.service = service; }

    @GetMapping public ResultDTO<?> list() { return ResultDTO.success(service.users()); }
    @PostMapping public ResultDTO<?> create(@AuthenticationPrincipal AuthPrincipal actor, @Valid @RequestBody Create body) {
        service.createUser(actor, body.username, body.password, body.role); return ResultDTO.success(true);
    }
    @PutMapping("/{id}") public ResultDTO<?> update(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable long id, @Valid @RequestBody Update body) {
        service.updateUser(actor, id, body.role, body.enabled); return ResultDTO.success(true);
    }
    @PostMapping("/{id}/password") public ResultDTO<?> reset(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable long id, @Valid @RequestBody Reset body) {
        service.resetPassword(actor, id, body.password); return ResultDTO.success(true);
    }
    @Data public static class Create {
        @NotBlank @Size(max=32) private String username;
        @NotBlank @Size(min=8,max=64) private String password;
        @NotBlank private String role;
    }
    @Data public static class Update { @NotBlank private String role; @NotNull private Boolean enabled; }
    @Data public static class Reset { @NotBlank @Size(min=8,max=64) private String password; }
}
