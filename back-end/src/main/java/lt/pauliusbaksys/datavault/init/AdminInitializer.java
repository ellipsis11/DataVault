package lt.pauliusbaksys.datavault.init;

import lt.pauliusbaksys.datavault.controller.AuthController;
import lt.pauliusbaksys.datavault.enums.UserRole;
import lt.pauliusbaksys.datavault.model.User;
import lt.pauliusbaksys.datavault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final String adminEmail;
    private final String adminPassword;
    private final Logger logger = LoggerFactory.getLogger(AuthController.class);


    public AdminInitializer(
            PasswordEncoder passwordEncoder, UserRepository userRepository,
            @Value("${admin.email}") String adminEmail,
            @Value("${admin.password}") String adminPassword
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException("Administratoriaus prisijungimo duomenys nesukonfigūruoti!");
        }

        userRepository.findUserByEmail(adminEmail).orElseGet(() -> {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(UserRole.ADMIN);
            return userRepository.save(admin);
        });
    }
}
