package com.example.machines.prediction;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {
    private final PredictionService predictionService;
    private final PredictionRepository predictionRepository;

    public PredictionController(PredictionService predictionService, PredictionRepository predictionRepository) {
        this.predictionService = predictionService;
        this.predictionRepository = predictionRepository;
    }

    @PostMapping(consumes = "multipart/form-data")
    public PredictionResponse predict(@RequestPart("file") MultipartFile file,
                                      @RequestParam(required = false) String assetTag) {
        return predictionService.predict(file, assetTag);
    }

    @GetMapping
    public List<PredictionEntity> recentPredictions() {
        return predictionRepository.findTop20ByOrderByCreatedAtDesc();
    }
}
