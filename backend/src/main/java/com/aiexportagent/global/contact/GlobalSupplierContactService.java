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

    /**
     * Insert a batch of contacts into the shared pool.
     *
     * <p>Trade-fair upload only ever calls this for suppliers the same import
     * just created. Attaching a contact to a pre-existing supplier would still
     * be mutating a record other tenants rely on, which uploads are not allowed
     * to do.
     *
     * <p><strong>Needs its own {@code @Transactional}</strong> — the class is
     * {@code readOnly = true} and would otherwise discard these writes silently.
     */
    @Transactional
    public List<GlobalSupplierContact> createAll(List<GlobalSupplierContact> contacts) {
        return globalSupplierContactRepository.saveAll(contacts);
    }
}
