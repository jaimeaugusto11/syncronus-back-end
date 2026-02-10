package com.zap.procurement.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class PendingApprovalDTO {
    private UUID id;
    private Integer level;
    private String status;

    // Approver info
    private UUID approverId;
    private String approverName;
    private String approverEmail;
    private String approverRole;

    // Requisition info
    private UUID requisitionId;
    private String requisitionCode;
    private String requesterName;
    private String departmentName;
    private String totalAmount;
    private String createdAt;
    private String justification;
    private Boolean extraordinary;
}
