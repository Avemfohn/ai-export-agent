package com.aiexportagent.scraping.upload;

import com.aiexportagent.common.exception.ApiException;
import com.aiexportagent.scraping.upload.dto.UploadSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Trade-fair exhibitor list upload.
 *
 * <p>Two steps by design: {@code /preview} reports what an import would do and
 * writes nothing, then {@code /upload} commits. The user sends the file to
 * both — no server-side upload storage, no session state between the calls, and
 * nothing to clean up if they abandon the preview.
 *
 * <p><strong>The commit path re-parses and re-validates from scratch.</strong>
 * Preview is a UX affordance, not a validation step; it must be impossible to
 * reach the write path with data that only preview approved.
 *
 * <p>{@code tenantId} is never accepted from the client — the {@code
 * scraping_jobs} row created by an import takes it from {@code TenantContext}.
 * The suppliers themselves are shared-pool rows and carry no tenant at all.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scraping-jobs")
public class SupplierUploadController {

    private final SupplierUploadService supplierUploadService;

    /** Parse and report. Reads the pool to count what is already there; writes nothing. */
    @PostMapping("/upload/preview")
    public UploadSummaryResponse preview(@RequestParam("file") MultipartFile file) {
        return supplierUploadService.preview(file.getOriginalFilename(), read(file));
    }

    /**
     * Parse and commit.
     *
     * <p>{@code expectedSha256} is optional and is an <em>integrity</em> check,
     * not a security boundary: it catches "you previewed one file and imported
     * another". It cannot be a security control, because the same user is
     * authorised to upload either file — sending different bytes here is not an
     * attack, it is just uploading a file.
     */
    @PostMapping("/upload")
    public UploadSummaryResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "expectedSha256", required = false) String expectedSha256) {

        byte[] bytes = read(file);
        if (expectedSha256 != null && !expectedSha256.isBlank()) {
            String actual = SupplierUploadService.sha256(bytes);
            if (!actual.equalsIgnoreCase(expectedSha256)) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "This file has changed since it was previewed. Preview it again before importing.");
            }
        }
        return supplierUploadService.commit(file.getOriginalFilename(), bytes);
    }

    private static byte[] read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No file was uploaded.");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Could not read the uploaded file: " + e.getMessage());
        }
    }
}
