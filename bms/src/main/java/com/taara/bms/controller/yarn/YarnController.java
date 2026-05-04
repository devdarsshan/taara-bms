package com.taara.bms.controller.yarn;

import com.taara.bms.dto.yarn.YarnDashboardResponse;
import com.taara.bms.dto.yarn.YarnOrderCreateRequest;
import com.taara.bms.dto.yarn.YarnOrderResponse;
import com.taara.bms.service.yarn.YarnService;
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

import com.taara.bms.dto.yarn.YarnOrderUpdateRequest;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/yarn")
public class YarnController {
    private static final Logger log = LoggerFactory.getLogger(YarnController.class);

    private final YarnService yarnService;

    public YarnController(YarnService yarnService) {
        this.yarnService = yarnService;
    }

    @GetMapping("/orders")
    public Page<YarnOrderResponse> getOrders(
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching yarn orders. styleAutoId='{}', fromDate={}, toDate={}, includeDeleted={}, page={}",
                styleAutoId, fromDate, toDate, includeDeleted, pageable.getPageNumber());
        return yarnService.getOrders(styleAutoId, fromDate, toDate, includeDeleted, pageable);
    }

    @PostMapping("/orders")
    public YarnOrderResponse createOrder(@Valid @RequestBody YarnOrderCreateRequest request) {
        log.info("Creating yarn order. styleAutoId='{}', orderDate={}", request.styleAutoId(), request.orderDate());
        return yarnService.createOrder(request);
    }

    @PutMapping("/orders/{autoId}")
    public YarnOrderResponse updateOrder(@PathVariable String autoId, @Valid @RequestBody YarnOrderUpdateRequest request) {
        log.info("Updating yarn order. autoId='{}', styleAutoId='{}', orderDate={}", autoId, request.styleAutoId(), request.orderDate());
        return yarnService.updateOrder(autoId, request);
    }

    @DeleteMapping("/orders/{autoId}")
    public void deleteOrder(@PathVariable String autoId) {
        log.info("Deleting yarn order. autoId='{}'", autoId);
        yarnService.deleteOrder(autoId);
    }

    @GetMapping("/dashboard")
    public YarnDashboardResponse getDashboard(
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        log.info("Fetching yarn dashboard. styleAutoId='{}', fromDate={}, toDate={}, includeDeleted={}",
                styleAutoId, fromDate, toDate, includeDeleted);
        return yarnService.getDashboard(styleAutoId, fromDate, toDate, includeDeleted);
    }
}
