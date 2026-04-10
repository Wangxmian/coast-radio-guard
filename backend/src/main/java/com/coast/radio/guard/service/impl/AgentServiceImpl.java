package com.coast.radio.guard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coast.radio.guard.ai.client.LlmClient;
import com.coast.radio.guard.common.constants.ResultCode;
import com.coast.radio.guard.dto.ai.LlmChatResponseDTO;
import com.coast.radio.guard.entity.AlarmRecord;
import com.coast.radio.guard.entity.AudioTask;
import com.coast.radio.guard.exception.BusinessException;
import com.coast.radio.guard.mapper.AlarmRecordMapper;
import com.coast.radio.guard.mapper.AudioTaskMapper;
import com.coast.radio.guard.service.AgentAnswerFormatter;
import com.coast.radio.guard.service.AgentIntentResolver;
import com.coast.radio.guard.service.AgentQueryService;
import com.coast.radio.guard.service.AgentService;
import com.coast.radio.guard.service.model.AgentIntentMatch;
import com.coast.radio.guard.service.model.AgentIntentType;
import com.coast.radio.guard.service.model.AgentStructuredResult;
import com.coast.radio.guard.vo.agent.AgentChatVO;
import com.coast.radio.guard.vo.agent.AgentReportVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AgentServiceImpl implements AgentService {

    private static final String SOURCE_DATA = "data";
    private static final String SOURCE_LLM = "llm";
    private static final String SOURCE_FALLBACK = "fallback";

    private final LlmClient llmClient;
    private final AlarmRecordMapper alarmRecordMapper;
    private final AudioTaskMapper audioTaskMapper;
    private final AgentIntentResolver agentIntentResolver;
    private final AgentQueryService agentQueryService;
    private final AgentAnswerFormatter agentAnswerFormatter;

    public AgentServiceImpl(LlmClient llmClient,
                            AlarmRecordMapper alarmRecordMapper,
                            AudioTaskMapper audioTaskMapper,
                            AgentIntentResolver agentIntentResolver,
                            AgentQueryService agentQueryService,
                            AgentAnswerFormatter agentAnswerFormatter) {
        this.llmClient = llmClient;
        this.alarmRecordMapper = alarmRecordMapper;
        this.audioTaskMapper = audioTaskMapper;
        this.agentIntentResolver = agentIntentResolver;
        this.agentQueryService = agentQueryService;
        this.agentAnswerFormatter = agentAnswerFormatter;
    }

    @Override
    public AgentChatVO chat(String message) {
        String question = message == null ? "" : message.trim();
        if (question.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "message 不能为空");
        }

        AgentIntentMatch intentMatch = agentIntentResolver.resolve(question);
        AgentStructuredResult structuredResult = agentQueryService.query(question, intentMatch.getIntent());
        String fallbackAnswer = agentAnswerFormatter.format(question, structuredResult);

        if (structuredResult.isSupported()) {
            LlmAttemptResult enhanced = tryFormatStructuredAnswer(question, intentMatch, structuredResult, fallbackAnswer);
            boolean enhancedByRealLlm = isRealLlmAnswer(enhanced.response());
            return AgentChatVO.builder()
                .success(true)
                .answer(enhancedByRealLlm ? enhanced.response().getAnswer() : fallbackAnswer)
                .intent(intentMatch.getIntent().getCode())
                .confidence(intentMatch.getConfidence())
                .source(enhancedByRealLlm ? SOURCE_LLM : SOURCE_DATA)
                .fallback(enhanced.failed())
                .structuredData(structuredResult.getStructuredData())
                .mode("chat")
                .providerInfo(enhancedByRealLlm
                    ? safeMap(enhanced.response().getProviderInfo())
                    : Map.of("llm", "backend-template"))
                .fallbackInfo(enhancedByRealLlm
                    ? safeMap(enhanced.response().getFallbackInfo())
                    : buildFallbackInfo(enhanced.failed(), enhanced.reason()))
                .build();
        }

        LlmAttemptResult llmReply = tryChat(question);
        boolean useRealLlm = isRealLlmAnswer(llmReply.response());
        return AgentChatVO.builder()
            .success(true)
            .answer(useRealLlm ? llmReply.response().getAnswer() : fallbackAnswer)
            .intent(intentMatch.getIntent().getCode())
            .confidence(intentMatch.getConfidence())
            .source(useRealLlm ? SOURCE_LLM : SOURCE_FALLBACK)
            .fallback(llmReply.failed())
            .structuredData(null)
            .mode("chat")
            .providerInfo(useRealLlm ? safeMap(llmReply.response().getProviderInfo()) : Map.of("llm", "backend-template"))
            .fallbackInfo(useRealLlm ? safeMap(llmReply.response().getFallbackInfo()) : buildFallbackInfo(llmReply.failed(), llmReply.reason()))
            .build();
    }

    @Override
    public AgentReportVO report(String type) {
        String reportType = normalize(type);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        long todayTaskCount = audioTaskMapper.selectCount(new LambdaQueryWrapper<AudioTask>()
            .ge(AudioTask::getCreateTime, start)
            .lt(AudioTask::getCreateTime, end));
        long todayHighRiskTaskCount = audioTaskMapper.selectCount(new LambdaQueryWrapper<AudioTask>()
            .eq(AudioTask::getRiskLevel, "HIGH")
            .ge(AudioTask::getCreateTime, start)
            .lt(AudioTask::getCreateTime, end));
        List<AlarmRecord> todayAlarms = alarmRecordMapper.selectList(new LambdaQueryWrapper<AlarmRecord>()
            .ge(AlarmRecord::getCreateTime, start)
            .lt(AlarmRecord::getCreateTime, end)
            .orderByDesc(AlarmRecord::getCreateTime)
            .last("LIMIT 20"));
        long todayAlarmCount = todayAlarms.size();
        long todayHighAlarmCount = todayAlarms.stream()
            .filter(x -> "HIGH".equalsIgnoreCase(x.getAlarmLevel()))
            .count();

        List<AudioTask> highRiskTasks = audioTaskMapper.selectList(new LambdaQueryWrapper<AudioTask>()
            .eq(AudioTask::getRiskLevel, "HIGH")
            .orderByDesc(AudioTask::getUpdateTime)
            .last("LIMIT 10"));

        List<String> items = new ArrayList<>();
        items.add("今日任务数: " + todayTaskCount);
        items.add("今日告警数: " + todayAlarmCount);
        items.add("今日高风险任务: " + todayHighRiskTaskCount);
        items.add("今日高等级告警: " + todayHighAlarmCount);
        if (!highRiskTasks.isEmpty()) {
            items.add("最近高风险任务: " + highRiskTasks.stream()
                .limit(5)
                .map(t -> "#" + t.getId() + "(频道" + t.getChannelId() + "," + t.getTaskStatus() + ")")
                .reduce((a, b) -> a + "、" + b)
                .orElse("-"));
        }

        String prompt = switch (reportType) {
            case "high-risk-summary" -> "请基于以下监控统计，生成“高风险事件摘要”，按 3-5 条要点输出：\n" + String.join("\n", items);
            case "analysis-report" -> "请基于以下统计，生成“分析报告摘要”，包含风险趋势、告警建议、值守建议：\n" + String.join("\n", items);
            case "weekly" -> "请基于以下今日统计，输出一个“本周周报模板草稿”，并注明当前仍以今日数据估算：\n" + String.join("\n", items);
            default -> "请基于以下今日统计，生成“今日日报”摘要，要求简洁、专业：\n" + String.join("\n", items);
        };

        LlmChatResponseDTO llm = llmClient.chat(prompt);
        String title = switch (reportType) {
            case "high-risk-summary" -> "高风险事件摘要";
            case "analysis-report" -> "分析报告";
            case "weekly" -> "本周周报（草稿）";
            default -> "今日日报";
        };
        return AgentReportVO.builder()
            .title(title)
            .summary(llm.getAnswer())
            .items(items)
            .providerInfo(llm.getProviderInfo() == null ? Map.of() : llm.getProviderInfo())
            .fallbackInfo(llm.getFallbackInfo() == null ? Map.of() : llm.getFallbackInfo())
            .build();
    }

    private String normalize(String type) {
        if (type == null) {
            return "daily";
        }
        String t = type.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) {
            return "daily";
        }
        return t;
    }

    private LlmAttemptResult tryFormatStructuredAnswer(String question,
                                                       AgentIntentMatch intentMatch,
                                                       AgentStructuredResult structuredResult,
                                                       String fallbackAnswer) {
        if (!structuredResult.isHasData()) {
            return new LlmAttemptResult(null, false, false, "本次回答直接使用后端模板");
        }
        try {
            LlmChatResponseDTO response = llmClient.formatAnswer(
                intentMatch.getIntent().getCode(),
                question,
                structuredResult.getStructuredData(),
                fallbackAnswer
            );
            if (isRealLlmAnswer(response)) {
                return new LlmAttemptResult(response, true, false, null);
            }
            return new LlmAttemptResult(response, true, true, "文本增强未使用真实 LLM，已改用后端模板");
        } catch (BusinessException ex) {
            return new LlmAttemptResult(null, true, true, "文本增强服务不可用，已改用后端模板");
        }
    }

    private LlmAttemptResult tryChat(String question) {
        try {
            LlmChatResponseDTO response = llmClient.chat(question);
            if (isRealLlmAnswer(response)) {
                return new LlmAttemptResult(response, true, false, null);
            }
            return new LlmAttemptResult(response, true, true, "LLM 对话能力不可用，已返回系统引导提示");
        } catch (BusinessException ex) {
            return new LlmAttemptResult(null, true, true, "LLM 对话能力调用失败，已返回系统引导提示");
        }
    }

    private boolean isRealLlmAnswer(LlmChatResponseDTO resp) {
        if (resp == null || resp.getAnswer() == null || resp.getAnswer().isBlank()) {
            return false;
        }
        Object provider = safeMap(resp.getProviderInfo()).get("llm");
        if (!(provider instanceof String providerName) || providerName.isBlank()) {
            return false;
        }
        if (providerName.toLowerCase(Locale.ROOT).contains("mock")) {
            return false;
        }
        Object llmFallback = safeMap(resp.getFallbackInfo()).get("llm");
        if (llmFallback instanceof Map<?, ?> map) {
            Object fallbackUsed = map.get("fallbackUsed");
            if (Boolean.TRUE.equals(fallbackUsed)) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> safeMap(Map<String, Object> map) {
        return map == null ? Map.of() : map;
    }

    private Map<String, Object> buildFallbackInfo(boolean fallbackUsed, String reason) {
        return Map.of(
            "llm", Map.of(
                "fallbackUsed", fallbackUsed,
                "fallbackReason", reason
            )
        );
    }

    private record LlmAttemptResult(
        LlmChatResponseDTO response,
        boolean attempted,
        boolean failed,
        String reason
    ) {
    }
}
