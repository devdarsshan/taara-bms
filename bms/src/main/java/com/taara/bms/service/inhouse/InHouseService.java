package com.taara.bms.service.inhouse;

import com.taara.bms.dto.inhouse.CuttingCreateRequest;
import com.taara.bms.dto.inhouse.CuttingAvailabilityResponse;
import com.taara.bms.dto.inhouse.CuttingResponse;
import com.taara.bms.dto.inhouse.CuttingUpdateRequest;
import com.taara.bms.dto.inhouse.InHouseDashboardResponse;
import com.taara.bms.dto.inhouse.InHouseDeliveryResponse;
import com.taara.bms.dto.inhouse.InHouseSplitBatchRequest;
import com.taara.bms.dto.inhouse.InHouseStockResponse;
import com.taara.bms.dto.inhouse.InHouseStockSplitResponse;
import com.taara.bms.dto.inhouse.StitchedStockResponse;
import com.taara.bms.entity.inhouse.CuttingEntry;
import com.taara.bms.entity.inhouse.InHouseDelivery;
import com.taara.bms.entity.inhouse.InHouseStockSplit;
import com.taara.bms.entity.masterdata.Dia;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.entity.stitching.StitchingDelivery;
import com.taara.bms.entity.stitching.StitchingOrder;
import com.taara.bms.enums.CuttingStatus;
import com.taara.bms.enums.SplitStatus;
import com.taara.bms.enums.StitchingOrderStatus;
import com.taara.bms.exception.BusinessValidationException;
import com.taara.bms.exception.DeleteConflictException;
import com.taara.bms.exception.ResourceNotFoundException;
import com.taara.bms.helper.BigDecimalUtils;
import com.taara.bms.mapper.common.ReferenceMapper;
import com.taara.bms.mapper.inhouse.InHouseMapper;
import com.taara.bms.repo.inhouse.CuttingEntryRepository;
import com.taara.bms.repo.inhouse.InHouseDeliveryRepository;
import com.taara.bms.repo.inhouse.InHouseStockSplitRepository;
import com.taara.bms.repo.stitching.StitchingDeliveryRepository;
import com.taara.bms.repo.stitching.StitchingOrderRepository;
import com.taara.bms.service.common.AutoIdSequence;
import com.taara.bms.service.common.AutoIdService;
import com.taara.bms.service.common.LookupService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InHouseService {

    private static final Logger log = LoggerFactory.getLogger(InHouseService.class);

    private final InHouseDeliveryRepository inHouseDeliveryRepository;
    private final InHouseStockSplitRepository inHouseStockSplitRepository;
    private final CuttingEntryRepository cuttingEntryRepository;
    private final StitchingOrderRepository stitchingOrderRepository;
    private final StitchingDeliveryRepository stitchingDeliveryRepository;
    private final LookupService lookupService;
    private final AutoIdService autoIdService;
    private final InHouseMapper mapper;
    private final ReferenceMapper referenceMapper;

    public InHouseService(
            InHouseDeliveryRepository inHouseDeliveryRepository,
            InHouseStockSplitRepository inHouseStockSplitRepository,
            CuttingEntryRepository cuttingEntryRepository,
            StitchingOrderRepository stitchingOrderRepository,
            StitchingDeliveryRepository stitchingDeliveryRepository,
            LookupService lookupService,
            AutoIdService autoIdService,
            InHouseMapper mapper,
            ReferenceMapper referenceMapper
    ) {
        this.inHouseDeliveryRepository = inHouseDeliveryRepository;
        this.inHouseStockSplitRepository = inHouseStockSplitRepository;
        this.cuttingEntryRepository = cuttingEntryRepository;
        this.stitchingOrderRepository = stitchingOrderRepository;
        this.stitchingDeliveryRepository = stitchingDeliveryRepository;
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
        Page<InHouseDeliveryResponse> deliveries = inHouseDeliveryRepository.findAll(deliverySpec(styleAutoId, fromDate, toDate, includeDeleted), pageable)
                .map(delivery -> mapper.toDeliveryResponse(
                        delivery,
                        inHouseStockSplitRepository.sumActiveQuantityByDelivery(delivery.getId())
                ));
        log.info("Fetched {} in-house deliveries", deliveries.getNumberOfElements());
        return deliveries;
    }

    @Transactional(readOnly = true)
    public List<InHouseStockSplitResponse> getSplits(String deliveryAutoId) {
        log.info("Fetching in-house splits for delivery '{}'", deliveryAutoId);
        InHouseDelivery delivery = getActiveDelivery(deliveryAutoId);
        List<InHouseStockSplitResponse> splits = inHouseStockSplitRepository.findByDelivery_IdAndIsDeletedFalse(delivery.getId()).stream()
                .map(split -> mapper.toSplitResponse(split, canDeleteSplit(split)))
                .toList();
        log.info("Fetched {} in-house split(s) for delivery '{}'", splits.size(), deliveryAutoId);
        return splits;
    }

    @Transactional
    public List<InHouseStockSplitResponse> createSplits(String deliveryAutoId, InHouseSplitBatchRequest request) {
        log.info("Creating {} in-house split(s) for delivery '{}'", request.splits().size(), deliveryAutoId);
        InHouseDelivery delivery = getActiveDelivery(deliveryAutoId);
        BigDecimal currentAllocated = inHouseStockSplitRepository.sumActiveQuantityByDelivery(delivery.getId());
        BigDecimal requestedTotal = request.splits().stream()
                .map(split -> BigDecimalUtils.scale(split.quantityKgs()))
                .reduce(BigDecimalUtils.ZERO, BigDecimal::add);
        log.debug("Split allocation check for delivery '{}': currentAllocated={}, requestedTotal={}, deliveryQuantity={}",
                deliveryAutoId, currentAllocated, requestedTotal, delivery.getQuantityKgs());
        if (currentAllocated.add(requestedTotal).compareTo(delivery.getQuantityKgs()) > 0) {
            throw new BusinessValidationException(
                    "SPLIT_EXCEEDS_DELIVERY",
                    "Split quantity exceeds the available delivery quantity",
                    Map.of(
                            "deliveryAutoId", delivery.getAutoId(),
                            "deliveryQuantity", delivery.getQuantityKgs(),
                            "alreadyAllocated", currentAllocated,
                            "requested", requestedTotal
                    )
            );
        }

        List<InHouseStockSplit> createdSplits = new ArrayList<>();
        for (var splitRequest : request.splits()) {
            Dia dia = lookupService.getActiveDiaByAutoId(splitRequest.diaAutoId());
            InHouseStockSplit split = new InHouseStockSplit();
            split.setAutoId(autoIdService.next(AutoIdSequence.INHOUSE_STOCK_SPLIT));
            split.setDelivery(delivery);
            split.setDia(dia);
            split.setStyle(delivery.getStyle());
            split.setQuantityKgs(BigDecimalUtils.scale(splitRequest.quantityKgs()));
            createdSplits.add(inHouseStockSplitRepository.save(split));
            log.info("Created in-house split '{}' for delivery '{}' and dia '{}'",
                    split.getAutoId(), delivery.getAutoId(), dia.getAutoId());
        }
        recalculateSplitStatus(delivery);
        List<InHouseStockSplitResponse> responses = createdSplits.stream()
                .map(split -> mapper.toSplitResponse(split, canDeleteSplit(split)))
                .toList();
        log.info("Created {} in-house split(s) for delivery '{}'", responses.size(), deliveryAutoId);
        return responses;
    }

    @Transactional
    public void deleteSplit(String deliveryAutoId, String splitAutoId) {
        log.info("Deleting in-house split '{}' from delivery '{}'", splitAutoId, deliveryAutoId);
        InHouseStockSplit split = lookupService.getActiveInHouseStockSplitByAutoId(splitAutoId);
        if (!split.getDelivery().getAutoId().equalsIgnoreCase(deliveryAutoId.trim())) {
            throw new ResourceNotFoundException("INHOUSE_SPLIT_NOT_FOUND", "In-house split not found");
        }

        BigDecimal totalSplit = inHouseStockSplitRepository.sumActiveQuantityByDiaAndStyle(split.getDia().getId(), split.getStyle().getId());
        BigDecimal totalCut = cuttingEntryRepository.sumActiveQuantityByDiaAndStyle(split.getDia().getId(), split.getStyle().getId());
        BigDecimal remainingSplit = totalSplit.subtract(split.getQuantityKgs());
        log.debug("Split delete guardrail for split '{}': totalSplit={}, totalCut={}, remainingSplit={}",
                splitAutoId, totalSplit, totalCut, remainingSplit);
        if (remainingSplit.compareTo(totalCut) < 0) {
            throw new DeleteConflictException(
                    "SPLIT_HAS_DOWNSTREAM_USAGE",
                    "Split cannot be deleted because active cutting already depends on this stock",
                    Map.of(
                            "deliveryAutoId", split.getDelivery().getAutoId(),
                            "diaAutoId", split.getDia().getAutoId(),
                            "styleAutoId", split.getStyle().getAutoId(),
                            "remainingSplit", remainingSplit,
                            "cuttingUsed", totalCut
                    )
            );
        }
        split.setDeleted(true);
        inHouseStockSplitRepository.save(split);
        recalculateSplitStatus(split.getDelivery());
        log.info("Deleted in-house split '{}'", splitAutoId);
    }

    @Transactional(readOnly = true)
    public List<InHouseStockResponse> getStock(String diaAutoId, String styleAutoId) {
        log.info("Calculating in-house stock with diaAutoId='{}', styleAutoId='{}'", diaAutoId, styleAutoId);
        Map<StockKey, StockBucket> buckets = new LinkedHashMap<>();
        for (InHouseStockSplit split : inHouseStockSplitRepository.findAll((root, query, cb) -> cb.isFalse(root.get("isDeleted")))) {
            if (diaAutoId != null && !diaAutoId.isBlank() && !split.getDia().getAutoId().equalsIgnoreCase(diaAutoId.trim())) {
                continue;
            }
            if (styleAutoId != null && !styleAutoId.isBlank() && !split.getStyle().getAutoId().equalsIgnoreCase(styleAutoId.trim())) {
                continue;
            }
            StockKey key = new StockKey(split.getDia().getId(), split.getStyle().getId());
            buckets.computeIfAbsent(key, ignored -> new StockBucket(split.getDia(), split.getStyle()))
                    .addSplit(split.getQuantityKgs());
        }
        for (CuttingEntry cutting : cuttingEntryRepository.findAll((root, query, cb) -> cb.isFalse(root.get("isDeleted")))) {
            StockKey key = new StockKey(cutting.getDia().getId(), cutting.getStyle().getId());
            StockBucket bucket = buckets.get(key);
            if (bucket != null) {
                bucket.addCutting(cutting.getQuantityUsedKgs());
            }
        }
        List<InHouseStockResponse> stock = buckets.values().stream()
                .map(bucket -> new InHouseStockResponse(
                        referenceMapper.toDiaRef(bucket.dia()),
                        referenceMapper.toStyleRef(bucket.style()),
                        bucket.available()
                ))
                .filter(response -> response.availableQuantityKgs().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing((InHouseStockResponse response) -> response.dia().diaValue())
                        .thenComparing(response -> response.style().styleName()))
                .toList();
        log.info("Calculated {} in-house stock bucket(s)", stock.size());
        return stock;
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
        Page<CuttingResponse> cuttings = cuttingEntryRepository.findAll(cuttingSpec(diaAutoId, styleAutoId, status, fromDate, toDate, includeDeleted), pageable)
                .map(mapper::toCuttingResponse);
        log.info("Fetched {} cutting records", cuttings.getNumberOfElements());
        return cuttings;
    }

    @Transactional
    public CuttingResponse createCutting(CuttingCreateRequest request) {
        log.info("Creating cutting for diaAutoId='{}', styleAutoId='{}', cuttingDate={}",
                request.diaAutoId(), request.styleAutoId(), request.cuttingDate());
        Dia dia = lookupService.getActiveDiaByAutoId(request.diaAutoId());
        Style style = lookupService.getActiveStyleByAutoId(request.styleAutoId());
        BigDecimal quantityUsed = BigDecimalUtils.scale(request.quantityUsedKgs());
        BigDecimal availableStock = calculateAvailableStock(dia.getId(), style.getId());
        log.debug("Cutting stock check for dia='{}', style='{}': requested={}, available={}",
                dia.getAutoId(), style.getAutoId(), quantityUsed, availableStock);
        if (quantityUsed.compareTo(availableStock) > 0) {
            throw new BusinessValidationException(
                    "CUTTING_EXCEEDS_AVAILABLE_STOCK",
                    "Cutting quantity exceeds available stock",
                    Map.of(
                            "diaAutoId", dia.getAutoId(),
                            "styleAutoId", style.getAutoId(),
                            "availableStock", availableStock,
                            "requestedQuantity", quantityUsed
                    )
            );
        }

        CuttingEntry cuttingEntry = new CuttingEntry();
        cuttingEntry.setAutoId(autoIdService.next(AutoIdSequence.CUTTING));
        cuttingEntry.setCuttingDate(request.cuttingDate());
        cuttingEntry.setDia(dia);
        cuttingEntry.setStyle(style);
        cuttingEntry.setQuantityUsedKgs(quantityUsed);
        cuttingEntry.setOutputPieces(request.outputPieces());
        cuttingEntry.setStatus(request.outputPieces() == null ? CuttingStatus.IN_PROGRESS : CuttingStatus.COMPLETED);
        cuttingEntry.setNotes(request.notes());
        CuttingResponse response = mapper.toCuttingResponse(cuttingEntryRepository.save(cuttingEntry));
        log.info("Created cutting '{}'", response.autoId());
        return response;
    }

    @Transactional(readOnly = true)
    public CuttingAvailabilityResponse getAvailableCuttingQuantity(String diaAutoId, String styleAutoId) {
        log.info("Calculating cutting availability for diaAutoId='{}', styleAutoId='{}'", diaAutoId, styleAutoId);
        Dia dia = lookupService.getActiveDiaByAutoId(diaAutoId);
        Style style = lookupService.getActiveStyleByAutoId(styleAutoId);
        BigDecimal available = calculateAvailableStock(dia.getId(), style.getId());
        return new CuttingAvailabilityResponse(dia.getAutoId(), style.getAutoId(), available);
    }

    @Transactional
    public CuttingResponse updateCutting(String cuttingAutoId, CuttingUpdateRequest request) {
        log.info("Updating cutting '{}'", cuttingAutoId);
        CuttingEntry cuttingEntry = lookupService.getActiveCuttingEntryByAutoId(cuttingAutoId);
        if (request.outputPieces() != null) {
            cuttingEntry.setOutputPieces(request.outputPieces());
        }
        if (request.notes() != null) {
            cuttingEntry.setNotes(request.notes());
        }
        cuttingEntry.setStatus(cuttingEntry.getOutputPieces() == null ? CuttingStatus.IN_PROGRESS : CuttingStatus.COMPLETED);
        CuttingResponse response = mapper.toCuttingResponse(cuttingEntryRepository.save(cuttingEntry));
        log.info("Updated cutting '{}' with status={}", response.autoId(), response.status());
        return response;
    }

    @Transactional
    public void deleteCutting(String cuttingAutoId) {
        log.info("Deleting cutting '{}'", cuttingAutoId);
        CuttingEntry cuttingEntry = lookupService.getActiveCuttingEntryByAutoId(cuttingAutoId);
        cuttingEntry.setDeleted(true);
        cuttingEntryRepository.save(cuttingEntry);
        log.info("Deleted cutting '{}'", cuttingAutoId);
    }

    @Transactional(readOnly = true)
    public List<StitchedStockResponse> getStitchedStock(String styleAutoId, LocalDate fromDate, LocalDate toDate) {
        log.info("Calculating stitched stock with styleAutoId='{}', fromDate={}, toDate={}", styleAutoId, fromDate, toDate);
        Map<UUID, StitchedStockBucket> buckets = new LinkedHashMap<>();

        for (StitchingDelivery delivery : stitchingDeliveryRepository.findAll((root, query, cb) -> cb.isFalse(root.get("isDeleted")))) {
            Style style = delivery.getStitchingOrder().getStyle();
            if (styleAutoId != null && !styleAutoId.isBlank() && !style.getAutoId().equalsIgnoreCase(styleAutoId.trim())) {
                continue;
            }
            if (fromDate != null && delivery.getDeliveryDate().isBefore(fromDate)) {
                continue;
            }
            if (toDate != null && delivery.getDeliveryDate().isAfter(toDate)) {
                continue;
            }
            buckets.computeIfAbsent(style.getId(), ignored -> new StitchedStockBucket(style))
                    .addGoodPieces(delivery.getPiecesDelivered(), delivery.getDeliveryDate());
        }

        for (StitchingOrder order : stitchingOrderRepository.findAll((root, query, cb) -> cb.and(
                cb.isFalse(root.get("isDeleted")),
                cb.equal(root.get("status"), StitchingOrderStatus.COMPLETE)
        ))) {
            if (styleAutoId != null && !styleAutoId.isBlank() && !order.getStyle().getAutoId().equalsIgnoreCase(styleAutoId.trim())) {
                continue;
            }
            LocalDate completionDate = order.getUpdatedAt() != null ? order.getUpdatedAt().toLocalDate() : order.getOrderDate();
            if (fromDate != null && completionDate.isBefore(fromDate)) {
                continue;
            }
            if (toDate != null && completionDate.isAfter(toDate)) {
                continue;
            }
            int deliveredPieces = Objects.requireNonNullElse(stitchingDeliveryRepository.sumActivePiecesByOrder(order.getId()), 0);
            int defectivePieces = Math.max(order.getPiecesOrdered() - deliveredPieces, 0);
            buckets.computeIfAbsent(order.getStyle().getId(), ignored -> new StitchedStockBucket(order.getStyle()))
                    .addDefectivePieces(defectivePieces, completionDate);
        }

        List<StitchedStockResponse> stitchedStock = buckets.values().stream()
                .map(bucket -> new StitchedStockResponse(
                        referenceMapper.toStyleRef(bucket.style()),
                        bucket.latestTransactionDate(),
                        bucket.goodPieces(),
                        bucket.defectivePieces()
                ))
                .sorted(Comparator.comparing((StitchedStockResponse response) -> response.style().styleName()))
                .toList();
        log.info("Calculated {} stitched stock bucket(s)", stitchedStock.size());
        return stitchedStock;
    }

    @Transactional(readOnly = true)
    public InHouseDashboardResponse getDashboard(String diaAutoId, String styleAutoId, LocalDate fromDate, LocalDate toDate) {
        log.info("Calculating in-house dashboard with diaAutoId='{}', styleAutoId='{}', fromDate={}, toDate={}",
                diaAutoId, styleAutoId, fromDate, toDate);
        List<InHouseStockResponse> stockResponses = getStock(diaAutoId, styleAutoId);
        BigDecimal totalFabricInStock = stockResponses.stream()
                .map(InHouseStockResponse::availableQuantityKgs)
                .reduce(BigDecimalUtils.ZERO, BigDecimal::add);

        List<CuttingEntry> cuttings = cuttingEntryRepository.findAll(cuttingSpec(diaAutoId, styleAutoId, null, fromDate, toDate, false));
        long cuttingInProgress = cuttings.stream().filter(cutting -> cutting.getStatus() == CuttingStatus.IN_PROGRESS).count();
        int totalPiecesCut = cuttings.stream()
                .filter(cutting -> cutting.getStatus() == CuttingStatus.COMPLETED && cutting.getOutputPieces() != null)
                .mapToInt(CuttingEntry::getOutputPieces)
                .sum();
        int readyToStitchPieces = calculateReadyToStitchPieces(styleAutoId);

        List<StitchedStockResponse> stitchedStock = getStitchedStock(styleAutoId, fromDate, toDate);
        int stitchedStockTotal = stitchedStock.stream().mapToInt(StitchedStockResponse::goodPieces).sum();
        int defectiveStockTotal = stitchedStock.stream().mapToInt(StitchedStockResponse::defectivePieces).sum();

        log.debug("In-house dashboard totals calculated: totalFabricInStock={}, cuttingInProgress={}, totalPiecesCut={}, readyToStitchPieces={}, stitchedStockTotal={}, defectiveStockTotal={}",
                totalFabricInStock, cuttingInProgress, totalPiecesCut, readyToStitchPieces, stitchedStockTotal, defectiveStockTotal);
        return new InHouseDashboardResponse(totalFabricInStock, cuttingInProgress, totalPiecesCut, readyToStitchPieces, stitchedStockTotal, defectiveStockTotal);
    }

    private BigDecimal calculateAvailableStock(UUID diaId, UUID styleId) {
        BigDecimal splitTotal = inHouseStockSplitRepository.sumActiveQuantityByDiaAndStyle(diaId, styleId);
        BigDecimal cuttingTotal = cuttingEntryRepository.sumActiveQuantityByDiaAndStyle(diaId, styleId);
        BigDecimal available = splitTotal.subtract(cuttingTotal);
        log.debug("Available stock calculated for diaId={}, styleId={}: splitTotal={}, cuttingTotal={}, available={}",
                diaId, styleId, splitTotal, cuttingTotal, available);
        return available;
    }

    private boolean canDeleteSplit(InHouseStockSplit split) {
        BigDecimal totalSplit = inHouseStockSplitRepository.sumActiveQuantityByDiaAndStyle(split.getDia().getId(), split.getStyle().getId());
        BigDecimal totalCut = cuttingEntryRepository.sumActiveQuantityByDiaAndStyle(split.getDia().getId(), split.getStyle().getId());
        BigDecimal remainingSplit = totalSplit.subtract(split.getQuantityKgs());
        return remainingSplit.compareTo(totalCut) >= 0;
    }

    private int calculateReadyToStitchPieces(String styleAutoId) {
        int completedCuttingPieces = cuttingEntryRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            predicates.add(cb.equal(root.get("status"), CuttingStatus.COMPLETED));
            if (styleAutoId != null && !styleAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("style").get("autoId")), styleAutoId.trim().toLowerCase()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream()
                .map(CuttingEntry::getOutputPieces)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        int committedToStitching = stitchingOrderRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (styleAutoId != null && !styleAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("style").get("autoId")), styleAutoId.trim().toLowerCase()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }).stream()
                .mapToInt(StitchingOrder::getPiecesOrdered)
                .sum();

        int available = Math.max(completedCuttingPieces - committedToStitching, 0);
        log.debug("Calculated ready-to-stitch pieces for styleAutoId='{}': completedCuttingPieces={}, committedToStitching={}, available={}",
                styleAutoId, completedCuttingPieces, committedToStitching, available);
        return available;
    }

    private void recalculateSplitStatus(InHouseDelivery delivery) {
        BigDecimal allocated = inHouseStockSplitRepository.sumActiveQuantityByDelivery(delivery.getId());
        if (allocated.compareTo(BigDecimal.ZERO) == 0) {
            delivery.setSplitStatus(SplitStatus.PENDING);
        } else if (allocated.compareTo(delivery.getQuantityKgs()) >= 0) {
            delivery.setSplitStatus(SplitStatus.FULLY_SPLIT);
        } else {
            delivery.setSplitStatus(SplitStatus.PARTIALLY_SPLIT);
        }
        inHouseDeliveryRepository.save(delivery);
        log.info("Recalculated split status for delivery '{}': allocated={}, deliveryQuantity={}, status={}",
                delivery.getAutoId(), allocated, delivery.getQuantityKgs(), delivery.getSplitStatus());
    }

    private InHouseDelivery getActiveDelivery(String autoId) {
        return lookupService.getActiveInHouseDeliveryByAutoId(autoId);
    }

    private Specification<InHouseDelivery> deliverySpec(String styleAutoId, LocalDate fromDate, LocalDate toDate, boolean includeDeleted) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
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
            var predicates = new ArrayList<Predicate>();
            if (!includeDeleted) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
            }
            if (diaAutoId != null && !diaAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("dia").get("autoId")), diaAutoId.trim().toLowerCase()));
            }
            if (styleAutoId != null && !styleAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("style").get("autoId")), styleAutoId.trim().toLowerCase()));
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
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private record StockKey(UUID diaId, UUID styleId) {
    }

    private static final class StockBucket {
        private final Dia dia;
        private final Style style;
        private BigDecimal splitQuantity = BigDecimalUtils.ZERO;
        private BigDecimal cuttingQuantity = BigDecimalUtils.ZERO;

        private StockBucket(Dia dia, Style style) {
            this.dia = dia;
            this.style = style;
        }

        private void addSplit(BigDecimal quantity) {
            splitQuantity = splitQuantity.add(quantity);
        }

        private void addCutting(BigDecimal quantity) {
            cuttingQuantity = cuttingQuantity.add(quantity);
        }

        private BigDecimal available() {
            return splitQuantity.subtract(cuttingQuantity);
        }

        private Dia dia() {
            return dia;
        }

        private Style style() {
            return style;
        }
    }

    private static final class StitchedStockBucket {
        private final Style style;
        private int goodPieces;
        private int defectivePieces;
        private LocalDate latestTransactionDate;

        private StitchedStockBucket(Style style) {
            this.style = style;
        }

        private void addGoodPieces(int pieces, LocalDate date) {
            goodPieces += pieces;
            updateLatest(date);
        }

        private void addDefectivePieces(int pieces, LocalDate date) {
            defectivePieces += pieces;
            updateLatest(date);
        }

        private void updateLatest(LocalDate date) {
            if (date == null) {
                return;
            }
            if (latestTransactionDate == null || date.isAfter(latestTransactionDate)) {
                latestTransactionDate = date;
            }
        }

        private Style style() {
            return style;
        }

        private int goodPieces() {
            return goodPieces;
        }

        private int defectivePieces() {
            return defectivePieces;
        }

        private LocalDate latestTransactionDate() {
            return latestTransactionDate;
        }
    }
}
