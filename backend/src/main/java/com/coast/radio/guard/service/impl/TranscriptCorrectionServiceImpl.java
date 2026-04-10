package com.coast.radio.guard.service.impl;

import com.coast.radio.guard.ai.client.LlmClient;
import com.coast.radio.guard.dto.ai.LlmCorrectionResponseDTO;
import com.coast.radio.guard.service.TranscriptCorrectionService;
import com.coast.radio.guard.service.model.TranscriptCorrectionResult;
import com.coast.radio.guard.util.TranscriptCorrectionDiffUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class TranscriptCorrectionServiceImpl implements TranscriptCorrectionService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public TranscriptCorrectionServiceImpl(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public TranscriptCorrectionResult correctTranscript(Long taskId, String rawTranscript, boolean correctionEnabled, boolean llmEnabled) {
        String raw = rawTranscript == null ? "" : rawTranscript;
        if (raw.isBlank()) {
            return fallback(raw, "empty transcript", "empty");
        }
        if (!llmEnabled) {
            return fallback(raw, "llmEnabled=false", "disabled");
        }
        if (!correctionEnabled) {
            return fallback(raw, "correctionEnabled=false", "disabled");
        }

        log.info("Transcript correction start, taskId={}", taskId);
        try {
            LlmCorrectionResponseDTO resp = llmClient.correctTranscript(taskId, raw);
            String corrected = normalizeCorrected(resp.getCorrectedTranscript(), raw);
            String provider = resolveProvider(resp.getProviderInfo(), resp.getStageInfo(), "correction");
            boolean fallback = Boolean.TRUE.equals(readFallback(resp.getFallbackInfo(), "correction"));
            String fallbackReason = readFallbackReason(resp.getFallbackInfo(), "correction");
            String diff = toJson(TranscriptCorrectionDiffUtil.build(raw, corrected));
            log.info("Transcript correction success, taskId={}, provider={}, fallback={}", taskId, provider, fallback);
            return TranscriptCorrectionResult.builder()
                .rawTranscript(raw)
                .correctedTranscript(corrected)
                .correctionDiff(diff)
                .correctionProvider(provider)
                .correctionFallback(fallback || corrected.equals(raw))
                .fallbackReason(fallbackReason)
                .providerInfo(resp.getProviderInfo() == null ? Map.of() : resp.getProviderInfo())
                .stageInfo(resp.getStageInfo() == null ? Map.of() : resp.getStageInfo())
                .fallbackInfo(resp.getFallbackInfo() == null ? Map.of() : resp.getFallbackInfo())
                .build();
        } catch (Exception ex) {
            log.warn("Transcript correction failed, taskId={}, err={}", taskId, ex.getMessage());
            return fallback(raw, ex.getMessage(), "error");
        }
    }

    private TranscriptCorrectionResult fallback(String raw, String reason, String provider) {
        Map<String, Object> providerInfo = new HashMap<>();
        Map<String, Object> stageInfo = new HashMap<>();
        Map<String, Object> fallbackInfo = new HashMap<>();
        providerInfo.put("correction", provider);
        stageInfo.put("correction", Map.of(
            "stage", "correction",
            "effectiveProvider", provider,
            "fallbackUsed", true,
            "fallbackReason", reason == null ? "fallback" : reason
        ));
        fallbackInfo.put("correction", Map.of(
            "fallbackUsed", true,
            "fallbackReason", reason == null ? "fallback" : reason
        ));
        return TranscriptCorrectionResult.builder()
            .rawTranscript(raw)
            .correctedTranscript(raw)
            .correctionDiff(toJson(TranscriptCorrectionDiffUtil.build(raw, raw)))
            .correctionProvider(provider)
            .correctionFallback(true)
            .fallbackReason(reason)
            .providerInfo(providerInfo)
            .stageInfo(stageInfo)
            .fallbackInfo(fallbackInfo)
            .build();
    }

    private String normalizeCorrected(String candidate, String raw) {
        if (candidate == null) {
            return raw;
        }
        String normalized = candidate.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "").trim();
        }
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.isBlank() ? raw : normalized;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private Boolean readFallback(Map<String, Object> fallbackInfo, String stage) {
        if (fallbackInfo == null) {
            return null;
        }
        Object node = fallbackInfo.get(stage);
        if (node instanceof Map<?, ?> map) {
            Object value = ((Map<String, Object>) map).get("fallbackUsed");
            return value instanceof Boolean b ? b : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String readFallbackReason(Map<String, Object> fallbackInfo, String stage) {
        if (fallbackInfo == null) {
            return null;
        }
        Object node = fallbackInfo.get(stage);
        if (node instanceof Map<?, ?> map) {
            Object value = ((Map<String, Object>) map).get("fallbackReason");
            return value == null ? null : String.valueOf(value);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String resolveProvider(Map<String, Object> providerInfo, Map<String, Object> stageInfo, String stage) {
        if (providerInfo != null && providerInfo.get(stage) != null) {
            return String.valueOf(providerInfo.get(stage));
        }
        if (stageInfo != null) {
            Object stageNode = stageInfo.get(stage);
            if (stageNode instanceof Map<?, ?> stageMap && ((Map<String, Object>) stageMap).get("effectiveProvider") != null) {
                return String.valueOf(((Map<String, Object>) stageMap).get("effectiveProvider"));
            }
        }
        return null;
    }
}
