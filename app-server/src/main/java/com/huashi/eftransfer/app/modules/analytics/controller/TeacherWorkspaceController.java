package com.huashi.eftransfer.app.modules.analytics.controller;

import com.huashi.eftransfer.app.modules.analytics.service.TeacherWorkspaceService;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherWorkspaceOverviewVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/teacher/workspace")
public class TeacherWorkspaceController {

    private final TeacherWorkspaceService teacherWorkspaceService;

    public TeacherWorkspaceController(TeacherWorkspaceService teacherWorkspaceService) {
        this.teacherWorkspaceService = teacherWorkspaceService;
    }

    @GetMapping("/overview")
    public ApiResponse<TeacherWorkspaceOverviewVO> getOverview() {
        return ApiResponse.success(teacherWorkspaceService.getOverview(), MDC.get("traceId"));
    }
}
