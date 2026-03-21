package com.taara.bms.controller.masterdata;

import com.taara.bms.dto.masterdata.StitchingSectionResponse;
import com.taara.bms.dto.masterdata.StitchingSectionUpsertRequest;
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
@RequestMapping("/api/master/stitching-sections")
public class StitchingSectionController {
    private static final Logger log = LoggerFactory.getLogger(StitchingSectionController.class);

    private final MasterDataService masterDataService;

    public StitchingSectionController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping
    public Page<StitchingSectionResponse> getSections(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching section list. search='{}', includeDeleted={}, page={}", search, includeDeleted, pageable.getPageNumber());
        return masterDataService.getSections(search, includeDeleted, pageable);
    }

    @PostMapping
    public StitchingSectionResponse createSection(@Valid @RequestBody StitchingSectionUpsertRequest request) {
        log.info("Creating stitching section. sectionName='{}', type={}", request.sectionName(), request.type());
        return masterDataService.createSection(request);
    }

    @PutMapping("/{autoId}")
    public StitchingSectionResponse updateSection(@PathVariable String autoId, @Valid @RequestBody StitchingSectionUpsertRequest request) {
        log.info("Updating stitching section. autoId='{}', sectionName='{}', type={}", autoId, request.sectionName(), request.type());
        return masterDataService.updateSection(autoId, request);
    }

    @DeleteMapping("/{autoId}")
    public void deleteSection(@PathVariable String autoId) {
        log.info("Deleting stitching section. autoId='{}'", autoId);
        masterDataService.deleteSection(autoId);
    }
}
