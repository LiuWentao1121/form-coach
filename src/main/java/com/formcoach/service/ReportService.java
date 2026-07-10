package com.formcoach.service;

import com.formcoach.dto.TrainingReport;
import com.formcoach.entity.TrainingSession;
import com.formcoach.common.PageResult;

public interface ReportService {

    TrainingReport getReport(Long sessionId);

    PageResult<TrainingSession> listSessions(Long userId, int page, int size);

    Object getUserStats(Long userId);

    Object compareSessions(Long sessionId1, Long sessionId2);

    /**
     * Weekly training report with trend analysis.
     */
    Object getWeeklyReport(Long userId);
}
