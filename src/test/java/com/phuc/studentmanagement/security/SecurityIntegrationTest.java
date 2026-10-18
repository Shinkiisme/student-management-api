package com.phuc.studentmanagement.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.phuc.studentmanagement.config.PostgresTestConfiguration;
import com.phuc.studentmanagement.model.Student;
import com.phuc.studentmanagement.repository.StudentRepository;
import com.phuc.studentmanagement.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@Transactional
class SecurityIntegrationTest {

        private static final String ADMIN_USERNAME = "security_test_admin";
        private static final String STUDENT_USERNAME = "security_test_student";
        private static final String PASSWORD = "SecurityTestPassword123!";

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private StudentRepository studentRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private JwtDecoder jwtDecoder;

        @Autowired
        private JwtEncoder jwtEncoder;

        private int adminUserId;
        private int ownStudentId;
        private int otherStudentId;

        @BeforeEach
        void setUp() {
                Student ownStudent = studentRepository.insert(
                                new Student("SECURITY_TEST_01", "Security Test Student", 20));
                Student otherStudent = studentRepository.insert(
                                new Student("SECURITY_TEST_02", "Another Test Student", 21));

                ownStudentId = ownStudent.getId();
                otherStudentId = otherStudent.getId();

                String passwordHash = passwordEncoder.encode(PASSWORD);

                jdbcTemplate.update("""
                                INSERT INTO users (username, password_hash, role, student_id, enabled)
                                VALUES (?, ?, 'ADMIN', NULL, TRUE)
                                """, ADMIN_USERNAME, passwordHash);

                jdbcTemplate.update("""
                                INSERT INTO users (username, password_hash, role, student_id, enabled)
                                VALUES (?, ?, 'STUDENT', ?, TRUE)
                                """, STUDENT_USERNAME, passwordHash, ownStudentId);

                adminUserId = userRepository.findByUsername(ADMIN_USERNAME)
                                .orElseThrow()
                                .getId();
        }

        @Test
        void login_shouldReturnSignedJwt_whenCredentialsAreCorrect() throws Exception {
                String token = login(ADMIN_USERNAME, PASSWORD);

                assertNotNull(token);

                Jwt jwt = jwtDecoder.decode(token);
                assertEquals(String.valueOf(adminUserId), jwt.getSubject());
                assertEquals("ADMIN", jwt.getClaimAsString("role"));
                assertEquals("student-management-api", jwt.getClaimAsString("iss"));
        }

        @Test
        void login_shouldReturnUnauthorized_whenPasswordIsIncorrect() throws Exception {
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "username": "security_test_admin",
                                                  "password": "incorrect-password"
                                                }
                                                """))
                                .andExpect(status().isUnauthorized())
                                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
        }

        @Test
        void login_shouldReturnUnauthorized_whenUsernameDoesNotExist() throws Exception {
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "username": "unknown_security_user",
                                                  "password": "SecurityTestPassword123!"
                                                }
                                                """))
                                .andExpect(status().isUnauthorized())
                                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
        }

        @Test
        void getStudents_shouldReturnUnauthorized_withoutJwt() throws Exception {
                mockMvc.perform(get("/students"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(401))
                                .andExpect(jsonPath("$.title").value("Unauthorized"))
                                .andExpect(jsonPath("$.detail").value("Authentication is required"));
        }

        @Test
        void getStudents_shouldReturnOk_withAdminJwt() throws Exception {
                String token = login(ADMIN_USERNAME, PASSWORD);

                mockMvc.perform(get("/students")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void getStudents_shouldReturnForbidden_withStudentJwt() throws Exception {
                String token = login(STUDENT_USERNAME, PASSWORD);

                mockMvc.perform(get("/students")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(403))
                                .andExpect(jsonPath("$.title").value("Forbidden"))
                                .andExpect(jsonPath("$.detail")
                                                .value("You do not have permission to access this resource"));
        }

        @Test
        void addStudent_shouldReturnForbidden_withStudentJwt() throws Exception {
                String token = login(STUDENT_USERNAME, PASSWORD);

                mockMvc.perform(post("/students")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isForbidden())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(403))
                                .andExpect(jsonPath("$.title").value("Forbidden"))
                                .andExpect(jsonPath("$.detail")
                                                .value("You do not have permission to access this resource"));
        }

        @Test
        void addStudent_shouldReachValidation_withAdminJwt() throws Exception {
                String token = login(ADMIN_USERNAME, PASSWORD);

                mockMvc.perform(post("/students")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void getStudentById_shouldReturnOk_whenStudentAccessesOwnRecord() throws Exception {
                String token = login(STUDENT_USERNAME, PASSWORD);

                mockMvc.perform(get("/students/{id}", ownStudentId)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(ownStudentId));
        }

        @Test
        void getStudentById_shouldReturnForbidden_whenStudentAccessesAnotherRecord() throws Exception {
                String token = login(STUDENT_USERNAME, PASSWORD);

                mockMvc.perform(get("/students/{id}", otherStudentId)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(403))
                                .andExpect(jsonPath("$.title").value("Forbidden"))
                                .andExpect(jsonPath("$.detail")
                                                .value("You do not have permission to access this resource"));
        }

        @Test
        void getStudentById_shouldReturnOk_whenAdminAccessesAnotherRecord() throws Exception {
                String token = login(ADMIN_USERNAME, PASSWORD);

                mockMvc.perform(get("/students/{id}", otherStudentId)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(otherStudentId));
        }

        @Test
        void getStudentEnrollments_shouldReturnOk_whenStudentAccessesOwnRecord() throws Exception {
                String token = login(STUDENT_USERNAME, PASSWORD);

                mockMvc.perform(get("/students/{studentId}/enrollments", ownStudentId)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());
        }

        @Test
        void getStudentEnrollments_shouldReturnForbidden_whenStudentAccessesAnotherRecord() throws Exception {
                String token = login(STUDENT_USERNAME, PASSWORD);

                mockMvc.perform(get("/students/{studentId}/enrollments", otherStudentId)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(403))
                                .andExpect(jsonPath("$.title").value("Forbidden"))
                                .andExpect(jsonPath("$.detail")
                                                .value("You do not have permission to access this resource"));
        }

        @Test
        void getCourses_shouldReturnOk_withStudentJwt() throws Exception {
                String token = login(STUDENT_USERNAME, PASSWORD);

                mockMvc.perform(get("/courses")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk());
        }

        @Test
        void getEnrollments_shouldReturnForbidden_withStudentJwt() throws Exception {
                String token = login(STUDENT_USERNAME, PASSWORD);

                mockMvc.perform(get("/enrollments")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(403))
                                .andExpect(jsonPath("$.title").value("Forbidden"))
                                .andExpect(jsonPath("$.detail")
                                                .value("You do not have permission to access this resource"));
        }

        @Test
        void getStudents_shouldReturnUnauthorized_withMalformedJwt() throws Exception {
                mockMvc.perform(get("/students")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
                                                containsString("error=\"invalid_token\"")))
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(401))
                                .andExpect(jsonPath("$.title").value("Unauthorized"))
                                .andExpect(jsonPath("$.detail").value("Authentication is required"));
        }

        @Test
        void getStudents_shouldReturnUnauthorized_withTamperedJwt() throws Exception {
                String token = login(ADMIN_USERNAME, PASSWORD);
                int signatureStart = token.lastIndexOf('.') + 1;
                char original = token.charAt(signatureStart);
                char replacement = original == 'A' ? 'B' : 'A';
                String tamperedToken = token.substring(0, signatureStart)
                                + replacement
                                + token.substring(signatureStart + 1);

                mockMvc.perform(get("/students")
                                .header("Authorization", "Bearer " + tamperedToken))
                                .andExpect(status().isUnauthorized())
                                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
                                                containsString("error=\"invalid_token\"")))
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(401))
                                .andExpect(jsonPath("$.title").value("Unauthorized"))
                                .andExpect(jsonPath("$.detail").value("Authentication is required"));
        }

        @Test
        void getStudents_shouldReturnUnauthorized_withExpiredJwt() throws Exception {
                Instant now = Instant.now();
                JwtClaimsSet claims = JwtClaimsSet.builder()
                                .issuer("student-management-api")
                                .subject(String.valueOf(adminUserId))
                                .issuedAt(now.minusSeconds(120))
                                .expiresAt(now.minusSeconds(60))
                                .claim("role", "ADMIN")
                                .build();
                JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
                String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                                .getTokenValue();

                mockMvc.perform(get("/students")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
                                                containsString("error=\"invalid_token\"")))
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(401))
                                .andExpect(jsonPath("$.title").value("Unauthorized"))
                                .andExpect(jsonPath("$.detail").value("Authentication is required"));
        }

        @Test
        void getStudents_shouldReturnUnauthorized_withInvalidIssuer() throws Exception {
                Instant now = Instant.now();
                JwtClaimsSet claims = JwtClaimsSet.builder()
                                .issuer("another-api")
                                .subject(String.valueOf(adminUserId))
                                .issuedAt(now)
                                .expiresAt(now.plusSeconds(900))
                                .claim("role", "ADMIN")
                                .build();
                JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
                String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                                .getTokenValue();

                mockMvc.perform(get("/students")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
                                                containsString("error=\"invalid_token\"")))
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(401))
                                .andExpect(jsonPath("$.title").value("Unauthorized"))
                                .andExpect(jsonPath("$.detail").value("Authentication is required"));
        }

        @Test
        void unknownEndpoint_shouldReturnForbidden_withAdminJwt() throws Exception {
                String token = login(ADMIN_USERNAME, PASSWORD);

                mockMvc.perform(get("/admin/unconfigured-endpoint")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(403))
                                .andExpect(jsonPath("$.title").value("Forbidden"))
                                .andExpect(jsonPath("$.detail")
                                                .value("You do not have permission to access this resource"));
        }

        private String login(String username, String password) throws Exception {
                String body = """
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password);

                String response = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                                .andExpect(jsonPath("$.expiresIn").value(900))
                                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                return JsonPath.read(response, "$.accessToken");
        }
}