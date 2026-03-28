package com.taara.bms.service.packing;

import com.taara.bms.dto.common.PieceAvailabilityResponse;
import com.taara.bms.dto.packing.PackingCreateRequest;
import com.taara.bms.dto.packing.PackingDashboardResponse;
import com.taara.bms.dto.packing.PackingResponse;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.entity.packing.PackingEntry;
import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.PackingStockType;
import com.taara.bms.exception.BusinessValidationException;
import com.taara.bms.mapper.common.ReferenceMapper;
import com.taara.bms.repo.packing.PackingEntryRepository;
import com.taara.bms.service.common.AutoIdSequence;
import com.taara.bms.service.common.AutoIdService;
import com.taara.bms.service.common.LookupService;
import com.taara.bms.service.inhouse.InHouseService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PackingService {

    private static final Logger log = LoggerFactory.getLogger(PackingService.class);

    private final PackingEntryRepository packingEntryRepository;
    private final LookupService lookupService;
    private final AutoIdService autoIdService;
    private final ReferenceMapper referenceMapper;
    private final InHouseService inHouseService;

    public PackingService(
            PackingEntryRepository packingEntryRepository,
            LookupService lookupService,
            AutoIdService autoIdService,
            ReferenceMapper referenceMapper,
            InHouseService inHouseService
    ) {
        this.packingEntryRepository = packingEntryRepository;
        this.lookupService = lookupService;
        this.autoIdService = autoIdService;
        this.referenceMapper = referenceMapper;
        this.inHouseService = inHouseService;
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

    @Transactional(readOnly = true)
    public PieceAvailabilityResponse getAvailablePieces(String styleAutoId, GarmentSize size, PackingStockType stockType) {
        Style style = lookupService.getActiveStyleByAutoId(styleAutoId);
        return new PieceAvailabilityResponse(style.getAutoId(), size, availablePieces(style, size, stockType));
    }

    @Transactional
    public void deleteEntry(String packingAutoId) {
        PackingEntry entry = lookupService.getActivePackingEntryByAutoId(packingAutoId);
        entry.setDeleted(true);
        packingEntryRepository.save(entry);
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
        int packedPieces = entries.stream().mapToInt(PackingEntry::getCorrectlyPackedPieces).sum();
        int defectivePieces = entries.stream().mapToInt(PackingEntry::getDefectivePieces).sum();
        return new PackingDashboardResponse(packedPieces, defectivePieces);
    }

    private int availablePieces(Style style, GarmentSize size, PackingStockType stockType) {
        return stockType == PackingStockType.PLAIN
                ? inHouseService.calculateStitchedPlainAvailable(style.getId(), size)
                : inHouseService.calculatePrintedAvailable(style.getId(), size);
    }

    private PackingResponse toResponse(PackingEntry entry) {
        return new PackingResponse(
                entry.getId(),
                entry.getAutoId(),
                entry.getPackingDate(),
                referenceMapper.toStyleRef(entry.getStyle()),
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
