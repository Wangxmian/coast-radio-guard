package com.coast.radio.guard.ai.client;

import com.coast.radio.guard.common.constants.ResultCode;
import com.coast.radio.guard.config.AiProperties;
import com.coast.radio.guard.dto.ai.RealtimeTranscribeResponseDTO;
import com.coast.radio.guard.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class RealtimeAsrClientImpl implements RealtimeAsrClient {

    private final RestTemplate restTemplate;
    private final AiProperties aiProperties;

    public RealtimeAsrClientImpl(@Qualifier("aiRestTemplate") RestTemplate restTemplate,
                                 AiProperties aiProperties) {
        this.restTemplate = restTemplate;
        this.aiProperties = aiProperties;
    }

    @Override
    public RealtimeTranscribeResponseDTO transcribeChunk(byte[] bytes,
                                                         String fileName,
                                                         Long taskId,
                                                         Integer startTime,
                                                         Integer endTime) {
        String url = aiProperties.getBaseUrl() + aiProperties.getRealtimeAsrPath();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return fileName == null || fileName.isBlank() ? "chunk.wav" : fileName;
                }
            };

            body.add("audio", resource);
            if (taskId != null) {
                body.add("taskId", String.valueOf(taskId));
            }
            if (startTime != null) {
                body.add("startTime", String.valueOf(startTime));
            }
            if (endTime != null) {
                body.add("endTime", String.valueOf(endTime));
            }

            HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);
            RealtimeTranscribeResponseDTO resp = restTemplate.postForObject(url, req, RealtimeTranscribeResponseDTO.class);
            if (resp == null || !Boolean.TRUE.equals(resp.getSuccess())) {
                throw new BusinessException(ResultCode.SERVER_ERROR, "实时转录服务返回失败");
            }
            return resp;
        } catch (HttpStatusCodeException ex) {
            log.error("Realtime ASR call failed, status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "实时转录服务调用失败: " + ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            log.error("Realtime ASR call failed", ex);
            throw new BusinessException(ResultCode.SERVER_ERROR, "实时转录服务调用失败: " + ex.getMessage());
        }
    }
}
