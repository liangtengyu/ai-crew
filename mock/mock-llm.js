/**
 * AI Crew 本地 Mock 演示模型
 * 一个极简的 OpenAI 兼容 /chat/completions 服务，无需任何 apiKey 即可演示完整协作流程。
 * 启动：node mock/mock-llm.js   （端口 18080）
 * 使用：到【模型配置】里把「本地 Mock 演示模型」绑定到角色，发布需求即可。
 */
const http = require('http')

// 验收关卡第一次打回，之后通过 —— 用于演示「返工轮」
let reviewCount = 0

function reply(content, verdict) {
  const obj = { content }
  if (verdict) obj.verdict = verdict
  return JSON.stringify(obj)
}

function answer(userPrompt) {
  // 用「【当前环节：X】」标记识别当前环节（该标记只出现在环节指令里，协作历史中没有），
  // 注意判断顺序：先长后短，避免「需求分析」被更早的分支误命中。
  if (userPrompt.includes('【当前环节：交付总结】')) {
    return reply(
      '## 📦 交付说明\n' +
        '**需求回顾**：上线员工请假审批小程序（提交-审批-通知闭环）。\n' +
        '**方案要点**：leave_order 单表 + 状态机，Spring Boot + MySQL + Vue3。\n' +
        '**实现情况**：T1-T4 全部完成并通过两轮验收；关键产出含建表 SQL、提交/审批接口、前端两页与站内信通知。\n' +
        '**验收结论**：产品验收通过（第二轮 APPROVE）。\n' +
        '**后续建议**：二期接入考勤机自动同步余额、增加审批报表；跨天扣减口径需 HR 最终确认后固化。'
    )
  }
  if (userPrompt.includes('【当前环节：验收评审】')) {
    reviewCount++
    if (reviewCount === 1) {
      return reply(
        '## 验收意见（第一轮）\n' +
          '对照任务清单逐项检查：T1/T3/T4 达标，但发现 2 个问题：\n' +
          '1. 提交接口未做**时段重叠校验**，可重复提交同一时段；\n' +
          '2. 主管驳回时**未记录驳回原因**，申请人不知道为什么被驳回。\n' +
          '以上两点必须修复后再提交验收。',
        'REJECT'
      )
    }
    return reply(
      '## 验收意见（第二轮）\n' +
        '1. 时段重叠校验：已验证生效，重叠提交被正确拦截；\n' +
        '2. 驳回原因：已落库并同步申请人。\n' +
        '任务清单 T1-T4 全部达标，**验收通过**。',
      'APPROVE'
    )
  }
  if (userPrompt.includes('【当前环节：开发实现】')) {
    // 返工场景：协作历史里会出现产品经理打回消息「验收意见（第一轮）」
    const rework = userPrompt.includes('验收意见（')
    const head = rework
      ? '### 返工说明\n已逐条回应验收意见：1) 重叠请假校验已前置到提交接口；2) 驳回需填写原因，已补。\n\n'
      : ''
    return reply(
      head +
        '## 开发实现\n' +
        '**思路**：单表 leave_order + 状态机（DRAFT→PENDING→APPROVED/REJECTED），提交时校验时段重叠与余额。\n\n' +
        '```sql\n' +
        'CREATE TABLE leave_order (\n' +
        '  id BIGINT PRIMARY KEY AUTO_INCREMENT,\n' +
        '  user_id BIGINT NOT NULL,\n' +
        '  leave_type VARCHAR(16) NOT NULL,\n' +
        '  start_date DATE NOT NULL,\n' +
        '  end_date DATE NOT NULL,\n' +
        '  reason VARCHAR(500),\n' +
        '  reject_reason VARCHAR(500),\n' +
        '  status VARCHAR(16) NOT NULL DEFAULT "PENDING",\n' +
        '  KEY idx_user (user_id, start_date, end_date)\n' +
        ');\n' +
        '```\n\n' +
        '```java\n' +
        '// 提交：先做时段重叠校验，再落库\n' +
        'long cnt = leaveOrderMapper.countOverlap(userId, req.getStart(), req.getEnd());\n' +
        'if (cnt > 0) throw new BizException("该时段已有请假申请，请勿重复提交");\n' +
        'leaveOrderMapper.insert(order);\n' +
        'notifyService.pushToLeader(order);   // T4 通知主管\n' +
        '```\n\n' +
        '**自测**：Postman 覆盖 正常提交/重叠提交/余额不足/同意/驳回 5 个用例，全部通过，逐条对应任务清单 T1-T4 完成。'
    )
  }
  if (userPrompt.includes('【当前环节：方案设计】')) {
    return reply(
      '## 设计方案\n' +
        '1. **总体流程**：员工提交请假单 → 校验余额 → 推送直属主管 → 主管审批（同意/驳回）→ 结果通知申请人。\n' +
        '2. **模块拆解**：申请模块、审批模块、通知模块、假期余额模块。\n' +
        '3. **技术选型**：后端 Spring Boot + MySQL（团队现成技术栈）；前端 Vue3 + Element Plus；通知用站内信 + 企微机器人。\n' +
        '4. **开发任务清单**：\n' +
        '   - T1 请假单表结构 + 提交接口（含余额校验）\n' +
        '   - T2 审批接口（同意/驳回，含状态机）\n' +
        '   - T3 前端申请页与审批列表页\n' +
        '   - T4 通知推送（先站内信）\n' +
        '5. **风险**：跨天请假按天扣减的口径需与 HR 确认；并发提交同一时段需做重叠校验。'
    )
  }
  if (userPrompt.includes('【当前环节：需求分析】')) {
    return reply(
      '## 需求分析\n' +
        '**需求复述**：BOSS 希望上线一个员工请假审批小程序，支持提交申请、主管审批、状态通知。\n' +
        '**目标用户**：全体员工（申请人）+ 各级主管（审批人）。\n' +
        '**核心价值**：把线下口头/纸面请假搬到线上，流程可追溯、数据可统计。\n' +
        '**待澄清点及默认假设**：审批层级默认 1 级（直属主管）；假期类型先支持 事假/年假/病假；暂不对接考勤机。\n' +
        '**范围边界**：本期只做审批流核心闭环，报表统计放到二期。'
    )
  }
  return reply('收到，已完成本环节工作。')
}

http
  .createServer((req, res) => {
    if (req.method === 'GET') {
      res.writeHead(200, { 'Content-Type': 'application/json' })
      res.end(JSON.stringify({ status: 'mock-llm ok' }))
      return
    }
    let body = ''
    req.on('data', (c) => (body += c))
    req.on('end', () => {
      let prompt = ''
      try {
        const json = JSON.parse(body)
        prompt = json.messages?.map((m) => m.content).join('\n') || ''
      } catch (e) {
        /* ignore */
      }
      setTimeout(() => {
        res.writeHead(200, { 'Content-Type': 'application/json' })
        res.end(
          JSON.stringify({
            id: 'mock-' + Date.now(),
            object: 'chat.completion',
            choices: [{ index: 0, message: { role: 'assistant', content: answer(prompt) }, finish_reason: 'stop' }]
          })
        )
      }, 600)
    })
  })
  .listen(18080, () => console.log('Mock LLM listening on http://127.0.0.1:18080 (POST /v1/chat/completions)'))
