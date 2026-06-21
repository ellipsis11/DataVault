package lt.pauliusbaksys.datavault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FrontRequest(@Email @NotBlank String email, @NotBlank @Size(min = 8) String password) {}
