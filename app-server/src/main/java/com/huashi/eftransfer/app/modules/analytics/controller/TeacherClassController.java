package com.huashi.eftransfer.app.modules.analytics.controller;

import com.huashi.eftransfer.app.modules.analytics.dto.TeacherClassStudentBatchRequest;
import com.huashi.eftransfer.app.modules.analytics.dto.TeacherClassUpsertRequest;
import com.huashi.eftransfer.app.modules.analytics.service.TeacherClassManagementService;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherClassDetailVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherClassInviteCodeVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherClassStudentCandidateVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeachingClassSummaryVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/teacher/classes")
public class TeacherClassController {

    private final TeacherClassManagementService teacherClassManagementService;

    public TeacherClassController(TeacherClassManagementService teacherClassManagementService) {
        this.teacherClassManagementService = teacherClassManagementService;
    }

    @GetMapping
    public ApiResponse<List<TeachingClassSummaryVO>> listClasses() {
        return ApiResponse.success(teacherClassManagementService.listClasses(), MDC.get("traceId"));
    }

    @PostMapping
    public ApiResponse<TeacherClassDetailVO> createClass(@Valid @RequestBody TeacherClassUpsertRequest request) {
        return ApiResponse.success(teacherClassManagementService.createClass(request), MDC.get("traceId"));
    }

    @PostMapping("/invite-code")
    public ApiResponse<TeacherClassInviteCodeVO> generateInviteCode() {
        return ApiResponse.success(teacherClassManagementService.generateInviteCode(), MDC.get("traceId"));
    }

    @GetMapping("/{classId}")
    public ApiResponse<TeacherClassDetailVO> getClassDetail(@PathVariable Long classId) {
        return ApiResponse.success(teacherClassManagementService.getClassDetail(classId), MDC.get("traceId"));
    }

    @PutMapping("/{classId}")
    public ApiResponse<TeacherClassDetailVO> updateClass(
            @PathVariable Long classId,
            @Valid @RequestBody TeacherClassUpsertRequest request
    ) {
        return ApiResponse.success(teacherClassManagementService.updateClass(classId, request), MDC.get("traceId"));
    }

    @DeleteMapping("/{classId}")
    public ApiResponse<Void> deleteClass(@PathVariable Long classId) {
        teacherClassManagementService.archiveClass(classId);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @GetMapping("/{classId}/student-candidates")
    public ApiResponse<List<TeacherClassStudentCandidateVO>> listStudentCandidates(
            @PathVariable Long classId,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(teacherClassManagementService.listStudentCandidates(classId, keyword), MDC.get("traceId"));
    }

    @PostMapping("/{classId}/students")
    public ApiResponse<TeacherClassDetailVO> addStudents(
            @PathVariable Long classId,
            @Valid @RequestBody TeacherClassStudentBatchRequest request
    ) {
        return ApiResponse.success(teacherClassManagementService.addStudents(classId, request), MDC.get("traceId"));
    }

    @PostMapping("/{classId}/students/remove")
    public ApiResponse<TeacherClassDetailVO> removeStudents(
            @PathVariable Long classId,
            @Valid @RequestBody TeacherClassStudentBatchRequest request
    ) {
        return ApiResponse.success(teacherClassManagementService.removeStudents(classId, request), MDC.get("traceId"));
    }
}
