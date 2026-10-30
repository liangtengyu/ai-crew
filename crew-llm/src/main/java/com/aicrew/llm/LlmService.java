package com.aicrew.llm;

import com.aicrew.common.entity.ModelConfig;
import com.aicrew.common.exception.BizException;
import com.aicrew.common.repository.ModelConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** LLM 调用门面：按模型配置 ID 发起对话，带配置校验 */
@Service
@RequiredArgsConstructor
public class LlmService {

    private final ModelConfigRepository modelConfigRepository;
    private final LlmClient llmClient;

    public String chat(Long modelConfigId, String systemPrompt, String userPrompt) {
        ModelConfig cfg = modelConfigRepository.findById(modelConfigId)
                .orElseThrow(() -> new BizException("角色绑定的模型配置不存在（id=" + modelConfigId + "），请到【模型配置】检查"));
        if (!Boolean.TRUE.equals(cfg.getEnabled())) {
            throw new BizException("模型「" + cfg.getName() + "」已停用，请到【模型配置】启用或换绑");
        }
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new BizException("模型「" + cfg.getName() + "」尚未填写 apiKey，请到【模型配置】填写后再发起协作");
        }
        return llmClient.chat(cfg, systemPrompt, userPrompt);
    }
}
