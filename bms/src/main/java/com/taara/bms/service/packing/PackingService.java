package com.taara.bms.service.packing;

import com.taara.bms.dto.common.PieceAvailabilityResponse;
import com.taara.bms.dto.packing.PackingCreateRequest;
import com.taara.bms.dto.packing.PackingDashboardResponse;
import com.taara.bms.dto.packing.PackingResponse;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.entity.packing.PackingEntry;
import com.taara.bms.service.common.AutoIdSequence;
import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.PackingStockType;
import com.taara.bms.exception.BusinessValidationException;
import com.taara.bms.repo.packing.PackingEntryRepository;
import com.taara.bms.service.common.AutoIdService;
import com.taara.bms.service.common.LookupService;
import com.taara.bms.service.inhouse.InHouseService;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PackingService {

    private static final Logger log = LoggerFactory.getLogger(PackingService.class);

    private final PackingEntryRepository packingEntryRepository;
    private final InHouseService inHouseService;
    private final LookupService lookupService;
    private final AutoIdService autoIdService;

    public PackingService(
            PackingEntryRepository packingEntryRepository,
            InHouseService inHouseService,
            LookupService lookupService,
            AutoIdService autoIdService
    ) {
        this.packingEntryRepository = packingEntryRepository;
        this.inHouseService = inHouseService;
        this.lookupService = lookupService;
        this.autoIdService = autoIdService;
    }

    @Transactional(readOnly = true)
    public PackingDashboardResponse getDashboard(
            String styleAutoId,
            GarmentSize size,
            PackingStockType stockType,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted
    ) {
        List<PackingEntry> entries = packingEntryRepository.findAll(entrySpec(styleAutoId, size, stockType, fromDate, toDate, includeDeleted));
        int totalPacked = entries.stream().mapToInt(PackingEntry::getCorrectlyPackedPieces).sum();
        int totalDefective = entries.stream().mapToInt(PackingEntry::getDefectivePieces).sum();
        
        List<com.taara.bms.dto.inhouse.ReadyToStitchBreakdownResponse> plainBreakdown = inHouseService.getStitchedPlainBreakdown();
        List<com.taara.bms.dto.inhouse.ReadyToStitchBreakdownResponse> printedBreakdown = inHouseService.getPrintedBreakdown();
        
        int totalPlain = plainBreakdown.stream().mapToInt(com.taara.bms.dto.inhouse.ReadyToStitchBreakdownResponse::totalPieces).sum();
        int totalPrinted = printedBreakdown.stream().mapToInt(com.taara.bms.dto.inhouse.ReadyToStitchBreakdownResponse::totalPieces).sum();

        return new PackingDashboardResponse(totalPacked, totalDefective, totalPlain, totalPrinted, plainBreakdown, printedBreakdown);
    }

    @Transactional(readOnly = true)
    public PieceAvailabilityResponse getAvailablePieces(String styleAutoId, GarmentSize size, PackingStockType stockType) {
        Style style = lookupService.getActiveStyleByAutoId(styleAutoId);
        return new PieceAvailabilityResponse(style.getAutoId(), size, availablePieces(style, size, stockType));
    }

    @Transactional(readOnly = true)
    public Page<PackingResponse> getEntries(
            String styleAutoId,
            GarmentSize size,
            PackingStockType stockType,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted,
            Pageable pageable
    ) {
        return packingEntryRepository.findAll(entrySpec(styleAutoId, size, stockType, fromDate, toDate, includeDeleted), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public PackingResponse createEntry(PackingCreateRequest request) {
        Style style = lookupService.getActiveStyleByAutoId(request.styleAutoId());
        int available = availablePieces(style, request.size(), request.stockType());
        int totalConsumed = request.correctlyPackedPieces() + request.defectivePieces();
        if (totalConsumed <= 0) {
            throw new BusinessValidationException("PACKING_CONSUMPTION_REQUIRED", "At least one packed or defective piece is required");
        }
        if (totalConsumed > available) {
            throw new BusinessValidationException(
                    "PACKING_EXCEEDS_AVAILABLE",
                    "Packing consumption exceeds the available stock for the selected source",
                    Map.of("availablePieces", available, "requestedPieces", totalConsumed)
            );
        }

        PackingEntry entry = new PackingEntry();
        entry.setAutoId(autoIdService.next(AutoIdSequence.PACKING));
        entry.setPackingDate(request.packingDate());
        entry.setStyle(style);
        entry.setSize(request.size());
        entry.setStockType(request.stockType());
        entry.setCorrectlyPackedPieces(request.correctlyPackedPieces());
        entry.setDefectivePieces(request.defectivePieces());
        return toResponse(packingEntryRepository.save(entry));
    }

    @Transactional
    public PackingResponse updateEntry(String packingAutoId, com.taara.bms.dto.packing.PackingUpdateRequest request) {
        PackingEntry entry = lookupService.getActivePackingEntryByAutoId(packingAutoId);
        
        Style style = lookupService.getActiveStyleByAutoId(request.styleAutoId());
        int available = availablePieces(style, request.size(), request.stockType());
        
        if (entry.getStyle().getId().equals(style.getId()) 
                && entry.getSize() == request.size() 
                && entry.getStockType() == request.stockType()) {
            available += entry.getCorrectlyPackedPieces() + entry.getDefectivePieces();
        }

        int totalConsumed = request.correctlyPackedPieces() + request.defectivePieces();
        if (totalConsumed <= 0) {
            throw new BusinessValidationException("PACKING_CONSUMPTION_REQUIRED", "At least one packed or defective piece is required");
        }
        if (totalConsumed > available) {
            throw new BusinessValidationException(
                    "PACKING_EXCEEDS_AVAILABLE",
                    "Packing consumption exceeds the available stock for the selected source",
                    Map.of("availablePieces", available, "requestedPieces", totalConsumed)
            );
        }

        entry.setPackingDate(request.packingDate());
        entry.setStyle(style);
        entry.setSize(request.size());
        entry.setStockType(request.stockType());
        entry.setCorrectlyPackedPieces(request.correctlyPackedPieces());
        entry.setDefectivePieces(request.defectivePieces());
        return toResponse(packingEntryRepository.save(entry));
    }

    @Transactional
    public void deleteEntry(String autoId) {
        PackingEntry entry = lookupService.getActivePackingEntryByAutoId(autoId);
        entry.setDeleted(true);
        packingEntryRepository.save(entry);
    }

    private int availablePieces(Style style, GarmentSize size, PackingStockType stockType) {
        return switch (stockType) {
            case PLAIN -> inHouseService.calculateStitchedPlainAvailable(style.getId(), size);
            case PRINTED -> inHouseService.calculatePrintedAvailable(style.getId(), size);
        };
    }

    private PackingResponse toResponse(PackingEntry entry) {
        return new PackingResponse(
                entry.getId(),
                entry.getAutoId(),
                entry.getPackingDate(),
                new com.taara.bms.dto.common.StyleRefResponse(entry.getStyle().getId(), entry.getStyle().getAutoId(), entry.getStyle().getStyleName()),
                entry.getSize(),
                entry.getStockType(),
                entry.getCorrectlyPackedPieces(),
                entry.getDefectivePieces(),
                entry.getCorrectlyPackedPieces() + entry.getDefectivePieces(),
                entry.isDeleted(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private Specification<PackingEntry> entrySpec(
            String styleAutoId,
            GarmentSize size,
            PackingStockType stockType,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!includeDeleted) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
            }
            if (styleAutoId != null && !styleAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("style").get("autoId")), styleAutoId.trim().toLowerCase()));
            }
            if (size != null) {
                predicates.add(cb.equal(root.get("size"), size));
            }
            if (stockType != null) {
                predicates.add(cb.equal(root.get("stockType"), stockType));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("packingDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("packingDate"), toDate));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
