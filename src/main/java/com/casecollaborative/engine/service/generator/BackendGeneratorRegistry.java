package com.casecollaborative.engine.service.generator;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BackendGeneratorRegistry {

    private final Map<String, BackendGeneratorStrategy> strategies;

    public BackendGeneratorRegistry(List<BackendGeneratorStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(BackendGeneratorStrategy::getTargetIdentifier, strategy -> strategy));
    }

    public BackendGeneratorStrategy getStrategy(String targetIdentifier) {
        BackendGeneratorStrategy strategy = strategies.get(targetIdentifier);
        if (strategy == null) {
            throw new IllegalArgumentException("No generator strategy found for target: " + targetIdentifier);
        }
        return strategy;
    }
}
