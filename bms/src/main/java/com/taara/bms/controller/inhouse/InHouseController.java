package com.taara.bms.controller.inhouse;

import com.taara.bms.dto.inhouse.CuttingCreateRequest;
import com.taara.bms.dto.inhouse.ExistingStockCreateRequest;
import com.taara.bms.dto.inhouse.ExistingStockResponse;
import com.taara.bms.dto.inhouse.CuttingAvailabilityResponse;
import com.taara.bms.dto.inhouse.CuttingResponse;
import com.taara.bms.dto.inhouse.CuttingUpdateRequest;
import com.taara.bms.dto.inhouse.InHouseDashboardResponse;
import com.taara.bms.dto.inhouse.InHouseDeliveryResponse;
import com.taara.bms.dto.inhouse.InHouseSplitBatchRequest;
import com.taara.bms.dto.inhouse.InHouseStockResponse;
import com.taara.bms.dto.inhouse.InHouseStockSplitResponse;
import com.taara.bms.dto.inhouse.StitchedStockResponse;
import com.taara.bms.enums.CuttingStatus;
import com.taara.bms.service.inhouse.InHouseService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
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
@RequestMapping("/api/inhouse")
public class InHouseController {
    private static final Logger log = LoggerFactory.getLogger(InHouseController.class);

    private final InHouseService inHouseService;

    public InHouseController(InHouseService inHouseService) {
        this.inHouseService = inHouseService;
    }

    @PostMapping("/existing-stocks")
    public ExistingStockResponse createExistingStock(@Valid @RequestBody ExistingStockCreateRequest request) {
        return inHouseService.createExistingStock(request);
    }

    @GetMapping("/existing-stocks")
    public List<ExistingStockResponse> getExistingStocks() {
        return inHouseService.getExistingStocks();
    }

    @GetMapping("/deliveries/{deliveryAutoId}/splits")
    public List<InHouseStockSplitResponse> getSplits(@PathVariable String deliveryAutoId) {
        log.info("Fetching in-house splits. deliveryAutoId='{}'", deliveryAutoId);
        return inHouseService.getSplits(deliveryAutoId);
    }

    @PostMapping("/deliveries/{deliveryAutoId}/splits")
    public List<InHouseStockSplitResponse> createSplits(
            @PathVariable String deliveryAutoId,
            @Valid @RequestBody InHouseSplitBatchRequest request
    ) {
        log.info("Creating in-house splits. deliveryAutoId='{}', splitCount={}", deliveryAutoId, request.splits().size());
        return inHouseService.createSplits(deliveryAutoId, request);
    }

    @DeleteMapping("/deliveries/{deliveryAutoId}/splits/{splitAutoId}")
    public void deleteSplit(@PathVariable String deliveryAutoId, @PathVariable String splitAutoId) {
        log.info("Deleting in-house split. deliveryAutoId='{}', splitAutoId='{}'", deliveryAutoId, splitAutoId);
        inHouseService.deleteSplit(deliveryAutoId, splitAutoId);
    }

    @GetMapping("/stock")
    public List<InHouseStockResponse> getStock(
            @RequestParam(required = false) String diaAutoId,
            @RequestParam(required = false) String styleAutoId
    ) {
        log.info("Fetching in-house stock. diaAutoId='{}', styleAutoId='{}'", diaAutoId, styleAutoId);
        return inHouseService.getStock(diaAutoId, styleAutoId);
    }

    @GetMapping("/cuttings")
    public Page<CuttingResponse> getCuttings(
            @RequestParam(required = false) String diaAutoId,
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(required = false) CuttingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching cuttings. diaAutoId='{}', styleAutoId='{}', status={}, fromDate={}, toDate={}, includeDeleted={}, page={}",
                diaAutoId, styleAutoId, status, fromDate, toDate, includeDeleted, pageable.getPageNumber());
        return inHouseService.getCuttings(diaAutoId, styleAutoId, status, fromDate, toDate, includeDeleted, pageable);
    }

    @PostMapping("/cuttings")
    public CuttingResponse createCutting(@Valid @RequestBody CuttingCreateRequest request) {
        log.info("Creating cutting. cuttingDate={}, rowCount={}", request.cuttingDate(), request.rows().size());
        return inHouseService.createCutting(request);
    }

    @GetMapping("/cuttings/available-quantity")
    public CuttingAvailabilityResponse getAvailableCuttingQuantity(
            @RequestParam String diaAutoId,
            @RequestParam String styleAutoId
    ) {
        log.info("Fetching cutting availability. diaAutoId='{}', styleAutoId='{}'", diaAutoId, styleAutoId);
        return inHouseService.getAvailableCuttingQuantity(diaAutoId, styleAutoId);
    }

    @PatchMapping("/cuttings/{cuttingAutoId}")
    public CuttingResponse updateCutting(@PathVariable String cuttingAutoId, @Valid @RequestBody CuttingUpdateRequest request) {
        log.info("Updating cutting. cuttingAutoId='{}', rowCount={}", cuttingAutoId, request.rows().size());
        return inHouseService.updateCutting(cuttingAutoId, request);
    }

    @DeleteMapping("/cuttings/{cuttingAutoId}")
    public void deleteCutting(@PathVariable String cuttingAutoId) {
        log.info("Deleting cutting. cuttingAutoId='{}'", cuttingAutoId);
        inHouseService.deleteCutting(cuttingAutoId);
    }

    @GetMapping("/stitched-stock")
    public List<StitchedStockResponse> getStitchedStock(
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        log.info("Fetching stitched stock. styleAutoId='{}', fromDate={}, toDate={}", styleAutoId, fromDate, toDate);
        return inHouseService.getStitchedStock(styleAutoId, fromDate, toDate);
    }

    @GetMapping("/dashboard")
    public InHouseDashboardResponse getDashboard(
            @RequestParam(required = false) String diaAutoId,
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        log.info("Fetching in-house dashboard. diaAutoId='{}', styleAutoId='{}', fromDate={}, toDate={}",
                diaAutoId, styleAutoId, fromDate, toDate);
        return inHouseService.getDashboard(diaAutoId, styleAutoId, fromDate, toDate);
    }
}
