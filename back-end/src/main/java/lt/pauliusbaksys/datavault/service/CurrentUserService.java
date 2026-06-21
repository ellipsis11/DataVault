package lt.pauliusbaksys.datavault.service;

import jakarta.servlet.http.HttpServletRequest;
import lt.pauliusbaksys.datavault.filter.JwtAuthFilter;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {

    private final UserService userService;
    private final JwtAuthFilter jwtAuthFilter;

    public CurrentUserService(UserService userService, JwtAuthFilter jwtAuthFilter) {
        this.userService = userService;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    public UUID getCurrentUserId(HttpServletRequest req){
        return userService.getUserId(jwtAuthFilter.resolveToken(req));
    }

    public UUID getCurrentUserId(String jwt){
        return userService.getUserId(jwt);
    }
}
