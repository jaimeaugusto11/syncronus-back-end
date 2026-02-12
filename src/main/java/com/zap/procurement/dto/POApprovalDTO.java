package com.zap.procurement.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class POApprovalDTO {
    private UUID id;
    private Integer level;
    private String status;
    private String comments;
    private LocalDateTime actionDate;

    // Purchase Order details
    private UUID purchaseOrderId;
    private String purchaseOrderCode;
    private String supplierName;
    private String departmentName;
    private String totalAmount;
    private String currency;
    private LocalDateTime poCreatedAt;

    // Approver details
    private UUID approverId;
    private String approverName;
}
