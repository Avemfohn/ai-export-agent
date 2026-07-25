package com.aiexportagent.global.contact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GlobalSupplierContactRepository extends JpaRepository<GlobalSupplierContact, UUID> {

    List<GlobalSupplierContact> findByGlobalSupplierId(UUID globalSupplierId);
}
