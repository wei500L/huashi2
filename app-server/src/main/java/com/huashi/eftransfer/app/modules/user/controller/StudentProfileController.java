package com.huashi.eftransfer.app.modules.user.controller;

import com.huashi.eftransfer.app.modules.analytics.service.AnalyticsQueryService;
import com.huashi.eftransfer.app.modules.user.dto.UpdateStudentLearningGoalRequest;
import com.huashi.eftransfer.app.modules.user.dto.UpdateStudentProfileRequest;
import com.huashi.eftransfer.app.modules.user.service.StudentProfileService;
import com.huashi.eftransfer.app.modules.user.vo.StudentLearningGoalVO;
import com.huashi.eftransfer.app.modules.user.vo.StudentProfileVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/student/profile")
public class StudentProfileController {

    private final AnalyticsQueryService analyticsQueryService;
    private final StudentProfileService studentProfileService;

    public StudentProfileController(
            AnalyticsQueryService analyticsQueryService,
            StudentProfileService studentProfileService
    ) {
        this.analyticsQueryService = analyticsQueryService;
        this.studentProfileService = studentProfileService;
    }

    @GetMapping("/goals")
    public ApiResponse<StudentLearningGoalVO> getLearningGoals() {
        return ApiResponse.success(analyticsQueryService.getCurrentStudentLearningGoal(), MDC.get("traceId"));
    }

    @PutMapping
    public ApiResponse<StudentProfileVO> updateCurrentStudentProfile(@Valid @RequestBody UpdateStudentProfileRequest request) {
        return ApiResponse.success(studentProfileService.updateCurrentStudentProfile(request), MDC.get("traceId"));
    }

    @PutMapping("/goals")
    public ApiResponse<StudentLearningGoalVO> updateLearningGoals(@Valid @RequestBody UpdateStudentLearningGoalRequest request) {
        studentProfileService.updateCurrentStudentLearningGoals(request);
        return ApiResponse.success(analyticsQueryService.getCurrentStudentLearningGoal(), MDC.get("traceId"));
    }
}
