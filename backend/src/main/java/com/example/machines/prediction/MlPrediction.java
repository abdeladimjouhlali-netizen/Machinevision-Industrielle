package com.example.machines.prediction;

import java.util.Map;

public record MlPrediction(String category, double confidence, Map<String, Double> scores, String disclaimer) {}
