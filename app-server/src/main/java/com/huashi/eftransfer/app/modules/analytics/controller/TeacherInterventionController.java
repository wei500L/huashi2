package com.huashi.eftransfer.app.modules.analytics.controller;

import com.huashi.eftransfer.app.modules.analytics.dto.TeacherInterventionPageQuery;
import com.huashi.eftransfer.app.modules.analytics.service.TeacherInterventionService;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherInterventionSummaryVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.page.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/teacher/interventions")
public class TeacherInterventionController {

    private final TeacherInterventionService teacherInterventionService;

    public TeacherInterventionController(TeacherInterventionService teacherInterventionService) {
        this.teacherInterventionService = teacherInterventionService;
    }

    @GetMapping
    public ApiResponse<PageResult<TeacherInterventionSummaryVO>> pageQuery(
            @Valid @ModelAttribute TeacherInterventionPageQuery query
    ) {
        return ApiResponse.success(teacherInterventionService.pageQuery(query), MDC.get("traceId"));
    }

    @PostMapping("/{interventionId}/complete")
    public ApiResponse<Void> markCompleted(@PathVariable Long interventionId) {
        teacherInterventionService.markCompleted(interventionId);
        return ApiResponse.success("Intervention marked as completed", MDC.get("traceId"));
    }
}
