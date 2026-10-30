package com.aicrew.config;

import com.aicrew.common.entity.AgentRole;
import com.aicrew.common.entity.ModelConfig;
import com.aicrew.common.entity.Playbook;
import com.aicrew.common.entity.PlaybookStep;
import com.aicrew.common.enums.RoleType;
import com.aicrew.common.repository.AgentRoleRepository;
import com.aicrew.common.repository.ModelConfigRepository;
import com.aicrew.common.repository.PlaybookRepository;
import com.aicrew.common.repository.PlaybookStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 首次启动种子数据：示例模型、示例角色、默认协作剧本 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final ModelConfigRepository modelRepository;
    private final AgentRoleRepository roleRepository;
    private final PlaybookRepository playbookRepository;
    private final PlaybookStepRepository stepRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (modelRepository.count() == 0) {
            seedModels();
            log.info("已初始化示例模型配置");
        }
        if (roleRepository.count() == 0) {
            seedRoles();
            log.info("已初始化示例角色");
        }
        if (playbookRepository.count() == 0) {
            seedPlaybook();
            log.info("已初始化默认协作剧本");
        }
    }

    private void seedModels() {
        model("DeepSeek 官方（示例）", "https://api.deepseek.com", "deepseek-chat");
        model("智谱 GLM（示例）", "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash");
        model("硅基流动（示例）", "https://api.siliconflow.cn/v1", "Qwen/Qwen2.5-7B-Instruct");
        ModelConfig mock = model("本地 Mock 演示模型", "http://127.0.0.1:18080/v1", "mock-demo");
        mock.setApiKey("mock-not-needed");
        modelRepository.save(mock);
    }

    private ModelConfig model(String name, String baseUrl, String modelId) {
        ModelConfig m = new ModelConfig();
        m.setName(name);
        m.setBaseUrl(baseUrl);
        m.setApiKey("");
        m.setModel(modelId);
        m.setTemperature(0.7);
        m.setMaxTokens(4096);
        m.setEnabled(true);
        return modelRepository.save(m);
    }

    private void seedRoles() {
        ModelConfig deepseek = modelRepository.findAll().stream()
                .filter(m -> m.getName().startsWith("DeepSeek")).findFirst().orElseThrow();
        ModelConfig glm = modelRepository.findAll().stream()
                .filter(m -> m.getName().startsWith("智谱")).findFirst().orElseThrow();

        role("产品经理·艾玛", RoleType.PM, "🧭", "#409EFF", deepseek.getId());
        role("开发工程师·李雷", RoleType.DEV, "👨‍💻", "#67C23A", glm.getId());
        role("测试工程师·小美", RoleType.TEST, "🧪", "#F56C6C", glm.getId());
        role("架构师·老王", RoleType.ARCHITECT, "🏛️", "#E6A23C", deepseek.getId());
    }

    private void role(String name, RoleType type, String emoji, String color, Long modelId) {
        AgentRole r = new AgentRole();
        r.setName(name);
        r.setRoleType(type);
        r.setEmoji(emoji);
        r.setColor(color);
        r.setSystemPrompt("");
        r.setModelConfigId(modelId);
        r.setEnabled(true);
        roleRepository.save(r);
    }

    private void seedPlaybook() {
        AgentRole pm = roleRepository.findAll().stream()
                .filter(r -> r.getName().contains("产品经理")).findFirst().orElseThrow();
        AgentRole dev = roleRepository.findAll().stream()
                .filter(r -> r.getName().contains("开发工程师")).findFirst().orElseThrow();

        Playbook pb = new Playbook();
        pb.setName("标准交付流程");
        pb.setDescription("需求分析 → 方案设计 → 开发实现 → 验收评审（可返工）→ 交付总结");
        pb.setMaxRounds(3);
        pb = playbookRepository.save(pb);

        step(pb.getId(), 1, "REQUIREMENT_ANALYSIS", "需求分析", pm.getId(), """
                【当前环节：需求分析】
                BOSS 提出了新需求，请你完成需求分析并同步团队：
                1. 用你自己的话复述需求要点，确认理解无偏差；
                2. 指出目标用户与核心价值；
                3. 列出需要澄清的问题，并直接给出你的默认假设（不要等待回复，按假设推进）；
                4. 明确本期范围边界（做什么 / 不做什么）。""", false, null);

        step(pb.getId(), 2, "SOLUTION_DESIGN", "方案设计", pm.getId(), """
                【当前环节：方案设计】
                请基于前面的需求分析输出设计方案：
                1. 总体思路与业务流程；
                2. 功能模块拆解（含每个模块要点）；
                3. 技术选型建议（结合团队现状说明理由）；
                4. 给开发工程师的开发任务清单（具体、可执行、可验收）；
                5. 风险与注意事项。""", false, null);

        step(pb.getId(), 3, "DEVELOPMENT", "开发实现", dev.getId(), """
                【当前环节：开发实现】
                请根据产品经理的方案与任务清单进行开发实现：
                1. 说明实现思路（模块 / 接口 / 数据结构）；
                2. 给出关键代码或伪代码（使用代码块）；
                3. 说明自测方式，并逐条对照任务清单说明完成情况；
                4. 列出遗留风险与待确认项。
                若你是被验收打回后返工，请先逐条回应验收意见，再给出修改后的实现。""", false, null);

        step(pb.getId(), 4, "ACCEPTANCE", "验收评审", pm.getId(), """
                【当前环节：验收评审】
                请以产品经理视角对开发提交的结果做严格验收：
                1. 对照方案中的任务清单逐项检查完成度；
                2. 若有问题，明确写出必须修改的点，开发工程师会据此返工；
                3. 全部达标才允许通过。""", true, 3);

        step(pb.getId(), 5, "DELIVERY", "交付总结", pm.getId(), """
                【当前环节：交付总结】
                请代表团队向 BOSS 输出最终交付说明：
                1. 需求回顾（一句话）；
                2. 方案要点；
                3. 实现情况与关键产出；
                4. 验收结论；
                5. 后续建议（迭代方向 / 需要人类介入的事项）。""", false, null);
    }

    private void step(Long playbookId, int order, String code, String name, Long roleId,
                      String template, boolean gate, Integer onRejectOrder) {
        PlaybookStep s = new PlaybookStep();
        s.setPlaybookId(playbookId);
        s.setStepOrder(order);
        s.setCode(code);
        s.setName(name);
        s.setPromptTemplate(template.strip());
        s.setActorRoleId(roleId);
        s.setRequireVerdict(gate);
        s.setOnRejectOrder(onRejectOrder);
        stepRepository.save(s);
    }
}
