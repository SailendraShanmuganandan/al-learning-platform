package com.allearning.backend.classes;

import com.allearning.backend.academic.entity.Subject;
import com.allearning.backend.academic.repository.SubjectRepository;
import com.allearning.backend.auth.AccountStatus;
import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRepository;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.classes.dto.ClassStatusUpdateRequest;
import com.allearning.backend.classes.dto.CreateClassRequest;
import com.allearning.backend.classes.dto.RegistrationStatusUpdateRequest;
import com.allearning.backend.classes.dto.UpdateClassRequest;
import com.allearning.backend.classes.entity.ClassEntity;
import com.allearning.backend.classes.entity.ClassStatus;
import com.allearning.backend.classes.entity.ClassType;
import com.allearning.backend.classes.entity.RegistrationStatus;
import com.allearning.backend.classes.repository.ClassRegistrationRepository;
import com.allearning.backend.classes.repository.ClassRepository;
import com.allearning.backend.security.JwtService;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:classintegrationtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
public class ClassIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private ClassRegistrationRepository classRegistrationRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;
    private User verifiedStudent;
    private User unverifiedStudent;
    private User otherStudent;
    private User admin;
    private String verifiedStudentToken;
    private String unverifiedStudentToken;
    private String otherStudentToken;
    private String adminToken;

    private Subject activeSubject;
    private Subject inactiveSubject;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilter(springSecurityFilterChain)
                .build();

        classRegistrationRepository.deleteAll();
        classRepository.deleteAll();
        subjectRepository.deleteAll();
        userRepository.deleteAll();

        verifiedStudent = new User("Verified Student", "verified@example.com", "$2a$10$hash", "0771234567", UserRole.STUDENT, AccountStatus.ACTIVE);
        verifiedStudent.setEmailVerified(true);
        verifiedStudent = userRepository.save(verifiedStudent);
        verifiedStudentToken = "Bearer " + jwtService.generateToken(verifiedStudent);

        unverifiedStudent = new User("Unverified Student", "unverified@example.com", "$2a$10$hash", "0771234568", UserRole.STUDENT, AccountStatus.ACTIVE);
        unverifiedStudent.setEmailVerified(false);
        unverifiedStudent = userRepository.save(unverifiedStudent);
        unverifiedStudentToken = "Bearer " + jwtService.generateToken(unverifiedStudent);

        otherStudent = new User("Other Student", "other@example.com", "$2a$10$hash", "0771234569", UserRole.STUDENT, AccountStatus.ACTIVE);
        otherStudent.setEmailVerified(true);
        otherStudent = userRepository.save(otherStudent);
        otherStudentToken = "Bearer " + jwtService.generateToken(otherStudent);

        admin = new User("System Admin", "admin@example.com", "$2a$10$hash", null, UserRole.ADMIN, AccountStatus.ACTIVE);
        admin.setEmailVerified(true);
        admin = userRepository.save(admin);
        adminToken = "Bearer " + jwtService.generateToken(admin);

        activeSubject = new Subject("Combined Mathematics", "A/L Maths");
        activeSubject.setStatus("ACTIVE");
        activeSubject = subjectRepository.save(activeSubject);

        inactiveSubject = new Subject("Old Syllabus Subject", "Archived");
        inactiveSubject.setStatus("INACTIVE");
        inactiveSubject = subjectRepository.save(inactiveSubject);
    }

    @Test
    @DisplayName("Public endpoints: hide INACTIVE classes and classes under INACTIVE subjects; filters work; invalid class_type -> 400")
    void publicEndpoints_hideInactive_andFilterCorrectly() throws Exception {
        ClassEntity c1 = new ClassEntity(activeSubject.getSubjectId(), "Active Maths 2026", "Revision", ClassType.HYBRID, "Saturdays 4-6 PM", new BigDecimal("3500.00"), ClassStatus.ACTIVE);
        classRepository.save(c1);

        ClassEntity c2Inactive = new ClassEntity(activeSubject.getSubjectId(), "Inactive Maths Class", "Desc", ClassType.ONLINE, "Sundays 10 AM", new BigDecimal("2500.00"), ClassStatus.INACTIVE);
        classRepository.save(c2Inactive);

        ClassEntity c3UnderInactiveSubject = new ClassEntity(inactiveSubject.getSubjectId(), "Class under Inactive Subject", "Desc", ClassType.PHYSICAL, "Mondays 2 PM", new BigDecimal("2000.00"), ClassStatus.ACTIVE);
        classRepository.save(c3UnderInactiveSubject);

        // 1. GET /api/classes (Public list) -> only c1 should be returned
        mockMvc.perform(get("/api/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(c1.getClassId()))
                .andExpect(jsonPath("$.content[0].name").value("Active Maths 2026"))
                .andExpect(jsonPath("$.content[0].subject.name").value("Combined Mathematics"))
                .andExpect(jsonPath("$.content[0].classType").value("HYBRID"));

        // 2. Filter by invalid class_type -> 400
        mockMvc.perform(get("/api/classes?class_type=INVALID_TYPE"))
                .andExpect(status().isBadRequest());

        // 3. Filter by valid class_type
        mockMvc.perform(get("/api/classes?class_type=HYBRID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(get("/api/classes?class_type=ONLINE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        // 4. Detail endpoint: ACTIVE class -> 200
        mockMvc.perform(get("/api/classes/" + c1.getClassId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c1.getClassId()))
                .andExpect(jsonPath("$.name").value("Active Maths 2026"));

        // 5. Detail endpoint: INACTIVE class -> 404
        mockMvc.perform(get("/api/classes/" + c2Inactive.getClassId()))
                .andExpect(status().isNotFound());

        // 6. Detail endpoint: class under INACTIVE subject -> 404
        mockMvc.perform(get("/api/classes/" + c3UnderInactiveSubject.getClassId()))
                .andExpect(status().isNotFound());

        // 7. Admin endpoint shows all classes including inactive
        mockMvc.perform(get("/api/admin/classes").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("Admin class CRUD: validations (negative fee, >2 decimals, inactive subject, missing schedule) and deactivation")
    void adminClassCrud_validationsAndDeactivation() throws Exception {
        // 1. Negative fee -> 400
        CreateClassRequest negFee = new CreateClassRequest(
                activeSubject.getSubjectId(),
                "Physics Class",
                "Desc",
                ClassType.PHYSICAL,
                "Fridays 3 PM",
                new BigDecimal("-100.00")
        );
        mockMvc.perform(post("/api/admin/classes")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negFee)))
                .andExpect(status().isBadRequest());

        // 2. Fee with 3 decimals -> 400
        CreateClassRequest threeDecFee = new CreateClassRequest(
                activeSubject.getSubjectId(),
                "Physics Class",
                "Desc",
                ClassType.PHYSICAL,
                "Fridays 3 PM",
                new BigDecimal("2500.125")
        );
        mockMvc.perform(post("/api/admin/classes")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(threeDecFee)))
                .andExpect(status().isBadRequest());

        // 3. Inactive subject -> 400
        CreateClassRequest inactiveSub = new CreateClassRequest(
                inactiveSubject.getSubjectId(),
                "Physics Class",
                "Desc",
                ClassType.PHYSICAL,
                "Fridays 3 PM",
                new BigDecimal("2500.00")
        );
        mockMvc.perform(post("/api/admin/classes")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inactiveSub)))
                .andExpect(status().isBadRequest());

        // 4. Missing schedule -> 400
        CreateClassRequest missingSched = new CreateClassRequest(
                activeSubject.getSubjectId(),
                "Physics Class",
                "Desc",
                ClassType.PHYSICAL,
                "",
                new BigDecimal("2500.00")
        );
        mockMvc.perform(post("/api/admin/classes")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingSched)))
                .andExpect(status().isBadRequest());

        // 5. Valid create -> 201
        CreateClassRequest valid = new CreateClassRequest(
                activeSubject.getSubjectId(),
                "Physics 2026 Batch",
                "Full syllabus coverage",
                ClassType.HYBRID,
                "Saturdays 8-11 AM",
                new BigDecimal("4000.00")
        );
        String createdJson = mockMvc.perform(post("/api/admin/classes")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valid)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Physics 2026 Batch"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        Long classId = objectMapper.readTree(createdJson).get("classId").asLong();

        // 6. Update class
        UpdateClassRequest update = new UpdateClassRequest(
                "Physics 2026 Batch Updated",
                "Updated desc",
                ClassType.ONLINE,
                "Saturdays 9-12 AM",
                new BigDecimal("4500.00")
        );
        mockMvc.perform(patch("/api/admin/classes/" + classId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Physics 2026 Batch Updated"))
                .andExpect(jsonPath("$.classType").value("ONLINE"))
                .andExpect(jsonPath("$.fee").value(4500.00));

        // 7. Deactivate class (PATCH status)
        mockMvc.perform(patch("/api/admin/classes/" + classId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ClassStatusUpdateRequest(ClassStatus.INACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        // Public detail now returns 404
        mockMvc.perform(get("/api/classes/" + classId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Student registration: unverified -> 403 EMAIL_NOT_VERIFIED, guest -> 401, admin -> 403, duplicate -> 409 ALREADY_REGISTERED")
    void studentRegistration_rbacAndConflictGuard() throws Exception {
        ClassEntity c = new ClassEntity(activeSubject.getSubjectId(), "Chemistry 2026", "Theory", ClassType.ONLINE, "Sundays 4 PM", new BigDecimal("3000.00"), ClassStatus.ACTIVE);
        c = classRepository.save(c);

        // 1. Guest -> 401
        mockMvc.perform(post("/api/classes/" + c.getClassId() + "/register"))
                .andExpect(status().isUnauthorized());

        // 2. Admin -> 403
        mockMvc.perform(post("/api/classes/" + c.getClassId() + "/register").header("Authorization", adminToken))
                .andExpect(status().isForbidden());

        // 3. Unverified student -> 403 EMAIL_NOT_VERIFIED
        mockMvc.perform(post("/api/classes/" + c.getClassId() + "/register").header("Authorization", unverifiedStudentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        // 4. Verified student -> 201 Created
        mockMvc.perform(post("/api/classes/" + c.getClassId() + "/register").header("Authorization", verifiedStudentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationId").isNumber())
                .andExpect(jsonPath("$.className").value("Chemistry 2026"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // 5. Register again while PENDING -> 409 ALREADY_REGISTERED
        mockMvc.perform(post("/api/classes/" + c.getClassId() + "/register").header("Authorization", verifiedStudentToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.currentStatus").value("PENDING"));
    }

    @Test
    @DisplayName("Lifecycle: Student cancel PENDING -> CANCELLED, can re-register (creates new row); CONFIRMED cancel -> 409 CANCEL_NOT_ALLOWED")
    void studentCancellation_andReRegistrationLifecycle() throws Exception {
        ClassEntity c = new ClassEntity(activeSubject.getSubjectId(), "Biology 2026", "Units 1-4", ClassType.HYBRID, "Sundays 1 PM", new BigDecimal("3200.00"), ClassStatus.ACTIVE);
        c = classRepository.save(c);

        // 1. Register -> PENDING
        String regJson = mockMvc.perform(post("/api/classes/" + c.getClassId() + "/register").header("Authorization", verifiedStudentToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long reg1Id = objectMapper.readTree(regJson).get("registrationId").asLong();

        // 2. Another student cannot cancel this registration -> 404
        mockMvc.perform(patch("/api/my/class-registrations/" + reg1Id + "/cancel").header("Authorization", otherStudentToken))
                .andExpect(status().isNotFound());

        // 3. Student cancels own PENDING registration -> 200 CANCELLED
        mockMvc.perform(patch("/api/my/class-registrations/" + reg1Id + "/cancel").header("Authorization", verifiedStudentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 4. Cancelling an already CANCELLED registration -> 409 INVALID_TRANSITION
        mockMvc.perform(patch("/api/my/class-registrations/" + reg1Id + "/cancel").header("Authorization", verifiedStudentToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));

        // 5. Student re-registers after cancellation -> creates a NEW row (201 Created)
        String reg2Json = mockMvc.perform(post("/api/classes/" + c.getClassId() + "/register").header("Authorization", verifiedStudentToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long reg2Id = objectMapper.readTree(reg2Json).get("registrationId").asLong();
        assert reg2Id.longValue() != reg1Id.longValue();

        // 6. Student view registrations -> both old (CANCELLED) and new (PENDING) appear, ordered newest first
        mockMvc.perform(get("/api/my/class-registrations").header("Authorization", verifiedStudentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].registrationId").value(reg2Id))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].registrationId").value(reg1Id))
                .andExpect(jsonPath("$[1].status").value("CANCELLED"));

        // 7. Admin confirms reg2
        mockMvc.perform(patch("/api/admin/class-registrations/" + reg2Id + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrationStatusUpdateRequest(RegistrationStatus.CONFIRMED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 8. Student attempts to cancel CONFIRMED registration -> 409 CANCEL_NOT_ALLOWED
        mockMvc.perform(patch("/api/my/class-registrations/" + reg2Id + "/cancel").header("Authorization", verifiedStudentToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANCEL_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value(containsString("Confirmed registrations can only be cancelled by the teacher")));
    }

    @Test
    @DisplayName("Admin transitions: PENDING -> CONFIRMED, REJECTED, CANCELLED; CONFIRMED -> CANCELLED; CANCELLED/REJECTED final")
    void adminRegistrationTransitions_stateMachineEnforced() throws Exception {
        ClassEntity c = new ClassEntity(activeSubject.getSubjectId(), "Combined Maths Paper Class", "Papers", ClassType.PHYSICAL, "Thursdays 5 PM", new BigDecimal("2500.00"), ClassStatus.ACTIVE);
        c = classRepository.save(c);

        // Create a registration for student
        String regJson = mockMvc.perform(post("/api/classes/" + c.getClassId() + "/register").header("Authorization", verifiedStudentToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long regId = objectMapper.readTree(regJson).get("registrationId").asLong();

        // 1. Pending count endpoint
        mockMvc.perform(get("/api/admin/class-registrations/pending-count").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        // 2. Admin filters by student name / email query 'verified'
        mockMvc.perform(get("/api/admin/class-registrations?q=verified").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].student.email").value("verified@example.com"));

        // 3. Confirm registration: PENDING -> CONFIRMED
        mockMvc.perform(patch("/api/admin/class-registrations/" + regId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrationStatusUpdateRequest(RegistrationStatus.CONFIRMED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 4. Invalid transition: CONFIRMED -> REJECTED -> 409 INVALID_TRANSITION
        mockMvc.perform(patch("/api/admin/class-registrations/" + regId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrationStatusUpdateRequest(RegistrationStatus.REJECTED))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));

        // 5. Valid transition: CONFIRMED -> CANCELLED -> 200
        mockMvc.perform(patch("/api/admin/class-registrations/" + regId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrationStatusUpdateRequest(RegistrationStatus.CANCELLED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 6. CANCELLED is final: CANCELLED -> CONFIRMED -> 409 INVALID_TRANSITION
        mockMvc.perform(patch("/api/admin/class-registrations/" + regId + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistrationStatusUpdateRequest(RegistrationStatus.CONFIRMED))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }
}
