package com.example.machines.equipment;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {
    private final EquipmentRepository equipmentRepository;
    private final Path documentsRoot;

    public EquipmentController(EquipmentRepository equipmentRepository,
                               @Value("${app.documents-dir}") String documentsDir) {
        this.equipmentRepository = equipmentRepository;
        this.documentsRoot = Path.of(documentsDir).toAbsolutePath().normalize();
    }

    @GetMapping
    public List<EquipmentView> listEquipment(@RequestParam(required = false) String category) {
        List<EquipmentEntity> equipment = category == null || category.isBlank()
                ? equipmentRepository.findAll()
                : equipmentRepository.findAllByCategoryIgnoreCaseOrderByManufacturerAscModelAsc(category.trim());
        return equipment.stream().map(this::toView).toList();
    }

    @GetMapping("/{assetTag}")
    public EquipmentView getEquipment(@PathVariable String assetTag) {
        return equipmentRepository.findById(assetTag)
                .map(this::toView)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Équipement introuvable"));
    }

    @GetMapping("/{assetTag}/documents/{kind}")
    public ResponseEntity<Resource> getDocument(@PathVariable String assetTag,
                                                @PathVariable String kind) {
        EquipmentEntity equipment = equipmentRepository.findById(assetTag)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Équipement introuvable"));

        String filename = switch (kind) {
            case "fiche" -> equipment.getTechnicalSheetFilename();
            case "manuel" -> equipment.getManualFilename();
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document inconnu");
        };
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF non renseigné");
        }

        Path file = documentsRoot.resolve(filename).normalize();
        if (!file.startsWith(documentsRoot) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF absent du dossier documents");
        }

        Resource resource = new FileSystemResource(file);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(file.getFileName().toString()).build());
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    private EquipmentView toView(EquipmentEntity equipment) {
        String base = "/api/equipment/" + equipment.getAssetTag() + "/documents/";
        return new EquipmentView(
                equipment.getAssetTag(),
                equipment.getCategory(),
                equipment.getManufacturer(),
                equipment.getModel(),
                equipment.getTechnicalSheetFilename() == null ? null : base + "fiche",
                equipment.getManualFilename() == null ? null : base + "manuel"
        );
    }
}
