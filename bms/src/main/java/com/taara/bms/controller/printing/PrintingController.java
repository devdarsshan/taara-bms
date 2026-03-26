package com.taara.bms.controller.printing;

import com.taara.bms.dto.common.PieceAvailabilityResponse;
import com.taara.bms.dto.printing.PrintingDashboardResponse;
import com.taara.bms.dto.printing.PrintingDeliveryCreateRequest;
import com.taara.bms.dto.printing.PrintingDeliveryResponse;
import com.taara.bms.dto.printing.PrintingOrderCreateRequest;
import com.taara.bms.dto.printing.PrintingOrderResponse;
import com.taara.bms.dto.stitching.StitchingOrderStatusUpdateRequest;
import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.StitchingOrderStatus;
import com.taara.bms.service.printing.PrintingService;
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
@RequestMapping("/api/printing")
public class PrintingController {

    private static final Logger log = LoggerFactory.getLogger(PrintingController.class);

    private final PrintingService printingService;

    public PrintingController(PrintingService printingService) {
        this.printingService = printingService;
    }

    @GetMapping("/orders")
    public Page<PrintingOrderResponse> getOrders(
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(required = false) String sectionAutoId,
            @RequestParam(name = "garmentSize", required = false) GarmentSize size,
            @RequestParam(required = false) StitchingOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching printing orders. styleAutoId='{}', sectionAutoId='{}', size={}, status={}, fromDate={}, toDate={}, includeDeleted={}, page={}",
                styleAutoId, sectionAutoId, size, status, fromDate, toDate, includeDeleted, pageable.getPageNumber());
        return printingService.getOrders(styleAutoId, sectionAutoId, size, status, fromDate, toDate, includeDeleted, pageable);
    }

    @PostMapping("/orders")
    public PrintingOrderResponse createOrder(@Valid @RequestBody PrintingOrderCreateRequest request) {
        log.info("Creating printing order. sectionAutoId='{}', styleAutoId='{}', size={}, piecesOrdered={}",
                request.printingSectionAutoId(), request.styleAutoId(), request.size(), request.piecesOrdered());
        return printingService.createOrder(request);
    }

    @GetMapping("/available-order-pieces")
    public PieceAvailabilityResponse getAvailableOrderPieces(
            @RequestParam String styleAutoId,
            @RequestParam(name = "garmentSize") GarmentSize size
    ) {
        log.info("Fetching available printing order pieces. styleAutoId='{}', size={}", styleAutoId, size);
        return printingService.getAvailableOrderPieces(styleAutoId, size);
    }

    @PatchMapping("/orders/{orderAutoId}/status")
    public PrintingOrderResponse updateOrderStatus(@PathVariable String orderAutoId, @Valid @RequestBody StitchingOrderStatusUpdateRequest request) {
        log.info("Updating printing order status. orderAutoId='{}', status={}", orderAutoId, request.status());
        return printingService.updateOrderStatus(orderAutoId, request);
    }

    @DeleteMapping("/orders/{orderAutoId}")
    public void deleteOrder(@PathVariable String orderAutoId) {
        log.info("Deleting printing order. orderAutoId='{}'", orderAutoId);
        printingService.deleteOrder(orderAutoId);
    }

    @GetMapping("/deliveries")
    public Page<PrintingDeliveryResponse> getDeliveries(
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(required = false) String orderAutoId,
            @RequestParam(name = "garmentSize", required = false) GarmentSize size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching printing deliveries. styleAutoId='{}', orderAutoId='{}', size={}, fromDate={}, toDate={}, includeDeleted={}, page={}",
                styleAutoId, orderAutoId, size, fromDate, toDate, includeDeleted, pageable.getPageNumber());
        return printingService.getDeliveries(styleAutoId, orderAutoId, size, fromDate, toDate, includeDeleted, pageable);
    }

    @PostMapping("/deliveries")
    public PrintingDeliveryResponse createDelivery(@Valid @RequestBody PrintingDeliveryCreateRequest request) {
        log.info("Creating printing delivery. orderAutoId='{}', piecesDelivered={}", request.printingOrderAutoId(), request.piecesDelivered());
        return printingService.createDelivery(request);
    }

    @GetMapping("/available-delivery-pieces")
    public PieceAvailabilityResponse getAvailableDeliveryPieces(@RequestParam String orderAutoId) {
        log.info("Fetching available printing delivery pieces. orderAutoId='{}'", orderAutoId);
        return printingService.getAvailableDeliveryPieces(orderAutoId);
    }

    @DeleteMapping("/deliveries/{deliveryAutoId}")
    public void deleteDelivery(@PathVariable String deliveryAutoId) {
        log.info("Deleting printing delivery. deliveryAutoId='{}'", deliveryAutoId);
        printingService.deleteDelivery(deliveryAutoId);
    }

    @GetMapping("/dashboard")
    public PrintingDashboardResponse getDashboard(
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(required = false) String sectionAutoId,
            @RequestParam(name = "garmentSize", required = false) GarmentSize size,
            @RequestParam(required = false) StitchingOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        log.info("Fetching printing dashboard. styleAutoId='{}', sectionAutoId='{}', size={}, status={}, fromDate={}, toDate={}, includeDeleted={}",
                styleAutoId, sectionAutoId, size, status, fromDate, toDate, includeDeleted);
        return printingService.getDashboard(styleAutoId, sectionAutoId, size, status, fromDate, toDate, includeDeleted);
    }
}
