package lt.pauliusbaksys.datavault.controller;

import jakarta.servlet.http.HttpServletRequest;
import lt.pauliusbaksys.datavault.dto.UserSearchItem;
import lt.pauliusbaksys.datavault.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/search")
    public List<UserSearchItem> searchUsers(@RequestParam String query){
        log.warn("Query: {}", query);
        return userService.searchUserSearchItems(query);
    }
}
