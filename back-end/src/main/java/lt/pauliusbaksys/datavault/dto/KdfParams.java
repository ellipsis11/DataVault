package lt.pauliusbaksys.datavault.dto;

public record KdfParams(
        String alg,
        int opsLimit,
        int memLimit,
        int outLen) {}
