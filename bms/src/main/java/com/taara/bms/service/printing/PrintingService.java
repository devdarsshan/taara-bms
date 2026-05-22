package com.taara.bms.service.printing;

import com.taara.bms.dto.common.PieceAvailabilityResponse;
import com.taara.bms.dto.printing.PrintingDashboardResponse;
import com.taara.bms.dto.printing.PrintingDeliveryCreateRequest;
import com.taara.bms.dto.printing.PrintingDeliveryResponse;
import com.taara.bms.dto.printing.PrintingOrderCreateRequest;
import com.taara.bms.dto.printing.PrintingOrderResponse;
import com.taara.bms.dto.stitching.StitchingOrderStatusUpdateRequest;
import com.taara.bms.entity.masterdata.StitchingSection;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.entity.printing.PrintingDelivery;
import com.taara.bms.entity.printing.PrintingOrder;
import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.SectionProcessType;
import com.taara.bms.enums.StitchingOrderStatus;
import com.taara.bms.exception.BusinessValidationException;
import com.taara.bms.exception.DeleteConflictException;
import com.taara.bms.mapper.common.ReferenceMapper;
import com.taara.bms.repo.printing.PrintingDeliveryRepository;
import com.taara.bms.repo.printing.PrintingOrderRepository;
import com.taara.bms.service.common.AutoIdSequence;
import com.taara.bms.service.common.AutoIdService;
import com.taara.bms.service.common.LookupService;
import com.taara.bms.service.inhouse.InHouseService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrintingService {

    private static final Logger log = LoggerFactory.getLogger(PrintingService.class);

    private final PrintingOrderRepository printingOrderRepository;
    private final PrintingDeliveryRepository printingDeliveryRepository;
    private final LookupService lookupService;
    private final AutoIdService autoIdService;
    private final ReferenceMapper referenceMapper;
    private final InHouseService inHouseService;

    public PrintingService(
            PrintingOrderRepository printingOrderRepository,
            PrintingDeliveryRepository printingDeliveryRepository,
            LookupService lookupService,
            AutoIdService autoIdService,
            ReferenceMapper referenceMapper,
            InHouseService inHouseService
    ) {
        this.printingOrderRepository = printingOrderRepository;
        this.printingDeliveryRepository = printingDeliveryRepository;
        this.lookupService = lookupService;
        this.autoIdService = autoIdService;
        this.referenceMapper = referenceMapper;
        this.inHouseService = inHouseService;
    }

    @Transactional(readOnly = true)
    public Page<PrintingOrderResponse> getOrders(
            String styleAutoId,
            String sectionAutoId,
            GarmentSize size,
            StitchingOrderStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted,
            Pageable pageable
    ) {
        return printingOrderRepository.findAll(orderSpec(styleAutoId, sectionAutoId, size, status, fromDate, toDate, includeDeleted), pageable)
                .map(this::toOrderResponse);
    }

    @Transactional(readOnly = true)
    public Page<PrintingDeliveryResponse> getDeliveries(
            String styleAutoId,
            String orderAutoId,
            GarmentSize size,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted,
            Pageable pageable
    ) {
        return printingDeliveryRepository.findAll(deliverySpec(styleAutoId, orderAutoId, size, fromDate, toDate, includeDeleted), pageable)
                .map(this::toDeliveryResponse);
    }

    @Transactional
    public PrintingOrderResponse createOrder(PrintingOrderCreateRequest request) {
        StitchingSection section = lookupService.getActiveSectionByAutoId(request.printingSectionAutoId());
        if (section.getProcessType() != SectionProcessType.PRINTING) {
            throw new BusinessValidationException("INVALID_PRINTING_SECTION", "The selected section is not configured for printing");
        }
        Style style = lookupService.getActiveStyleByAutoId(request.styleAutoId());
        int availablePieces = inHouseService.calculateStitchedPlainAvailable(style.getId(), request.size());
        if (request.piecesOrdered() > availablePieces) {
            throw new BusinessValidationException(
                    "PRINTING_ORDER_EXCEEDS_AVAILABLE",
                    "Pieces ordered exceed the available stitched plain stock for the selected style and size",
                    Map.of("availablePieces", availablePieces, "requestedPieces", request.piecesOrdered())
            );
        }

        PrintingOrder order = new PrintingOrder();
        order.setAutoId(autoIdService.next(AutoIdSequence.PRINTING_ORDER));
        order.setOrderDate(request.orderDate());
        order.setPrintingSection(section);
        order.setStyle(style);
        order.setSize(request.size());
        order.setPiecesOrdered(request.piecesOrdered());
        order.setStatus(StitchingOrderStatus.PENDING);
        order.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
        return toOrderResponse(printingOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public PieceAvailabilityResponse getAvailableOrderPieces(String styleAutoId, GarmentSize size) {
        Style style = lookupService.getActiveStyleByAutoId(styleAutoId);
        return new PieceAvailabilityResponse(style.getAutoId(), size, inHouseService.calculateStitchedPlainAvailable(style.getId(), size));
    }

    @Transactional
    public PrintingOrderResponse updateOrderStatus(String orderAutoId, StitchingOrderStatusUpdateRequest request) {
        PrintingOrder order = lookupService.getActivePrintingOrderByAutoId(orderAutoId);
        order.setStatus(request.status());
        return toOrderResponse(printingOrderRepository.save(order));
    }

    @Transactional
    public PrintingOrderResponse updateOrder(String orderAutoId, com.taara.bms.dto.printing.PrintingOrderUpdateRequest request) {
        PrintingOrder order = lookupService.getActivePrintingOrderByAutoId(orderAutoId);
        
        StitchingSection section = lookupService.getActiveSectionByAutoId(request.printingSectionAutoId());
        if (section.getProcessType() != SectionProcessType.PRINTING) {
            throw new BusinessValidationException("INVALID_PRINTING_SECTION", "The selected section is not configured for printing");
        }
        Style style = lookupService.getActiveStyleByAutoId(request.styleAutoId());
        
        int availablePieces = inHouseService.calculateStitchedPlainAvailable(style.getId(), request.size());
        
        if (order.getStyle().getId().equals(style.getId()) && order.getSize() == request.size()) {
            availablePieces += order.getPiecesOrdered();
        }
        
        if (request.piecesOrdered() > availablePieces) {
            throw new BusinessValidationException(
                    "PRINTING_ORDER_EXCEEDS_AVAILABLE",
                    "Pieces ordered exceed the available stitched plain stock for the selected style and size",
                    Map.of("availablePieces", availablePieces, "requestedPieces", request.piecesOrdered())
            );
        }
        
        int delivered = Objects.requireNonNullElse(printingDeliveryRepository.sumActiveDeliveredByOrder(order.getId()), 0);
        if (request.piecesOrdered() < delivered) {
            throw new BusinessValidationException(
                    "PRINTING_ORDER_BELOW_DELIVERED",
                    "Pieces ordered cannot be less than already delivered pieces",
                    Map.of("deliveredPieces", delivered, "requestedPieces", request.piecesOrdered())
            );
        }

        order.setOrderDate(request.orderDate());
        order.setPrintingSection(section);
        order.setStyle(style);
        order.setSize(request.size());
        order.setPiecesOrdered(request.piecesOrdered());
        order.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
        
        updatePrintingOrderStatus(order);
        return toOrderResponse(order);
    }

    @Transactional
    public void deleteOrder(String orderAutoId) {
        PrintingOrder order = lookupService.getActivePrintingOrderByAutoId(orderAutoId);
        if (Objects.requireNonNullElse(printingDeliveryRepository.sumActiveDeliveredByOrder(order.getId()), 0) > 0) {
            throw new DeleteConflictException(
                    "PRINTING_ORDER_HAS_DELIVERIES",
                    "Printing order cannot be deleted because deliveries already depend on it",
                    Map.of("orderAutoId", order.getAutoId())
            );
        }
        order.setDeleted(true);
        printingOrderRepository.save(order);
    }

    @Transactional
    public PrintingDeliveryResponse createDelivery(PrintingDeliveryCreateRequest request) {
        PrintingOrder order = lookupService.getActivePrintingOrderByAutoId(request.printingOrderAutoId());
        int deliveredBefore = Objects.requireNonNullElse(printingDeliveryRepository.sumActiveDeliveredByOrder(order.getId()), 0);
        int pending = Math.max(order.getPiecesOrdered() - deliveredBefore, 0);
        if (request.piecesDelivered() > pending) {
            throw new BusinessValidationException(
                    "PRINTING_DELIVERY_EXCEEDS_PENDING",
                    "Delivered pcs exceed the pending pcs for this printing order",
                    Map.of("availablePieces", pending, "requestedPieces", request.piecesDelivered())
            );
        }

        PrintingDelivery delivery = new PrintingDelivery();
        delivery.setAutoId(autoIdService.next(AutoIdSequence.PRINTING_DELIVERY));
        delivery.setDeliveryDate(request.deliveryDate());
        delivery.setPrintingOrder(order);
        delivery.setPiecesDelivered(request.piecesDelivered());
        PrintingDelivery saved = printingDeliveryRepository.save(delivery);
        updatePrintingOrderStatus(order);
        return toDeliveryResponse(saved);
    }

    @Transactional
    public PrintingDeliveryResponse updateDelivery(String deliveryAutoId, com.taara.bms.dto.printing.PrintingDeliveryUpdateRequest request) {
        PrintingDelivery delivery = lookupService.getActivePrintingDeliveryByAutoId(deliveryAutoId);
        PrintingOrder currentOrder = delivery.getPrintingOrder();
        PrintingOrder newOrder = lookupService.getActivePrintingOrderByAutoId(request.printingOrderAutoId());
        
        int deliveredBefore = Objects.requireNonNullElse(printingDeliveryRepository.sumActiveDeliveredByOrder(newOrder.getId()), 0);
        if (currentOrder.getId().equals(newOrder.getId())) {
            deliveredBefore -= delivery.getPiecesDelivered();
        }
        
        int pending = Math.max(newOrder.getPiecesOrdered() - deliveredBefore, 0);
        if (request.piecesDelivered() > pending) {
            throw new BusinessValidationException(
                    "PRINTING_DELIVERY_EXCEEDS_PENDING",
                    "Delivered pcs exceed the pending pcs for this printing order",
                    Map.of("availablePieces", pending, "requestedPieces", request.piecesDelivered())
            );
        }

        delivery.setDeliveryDate(request.deliveryDate());
        delivery.setPrintingOrder(newOrder);
        delivery.setPiecesDelivered(request.piecesDelivered());
        PrintingDelivery saved = printingDeliveryRepository.save(delivery);
        
        if (!currentOrder.getId().equals(newOrder.getId())) {
            updatePrintingOrderStatus(currentOrder);
        }
        updatePrintingOrderStatus(newOrder);
        
        return toDeliveryResponse(saved);
    }

    @Transactional(readOnly = true)
    public PieceAvailabilityResponse getAvailableDeliveryPieces(String orderAutoId) {
        PrintingOrder order = lookupService.getActivePrintingOrderByAutoId(orderAutoId);
        int deliveredBefore = Objects.requireNonNullElse(printingDeliveryRepository.sumActiveDeliveredByOrder(order.getId()), 0);
        return new PieceAvailabilityResponse(order.getStyle().getAutoId(), order.getSize(), Math.max(order.getPiecesOrdered() - deliveredBefore, 0));
    }

    @Transactional
    public void deleteDelivery(String deliveryAutoId) {
        PrintingDelivery delivery = lookupService.getActivePrintingDeliveryByAutoId(deliveryAutoId);
        PrintingOrder order = delivery.getPrintingOrder();
        delivery.setDeleted(true);
        printingDeliveryRepository.save(delivery);
        updatePrintingOrderStatus(order);
    }

    @Transactional(readOnly = true)
    public PrintingDashboardResponse getDashboard(
            String styleAutoId,
            String sectionAutoId,
            GarmentSize size,
            StitchingOrderStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeDeleted
    ) {
        List<PrintingOrder> orders = printingOrderRepository.findAll(orderSpec(styleAutoId, sectionAutoId, size, status, fromDate, toDate, includeDeleted));
        List<PrintingDelivery> deliveries = printingDeliveryRepository.findAll(deliverySpec(styleAutoId, null, size, fromDate, toDate, includeDeleted));
        int totalPiecesInPrinting = orders.stream()
                .filter(order -> order.getStatus() == StitchingOrderStatus.PENDING || order.getStatus() == StitchingOrderStatus.PARTIALLY_DELIVERED)
                .mapToInt(this::pendingPieces)
                .sum();
        int deliveredPieces = deliveries.stream().mapToInt(PrintingDelivery::getPiecesDelivered).sum();
        long pendingOrdersCount = orders.stream().filter(order -> order.getStatus() == StitchingOrderStatus.PENDING).count();
        int defectivePieces = orders.stream()
                .filter(order -> order.getStatus() == StitchingOrderStatus.COMPLETE)
                .mapToInt(this::pendingPieces)
                .sum();
                
        int stitchedStockTotal = inHouseService.getStitchedPlainBreakdown().stream().mapToInt(com.taara.bms.dto.inhouse.ReadyToStitchBreakdownResponse::totalPieces).sum();

        return new PrintingDashboardResponse(totalPiecesInPrinting, deliveredPieces, pendingOrdersCount, defectivePieces, stitchedStockTotal, inHouseService.getStitchedPlainBreakdown());
    }

    private void updatePrintingOrderStatus(PrintingOrder order) {
        int delivered = Objects.requireNonNullElse(printingDeliveryRepository.sumActiveDeliveredByOrder(order.getId()), 0);
        if (delivered >= order.getPiecesOrdered()) {
            order.setStatus(StitchingOrderStatus.AUTO_CLOSED);
        } else if (delivered > 0) {
            order.setStatus(StitchingOrderStatus.PARTIALLY_DELIVERED);
        } else {
            order.setStatus(StitchingOrderStatus.PENDING);
        }
        printingOrderRepository.save(order);
    }

    private int pendingPieces(PrintingOrder order) {
        int delivered = Objects.requireNonNullElse(printingDeliveryRepository.sumActiveDeliveredByOrder(order.getId()), 0);
        return Math.max(order.getPiecesOrdered() - delivered, 0);
    }

    private PrintingOrderResponse toOrderResponse(PrintingOrder order) {
        int delivered = Objects.requireNonNullElse(printingDeliveryRepository.sumActiveDeliveredByOrder(order.getId()), 0);
        return new PrintingOrderResponse(
                order.getId(),
                order.getAutoId(),
                order.getOrderDate(),
                referenceMapper.toSectionRef(order.getPrintingSection()),
                referenceMapper.toStyleRef(order.getStyle()),
                order.getSize(),
                order.getPiecesOrdered(),
                delivered,
                Math.max(order.getPiecesOrdered() - delivered, 0),
                order.getStatus(),
                order.getNotes(),
                order.isDeleted(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private PrintingDeliveryResponse toDeliveryResponse(PrintingDelivery delivery) {
        PrintingOrder order = delivery.getPrintingOrder();
        return new PrintingDeliveryResponse(
                delivery.getId(),
                delivery.getAutoId(),
                delivery.getDeliveryDate(),
                order.getAutoId(),
                referenceMapper.toSectionRef(order.getPrintingSection()),
                referenceMapper.toStyleRef(order.getStyle()),
                order.getSize(),
                delivery.getPiecesDelivered(),
                delivery.isDeleted(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }

    private Specification<PrintingOrder> orderSpec(
            String styleAutoId,
            String sectionAutoId,
            GarmentSize size,
            StitchingOrderStatus status,
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
                predicates.add(cb.equal(cb.lower(root.get("printingSection").get("autoId")), sectionAutoId.trim().toLowerCase()));
            }
            if (size != null) {
                predicates.add(cb.equal(root.get("size"), size));
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

    private Specification<PrintingDelivery> deliverySpec(
            String styleAutoId,
            String orderAutoId,
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
                predicates.add(cb.equal(cb.lower(root.get("printingOrder").get("style").get("autoId")), styleAutoId.trim().toLowerCase()));
            }
            if (orderAutoId != null && !orderAutoId.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("printingOrder").get("autoId")), orderAutoId.trim().toLowerCase()));
            }
            if (size != null) {
                predicates.add(cb.equal(root.get("printingOrder").get("size"), size));
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
