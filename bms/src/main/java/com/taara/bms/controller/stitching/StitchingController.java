package com.taara.bms.controller.stitching;

import com.taara.bms.dto.stitching.StitchingDashboardResponse;
import com.taara.bms.dto.stitching.StitchingDeliveryCreateRequest;
import com.taara.bms.dto.stitching.StitchingDeliveryResponse;
import com.taara.bms.dto.stitching.StitchingAvailabilityResponse;
import com.taara.bms.dto.stitching.StitchingOrderCreateRequest;
import com.taara.bms.dto.stitching.StitchingOrderResponse;
import com.taara.bms.dto.stitching.StitchingOrderStatusUpdateRequest;
import com.taara.bms.enums.StitchingOrderStatus;
import com.taara.bms.service.stitching.StitchingService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stitching")
public class StitchingController {
    private static final Logger log = LoggerFactory.getLogger(StitchingController.class);

    private final StitchingService stitchingService;

    public StitchingController(StitchingService stitchingService) {
        this.stitchingService = stitchingService;
    }

    @GetMapping("/orders")
    public Page<StitchingOrderResponse> getOrders(
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(required = false) String sectionAutoId,
            @RequestParam(required = false) StitchingOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching stitching orders. styleAutoId='{}', sectionAutoId='{}', status={}, fromDate={}, toDate={}, includeDeleted={}, page={}",
                styleAutoId, sectionAutoId, status, fromDate, toDate, includeDeleted, pageable.getPageNumber());
        return stitchingService.getOrders(styleAutoId, sectionAutoId, status, fromDate, toDate, includeDeleted, pageable);
    }

    @PostMapping("/orders")
    public StitchingOrderResponse createOrder(@Valid @RequestBody StitchingOrderCreateRequest request) {
        log.info("Creating stitching order. styleAutoId='{}', sectionAutoId='{}', orderDate={}",
                request.styleAutoId(), request.stitchingSectionAutoId(), request.orderDate());
        return stitchingService.createOrder(request);
    }

    @GetMapping("/available-order-pieces")
    public StitchingAvailabilityResponse getAvailableOrderPieces(@RequestParam String styleAutoId) {
        log.info("Fetching available stitching order pieces. styleAutoId='{}'", styleAutoId);
        return stitchingService.getAvailableOrderPieces(styleAutoId);
    }

    @PatchMapping("/orders/{orderAutoId}/status")
    public StitchingOrderResponse updateOrderStatus(@PathVariable String orderAutoId, @Valid @RequestBody StitchingOrderStatusUpdateRequest request) {
        log.info("Updating stitching order status. orderAutoId='{}', status={}", orderAutoId, request.status());
        return stitchingService.updateOrderStatus(orderAutoId, request);
    }

    @DeleteMapping("/orders/{orderAutoId}")
    public void deleteOrder(@PathVariable String orderAutoId) {
        log.info("Deleting stitching order. orderAutoId='{}'", orderAutoId);
        stitchingService.deleteOrder(orderAutoId);
    }

    @GetMapping("/deliveries")
    public Page<StitchingDeliveryResponse> getDeliveries(
            @RequestParam(required = false) String orderAutoId,
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(required = false) String sectionAutoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching stitching deliveries. orderAutoId='{}', styleAutoId='{}', sectionAutoId='{}', fromDate={}, toDate={}, includeDeleted={}, page={}",
                orderAutoId, styleAutoId, sectionAutoId, fromDate, toDate, includeDeleted, pageable.getPageNumber());
        return stitchingService.getDeliveries(orderAutoId, styleAutoId, sectionAutoId, fromDate, toDate, includeDeleted, pageable);
    }

    @PostMapping("/deliveries")
    public StitchingDeliveryResponse createDelivery(@Valid @RequestBody StitchingDeliveryCreateRequest request) {
        log.info("Creating stitching delivery. orderAutoId='{}', deliveryDate={}, overrideWarnings={}",
                request.stitchingOrderAutoId(), request.deliveryDate(), request.overrideWarnings());
        return stitchingService.createDelivery(request);
    }

    @GetMapping("/orders/{orderAutoId}/available-delivery-pieces")
    public StitchingAvailabilityResponse getAvailableDeliveryPieces(@PathVariable String orderAutoId) {
        log.info("Fetching available stitching delivery pieces. orderAutoId='{}'", orderAutoId);
        return stitchingService.getAvailableDeliveryPieces(orderAutoId);
    }

    @DeleteMapping("/deliveries/{deliveryAutoId}")
    public void deleteDelivery(@PathVariable String deliveryAutoId) {
        log.info("Deleting stitching delivery. deliveryAutoId='{}'", deliveryAutoId);
        stitchingService.deleteDelivery(deliveryAutoId);
    }

    @GetMapping("/dashboard")
    public StitchingDashboardResponse getDashboard(
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(required = false) String sectionAutoId,
            @RequestParam(required = false) StitchingOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        log.info("Fetching stitching dashboard. styleAutoId='{}', sectionAutoId='{}', status={}, fromDate={}, toDate={}, includeDeleted={}",
                styleAutoId, sectionAutoId, status, fromDate, toDate, includeDeleted);
        return stitchingService.getDashboard(styleAutoId, sectionAutoId, status, fromDate, toDate, includeDeleted);
    }
}
