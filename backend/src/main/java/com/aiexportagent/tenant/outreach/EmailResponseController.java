package com.aiexportagent.tenant.outreach;

import com.aiexportagent.tenant.outreach.dto.EmailResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * tenantId is never accepted from the client — EmailResponseService pulls it
 * from TenantContext.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email-responses")
public class EmailResponseController {

    private final EmailResponseService emailResponseService;

    @GetMapping
    public List<EmailResponseDto> list() {
        return emailResponseService.listForCurrentTenant().stream()
                .map(EmailResponseDto::from)
                .toList();
    }
}
