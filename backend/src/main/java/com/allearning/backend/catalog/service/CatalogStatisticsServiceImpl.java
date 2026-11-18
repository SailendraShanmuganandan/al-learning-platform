package com.allearning.backend.catalog.service;

import com.allearning.backend.catalog.dto.CatalogStatisticsDto;
import com.allearning.backend.catalog.dto.LowStockItemDto;
import com.allearning.backend.catalog.entity.ProductStatus;
import com.allearning.backend.catalog.entity.ProductType;
import com.allearning.backend.catalog.repository.NoteRepository;
import com.allearning.backend.catalog.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogStatisticsServiceImpl implements CatalogStatisticsService {

    private final ProductRepository productRepository;
    private final NoteRepository noteRepository;

    public CatalogStatisticsServiceImpl(ProductRepository productRepository, NoteRepository noteRepository) {
        this.productRepository = productRepository;
        this.noteRepository = noteRepository;
    }

    @Override
    public CatalogStatisticsDto getCatalogStatistics(int lowStockThreshold) {
        long publishedNotes = productRepository.countByStatusAndProductType(ProductStatus.PUBLISHED, ProductType.PHYSICAL_NOTE);
        long publishedVideos = productRepository.countByStatusAndProductType(ProductStatus.PUBLISHED, ProductType.VIDEO);
        long draftProducts = productRepository.countByStatus(ProductStatus.DRAFT);

        long lowStockCount = noteRepository.countLowStockNotes(lowStockThreshold);
        List<LowStockItemDto> lowStockItems = noteRepository.findLowStockItems(lowStockThreshold, PageRequest.of(0, 5));

        return new CatalogStatisticsDto(publishedNotes, publishedVideos, draftProducts, lowStockCount, lowStockItems);
    }
}
