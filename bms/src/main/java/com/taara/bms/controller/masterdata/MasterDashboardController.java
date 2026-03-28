package com.taara.bms.controller.masterdata;

import com.taara.bms.dto.masterdata.MasterDashboardResponse;
import com.taara.bms.service.masterdata.MasterDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master/dashboard")
public class MasterDashboardController {
    private static final Logger log = LoggerFactory.getLogger(MasterDashboardController.class);

    private final MasterDataService masterDataService;

    public MasterDashboardController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping
    public MasterDashboardResponse getDashboard(@RequestParam(defaultValue = "false") boolean includeDeleted) {
        log.info("Fetching master dashboard. includeDeleted={}", includeDeleted);
        return masterDataService.getDashboard(includeDeleted);
    }
}
