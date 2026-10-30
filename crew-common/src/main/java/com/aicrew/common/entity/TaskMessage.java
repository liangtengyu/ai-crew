package com.aicrew.common.entity;

import com.aicrew.common.enums.MsgType;
import com.aicrew.common.enums.SenderType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 协作消息：团队聊天时间线中的一条消息 */
@Data
@Entity
@Table(name = "task_message")
public class TaskMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /** 任务内递增序号，用于排序 */
    private Long seq;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SenderType senderType;

    /** 发送者角色 ID（AI 角色时有效） */
    @Column(name = "sender_role_id")
    private Long senderRoleId;

    /** 发送者展示名，如「产品经理·艾玛」/「BOSS」/「系统」 */
    @Column(name = "sender_name", nullable = false, length = 100)
    private String senderName;

    /** 发送者头像 emoji */
    @Column(length = 16)
    private String emoji;

    /** 接收方展示名，如「开发工程师·李雷」/「BOSS」 */
    @Column(name = "target_name", length = 100)
    private String targetName;

    /** 消息正文 */
    @Column(nullable = false, length = 20000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MsgType msgType = MsgType.CHAT;

    /** 所属剧本步骤序号 */
    @Column(name = "step_order")
    private Integer stepOrder;

    /** 验收关卡结论：APPROVE / REJECT */
    private String verdict;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
