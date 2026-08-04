package com.aiexportagent.global.supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GlobalSupplierRepository extends JpaRepository<GlobalSupplier, UUID> {

    Optional<GlobalSupplier> findByDomain(String domain);

    /** Batched dedup lookup for trade-fair upload — one query per chunk. */
    List<GlobalSupplier> findByDomainIn(Collection<String> domains);
}
