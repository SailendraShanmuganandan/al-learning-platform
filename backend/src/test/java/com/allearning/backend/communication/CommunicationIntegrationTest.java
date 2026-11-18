package com.allearning.backend.communication;

import com.allearning.backend.auth.AccountStatus;
import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.communication.dto.*;
import com.allearning.backend.communication.entity.Announcement;
import com.allearning.backend.communication.entity.AnnouncementStatus;
import com.allearning.backend.communication.entity.ContactMessage;
import com.allearning.backend.communication.entity.ContactMessageStatus;
import com.allearning.backend.communication.repository.AnnouncementRepository;
import com.allearning.backend.communication.repository.ContactMessageRepository;
import com.allearning.backend.communication.service.ContactRateLimiter;
import com.allearning.backend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:communicationintegrationtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "app.contact.rate-limit-per-hour=5"
})
public class CommunicationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @Autowired
    private ContactRateLimiter contactRateLimiter;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private User adminUser;
    private User studentUser;
    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilter(springSecurityFilterChain)
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();

        contactRateLimiter.reset();
        contactMessageRepository.deleteAll();
        announcementRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = new User("Admin Officer", "admin@test.com", "hash", "+94771111111", UserRole.ADMIN, AccountStatus.ACTIVE);
        adminUser.setEmailVerified(true);
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateToken(adminUser);

        studentUser = new User("Student User", "student@test.com", "hash", "+94772222222", UserRole.STUDENT, AccountStatus.ACTIVE);
        studentUser.setEmailVerified(true);
        studentUser = userRepository.save(studentUser);
        studentToken = jwtService.generateToken(studentUser);
    }

    // =========================================================================
    // ANNOUNCEMENTS TESTS
    // =========================================================================

    @Test
    @DisplayName("Public announcement endpoints: only PUBLISHED visible, no admin identity in public DTOs")
    void testPublicAnnouncements() throws Exception {
        // Create 1 DRAFT, 1 ARCHIVED, 2 PUBLISHED
        Announcement draft = new Announcement(adminUser.getUserId(), "Draft Notice", "<script>alert('xss')</script> Content for draft", AnnouncementStatus.DRAFT);
        announcementRepository.save(draft);

        Announcement archived = new Announcement(adminUser.getUserId(), "Archived Notice", "Archived content", AnnouncementStatus.ARCHIVED);
        announcementRepository.save(archived);

        Announcement pub1 = new Announcement(adminUser.getUserId(), "Published 1", "Public content 1", AnnouncementStatus.PUBLISHED);
        pub1.setPublishedAt(OffsetDateTime.now().minusHours(2));
        pub1 = announcementRepository.save(pub1);

        Announcement pub2 = new Announcement(adminUser.getUserId(), "Published 2", "Public content 2 with <b style='color:red'>html</b> tags", AnnouncementStatus.PUBLISHED);
        pub2.setPublishedAt(OffsetDateTime.now().minusHours(1));
        pub2 = announcementRepository.save(pub2);

        // GET /api/announcements (public)
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(pub2.getAnnouncementId().intValue())))
                .andExpect(jsonPath("$.content[0].title", is("Published 2")))
                .andExpect(jsonPath("$.content[0].excerpt", containsString("Public content 2 with <b style='color:red'>html</b> tags")))
                // Verify public DTO does NOT expose created_by or createdByName
                .andExpect(jsonPath("$.content[0].createdBy").doesNotExist())
                .andExpect(jsonPath("$.content[0].createdByName").doesNotExist())
                .andExpect(jsonPath("$.content[1].id", is(pub1.getAnnouncementId().intValue())));

        // GET /api/announcements/{id}
        mockMvc.perform(get("/api/announcements/" + pub1.getAnnouncementId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(pub1.getAnnouncementId().intValue())))
                .andExpect(jsonPath("$.title", is("Published 1")))
                .andExpect(jsonPath("$.content", is("Public content 1")))
                .andExpect(jsonPath("$.createdBy").doesNotExist())
                .andExpect(jsonPath("$.createdByName").doesNotExist());

        // DRAFT and ARCHIVED return 404
        mockMvc.perform(get("/api/announcements/" + draft.getAnnouncementId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/announcements/" + archived.getAnnouncementId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Admin announcement CRUD and state machine transitions (DRAFT -> PUBLISHED -> ARCHIVED -> DRAFT)")
    void testAdminAnnouncementLifecycleAndTransitions() throws Exception {
        // 1. Unauthenticated / Student rejected
        mockMvc.perform(get("/api/admin/announcements"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/announcements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // 2. Admin creates announcement
        AnnouncementCreateRequest createReq = new AnnouncementCreateRequest("Exam Timetable 2026", "The timetable is now finalized.");
        String createJson = mockMvc.perform(post("/api/admin/announcements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Exam Timetable 2026")))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.createdBy", is(adminUser.getUserId().intValue())))
                .andExpect(jsonPath("$.createdByName", is("Admin Officer")))
                .andExpect(jsonPath("$.publishedAt").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        Number idNum = com.jayway.jsonpath.JsonPath.read(createJson, "$.id");
        Long id = idNum.longValue();

        // 3. Admin edits title and content in DRAFT status
        AnnouncementUpdateRequest updateReq = new AnnouncementUpdateRequest("Exam Timetable 2026 - Revised", "Updated schedule details.");
        mockMvc.perform(patch("/api/admin/announcements/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Exam Timetable 2026 - Revised")))
                .andExpect(jsonPath("$.content", is("Updated schedule details.")));

        // 4. Invalid transition: DRAFT -> DRAFT (409)
        mockMvc.perform(patch("/api/admin/announcements/" + id + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnnouncementStatusUpdateRequest(AnnouncementStatus.DRAFT))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVALID_TRANSITION")));

        // 5. Allowed transition: DRAFT -> PUBLISHED (sets published_at)
        mockMvc.perform(patch("/api/admin/announcements/" + id + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnnouncementStatusUpdateRequest(AnnouncementStatus.PUBLISHED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.publishedAt", notNullValue()));

        // 6. Invalid transition: PUBLISHED -> DRAFT (409)
        mockMvc.perform(patch("/api/admin/announcements/" + id + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnnouncementStatusUpdateRequest(AnnouncementStatus.DRAFT))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVALID_TRANSITION")));

        // 7. Allowed transition: PUBLISHED -> ARCHIVED
        mockMvc.perform(patch("/api/admin/announcements/" + id + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnnouncementStatusUpdateRequest(AnnouncementStatus.ARCHIVED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ARCHIVED")));

        // 8. Invalid transition: ARCHIVED -> PUBLISHED (409)
        mockMvc.perform(patch("/api/admin/announcements/" + id + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnnouncementStatusUpdateRequest(AnnouncementStatus.PUBLISHED))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVALID_TRANSITION")));

        // 9. Allowed transition: ARCHIVED -> DRAFT
        mockMvc.perform(patch("/api/admin/announcements/" + id + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnnouncementStatusUpdateRequest(AnnouncementStatus.DRAFT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DRAFT")));

        // 10. Re-publish from DRAFT -> updates published_at
        mockMvc.perform(patch("/api/admin/announcements/" + id + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnnouncementStatusUpdateRequest(AnnouncementStatus.PUBLISHED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PUBLISHED")))
                .andExpect(jsonPath("$.publishedAt", notNullValue()));
    }

    // =========================================================================
    // CONTACT MESSAGES TESTS
    // =========================================================================

    @Test
    @DisplayName("POST /api/contact: works for guest, logged-in student, expired token; enforces honeypot and validation")
    void testPublicContactForm() throws Exception {
        // 1. Guest submission -> 201, no user linked
        ContactMessageRequest validReq = new ContactMessageRequest(
                "Jane Doe",
                "Jane.Doe@Example.COM",
                "+94773333333",
                "I would like to inquire about the Combined Maths revision class.",
                null
        );

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", notNullValue()));

        assertEquals(1, contactMessageRepository.count());
        ContactMessage saved = contactMessageRepository.findAll().get(0);
        assertEquals("Jane Doe", saved.getName());
        assertEquals("jane.doe@example.com", saved.getEmail()); // Lowercase check
        assertEquals("+94773333333", saved.getPhone());
        assertEquals(ContactMessageStatus.UNREAD, saved.getStatus());

        // 2. Logged-in student submission (no user FK linked)
        contactRateLimiter.reset();
        ContactMessageRequest studentReq = new ContactMessageRequest(
                "Student Name",
                "student@example.com",
                null,
                "Can you please explain lesson 4 in Physics?",
                ""
        );

        mockMvc.perform(post("/api/contact")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));

        assertEquals(2, contactMessageRepository.count());

        // 3. Expired / Invalid token in header -> processed normally, NOT 401
        contactRateLimiter.reset();
        String fakeExpiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJmb28iLCJleHAiOjEwMDAwMDAwMDB9.invalid";
        mockMvc.perform(post("/api/contact")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fakeExpiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentReq)))
                .andExpect(status().isCreated());

        assertEquals(3, contactMessageRepository.count());

        // 4. Honeypot submission -> returns 201, but creates NO row
        contactRateLimiter.reset();
        ContactMessageRequest botReq = new ContactMessageRequest(
                "Spam Bot",
                "bot@spam.com",
                null,
                "Buy discount medicines right here at our shop!",
                "https://spamsite.xyz"
        );

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(botReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));

        // Still 3 rows! Honeypot created no row
        assertEquals(3, contactMessageRepository.count());

        // 5. Validation failures -> 400
        // Invalid email
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContactMessageRequest("Alice", "not-an-email", null, "Hello valid message", null))))
                .andExpect(status().isBadRequest());

        // Short message (< 10 chars)
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContactMessageRequest("Alice", "alice@example.com", null, "Hi", null))))
                .andExpect(status().isBadRequest());

        // Invalid phone format
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContactMessageRequest("Alice", "alice@example.com", "0771234567", "Hello valid message", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Rate limiting: 6th submission within an hour from same IP -> 429 with Retry-After header")
    void testContactRateLimiter() throws Exception {
        contactRateLimiter.reset();

        ContactMessageRequest req = new ContactMessageRequest(
                "Test User",
                "test@example.com",
                null,
                "This is a legitimate question about classes.",
                null
        );

        String reqJson = objectMapper.writeValueAsString(req);

        // Submissions 1 through 5 succeed
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/contact")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(reqJson))
                    .andExpect(status().isCreated());
        }

        // 6th submission -> 429 TOO_MANY_REQUESTS with Retry-After header
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.code", is("TOO_MANY_REQUESTS")));
    }

    @Test
    @DisplayName("Admin contact messages: list, search, view (no status change), update status, and unread count")
    void testAdminContactMessages() throws Exception {
        ContactMessage m1 = new ContactMessage("Kavindu Silva", "kavindu@test.com", "+94771234567", "Question about Combined Maths tuition.");
        contactMessageRepository.save(m1);

        ContactMessage m2 = new ContactMessage("Nimal Perera", "nimal@test.com", null, "Can I get a refund on order #12?");
        m2.setStatus(ContactMessageStatus.READ);
        contactMessageRepository.save(m2);

        ContactMessage m3 = new ContactMessage("Sunil Fernando", "sunil@other.com", null, "Class registration inquiry.");
        m3.setStatus(ContactMessageStatus.RESPONDED);
        contactMessageRepository.save(m3);

        // 1. Unread count -> 1
        mockMvc.perform(get("/api/admin/contact-messages/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(1)))
                .andExpect(jsonPath("$.count", is(1)));

        // 2. List with filter: status = UNREAD
        mockMvc.perform(get("/api/admin/contact-messages?status=UNREAD")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Kavindu Silva")));

        // 3. Search by q = "nimal"
        mockMvc.perform(get("/api/admin/contact-messages?q=nimal")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email", is("nimal@test.com")));

        // 4. View message by ID: reading does NOT change status
        mockMvc.perform(get("/api/admin/contact-messages/" + m1.getMessageId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(m1.getMessageId().intValue())))
                .andExpect(jsonPath("$.status", is("UNREAD")))
                .andExpect(jsonPath("$.message", is("Question about Combined Maths tuition.")));

        // Verify still UNREAD in DB
        ContactMessage afterRead = contactMessageRepository.findById(m1.getMessageId()).orElseThrow();
        assertEquals(ContactMessageStatus.UNREAD, afterRead.getStatus());

        // 5. Update status: UNREAD -> READ
        mockMvc.perform(patch("/api/admin/contact-messages/" + m1.getMessageId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContactMessageStatusUpdateRequest(ContactMessageStatus.READ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("READ")));

        // Setting same status again is a no-op returning 200
        mockMvc.perform(patch("/api/admin/contact-messages/" + m1.getMessageId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContactMessageStatusUpdateRequest(ContactMessageStatus.READ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("READ")));

        // Update to RESPONDED, then ARCHIVED
        mockMvc.perform(patch("/api/admin/contact-messages/" + m1.getMessageId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContactMessageStatusUpdateRequest(ContactMessageStatus.RESPONDED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESPONDED")));

        mockMvc.perform(patch("/api/admin/contact-messages/" + m1.getMessageId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ContactMessageStatusUpdateRequest(ContactMessageStatus.ARCHIVED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ARCHIVED")));

        // Unread count now 0
        mockMvc.perform(get("/api/admin/contact-messages/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(0)));
    }
}
