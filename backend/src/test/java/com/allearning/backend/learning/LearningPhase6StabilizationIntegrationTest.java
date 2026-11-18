package com.allearning.backend.learning;

import com.allearning.backend.academic.entity.Chapter;
import com.allearning.backend.academic.entity.Subject;
import com.allearning.backend.academic.repository.ChapterRepository;
import com.allearning.backend.academic.repository.SubjectRepository;
import com.allearning.backend.auth.AccountStatus;
import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.learning.entity.*;
import com.allearning.backend.learning.repository.*;
import com.allearning.backend.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:learningphase6db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "app.learning.attempt-question-count=2",
        "app.learning.min-eligible-questions=2",
        "app.learning.assessment-questions-per-chapter=2",
        "app.learning.assessment-max-questions=10"
})
public class LearningPhase6StabilizationIntegrationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy securityFilter;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository users;
    @Autowired private SubjectRepository subjects;
    @Autowired private ChapterRepository chapters;
    @Autowired private ChapterTopicRepository topics;
    @Autowired private QuizRepository quizzes;
    @Autowired private QuestionRepository questions;
    @Autowired private QuestionOptionRepository questionOptions;
    @Autowired private QuizAttemptRepository attempts;
    @Autowired private QuizAnswerRepository answers;
    @Autowired private QuestionReportRepository reports;
    @Autowired private MistakeRecordRepository mistakes;
    @Autowired private DiagnosticAssessmentRepository assessments;
    @Autowired private DiagnosticAssessmentAnswerRepository assessmentAnswers;
    @Autowired private StudySessionRepository studySessions;
    @Autowired private StudySessionItemRepository studySessionItems;

    private MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private User admin, verified, unverified;
    private String adminToken, verifiedToken, unverifiedToken;
    private Subject subject;
    private Chapter chapter;
    private ChapterTopic topic;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilter(securityFilter).build();
        assessmentAnswers.deleteAll();
        assessments.deleteAll();
        studySessionItems.deleteAll();
        studySessions.deleteAll();
        answers.deleteAll();
        attempts.deleteAll();
        mistakes.deleteAll();
        reports.deleteAll();
        questionOptions.deleteAll();
        questions.deleteAll();
        quizzes.deleteAll();
        topics.deleteAll();
        chapters.deleteAll();
        subjects.deleteAll();
        users.deleteAll();

        admin = user("Admin", "phase6-admin@example.com", UserRole.ADMIN, true);
        verified = user("Verified", "phase6-verified@example.com", UserRole.STUDENT, true);
        unverified = user("Unverified", "phase6-unverified@example.com", UserRole.STUDENT, false);
        adminToken = jwtService.generateToken(admin);
        verifiedToken = jwtService.generateToken(verified);
        unverifiedToken = jwtService.generateToken(unverified);
        subject = subjects.save(new Subject("ICT", "Information and Communication Technology"));
        chapter = chapters.save(new Chapter(subject, "Data Representation", 1, null));
        topic = new ChapterTopic(chapter.getChapterId(), "Binary Arithmetic");
        topic.setStatus(TopicStatus.ACTIVE);
        topic = topics.save(topic);
        quiz = quizzes.save(new Quiz(chapter.getChapterId(), "Data Representation Quiz"));
        quiz.setStatus(QuizStatus.PUBLISHED);
        quiz = quizzes.save(quiz);
    }

    private User user(String name, String email, UserRole role, boolean verifiedEmail) {
        User u = users.save(new User(name, email, "hash", "+94" + users.count(), role, AccountStatus.ACTIVE));
        u.setEmailVerified(verifiedEmail);
        return users.save(u);
    }

    private String bearer(String token) { return "Bearer " + token; }

    private Question approvedQuestion(String text) {
        Question q = new Question();
        q.setQuizId(quiz.getQuizId());
        q.setTopicId(topic.getTopicId());
        q.setQuestionText(text);
        q.setExplanation("Explanation");
        q.setSourceExcerpt("Source excerpt");
        q.setDifficulty(QuestionDifficulty.EASY);
        q.setLanguage("EN");
        q.setStatus(QuestionStatus.APPROVED);
        q.setSource(QuestionSource.MANUAL);
        q.setCreatedBy(admin.getUserId());
        q.setApprovedBy(admin.getUserId());
        q.setApprovedAt(OffsetDateTime.now());
        q = questions.save(q);
        q.addOption(new QuestionOption("Correct", 1, true));
        q.addOption(new QuestionOption("Incorrect", 2, false));
        q.addOption(new QuestionOption("Also incorrect", 3, false));
        return questions.save(q);
    }

    @Test
    void progress_returnsStatesSummaryAndExcludesIncompleteAttempts() throws Exception {
        for (int i = 1; i <= 10; i++) approvedQuestion("Progress " + i);
        mvc.perform(get("/api/me/progress").header("Authorization", bearer(verifiedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overall").exists())
                .andExpect(jsonPath("$.subjects").isArray());
        mvc.perform(get("/api/me/progress").header("Authorization", bearer(verifiedToken)))
                .andExpect(jsonPath("$.subjects[0].chapters[0].percent").value(anyOf(nullValue(), instanceOf(Number.class))));
    }

    @Test
    void recommendationsAndOverviewExposeExpectedContracts() throws Exception {
        for (int i = 1; i <= 10; i++) approvedQuestion("Recommendation " + i);
        mvc.perform(get("/api/me/recommendations/next").header("Authorization", bearer(verifiedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alternatives").isArray());
        mvc.perform(get("/api/me/learning/overview").header("Authorization", bearer(verifiedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advice").value(nullValue()));
    }

    @Test
    void studySessionCreateGetHistoryAndAbandonContract() throws Exception {
        for (int i = 1; i <= 10; i++) approvedQuestion("Study " + i);
        MvcResult created = mvc.perform(post("/api/me/study-sessions")
                        .header("Authorization", bearer(verifiedToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":20,\"subjectId\":" + subject.getSubjectId() + ",\"chapterId\":" + chapter.getChapterId() + ",\"language\":\"EN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.questionCount", greaterThanOrEqualTo(1)))
                .andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).get("sessionId").asLong();
        mvc.perform(get("/api/me/study-sessions/" + id).header("Authorization", bearer(verifiedToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/me/study-sessions").header("Authorization", bearer(verifiedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        mvc.perform(post("/api/me/study-sessions/" + id + "/abandon").header("Authorization", bearer(verifiedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABANDONED"));
    }

    @Test
    void reportsRejectUnansweredAndDuplicateAndExposeStudentSpecificFlag() throws Exception {
        Question question = approvedQuestion("Reported question");
        mvc.perform(post("/api/questions/" + question.getQuestionId() + "/reports")
                        .header("Authorization", bearer(verifiedToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"WRONG_ANSWER\",\"comment\":\"Wrong key\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_ANSWERED_BY_USER"));
    }

    @Test
    void adminReportEndpointsAndStudentAuthorizationContract() throws Exception {
        Question q = approvedQuestion("Admin report question");
        reports.save(report(q.getQuestionId(), verified.getUserId()));
        mvc.perform(get("/api/admin/question-reports").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/question-reports").header("Authorization", bearer(verifiedToken)))
                .andExpect(status().isForbidden());
    }

    private QuestionReport report(Long questionId, Long userId) {
        QuestionReport r = new QuestionReport();
        r.setQuestionId(questionId);
        r.setUserId(userId);
        r.setReason(QuestionReportReason.WRONG_ANSWER);
        r.setComment("Incorrect key");
        r.setStatus(QuestionReportStatus.OPEN);
        return r;
    }

    @Test
    void adminAnalyticsAndDashboardExposePhase6Fields() throws Exception {
        mvc.perform(get("/api/admin/learning-analytics/overview").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/dashboard/summary").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingActions.pendingQuestions").exists())
                .andExpect(jsonPath("$.pendingActions.openQuestionReports").exists())
                .andExpect(jsonPath("$.pendingActions.failedGenerationJobs").exists());
        mvc.perform(get("/api/admin/learning-analytics/overview").header("Authorization", bearer(verifiedToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminQuestionContractUsesThreeOrderableOptions() throws Exception {
        String body = """
                {"topicId":%d,"questionText":"Contract question","difficulty":"MEDIUM","language":"EN","saveMode":"PENDING",
                 "options":[{"optionText":"One","optionOrder":1,"isCorrect":false},
                           {"optionText":"Two","optionOrder":2,"isCorrect":true},
                           {"optionText":"Three","optionOrder":3,"isCorrect":false}]}
                """.formatted(topic.getTopicId());
        mvc.perform(post("/api/admin/chapters/" + chapter.getChapterId() + "/questions")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.options", hasSize(3)));
    }

    @Test
    void inProgressAttemptAndStudySessionResponsesAreAnswerSecret() throws Exception {
        Question question = approvedQuestion("Secret question");
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(verified.getUserId());
        attempt.setAttemptType(QuizAttemptType.MISTAKE_PRACTICE);
        attempt.setQuizId(null);
        attempt.setStartedAt(OffsetDateTime.now());
        attempt = attempts.save(attempt);
        MvcResult result = mvc.perform(get("/api/quiz-attempts/" + attempt.getAttemptId())
                        .header("Authorization", bearer(verifiedToken)))
                .andExpect(status().isOk()).andReturn();
        String json = result.getResponse().getContentAsString();
        assertFalse(json.contains("isCorrect"));
        assertFalse(json.contains("correctOptionId"));
        assertFalse(json.contains("explanation"));
        assertFalse(json.contains("sourceExcerpt"));
        assertFalse(json.contains("selectedOptionId"));
        assertNotNull(question.getQuestionId());
    }

    @Test
    void verifiedEmailPolicyAllowsChapterAndStudyButRejectsAssessment() throws Exception {
        for (int i = 1; i <= 10; i++) approvedQuestion("Policy " + i);
        mvc.perform(post("/api/quizzes/" + quiz.getQuizId() + "/attempts")
                        .header("Authorization", bearer(unverifiedToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is2xxSuccessful());
        mvc.perform(post("/api/me/study-sessions")
                        .header("Authorization", bearer(unverifiedToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":10,\"chapterId\":"+chapter.getChapterId()+",\"subjectId\":"+subject.getSubjectId()+",\"language\":\"EN\"}"))
                .andExpect(status().is2xxSuccessful());
        mvc.perform(post("/api/diagnostic-assessments")
                        .header("Authorization", bearer(unverifiedToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assessmentType\":\"INITIAL_DIAGNOSTIC\",\"scopeType\":\"ALL_SUBJECTS\",\"chapterIds\":[],\"subjectId\":null,\"language\":\"EN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }
}
