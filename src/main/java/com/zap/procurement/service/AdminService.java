package com.zap.procurement.service;

import com.zap.procurement.dto.AdminDashboardDTO;
import com.zap.procurement.repository.DepartmentRepository;
import com.zap.procurement.repository.UserRepository;
import com.zap.procurement.repository.WorkflowTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private WorkflowTemplateRepository workflowTemplateRepository;

    public AdminDashboardDTO getDashboardStats(UUID tenantId) {
        AdminDashboardDTO dto = new AdminDashboardDTO();

        // In a real multi-tenant scenario, we would filter by tenantId.
        // Assuming repositories have methods like countByTenantId or we filter standard
        // count if tenant isolation is enforced purely by filter.
        // Based on previous checks, repositories seem to have findByTenantId.
        // Let's use the list size for now or specific count methods if they exist.
        // For efficiency, count methods are better, but I'll use what's safe based on
        // known repo methods or add count methods if needed.
        // Simple count() returns all records. If tenant isolation is manual, we need
        // countByTenantId.
        // Checking DepartmentRepository saw findByTenantId.
        // Let's assume we need to add countByTenantId to repositories or just use
        // findByTenantId(tenantId).size() for now as it's safer without modifying repos
        // yet.

        dto.setTotalUsers((long) userRepository.findByTenantId(tenantId).size());
        dto.setTotalDepartments((long) departmentRepository.findByTenantId(tenantId).size());
        dto.setTotalWorkflows((long) workflowTemplateRepository.findByTenantId(tenantId).size());

        return dto;
    }
}
