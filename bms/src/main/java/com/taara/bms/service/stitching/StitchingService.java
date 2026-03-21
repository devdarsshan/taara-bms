package com.taara.bms.service.stitching;

import com.taara.bms.dto.stitching.SectionPendingPiecesResponse;
import com.taara.bms.dto.stitching.StitchingAvailabilityResponse;
import com.taara.bms.dto.stitching.StitchingDashboardResponse;
import com.taara.bms.dto.stitching.StitchingDeliveryCreateRequest;
import com.taara.bms.dto.stitching.StitchingDeliveryResponse;
import com.taara.bms.dto.stitching.StitchingOrderCreateRequest;
import com.taara.bms.dto.stitching.StitchingOrderResponse;
import com.taara.bms.dto.stitching.StitchingOrderStatusUpdateRequest;
import com.taara.bms.entity.masterdata.StitchingSection;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.entity.inhouse.CuttingEntry;
import com.taara.bms.entity.stitching.StitchingDelivery;
import com.taara.bms.entity.stitching.StitchingOrder;
import com.taara.bms.enums.StitchingOrderStatus;
import com.taara.bms.enums.WarningCode;
import com.taara.bms.exception.BusinessValidationException;
import com.taara.bms.exception.DeleteConflictException;
import com.taara.bms.exception.ResourceNotFoundException;
import com.taara.bms.exception.WarningRequiredException;
import com.taara.bms.mapper.common.ReferenceMapper;
import com.taara.bms.mapper.stitching.StitchingMapper;
import com.taara.bms.repo.inhouse.CuttingEntryRepository;
import com.taara.bms.repo.stitching.StitchingDeliveryRepository;
import com.taara.bms.repo.stitching.StitchingOrderRepository;
import com.taara.bms.service.common.AutoIdSequence;
import com.taara.bms.service.common.AutoIdService;
import com.taara.bms.service.common.LookupService;
import jakarta.persistence.criteria.Predicate;
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
public class StitchingService {

    private static final Logger log = LoggerFactory.getLogger(StitchingService.class);

    private final StitchingOrderRepository stitchingOrderRepository;
    private final StitchingDeliveryRepository stitchingDeliveryRepository;
    private final CuttingEntryRepository cuttingEntryRepository;
    private final LookupService lookupService;
    private final AutoIdService autoIdService;
    private final StitchingMapper mapper;
    private final ReferenceMapper referenceMapper;

    public StitchingService(
            StitchingOrderRepository stitchingOrderRepository,
            StitchingDeliveryRepository stitchingDeliveryRepository,
            CuttingEntryRepository cuttingEntryRepository,
            LookupService lookupService,
            AutoIdService autoIdService,
            StitchingMapper mapper,
            ReferenceMapper referenceMapper
    ) {
        this.stitchingOrderRepository = stitchingOrderRepository;
        this.stitchingDeliveryRepository = stitchingDeliveryRepository;
        this.cuttingEntryRepository = cuttingEntryRepository;
        this.lookupService = lookupService;
        this.autoIdService = autoIdService;
        this.mapper = mapper;
        this.referenceMapper = referenceMapper;
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
        Page<StitchingOrderResponse> orders = stitchingOrderRepository.findAll(orderSpec(styleAutoId, sectionAutoId, status, fromDate, toDate, includeDeleted), pageable)
                .map(order -> mapper.toOrderResponse(order, deliveredPieces(order.getId())));
        log.info("Fetched {} stitching orders", orders.getNumberOfElements());
        return orders;
    }

    @Transactional(readOnly = true)
    public Page<StitchingDeliveryResponse> getDeliveries(
            String orderAutoId,
            String styleAutoId,
            String sectionAutoId,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching stitching deliveries with orderAutoId='{}', styleAutoId='{}', sectionAutoId='{}', fromDate={}, toDate={}, includeDeleted={}, pageable={}",
                orderAutoId, styleAutoId, sectionAutoId, fromDate, toDate, includeDeleted, pageable);
        Page<StitchingDeliveryResponse> deliveries = stitchingDeliveryRepository.findAll(
                        deliverySpec(orderAutoId, styleAutoId, sectionAutoId, fromDate, toDate, includeDeleted), pageable)
                .map(mapper::toDeliveryResponse);
        log.info("Fetched {} stitching deliveries", deliveries.getNumberOfElements());
        return deliveries;
    }

    @Transactional
    public StitchingOrderResponse createOrder(StitchingOrderCreateRequest request) {
        log.info("Creating stitching order for styleAutoId='{}', sectionAutoId='{}', orderDate={}",
                request.styleAutoId(), request.stitchingSectionAutoId(), request.orderDate());
        StitchingSection section = lookupService.getActiveSectionByAutoId(request.stitchingSectionAutoId());
        Style style = lookupService.getActiveStyleByAutoId(request.styleAutoId());
        int availablePieces = calculateAvailableOrderPieces(style);
        log.debug("Available order pieces for style '{}': {}", style.getAutoId(), availablePieces);
        if (availablePieces <= 0) {
            throw new BusinessValidationException(
                    "NO_STITCHING_PIECES_AVAILABLE",
                    "No pieces are available to move into stitching for this style",
                    Map.of("styleAutoId", style.getAutoId(), "availablePieces", availablePieces)
            );
        }
        if (request.piecesOrdered() > availablePieces) {
            throw new BusinessValidationException(
                    "STITCHING_ORDER_EXCEEDS_AVAILABLE_PIECES",
                    "Pieces ordered exceed the available pieces for this style",
                    Map.of("styleAutoId", style.getAutoId(), "availablePieces", availablePieces, "requestedPieces", request.piecesOrdered())
            );
        }

        StitchingOrder order = new StitchingOrder();
        order.setAutoId(autoIdService.next(AutoIdSequence.STITCHING_ORDER));
        order.setOrderDate(request.orderDate());
        order.setStitchingSection(section);
        order.setStyle(style);
        order.setPiecesOrdered(request.piecesOrdered());
        order.setStatus(StitchingOrderStatus.PENDING);
        order.setNotes(request.notes());
        StitchingOrderResponse response = mapper.toOrderResponse(stitchingOrderRepository.save(order), 0);
        log.info("Created stitching order '{}'", response.autoId());
        return response;
    }

    @Transactional(readOnly = true)
    public StitchingAvailabilityResponse getAvailableOrderPieces(String styleAutoId) {
        Style style = lookupService.getActiveStyleByAutoId(styleAutoId);
        int availablePieces = calculateAvailableOrderPieces(style);
        log.info("Available stitching order pieces for style '{}': {}", styleAutoId, availablePieces);
        return new StitchingAvailabilityResponse(style.getAutoId(), availablePieces);
    }

    @Transactional
    public StitchingOrderResponse updateOrderStatus(String orderAutoId, StitchingOrderStatusUpdateRequest request) {
        log.info("Updating stitching order '{}' to status={}", orderAutoId, request.status());
        StitchingOrder order = lookupService.getActiveStitchingOrderByAutoId(orderAutoId);
        order.setStatus(request.status());
        StitchingOrder saved = stitchingOrderRepository.save(order);
        StitchingOrderResponse response = mapper.toOrderResponse(saved, deliveredPieces(saved.getId()));
        log.info("Updated stitching order '{}' to status={}", orderAutoId, response.status());
        return response;
    }

    @Transactional
    public void deleteOrder(String orderAutoId) {
        log.info("Deleting stitching order '{}'", orderAutoId);
        StitchingOrder order = lookupService.getActiveStitchingOrderByAutoId(orderAutoId);
        if (stitchingDeliveryRepository.existsByStitchingOrder_IdAndIsDeletedFalse(order.getId())) {
            throw new DeleteConflictException(
                    "STITCHING_ORDER_HAS_DELIVERIES",
                    "Stitching order cannot be deleted because deliveries already exist",
                    Map.of("orderAutoId", order.getAutoId())
            );
        }
        order.setDeleted(true);
        stitchingOrderRepository.save(order);
        log.info("Deleted stitching order '{}'", orderAutoId);
    }

    @Transactional
    public StitchingDeliveryResponse createDelivery(StitchingDeliveryCreateRequest request) {
        log.info("Creating stitching delivery for orderAutoId='{}' on deliveryDate={}, overrideWarnings={}",
                request.stitchingOrderAutoId(), request.deliveryDate(), request.overrideWarnings());
        StitchingOrder order = lookupService.getActiveStitchingOrderByAutoId(request.stitchingOrderAutoId());
        int availablePieces = calculateAvailableDeliveryPieces(order);
        log.debug("Available delivery pieces for order '{}': {}", order.getAutoId(), availablePieces);
        if (availablePieces <= 0) {
            throw new BusinessValidationException(
                    "NO_STITCHING_DELIVERY_PIECES_AVAILABLE",
                    "No pieces are available to deliver for this stitching order",
                    Map.of("orderAutoId", order.getAutoId(), "availablePieces", availablePieces)
            );
        }

        int deliveredBefore = deliveredPieces(order.getId());
        int deliveredAfter = deliveredBefore + request.piecesDelivered();
        log.debug("Stitching delivery guardrail for order '{}': piecesOrdered={}, deliveredBefore={}, incoming={}, deliveredAfter={}",
                order.getAutoId(), order.getPiecesOrdered(), deliveredBefore, request.piecesDelivered(), deliveredAfter);
        if (deliveredAfter > order.getPiecesOrdered() && !request.overrideWarnings()) {
            throw new WarningRequiredException(
                    WarningCode.STITCHING_DELIVERY_EXCEEDS_ORDER,
                    "Pieces delivered exceed pieces ordered for this stitching order",
                    Map.of(
                            "orderAutoId", order.getAutoId(),
                            "piecesOrdered", order.getPiecesOrdered(),
                            "deliveredBefore", deliveredBefore,
                            "deliveredAfter", deliveredAfter
                    )
            );
        }

        StitchingDelivery delivery = new StitchingDelivery();
        delivery.setAutoId(autoIdService.next(AutoIdSequence.STITCHING_DELIVERY));
        delivery.setDeliveryDate(request.deliveryDate());
        delivery.setStitchingOrder(order);
        delivery.setPiecesDelivered(request.piecesDelivered());
        StitchingDeliveryResponse response = mapper.toDeliveryResponse(stitchingDeliveryRepository.save(delivery));
        log.info("Created stitching delivery '{}'", response.autoId());
        return response;
    }

    @Transactional(readOnly = true)
    public StitchingAvailabilityResponse getAvailableDeliveryPieces(String orderAutoId) {
        StitchingOrder order = lookupService.getActiveStitchingOrderByAutoId(orderAutoId);
        int availablePieces = calculateAvailableDeliveryPieces(order);
        log.info("Available stitching delivery pieces for order '{}': {}", orderAutoId, availablePieces);
        return new StitchingAvailabilityResponse(order.getStyle().getAutoId(), availablePieces);
    }

    @Transactional
    public void deleteDelivery(String deliveryAutoId) {
        log.info("Deleting stitching delivery '{}'", deliveryAutoId);
        StitchingDelivery delivery = lookupService.getActiveStitchingDeliveryByAutoId(deliveryAutoId);
        delivery.setDeleted(true);
        stitchingDeliveryRepository.save(delivery);
        log.info("Deleted stitching delivery '{}'", deliveryAutoId);
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
        List<StitchingOrder> filteredOrders = stitchingOrderRepository.findAll(orderSpec(styleAutoId, sectionAutoId, status, fromDate, toDate, includeDeleted));
        List<StitchingDelivery> filteredDeliveries = stitchingDeliveryRepository.findAll(deliverySpec(null, styleAutoId, sectionAutoId, fromDate, toDate, includeDeleted));

        int totalPiecesInStitching = filteredOrders.stream()
                .filter(order -> order.getStatus() == StitchingOrderStatus.PENDING || order.getStatus() == StitchingOrderStatus.PARTIALLY_DELIVERED)
                .mapToInt(order -> Math.max(order.getPiecesOrdered() - deliveredPieces(order.getId()), 0))
                .sum();
        int piecesDelivered = filteredDeliveries.stream().mapToInt(StitchingDelivery::getPiecesDelivered).sum();
        long pendingOrdersCount = filteredOrders.stream().filter(order -> order.getStatus() == StitchingOrderStatus.PENDING).count();
        long partiallyDeliveredCount = filteredOrders.stream().filter(order -> order.getStatus() == StitchingOrderStatus.PARTIALLY_DELIVERED).count();
        int defectivePieces = filteredOrders.stream()
                .filter(order -> order.getStatus() == StitchingOrderStatus.COMPLETE)
                .mapToInt(order -> Math.max(order.getPiecesOrdered() - deliveredPieces(order.getId()), 0))
                .sum();

        Map<UUID, SectionBucket> sectionBuckets = new LinkedHashMap<>();
        for (StitchingOrder order : filteredOrders) {
            if (order.getStatus() != StitchingOrderStatus.PENDING && order.getStatus() != StitchingOrderStatus.PARTIALLY_DELIVERED) {
                continue;
            }
            int pendingPieces = Math.max(order.getPiecesOrdered() - deliveredPieces(order.getId()), 0);
            sectionBuckets.computeIfAbsent(order.getStitchingSection().getId(), ignored -> new SectionBucket(order.getStitchingSection()))
                    .addPendingPieces(pendingPieces);
        }

        List<SectionPendingPiecesResponse> ordersBySection = sectionBuckets.values().stream()
                .map(bucket -> new SectionPendingPiecesResponse(referenceMapper.toSectionRef(bucket.section()), bucket.pendingPieces()))
                .sorted(Comparator.comparing(response -> response.section().sectionName()))
                .toList();

        log.debug("Stitching dashboard totals calculated: totalPiecesInStitching={}, piecesDelivered={}, pendingOrdersCount={}, partiallyDeliveredCount={}, defectivePieces={}, sectionBuckets={}",
                totalPiecesInStitching, piecesDelivered, pendingOrdersCount, partiallyDeliveredCount, defectivePieces, ordersBySection.size());
        return new StitchingDashboardResponse(
                totalPiecesInStitching,
                piecesDelivered,
                pendingOrdersCount,
                partiallyDeliveredCount,
                defectivePieces,
                ordersBySection
        );
    }

    private int deliveredPieces(UUID orderId) {
        int deliveredPieces = Objects.requireNonNullElse(stitchingDeliveryRepository.sumActivePiecesByOrder(orderId), 0);
        log.debug("Delivered pieces calculated for orderId={}: {}", orderId, deliveredPieces);
        return deliveredPieces;
    }

    private int calculateAvailableOrderPieces(Style style) {
        int completedCuttingPieces = cuttingEntryRepository.findAll((root, query, cb) -> cb.and(
                cb.isFalse(root.get("isDeleted")),
                cb.equal(root.get("style").get("id"), style.getId()),
                cb.equal(root.get("status"), com.taara.bms.enums.CuttingStatus.COMPLETED)
        )).stream()
                .map(CuttingEntry::getOutputPieces)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        int committedToStitching = stitchingOrderRepository.findAll((root, query, cb) -> cb.and(
                cb.isFalse(root.get("isDeleted")),
                cb.equal(root.get("style").get("id"), style.getId())
        )).stream()
                .mapToInt(StitchingOrder::getPiecesOrdered)
                .sum();

        int available = Math.max(completedCuttingPieces - committedToStitching, 0);
        log.debug("Calculated available order pieces for style '{}': completedCuttingPieces={}, committedToStitching={}, available={}",
                style.getAutoId(), completedCuttingPieces, committedToStitching, available);
        return available;
    }

    private int calculateAvailableDeliveryPieces(StitchingOrder order) {
        int available = Math.max(order.getPiecesOrdered() - deliveredPieces(order.getId()), 0);
        log.debug("Calculated available delivery pieces for order '{}': {}", order.getAutoId(), available);
        return available;
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
            var predicates = new ArrayList<Predicate>();
            if (!includeDeleted) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
            }
            if (styleAutoId != null && !styleAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("style").get("autoId")), styleAutoId.trim().toLowerCase()));
            }
            if (sectionAutoId != null && !sectionAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("stitchingSection").get("autoId")), sectionAutoId.trim().toLowerCase()));
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
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<StitchingDelivery> deliverySpec(
            String orderAutoId,
            String styleAutoId,
            String sectionAutoId,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted
    ) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (!includeDeleted) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
            }
            if (orderAutoId != null && !orderAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("stitchingOrder").get("autoId")), orderAutoId.trim().toLowerCase()));
            }
            if (styleAutoId != null && !styleAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("stitchingOrder").get("style").get("autoId")), styleAutoId.trim().toLowerCase()));
            }
            if (sectionAutoId != null && !sectionAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("stitchingOrder").get("stitchingSection").get("autoId")), sectionAutoId.trim().toLowerCase()));
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
