package com.allearning.backend.learning;

import com.allearning.backend.academic.entity.Chapter;
import com.allearning.backend.academic.entity.Subject;
import com.allearning.backend.academic.repository.ChapterRepository;
import com.allearning.backend.academic.repository.SubjectRepository;
import com.allearning.backend.auth.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:learningintegrationtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
class LearningIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy securityFilter;
    @Autowired JwtService jwtService;
    @Autowired UserRepository users;
    @Autowired SubjectRepository subjects;
    @Autowired ChapterRepository chapters;
    @Autowired ChapterTopicRepository topics;
    @Autowired QuizRepository quizzes;
    @Autowired QuestionRepository questions;

    private MockMvc mvc;
    private ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private User admin;
    private String adminToken;
    private String studentToken;
    private Chapter chapter;
    private Chapter secondChapter;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilter(securityFilter).build();
        questions.deleteAll();
        quizzes.deleteAll();
        topics.deleteAll();
        chapters.deleteAll();
        subjects.deleteAll();
        users.deleteAll();

        admin = users.save(new User("Learning Admin", "learning-admin@test.com", "hash", "+94770000001", UserRole.ADMIN, AccountStatus.ACTIVE));
        admin.setEmailVerified(true);
        admin = users.save(admin);
        adminToken = jwtService.generateToken(admin);
        User student = users.save(new User("Learning Student", "learning-student@test.com", "hash", "+94770000002", UserRole.STUDENT, AccountStatus.ACTIVE));
        student.setEmailVerified(true);
        users.save(student);
        studentToken = jwtService.generateToken(student);

        Subject subject = subjects.save(new Subject("Learning", "Learning subject"));
        chapter = chapters.save(new Chapter(subject, "Algebra", 1, null));
        secondChapter = chapters.save(new Chapter(subject, "Geometry", 2, null));
    }

    private String auth(String token) { return "Bearer " + token; }

    private JsonNode postTopic(String name, Long chapterId, String token) throws Exception {
        MvcResult result = mvc.perform(post("/api/admin/chapters/" + chapterId + "/topics")
                        .header("Authorization", auth(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn();
        return result.getResponse().getContentAsString().isBlank() ? null : mapper.readTree(result.getResponse().getContentAsString());
    }

    private Long createTopic(String name) throws Exception {
        JsonNode body = postTopic(name, chapter.getChapterId(), adminToken);
        assertNotNull(body);
        return body.get("topicId").asLong();
    }

    private String questionJson(Long topicId, String text, String saveMode) {
        String mode = saveMode == null ? "" : ",\"saveMode\":\"" + saveMode + "\"";
        return "{\"topicId\":" + topicId + ",\"questionText\":\"" + text +
                "\",\"difficulty\":\"EASY\",\"language\":\"EN\",\"options\":[" +
                "{\"optionText\":\"A\",\"optionOrder\":1,\"isCorrect\":true}," +
                "{\"optionText\":\"B\",\"optionOrder\":2,\"isCorrect\":false}]\"" + mode.replaceFirst("^,", "},\"") + "}";
    }

    private Long createQuestion(Long topicId, String saveMode) throws Exception {
        String mode = saveMode == null ? "" : ",\"saveMode\":\"" + saveMode + "\"";
        String body = "{\"topicId\":" + topicId + ",\"questionText\":\"Which answer is correct?\",\"difficulty\":\"EASY\",\"language\":\"EN\"" +
                ",\"options\":[{\"optionText\":\"A\",\"optionOrder\":1,\"isCorrect\":true},{\"optionText\":\"B\",\"optionOrder\":2,\"isCorrect\":false}]}" ;
        if (!saveModeIsNull(saveMode)) body = body.replace("}]}", "}]"+mode+"}");
        MvcResult result = mvc.perform(post("/api/admin/chapters/" + chapter.getChapterId() + "/questions")
                        .header("Authorization", auth(adminToken)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("questionId").asLong();
    }
    private boolean saveModeIsNull(String saveMode) { return saveMode == null; }

    @Test void t1_adminCreatesTopic() throws Exception {
        mvc.perform(post("/api/admin/chapters/" + chapter.getChapterId() + "/topics")
                .header("Authorization", auth(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Angles\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.topicId").exists());
    }
    @Test void t2_duplicateTopicName() throws Exception {
        createTopic("Angles");
        mvc.perform(post("/api/admin/chapters/" + chapter.getChapterId() + "/topics")
                .header("Authorization", auth(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"angles\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_TOPIC_NAME"));
    }
    @Test void t3_sameTopicNameDifferentChapter() throws Exception {
        createTopic("Angles");
        mvc.perform(post("/api/admin/chapters/" + secondChapter.getChapterId() + "/topics")
                .header("Authorization", auth(adminToken)).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Angles\"}"))
                .andExpect(status().isCreated());
    }
    @Test void t4_updateTopicName() throws Exception {
        Long id = createTopic("Angles");
        mvc.perform(patch("/api/admin/topics/" + id).header("Authorization", auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Triangles\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Triangles"));
    }
    @Test void t5_updateTopicStatusInactive() throws Exception {
        Long id = createTopic("Angles");
        mvc.perform(patch("/api/admin/topics/" + id).header("Authorization", auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test void q1_quizAutoCreatedAndQuestionLinked() throws Exception {
        Long topicId = createTopic("Angles");
        Long questionId = createQuestion(topicId, null);
        assertTrue(quizzes.findByChapterId(chapter.getChapterId()).isPresent());
        assertTrue(questions.findById(questionId).isPresent());
    }
    @Test void q2_secondQuestionReusesQuiz() throws Exception {
        Long topicId = createTopic("Angles");
        createQuestion(topicId, null);
        createQuestion(topicId, null);
        assertEquals(1, quizzes.findAll().size());
    }
    @Test void qu1_pendingByDefault() throws Exception {
        Long id = createQuestion(createTopic("Angles"), null);
        mvc.perform(get("/api/admin/questions/" + id).header("Authorization", auth(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
    }
    @Test void qu2_approvedSaveMode() throws Exception {
        Long id = createQuestion(createTopic("Angles"), "APPROVED");
        mvc.perform(get("/api/admin/questions/" + id).header("Authorization", auth(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").value(admin.getUserId()));
    }
    @Test void qu3_inactiveTopicApprovalRejected() throws Exception {
        Long topicId = createTopic("Angles");
        ChapterTopic topic = topics.findById(topicId).orElseThrow();
        topic.setStatus(TopicStatus.INACTIVE);
        topics.save(topic);
        mvc.perform(post("/api/admin/chapters/" + chapter.getChapterId() + "/questions")
                        .header("Authorization", auth(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicId\":"+topicId+",\"questionText\":\"Question\",\"difficulty\":\"EASY\",\"language\":\"EN\",\"saveMode\":\"APPROVED\",\"options\":[{\"optionText\":\"A\",\"optionOrder\":1,\"isCorrect\":true},{\"optionText\":\"B\",\"optionOrder\":2,\"isCorrect\":false}]}")
                ).andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.containsString("Approval precondition failed")));
    }
    @Test void qu4_listQuestionAndAnswerCount() throws Exception {
        createQuestion(createTopic("Angles"), null);
        mvc.perform(get("/api/admin/chapters/" + chapter.getChapterId() + "/questions")
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].answerCount").value(0));
    }
    @Test void qu5_detailIncludesOptions() throws Exception {
        Long id = createQuestion(createTopic("Angles"), null);
        mvc.perform(get("/api/admin/questions/" + id).header("Authorization", auth(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.options.length()").value(2));
    }

    @Test void st1_pendingToApproved() throws Exception {
        Long id = createQuestion(createTopic("Angles"), null);
        mvc.perform(patch("/api/admin/questions/" + id + "/status").header("Authorization", auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
    }
    @Test void st2_pendingToArchived() throws Exception {
        Long id = createQuestion(createTopic("Angles"), null);
        mvc.perform(patch("/api/admin/questions/" + id + "/status").header("Authorization", auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"));
    }
    @Test void st3_archivedToApprovedConflict() throws Exception {
        Long id = createQuestion(createTopic("Angles"), null);
        mvc.perform(patch("/api/admin/questions/" + id + "/status").header("Authorization", auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ARCHIVED\"}"));
        mvc.perform(patch("/api/admin/questions/" + id + "/status").header("Authorization", auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test void securityNoAuth() throws Exception {
        mvc.perform(get("/api/admin/chapters/" + chapter.getChapterId() + "/topics")).andExpect(status().isUnauthorized());
    }
    @Test void securityStudentForbidden() throws Exception {
        mvc.perform(get("/api/admin/chapters/" + chapter.getChapterId() + "/topics").header("Authorization", auth(studentToken)))
                .andExpect(status().isForbidden());
    }
    @Test void securityAdminAllowed() throws Exception {
        mvc.perform(get("/api/admin/chapters/" + chapter.getChapterId() + "/topics").header("Authorization", auth(adminToken)))
                .andExpect(status().isOk());
    }
    @Test void questionCreationRequiresAuth() throws Exception {
        mvc.perform(post("/api/admin/chapters/" + chapter.getChapterId() + "/questions")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void qz1_quizFilter() throws Exception {
        Long topicId = createTopic("Angles");
        createQuestion(topicId, null);
        mvc.perform(get("/api/admin/quizzes").param("chapterId", chapter.getChapterId().toString()).header("Authorization", auth(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].chapterId").value(chapter.getChapterId()));
    }
    @Test void qz2_updateQuiz() throws Exception {
        Long topicId = createTopic("Angles");
        createQuestion(topicId, null);
        Long quizId = quizzes.findByChapterId(chapter.getChapterId()).orElseThrow().getQuizId();
        mvc.perform(patch("/api/admin/quizzes/" + quizId).header("Authorization", auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Updated title\",\"description\":\"Updated description\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated title"));
    }
    @Test void qz3_publishQuiz() throws Exception {
        Long topicId = createTopic("Angles");
        createQuestion(topicId, null);
        Long quizId = quizzes.findByChapterId(chapter.getChapterId()).orElseThrow().getQuizId();
        mvc.perform(patch("/api/admin/quizzes/" + quizId + "/status").header("Authorization", auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
    }
}
