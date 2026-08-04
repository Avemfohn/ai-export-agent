package com.aiexportagent.global.supplier;

import com.aiexportagent.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    /**
     * Which of these domains are already pooled. Used by trade-fair upload to
     * resolve a whole chunk in one query instead of a point lookup per row —
     * a 5,000-row file would otherwise issue 5,000 selects.
     */
    public Set<String> findExistingDomains(Collection<String> domains) {
        if (domains.isEmpty()) return Set.of();
        return globalSupplierRepository.findByDomainIn(domains).stream()
                .map(GlobalSupplier::getDomain)
                .collect(Collectors.toSet());
    }

    /**
     * Insert a batch of new suppliers into the shared pool.
     *
     * <p><strong>Add-only.</strong> Callers must have already excluded domains
     * that exist; this method never updates an existing row. One tenant's
     * upload must not be able to modify a company record another tenant's
     * scoring depends on — see CLAUDE.md "Master Pool Architecture". That is a
     * security boundary, not just data hygiene: it caps the blast radius of a
     * malicious file to domains nobody has pooled yet.
     *
     * <p><strong>Needs its own {@code @Transactional}</strong> — the class is
     * {@code readOnly = true}, which sets {@code FlushMode.MANUAL} and would
     * silently discard these writes. The same trap that swallowed the Phase 0
     * requeue write.
     *
     * <p>Takes a whole chunk deliberately. The transaction (and therefore the
     * JDBC batch) has to span many rows to be worth anything, and the boundary
     * only applies because the caller is a different bean — self-invocation
     * would bypass Spring's proxy entirely.
     */
    @Transactional
    public List<GlobalSupplier> createAll(List<GlobalSupplier> suppliers) {
        return globalSupplierRepository.saveAll(suppliers);
    }
}
