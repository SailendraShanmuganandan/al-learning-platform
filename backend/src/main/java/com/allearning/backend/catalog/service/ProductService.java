package com.allearning.backend.catalog.service;

import com.allearning.backend.academic.service.ChapterValidationService;
import com.allearning.backend.academic.service.ChapterValidationService.PublicChapterSummary;
import com.allearning.backend.catalog.dto.*;
import com.allearning.backend.catalog.entity.*;
import com.allearning.backend.catalog.repository.NoteRepository;
import com.allearning.backend.catalog.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import com.allearning.backend.integration.video.SignedEmbedResult;
import com.allearning.backend.integration.video.VideoPlaybackProvider;
import com.allearning.backend.integration.video.VideoProviderInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Transactional
public class ProductService implements CatalogService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private final ProductRepository productRepository;
    private final NoteRepository noteRepository;
    private final ChapterValidationService chapterValidationService;
    private final VideoPlaybackProvider videoPlaybackProvider;
    private final VideoAttachmentStorageService videoAttachmentStorageService;

    public ProductService(ProductRepository productRepository,
                          NoteRepository noteRepository,
                          ChapterValidationService chapterValidationService,
                          VideoPlaybackProvider videoPlaybackProvider,
                          VideoAttachmentStorageService videoAttachmentStorageService) {
        this.productRepository = productRepository;
        this.noteRepository = noteRepository;
        this.chapterValidationService = chapterValidationService;
        this.videoPlaybackProvider = videoPlaybackProvider;
        this.videoAttachmentStorageService = videoAttachmentStorageService;
    }

    private void validateUuid(String value) {
        if (value == null || !UUID_PATTERN.matcher(value.trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bunny Video ID must be a valid UUID/GUID");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> listAdminProducts(
            ProductType type,
            ProductStatus status,
            String search,
            Pageable pageable) {

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null) {
                predicates.add(cb.equal(root.get("productType"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable).map(ProductSummaryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getAdminProduct(Long productId) {
        Product product = findProductOrThrow(productId);
        return ProductDetailResponse.from(product);
    }

    @Override
    public ProductDetailResponse createProduct(ProductCreateRequest request) {
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be non-negative");
        }

        Product product = new Product();
        product.setName(request.name().trim());
        product.setSlug(generateUniqueSlug(request.name()));
        product.setDescription(request.description());
        product.setProductType(request.productType());
        product.setPrice(request.price());
        product.setStatus(ProductStatus.DRAFT);
        product.setThumbnailUrl(request.thumbnailUrl());

        // Validate type-specific constraints & build subtype
        if (request.productType() == ProductType.PHYSICAL_NOTE) {
            if (request.providerVideoId() != null || request.durationSeconds() != null || request.videoDescription() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Video fields (providerVideoId, durationSeconds, videoDescription) are not allowed for PHYSICAL_NOTE products");
            }
            if (request.pageCount() == null || request.pageCount() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Page count is required and must be greater than 0 for PHYSICAL_NOTE");
            }
            int stock = request.stockQuantity() != null ? request.stockQuantity() : 0;
            if (stock < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock quantity cannot be negative");
            }

            Note note = new Note(product, request.pageCount(), stock);
            product.setNote(note);
            product.setVideo(null);

        } else if (request.productType() == ProductType.VIDEO) {
            if (request.pageCount() != null || request.stockQuantity() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Note fields (pageCount, stockQuantity) are not allowed for VIDEO products");
            }

            String providerVideoId = request.providerVideoId() != null && !request.providerVideoId().isBlank()
                    ? request.providerVideoId().trim()
                    : null;
            Integer duration = request.durationSeconds();

            if (providerVideoId != null) {
                validateUuid(providerVideoId);
                Optional<VideoProviderInfo> infoOpt = videoPlaybackProvider.getVideoInfo(providerVideoId);
                if (infoOpt.isPresent()) {
                    VideoProviderInfo info = infoOpt.get();
                    if (!info.exists()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VIDEO_NOT_FOUND_IN_PROVIDER");
                    }
                    if ((duration == null || duration <= 0) && info.durationSeconds() != null && info.durationSeconds() > 0) {
                        duration = info.durationSeconds();
                    }
                }
            }

            Video video = new Video(product, providerVideoId, duration, request.videoDescription());
            product.setVideo(video);
            product.setNote(null);
        }

        // Validate linked chapters if provided
        if (request.chapterIds() != null && !request.chapterIds().isEmpty()) {
            validateChaptersExist(request.chapterIds());
            product.setChapterIds(new HashSet<>(request.chapterIds()));
        }

        Product saved = productRepository.save(product);
        log.info("Created product '{}' (id={}, type={}, slug={})", saved.getName(), saved.getProductId(), saved.getProductType(), saved.getSlug());
        return ProductDetailResponse.from(saved);
    }

    @Override
    public ProductDetailResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = findProductOrThrow(productId);

        // FR-PROD-01: Product type can never be changed
        if (request.productType() != null && request.productType() != product.getProductType()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product type cannot be changed");
        }

        // Validate common fields
        if (request.name() != null) {
            if (request.name().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name cannot be blank");
            }
            // FR-PROD-03: The slug stays stable when name is edited! Do not regenerate.
            product.setName(request.name().trim());
        }

        if (request.description() != null) {
            product.setDescription(request.description());
        }

        if (request.price() != null) {
            if (request.price().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be non-negative");
            }
            product.setPrice(request.price());
        }

        if (request.thumbnailUrl() != null) {
            product.setThumbnailUrl(request.thumbnailUrl().trim().isEmpty() ? null : request.thumbnailUrl().trim());
        }

        // Validate type-specific updates and reject mismatched fields
        if (product.getProductType() == ProductType.PHYSICAL_NOTE) {
            if (request.providerVideoId() != null || request.durationSeconds() != null || request.videoDescription() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Video fields (providerVideoId, durationSeconds, videoDescription) are not allowed for PHYSICAL_NOTE products");
            }
            Note note = product.getNote();
            if (note == null) {
                note = new Note();
                note.setProduct(product);
                product.setNote(note);
            }
            if (request.pageCount() != null) {
                if (request.pageCount() <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page count must be greater than 0");
                }
                note.setPageCount(request.pageCount());
            }
            if (request.stockQuantity() != null) {
                if (request.stockQuantity() < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock quantity cannot be negative");
                }
                note.setStockQuantity(request.stockQuantity());
            }

        } else if (product.getProductType() == ProductType.VIDEO) {
            if (request.pageCount() != null || request.stockQuantity() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Note fields (pageCount, stockQuantity) are not allowed for VIDEO products");
            }
            Video video = product.getVideo();
            if (video == null) {
                video = new Video();
                video.setProduct(product);
                product.setVideo(video);
            }
            if (request.providerVideoId() != null) {
                String providerVideoId = request.providerVideoId().trim();
                if (providerVideoId.isEmpty()) {
                    video.setProviderVideoId(null);
                } else {
                    validateUuid(providerVideoId);
                    video.setProviderVideoId(providerVideoId);
                    Optional<VideoProviderInfo> infoOpt = videoPlaybackProvider.getVideoInfo(providerVideoId);
                    if (infoOpt.isPresent()) {
                        VideoProviderInfo info = infoOpt.get();
                        if (!info.exists()) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VIDEO_NOT_FOUND_IN_PROVIDER");
                        }
                        if ((request.durationSeconds() == null || request.durationSeconds() <= 0)
                                && (video.getDurationSeconds() == null || video.getDurationSeconds() <= 0)
                                && info.durationSeconds() != null && info.durationSeconds() > 0) {
                            video.setDurationSeconds(info.durationSeconds());
                        }
                    }
                }
            }
            if (request.durationSeconds() != null) {
                if (request.durationSeconds() <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be greater than 0");
                }
                video.setDurationSeconds(request.durationSeconds());
            }
            if (request.videoDescription() != null) {
                video.setDescription(request.videoDescription());
            }
        }

        // FR-PROD-06: Editing replaces the link set
        if (request.chapterIds() != null) {
            validateChaptersExist(request.chapterIds());
            product.getChapterIds().clear();
            product.getChapterIds().addAll(request.chapterIds());
        }

        Product saved = productRepository.save(product);
        log.info("Updated product id={} (name={})", saved.getProductId(), saved.getName());
        return ProductDetailResponse.from(saved);
    }

    @Override
    public ProductDetailResponse updateProductStatus(Long productId, ProductStatus newStatus) {
        Product product = findProductOrThrow(productId);
        ProductStatus currentStatus = product.getStatus();

        if (currentStatus == newStatus) {
            // No-op or 409: prompt says "Status transitions allowed: DRAFT -> PUBLISHED, PUBLISHED -> ARCHIVED,
            // DRAFT -> ARCHIVED, ARCHIVED -> DRAFT. Any other transition returns 409."
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Product is already in " + currentStatus + " status");
        }

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        // FR-PROD-08: Publishing validation
        if (newStatus == ProductStatus.PUBLISHED) {
            validateForPublishing(product);
        }

        product.setStatus(newStatus);
        Product saved = productRepository.save(product);
        log.info("Transitioned product id={} status from {} to {}", productId, currentStatus, newStatus);
        return ProductDetailResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductDetailResponse> findById(Long productId) {
        return productRepository.findById(productId).map(ProductDetailResponse::from);
    }

    // -------------------------------------------------------------------------
    // Public browsing (no auth required)
    // -------------------------------------------------------------------------

    private static final Set<String> VALID_SORTS = Set.of("newest", "price_asc", "price_desc");

    @Override
    @Transactional(readOnly = true)
    public Page<PublicProductSummaryResponse> listPublicProducts(
            ProductType type,
            Long subjectId,
            Long chapterId,
            String q,
            String sort,
            Pageable pageable) {

        // Validate sort
        String resolvedSort = (sort == null || sort.isBlank()) ? "newest" : sort.trim().toLowerCase(Locale.ROOT);
        if (!VALID_SORTS.contains(resolvedSort)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid sort value '" + sort + "'. Allowed: newest, price_asc, price_desc");
        }

        // Resolve subjectId → chapterIds (via academic module, no repo access)
        Set<Long> subjectChapterIds = null;
        if (subjectId != null) {
            List<Long> ids = chapterValidationService.getPublicChapterIdsForSubject(subjectId);
            subjectChapterIds = new HashSet<>(ids);
        }

        final Set<Long> filterChapterIds = subjectChapterIds;

        Specification<Product> spec = buildPublicSpec(type, filterChapterIds, chapterId, q);

        // Apply sort on top of the spec-driven page
        Pageable sortedPageable = applySortToPageable(pageable, resolvedSort);

        return productRepository.findAll(spec, sortedPageable).map(PublicProductSummaryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicProductDetailResponse getPublicProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        List<PublicChapterSummary> publicChapters = resolvePublicChapters(product.getChapterIds());
        return PublicProductDetailResponse.from(product, publicChapters);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicProductSummaryResponse> getPublicProductsForChapter(Long chapterId) {
        // 404 if chapter is not publicly visible
        if (!chapterValidationService.isPubliclyVisible(chapterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found");
        }

        List<Product> products = productRepository.findByChapterIdInAndStatus(
                List.of(chapterId), ProductStatus.PUBLISHED);

        return products.stream()
                .map(PublicProductSummaryResponse::from)
                .toList();
    }


    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void validateChaptersExist(Collection<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return;
        }
        List<Long> missing = chapterValidationService.findMissingChapterIds(chapterIds);
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown chapter ID(s): " + missing);
        }
    }

    /**
     * FR-PROD-08: Validates that product has all required fields before moving to PUBLISHED:
     * - Name non-blank
     * - Price non-null and >= 0
     * - Linked to at least one chapter
     * - Note: page_count set and > 0
     * - Video: video_url non-blank and duration_seconds > 0
     * Returns 400 listing all missing items if any fail.
     */
    private void validateForPublishing(Product product) {
        List<String> missing = new ArrayList<>();

        if (product.getName() == null || product.getName().trim().isEmpty()) {
            missing.add("Product name is required");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            missing.add("Valid non-negative price is required");
        }
        if (product.getChapterIds() == null || product.getChapterIds().isEmpty()) {
            missing.add("Product must be linked to at least one chapter");
        }

        if (product.getProductType() == ProductType.PHYSICAL_NOTE) {
            Note note = product.getNote();
            if (note == null || note.getPageCount() == null || note.getPageCount() <= 0) {
                missing.add("Note page count must be set and greater than 0");
            }
        } else if (product.getProductType() == ProductType.VIDEO) {
            Video video = product.getVideo();
            if (video == null || video.getProviderVideoId() == null || video.getProviderVideoId().trim().isEmpty()) {
                missing.add("Bunny Video ID is required");
            }
            if (video == null || video.getDurationSeconds() == null || video.getDurationSeconds() <= 0) {
                missing.add("Video duration must be set and greater than 0");
            }
            if (video != null && video.getProviderVideoId() != null && !video.getProviderVideoId().trim().isEmpty()) {
                Optional<VideoProviderInfo> infoOpt = videoPlaybackProvider.getVideoInfo(video.getProviderVideoId().trim());
                if (infoOpt.isPresent()) {
                    VideoProviderInfo info = infoOpt.get();
                    if (!info.exists()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VIDEO_NOT_FOUND_IN_PROVIDER");
                    }
                    if (!info.encodingFinished()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VIDEO_NOT_READY");
                    }
                }
            }
        }

        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot publish product. Missing requirements: " + String.join(", ", missing));
        }
    }

    /**
     * FR-PROD-03: Generates unique lowercase hyphenated slug from name, appending numeric suffix on conflict.
     */
    private String generateUniqueSlug(String name) {
        String baseSlug = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
        if (baseSlug.isEmpty()) {
            baseSlug = "product";
        }

        String slug = baseSlug;
        int suffix = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }
        return slug;
    }

    /**
     * Builds a Specification for public product browsing.
     * Always restricts to PUBLISHED status.
     */
    private Specification<Product> buildPublicSpec(
            ProductType type,
            Set<Long> subjectChapterIds,
            Long chapterId,
            String q) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always public = PUBLISHED only
            predicates.add(cb.equal(root.get("status"), ProductStatus.PUBLISHED));

            if (type != null) {
                predicates.add(cb.equal(root.get("productType"), type));
            }

            // Chapter filtering: if both subjectId and chapterId provided, intersect
            if (chapterId != null && subjectChapterIds != null) {
                // Intersection: must be in subject's chapters AND be the specified chapter
                if (!subjectChapterIds.contains(chapterId)) {
                    // Impossible condition — return nothing
                    predicates.add(cb.disjunction());
                } else {
                    jakarta.persistence.criteria.Join<Object, Object> join =
                            root.join("chapterIds");
                    predicates.add(cb.equal(join, chapterId));
                }
            } else if (chapterId != null) {
                jakarta.persistence.criteria.Join<Object, Object> join = root.join("chapterIds");
                predicates.add(cb.equal(join, chapterId));
            } else if (subjectChapterIds != null) {
                if (subjectChapterIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    jakarta.persistence.criteria.Join<Object, Object> join = root.join("chapterIds");
                    predicates.add(join.in(subjectChapterIds));
                }
            }

            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            if (query != null) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Converts a sort string to a Pageable with the appropriate Spring Sort.
     */
    private Pageable applySortToPageable(Pageable pageable, String sort) {
        Sort springSort = switch (sort) {
            case "price_asc" -> Sort.by(Sort.Order.asc("price"));
            case "price_desc" -> Sort.by(Sort.Order.desc("price"));
            default -> Sort.by(Sort.Order.desc("createdAt")); // newest
        };
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), springSort);
    }

    /**
     * Fetches public-visible chapter summaries for a product's chapter IDs.
     * Silently omits chapters that are INACTIVE or belong to INACTIVE subjects.
     */
    private List<PublicChapterSummary> resolvePublicChapters(Set<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return List.of();
        }
        return chapterValidationService.getPublicChapterSummaries(chapterIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductPurchaseFacts> getProductPurchaseFacts(Long productId) {
        if (productId == null) {
            return Optional.empty();
        }
        return productRepository.findById(productId).map(product -> {
            Integer stockQuantity = null;
            if (product.getProductType() == ProductType.PHYSICAL_NOTE && product.getNote() != null) {
                stockQuantity = product.getNote().getStockQuantity();
            }
            return new ProductPurchaseFacts(
                    product.getProductId(),
                    product.getName(),
                    product.getSlug(),
                    product.getProductType(),
                    product.getStatus(),
                    product.getPrice(),
                    product.getThumbnailUrl(),
                    stockQuantity
            );
        });
    }

    @Override
    public boolean reserveStock(Long productId, int quantity) {
        if (productId == null || quantity <= 0) {
            return false;
        }
        int updated = noteRepository.decrementStockIfAvailable(productId, quantity);
        return updated > 0;
    }

    @Override
    public void releaseStock(Long productId, int quantity) {
        if (productId == null || quantity <= 0) {
            return;
        }
        noteRepository.incrementStock(productId, quantity);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPlaybackResponse getAdminPlayback(Long productId) {
        Product product = findProductOrThrow(productId);
        if (product.getProductType() != ProductType.VIDEO || product.getVideo() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is not a video");
        }
        String providerVideoId = product.getVideo().getProviderVideoId();
        if (providerVideoId == null || providerVideoId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product does not have a Bunny Video ID configured");
        }
        SignedEmbedResult signed = videoPlaybackProvider.signEmbedUrl(providerVideoId, 120);
        return new AdminPlaybackResponse(signed.embedUrl(), signed.expiresAt());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VideoProductFacts> getVideoProductFacts(Long productId) {
        if (productId == null) {
            return Optional.empty();
        }
        return productRepository.findById(productId)
                .filter(p -> p.getProductType() == ProductType.VIDEO && p.getVideo() != null)
                .map(p -> new VideoProductFacts(
                        p.getProductId(),
                        p.getName(),
                        p.getSlug(),
                        p.getThumbnailUrl(),
                        p.getVideo().getDurationSeconds(),
                        p.getVideo().getDescription(),
                        p.getStatus(),
                        p.getVideo().getProviderVideoId(),
                        p.getVideo().getPdfFilePath() != null && !p.getVideo().getPdfFilePath().isBlank(),
                        p.getVideo().getPdfOriginalFilename(),
                        p.getVideo().getPdfFileSize()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, VideoProductFacts> getVideoProductFacts(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, VideoProductFacts> map = new HashMap<>();
        for (Product p : products) {
            if (p.getProductType() == ProductType.VIDEO && p.getVideo() != null) {
                map.put(p.getProductId(), new VideoProductFacts(
                        p.getProductId(),
                        p.getName(),
                        p.getSlug(),
                        p.getThumbnailUrl(),
                        p.getVideo().getDurationSeconds(),
                        p.getVideo().getDescription(),
                        p.getStatus(),
                        p.getVideo().getProviderVideoId(),
                        p.getVideo().getPdfFilePath() != null && !p.getVideo().getPdfFilePath().isBlank(),
                        p.getVideo().getPdfOriginalFilename(),
                        p.getVideo().getPdfFileSize()
                ));
            }
        }
        return map;
    }

    @Override
    public ProductDetailResponse uploadVideoAttachmentPdf(Long productId, org.springframework.web.multipart.MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId));

        if (product.getProductType() != ProductType.VIDEO || product.getVideo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF attachments can only be added to VIDEO products");
        }

        Video video = product.getVideo();
        VideoAttachmentStorageService.StoredPdfInfo stored = videoAttachmentStorageService.storePdf(
                productId, file, video.getPdfFilePath()
        );

        video.setPdfFilePath(stored.relativePath());
        video.setPdfOriginalFilename(stored.originalFilename());
        video.setPdfFileSize(stored.fileSize());

        productRepository.save(product);
        log.info("Attached PDF '{}' ({} bytes) to video product {}", stored.originalFilename(), stored.fileSize(), productId);
        return ProductDetailResponse.from(product);
    }

    @Override
    public ProductDetailResponse deleteVideoAttachmentPdf(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId));

        if (product.getProductType() != ProductType.VIDEO || product.getVideo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product is not a video");
        }

        Video video = product.getVideo();
        if (video.getPdfFilePath() != null) {
            videoAttachmentStorageService.deleteFile(video.getPdfFilePath());
            video.setPdfFilePath(null);
            video.setPdfOriginalFilename(null);
            video.setPdfFileSize(null);
            productRepository.save(product);
            log.info("Removed PDF attachment from video product {}", productId);
        }

        return ProductDetailResponse.from(product);
    }

    @Override
    @Transactional(readOnly = true)
    public VideoPdfResource getVideoAttachmentPdfResource(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId));

        if (product.getProductType() != ProductType.VIDEO || product.getVideo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product is not a video");
        }

        Video video = product.getVideo();
        if (video.getPdfFilePath() == null || video.getPdfFilePath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No PDF attachment found for this video");
        }

        org.springframework.core.io.Resource resource = videoAttachmentStorageService.loadAsResource(video.getPdfFilePath());
        String filename = video.getPdfOriginalFilename() != null ? video.getPdfOriginalFilename() : "lecture_notes.pdf";
        long size = video.getPdfFileSize() != null ? video.getPdfFileSize() : 0L;

        return new VideoPdfResource(resource, filename, size);
    }
}
