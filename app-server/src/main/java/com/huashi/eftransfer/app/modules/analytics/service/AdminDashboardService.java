package com.huashi.eftransfer.app.modules.analytics.service;

import com.huashi.eftransfer.app.modules.analytics.vo.AdminDashboardAiTrendPointVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AdminDashboardCompletionTrendPointVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AdminDashboardOverviewVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AdminDashboardRegistrationTrendPointVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AdminDashboardSceneDistributionVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AdminDashboardVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    private static final int SUMMARY_WINDOW_DAYS = 30;
    private static final int TREND_WINDOW_DAYS = 14;

    private final AdminDashboardRepository repository;

    public AdminDashboardService(AdminDashboardRepository repository) {
        this.repository = repository;
    }

    public AdminDashboardVO getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime summarySince = now.minusDays(SUMMARY_WINDOW_DAYS);
        LocalDate trendStart = today.minusDays(TREND_WINDOW_DAYS - 1L);

        long totalUsers = repository.countUsers();
        long enabledUsers = repository.countEnabledUsers();
        long registrationsLast30Days = repository.countRegistrationsSince(summarySince);
        long dailyActiveUsers = repository.countActiveUsersSince(now.minusDays(1));
        long weeklyActiveUsers = repository.countActiveUsersSince(now.minusDays(7));
        long diagnosisCompletedLast30Days = repository.countCompletedDiagnosisSince(summarySince);
        long trainingCompletedLast30Days = repository.countCompletedTrainingSince(summarySince);
        long assessmentCompletedLast30Days = repository.countSubmittedAssessmentsSince(summarySince);
        long aiCallsLast30Days = repository.countAiCallsSince(summarySince);
        long aiFallbackCountLast30Days = repository.countAiFallbacksSince(summarySince);

        AdminDashboardOverviewVO overview = new AdminDashboardOverviewVO(
                totalUsers,
                enabledUsers,
                registrationsLast30Days,
                dailyActiveUsers,
                weeklyActiveUsers,
                diagnosisCompletedLast30Days,
                trainingCompletedLast30Days,
                assessmentCompletedLast30Days,
                aiCallsLast30Days,
                aiFallbackCountLast30Days,
                ratio(aiFallbackCountLast30Days, aiCallsLast30Days),
                now
        );

        Map<LocalDate, Long> registrations = repository.registrationsByDay(trendStart, today);
        Map<LocalDate, Long> diagnosis = repository.completedDiagnosisByDay(trendStart, today);
        Map<LocalDate, Long> training = repository.completedTrainingByDay(trendStart, today);
        Map<LocalDate, Long> assessments = repository.submittedAssessmentsByDay(trendStart, today);
        Map<LocalDate, Long> aiCalls = repository.aiCallsByDay(trendStart, today);
        Map<LocalDate, Long> aiFallbacks = repository.aiFallbacksByDay(trendStart, today);
        Map<String, Long> aiScenes = repository.aiSceneDistributionSince(summarySince);

        List<AdminDashboardRegistrationTrendPointVO> registrationTrend = new ArrayList<>();
        List<AdminDashboardCompletionTrendPointVO> completionTrend = new ArrayList<>();
        List<AdminDashboardAiTrendPointVO> aiTrend = new ArrayList<>();

        for (LocalDate cursor = trendStart; !cursor.isAfter(today); cursor = cursor.plusDays(1)) {
            long registrationCount = registrations.getOrDefault(cursor, 0L);
            long diagnosisCount = diagnosis.getOrDefault(cursor, 0L);
            long trainingCount = training.getOrDefault(cursor, 0L);
            long assessmentCount = assessments.getOrDefault(cursor, 0L);
            long aiCallCount = aiCalls.getOrDefault(cursor, 0L);
            long aiFallbackCount = aiFallbacks.getOrDefault(cursor, 0L);
            String date = cursor.toString();

            registrationTrend.add(new AdminDashboardRegistrationTrendPointVO(date, registrationCount));
            completionTrend.add(new AdminDashboardCompletionTrendPointVO(date, diagnosisCount, trainingCount, assessmentCount));
            aiTrend.add(new AdminDashboardAiTrendPointVO(date, aiCallCount, aiFallbackCount, ratio(aiFallbackCount, aiCallCount)));
        }

        List<AdminDashboardSceneDistributionVO> aiSceneDistribution = toSceneDistribution(aiScenes, aiCallsLast30Days);

        return new AdminDashboardVO(overview, registrationTrend, completionTrend, aiTrend, aiSceneDistribution);
    }

    private List<AdminDashboardSceneDistributionVO> toSceneDistribution(Map<String, Long> sceneCounts, long total) {
        List<AdminDashboardSceneDistributionVO> items = new ArrayList<>();
        for (Map.Entry<String, Long> entry : sceneCounts.entrySet()) {
            items.add(new AdminDashboardSceneDistributionVO(entry.getKey(), entry.getValue(), ratio(entry.getValue(), total)));
        }
        return items;
    }

    private double ratio(long numerator, long denominator) {
        if (denominator <= 0L) {
            return 0D;
        }
        return (double) numerator / (double) denominator;
    }
}
