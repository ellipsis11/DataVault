package lt.pauliusbaksys.datavault.controller;

import jakarta.servlet.http.HttpServletRequest;
import lt.pauliusbaksys.datavault.dto.AdminUserDetails;
import lt.pauliusbaksys.datavault.dto.AdminUserPage;
import lt.pauliusbaksys.datavault.service.CurrentUserService;
import lt.pauliusbaksys.datavault.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    public AdminUserController(UserService userService, CurrentUserService currentUserService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public AdminUserPage getAllUsers(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){
        return userService.getUsersForAdmin(query.trim(), page, size);
    }

    @GetMapping("/{userId}")
    public AdminUserDetails getUserDetails(@PathVariable UUID userId){
        return userService.getUserDetails(userId);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(HttpServletRequest req, @PathVariable UUID userId){
        userService.deleteUser(userId, currentUserService.getCurrentUserId(req));
        return ResponseEntity.noContent().build();
    }
}
