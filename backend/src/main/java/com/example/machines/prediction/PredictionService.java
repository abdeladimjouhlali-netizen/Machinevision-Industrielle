package com.example.machines.prediction;

import com.example.machines.equipment.EquipmentEntity;
import com.example.machines.equipment.EquipmentRepository;
import com.example.machines.equipment.EquipmentView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class PredictionService {
    private final HttpClient mlClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper objectMapper;
    private final String mlBaseUrl;
    private final PredictionRepository predictionRepository;
    private final EquipmentRepository equipmentRepository;

    public PredictionService(ObjectMapper objectMapper,
                             @Value("${app.ml-base-url}") String mlBaseUrl,
                             PredictionRepository predictionRepository,
                             EquipmentRepository equipmentRepository) {
        this.objectMapper = objectMapper;
        this.mlBaseUrl = mlBaseUrl.replaceAll("/+$", "");
        this.predictionRepository = predictionRepository;
        this.equipmentRepository = equipmentRepository;
    }

    public PredictionResponse predict(MultipartFile image, String requestedAssetTag) {
        if (image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ajoute une photo.");
        }

        MlPrediction mlResult;
        try {
            byte[] bytes = image.getBytes();
            String boundary = "----MachineVision" + UUID.randomUUID().toString().replace("-", "");
            String filename = image.getOriginalFilename() == null ? "machine-image" : image.getOriginalFilename();
            filename = filename.replaceAll("[\\r\\n\\\"]", "_");
            String contentType = image.getContentType() == null ? "application/octet-stream" : image.getContentType();
            byte[] multipart = multipartBody(boundary, filename, contentType, bytes);
            HttpRequest request = HttpRequest.newBuilder(URI.create(mlBaseUrl + "/predict"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipart))
                    .build();
            HttpResponse<String> response = mlClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Le service de prédiction a refusé l’image (HTTP " + response.statusCode() + ").");
            }
            mlResult = objectMapper.readValue(response.body(), MlPrediction.class);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "L’appel au service de prédiction a été interrompu.", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Le service de prédiction n’a pas répondu.", exception);
        }
        if (mlResult == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Réponse vide du service de prédiction.");
        }

        String assetTag = requestedAssetTag == null || requestedAssetTag.isBlank() ? null : requestedAssetTag.trim();
        EquipmentEntity equipment = assetTag == null ? null : equipmentRepository.findById(assetTag).orElse(null);
        PredictionEntity saved = predictionRepository.save(
                new PredictionEntity(equipment == null ? null : equipment.getAssetTag(), mlResult.category(), mlResult.confidence())
        );
        EquipmentView equipmentView = equipment == null ? null : toView(equipment);

        return new PredictionResponse(saved.getId(), assetTag, mlResult.category(), mlResult.confidence(),
                saved.getCreatedAt(), mlResult.disclaimer(), equipmentView);
    }

    private EquipmentView toView(EquipmentEntity equipment) {
        String base = "/api/equipment/" + equipment.getAssetTag() + "/documents/";
        return new EquipmentView(
                equipment.getAssetTag(), equipment.getCategory(), equipment.getManufacturer(), equipment.getModel(),
                equipment.getTechnicalSheetFilename() == null ? null : base + "fiche",
                equipment.getManualFilename() == null ? null : base + "manuel"
        );
    }

    private static byte[] multipartBody(String boundary, String filename, String contentType, byte[] fileBytes)
            throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(fileBytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return body.toByteArray();
    }
}
