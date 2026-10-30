package com.aicrew.server.controller;

import com.aicrew.common.dto.Result;
import com.aicrew.common.enums.TaskStatus;
import com.aicrew.common.repository.AgentRoleRepository;
import com.aicrew.common.repository.ModelConfigRepository;
import com.aicrew.common.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 概览统计 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final TaskRepository taskRepository;
    private final AgentRoleRepository roleRepository;
    private final ModelConfigRepository modelRepository;

    @GetMapping
    public Result<Map<String, Object>> stats() {
        Map<String, Object> map = new LinkedHashMap<>();
        var tasks = taskRepository.findAll();
        map.put("taskTotal", tasks.size());
        map.put("taskRunning", tasks.stream().filter(t -> t.getStatus() == TaskStatus.RUNNING).count());
        map.put("taskDelivered", tasks.stream().filter(t -> t.getStatus() == TaskStatus.DELIVERED).count());
        map.put("taskAccepted", tasks.stream().filter(t -> t.getStatus() == TaskStatus.ACCEPTED).count());
        map.put("roleTotal", roleRepository.count());
        map.put("modelTotal", modelRepository.count());
        return Result.ok(map);
    }
}
