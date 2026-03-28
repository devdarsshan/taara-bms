package com.taara.bms.controller.masterdata;

import com.taara.bms.dto.masterdata.StyleResponse;
import com.taara.bms.dto.masterdata.StyleUpsertRequest;
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
@RequestMapping("/api/master/styles")
public class StyleController {
    private static final Logger log = LoggerFactory.getLogger(StyleController.class);

    private final MasterDataService masterDataService;

    public StyleController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping
    public Page<StyleResponse> getStyles(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable
    ) {
        log.info("Fetching style list. search='{}', includeDeleted={}, page={}", search, includeDeleted, pageable.getPageNumber());
        return masterDataService.getStyles(search, includeDeleted, pageable);
    }

    @PostMapping
    public StyleResponse createStyle(@Valid @RequestBody StyleUpsertRequest request) {
        log.info("Creating style. styleName='{}', colorsCount={}", request.styleName(), request.colors() != null ? request.colors().size() : 0);
        return masterDataService.createStyle(request);
    }

    @PutMapping("/{autoId}")
    public StyleResponse updateStyle(@PathVariable String autoId, @Valid @RequestBody StyleUpsertRequest request) {
        log.info("Updating style. autoId='{}', styleName='{}'", autoId, request.styleName());
        return masterDataService.updateStyle(autoId, request);
    }

    @DeleteMapping("/{autoId}")
    public void deleteStyle(@PathVariable String autoId) {
        log.info("Deleting style. autoId='{}'", autoId);
        masterDataService.deleteStyle(autoId);
    }
}
