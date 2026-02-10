package com.zap.procurement.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class DemoUserRequest {
    private String email;
    private String role; // Role name (slug or name)
    private UUID tenantId;
}
