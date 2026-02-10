package com.zap.procurement.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierLoginResponse extends LoginResponse {
    private boolean mustChangePassword;

    public SupplierLoginResponse(String token, String refreshToken, UserDTO user, boolean mustChangePassword) {
        super(token, refreshToken, user);
        this.mustChangePassword = mustChangePassword;
    }
}
