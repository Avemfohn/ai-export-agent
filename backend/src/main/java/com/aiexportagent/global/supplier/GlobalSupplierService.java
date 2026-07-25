package com.aiexportagent.global.supplier;

import com.aiexportagent.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * No tenant filtering here on purpose — global_suppliers is a shared pool
 * read cross-tenant by design (see CLAUDE.md).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlobalSupplierService {

    private final GlobalSupplierRepository globalSupplierRepository;

    public List<GlobalSupplier> findAll() {
        return globalSupplierRepository.findAll();
    }

    public GlobalSupplier getById(UUID id) {
        return globalSupplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Global supplier not found: " + id));
    }

    public Optional<GlobalSupplier> findByDomain(String domain) {
        return globalSupplierRepository.findByDomain(domain);
    }
}
