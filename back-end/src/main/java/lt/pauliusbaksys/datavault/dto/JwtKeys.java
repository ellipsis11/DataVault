package lt.pauliusbaksys.datavault.dto;

import java.security.PrivateKey;
import java.security.PublicKey;

public record JwtKeys(PrivateKey privateKey, PublicKey publicKey) {}
