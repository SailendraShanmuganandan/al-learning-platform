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
import org.junit.jupiter.api.DisplayName;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:learningphase5db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
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
public class LearningPhase5IntegrationTest {

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
    @Autowired private QuizAttemptRepository quizAttempts;
    @Autowired private QuizAnswerRepository quizAnswers;
    @Autowired private DiagnosticAssessmentRepository assessments;
    @Autowired private DiagnosticAssessmentAnswerRepository assessmentAnswers;
    @Autowired private MistakeRecordRepository mistakeRecords;
    @Autowired private QuestionReportRepository questionReports;
    @Autowired private ChapterDocumentRepository chapterDocuments;

    private MockMvc mvc;
    private ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    private User admin;
    private User verifiedStudent;
    private User unverifiedStudent;
    private User otherStudent;

    private String adminToken;
    private String verifiedStudentToken;
    private String unverifiedStudentToken;
    private String otherStudentToken;

    private Subject subject;
    private Chapter chapter;
    private ChapterTopic topic;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilter(securityFilter).build();

        assessmentAnswers.deleteAll();
        assessments.deleteAll();
        quizAnswers.deleteAll();
        quizAttempts.deleteAll();
        mistakeRecords.deleteAll();
        questionReports.deleteAll();
        chapterDocuments.deleteAll();
        questionOptions.deleteAll();
        questions.deleteAll();
        quizzes.deleteAll();
        topics.deleteAll();
        chapters.deleteAll();
        subjects.deleteAll();
        users.deleteAll();

        admin = users.save(new User("Admin User", "admin@phase5.com", "hash", "+94771111111", UserRole.ADMIN, AccountStatus.ACTIVE));
        admin.setEmailVerified(true);
        admin = users.save(admin);
        adminToken = jwtService.generateToken(admin);

        verifiedStudent = users.save(new User("Verified Student", "student.verified@phase5.com", "hash", "+94772222222", UserRole.STUDENT, AccountStatus.ACTIVE));
        verifiedStudent.setEmailVerified(true);
        verifiedStudent = users.save(verifiedStudent);
        verifiedStudentToken = jwtService.generateToken(verifiedStudent);

        unverifiedStudent = users.save(new User("Unverified Student", "student.unverified@phase5.com", "hash", "+94773333333", UserRole.STUDENT, AccountStatus.ACTIVE));
        unverifiedStudent.setEmailVerified(false);
        unverifiedStudent = users.save(unverifiedStudent);
        unverifiedStudentToken = jwtService.generateToken(unverifiedStudent);

        otherStudent = users.save(new User("Other Student", "other@phase5.com", "hash", "+94774444444", UserRole.STUDENT, AccountStatus.ACTIVE));
        otherStudent.setEmailVerified(true);
        otherStudent = users.save(otherStudent);
        otherStudentToken = jwtService.generateToken(otherStudent);

        subject = subjects.save(new Subject("ICT", "Information & Communication Technology"));
        chapter = chapters.save(new Chapter(subject, "Data Representation", 1, null));

        topic = new ChapterTopic(chapter.getChapterId(), "Binary Arithmetic");
        topic.setStatus(TopicStatus.ACTIVE);
        topic = topics.save(topic);

        quiz = new Quiz(chapter.getChapterId(), "Data Representation Quiz");
        quiz.setStatus(QuizStatus.PUBLISHED);
        quiz = quizzes.save(quiz);
    }

    private String auth(String token) { return "Bearer " + token; }

    private Question createApprovedQuestion(String text, String optA, String optB, int correctOptOrder) {
        Question q = new Question();
        q.setQuizId(quiz.getQuizId());
        q.setQuestionText(text);
        q.setExplanation("Explanation for " + text);
        q.setTopicId(topic.getTopicId());
        q.setDifficulty(QuestionDifficulty.EASY);
        q.setLanguage("EN");
        q.setStatus(QuestionStatus.APPROVED);
        q.setSource(QuestionSource.MANUAL);
        q.setCreatedBy(admin.getUserId());
        q.setApprovedBy(admin.getUserId());
        q.setApprovedAt(OffsetDateTime.now());
        q = questions.save(q);

        QuestionOption o1 = new QuestionOption(optA, 1, correctOptOrder == 1);
        q.addOption(o1);
        QuestionOption o2 = new QuestionOption(optB, 2, correctOptOrder == 2);
        q.addOption(o2);

        return questions.save(q);
    }

    // --- Section A3: Option Immutability (BR-38) ---

    @Test
    @DisplayName("Option Immutability (BR-38): Changing correct option after recorded answers returns 409 OPTIONS_IMMUTABLE")
    void optionImmutabilityCorrectOptionChangedTest() throws Exception {
        Question q = createApprovedQuestion("What is 1+1 in binary?", "10", "11", 1);
        Long qId = q.getQuestionId();

        // Simulate an answer recorded for this question
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(verifiedStudent.getUserId());
        attempt.setAttemptType(QuizAttemptType.CHAPTER_QUIZ);
        attempt.setQuizId(quiz.getQuizId());
        attempt.setTotalMarks(1);
        attempt.setStartedAt(OffsetDateTime.now());
        attempt.setCompletedAt(OffsetDateTime.now());
        attempt.setScore(1);
        attempt = quizAttempts.save(attempt);

        QuizAnswer ans = new QuizAnswer();
        ans.setAttemptId(attempt.getAttemptId());
        ans.setQuestionId(qId);
        ans.setQuestionOrder(1);
        ans.setSelectedOptionId(q.getOptions().get(0).getOptionId());
        ans.setIsCorrect(true);
        quizAnswers.save(ans);

        // Attempt to update question and change correct option (order 2 is now marked correct)
        String updateJson = "{"
                + "\"questionText\":\"Updated text\","
                + "\"difficulty\":\"EASY\","
                + "\"language\":\"EN\","
                + "\"topicId\":" + topic.getTopicId() + ","
                + "\"options\":["
                + "{\"optionOrder\":1,\"optionText\":\"10\",\"isCorrect\":false},"
                + "{\"optionOrder\":2,\"optionText\":\"11\",\"isCorrect\":true}"
                + "]}";

        mvc.perform(put("/api/admin/questions/" + qId)
                        .header("Authorization", auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPTIONS_IMMUTABLE"));
    }

    @Test
    @DisplayName("Option Immutability (BR-38): Adding or deleting options after recorded answers returns 409 OPTIONS_IMMUTABLE")
    void optionImmutabilityOptionCountChangedTest() throws Exception {
        Question q = createApprovedQuestion("What is 2+2?", "4", "5", 1);
        Long qId = q.getQuestionId();

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(verifiedStudent.getUserId());
        attempt.setAttemptType(QuizAttemptType.CHAPTER_QUIZ);
        attempt.setQuizId(quiz.getQuizId());
        attempt.setTotalMarks(1);
        attempt.setStartedAt(OffsetDateTime.now());
        attempt.setCompletedAt(OffsetDateTime.now());
        attempt = quizAttempts.save(attempt);

        QuizAnswer ans = new QuizAnswer();
        ans.setAttemptId(attempt.getAttemptId());
        ans.setQuestionId(qId);
        ans.setQuestionOrder(1);
        quizAnswers.save(ans);

        // Adding an extra option
        String updateJson = "{"
                + "\"questionText\":\"What is 2+2?\","
                + "\"difficulty\":\"EASY\","
                + "\"language\":\"EN\","
                + "\"topicId\":" + topic.getTopicId() + ","
                + "\"options\":["
                + "{\"optionOrder\":1,\"optionText\":\"4\",\"isCorrect\":true},"
                + "{\"optionOrder\":2,\"optionText\":\"5\",\"isCorrect\":false},"
                + "{\"optionOrder\":3,\"optionText\":\"6\",\"isCorrect\":false}"
                + "]}";

        mvc.perform(put("/api/admin/questions/" + qId)
                        .header("Authorization", auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OPTIONS_IMMUTABLE"));
    }

    @Test
    @DisplayName("Option Immutability (BR-38): Updating option TEXT on an answered question is allowed (200 OK)")
    void optionImmutabilityTextUpdateAllowedTest() throws Exception {
        Question q = createApprovedQuestion("Capital of Sri Lanka?", "Colombo", "Kandy", 1);
        Long qId = q.getQuestionId();

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(verifiedStudent.getUserId());
        attempt.setAttemptType(QuizAttemptType.CHAPTER_QUIZ);
        attempt.setQuizId(quiz.getQuizId());
        attempt.setTotalMarks(1);
        attempt.setStartedAt(OffsetDateTime.now());
        attempt.setCompletedAt(OffsetDateTime.now());
        attempt = quizAttempts.save(attempt);

        QuizAnswer ans = new QuizAnswer();
        ans.setAttemptId(attempt.getAttemptId());
        ans.setQuestionId(qId);
        ans.setQuestionOrder(1);
        quizAnswers.save(ans);

        // Update option text only, keeping orders and isCorrect identical
        String updateJson = "{"
                + "\"questionText\":\"Capital of Sri Lanka?\","
                + "\"difficulty\":\"EASY\","
                + "\"language\":\"EN\","
                + "\"topicId\":" + topic.getTopicId() + ","
                + "\"options\":["
                + "{\"optionOrder\":1,\"optionText\":\"Sri Jayawardenepura Kotte\",\"isCorrect\":true},"
                + "{\"optionOrder\":2,\"optionText\":\"Kandy City\",\"isCorrect\":false}"
                + "]}";

        mvc.perform(put("/api/admin/questions/" + qId)
                        .header("Authorization", auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].optionText").value("Sri Jayawardenepura Kotte"));
    }

    // --- Section A3: Bulk Status with Partial Failures ---

    @Test
    @DisplayName("Bulk Status: Independent processing returns updatedIds and skipped reasons without rolling back valid ones")
    void bulkStatusIndependentExecutionTest() throws Exception {
        Question q1 = createApprovedQuestion("Q1", "A", "B", 1); // APPROVED -> can transition to ARCHIVED
        Question q2 = createApprovedQuestion("Q2", "A", "B", 1); // APPROVED -> can transition to ARCHIVED

        // q3 is ARCHIVED -> transition to APPROVED is invalid (throws 409 INVALID_STATUS_TRANSITION)
        Question q3 = createApprovedQuestion("Q3", "A", "B", 1);
        q3.setStatus(QuestionStatus.ARCHIVED);
        questions.save(q3);

        String bulkJson = "{"
                + "\"questionIds\":[" + q1.getQuestionId() + "," + q3.getQuestionId() + "," + q2.getQuestionId() + "," + 99999L + "],"
                + "\"targetStatus\":\"ARCHIVED\""
                + "}";

        mvc.perform(post("/api/admin/questions/bulk-status")
                        .header("Authorization", auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedIds", hasItems(q1.getQuestionId().intValue(), q2.getQuestionId().intValue())))
                .andExpect(jsonPath("$.skipped", hasSize(2))); // q3 (invalid ARCHIVED->ARCHIVED or already archived) and 99999 (not found)
    }

    // --- Section D: Chapter Quiz Attempts & Public Availability ---

    @Test
    @DisplayName("Public Availability: /api/quizzes/{id} is public and reports availability without questions")
    void publicQuizAvailabilityTest() throws Exception {
        createApprovedQuestion("Q1", "A", "B", 1);
        createApprovedQuestion("Q2", "A", "B", 1);

        mvc.perform(get("/api/quizzes/" + quiz.getQuizId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.questions").doesNotExist());
    }

    @Test
    @DisplayName("Chapter Quiz: Start, Resume, and Submit lifecycle")
    void chapterQuizAttemptLifecycleTest() throws Exception {
        Question q1 = createApprovedQuestion("Q1", "A", "B", 1);
        Question q2 = createApprovedQuestion("Q2", "C", "D", 2);

        // Start attempt
        MvcResult startResult = mvc.perform(post("/api/quizzes/" + quiz.getQuizId() + "/attempts")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"EN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.resumed").value(false))
                .andExpect(jsonPath("$.questions", hasSize(2)))
                .andReturn();

        JsonNode startBody = mapper.readTree(startResult.getResponse().getContentAsString());
        long attemptId = startBody.get("attemptId").asLong();

        // Second POST resumes the same attempt
        mvc.perform(post("/api/quizzes/" + quiz.getQuizId() + "/attempts")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(attemptId))
                .andExpect(jsonPath("$.resumed").value(true));

        // Submit with missing answer -> 400 UNANSWERED_QUESTIONS
        mvc.perform(post("/api/quiz-attempts/" + attemptId + "/submit")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":" + q1.getQuestionId() + ",\"selectedOptionId\":" + q1.getOptions().get(0).getOptionId() + "}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNANSWERED_QUESTIONS"));

        // Non-owner cannot view attempt -> 404
        mvc.perform(get("/api/quiz-attempts/" + attemptId)
                        .header("Authorization", auth(otherStudentToken)))
                .andExpect(status().isNotFound());

        // Full submission: Q1 correct, Q2 wrong
        String submitJson = "{\"answers\":["
                + "{\"questionId\":" + q1.getQuestionId() + ",\"selectedOptionId\":" + q1.getOptions().get(0).getOptionId() + "},"
                + "{\"questionId\":" + q2.getQuestionId() + ",\"selectedOptionId\":" + q2.getOptions().get(0).getOptionId() + "}"
                + "]}";

        mvc.perform(post("/api/quiz-attempts/" + attemptId + "/submit")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.totalMarks").value(2))
                .andExpect(jsonPath("$.percentage").value(50.0))
                .andExpect(jsonPath("$.chapterBreakdown", hasSize(1)));

        // Double submit -> 409 ATTEMPT_ALREADY_SUBMITTED
        mvc.perform(post("/api/quiz-attempts/" + attemptId + "/submit")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTEMPT_ALREADY_SUBMITTED"));

        // my-attempts endpoint returns completed attempt
        mvc.perform(get("/api/quizzes/" + quiz.getQuizId() + "/my-attempts")
                        .header("Authorization", auth(verifiedStudentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.attemptCount").value(1))
                .andExpect(jsonPath("$.summary.latestPercentage").value(50.0));
    }

    // --- Section B & G: Email Verification Policy for Assessments ---

    @Test
    @DisplayName("Email Verification Policy: Unverified student can take chapter quiz but gets 403 EMAIL_NOT_VERIFIED on assessments")
    void emailVerificationPolicyTest() throws Exception {
        createApprovedQuestion("Q1", "A", "B", 1);
        createApprovedQuestion("Q2", "C", "D", 2);

        // Chapter quiz allowed for unverified student (201 Created)
        mvc.perform(post("/api/quizzes/" + quiz.getQuizId() + "/attempts")
                        .header("Authorization", auth(unverifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());

        // Assessment create forbidden for unverified student -> 403 EMAIL_NOT_VERIFIED
        String assessmentJson = "{"
                + "\"assessmentType\":\"INITIAL_DIAGNOSTIC\","
                + "\"scopeType\":\"CHAPTERS\","
                + "\"chapterIds\":[" + chapter.getChapterId() + "]"
                + "}";

        mvc.perform(post("/api/diagnostic-assessments")
                        .header("Authorization", auth(unverifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assessmentJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        // Verified student can create assessment -> 201 Created
        mvc.perform(post("/api/diagnostic-assessments")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assessmentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    // --- Section E: Mistakes & Practice Lifecycle ---

    @Test
    @DisplayName("Mistakes: Created on wrong answer, mastered after 3 correct practice answers, reset on wrong")
    void mistakesLifecycleTest() throws Exception {
        Question q1 = createApprovedQuestion("Q1", "A", "B", 1);
        Question q2 = createApprovedQuestion("Q2", "C", "D", 2);

        // Start attempt and submit wrong answer for Q2
        MvcResult startResult = mvc.perform(post("/api/quizzes/" + quiz.getQuizId() + "/attempts")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();
        long attemptId = mapper.readTree(startResult.getResponse().getContentAsString()).get("attemptId").asLong();

        String submitJson = "{\"answers\":["
                + "{\"questionId\":" + q1.getQuestionId() + ",\"selectedOptionId\":" + q1.getOptions().get(0).getOptionId() + "},"
                + "{\"questionId\":" + q2.getQuestionId() + ",\"selectedOptionId\":" + q2.getOptions().get(0).getOptionId() + "}" // wrong for Q2
                + "]}";

        mvc.perform(post("/api/quiz-attempts/" + attemptId + "/submit")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitJson))
                .andExpect(status().isOk());

        // Mistake record for Q2 should now be ACTIVE
        mvc.perform(get("/api/me/mistakes")
                        .header("Authorization", auth(verifiedStudentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].questionId").value(q2.getQuestionId()))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].correctStreak").value(0));

        // Mistakes summary
        mvc.perform(get("/api/me/mistakes/summary")
                        .header("Authorization", auth(verifiedStudentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActive").value(1))
                .andExpect(jsonPath("$.totalPracticeable").value(1));

        // Start mistake practice
        MvcResult practiceResult = mvc.perform(post("/api/me/mistakes/practice")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attemptType").value("MISTAKE_PRACTICE"))
                .andReturn();
        long practiceAttemptId = mapper.readTree(practiceResult.getResponse().getContentAsString()).get("attemptId").asLong();

        // Submit correct answer for Q2 in practice
        String practiceSubmit1 = "{\"answers\":[{\"questionId\":" + q2.getQuestionId() + ",\"selectedOptionId\":" + q2.getOptions().get(1).getOptionId() + "}]}";
        mvc.perform(post("/api/quiz-attempts/" + practiceAttemptId + "/submit")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(practiceSubmit1))
                .andExpect(status().isOk());

        MistakeRecord mr = mistakeRecords.findByUserIdAndQuestionId(verifiedStudent.getUserId(), q2.getQuestionId()).orElseThrow();
        assertEquals(1, mr.getCorrectStreak());
        assertEquals(MistakeStatus.ACTIVE, mr.getStatus());

        // Practice #2 correct
        long practiceAttemptId2 = mapper.readTree(mvc.perform(post("/api/me/mistakes/practice")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse().getContentAsString()).get("attemptId").asLong();
        mvc.perform(post("/api/quiz-attempts/" + practiceAttemptId2 + "/submit")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(practiceSubmit1)).andExpect(status().isOk());

        mr = mistakeRecords.findByUserIdAndQuestionId(verifiedStudent.getUserId(), q2.getQuestionId()).orElseThrow();
        assertEquals(2, mr.getCorrectStreak());

        // Practice #3 correct -> MASTERED!
        long practiceAttemptId3 = mapper.readTree(mvc.perform(post("/api/me/mistakes/practice")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse().getContentAsString()).get("attemptId").asLong();
        mvc.perform(post("/api/quiz-attempts/" + practiceAttemptId3 + "/submit")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(practiceSubmit1)).andExpect(status().isOk());

        mr = mistakeRecords.findByUserIdAndQuestionId(verifiedStudent.getUserId(), q2.getQuestionId()).orElseThrow();
        assertEquals(3, mr.getCorrectStreak());
        assertEquals(MistakeStatus.MASTERED, mr.getStatus());
        assertNotNull(mr.getMasteredAt());

        // Practicing now with no active mistakes -> 409 NO_MISTAKES_TO_PRACTICE
        mvc.perform(post("/api/me/mistakes/practice")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_MISTAKES_TO_PRACTICE"));
    }

    // --- Section H: Security - No Answer Leak in In-Progress JSON ---

    @Test
    @DisplayName("Security Checklist: In-progress attempt and assessment JSON MUST NEVER contain isCorrect, correctOptionId, explanation, or sourceExcerpt")
    void securityNoAnswerLeakInJsonTest() throws Exception {
        createApprovedQuestion("Security Q1", "A", "B", 1);
        createApprovedQuestion("Security Q2", "C", "D", 2);

        // 1. In-progress Quiz Attempt
        MvcResult attemptRes = mvc.perform(post("/api/quizzes/" + quiz.getQuizId() + "/attempts")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();

        String attemptJson = attemptRes.getResponse().getContentAsString();
        assertFalse(attemptJson.contains("isCorrect"), "In-progress attempt JSON leaked isCorrect");
        assertFalse(attemptJson.contains("correctOptionId"), "In-progress attempt JSON leaked correctOptionId");
        assertFalse(attemptJson.contains("explanation"), "In-progress attempt JSON leaked explanation");
        assertFalse(attemptJson.contains("sourceExcerpt"), "In-progress attempt JSON leaked sourceExcerpt");

        // 2. In-progress Diagnostic Assessment
        String assessmentReq = "{"
                + "\"assessmentType\":\"INITIAL_DIAGNOSTIC\","
                + "\"scopeType\":\"CHAPTERS\","
                + "\"chapterIds\":[" + chapter.getChapterId() + "]"
                + "}";

        MvcResult assessRes = mvc.perform(post("/api/diagnostic-assessments")
                        .header("Authorization", auth(verifiedStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assessmentReq))
                .andExpect(status().isCreated())
                .andReturn();

        String assessJson = assessRes.getResponse().getContentAsString();
        assertFalse(assessJson.contains("isCorrect"), "In-progress assessment JSON leaked isCorrect");
        assertFalse(assessJson.contains("correctOptionId"), "In-progress assessment JSON leaked correctOptionId");
        assertFalse(assessJson.contains("explanation"), "In-progress assessment JSON leaked explanation");
        assertFalse(assessJson.contains("sourceExcerpt"), "In-progress assessment JSON leaked sourceExcerpt");
    }

    // --- Section A4: Admin Question List Fields & Filters ---

    @Test
    @DisplayName("Admin Question List: Verifies fields (answerCount, correctRate, openReportCount, sourceDocumentRemoved) and filters")
    void adminQuestionListFieldsAndFiltersTest() throws Exception {
        // Create 2 questions
        Question q1 = createApprovedQuestion("Filter Q1", "A", "B", 1);
        Question q2 = createApprovedQuestion("Filter Q2", "C", "D", 2);

        // Add a document
        ChapterDocument doc = new ChapterDocument(chapter.getChapterId(), "lecture.pdf", "/uploads/lecture.pdf", 1024L, "application/pdf", admin.getUserId());
        doc.setExtractedText("Extracted text");
        doc = chapterDocuments.save(doc);

        q1.setSourceDocumentId(doc.getDocumentId());
        q1.setGenerationJobId(555L);
        questions.save(q1);

        // Create an open report on Q2
        QuestionReport rep = new QuestionReport();
        rep.setQuestionId(q2.getQuestionId());
        rep.setUserId(verifiedStudent.getUserId());
        rep.setReason(QuestionReportReason.UNCLEAR);
        rep.setComment("Typo in question");
        rep.setStatus(QuestionReportStatus.OPEN);
        questionReports.save(rep);

        // Record 1 completed answer on Q1
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(verifiedStudent.getUserId());
        attempt.setAttemptType(QuizAttemptType.CHAPTER_QUIZ);
        attempt.setQuizId(quiz.getQuizId());
        attempt.setTotalMarks(1);
        attempt.setScore(1);
        attempt.setStartedAt(OffsetDateTime.now());
        attempt.setCompletedAt(OffsetDateTime.now());
        attempt = quizAttempts.save(attempt);

        QuizAnswer ans = new QuizAnswer();
        ans.setAttemptId(attempt.getAttemptId());
        ans.setQuestionId(q1.getQuestionId());
        ans.setQuestionOrder(1);
        ans.setSelectedOptionId(q1.getOptions().get(0).getOptionId());
        ans.setIsCorrect(true);
        quizAnswers.save(ans);

        // 1. Check fields without filters
        mvc.perform(get("/api/admin/chapters/" + chapter.getChapterId() + "/questions")
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        // 2. Filter by sourceDocumentId
        mvc.perform(get("/api/admin/chapters/" + chapter.getChapterId() + "/questions")
                        .param("sourceDocumentId", doc.getDocumentId().toString())
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].questionId").value(q1.getQuestionId()))
                .andExpect(jsonPath("$.content[0].answerCount").value(1))
                .andExpect(jsonPath("$.content[0].correctRate").value(100.0))
                .andExpect(jsonPath("$.content[0].sourceDocumentRemoved").value(false));

        // 3. Filter by generationJobId
        mvc.perform(get("/api/admin/chapters/" + chapter.getChapterId() + "/questions")
                        .param("generationJobId", "555")
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].questionId").value(q1.getQuestionId()));

        // 4. Filter by reported = true (Q2 has open report)
        mvc.perform(get("/api/admin/chapters/" + chapter.getChapterId() + "/questions")
                        .param("reported", "true")
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].questionId").value(q2.getQuestionId()))
                .andExpect(jsonPath("$.content[0].openReportCount").value(1))
                .andExpect(jsonPath("$.content[0].correctRate").doesNotExist()); // null when no answers

        // 5. Delete document and filter by sourceDocumentRemoved = true
        chapterDocuments.delete(doc);
        mvc.perform(get("/api/admin/chapters/" + chapter.getChapterId() + "/questions")
                        .param("sourceDocumentRemoved", "true")
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].questionId").value(q1.getQuestionId()))
                .andExpect(jsonPath("$.content[0].sourceDocumentRemoved").value(true));
    }

    @Test
    @DisplayName("Admin questions endpoint across chapters, subjects, and student 403")
    void adminAllQuestionsEndpointTest() throws Exception {
        Chapter c2 = chapters.save(new Chapter(subject, "Chapter 2", 2, null));
        Quiz qz2 = new Quiz(c2.getChapterId(), "Quiz 2");
        qz2.setStatus(QuizStatus.PUBLISHED);
        qz2 = quizzes.save(qz2);

        Subject s2 = subjects.save(new Subject("Science", "Science Subject"));
        Chapter c3 = chapters.save(new Chapter(s2, "Chapter 3", 1, null));
        Quiz qz3 = new Quiz(c3.getChapterId(), "Quiz 3");
        qz3.setStatus(QuizStatus.PUBLISHED);
        qz3 = quizzes.save(qz3);

        Question q1 = new Question();
        q1.setQuizId(quiz.getQuizId());
        q1.setQuestionText("Question in Chapter 1");
        q1.setDifficulty(QuestionDifficulty.EASY);
        q1.setLanguage("EN");
        q1.setStatus(QuestionStatus.PENDING);
        q1.setSource(QuestionSource.MANUAL);
        q1.setCreatedBy(admin.getUserId());
        q1 = questions.save(q1);

        Question q2 = new Question();
        q2.setQuizId(qz2.getQuizId());
        q2.setQuestionText("Question in Chapter 2");
        q2.setDifficulty(QuestionDifficulty.MEDIUM);
        q2.setLanguage("EN");
        q2.setStatus(QuestionStatus.PENDING);
        q2.setSource(QuestionSource.MANUAL);
        q2.setCreatedBy(admin.getUserId());
        q2 = questions.save(q2);

        Question q3 = new Question();
        q3.setQuizId(qz3.getQuizId());
        q3.setQuestionText("Question in Chapter 3");
        q3.setDifficulty(QuestionDifficulty.HARD);
        q3.setLanguage("EN");
        q3.setStatus(QuestionStatus.APPROVED);
        q3.setSource(QuestionSource.MANUAL);
        q3.setCreatedBy(admin.getUserId());
        q3 = questions.save(q3);

        // 1. Student gets 403
        mvc.perform(get("/api/admin/questions")
                        .header("Authorization", auth(verifiedStudentToken)))
                .andExpect(status().isForbidden());

        // 2. Admin no filter -> returns questions with chapterId, chapterName, subjectName
        mvc.perform(get("/api/admin/questions")
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[?(@.questionId == " + q1.getQuestionId() + ")].chapterId").value(hasItem(chapter.getChapterId().intValue())))
                .andExpect(jsonPath("$.content[?(@.questionId == " + q1.getQuestionId() + ")].chapterName").value(hasItem("Data Representation")))
                .andExpect(jsonPath("$.content[?(@.questionId == " + q1.getQuestionId() + ")].subjectName").value(hasItem("ICT")));

        // 3. Status filter = PENDING across two chapters
        mvc.perform(get("/api/admin/questions")
                        .param("status", "PENDING")
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.questionId == " + q1.getQuestionId() + ")].questionId").value(hasItem(q1.getQuestionId().intValue())))
                .andExpect(jsonPath("$.content[?(@.questionId == " + q2.getQuestionId() + ")].questionId").value(hasItem(q2.getQuestionId().intValue())))
                .andExpect(jsonPath("$.content[?(@.questionId == " + q3.getQuestionId() + ")]").doesNotExist());

        // 4. Filter by subjectId = subject
        mvc.perform(get("/api/admin/questions")
                        .param("subjectId", subject.getSubjectId().toString())
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.questionId == " + q1.getQuestionId() + ")].questionId").value(hasItem(q1.getQuestionId().intValue())))
                .andExpect(jsonPath("$.content[?(@.questionId == " + q2.getQuestionId() + ")].questionId").value(hasItem(q2.getQuestionId().intValue())))
                .andExpect(jsonPath("$.content[?(@.questionId == " + q3.getQuestionId() + ")]").doesNotExist());

        // 5. Quiz summary and detail return pendingCount
        mvc.perform(get("/api/admin/quizzes/" + quiz.getQuizId())
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCount").value(1));

        mvc.perform(get("/api/admin/quizzes")
                        .param("chapterId", chapter.getChapterId().toString())
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].pendingCount").value(1));
    }
}
