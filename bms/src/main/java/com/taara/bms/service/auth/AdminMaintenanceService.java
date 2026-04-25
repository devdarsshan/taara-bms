package com.taara.bms.service.auth;

import com.taara.bms.dto.common.MessageResponse;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdminMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(AdminMaintenanceService.class);

    private static final List<String> BUSINESS_TABLES = List.of(
            "packing_entries",
            "printing_deliveries",
            "printing_orders",
            "stitching_delivery_allocations",
            "stitching_deliveries",
            "stitching_order_rows",
            "stitching_orders",
            "cutting_rows",
            "cuttings",
            "inhouse_stock_splits",
            "inhouse_deliveries",
            "spinning_deliveries",
            "spinning_orders",
            "yarn_orders",
            "stitching_sections",
            "dias",
            "styles"
    );

    private static final List<String> AUTO_ID_SEQUENCES = List.of(
            "style_auto_seq",
            "dia_auto_seq",
            "stitching_section_auto_seq",
            "yarn_order_auto_seq",
            "spinning_order_auto_seq",
            "spinning_delivery_auto_seq",
            "inhouse_delivery_auto_seq",
            "inhouse_stock_split_auto_seq",
            "cutting_auto_seq",
            "stitching_order_auto_seq",
            "stitching_delivery_auto_seq",
            "printing_order_auto_seq",
            "printing_delivery_auto_seq",
            "packing_auto_seq"
    );

    private final EntityManager entityManager;

    public AdminMaintenanceService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public MessageResponse resetBusinessData() {
        log.warn("Admin maintenance reset requested. Clearing {} business tables while preserving app_users.", BUSINESS_TABLES.size());
        entityManager.createNativeQuery("truncate table " + String.join(", ", BUSINESS_TABLES) + " restart identity cascade")
                .executeUpdate();
        for (String sequence : AUTO_ID_SEQUENCES) {
            entityManager.createNativeQuery("alter sequence " + sequence + " restart with 1")
                    .executeUpdate();
        }
        log.warn("Admin maintenance reset completed. app_users table was preserved.");
        return new MessageResponse("Business data has been cleared. Allowed users were preserved.");
    }
}
