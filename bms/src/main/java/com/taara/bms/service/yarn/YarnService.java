package com.taara.bms.service.yarn;

import com.taara.bms.dto.yarn.YarnDashboardResponse;
import com.taara.bms.dto.yarn.YarnOrderCreateRequest;
import com.taara.bms.dto.yarn.YarnOrderResponse;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.entity.spinning.SpinningOrder;
import com.taara.bms.entity.yarn.YarnOrder;
import com.taara.bms.exception.BusinessValidationException;
import com.taara.bms.exception.DeleteConflictException;
import com.taara.bms.exception.ResourceNotFoundException;
import com.taara.bms.helper.BigDecimalUtils;
import com.taara.bms.mapper.yarn.YarnMapper;
import com.taara.bms.repo.spinning.SpinningDeliveryRepository;
import com.taara.bms.repo.spinning.SpinningOrderRepository;
import com.taara.bms.repo.yarn.YarnOrderRepository;
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

import com.taara.bms.dto.yarn.YarnOrderUpdateRequest;
import java.util.Optional;

@Service
public class YarnService {

    private static final Logger log = LoggerFactory.getLogger(YarnService.class);

    private final YarnOrderRepository yarnOrderRepository;
    private final SpinningOrderRepository spinningOrderRepository;
    private final SpinningDeliveryRepository spinningDeliveryRepository;
    private final LookupService lookupService;
    private final AutoIdService autoIdService;
    private final YarnMapper mapper;

    public YarnService(
            YarnOrderRepository yarnOrderRepository,
            SpinningOrderRepository spinningOrderRepository,
            SpinningDeliveryRepository spinningDeliveryRepository,
            LookupService lookupService,
            AutoIdService autoIdService,
            YarnMapper mapper
    ) {
        this.yarnOrderRepository = yarnOrderRepository;
        this.spinningOrderRepository = spinningOrderRepository;
        this.spinningDeliveryRepository = spinningDeliveryRepository;
        this.lookupService = lookupService;
        this.autoIdService = autoIdService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<YarnOrderResponse> getOrders(String styleAutoId, LocalDate fromDate, LocalDate toDate, boolean includeDeleted, Pageable pageable) {
        log.info("Fetching yarn orders with styleAutoId='{}', fromDate={}, toDate={}, includeDeleted={}, pageable={}",
                styleAutoId, fromDate, toDate, includeDeleted, pageable);
        Page<YarnOrderResponse> orders = yarnOrderRepository.findAll(orderSpec(styleAutoId, fromDate, toDate, includeDeleted), pageable)
                .map(mapper::toResponse);
        log.info("Fetched {} yarn orders", orders.getNumberOfElements());
        return orders;
    }

    @Transactional(readOnly = true)
    public YarnDashboardResponse getDashboard(String styleAutoId, LocalDate fromDate, LocalDate toDate, boolean includeDeleted) {
        log.info("Calculating yarn dashboard with styleAutoId='{}', fromDate={}, toDate={}, includeDeleted={}",
                styleAutoId, fromDate, toDate, includeDeleted);
        BigDecimal totalOrdered = yarnOrderRepository.findAll(orderSpec(styleAutoId, fromDate, toDate, includeDeleted))
                .stream()
                .map(YarnOrder::getQuantityKgs)
                .reduce(BigDecimalUtils.ZERO, BigDecimal::add);
        BigDecimal dispatched = spinningOrderRepository.findAll(spinningOrderSpec(styleAutoId, fromDate, toDate, includeDeleted, null))
                .stream()
                .map(SpinningOrder::getQuantitySentKgs)
                .reduce(BigDecimalUtils.ZERO, BigDecimal::add);
        log.debug("Yarn dashboard totals calculated: totalOrdered={}, dispatched={}, yarnInOrder={}",
                totalOrdered, dispatched, totalOrdered.subtract(dispatched));
        YarnDashboardResponse response = new YarnDashboardResponse(
                totalOrdered,
                totalOrdered.subtract(dispatched),
                dispatched
        );
        log.info("Yarn dashboard ready");
        return response;
    }

    @Transactional
    public YarnOrderResponse createOrder(YarnOrderCreateRequest request) {
        log.info("Creating yarn order for styleAutoId='{}' on orderDate={}", request.styleAutoId(), request.orderDate());
        Style style = lookupService.getActiveStyleByAutoId(request.styleAutoId());
        BigDecimal quantity = BigDecimalUtils.scale(request.quantityKgs());
        if (quantity.signum() <= 0) {
            throw new BusinessValidationException("INVALID_YARN_QUANTITY", "Quantity must be positive");
        }
        log.debug("Validated yarn quantity={} for style='{}'", quantity, style.getAutoId());

        YarnOrder yarnOrder = new YarnOrder();
        yarnOrder.setAutoId(autoIdService.next(AutoIdSequence.YARN_ORDER));
        yarnOrder.setOrderDate(request.orderDate());
        yarnOrder.setStyle(style);
        yarnOrder.setQuantityKgs(quantity);
        yarnOrder.setSupplierNotes(request.supplierNotes());
        YarnOrder saved = yarnOrderRepository.save(yarnOrder);
        log.info("Created yarn order '{}'", saved.getAutoId());

        SpinningOrder spinningOrder = new SpinningOrder();
        spinningOrder.setAutoId(autoIdService.next(AutoIdSequence.SPINNING_ORDER));
        spinningOrder.setDispatchDate(request.orderDate());
        spinningOrder.setLinkedYarnOrder(saved);
        spinningOrder.setStyle(style);
        spinningOrder.setQuantitySentKgs(quantity);
        spinningOrder.setFactoryNotes(request.supplierNotes());
        spinningOrder.setAutoCreated(true);
        spinningOrderRepository.save(spinningOrder);
        log.info("Auto-created linked spinning order '{}' for yarn order '{}'", spinningOrder.getAutoId(), saved.getAutoId());

        YarnOrderResponse response = mapper.toResponse(saved);
        log.info("Completed yarn order create flow for '{}'", response.autoId());
        return response;
    }

    @Transactional
    public YarnOrderResponse updateOrder(String autoId, YarnOrderUpdateRequest request) {
        log.info("Updating yarn order '{}'", autoId);
        YarnOrder yarnOrder = lookupService.getActiveYarnOrderByAutoId(autoId);

        Style style = lookupService.getActiveStyleByAutoId(request.styleAutoId());
        BigDecimal quantity = BigDecimalUtils.scale(request.quantityKgs());
        if (quantity.signum() <= 0) {
            throw new BusinessValidationException("INVALID_YARN_QUANTITY", "Quantity must be positive");
        }

        yarnOrder.setOrderDate(request.orderDate());
        yarnOrder.setStyle(style);
        yarnOrder.setQuantityKgs(quantity);
        yarnOrder.setSupplierNotes(request.supplierNotes());

        SpinningOrder linkedSpinningOrder = spinningOrderRepository.findByLinkedYarnOrderAndIsDeletedFalse(yarnOrder).orElse(null);
        if (linkedSpinningOrder != null) {
            log.info("Updating linked spinning order '{}'", linkedSpinningOrder.getAutoId());
            linkedSpinningOrder.setDispatchDate(request.orderDate());
            linkedSpinningOrder.setStyle(style);
            
            // Check if the new quantity is sufficient for downstream usage
            BigDecimal totalReceived = spinningDeliveryRepository.sumActiveFinalQuantityByStyle(linkedSpinningOrder.getStyle().getId());
            if (quantity.compareTo(totalReceived) < 0) {
                throw new BusinessValidationException("QUANTITY_TOO_LOW", "Updated quantity is less than already received spinning deliveries.");
            }
            linkedSpinningOrder.setQuantitySentKgs(quantity);
            linkedSpinningOrder.setFactoryNotes(request.supplierNotes());
            spinningOrderRepository.save(linkedSpinningOrder);
        }

        YarnOrder saved = yarnOrderRepository.save(yarnOrder);
        log.info("Updated yarn order '{}'", saved.getAutoId());
        return mapper.toResponse(saved);
    }

    @Transactional
    public void deleteOrder(String autoId) {
        log.info("Deleting yarn order '{}'", autoId);
        YarnOrder yarnOrder = lookupService.getActiveYarnOrderByAutoId(autoId);
        SpinningOrder linkedSpinningOrder = spinningOrderRepository.findByLinkedYarnOrderAndIsDeletedFalse(yarnOrder).orElse(null);
        if (linkedSpinningOrder != null) {
            log.info("Found linked spinning order '{}' for yarn order '{}'", linkedSpinningOrder.getAutoId(), autoId);
            ensureSpinningDeletionSafe(linkedSpinningOrder);
            linkedSpinningOrder.setDeleted(true);
            spinningOrderRepository.save(linkedSpinningOrder);
            log.info("Deleted linked spinning order '{}'", linkedSpinningOrder.getAutoId());
        }
        yarnOrder.setDeleted(true);
        yarnOrderRepository.save(yarnOrder);
        log.info("Deleted yarn order '{}'", autoId);
    }

    private void ensureSpinningDeletionSafe(SpinningOrder spinningOrder) {
        BigDecimal totalDispatched = spinningOrderRepository.sumActiveQuantityByStyle(spinningOrder.getStyle().getId());
        BigDecimal totalReceived = spinningDeliveryRepository.sumActiveFinalQuantityByStyle(spinningOrder.getStyle().getId());
        BigDecimal remainingDispatched = totalDispatched.subtract(spinningOrder.getQuantitySentKgs());
        log.debug("Checking downstream usage for spinning order '{}': totalDispatched={}, totalReceived={}, remainingDispatched={}",
                spinningOrder.getAutoId(), totalDispatched, totalReceived, remainingDispatched);
        if (remainingDispatched.compareTo(totalReceived) < 0) {
            throw new DeleteConflictException(
                    "SPINNING_ORDER_HAS_DOWNSTREAM_USAGE",
                    "The linked spinning order cannot be deleted because received stock already depends on it",
                    Map.of(
                            "styleAutoId", spinningOrder.getStyle().getAutoId(),
                            "remainingDispatched", remainingDispatched,
                            "received", totalReceived
                    )
            );
        }
    }

    private Specification<YarnOrder> orderSpec(String styleAutoId, LocalDate fromDate, LocalDate toDate, boolean includeDeleted) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (!includeDeleted) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
            }
            if (styleAutoId != null && !styleAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("style").get("autoId")), styleAutoId.trim().toLowerCase()));
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

    private Specification<SpinningOrder> spinningOrderSpec(
            String styleAutoId,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted,
            Boolean linkedYarnOrder
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
                predicates.add(linkedYarnOrder
                        ? cb.isNotNull(root.get("linkedYarnOrder"))
                        : cb.isNull(root.get("linkedYarnOrder")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
