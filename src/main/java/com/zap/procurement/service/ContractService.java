package com.zap.procurement.service;

import com.zap.procurement.domain.Contract;
import com.zap.procurement.domain.User;
import com.zap.procurement.repository.ContractRepository;
import com.zap.procurement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Contract> getAllContracts(UUID tenantId) {
        return contractRepository.findByTenantId(tenantId);
    }

    public Contract getContractById(UUID id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found: " + id));
    }

    @Transactional
    public Contract createContract(Contract contract) {
        if (contract.getCode() == null) {
            contract.setCode("CTR-" + System.currentTimeMillis());
        }
        return contractRepository.save(contract);
    }

    @Transactional
    public Contract signContract(UUID contractId, UUID userId, boolean isSupplier) {
        Contract contract = getContractById(contractId);

        if (isSupplier) {
            contract.setSupplierSignedAt(LocalDateTime.now());
            contract.setSupplierSignatureHash("SIG-SUP-" + UUID.randomUUID().toString());
        } else {
            contract.setBuyerSignedAt(LocalDateTime.now());
            contract.setBuyerSignatureHash("SIG-BUY-" + UUID.randomUUID().toString());
        }

        // Update status if both signed
        if (contract.getBuyerSignedAt() != null && contract.getSupplierSignedAt() != null) {
            contract.setStatus(Contract.ContractStatus.SIGNED);
        } else {
            contract.setStatus(Contract.ContractStatus.PENDING_SIGNATURES);
        }

        return contractRepository.save(contract);
    }

    @Transactional
    public Contract activateContract(UUID contractId) {
        Contract contract = getContractById(contractId);
        if (contract.getStatus() == Contract.ContractStatus.SIGNED) {
            contract.setStatus(Contract.ContractStatus.ACTIVE);
        } else {
            throw new RuntimeException("Contract must be signed before activation");
        }
        return contractRepository.save(contract);
    }

    public List<Contract> getContractsNearExpiration(UUID tenantId, int days) {
        java.time.LocalDate limitDate = java.time.LocalDate.now().plusDays(days);
        return getAllContracts(tenantId).stream()
                .filter(c -> c.getStatus() == Contract.ContractStatus.ACTIVE)
                .filter(c -> c.getEndDate() != null && !c.getEndDate().isAfter(limitDate))
                .collect(java.util.stream.Collectors.toList());
    }
}
