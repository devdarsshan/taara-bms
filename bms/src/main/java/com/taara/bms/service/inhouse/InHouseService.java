package com.taara.bms.service.inhouse;

import com.taara.bms.dto.inhouse.CuttingAvailabilityResponse;
import com.taara.bms.dto.inhouse.CuttingCreateRequest;
import com.taara.bms.dto.inhouse.CuttingResponse;
import com.taara.bms.dto.inhouse.CuttingRowRequest;
import com.taara.bms.dto.inhouse.CuttingUpdateRequest;
import com.taara.bms.dto.inhouse.ExistingStockCreateRequest;
import com.taara.bms.dto.inhouse.ExistingStockResponse;
import com.taara.bms.dto.inhouse.InHouseDashboardResponse;
import com.taara.bms.dto.inhouse.InHouseDeliveryResponse;
import com.taara.bms.dto.inhouse.InHouseSplitBatchRequest;
import com.taara.bms.dto.inhouse.InHouseSplitRequest;
import com.taara.bms.dto.inhouse.InHouseStockResponse;
import com.taara.bms.dto.inhouse.InHouseStockSplitResponse;
import com.taara.bms.dto.inhouse.StitchedStockResponse;
import com.taara.bms.entity.inhouse.CuttingEntry;
import com.taara.bms.entity.inhouse.CuttingEntryRow;
import com.taara.bms.entity.inhouse.InHouseDelivery;
import com.taara.bms.entity.inhouse.InHouseExistingStock;
import com.taara.bms.entity.inhouse.InHouseStockSplit;
import com.taara.bms.entity.masterdata.Dia;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.entity.packing.PackingEntry;
import com.taara.bms.entity.printing.PrintingDelivery;
import com.taara.bms.entity.printing.PrintingOrder;
import com.taara.bms.entity.stitching.StitchingDelivery;
import com.taara.bms.entity.stitching.StitchingOrder;
import com.taara.bms.entity.stitching.StitchingOrderRow;
import com.taara.bms.enums.CuttingStatus;
import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.PackingStockType;
import com.taara.bms.enums.SplitStatus;
import com.taara.bms.enums.StitchingOrderStatus;
import com.taara.bms.exception.BusinessValidationException;
import com.taara.bms.exception.DeleteConflictException;
import com.taara.bms.helper.BigDecimalUtils;
import com.taara.bms.mapper.common.ReferenceMapper;
import com.taara.bms.mapper.inhouse.InHouseMapper;
import com.taara.bms.repo.inhouse.CuttingEntryRepository;
import com.taara.bms.repo.inhouse.CuttingEntryRowRepository;
import com.taara.bms.repo.inhouse.InHouseDeliveryRepository;
import com.taara.bms.repo.inhouse.InHouseExistingStockRepository;
import com.taara.bms.repo.inhouse.InHouseStockSplitRepository;
import com.taara.bms.repo.packing.PackingEntryRepository;
import com.taara.bms.repo.printing.PrintingDeliveryRepository;
import com.taara.bms.repo.printing.PrintingOrderRepository;
import com.taara.bms.repo.stitching.StitchingDeliveryAllocationRepository;
import com.taara.bms.repo.stitching.StitchingDeliveryRepository;
import com.taara.bms.repo.stitching.StitchingOrderRepository;
import com.taara.bms.repo.stitching.StitchingOrderRowRepository;
import com.taara.bms.service.common.AutoIdSequence;
import com.taara.bms.service.common.AutoIdService;
import com.taara.bms.service.common.LookupService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InHouseService {

    private static final Logger log = LoggerFactory.getLogger(InHouseService.class);

    private final InHouseDeliveryRepository inHouseDeliveryRepository;
    private final InHouseStockSplitRepository inHouseStockSplitRepository;
    private final CuttingEntryRepository cuttingEntryRepository;
    private final CuttingEntryRowRepository cuttingEntryRowRepository;
    private final InHouseExistingStockRepository existingStockRepository;
    private final StitchingOrderRepository stitchingOrderRepository;
    private final StitchingOrderRowRepository stitchingOrderRowRepository;
    private final StitchingDeliveryRepository stitchingDeliveryRepository;
    private final StitchingDeliveryAllocationRepository stitchingDeliveryAllocationRepository;
    private final PrintingOrderRepository printingOrderRepository;
    private final PrintingDeliveryRepository printingDeliveryRepository;
    private final PackingEntryRepository packingEntryRepository;
    private final LookupService lookupService;
    private final AutoIdService autoIdService;
    private final InHouseMapper mapper;
    private final ReferenceMapper referenceMapper;

    public InHouseService(
            InHouseDeliveryRepository inHouseDeliveryRepository,
            InHouseStockSplitRepository inHouseStockSplitRepository,
            CuttingEntryRepository cuttingEntryRepository,
            CuttingEntryRowRepository cuttingEntryRowRepository,
            InHouseExistingStockRepository existingStockRepository,
            StitchingOrderRepository stitchingOrderRepository,
            StitchingOrderRowRepository stitchingOrderRowRepository,
            StitchingDeliveryRepository stitchingDeliveryRepository,
            StitchingDeliveryAllocationRepository stitchingDeliveryAllocationRepository,
            PrintingOrderRepository printingOrderRepository,
            PrintingDeliveryRepository printingDeliveryRepository,
            PackingEntryRepository packingEntryRepository,
            LookupService lookupService,
            AutoIdService autoIdService,
            InHouseMapper mapper,
            ReferenceMapper referenceMapper
    ) {
        this.inHouseDeliveryRepository = inHouseDeliveryRepository;
        this.inHouseStockSplitRepository = inHouseStockSplitRepository;
        this.cuttingEntryRepository = cuttingEntryRepository;
        this.cuttingEntryRowRepository = cuttingEntryRowRepository;
        this.existingStockRepository = existingStockRepository;
        this.stitchingOrderRepository = stitchingOrderRepository;
        this.stitchingOrderRowRepository = stitchingOrderRowRepository;
        this.stitchingDeliveryRepository = stitchingDeliveryRepository;
        this.stitchingDeliveryAllocationRepository = stitchingDeliveryAllocationRepository;
        this.printingOrderRepository = printingOrderRepository;
        this.printingDeliveryRepository = printingDeliveryRepository;
        this.packingEntryRepository = packingEntryRepository;
        this.lookupService = lookupService;
        this.autoIdService = autoIdService;
        this.mapper = mapper;
        this.referenceMapper = referenceMapper;
    }

    @Transactional(readOnly = true)
    public Page<InHouseDeliveryResponse> getDeliveries(
            String styleAutoId,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching in-house deliveries with styleAutoId='{}', fromDate={}, toDate={}, includeDeleted={}, pageable={}",
                styleAutoId, fromDate, toDate, includeDeleted, pageable);
        return inHouseDeliveryRepository.findAll(deliverySpec(styleAutoId, fromDate, toDate, includeDeleted), pageable)
                .map(delivery -> mapper.toDeliveryResponse(
                        delivery,
                        BigDecimalUtils.scale(inHouseStockSplitRepository.sumActiveQuantityByDelivery(delivery.getId()))
                ));
    }

    @Transactional(readOnly = true)
    public List<InHouseStockSplitResponse> getSplits(String deliveryAutoId) {
        log.info("Fetching splits for delivery '{}'", deliveryAutoId);
        InHouseDelivery delivery = lookupService.getActiveInHouseDeliveryByAutoId(deliveryAutoId);
        return inHouseStockSplitRepository.findByDelivery_IdAndIsDeletedFalse(delivery.getId()).stream()
                .map(split -> mapper.toSplitResponse(split, canDeleteSplit(split)))
                .sorted(Comparator.comparing(InHouseStockSplitResponse::createdAt))
                .toList();
    }

    @Transactional
    public List<InHouseStockSplitResponse> createSplits(String deliveryAutoId, InHouseSplitBatchRequest request) {
        log.info("Creating {} splits for delivery '{}'", request.splits().size(), deliveryAutoId);
        InHouseDelivery delivery = lookupService.getActiveInHouseDeliveryByAutoId(deliveryAutoId);
        BigDecimal currentAllocated = BigDecimalUtils.scale(inHouseStockSplitRepository.sumActiveQuantityByDelivery(delivery.getId()));
        BigDecimal requestedTotal = request.splits().stream()
                .map(InHouseSplitRequest::quantityKgs)
                .filter(Objects::nonNull)
                .map(BigDecimalUtils::scale)
                .reduce(BigDecimalUtils.ZERO, BigDecimal::add);
        BigDecimal totalQuantity = BigDecimalUtils.scale(delivery.getQuantityKgs());
        if (currentAllocated.add(requestedTotal).compareTo(totalQuantity) > 0) {
            throw new BusinessValidationException(
                    "SPLIT_EXCEEDS_DELIVERY",
                    "Split quantity exceeds the available delivery quantity",
                    Map.of(
                            "deliveryAutoId", delivery.getAutoId(),
                            "deliveryQuantityKgs", totalQuantity,
                            "alreadyAllocatedQuantityKgs", currentAllocated,
                            "requestedQuantityKgs", requestedTotal
                    )
            );
        }

        for (InHouseSplitRequest splitRequest : request.splits()) {
            Dia dia = lookupService.getActiveDiaByAutoId(splitRequest.diaAutoId());
            InHouseStockSplit split = new InHouseStockSplit();
            split.setAutoId(autoIdService.next(AutoIdSequence.INHOUSE_STOCK_SPLIT));
            split.setDelivery(delivery);
            split.setDia(dia);
            split.setStyle(delivery.getStyle());
            split.setQuantityKgs(BigDecimalUtils.scale(splitRequest.quantityKgs()));
            inHouseStockSplitRepository.save(split);
        }
        recalculateSplitStatus(delivery);
        return getSplits(deliveryAutoId);
    }

    @Transactional
    public void deleteSplit(String deliveryAutoId, String splitAutoId) {
        log.info("Deleting split '{}' from delivery '{}'", splitAutoId, deliveryAutoId);
        InHouseDelivery delivery = lookupService.getActiveInHouseDeliveryByAutoId(deliveryAutoId);
        InHouseStockSplit split = lookupService.getActiveInHouseStockSplitByAutoId(splitAutoId);
        if (!split.getDelivery().getId().equals(delivery.getId())) {
            throw new BusinessValidationException("SPLIT_DELIVERY_MISMATCH", "The selected split does not belong to this delivery");
        }
        if (!canDeleteSplit(split)) {
            throw new DeleteConflictException(
                    "SPLIT_HAS_DOWNSTREAM_USAGE",
                    "Split cannot be deleted because downstream cutting already depends on this stock",
                    Map.of("splitAutoId", split.getAutoId())
            );
        }
        split.setDeleted(true);
        inHouseStockSplitRepository.save(split);
        recalculateSplitStatus(delivery);
    }

    @Transactional(readOnly = true)
    public List<InHouseStockResponse> getStock(String diaAutoId, String styleAutoId) {
        log.info("Calculating in-house stock with diaAutoId='{}', styleAutoId='{}'", diaAutoId, styleAutoId);
        Map<FabricKey, BigDecimal> fabricByCombo = new LinkedHashMap<>();

        for (InHouseStockSplit split : inHouseStockSplitRepository.findAll()) {
            if (split.isDeleted() || !matchesDia(split.getDia(), diaAutoId) || !matchesStyle(split.getStyle(), styleAutoId)) {
                continue;
            }
            FabricKey key = new FabricKey(split.getDia(), split.getStyle());
            fabricByCombo.merge(key, BigDecimalUtils.scale(split.getQuantityKgs()), BigDecimal::add);
        }

        for (InHouseExistingStock added : existingStockRepository.findAll()) {
            if (added.isDeleted() || !matchesDia(added.getDia(), diaAutoId) || !matchesStyle(added.getStyle(), styleAutoId)) {
                continue;
            }
            FabricKey key = new FabricKey(added.getDia(), added.getStyle());
            fabricByCombo.merge(key, BigDecimalUtils.scale(added.getQuantityKgs()), BigDecimal::add);
        }

        for (CuttingEntry cuttingEntry : cuttingEntryRepository.findAll()) {
            if (cuttingEntry.isDeleted()) {
                continue;
            }
            for (CuttingEntryRow row : cuttingEntry.getRows()) {
                if (!matchesDia(row.getDia(), diaAutoId) || !matchesStyle(row.getStyle(), styleAutoId)) {
                    continue;
                }
                FabricKey key = new FabricKey(row.getDia(), row.getStyle());
                fabricByCombo.merge(key, BigDecimalUtils.scale(row.getQuantityUsedKgs()).negate(), BigDecimal::add);
            }
        }

        return fabricByCombo.entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimalUtils.ZERO) > 0)
                .sorted(Comparator
                        .comparing((Map.Entry<FabricKey, BigDecimal> entry) -> entry.getKey().dia().getDiaValue())
                        .thenComparing(entry -> entry.getKey().style().getStyleName()))
                .map(entry -> new InHouseStockResponse(
                        referenceMapper.toDiaRef(entry.getKey().dia()),
                        referenceMapper.toStyleRef(entry.getKey().style()),
                        BigDecimalUtils.scale(entry.getValue())
                ))
                .toList();
    }

    @Transactional
    public ExistingStockResponse createExistingStock(ExistingStockCreateRequest request) {
        log.info("Creating existing stock entry for dia='{}', style='{}'", request.diaAutoId(), request.styleAutoId());
        InHouseExistingStock entry = new InHouseExistingStock();
        entry.setAutoId(autoIdService.next(AutoIdSequence.GENERAL));
        entry.setEntryDate(request.entryDate());
        entry.setDia(lookupService.getActiveDiaByAutoId(request.diaAutoId()));
        entry.setStyle(lookupService.getActiveStyleByAutoId(request.styleAutoId()));
        entry.setQuantityKgs(BigDecimalUtils.scale(request.quantityKgs()));
        entry.setNotes(blankToNull(request.notes()));
        InHouseExistingStock saved = existingStockRepository.save(entry);
        log.info("Created existing stock '{}'", saved.getAutoId());
        return mapper.toExistingStockResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExistingStockResponse> getExistingStocks() {
        return existingStockRepository.findAllByIsDeletedFalseOrderByEntryDateDesc().stream()
                .map(mapper::toExistingStockResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CuttingResponse> getCuttings(
            String diaAutoId,
            String styleAutoId,
            CuttingStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching cuttings with diaAutoId='{}', styleAutoId='{}', status={}, fromDate={}, toDate={}, includeDeleted={}, pageable={}",
                diaAutoId, styleAutoId, status, fromDate, toDate, includeDeleted, pageable);
        return cuttingEntryRepository.findAll(cuttingSpec(diaAutoId, styleAutoId, status, fromDate, toDate, includeDeleted), pageable)
                .map(mapper::toCuttingResponse);
    }

    @Transactional
    public CuttingResponse createCutting(CuttingCreateRequest request) {
        log.info("Creating cutting entry on {} with {} rows", request.cuttingDate(), request.rows().size());
        validateCuttingRows(request.rows(), null);
        CuttingEntry cuttingEntry = new CuttingEntry();
        cuttingEntry.setAutoId(autoIdService.next(AutoIdSequence.CUTTING));
        cuttingEntry.setCuttingDate(request.cuttingDate());
        cuttingEntry.setNotes(blankToNull(request.notes()));
        cuttingEntry.setTotalOutputPieces(request.totalOutputPieces() != null ? request.totalOutputPieces() : 0);
        replaceCuttingRows(cuttingEntry, request.rows());
        recomputeCuttingTotals(cuttingEntry);
        CuttingEntry saved = cuttingEntryRepository.save(cuttingEntry);
        log.info("Created cutting '{}'", saved.getAutoId());
        return mapper.toCuttingResponse(saved);
    }

    @Transactional(readOnly = true)
    public CuttingAvailabilityResponse getAvailableCuttingQuantity(String diaAutoId, String styleAutoId) {
        Dia dia = lookupService.getActiveDiaByAutoId(diaAutoId);
        Style style = lookupService.getActiveStyleByAutoId(styleAutoId);
        BigDecimal available = calculateAvailableFabricKgs(dia.getId(), style.getId(), null);
        log.info("Calculated cutting availability for dia='{}', style='{}': {}", diaAutoId, styleAutoId, available);
        return new CuttingAvailabilityResponse(dia.getAutoId(), style.getAutoId(), available);
    }

    @Transactional
    public CuttingResponse updateCutting(String cuttingAutoId, CuttingUpdateRequest request) {
        log.info("Updating cutting '{}' with {} rows", cuttingAutoId, request.rows().size());
        CuttingEntry cuttingEntry = lookupService.getActiveCuttingEntryByAutoId(cuttingAutoId);
        validateCuttingRows(request.rows(), cuttingEntry);
        cuttingEntry.setNotes(blankToNull(request.notes()));
        cuttingEntry.setTotalOutputPieces(request.totalOutputPieces() != null ? request.totalOutputPieces() : 0);
        replaceCuttingRows(cuttingEntry, request.rows());
        recomputeCuttingTotals(cuttingEntry);
        CuttingEntry saved = cuttingEntryRepository.save(cuttingEntry);
        log.info("Updated cutting '{}' with status={}", saved.getAutoId(), saved.getStatus());
        return mapper.toCuttingResponse(saved);
    }

    @Transactional
    public void deleteCutting(String cuttingAutoId) {
        log.info("Deleting cutting '{}'", cuttingAutoId);
        CuttingEntry cuttingEntry = lookupService.getActiveCuttingEntryByAutoId(cuttingAutoId);
        cuttingEntry.setDeleted(true);
        cuttingEntryRepository.save(cuttingEntry);
    }

    @Transactional(readOnly = true)
    public List<StitchedStockResponse> getStitchedStock(String styleAutoId, LocalDate fromDate, LocalDate toDate) {
        log.info("Calculating stitched stock with styleAutoId='{}', fromDate={}, toDate={}", styleAutoId, fromDate, toDate);
        return buildStitchedStock(styleAutoId, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    public InHouseDashboardResponse getDashboard(String diaAutoId, String styleAutoId, LocalDate fromDate, LocalDate toDate) {
        log.info("Calculating in-house dashboard with diaAutoId='{}', styleAutoId='{}', fromDate={}, toDate={}",
                diaAutoId, styleAutoId, fromDate, toDate);
        List<InHouseStockResponse> stockRows = getStock(diaAutoId, styleAutoId);
        BigDecimal fabricInStock = stockRows.stream()
                .map(InHouseStockResponse::availableQuantityKgs)
                .reduce(BigDecimalUtils.ZERO, BigDecimal::add);
        long cuttingInProgressCount = cuttingEntryRepository.findAll().stream()
                .filter(entry -> !entry.isDeleted())
                .filter(entry -> entry.getStatus() == CuttingStatus.IN_PROGRESS)
                .count();
        int totalPiecesCut = cuttingEntryRepository.findAll().stream()
                .filter(entry -> !entry.isDeleted())
                .filter(entry -> entry.getStatus() == CuttingStatus.COMPLETED)
                .map(CuttingEntry::getTotalOutputPieces)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        List<StitchedStockResponse> stitchedStockRows = buildStitchedStock(styleAutoId, fromDate, toDate);
        int stitchedPlainTotal = stitchedStockRows.stream().mapToInt(row -> Objects.requireNonNullElse(row.plainPieces(), 0)).sum();
        int printedTotal = stitchedStockRows.stream().mapToInt(row -> Objects.requireNonNullElse(row.printedPieces(), 0)).sum();
        int defectiveTotal = calculateCombinedDefectiveTotal(styleAutoId);
        return new InHouseDashboardResponse(
                BigDecimalUtils.scale(fabricInStock),
                cuttingInProgressCount,
                totalPiecesCut,
                calculateReadyToStitchTotal(styleAutoId),
                stitchedPlainTotal,
                printedTotal,
                defectiveTotal
        );
    }

    @Transactional(readOnly = true)
    public int calculateReadyToStitchAvailable(UUID styleId, GarmentSize size) {
        int cutPieces = Objects.requireNonNullElse(cuttingEntryRowRepository.sumCompletedOutputPiecesByStyleAndSize(styleId, size), BigDecimal.ZERO).intValue();
        int committedPieces = Objects.requireNonNullElse(stitchingOrderRowRepository.sumActivePiecesTakenByStyleAndSize(styleId, size), 0);
        int available = Math.max(cutPieces - committedPieces, 0);
        log.debug("Ready-to-stitch availability for styleId={}, size={}: cutPieces={}, committedPieces={}, available={}",
                styleId, size, cutPieces, committedPieces, available);
        return available;
    }

    @Transactional(readOnly = true)
    public int calculateStitchedPlainAvailable(UUID styleId, GarmentSize size) {
        int delivered = Objects.requireNonNullElse(stitchingDeliveryRepository.sumActiveDeliveredByStyleAndSize(styleId, size), 0);
        int sentToPrinting = Objects.requireNonNullElse(printingOrderRepository.sumActiveOrderedByStyleAndSize(styleId, size), 0);
        int packedPlain = Objects.requireNonNullElse(
                packingEntryRepository.sumConsumedBySourceAndStyleAndSize(PackingStockType.PLAIN, styleId, size),
                0
        );
        int available = Math.max(delivered - sentToPrinting - packedPlain, 0);
        log.debug("Stitched plain availability for styleId={}, size={}: delivered={}, sentToPrinting={}, packedPlain={}, available={}",
                styleId, size, delivered, sentToPrinting, packedPlain, available);
        return available;
    }

    @Transactional(readOnly = true)
    public int calculatePrintedAvailable(UUID styleId, GarmentSize size) {
        int printedDelivered = Objects.requireNonNullElse(printingDeliveryRepository.sumActiveDeliveredByStyleAndSize(styleId, size), 0);
        int packedPrinted = Objects.requireNonNullElse(
                packingEntryRepository.sumConsumedBySourceAndStyleAndSize(PackingStockType.PRINTED, styleId, size),
                0
        );
        int available = Math.max(printedDelivered - packedPrinted, 0);
        log.debug("Printed availability for styleId={}, size={}: delivered={}, packedPrinted={}, available={}",
                styleId, size, printedDelivered, packedPrinted, available);
        return available;
    }

    private void validateCuttingRows(List<CuttingRowRequest> rows, CuttingEntry existingEntry) {
        Map<FabricIdKey, BigDecimal> requestedByCombo = new HashMap<>();
        for (CuttingRowRequest row : rows) {
            Dia dia = lookupService.getActiveDiaByAutoId(row.diaAutoId());
            Style style = lookupService.getActiveStyleByAutoId(row.styleAutoId());
            requestedByCombo.merge(
                    new FabricIdKey(dia.getId(), style.getId()),
                    BigDecimalUtils.scale(row.quantityUsedKgs()),
                    BigDecimal::add
            );
        }

        for (Map.Entry<FabricIdKey, BigDecimal> entry : requestedByCombo.entrySet()) {
            BigDecimal available = calculateAvailableFabricKgs(entry.getKey().diaId(), entry.getKey().styleId(), existingEntry);
            if (entry.getValue().compareTo(available) > 0) {
                throw new BusinessValidationException(
                        "CUTTING_EXCEEDS_STOCK",
                        "Quantity used exceeds the available in-house fabric stock",
                        Map.of(
                                "diaId", entry.getKey().diaId(),
                                "styleId", entry.getKey().styleId(),
                                "availableQuantityKgs", available,
                                "requestedQuantityKgs", entry.getValue()
                        )
                );
            }
        }
    }

    private void replaceCuttingRows(CuttingEntry cuttingEntry, List<CuttingRowRequest> rows) {
        cuttingEntry.getRows().clear();
        for (CuttingRowRequest rowRequest : rows) {
            CuttingEntryRow row = new CuttingEntryRow();
            row.setCuttingEntry(cuttingEntry);
            row.setDia(lookupService.getActiveDiaByAutoId(rowRequest.diaAutoId()));
            row.setStyle(lookupService.getActiveStyleByAutoId(rowRequest.styleAutoId()));
            row.setSize(rowRequest.size());
            row.setQuantityUsedKgs(BigDecimalUtils.scale(rowRequest.quantityUsedKgs()));
            row.setRatePerPiece(rowRequest.ratePerPiece() != null ? BigDecimalUtils.scale(rowRequest.ratePerPiece()) : null);
            cuttingEntry.getRows().add(row);
        }
    }

    private void recomputeCuttingTotals(CuttingEntry cuttingEntry) {
        BigDecimal totalQuantity = cuttingEntry.getRows().stream()
                .map(CuttingEntryRow::getQuantityUsedKgs)
                .filter(Objects::nonNull)
                .map(BigDecimalUtils::scale)
                .reduce(BigDecimalUtils.ZERO, BigDecimal::add);
        
        int totalOutputPieces = cuttingEntry.getTotalOutputPieces() != null ? cuttingEntry.getTotalOutputPieces() : 0;
        boolean completed = totalOutputPieces > 0;

        cuttingEntry.setTotalQuantityUsedKgs(BigDecimalUtils.scale(totalQuantity));
        cuttingEntry.setStatus(completed ? CuttingStatus.COMPLETED : CuttingStatus.IN_PROGRESS);
        if (totalQuantity.signum() > 0 && totalOutputPieces > 0) {
            cuttingEntry.setPcsPerKg(BigDecimal.valueOf(totalOutputPieces).divide(totalQuantity, 2, RoundingMode.HALF_UP));
        } else {
            cuttingEntry.setPcsPerKg(null);
        }
        log.debug("Cutting '{}' totals recalculated: totalQuantity={}, totalOutputPieces={}, pcsPerKg={}, status={}",
                cuttingEntry.getAutoId(), cuttingEntry.getTotalQuantityUsedKgs(), cuttingEntry.getTotalOutputPieces(),
                cuttingEntry.getPcsPerKg(), cuttingEntry.getStatus());
    }

    private BigDecimal calculateAvailableFabricKgs(UUID diaId, UUID styleId, CuttingEntry excludingEntry) {
        BigDecimal splitQuantity = BigDecimalUtils.scale(inHouseStockSplitRepository.sumActiveQuantityByDiaAndStyle(diaId, styleId));
        BigDecimal addedQuantity = BigDecimalUtils.scale(existingStockRepository.sumQuantityByDiaAndStyle(diaId, styleId));
        BigDecimal usedQuantity = BigDecimalUtils.scale(cuttingEntryRowRepository.sumActiveQuantityUsedByDiaAndStyle(diaId, styleId));
        if (excludingEntry != null) {
            BigDecimal currentEntryUsed = excludingEntry.getRows().stream()
                    .filter(row -> row.getDia().getId().equals(diaId) && row.getStyle().getId().equals(styleId))
                    .map(CuttingEntryRow::getQuantityUsedKgs)
                    .filter(Objects::nonNull)
                    .map(BigDecimalUtils::scale)
                    .reduce(BigDecimalUtils.ZERO, BigDecimal::add);
            usedQuantity = usedQuantity.subtract(currentEntryUsed);
        }
        return BigDecimalUtils.scale(splitQuantity.add(addedQuantity).subtract(usedQuantity).max(BigDecimalUtils.ZERO));
    }

    private boolean canDeleteSplit(InHouseStockSplit split) {
        BigDecimal totalSplitStock = BigDecimalUtils.scale(
                inHouseStockSplitRepository.sumActiveQuantityByDiaAndStyle(split.getDia().getId(), split.getStyle().getId())
        );
        BigDecimal totalAddedStock = BigDecimalUtils.scale(
                existingStockRepository.sumQuantityByDiaAndStyle(split.getDia().getId(), split.getStyle().getId())
        );
        BigDecimal totalCuttingUsage = BigDecimalUtils.scale(
                cuttingEntryRowRepository.sumActiveQuantityUsedByDiaAndStyle(split.getDia().getId(), split.getStyle().getId())
        );
        BigDecimal remainingIfDeleted = totalSplitStock.add(totalAddedStock).subtract(BigDecimalUtils.scale(split.getQuantityKgs()));
        boolean canDelete = remainingIfDeleted.compareTo(totalCuttingUsage) >= 0;
        log.debug("Split delete check for '{}': totalSplitStock={}, totalCuttingUsage={}, remainingIfDeleted={}, canDelete={}",
                split.getAutoId(), totalSplitStock, totalCuttingUsage, remainingIfDeleted, canDelete);
        return canDelete;
    }

    private void recalculateSplitStatus(InHouseDelivery delivery) {
        BigDecimal allocated = BigDecimalUtils.scale(inHouseStockSplitRepository.sumActiveQuantityByDelivery(delivery.getId()));
        BigDecimal totalQuantity = BigDecimalUtils.scale(delivery.getQuantityKgs());
        SplitStatus status;
        if (allocated.signum() == 0) {
            status = SplitStatus.PENDING;
        } else if (allocated.compareTo(totalQuantity) >= 0) {
            status = SplitStatus.FULLY_SPLIT;
        } else {
            status = SplitStatus.PARTIALLY_SPLIT;
        }
        delivery.setSplitStatus(status);
        inHouseDeliveryRepository.save(delivery);
        log.debug("Delivery '{}' split status recalculated: allocated={}, totalQuantity={}, status={}",
                delivery.getAutoId(), allocated, totalQuantity, status);
    }

    private List<StitchedStockResponse> buildStitchedStock(String styleAutoId, LocalDate fromDate, LocalDate toDate) {
        Map<StockKey, StockBucket> buckets = new LinkedHashMap<>();

        for (StitchingDelivery delivery : stitchingDeliveryRepository.findAll()) {
            if (delivery.isDeleted() || !matchesStyle(delivery.getStyle(), styleAutoId) || !matchesDate(delivery.getDeliveryDate(), fromDate, toDate)) {
                continue;
            }
            StockKey key = new StockKey(delivery.getStyle(), delivery.getSize());
            buckets.computeIfAbsent(key, ignored -> new StockBucket(delivery.getStyle(), delivery.getSize()))
                    .addPlain(delivery.getPiecesDelivered(), delivery.getDeliveryDate());
        }

        for (PrintingOrder order : printingOrderRepository.findAll()) {
            if (order.isDeleted() || !matchesStyle(order.getStyle(), styleAutoId) || !matchesDate(order.getOrderDate(), fromDate, toDate)) {
                continue;
            }
            StockKey key = new StockKey(order.getStyle(), order.getSize());
            buckets.computeIfAbsent(key, ignored -> new StockBucket(order.getStyle(), order.getSize()))
                    .consumePlain(order.getPiecesOrdered(), order.getOrderDate());
        }

        for (PrintingDelivery delivery : printingDeliveryRepository.findAll()) {
            if (delivery.isDeleted()) {
                continue;
            }
            PrintingOrder order = delivery.getPrintingOrder();
            if (!matchesStyle(order.getStyle(), styleAutoId) || !matchesDate(delivery.getDeliveryDate(), fromDate, toDate)) {
                continue;
            }
            StockKey key = new StockKey(order.getStyle(), order.getSize());
            buckets.computeIfAbsent(key, ignored -> new StockBucket(order.getStyle(), order.getSize()))
                    .addPrinted(delivery.getPiecesDelivered(), delivery.getDeliveryDate());
        }

        for (PackingEntry packingEntry : packingEntryRepository.findAll()) {
            if (packingEntry.isDeleted() || !matchesStyle(packingEntry.getStyle(), styleAutoId) || !matchesDate(packingEntry.getPackingDate(), fromDate, toDate)) {
                continue;
            }
            StockKey key = new StockKey(packingEntry.getStyle(), packingEntry.getSize());
            StockBucket bucket = buckets.computeIfAbsent(key, ignored -> new StockBucket(packingEntry.getStyle(), packingEntry.getSize()));
            int totalConsumed = packingEntry.getCorrectlyPackedPieces() + packingEntry.getDefectivePieces();
            if (packingEntry.getStockType() == PackingStockType.PLAIN) {
                bucket.consumePlain(totalConsumed, packingEntry.getPackingDate());
            } else {
                bucket.consumePrinted(totalConsumed, packingEntry.getPackingDate());
            }
            bucket.addDefective(packingEntry.getDefectivePieces(), packingEntry.getPackingDate());
        }

        for (StitchingOrder order : stitchingOrderRepository.findAll()) {
            if (order.isDeleted() || !matchesDate(order.getOrderDate(), fromDate, toDate)) {
                continue;
            }
            Style style = resolvePrimaryStyle(order);
            if (!matchesStyle(style, styleAutoId)) {
                continue;
            }
            int defective = calculateStitchingDefective(order);
            if (defective <= 0 || style == null) {
                continue;
            }
            StockKey key = new StockKey(style, order.getExpectedSize());
            buckets.computeIfAbsent(key, ignored -> new StockBucket(style, order.getExpectedSize()))
                    .addDefective(defective, order.getOrderDate());
        }

        for (PrintingOrder order : printingOrderRepository.findAll()) {
            if (order.isDeleted() || !matchesStyle(order.getStyle(), styleAutoId) || !matchesDate(order.getOrderDate(), fromDate, toDate)) {
                continue;
            }
            int defective = calculatePrintingDefective(order);
            if (defective <= 0) {
                continue;
            }
            StockKey key = new StockKey(order.getStyle(), order.getSize());
            buckets.computeIfAbsent(key, ignored -> new StockBucket(order.getStyle(), order.getSize()))
                    .addDefective(defective, order.getOrderDate());
        }

        return buckets.values().stream()
                .sorted(Comparator.comparing((StockBucket bucket) -> bucket.style().getStyleName()).thenComparing(bucket -> bucket.size().name()))
                .map(bucket -> new StitchedStockResponse(
                        referenceMapper.toStyleRef(bucket.style()),
                        bucket.size(),
                        Math.max(bucket.plainPieces(), 0),
                        Math.max(bucket.printedPieces(), 0),
                        bucket.latestTransactionDate(),
                        Math.max(bucket.defectivePieces(), 0)
                ))
                .toList();
    }

    private int calculateReadyToStitchTotal(String styleAutoId) {
        Map<UUID, EnumMap<GarmentSize, Integer>> availableByStyleAndSize = new HashMap<>();
        for (CuttingEntryRow row : cuttingEntryRowRepository.findAllActiveRows()) {
            if (!matchesStyle(row.getStyle(), styleAutoId)) {
                continue;
            }
            availableByStyleAndSize
                    .computeIfAbsent(row.getStyle().getId(), ignored -> new EnumMap<>(GarmentSize.class))
                    .put(row.getSize(), 0);
        }

        return availableByStyleAndSize.entrySet().stream()
                .flatMap(entry -> entry.getValue().keySet().stream().map(size -> calculateReadyToStitchAvailable(entry.getKey(), size)))
                .mapToInt(Integer::intValue)
                .sum();
    }

    private int calculateCombinedDefectiveTotal(String styleAutoId) {
        int stitchingDefective = stitchingOrderRepository.findAll().stream()
                .filter(order -> !order.isDeleted())
                .filter(order -> matchesStyle(resolvePrimaryStyle(order), styleAutoId))
                .mapToInt(this::calculateStitchingDefective)
                .sum();
        int printingDefective = printingOrderRepository.findAll().stream()
                .filter(order -> !order.isDeleted())
                .filter(order -> matchesStyle(order.getStyle(), styleAutoId))
                .mapToInt(this::calculatePrintingDefective)
                .sum();
        int packingDefective = packingEntryRepository.findAll().stream()
                .filter(entry -> !entry.isDeleted())
                .filter(entry -> matchesStyle(entry.getStyle(), styleAutoId))
                .mapToInt(PackingEntry::getDefectivePieces)
                .sum();
        return stitchingDefective + printingDefective + packingDefective;
    }

    private int calculateStitchingDefective(StitchingOrder order) {
        if (order.getStatus() != StitchingOrderStatus.COMPLETE) {
            return 0;
        }
        int delivered = Objects.requireNonNullElse(stitchingDeliveryAllocationRepository.sumActiveAllocatedByOrder(order.getId()), 0);
        return Math.max(order.getExpectedPieces() - delivered, 0);
    }

    private int calculatePrintingDefective(PrintingOrder order) {
        if (order.getStatus() != StitchingOrderStatus.COMPLETE) {
            return 0;
        }
        int delivered = Objects.requireNonNullElse(printingDeliveryRepository.sumActiveDeliveredByOrder(order.getId()), 0);
        return Math.max(order.getPiecesOrdered() - delivered, 0);
    }

    private Style resolvePrimaryStyle(StitchingOrder order) {
        return order.getRows().stream().findFirst().map(StitchingOrderRow::getStyle).orElse(null);
    }

    private boolean matchesStyle(Style style, String styleAutoId) {
        if (styleAutoId == null || styleAutoId.isBlank()) {
            return true;
        }
        return style != null && style.getAutoId().equalsIgnoreCase(styleAutoId.trim());
    }

    private boolean matchesDia(Dia dia, String diaAutoId) {
        if (diaAutoId == null || diaAutoId.isBlank()) {
            return true;
        }
        return dia != null && dia.getAutoId().equalsIgnoreCase(diaAutoId.trim());
    }

    private boolean matchesDate(LocalDate value, LocalDate fromDate, LocalDate toDate) {
        if (value == null) {
            return false;
        }
        boolean afterStart = fromDate == null || !value.isBefore(fromDate);
        boolean beforeEnd = toDate == null || !value.isAfter(toDate);
        return afterStart && beforeEnd;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Specification<InHouseDelivery> deliverySpec(String styleAutoId, LocalDate fromDate, LocalDate toDate, boolean includeDeleted) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!includeDeleted) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
            }
            if (styleAutoId != null && !styleAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("style").get("autoId")), styleAutoId.trim().toLowerCase()));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("deliveryDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("deliveryDate"), toDate));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<CuttingEntry> cuttingSpec(
            String diaAutoId,
            String styleAutoId,
            CuttingStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted
    ) {
        return (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("rows", JoinType.LEFT);
            }
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();
            if (!includeDeleted) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("cuttingDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("cuttingDate"), toDate));
            }

            Join<CuttingEntry, CuttingEntryRow> rowJoin = root.join("rows", JoinType.LEFT);
            if (diaAutoId != null && !diaAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(rowJoin.get("dia").get("autoId")), diaAutoId.trim().toLowerCase()));
            }
            if (styleAutoId != null && !styleAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(rowJoin.get("style").get("autoId")), styleAutoId.trim().toLowerCase()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private record FabricKey(Dia dia, Style style) {
    }

    private record FabricIdKey(UUID diaId, UUID styleId) {
    }

    private record StockKey(Style style, GarmentSize size) {
    }

    private static final class StockBucket {
        private final Style style;
        private final GarmentSize size;
        private int plainPieces;
        private int printedPieces;
        private int defectivePieces;
        private LocalDate latestTransactionDate;

        private StockBucket(Style style, GarmentSize size) {
            this.style = style;
            this.size = size;
        }

        private void addPlain(int pieces, LocalDate transactionDate) {
            plainPieces += pieces;
            touch(transactionDate);
        }

        private void consumePlain(int pieces, LocalDate transactionDate) {
            plainPieces -= pieces;
            touch(transactionDate);
        }

        private void addPrinted(int pieces, LocalDate transactionDate) {
            printedPieces += pieces;
            touch(transactionDate);
        }

        private void consumePrinted(int pieces, LocalDate transactionDate) {
            printedPieces -= pieces;
            touch(transactionDate);
        }

        private void addDefective(int pieces, LocalDate transactionDate) {
            defectivePieces += pieces;
            touch(transactionDate);
        }

        private void touch(LocalDate transactionDate) {
            if (transactionDate == null) {
                return;
            }
            if (latestTransactionDate == null || transactionDate.isAfter(latestTransactionDate)) {
                latestTransactionDate = transactionDate;
            }
        }

        private Style style() {
            return style;
        }

        private GarmentSize size() {
            return size;
        }

        private int plainPieces() {
            return plainPieces;
        }

        private int printedPieces() {
            return printedPieces;
        }

        private int defectivePieces() {
            return defectivePieces;
        }

        private LocalDate latestTransactionDate() {
            return latestTransactionDate;
        }
    }
}
