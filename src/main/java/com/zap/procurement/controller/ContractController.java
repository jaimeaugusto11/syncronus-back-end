package com.zap.procurement.controller;

import com.zap.procurement.config.TenantContext;
import com.zap.procurement.domain.Contract;
import com.zap.procurement.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/contracts")
@CrossOrigin(origins = "*")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER', 'DEPT_HEAD')")
    public ResponseEntity<List<Contract>> getAllContracts() {
        return ResponseEntity.ok(contractService.getAllContracts(TenantContext.getCurrentTenant()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContract(@PathVariable UUID id) {
        return ResponseEntity.ok(contractService.getContractById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER')")
    public ResponseEntity<Contract> createContract(@RequestBody Contract contract) {
        contract.setTenantId(TenantContext.getCurrentTenant());
        return ResponseEntity.ok(contractService.createContract(contract));
    }

    @PostMapping("/{id}/sign")
    public ResponseEntity<Contract> signContract(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean isSupplier) {
        // In a real scenario, the userId would come from the JWT/Principal
        // For simulation, we just mark it as signed by the respective role
        return ResponseEntity.ok(contractService.signContract(id, null, isSupplier));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER')")
    public ResponseEntity<Contract> activateContract(@PathVariable UUID id) {
        return ResponseEntity.ok(contractService.activateContract(id));
    }
}
