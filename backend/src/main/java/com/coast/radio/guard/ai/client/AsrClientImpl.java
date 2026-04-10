package com.coast.radio.guard.ai.client;

import com.coast.radio.guard.common.constants.ResultCode;
import com.coast.radio.guard.config.AiProperties;
import com.coast.radio.guard.dto.ai.AsrTranscribeRequestDTO;
import com.coast.radio.guard.dto.ai.AsrTranscribeResponseDTO;
import com.coast.radio.guard.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class AsrClientImpl implements AsrClient {

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;

    public AsrClientImpl(@Qualifier("aiRestTemplate") RestTemplate restTemplate, AiProperties aiProperties) {
        this.restTemplate = restTemplate;
        this.aiProperties = aiProperties;
    }

    @Override
    public AsrTranscribeResponseDTO transcribe(Long taskId, String enhancedFilePath) {
        String url = aiProperties.getBaseUrl() + aiProperties.getAsrPath();
        try {
            AsrTranscribeResponseDTO resp = restTemplate.postForObject(
                url,
                new AsrTranscribeRequestDTO(taskId, enhancedFilePath),
                AsrTranscribeResponseDTO.class
            );
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess())) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "ASR 服务返回失败");
            }
            return resp;
        } catch (HttpStatusCodeException ex) {
            log.error("ASR call failed with status={}, taskId={}, body={}",
                ex.getStatusCode(), taskId, ex.getResponseBodyAsString(), ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "ASR 服务调用失败: " + ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            log.error("ASR call failed, taskId={}", taskId, ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "ASR 服务调用失败: " + ex.getMessage());
        }
    }
}
