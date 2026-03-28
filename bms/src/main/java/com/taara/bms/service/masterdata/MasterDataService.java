package com.taara.bms.service.masterdata;

import com.taara.bms.dto.masterdata.DiaResponse;
import com.taara.bms.dto.masterdata.DiaUpsertRequest;
import com.taara.bms.dto.masterdata.MasterDashboardResponse;
import com.taara.bms.dto.masterdata.StitchingSectionResponse;
import com.taara.bms.dto.masterdata.StitchingSectionUpsertRequest;
import com.taara.bms.dto.masterdata.StyleResponse;
import com.taara.bms.dto.masterdata.StyleUpsertRequest;
import com.taara.bms.entity.masterdata.Dia;
import com.taara.bms.entity.masterdata.StitchingSection;
import com.taara.bms.entity.masterdata.Style;
import com.taara.bms.exception.DeleteConflictException;
import com.taara.bms.exception.ResourceNotFoundException;
import com.taara.bms.mapper.masterdata.MasterDataMapper;
import com.taara.bms.repo.inhouse.CuttingEntryRepository;
import com.taara.bms.repo.inhouse.InHouseDeliveryRepository;
import com.taara.bms.repo.inhouse.InHouseStockSplitRepository;
import com.taara.bms.repo.masterdata.DiaRepository;
import com.taara.bms.repo.masterdata.StitchingSectionRepository;
import com.taara.bms.repo.masterdata.StyleRepository;
import com.taara.bms.repo.printing.PrintingOrderRepository;
import com.taara.bms.repo.spinning.SpinningDeliveryRepository;
import com.taara.bms.repo.spinning.SpinningOrderRepository;
import com.taara.bms.repo.stitching.StitchingOrderRepository;
import com.taara.bms.repo.yarn.YarnOrderRepository;
import com.taara.bms.service.common.AutoIdSequence;
import com.taara.bms.service.common.AutoIdService;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MasterDataService {

    private static final Logger log = LoggerFactory.getLogger(MasterDataService.class);

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
    private final PrintingOrderRepository printingOrderRepository;
    private final AutoIdService autoIdService;
    private final MasterDataMapper mapper;

    public MasterDataService(
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
            PrintingOrderRepository printingOrderRepository,
            AutoIdService autoIdService,
            MasterDataMapper mapper
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
        this.printingOrderRepository = printingOrderRepository;
        this.autoIdService = autoIdService;
        this.mapper = mapper;
    }

    public Page<StyleResponse> getStyles(String search, boolean includeDeleted, Pageable pageable) {
        log.info("Fetching styles with search='{}', includeDeleted={}, pageable={}", search, includeDeleted, pageable);
        Page<StyleResponse> styles = styleRepository.findAll(styleSpec(search, includeDeleted), pageable).map(mapper::toStyleResponse);
        log.info("Fetched {} style records", styles.getNumberOfElements());
        return styles;
    }

    public Page<DiaResponse> getDias(String search, boolean includeDeleted, Pageable pageable) {
        log.info("Fetching dias with search='{}', includeDeleted={}, pageable={}", search, includeDeleted, pageable);
        Page<DiaResponse> dias = diaRepository.findAll(diaSpec(search, includeDeleted), pageable).map(mapper::toDiaResponse);
        log.info("Fetched {} dia records", dias.getNumberOfElements());
        return dias;
    }

    public Page<StitchingSectionResponse> getSections(String search, boolean includeDeleted, Pageable pageable) {
        log.info("Fetching stitching sections with search='{}', includeDeleted={}, pageable={}", search, includeDeleted, pageable);
        Page<StitchingSectionResponse> sections = stitchingSectionRepository.findAll(sectionSpec(search, includeDeleted), pageable)
                .map(mapper::toSectionResponse);
        log.info("Fetched {} stitching section records", sections.getNumberOfElements());
        return sections;
    }

    public MasterDashboardResponse getDashboard(boolean includeDeleted) {
        log.info("Fetching master dashboard with includeDeleted={}", includeDeleted);
        Specification<Style> styleSpec = styleSpec(null, includeDeleted);
        Specification<Dia> diaSpec = diaSpec(null, includeDeleted);
        Specification<StitchingSection> sectionSpec = sectionSpec(null, includeDeleted);
        MasterDashboardResponse response = new MasterDashboardResponse(
                styleRepository.count(styleSpec),
                diaRepository.count(diaSpec),
                stitchingSectionRepository.count(sectionSpec)
        );
        log.info("Master dashboard calculated: styles={}, dias={}, sections={}",
                response.totalStyles(), response.totalDiaTypes(), response.totalStitchingSections());
        return response;
    }

    @Transactional
    public StyleResponse createStyle(StyleUpsertRequest request) {
        log.info("Creating style with name='{}'", request.styleName());
        if (styleRepository.existsByStyleNameIgnoreCaseAndIsDeletedFalse(request.styleName().trim())) {
            throw new DeleteConflictException("STYLE_NAME_EXISTS", "Style name already exists", Map.of("styleName", request.styleName()));
        }
        Style style = new Style();
        style.setAutoId(autoIdService.next(AutoIdSequence.STYLE));
        applyStyle(style, request);
        StyleResponse response = mapper.toStyleResponse(styleRepository.save(style));
        log.info("Created style '{}'", response.autoId());
        return response;
    }

    @Transactional
    public StyleResponse updateStyle(String autoId, StyleUpsertRequest request) {
        log.info("Updating style '{}'", autoId);
        Style style = getStyleEntity(autoId);
        if (styleRepository.existsByStyleNameIgnoreCaseAndIdNotAndIsDeletedFalse(request.styleName().trim(), style.getId())) {
            throw new DeleteConflictException("STYLE_NAME_EXISTS", "Style name already exists", Map.of("styleName", request.styleName()));
        }
        applyStyle(style, request);
        StyleResponse response = mapper.toStyleResponse(styleRepository.save(style));
        log.info("Updated style '{}'", response.autoId());
        return response;
    }

    @Transactional
    public void deleteStyle(String autoId) {
        log.info("Deleting style '{}'", autoId);
        Style style = getStyleEntity(autoId);
        if (isStyleReferenced(style.getId())) {
            throw new DeleteConflictException(
                    "STYLE_IN_USE",
                    "Style cannot be deleted because it is referenced by active transactions",
                    Map.of("styleAutoId", style.getAutoId())
            );
        }
        style.setDeleted(true);
        styleRepository.save(style);
        log.info("Deleted style '{}'", autoId);
    }

    @Transactional
    public DiaResponse createDia(DiaUpsertRequest request) {
        log.info("Creating dia '{}'", request.diaValue());
        if (diaRepository.existsByDiaValueIgnoreCaseAndIsDeletedFalse(request.diaValue().trim())) {
            throw new DeleteConflictException("DIA_VALUE_EXISTS", "Dia value already exists", Map.of("diaValue", request.diaValue()));
        }
        Dia dia = new Dia();
        dia.setAutoId(autoIdService.next(AutoIdSequence.DIA));
        dia.setDiaValue(request.diaValue().trim());
        DiaResponse response = mapper.toDiaResponse(diaRepository.save(dia));
        log.info("Created dia '{}'", response.autoId());
        return response;
    }

    @Transactional
    public DiaResponse updateDia(String autoId, DiaUpsertRequest request) {
        log.info("Updating dia '{}'", autoId);
        Dia dia = getDiaEntity(autoId);
        if (diaRepository.existsByDiaValueIgnoreCaseAndIdNotAndIsDeletedFalse(request.diaValue().trim(), dia.getId())) {
            throw new DeleteConflictException("DIA_VALUE_EXISTS", "Dia value already exists", Map.of("diaValue", request.diaValue()));
        }
        dia.setDiaValue(request.diaValue().trim());
        DiaResponse response = mapper.toDiaResponse(diaRepository.save(dia));
        log.info("Updated dia '{}'", response.autoId());
        return response;
    }

    @Transactional
    public void deleteDia(String autoId) {
        log.info("Deleting dia '{}'", autoId);
        Dia dia = getDiaEntity(autoId);
        if (inHouseStockSplitRepository.existsByDia_IdAndIsDeletedFalse(dia.getId()) || cuttingEntryRepository.existsActiveByRowDiaId(dia.getId())) {
            throw new DeleteConflictException("DIA_IN_USE", "Dia cannot be deleted because it is referenced by active transactions", Map.of("diaAutoId", dia.getAutoId()));
        }
        dia.setDeleted(true);
        diaRepository.save(dia);
        log.info("Deleted dia '{}'", autoId);
    }

    @Transactional
    public StitchingSectionResponse createSection(StitchingSectionUpsertRequest request) {
        log.info("Creating stitching section with name='{}' and type={}", request.sectionName(), request.type());
        if (stitchingSectionRepository.existsBySectionNameIgnoreCaseAndIsDeletedFalse(request.sectionName().trim())) {
            throw new DeleteConflictException("SECTION_NAME_EXISTS", "Section name already exists", Map.of("sectionName", request.sectionName()));
        }
        StitchingSection section = new StitchingSection();
        section.setAutoId(autoIdService.next(AutoIdSequence.STITCHING_SECTION));
        section.setSectionName(request.sectionName().trim());
        section.setType(request.type());
        section.setProcessType(request.processType());
        StitchingSectionResponse response = mapper.toSectionResponse(stitchingSectionRepository.save(section));
        log.info("Created stitching section '{}'", response.autoId());
        return response;
    }

    @Transactional
    public StitchingSectionResponse updateSection(String autoId, StitchingSectionUpsertRequest request) {
        log.info("Updating stitching section '{}'", autoId);
        StitchingSection section = getSectionEntity(autoId);
        if (stitchingSectionRepository.existsBySectionNameIgnoreCaseAndIdNotAndIsDeletedFalse(request.sectionName().trim(), section.getId())) {
            throw new DeleteConflictException("SECTION_NAME_EXISTS", "Section name already exists", Map.of("sectionName", request.sectionName()));
        }
        section.setSectionName(request.sectionName().trim());
        section.setType(request.type());
        section.setProcessType(request.processType());
        StitchingSectionResponse response = mapper.toSectionResponse(stitchingSectionRepository.save(section));
        log.info("Updated stitching section '{}'", response.autoId());
        return response;
    }

    @Transactional
    public void deleteSection(String autoId) {
        log.info("Deleting stitching section '{}'", autoId);
        StitchingSection section = getSectionEntity(autoId);
        if (stitchingOrderRepository.existsActiveByRowSectionId(section.getId())
                || printingOrderRepository.existsByPrintingSection_IdAndIsDeletedFalse(section.getId())) {
            throw new DeleteConflictException("SECTION_IN_USE", "Section cannot be deleted because it has active stitching orders", Map.of("sectionAutoId", section.getAutoId()));
        }
        section.setDeleted(true);
        stitchingSectionRepository.save(section);
        log.info("Deleted stitching section '{}'", autoId);
    }

    private void applyStyle(Style style, StyleUpsertRequest request) {
        style.setStyleName(request.styleName().trim());
        style.setColors(request.colors() == null ? java.util.List.of() : request.colors().stream().map(String::trim).filter(color -> !color.isBlank()).toList());
    }

    private boolean isStyleReferenced(java.util.UUID styleId) {
        boolean referenced = yarnOrderRepository.existsByStyle_IdAndIsDeletedFalse(styleId)
                || spinningOrderRepository.existsByStyle_IdAndIsDeletedFalse(styleId)
                || spinningDeliveryRepository.existsByStyle_IdAndIsDeletedFalse(styleId)
                || inHouseDeliveryRepository.existsByStyle_IdAndIsDeletedFalse(styleId)
                || inHouseStockSplitRepository.existsByStyle_IdAndIsDeletedFalse(styleId)
                || cuttingEntryRepository.existsActiveByRowStyleId(styleId)
                || stitchingOrderRepository.existsActiveByRowStyleId(styleId)
                || printingOrderRepository.existsByStyle_IdAndIsDeletedFalse(styleId);
        log.debug("Style reference check for styleId={} returned {}", styleId, referenced);
        return referenced;
    }

    private Style getStyleEntity(String autoId) {
        Style style = styleRepository.findByAutoIdIgnoreCase(autoId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("STYLE_NOT_FOUND", "Style not found"));
        if (style.isDeleted()) {
            throw new ResourceNotFoundException("STYLE_NOT_FOUND", "Style not found");
        }
        return style;
    }

    private Dia getDiaEntity(String autoId) {
        Dia dia = diaRepository.findByAutoIdIgnoreCase(autoId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("DIA_NOT_FOUND", "Dia not found"));
        if (dia.isDeleted()) {
            throw new ResourceNotFoundException("DIA_NOT_FOUND", "Dia not found");
        }
        return dia;
    }

    private StitchingSection getSectionEntity(String autoId) {
        StitchingSection section = stitchingSectionRepository.findByAutoIdIgnoreCase(autoId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("SECTION_NOT_FOUND", "Stitching section not found"));
        if (section.isDeleted()) {
            throw new ResourceNotFoundException("SECTION_NOT_FOUND", "Stitching section not found");
        }
        return section;
    }

    private Specification<Style> styleSpec(String search, boolean includeDeleted) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (!includeDeleted) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
            }
            if (search != null && !search.isBlank()) {
                String likeValue = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("styleName")), likeValue),
                        cb.like(cb.lower(root.get("autoId")), likeValue)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<Dia> diaSpec(String search, boolean includeDeleted) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (!includeDeleted) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
            }
            if (search != null && !search.isBlank()) {
                String likeValue = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("diaValue")), likeValue),
                        cb.like(cb.lower(root.get("autoId")), likeValue)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<StitchingSection> sectionSpec(String search, boolean includeDeleted) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (!includeDeleted) {
                predicates.add(cb.isFalse(root.get("isDeleted")));
            }
            if (search != null && !search.isBlank()) {
                String likeValue = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("sectionName")), likeValue),
                        cb.like(cb.lower(root.get("autoId")), likeValue)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
