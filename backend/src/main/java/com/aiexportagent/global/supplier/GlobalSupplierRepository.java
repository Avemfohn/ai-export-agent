package com.aiexportagent.global.supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GlobalSupplierRepository extends JpaRepository<GlobalSupplier, UUID> {

    Optional<GlobalSupplier> findByDomain(String domain);
}
