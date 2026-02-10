package com.zap.procurement.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UserDTO {
    private UUID id;
    private String name;
    private String email;
    private String role;
    private UUID departmentId;
    private String departmentName;
    private String avatarUrl;
    private String status;
    private UUID tenantId;
    private UUID supplierId;
    private java.util.Set<String> permissions;
}
