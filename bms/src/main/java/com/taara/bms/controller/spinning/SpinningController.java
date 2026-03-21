package com.taara.bms.controller.spinning;

import com.taara.bms.dto.spinning.SpinningDashboardResponse;
import com.taara.bms.dto.spinning.SpinningDeliveryCreateRequest;
import com.taara.bms.dto.spinning.SpinningDeliveryResponse;
import com.taara.bms.dto.spinning.SpinningOrderCreateRequest;
import com.taara.bms.dto.spinning.SpinningOrderResponse;
import com.taara.bms.service.spinning.SpinningService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spinning")
public class SpinningController {
    private static final Logger log = LoggerFactory.getLogger(SpinningController.class);

    private final SpinningService spinningService;

    public SpinningController(SpinningService spinningService) {
        this.spinningService = spinningService;
    }

    @GetMapping("/orders")
    public Page<SpinningOrderResponse> getOrders(
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Boolean linkedYarnOrder,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching spinning orders. styleAutoId='{}', linkedYarnOrder={}, fromDate={}, toDate={}, includeDeleted={}, page={}",
                styleAutoId, linkedYarnOrder, fromDate, toDate, includeDeleted, pageable.getPageNumber());
        return spinningService.getOrders(styleAutoId, fromDate, toDate, linkedYarnOrder, includeDeleted, pageable);
    }

    @PostMapping("/orders")
    public SpinningOrderResponse createOrder(@Valid @RequestBody SpinningOrderCreateRequest request) {
        log.info("Creating spinning order. styleAutoId='{}', linkedYarnOrderAutoId='{}', dispatchDate={}",
                request.styleAutoId(), request.linkedYarnOrderAutoId(), request.dispatchDate());
        return spinningService.createOrder(request);
    }

    @DeleteMapping("/orders/{autoId}")
    public void deleteOrder(@PathVariable String autoId) {
        log.info("Deleting spinning order. autoId='{}'", autoId);
        spinningService.deleteOrder(autoId);
    }

    @GetMapping("/deliveries")
    public Page<SpinningDeliveryResponse> getDeliveries(
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching spinning deliveries. styleAutoId='{}', fromDate={}, toDate={}, includeDeleted={}, page={}",
                styleAutoId, fromDate, toDate, includeDeleted, pageable.getPageNumber());
        return spinningService.getDeliveries(styleAutoId, fromDate, toDate, includeDeleted, pageable);
    }

    @PostMapping("/deliveries")
    public SpinningDeliveryResponse createDelivery(@Valid @RequestBody SpinningDeliveryCreateRequest request) {
        log.info("Creating spinning delivery. styleAutoId='{}', deliveryDate={}", request.styleAutoId(), request.deliveryDate());
        return spinningService.createDelivery(request);
    }

    @DeleteMapping("/deliveries/{autoId}")
    public void deleteDelivery(@PathVariable String autoId) {
        log.info("Deleting spinning delivery. autoId='{}'", autoId);
        spinningService.deleteDelivery(autoId);
    }

    @GetMapping("/dashboard")
    public SpinningDashboardResponse getDashboard(
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        log.info("Fetching spinning dashboard. styleAutoId='{}', fromDate={}, toDate={}, includeDeleted={}",
                styleAutoId, fromDate, toDate, includeDeleted);
        return spinningService.getDashboard(styleAutoId, fromDate, toDate, includeDeleted);
    }
}
