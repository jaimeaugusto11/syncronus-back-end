package com.zap.procurement.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ApprovalActionDTO {
    private UUID approvalId;
    private String action;
    private String comments;
    private UUID delegateToUserId;
}
