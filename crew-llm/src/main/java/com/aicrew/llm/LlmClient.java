package com.aicrew.llm;

import com.aicrew.common.entity.ModelConfig;

/** 大模型客户端抽象：换厂商/换协议只需新增实现 */
public interface LlmClient {

    /**
     * 发起一次对话
     *
     * @param cfg         模型配置（BaseURL / Key / 模型名 / 温度）
     * @param systemPrompt 角色人设系统提示
     * @param userPrompt   用户提示（环节指令 + 上下文）
     * @return 模型回复文本
     */
    String chat(ModelConfig cfg, String systemPrompt, String userPrompt);
}
