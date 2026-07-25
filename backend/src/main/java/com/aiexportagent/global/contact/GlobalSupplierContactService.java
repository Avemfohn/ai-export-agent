package com.aiexportagent.global.contact;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * No tenant filtering here on purpose — global_supplier_contacts is part of
 * the shared pool, read cross-tenant by design (see CLAUDE.md).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlobalSupplierContactService {

    private final GlobalSupplierContactRepository globalSupplierContactRepository;

    public List<GlobalSupplierContact> findByGlobalSupplierId(UUID globalSupplierId) {
        return globalSupplierContactRepository.findByGlobalSupplierId(globalSupplierId);
    }
}
