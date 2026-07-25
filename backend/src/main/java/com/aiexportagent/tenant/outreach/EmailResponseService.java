package com.aiexportagent.tenant.outreach;

import com.aiexportagent.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailResponseService {

    private final EmailResponseRepository emailResponseRepository;

    public List<EmailResponse> listForCurrentTenant() {
        return emailResponseRepository.findByTenantId(TenantContext.get());
    }
}
