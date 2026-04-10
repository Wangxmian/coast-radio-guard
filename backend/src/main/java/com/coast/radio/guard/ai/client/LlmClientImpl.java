package com.coast.radio.guard.ai.client;

import com.coast.radio.guard.common.constants.ResultCode;
import com.coast.radio.guard.config.AiProperties;
import com.coast.radio.guard.dto.ai.LlmAnalyzeRequestDTO;
import com.coast.radio.guard.dto.ai.LlmAnalyzeResponseDTO;
import com.coast.radio.guard.dto.ai.LlmChatRequestDTO;
import com.coast.radio.guard.dto.ai.LlmChatResponseDTO;
import com.coast.radio.guard.dto.ai.LlmCorrectionRequestDTO;
import com.coast.radio.guard.dto.ai.LlmCorrectionResponseDTO;
import com.coast.radio.guard.dto.ai.LlmFormatAnswerRequestDTO;
import com.coast.radio.guard.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class LlmClientImpl implements LlmClient {

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;

    public LlmClientImpl(@Qualifier("aiRestTemplate") RestTemplate restTemplate, AiProperties aiProperties) {
        this.restTemplate = restTemplate;
        this.aiProperties = aiProperties;
    }

    @Override
    public LlmAnalyzeResponseDTO analyze(Long taskId, String transcriptText) {
        String url = aiProperties.getBaseUrl() + aiProperties.getLlmPath();
        try {
            LlmAnalyzeResponseDTO resp = restTemplate.postForObject(
                url,
                new LlmAnalyzeRequestDTO(taskId, transcriptText),
                LlmAnalyzeResponseDTO.class
            );
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess())) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "LLM 服务返回失败");
            }
            return resp;
        } catch (HttpStatusCodeException ex) {
            log.error("LLM call failed with status={}, taskId={}, body={}",
                ex.getStatusCode(), taskId, ex.getResponseBodyAsString(), ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "LLM 服务调用失败: " + ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            log.error("LLM call failed, taskId={}", taskId, ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "LLM 服务调用失败: " + ex.getMessage());
        }
    }

    @Override
    public LlmCorrectionResponseDTO correctTranscript(Long taskId, String transcriptText) {
        String url = aiProperties.getBaseUrl() + aiProperties.getLlmCorrectionPath();
        try {
            LlmCorrectionResponseDTO resp = restTemplate.postForObject(
                url,
                new LlmCorrectionRequestDTO(taskId, transcriptText),
                LlmCorrectionResponseDTO.class
            );
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess())) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "LLM 纠错服务返回失败");
            }
            return resp;
        } catch (HttpStatusCodeException ex) {
            log.error("LLM correction call failed with status={}, taskId={}, body={}",
                ex.getStatusCode(), taskId, ex.getResponseBodyAsString(), ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "LLM 纠错服务调用失败: " + ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            log.error("LLM correction call failed, taskId={}", taskId, ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "LLM 纠错服务调用失败: " + ex.getMessage());
        }
    }

    @Override
    public LlmChatResponseDTO chat(String message) {
        String url = aiProperties.getBaseUrl() + aiProperties.getLlmChatPath();
        try {
            LlmChatResponseDTO resp = restTemplate.postForObject(
                url,
                new LlmChatRequestDTO(message),
                LlmChatResponseDTO.class
            );
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess())) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "LLM Chat 服务返回失败");
            }
            return resp;
        } catch (HttpStatusCodeException ex) {
            log.error("LLM chat call failed with status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "LLM Chat 服务调用失败: " + ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            log.error("LLM chat call failed", ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "LLM Chat 服务调用失败: " + ex.getMessage());
        }
    }

    @Override
    public LlmChatResponseDTO formatAnswer(String intent, String question, Object structuredData, String fallbackAnswer) {
        String url = aiProperties.getBaseUrl() + aiProperties.getLlmFormatAnswerPath();
        try {
            LlmChatResponseDTO resp = restTemplate.postForObject(
                url,
                new LlmFormatAnswerRequestDTO(intent, question, structuredData, fallbackAnswer),
                LlmChatResponseDTO.class
            );
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess())) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "LLM 文本增强服务返回失败");
            }
            return resp;
        } catch (HttpStatusCodeException ex) {
            log.error("LLM answer formatting failed with status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "LLM 文本增强服务调用失败: " + ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            log.error("LLM answer formatting failed", ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "LLM 文本增强服务调用失败: " + ex.getMessage());
        }
    }
}
