package com.taara.bms.controller.masterdata;

import com.taara.bms.dto.masterdata.DiaResponse;
import com.taara.bms.dto.masterdata.DiaUpsertRequest;
import com.taara.bms.service.masterdata.MasterDataService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master/dias")
public class DiaController {
    private static final Logger log = LoggerFactory.getLogger(DiaController.class);

    private final MasterDataService masterDataService;

    public DiaController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping
    public Page<DiaResponse> getDias(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching dia list. search='{}', includeDeleted={}, page={}", search, includeDeleted, pageable.getPageNumber());
        return masterDataService.getDias(search, includeDeleted, pageable);
    }

    @PostMapping
    public DiaResponse createDia(@Valid @RequestBody DiaUpsertRequest request) {
        log.info("Creating dia. diaValue='{}'", request.diaValue());
        return masterDataService.createDia(request);
    }

    @PutMapping("/{autoId}")
    public DiaResponse updateDia(@PathVariable String autoId, @Valid @RequestBody DiaUpsertRequest request) {
        log.info("Updating dia. autoId='{}', diaValue='{}'", autoId, request.diaValue());
        return masterDataService.updateDia(autoId, request);
    }

    @DeleteMapping("/{autoId}")
    public void deleteDia(@PathVariable String autoId) {
        log.info("Deleting dia. autoId='{}'", autoId);
        masterDataService.deleteDia(autoId);
    }
}
