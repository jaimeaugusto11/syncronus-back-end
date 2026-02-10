package com.zap.procurement.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ApprovalDTO {
    private UUID id;
    private Integer level;
    private String status;
    private String comments;
    private String actionDate;

    // Approver info
    private UUID approverId;
    private String approverName;
    private String approverEmail;
    private String approverRole;
}
