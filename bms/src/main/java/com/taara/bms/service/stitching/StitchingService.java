package com.taara.bms.service.stitching;

import com.taara.bms.dto.stitching.SectionPendingPiecesResponse;
import com.taara.bms.dto.stitching.StitchingAvailabilityResponse;
import com.taara.bms.dto.stitching.StitchingDashboardResponse;
import com.taara.bms.dto.stitching.StitchingDeliveryCreateRequest;
import com.taara.bms.dto.stitching.StitchingDeliveryResponse;
import com.taara.bms.dto.stitching.StitchingOrderCreateRequest;
import com.taara.bms.dto.stitching.StitchingOrderResponse;
import com.taara.bms.dto.stitching.StitchingOrderRowRequest;
import com.taara.bms.dto.stitching.StitchingOrderStatusUpdateRequest;
import com.taara.bms.entity.masterdata.StitchingSection;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.entity.stitching.StitchingDelivery;
import com.taara.bms.dto.stitching.StitchingOrderUpdateRequest;
import com.taara.bms.entity.stitching.StitchingDeliveryAllocation;
import com.taara.bms.entity.stitching.StitchingOrder;
import com.taara.bms.entity.stitching.StitchingOrderRow;
import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.SectionProcessType;
import com.taara.bms.enums.StitchingOrderStatus;
import com.taara.bms.exception.BusinessValidationException;
import com.taara.bms.exception.DeleteConflictException;
import com.taara.bms.mapper.common.ReferenceMapper;
import com.taara.bms.mapper.stitching.StitchingMapper;
import com.taara.bms.repo.stitching.StitchingDeliveryAllocationRepository;
import com.taara.bms.repo.stitching.StitchingDeliveryRepository;
import com.taara.bms.repo.stitching.StitchingOrderRepository;
import com.taara.bms.service.common.AutoIdSequence;
import com.taara.bms.service.common.AutoIdService;
import com.taara.bms.service.common.LookupService;
import com.taara.bms.service.inhouse.InHouseService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
public class StitchingService {

    private static final Logger log = LoggerFactory.getLogger(StitchingService.class);

    private final StitchingOrderRepository stitchingOrderRepository;
    private final StitchingDeliveryRepository stitchingDeliveryRepository;
    private final StitchingDeliveryAllocationRepository stitchingDeliveryAllocationRepository;
    private final LookupService lookupService;
    private final AutoIdService autoIdService;
    private final StitchingMapper mapper;
    private final ReferenceMapper referenceMapper;
    private final InHouseService inHouseService;

    public StitchingService(
            StitchingOrderRepository stitchingOrderRepository,
            StitchingDeliveryRepository stitchingDeliveryRepository,
            StitchingDeliveryAllocationRepository stitchingDeliveryAllocationRepository,
            LookupService lookupService,
            AutoIdService autoIdService,
            StitchingMapper mapper,
            ReferenceMapper referenceMapper,
            InHouseService inHouseService
    ) {
        this.stitchingOrderRepository = stitchingOrderRepository;
        this.stitchingDeliveryRepository = stitchingDeliveryRepository;
        this.stitchingDeliveryAllocationRepository = stitchingDeliveryAllocationRepository;
        this.lookupService = lookupService;
        this.autoIdService = autoIdService;
        this.mapper = mapper;
        this.referenceMapper = referenceMapper;
        this.inHouseService = inHouseService;
    }

    @Transactional(readOnly = true)
    public Page<StitchingOrderResponse> getOrders(
            String styleAutoId,
            String sectionAutoId,
            StitchingOrderStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching stitching orders with styleAutoId='{}', sectionAutoId='{}', status={}, fromDate={}, toDate={}, includeDeleted={}, pageable={}",
                styleAutoId, sectionAutoId, status, fromDate, toDate, includeDeleted, pageable);
        return stitchingOrderRepository.findAll(orderSpec(styleAutoId, sectionAutoId, status, fromDate, toDate, includeDeleted), pageable)
                .map(order -> mapper.toOrderResponse(order, deliveredPieces(order)));
    }

    @Transactional(readOnly = true)
    public Page<StitchingDeliveryResponse> getDeliveries(
            String styleAutoId,
            String sectionAutoId,
            GarmentSize size,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching stitching deliveries with styleAutoId='{}', sectionAutoId='{}', size={}, fromDate={}, toDate={}, includeDeleted={}, pageable={}",
                styleAutoId, sectionAutoId, size, fromDate, toDate, includeDeleted, pageable);
        return stitchingDeliveryRepository.findAll(deliverySpec(styleAutoId, sectionAutoId, size, fromDate, toDate, includeDeleted), pageable)
                .map(mapper::toDeliveryResponse);
    }

    @Transactional
    public StitchingOrderResponse createOrder(StitchingOrderCreateRequest request) {
        log.info("Creating stitching order on {} with {} rows", request.orderDate(), request.rows().size());
        validateOrderRows(request.rows());
        int totalPiecesTaken = request.rows().stream().mapToInt(StitchingOrderRowRequest::piecesTaken).sum();
        if (request.expectedPieces() > totalPiecesTaken) {
            throw new BusinessValidationException(
                    "EXPECTED_EXCEEDS_TAKEN",
                    "Expected pcs cannot be greater than the total pieces taken for stitching",
                    Map.of("expectedPieces", request.expectedPieces(), "totalPiecesTaken", totalPiecesTaken)
            );
        }

        StitchingOrder order = new StitchingOrder();
        order.setAutoId(autoIdService.next(AutoIdSequence.STITCHING_ORDER));
        order.setOrderDate(request.orderDate());
        order.setExpectedSize(request.expectedSize());
        order.setExpectedPieces(request.expectedPieces());
        order.setStatus(StitchingOrderStatus.PENDING);
        order.setNotes(blankToNull(request.notes()));
        replaceOrderRows(order, request.rows());
        StitchingOrder saved = stitchingOrderRepository.save(order);
        log.info("Created stitching order '{}'", saved.getAutoId());
        return mapper.toOrderResponse(saved, 0);
    }


    @Transactional
    public StitchingOrderResponse updateOrder(String orderAutoId, StitchingOrderUpdateRequest request) {
        log.info("Updating stitching order '{}'", orderAutoId);
        StitchingOrder order = lookupService.getActiveStitchingOrderByAutoId(orderAutoId);

        order.setOrderDate(request.orderDate());
        order.setExpectedSize(request.expectedSize());
        order.setExpectedPieces(request.expectedPieces());
        order.setNotes(blankToNull(request.notes()));

        order.getRows().clear();
        for (StitchingOrderRowRequest rowRequest : request.rows()) {
            StitchingOrderRow row = new StitchingOrderRow();
            row.setStitchingOrder(order);
            row.setStitchingSection(lookupService.getActiveSectionByAutoId(rowRequest.stitchingSectionAutoId()));
            row.setStyle(lookupService.getActiveStyleByAutoId(rowRequest.styleAutoId()));
            row.setSize(rowRequest.size());
            row.setPiecesTaken(rowRequest.piecesTaken());
            row.setRatePerPiece(rowRequest.ratePerPiece());
            order.getRows().add(row);
        }

        StitchingOrder saved = stitchingOrderRepository.save(order);
        log.info("Updated stitching order '{}'", saved.getAutoId());
        return mapper.toOrderResponse(saved, deliveredPieces(saved));
    }

    @Transactional(readOnly = true)
    public StitchingAvailabilityResponse getAvailableOrderPieces(String styleAutoId, GarmentSize size) {
        Style style = lookupService.getActiveStyleByAutoId(styleAutoId);
        int availablePieces = inHouseService.calculateReadyToStitchAvailable(style.getId(), size);
        log.info("Available stitching order pieces for style='{}', size={} => {}", styleAutoId, size, availablePieces);
        return new StitchingAvailabilityResponse(style.getAutoId(), size, availablePieces);
    }

    @Transactional
    public StitchingOrderResponse updateOrderStatus(String orderAutoId, StitchingOrderStatusUpdateRequest request) {
        log.info("Updating stitching order '{}' status to {}", orderAutoId, request.status());
        StitchingOrder order = lookupService.getActiveStitchingOrderByAutoId(orderAutoId);
        order.setStatus(request.status());
        StitchingOrder saved = stitchingOrderRepository.save(order);
        return mapper.toOrderResponse(saved, deliveredPieces(saved));
    }

    @Transactional
    public void deleteOrder(String orderAutoId) {
        log.info("Deleting stitching order '{}'", orderAutoId);
        StitchingOrder order = lookupService.getActiveStitchingOrderByAutoId(orderAutoId);
        if (Objects.requireNonNullElse(stitchingDeliveryAllocationRepository.sumActiveAllocatedByOrder(order.getId()), 0) > 0) {
            throw new DeleteConflictException(
                    "STITCHING_ORDER_HAS_DELIVERIES",
                    "Stitching order cannot be deleted because deliveries already depend on it",
                    Map.of("orderAutoId", order.getAutoId())
            );
        }
        order.setDeleted(true);
        stitchingOrderRepository.save(order);
    }

    @Transactional
    public StitchingDeliveryResponse createDelivery(StitchingDeliveryCreateRequest request) {
        log.info("Creating stitching delivery for section='{}', style='{}', size={}, piecesDelivered={}",
                request.stitchingSectionAutoId(), request.styleAutoId(), request.size(), request.piecesDelivered());
        StitchingSection section = lookupService.getActiveSectionByAutoId(request.stitchingSectionAutoId());
        if (section.getProcessType() != SectionProcessType.STITCHING) {
            throw new BusinessValidationException("INVALID_STITCHING_SECTION", "The selected section is not configured for stitching");
        }
        Style style = lookupService.getActiveStyleByAutoId(request.styleAutoId());
        List<StitchingOrder> matchingOrders = findOpenOrdersForDelivery(section.getId(), style.getId(), request.size());
        int availablePieces = matchingOrders.stream().mapToInt(this::pendingPieces).sum();
        if (request.piecesDelivered() > availablePieces) {
            throw new BusinessValidationException(
                    "DELIVERY_EXCEEDS_PENDING",
                    "Delivered pcs exceed the available pending pcs for this section, style, and size",
                    Map.of(
                            "sectionAutoId", section.getAutoId(),
                            "styleAutoId", style.getAutoId(),
                            "size", request.size(),
                            "availablePieces", availablePieces,
                            "requestedPieces", request.piecesDelivered()
                    )
            );
        }

        StitchingDelivery delivery = new StitchingDelivery();
        delivery.setAutoId(autoIdService.next(AutoIdSequence.STITCHING_DELIVERY));
        delivery.setDeliveryDate(request.deliveryDate());
        delivery.setStitchingSection(section);
        delivery.setStyle(style);
        delivery.setSize(request.size());
        delivery.setPiecesDelivered(request.piecesDelivered());

        int remaining = request.piecesDelivered();
        for (StitchingOrder order : matchingOrders) {
            if (remaining <= 0) {
                break;
            }
            int pending = pendingPieces(order);
            if (pending <= 0) {
                continue;
            }
            int allocated = Math.min(remaining, pending);
            StitchingDeliveryAllocation allocation = new StitchingDeliveryAllocation();
            allocation.setStitchingDelivery(delivery);
            allocation.setStitchingOrder(order);
            allocation.setAllocatedPieces(allocated);
            delivery.getAllocations().add(allocation);
            remaining -= allocated;
        }

        StitchingDelivery savedDelivery = stitchingDeliveryRepository.save(delivery);
        for (StitchingDeliveryAllocation allocation : savedDelivery.getAllocations()) {
            updateOrderStatusFromAllocations(allocation.getStitchingOrder());
        }
        log.info("Created stitching delivery '{}' with {} allocations", savedDelivery.getAutoId(), savedDelivery.getAllocations().size());
        return mapper.toDeliveryResponse(savedDelivery);
    }

    @Transactional(readOnly = true)
    public StitchingAvailabilityResponse getAvailableDeliveryPieces(String sectionAutoId, String styleAutoId, GarmentSize size) {
        StitchingSection section = lookupService.getActiveSectionByAutoId(sectionAutoId);
        Style style = lookupService.getActiveStyleByAutoId(styleAutoId);
        int availablePieces = findOpenOrdersForDelivery(section.getId(), style.getId(), size).stream().mapToInt(this::pendingPieces).sum();
        log.info("Available stitching delivery pieces for section='{}', style='{}', size={} => {}",
                sectionAutoId, styleAutoId, size, availablePieces);
        return new StitchingAvailabilityResponse(style.getAutoId(), size, availablePieces);
    }

    @Transactional
    public void deleteDelivery(String deliveryAutoId) {
        log.info("Deleting stitching delivery '{}'", deliveryAutoId);
        StitchingDelivery delivery = lookupService.getActiveStitchingDeliveryByAutoId(deliveryAutoId);
        List<StitchingOrder> affectedOrders = delivery.getAllocations().stream()
                .map(StitchingDeliveryAllocation::getStitchingOrder)
                .distinct()
                .toList();
        delivery.setDeleted(true);
        stitchingDeliveryRepository.save(delivery);
        affectedOrders.forEach(this::updateOrderStatusFromAllocations);
    }

    @Transactional(readOnly = true)
    public StitchingDashboardResponse getDashboard(
            String styleAutoId,
            String sectionAutoId,
            StitchingOrderStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted
    ) {
        log.info("Calculating stitching dashboard with styleAutoId='{}', sectionAutoId='{}', status={}, fromDate={}, toDate={}, includeDeleted={}",
                styleAutoId, sectionAutoId, status, fromDate, toDate, includeDeleted);
        List<StitchingOrder> orders = stitchingOrderRepository.findAll(orderSpec(styleAutoId, sectionAutoId, status, fromDate, toDate, includeDeleted));
        List<StitchingDelivery> deliveries = stitchingDeliveryRepository.findAll(deliverySpec(styleAutoId, sectionAutoId, null, fromDate, toDate, includeDeleted));

        int totalPiecesInStitching = orders.stream()
                .filter(order -> order.getStatus() == StitchingOrderStatus.PENDING || order.getStatus() == StitchingOrderStatus.PARTIALLY_DELIVERED)
                .mapToInt(this::pendingPieces)
                .sum();
        int deliveredPieces = deliveries.stream().mapToInt(StitchingDelivery::getPiecesDelivered).sum();
        long pendingOrdersCount = orders.stream().filter(order -> order.getStatus() == StitchingOrderStatus.PENDING).count();
        long partialOrdersCount = orders.stream().filter(order -> order.getStatus() == StitchingOrderStatus.PARTIALLY_DELIVERED).count();
        int defectivePieces = orders.stream().mapToInt(this::calculateDefectivePieces).sum();

        Map<UUID, SectionBucket> sectionBuckets = new LinkedHashMap<>();
        for (StitchingOrder order : orders) {
            StitchingSection primarySection = resolvePrimarySection(order);
            if (primarySection == null) {
                continue;
            }
            if (pendingPieces(order) <= 0) {
                continue;
            }
            sectionBuckets.computeIfAbsent(primarySection.getId(), ignored -> new SectionBucket(primarySection))
                    .addPendingPieces(pendingPieces(order));
        }

        List<SectionPendingPiecesResponse> ordersBySection = sectionBuckets.values().stream()
                .map(bucket -> new SectionPendingPiecesResponse(referenceMapper.toSectionRef(bucket.section()), bucket.pendingPieces()))
                .sorted(Comparator.comparing(response -> response.section().sectionName()))
                .toList();

        return new StitchingDashboardResponse(
                totalPiecesInStitching,
                deliveredPieces,
                pendingOrdersCount,
                partialOrdersCount,
                defectivePieces,
                ordersBySection
        );
    }

    private void validateOrderRows(List<StitchingOrderRowRequest> rows) {
        Map<StyleSizeKey, Integer> requestedByCombo = new LinkedHashMap<>();
        for (StitchingOrderRowRequest row : rows) {
            StitchingSection section = lookupService.getActiveSectionByAutoId(row.stitchingSectionAutoId());
            if (section.getProcessType() != SectionProcessType.STITCHING) {
                throw new BusinessValidationException("INVALID_STITCHING_SECTION", "The selected section is not configured for stitching");
            }
            Style style = lookupService.getActiveStyleByAutoId(row.styleAutoId());
            requestedByCombo.merge(new StyleSizeKey(style.getId(), row.size()), row.piecesTaken(), Integer::sum);
        }

        for (Map.Entry<StyleSizeKey, Integer> entry : requestedByCombo.entrySet()) {
            int availablePieces = inHouseService.calculateReadyToStitchAvailable(entry.getKey().styleId(), entry.getKey().size());
            if (entry.getValue() > availablePieces) {
                throw new BusinessValidationException(
                        "STITCHING_ORDER_EXCEEDS_AVAILABLE",
                        "Pieces taken exceed the available ready-to-stitch pieces for the selected style and size",
                        Map.of(
                                "styleId", entry.getKey().styleId(),
                                "size", entry.getKey().size(),
                                "availablePieces", availablePieces,
                                "requestedPieces", entry.getValue()
                        )
                );
            }
        }
    }

    private void replaceOrderRows(StitchingOrder order, List<StitchingOrderRowRequest> rows) {
        order.getRows().clear();
        for (StitchingOrderRowRequest rowRequest : rows) {
            StitchingOrderRow row = new StitchingOrderRow();
            row.setStitchingOrder(order);
            row.setStitchingSection(lookupService.getActiveSectionByAutoId(rowRequest.stitchingSectionAutoId()));
            row.setStyle(lookupService.getActiveStyleByAutoId(rowRequest.styleAutoId()));
            row.setSize(rowRequest.size());
            row.setPiecesTaken(rowRequest.piecesTaken());
            order.getRows().add(row);
        }
    }

    private List<StitchingOrder> findOpenOrdersForDelivery(UUID sectionId, UUID styleId, GarmentSize size) {
        return stitchingOrderRepository.findAll().stream()
                .filter(order -> !order.isDeleted())
                .filter(order -> order.getStatus() != StitchingOrderStatus.COMPLETE && order.getStatus() != StitchingOrderStatus.AUTO_CLOSED)
                .filter(order -> order.getExpectedSize() == size)
                .filter(order -> order.getRows().stream().anyMatch(row ->
                        row.getStitchingSection().getId().equals(sectionId) && row.getStyle().getId().equals(styleId)))
                .sorted(Comparator.comparing(StitchingOrder::getOrderDate).thenComparing(StitchingOrder::getCreatedAt))
                .toList();
    }

    private int deliveredPieces(StitchingOrder order) {
        return Objects.requireNonNullElse(stitchingDeliveryAllocationRepository.sumActiveAllocatedByOrder(order.getId()), 0);
    }

    private int pendingPieces(StitchingOrder order) {
        return Math.max(order.getExpectedPieces() - deliveredPieces(order), 0);
    }

    private int calculateDefectivePieces(StitchingOrder order) {
        if (order.getStatus() != StitchingOrderStatus.COMPLETE) {
            return 0;
        }
        return pendingPieces(order);
    }

    private void updateOrderStatusFromAllocations(StitchingOrder order) {
        int delivered = deliveredPieces(order);
        if (delivered >= order.getExpectedPieces()) {
            order.setStatus(StitchingOrderStatus.AUTO_CLOSED);
        } else if (delivered > 0) {
            order.setStatus(StitchingOrderStatus.PARTIALLY_DELIVERED);
        } else {
            order.setStatus(StitchingOrderStatus.PENDING);
        }
        stitchingOrderRepository.save(order);
        log.debug("Updated stitching order '{}' status from allocations. delivered={}, expected={}, status={}",
                order.getAutoId(), delivered, order.getExpectedPieces(), order.getStatus());
    }

    private StitchingSection resolvePrimarySection(StitchingOrder order) {
        return order.getRows().stream().findFirst().map(StitchingOrderRow::getStitchingSection).orElse(null);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Specification<StitchingOrder> orderSpec(
            String styleAutoId,
            String sectionAutoId,
            StitchingOrderStatus status,
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
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), toDate));
            }
            Join<StitchingOrder, StitchingOrderRow> rowJoin = root.join("rows", JoinType.LEFT);
            if (styleAutoId != null && !styleAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(rowJoin.get("style").get("autoId")), styleAutoId.trim().toLowerCase()));
            }
            if (sectionAutoId != null && !sectionAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(rowJoin.get("stitchingSection").get("autoId")), sectionAutoId.trim().toLowerCase()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<StitchingDelivery> deliverySpec(
            String styleAutoId,
            String sectionAutoId,
            GarmentSize size,
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
            if (sectionAutoId != null && !sectionAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("stitchingSection").get("autoId")), sectionAutoId.trim().toLowerCase()));
            }
            if (size != null) {
                predicates.add(cb.equal(root.get("size"), size));
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

    private record StyleSizeKey(UUID styleId, GarmentSize size) {
    }

    private static final class SectionBucket {
        private final StitchingSection section;
        private int pendingPieces;

        private SectionBucket(StitchingSection section) {
            this.section = section;
        }

        private void addPendingPieces(int pieces) {
            pendingPieces += pieces;
        }

        private StitchingSection section() {
            return section;
        }

        private int pendingPieces() {
            return pendingPieces;
        }
    }
}
