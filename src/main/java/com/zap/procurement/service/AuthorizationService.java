package com.zap.procurement.service;

import com.zap.procurement.domain.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthorizationService {

    public boolean canApproveRequisition(User user) {
        if (user.getRole() == null)
            return false;
        String roleName = user.getRole().getName();
        return "ADMIN_GERAL".equalsIgnoreCase(roleName) ||
                "GESTOR_PROCUREMENT".equalsIgnoreCase(roleName);
    }

    public boolean canCreateRFQ(User user) {
        if (user.getRole() == null)
            return false;
        String roleName = user.getRole().getName();
        return "ADMIN_GERAL".equalsIgnoreCase(roleName) ||
                "GESTOR_PROCUREMENT".equalsIgnoreCase(roleName);
    }

    public boolean canViewAllRequisitions(User user) {
        if (user.getRole() == null)
            return false;
        String roleName = user.getRole().getName();
        return "ADMIN_GERAL".equalsIgnoreCase(roleName) ||
                "GESTOR_PROCUREMENT".equalsIgnoreCase(roleName);
    }

    public boolean canManageUsers(User user) {
        if (user.getRole() == null)
            return false;
        return "ADMIN_GERAL".equalsIgnoreCase(user.getRole().getName());
    }

    public boolean canCreatePurchaseOrder(User user) {
        if (user.getRole() == null)
            return false;
        String roleName = user.getRole().getName();
        return "ADMIN_GERAL".equalsIgnoreCase(roleName) ||
                "GESTOR_PROCUREMENT".equalsIgnoreCase(roleName);
    }

    public boolean canSubmitProposal(User user) {
        return false;
    }

    public boolean canViewRFQ(User user, UUID rfqId) {
        if (user.getRole() == null)
            return false;
        String roleName = user.getRole().getName();
        return "ADMIN_GERAL".equalsIgnoreCase(roleName) ||
                "GESTOR_PROCUREMENT".equalsIgnoreCase(roleName);
    }

    public boolean canEditRequisition(User user, UUID requisitionCreatorId, String status) {
        if (user.getRole() == null)
            return false;
        String roleName = user.getRole().getName();

        if ("ADMIN_GERAL".equalsIgnoreCase(roleName)) {
            return true;
        }

        if ("REQUISITANTE".equalsIgnoreCase(roleName)) {
            return user.getId().equals(requisitionCreatorId) && "DRAFT".equals(status);
        }

        return false;
    }

    public boolean canViewAuditLogs(User user) {
        if (user.getRole() == null)
            return false;
        return "ADMIN_GERAL".equalsIgnoreCase(user.getRole().getName());
    }

    public boolean canManageSuppliers(User user) {
        if (user.getRole() == null)
            return false;
        String roleName = user.getRole().getName();
        return "ADMIN_GERAL".equalsIgnoreCase(roleName) ||
                "GESTOR_PROCUREMENT".equalsIgnoreCase(roleName);
    }

    public String getDashboardRoute(User user) {
        if (user.getRole() == null)
            return "/dashboard";
        String roleName = user.getRole().getName();

        if ("ADMIN_GERAL".equalsIgnoreCase(roleName)) {
            return "/admin/dashboard";
        } else if ("GESTOR_PROCUREMENT".equalsIgnoreCase(roleName)) {
            return "/procurement/dashboard";
        } else if ("REQUISITANTE".equalsIgnoreCase(roleName)) {
            return "/dashboard";
        }

        return "/dashboard";
    }
}
