package com.taara.bms.service.common;

import com.taara.bms.entity.inhouse.CuttingEntry;
import com.taara.bms.entity.inhouse.InHouseDelivery;
import com.taara.bms.entity.inhouse.InHouseStockSplit;
import com.taara.bms.entity.masterdata.Dia;
import com.taara.bms.entity.masterdata.StitchingSection;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.entity.packing.PackingEntry;
import com.taara.bms.entity.printing.PrintingDelivery;
import com.taara.bms.entity.printing.PrintingOrder;
import com.taara.bms.entity.spinning.SpinningDelivery;
import com.taara.bms.entity.spinning.SpinningOrder;
import com.taara.bms.entity.stitching.StitchingDelivery;
import com.taara.bms.entity.stitching.StitchingOrder;
import com.taara.bms.entity.yarn.YarnOrder;
import com.taara.bms.exception.ResourceNotFoundException;
import com.taara.bms.repo.inhouse.CuttingEntryRepository;
import com.taara.bms.repo.inhouse.InHouseDeliveryRepository;
import com.taara.bms.repo.inhouse.InHouseStockSplitRepository;
import com.taara.bms.repo.masterdata.DiaRepository;
import com.taara.bms.repo.masterdata.StitchingSectionRepository;
import com.taara.bms.repo.masterdata.StyleRepository;
import com.taara.bms.repo.packing.PackingEntryRepository;
import com.taara.bms.repo.printing.PrintingDeliveryRepository;
import com.taara.bms.repo.printing.PrintingOrderRepository;
import com.taara.bms.repo.spinning.SpinningDeliveryRepository;
import com.taara.bms.repo.spinning.SpinningOrderRepository;
import com.taara.bms.repo.stitching.StitchingDeliveryRepository;
import com.taara.bms.repo.stitching.StitchingOrderRepository;
import com.taara.bms.repo.yarn.YarnOrderRepository;
import org.springframework.stereotype.Service;

@Service
public class LookupService {

    private final StyleRepository styleRepository;
    private final DiaRepository diaRepository;
    private final StitchingSectionRepository stitchingSectionRepository;
    private final YarnOrderRepository yarnOrderRepository;
    private final SpinningOrderRepository spinningOrderRepository;
    private final SpinningDeliveryRepository spinningDeliveryRepository;
    private final InHouseDeliveryRepository inHouseDeliveryRepository;
    private final InHouseStockSplitRepository inHouseStockSplitRepository;
    private final CuttingEntryRepository cuttingEntryRepository;
    private final StitchingOrderRepository stitchingOrderRepository;
    private final StitchingDeliveryRepository stitchingDeliveryRepository;
    private final PrintingOrderRepository printingOrderRepository;
    private final PrintingDeliveryRepository printingDeliveryRepository;
    private final PackingEntryRepository packingEntryRepository;

    public LookupService(
            StyleRepository styleRepository,
            DiaRepository diaRepository,
            StitchingSectionRepository stitchingSectionRepository,
            YarnOrderRepository yarnOrderRepository,
            SpinningOrderRepository spinningOrderRepository,
            SpinningDeliveryRepository spinningDeliveryRepository,
            InHouseDeliveryRepository inHouseDeliveryRepository,
            InHouseStockSplitRepository inHouseStockSplitRepository,
            CuttingEntryRepository cuttingEntryRepository,
            StitchingOrderRepository stitchingOrderRepository,
            StitchingDeliveryRepository stitchingDeliveryRepository,
            PrintingOrderRepository printingOrderRepository,
            PrintingDeliveryRepository printingDeliveryRepository,
            PackingEntryRepository packingEntryRepository
    ) {
        this.styleRepository = styleRepository;
        this.diaRepository = diaRepository;
        this.stitchingSectionRepository = stitchingSectionRepository;
        this.yarnOrderRepository = yarnOrderRepository;
        this.spinningOrderRepository = spinningOrderRepository;
        this.spinningDeliveryRepository = spinningDeliveryRepository;
        this.inHouseDeliveryRepository = inHouseDeliveryRepository;
        this.inHouseStockSplitRepository = inHouseStockSplitRepository;
        this.cuttingEntryRepository = cuttingEntryRepository;
        this.stitchingOrderRepository = stitchingOrderRepository;
        this.stitchingDeliveryRepository = stitchingDeliveryRepository;
        this.printingOrderRepository = printingOrderRepository;
        this.printingDeliveryRepository = printingDeliveryRepository;
        this.packingEntryRepository = packingEntryRepository;
    }

    public Style getActiveStyleByAutoId(String autoId) {
        Style style = styleRepository.findByAutoIdIgnoreCase(normalize(autoId))
                .orElseThrow(() -> new ResourceNotFoundException("STYLE_NOT_FOUND", "Style not found"));
        if (style.isDeleted()) {
            throw new ResourceNotFoundException("STYLE_NOT_FOUND", "Style not found");
        }
        return style;
    }

    public Dia getActiveDiaByAutoId(String autoId) {
        Dia dia = diaRepository.findByAutoIdIgnoreCase(normalize(autoId))
                .orElseThrow(() -> new ResourceNotFoundException("DIA_NOT_FOUND", "Dia not found"));
        if (dia.isDeleted()) {
            throw new ResourceNotFoundException("DIA_NOT_FOUND", "Dia not found");
        }
        return dia;
    }

    public StitchingSection getActiveSectionByAutoId(String autoId) {
        StitchingSection section = stitchingSectionRepository.findByAutoIdIgnoreCase(normalize(autoId))
                .orElseThrow(() -> new ResourceNotFoundException("SECTION_NOT_FOUND", "Stitching section not found"));
        if (section.isDeleted()) {
            throw new ResourceNotFoundException("SECTION_NOT_FOUND", "Stitching section not found");
        }
        return section;
    }

    public YarnOrder getActiveYarnOrderByAutoId(String autoId) {
        YarnOrder yarnOrder = yarnOrderRepository.findByAutoIdIgnoreCase(normalize(autoId))
                .orElseThrow(() -> new ResourceNotFoundException("YARN_ORDER_NOT_FOUND", "Yarn order not found"));
        if (yarnOrder.isDeleted()) {
            throw new ResourceNotFoundException("YARN_ORDER_NOT_FOUND", "Yarn order not found");
        }
        return yarnOrder;
    }

    public SpinningOrder getActiveSpinningOrderByAutoId(String autoId) {
        SpinningOrder spinningOrder = spinningOrderRepository.findByAutoIdIgnoreCase(normalize(autoId))
                .orElseThrow(() -> new ResourceNotFoundException("SPINNING_ORDER_NOT_FOUND", "Spinning order not found"));
        if (spinningOrder.isDeleted()) {
            throw new ResourceNotFoundException("SPINNING_ORDER_NOT_FOUND", "Spinning order not found");
        }
        return spinningOrder;
    }

    public SpinningDelivery getActiveSpinningDeliveryByAutoId(String autoId) {
        SpinningDelivery spinningDelivery = spinningDeliveryRepository.findByAutoIdIgnoreCase(normalize(autoId))
                .orElseThrow(() -> new ResourceNotFoundException("SPINNING_DELIVERY_NOT_FOUND", "Spinning delivery not found"));
        if (spinningDelivery.isDeleted()) {
            throw new ResourceNotFoundException("SPINNING_DELIVERY_NOT_FOUND", "Spinning delivery not found");
        }
        return spinningDelivery;
    }

    public InHouseDelivery getActiveInHouseDeliveryByAutoId(String autoId) {
        InHouseDelivery inHouseDelivery = inHouseDeliveryRepository.findByAutoIdIgnoreCase(normalize(autoId))
                .orElseThrow(() -> new ResourceNotFoundException("INHOUSE_DELIVERY_NOT_FOUND", "In-house delivery not found"));
        if (inHouseDelivery.isDeleted()) {
            throw new ResourceNotFoundException("INHOUSE_DELIVERY_NOT_FOUND", "In-house delivery not found");
        }
        return inHouseDelivery;
    }

    public InHouseStockSplit getActiveInHouseStockSplitByAutoId(String autoId) {
        InHouseStockSplit split = inHouseStockSplitRepository.findByAutoIdIgnoreCase(normalize(autoId))
                .orElseThrow(() -> new ResourceNotFoundException("INHOUSE_SPLIT_NOT_FOUND", "In-house split not found"));
        if (split.isDeleted()) {
            throw new ResourceNotFoundException("INHOUSE_SPLIT_NOT_FOUND", "In-house split not found");
        }
        return split;
    }

    public CuttingEntry getActiveCuttingEntryByAutoId(String autoId) {
        CuttingEntry cuttingEntry = cuttingEntryRepository.findByAutoIdIgnoreCase(normalize(autoId))
                .orElseThrow(() -> new ResourceNotFoundException("CUTTING_NOT_FOUND", "Cutting entry not found"));
        if (cuttingEntry.isDeleted()) {
            throw new ResourceNotFoundException("CUTTING_NOT_FOUND", "Cutting entry not found");
        }
        return cuttingEntry;
    }

    public StitchingOrder getActiveStitchingOrderByAutoId(String autoId) {
        StitchingOrder stitchingOrder = stitchingOrderRepository.findByAutoIdIgnoreCase(normalize(autoId))
                .orElseThrow(() -> new ResourceNotFoundException("STITCHING_ORDER_NOT_FOUND", "Stitching order not found"));
        if (stitchingOrder.isDeleted()) {
            throw new ResourceNotFoundException("STITCHING_ORDER_NOT_FOUND", "Stitching order not found");
        }
        return stitchingOrder;
    }

    public StitchingDelivery getActiveStitchingDeliveryByAutoId(String autoId) {
        StitchingDelivery stitchingDelivery = stitchingDeliveryRepository.findByAutoIdIgnoreCase(normalize(autoId))
                .orElseThrow(() -> new ResourceNotFoundException("STITCHING_DELIVERY_NOT_FOUND", "Stitching delivery not found"));
        if (stitchingDelivery.isDeleted()) {
            throw new ResourceNotFoundException("STITCHING_DELIVERY_NOT_FOUND", "Stitching delivery not found");
        }
        return stitchingDelivery;
    }

    public PrintingOrder getActivePrintingOrderByAutoId(String autoId) {
        PrintingOrder printingOrder = printingOrderRepository.findByAutoIdIgnoreCase(normalize(autoId))
                .orElseThrow(() -> new ResourceNotFoundException("PRINTING_ORDER_NOT_FOUND", "Printing order not found"));
        if (printingOrder.isDeleted()) {
            throw new ResourceNotFoundException("PRINTING_ORDER_NOT_FOUND", "Printing order not found");
        }
        return printingOrder;
    }

    public PrintingDelivery getActivePrintingDeliveryByAutoId(String autoId) {
        PrintingDelivery printingDelivery = printingDeliveryRepository.findByAutoIdIgnoreCase(normalize(autoId))
                .orElseThrow(() -> new ResourceNotFoundException("PRINTING_DELIVERY_NOT_FOUND", "Printing delivery not found"));
        if (printingDelivery.isDeleted()) {
            throw new ResourceNotFoundException("PRINTING_DELIVERY_NOT_FOUND", "Printing delivery not found");
        }
        return printingDelivery;
    }

    public PackingEntry getActivePackingEntryByAutoId(String autoId) {
        PackingEntry packingEntry = packingEntryRepository.findByAutoIdIgnoreCase(normalize(autoId))
                .orElseThrow(() -> new ResourceNotFoundException("PACKING_NOT_FOUND", "Packing entry not found"));
        if (packingEntry.isDeleted()) {
            throw new ResourceNotFoundException("PACKING_NOT_FOUND", "Packing entry not found");
        }
        return packingEntry;
    }

    private String normalize(String autoId) {
        return autoId == null ? null : autoId.trim();
    }
}
