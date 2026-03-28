package com.taara.bms.service.spinning;

import com.taara.bms.dto.spinning.SpinningDashboardResponse;
import com.taara.bms.dto.spinning.SpinningDeliveryCreateRequest;
import com.taara.bms.dto.spinning.SpinningDeliveryResponse;
import com.taara.bms.dto.spinning.SpinningOrderCreateRequest;
import com.taara.bms.dto.spinning.SpinningOrderResponse;
import com.taara.bms.entity.inhouse.InHouseDelivery;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.entity.spinning.SpinningDelivery;
import com.taara.bms.entity.spinning.SpinningOrder;
import com.taara.bms.entity.yarn.YarnOrder;
import com.taara.bms.enums.SplitStatus;
import com.taara.bms.exception.BusinessValidationException;
import com.taara.bms.exception.DeleteConflictException;
import com.taara.bms.exception.ResourceNotFoundException;
import com.taara.bms.helper.BigDecimalUtils;
import com.taara.bms.mapper.spinning.SpinningMapper;
import com.taara.bms.repo.inhouse.InHouseDeliveryRepository;
import com.taara.bms.repo.inhouse.InHouseStockSplitRepository;
import com.taara.bms.repo.spinning.SpinningDeliveryRepository;
import com.taara.bms.repo.spinning.SpinningOrderRepository;
import com.taara.bms.service.common.AutoIdSequence;
import com.taara.bms.service.common.AutoIdService;
import com.taara.bms.service.common.LookupService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpinningService {

    private static final Logger log = LoggerFactory.getLogger(SpinningService.class);

    private final SpinningOrderRepository spinningOrderRepository;
    private final SpinningDeliveryRepository spinningDeliveryRepository;
    private final InHouseDeliveryRepository inHouseDeliveryRepository;
    private final InHouseStockSplitRepository inHouseStockSplitRepository;
    private final LookupService lookupService;
    private final AutoIdService autoIdService;
    private final SpinningMapper mapper;

    public SpinningService(
            SpinningOrderRepository spinningOrderRepository,
            SpinningDeliveryRepository spinningDeliveryRepository,
            InHouseDeliveryRepository inHouseDeliveryRepository,
            InHouseStockSplitRepository inHouseStockSplitRepository,
            LookupService lookupService,
            AutoIdService autoIdService,
            SpinningMapper mapper
    ) {
        this.spinningOrderRepository = spinningOrderRepository;
        this.spinningDeliveryRepository = spinningDeliveryRepository;
        this.inHouseDeliveryRepository = inHouseDeliveryRepository;
        this.inHouseStockSplitRepository = inHouseStockSplitRepository;
        this.lookupService = lookupService;
        this.autoIdService = autoIdService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<SpinningOrderResponse> getOrders(
            String styleAutoId,
            LocalDate fromDate,
            LocalDate toDate,
            Boolean linkedYarnOrder,
            boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching spinning orders with styleAutoId='{}', fromDate={}, toDate={}, linkedYarnOrder={}, includeDeleted={}, pageable={}",
                styleAutoId, fromDate, toDate, linkedYarnOrder, includeDeleted, pageable);
        Page<SpinningOrderResponse> orders = spinningOrderRepository.findAll(orderSpec(styleAutoId, fromDate, toDate, linkedYarnOrder, includeDeleted), pageable)
                .map(mapper::toOrderResponse);
        log.info("Fetched {} spinning orders", orders.getNumberOfElements());
        return orders;
    }

    @Transactional(readOnly = true)
    public Page<SpinningDeliveryResponse> getDeliveries(
            String styleAutoId,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching spinning deliveries with styleAutoId='{}', fromDate={}, toDate={}, includeDeleted={}, pageable={}",
                styleAutoId, fromDate, toDate, includeDeleted, pageable);
        Page<SpinningDeliveryResponse> deliveries = spinningDeliveryRepository.findAll(deliverySpec(styleAutoId, fromDate, toDate, includeDeleted), pageable)
                .map(mapper::toDeliveryResponse);
        log.info("Fetched {} spinning deliveries", deliveries.getNumberOfElements());
        return deliveries;
    }

    @Transactional(readOnly = true)
    public SpinningDashboardResponse getDashboard(String styleAutoId, LocalDate fromDate, LocalDate toDate, boolean includeDeleted) {
        log.info("Calculating spinning dashboard with styleAutoId='{}', fromDate={}, toDate={}, includeDeleted={}",
                styleAutoId, fromDate, toDate, includeDeleted);
        BigDecimal totalDispatched = spinningOrderRepository.findAll(orderSpec(styleAutoId, fromDate, toDate, null, includeDeleted))
                .stream()
                .map(SpinningOrder::getQuantitySentKgs)
                .reduce(BigDecimalUtils.ZERO, BigDecimal::add);
        BigDecimal totalReceived = spinningDeliveryRepository.findAll(deliverySpec(styleAutoId, fromDate, toDate, includeDeleted))
                .stream()
                .map(SpinningDelivery::getFinalQuantityKgs)
                .reduce(BigDecimalUtils.ZERO, BigDecimal::add);
        BigDecimal overallPending = spinningOrderRepository.sumActiveQuantityByStyle(null)
                .subtract(spinningDeliveryRepository.sumActiveFinalQuantityByStyle(null));
        log.debug("Spinning dashboard totals calculated: dispatched={}, received={}, pending={}",
                totalDispatched, totalReceived, overallPending);
        return new SpinningDashboardResponse(totalDispatched, totalReceived, overallPending);
    }

    @Transactional
    public SpinningOrderResponse createOrder(SpinningOrderCreateRequest request) {
        log.info("Creating spinning order for styleAutoId='{}' on dispatchDate={}", request.styleAutoId(), request.dispatchDate());
        Style style = lookupService.getActiveStyleByAutoId(request.styleAutoId());
        BigDecimal quantity = BigDecimalUtils.scale(request.quantitySentKgs());
        if (quantity.signum() <= 0) {
            throw new BusinessValidationException("INVALID_SPINNING_QUANTITY", "Quantity sent must be positive");
        }
        log.debug("Validated spinning order quantity={} for style='{}'", quantity, style.getAutoId());

        YarnOrder linkedYarnOrder = null;
        if (request.linkedYarnOrderAutoId() != null && !request.linkedYarnOrderAutoId().isBlank()) {
            linkedYarnOrder = lookupService.getActiveYarnOrderByAutoId(request.linkedYarnOrderAutoId());
            log.debug("Resolved linked yarn order '{}'", linkedYarnOrder.getAutoId());
            if (!linkedYarnOrder.getStyle().getId().equals(style.getId())) {
                throw new BusinessValidationException("STYLE_MISMATCH", "Linked yarn order style must match the spinning order style");
            }
        }

        SpinningOrder order = new SpinningOrder();
        order.setAutoId(autoIdService.next(AutoIdSequence.SPINNING_ORDER));
        order.setDispatchDate(request.dispatchDate());
        order.setLinkedYarnOrder(linkedYarnOrder);
        order.setStyle(style);
        order.setQuantitySentKgs(quantity);
        order.setFactoryNotes(request.factoryNotes());
        order.setAutoCreated(false);
        SpinningOrderResponse response = mapper.toOrderResponse(spinningOrderRepository.save(order));
        log.info("Created spinning order '{}'", response.autoId());
        return response;
    }

    @Transactional
    public void deleteOrder(String autoId) {
        log.info("Deleting spinning order '{}'", autoId);
        SpinningOrder order = lookupService.getActiveSpinningOrderByAutoId(autoId);
        ensureDeletionSafe(order);
        order.setDeleted(true);
        spinningOrderRepository.save(order);
        log.info("Deleted spinning order '{}'", autoId);
    }

    @Transactional
    public SpinningDeliveryResponse createDelivery(SpinningDeliveryCreateRequest request) {
        log.info("Creating spinning delivery for styleAutoId='{}' on deliveryDate={}", request.styleAutoId(), request.deliveryDate());
        Style style = lookupService.getActiveStyleByAutoId(request.styleAutoId());
        BigDecimal actualQuantity = BigDecimalUtils.scale(request.actualQuantityKgs());
        BigDecimal buffer = BigDecimalUtils.scale(request.bufferQuantityKgs());
        BigDecimal finalQuantity = actualQuantity.add(buffer);
        log.debug("Calculated spinning delivery quantities: actual={}, buffer={}, final={}", actualQuantity, buffer, finalQuantity);
        if (finalQuantity.signum() <= 0) {
            throw new BusinessValidationException("INVALID_FINAL_QUANTITY", "Final quantity must be positive");
        }

        BigDecimal totalDispatched = spinningOrderRepository.sumActiveQuantityByStyle(style.getId());
        BigDecimal totalReceived = spinningDeliveryRepository.sumActiveFinalQuantityByStyle(style.getId());
        log.debug("Spinning delivery guardrail for style '{}': dispatched={}, received={}, incomingFinal={}",
                style.getAutoId(), totalDispatched, totalReceived, finalQuantity);
        if (totalReceived.add(finalQuantity).compareTo(totalDispatched) > 0) {
            throw new BusinessValidationException(
                    "SPINNING_DELIVERY_EXCEEDS_DISPATCHED",
                    "Spinning delivery exceeds dispatched quantity for this style",
                    Map.of(
                            "styleAutoId", style.getAutoId(),
                            "dispatched", totalDispatched,
                            "received", totalReceived,
                            "incomingFinalQuantity", finalQuantity
                    )
            );
        }

        SpinningDelivery delivery = new SpinningDelivery();
        delivery.setAutoId(autoIdService.next(AutoIdSequence.SPINNING_DELIVERY));
        delivery.setDeliveryDate(request.deliveryDate());
        delivery.setStyle(style);
        delivery.setActualQuantityKgs(actualQuantity);
        delivery.setBufferQuantityKgs(buffer);
        delivery.setFinalQuantityKgs(finalQuantity);
        delivery.setNotes(request.notes());
        SpinningDelivery savedDelivery = spinningDeliveryRepository.save(delivery);
        log.info("Created spinning delivery '{}'", savedDelivery.getAutoId());

        InHouseDelivery inHouseDelivery = new InHouseDelivery();
        inHouseDelivery.setAutoId(autoIdService.next(AutoIdSequence.INHOUSE_DELIVERY));
        inHouseDelivery.setDeliveryDate(request.deliveryDate());
        inHouseDelivery.setSpinningDelivery(savedDelivery);
        inHouseDelivery.setStyle(style);
        inHouseDelivery.setQuantityKgs(finalQuantity);
        inHouseDelivery.setSplitStatus(SplitStatus.PENDING);
        inHouseDeliveryRepository.save(inHouseDelivery);
        log.info("Auto-created in-house delivery '{}' from spinning delivery '{}'", inHouseDelivery.getAutoId(), savedDelivery.getAutoId());

        return mapper.toDeliveryResponse(savedDelivery);
    }

    @Transactional
    public void deleteDelivery(String autoId) {
        log.info("Deleting spinning delivery '{}'", autoId);
        SpinningDelivery delivery = lookupService.getActiveSpinningDeliveryByAutoId(autoId);
        InHouseDelivery linkedDelivery = inHouseDeliveryRepository.findBySpinningDeliveryAndIsDeletedFalse(delivery).orElse(null);
        if (linkedDelivery != null) {
            log.info("Found linked in-house delivery '{}' for spinning delivery '{}'", linkedDelivery.getAutoId(), autoId);
            if (!inHouseStockSplitRepository.findByDelivery_IdAndIsDeletedFalse(linkedDelivery.getId()).isEmpty()) {
                throw new DeleteConflictException(
                        "INHOUSE_DELIVERY_HAS_SPLITS",
                        "Spinning delivery cannot be deleted because the linked in-house delivery has splits",
                        Map.of("inHouseDeliveryAutoId", linkedDelivery.getAutoId())
                );
            }
            linkedDelivery.setDeleted(true);
            inHouseDeliveryRepository.save(linkedDelivery);
            log.info("Deleted linked in-house delivery '{}'", linkedDelivery.getAutoId());
        }
        delivery.setDeleted(true);
        spinningDeliveryRepository.save(delivery);
        log.info("Deleted spinning delivery '{}'", autoId);
    }

    private void ensureDeletionSafe(SpinningOrder order) {
        BigDecimal totalDispatched = spinningOrderRepository.sumActiveQuantityByStyle(order.getStyle().getId());
        BigDecimal totalReceived = spinningDeliveryRepository.sumActiveFinalQuantityByStyle(order.getStyle().getId());
        BigDecimal remainingDispatched = totalDispatched.subtract(order.getQuantitySentKgs());
        log.debug("Checking spinning order downstream usage '{}': totalDispatched={}, totalReceived={}, remainingDispatched={}",
                order.getAutoId(), totalDispatched, totalReceived, remainingDispatched);
        if (remainingDispatched.compareTo(totalReceived) < 0) {
            throw new DeleteConflictException(
                    "SPINNING_ORDER_HAS_DOWNSTREAM_USAGE",
                    "Spinning order cannot be deleted because received stock already depends on it",
                    Map.of(
                            "styleAutoId", order.getStyle().getAutoId(),
                            "remainingDispatched", remainingDispatched,
                            "received", totalReceived
                    )
            );
        }
    }

    private Specification<SpinningOrder> orderSpec(
            String styleAutoId,
            LocalDate fromDate,
            LocalDate toDate,
            Boolean linkedYarnOrder,
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
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dispatchDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dispatchDate"), toDate));
            }
            if (linkedYarnOrder != null) {
                predicates.add(linkedYarnOrder ? cb.isNotNull(root.get("linkedYarnOrder")) : cb.isNull(root.get("linkedYarnOrder")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<SpinningDelivery> deliverySpec(String styleAutoId, LocalDate fromDate, LocalDate toDate, boolean includeDeleted) {
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
}
