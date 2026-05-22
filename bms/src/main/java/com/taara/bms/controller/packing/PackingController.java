package com.taara.bms.controller.packing;

import com.taara.bms.dto.common.PieceAvailabilityResponse;
import com.taara.bms.dto.packing.PackingCreateRequest;
import com.taara.bms.dto.packing.PackingDashboardResponse;
import com.taara.bms.dto.packing.PackingResponse;
import com.taara.bms.enums.GarmentSize;
import com.taara.bms.enums.PackingStockType;
import com.taara.bms.service.packing.PackingService;
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

import com.taara.bms.dto.packing.PackingUpdateRequest;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/packing")
public class PackingController {

    private static final Logger log = LoggerFactory.getLogger(PackingController.class);

    private final PackingService packingService;

    public PackingController(PackingService packingService) {
        this.packingService = packingService;
    }

    @GetMapping("/entries")
    public Page<PackingResponse> getEntries(
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(name = "garmentSize", required = false) GarmentSize size,
            @RequestParam(required = false) PackingStockType stockType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching packing entries. styleAutoId='{}', size={}, stockType={}, fromDate={}, toDate={}, includeDeleted={}, page={}",
                styleAutoId, size, stockType, fromDate, toDate, includeDeleted, pageable.getPageNumber());
        return packingService.getEntries(styleAutoId, size, stockType, fromDate, toDate, includeDeleted, pageable);
    }

    @PostMapping("/entries")
    public PackingResponse createEntry(@Valid @RequestBody PackingCreateRequest request) {
        log.info("Creating packing entry. styleAutoId='{}', size={}, stockType={}, packed={}, defective={}",
                request.styleAutoId(), request.size(), request.stockType(), request.correctlyPackedPieces(), request.defectivePieces());
        return packingService.createEntry(request);
    }

    @PutMapping("/entries/{packingAutoId}")
    public PackingResponse updateEntry(@PathVariable String packingAutoId, @Valid @RequestBody PackingUpdateRequest request) {
        log.info("Updating packing entry. packingAutoId='{}', styleAutoId='{}', size={}, stockType={}, packed={}, defective={}",
                packingAutoId, request.styleAutoId(), request.size(), request.stockType(), request.correctlyPackedPieces(), request.defectivePieces());
        return packingService.updateEntry(packingAutoId, request);
    }

    @GetMapping("/available-pieces")
    public PieceAvailabilityResponse getAvailablePieces(
            @RequestParam String styleAutoId,
            @RequestParam(name = "garmentSize") GarmentSize size,
            @RequestParam PackingStockType stockType
    ) {
        log.info("Fetching available packing pieces. styleAutoId='{}', size={}, stockType={}", styleAutoId, size, stockType);
        return packingService.getAvailablePieces(styleAutoId, size, stockType);
    }

    @DeleteMapping("/entries/{packingAutoId}")
    public void deleteEntry(@PathVariable String packingAutoId) {
        log.info("Deleting packing entry. packingAutoId='{}'", packingAutoId);
        packingService.deleteEntry(packingAutoId);
    }

    @GetMapping("/dashboard")
    public PackingDashboardResponse getDashboard(
            @RequestParam(required = false) String styleAutoId,
            @RequestParam(name = "garmentSize", required = false) GarmentSize size,
            @RequestParam(required = false) PackingStockType stockType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        log.info("Fetching packing dashboard. styleAutoId='{}', size={}, stockType={}, fromDate={}, toDate={}, includeDeleted={}",
                styleAutoId, size, stockType, fromDate, toDate, includeDeleted);
        return packingService.getDashboard(styleAutoId, size, stockType, fromDate, toDate, includeDeleted);
    }
}
