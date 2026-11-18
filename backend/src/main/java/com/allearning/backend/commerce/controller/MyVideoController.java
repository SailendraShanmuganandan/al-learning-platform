package com.allearning.backend.commerce.controller;

import com.allearning.backend.auth.AuthService;
import com.allearning.backend.auth.User;
import com.allearning.backend.catalog.dto.VideoProductFacts;
import com.allearning.backend.catalog.service.CatalogService;
import com.allearning.backend.commerce.dto.MyVideoDetailResponse;
import com.allearning.backend.commerce.dto.MyVideoSummaryResponse;
import com.allearning.backend.commerce.service.VideoOwnershipService;
import com.allearning.backend.commerce.service.VideoOwnershipService.OwnedVideoPurchase;
import com.allearning.backend.integration.video.SignedEmbedResult;
import com.allearning.backend.integration.video.VideoPlaybackProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Controller for student video library and protected playback (FR-VID-01, FR-VID-05, FR-VID-06; BR-10).
 * Accessible exclusively to authenticated users with ROLE_STUDENT.
 */
@RestController
@RequestMapping("/api/my/videos")
public class MyVideoController {

    private static final Logger log = LoggerFactory.getLogger(MyVideoController.class);

    private final VideoOwnershipService videoOwnershipService;
    private final CatalogService catalogService;
    private final VideoPlaybackProvider videoPlaybackProvider;
    private final AuthService authService;

    public MyVideoController(
            VideoOwnershipService videoOwnershipService,
            CatalogService catalogService,
            VideoPlaybackProvider videoPlaybackProvider,
            AuthService authService
    ) {
        this.videoOwnershipService = videoOwnershipService;
        this.catalogService = catalogService;
        this.videoPlaybackProvider = videoPlaybackProvider;
        this.authService = authService;
    }

    /**
     * GET /api/my/videos
     * Returns the authenticated student's owned video courses, de-duplicated and sorted by purchased_at descending.
     * Includes ARCHIVED products (FR-VID-05, BR-10). Never exposes video IDs, tokens, or embed URLs.
     */
    @GetMapping
    public ResponseEntity<List<MyVideoSummaryResponse>> getMyVideos(Authentication authentication) {
        Long userId = extractUserId(authentication);
        List<OwnedVideoPurchase> purchases = videoOwnershipService.getOwnedVideos(userId);

        if (purchases.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Long> productIds = purchases.stream().map(OwnedVideoPurchase::productId).toList();
        Map<Long, VideoProductFacts> factsMap = catalogService.getVideoProductFacts(productIds);

        List<MyVideoSummaryResponse> result = new ArrayList<>();
        for (OwnedVideoPurchase purchase : purchases) {
            VideoProductFacts facts = factsMap.get(purchase.productId());
            if (facts != null) {
                result.add(new MyVideoSummaryResponse(
                        facts.productId(),
                        facts.slug(),
                        facts.name(),
                        facts.thumbnailUrl(),
                        facts.durationSeconds(),
                        facts.description(),
                        purchase.purchasedAt(),
                        facts.hasAttachmentPdf()
                ));
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/my/videos/{productId}
     * Returns video details along with a freshly signed embed player URL.
     * Enforces ownership, returns 403 VIDEO_NOT_OWNED if not owned, and sets Cache-Control: no-store.
     */
    @GetMapping("/{productId}")
    public ResponseEntity<MyVideoDetailResponse> getMyVideoPlayback(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        Long userId = extractUserId(authentication);

        // 1. Verify product exists and is a VIDEO
        VideoProductFacts facts = catalogService.getVideoProductFacts(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video product not found"));

        // 2. Check ownership first
        boolean owns = videoOwnershipService.userOwnsVideo(userId, productId);
        if (!owns) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "VIDEO_NOT_OWNED");
        }

        // 3. Ensure provider video ID is configured
        if (facts.providerVideoId() == null || facts.providerVideoId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "VIDEO_PLAYBACK_NOT_CONFIGURED");
        }

        // 4. Sign fresh embed URL (throws 503 if provider unconfigured)
        SignedEmbedResult signed = videoPlaybackProvider.signEmbedUrl(facts.providerVideoId(), 120);

        // 5. Determine purchase date
        OffsetDateTime purchasedAt = videoOwnershipService.getOwnedVideos(userId).stream()
                .filter(p -> p.productId().equals(productId))
                .map(OwnedVideoPurchase::purchasedAt)
                .findFirst()
                .orElse(OffsetDateTime.now());

        // Log userId and productId only (NEVER the signed URL or token key)
        log.info("Dispensing signed playback URL for student userId={}, productId={}", userId, productId);

        MyVideoDetailResponse response = new MyVideoDetailResponse(
                facts.productId(),
                facts.slug(),
                facts.name(),
                facts.thumbnailUrl(),
                facts.durationSeconds(),
                facts.description(),
                purchasedAt,
                signed.embedUrl(),
                signed.expiresAt(),
                facts.hasAttachmentPdf(),
                facts.attachmentPdfName(),
                facts.attachmentPdfSize()
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Cache-Control", "no-store")
                .body(response);
    }

    /**
     * GET /api/my/videos/{productId}/notes-pdf
     * Downloads the supplementary lecture notes PDF if the student owns the video.
     */
    @GetMapping("/{productId}/notes-pdf")
    public ResponseEntity<org.springframework.core.io.Resource> downloadMyVideoNotesPdf(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        Long userId = extractUserId(authentication);

        boolean owns = videoOwnershipService.userOwnsVideo(userId, productId);
        if (!owns) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "VIDEO_NOT_OWNED");
        }

        var res = catalogService.getVideoAttachmentPdfResource(productId);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + res.filename() + "\"")
                .body(res.resource());
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getUserId();
        }
        String email = authentication.getName();
        return authService.getMe(email).userId();
    }
}
