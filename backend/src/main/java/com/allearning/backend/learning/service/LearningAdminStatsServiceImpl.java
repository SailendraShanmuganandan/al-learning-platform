package com.allearning.backend.learning.service;

import com.allearning.backend.learning.entity.JobStatus;
import com.allearning.backend.learning.entity.QuestionReportStatus;
import com.allearning.backend.learning.entity.QuestionStatus;
import com.allearning.backend.learning.repository.QuestionGenerationJobRepository;
import com.allearning.backend.learning.repository.QuestionReportRepository;
import com.allearning.backend.learning.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional(readOnly = true)
public class LearningAdminStatsServiceImpl implements LearningAdminStatsService {

    private final QuestionRepository questionRepository;
    private final QuestionReportRepository questionReportRepository;
    private final QuestionGenerationJobRepository questionGenerationJobRepository;

    public LearningAdminStatsServiceImpl(
            QuestionRepository questionRepository,
            QuestionReportRepository questionReportRepository,
            QuestionGenerationJobRepository questionGenerationJobRepository
    ) {
        this.questionRepository = questionRepository;
        this.questionReportRepository = questionReportRepository;
        this.questionGenerationJobRepository = questionGenerationJobRepository;
    }

    @Override
    public long countPendingQuestions() {
        return questionRepository.countByStatus(QuestionStatus.PENDING);
    }

    @Override
    public long countOpenQuestionReports() {
        return questionReportRepository.countByStatus(QuestionReportStatus.OPEN);
    }

    @Override
    public long countFailedGenerationJobsSince(OffsetDateTime since) {
        return questionGenerationJobRepository.countByStatusAndCompletedAtGreaterThanEqual(JobStatus.FAILED, since);
    }
}

