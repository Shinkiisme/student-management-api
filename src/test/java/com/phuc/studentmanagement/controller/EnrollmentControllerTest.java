
package com.phuc.studentmanagement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.phuc.studentmanagement.exception.EnrollmentAlreadyExistsException;
import com.phuc.studentmanagement.exception.ResourceNotFoundException;
import com.phuc.studentmanagement.model.Enrollment;
import com.phuc.studentmanagement.service.EnrollmentService;

@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @Test
    void getAllEnrollments_shouldReturnAllEnrollments() throws Exception {
        Enrollment enrollment1 = new Enrollment(1, 1, 2);
        Enrollment enrollment2 = new Enrollment(2, 1, 3);

        when(enrollmentService.getAllEnrollments())
                .thenReturn(List.of(enrollment1, enrollment2));

        mockMvc.perform(get("/enrollments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].studentId").value(1))
                .andExpect(jsonPath("$[0].courseId").value(2))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].studentId").value(1))
                .andExpect(jsonPath("$[1].courseId").value(3));

        verify(enrollmentService).getAllEnrollments();
    }

    @Test
    void getAllEnrollments_shouldReturnEmptyList_whenNoEnrollmentsExist() throws Exception {
        when(enrollmentService.getAllEnrollments())
                .thenReturn(List.of());

        mockMvc.perform(get("/enrollments"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(enrollmentService).getAllEnrollments();
    }

    @Test
    void getEnrollmentById_shouldReturnEnrollment_whenEnrollmentExists() throws Exception {
        Enrollment enrollment = new Enrollment(1, 1, 2);

        when(enrollmentService.getEnrollmentById(1))
                .thenReturn(Optional.of(enrollment));

        mockMvc.perform(get("/enrollments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.studentId").value(1))
                .andExpect(jsonPath("$.courseId").value(2));

        verify(enrollmentService).getEnrollmentById(1);
    }

    @Test
    void getEnrollmentById_shouldReturnNotFound_whenEnrollmentDoesNotExist() throws Exception {
        when(enrollmentService.getEnrollmentById(999))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/enrollments/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Enrollment with id 999 not found"));

        verify(enrollmentService).getEnrollmentById(999);
    }

    @Test
    void addEnrollment_shouldReturnCreatedEnrollment_whenRequestIsValid() throws Exception {
        Enrollment createdEnrollment = new Enrollment(13, 1, 2);

        when(enrollmentService.addEnrollment(any(Enrollment.class)))
                .thenReturn(createdEnrollment);

        mockMvc.perform(post("/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "studentId": 1,
                            "courseId": 2
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(13))
                .andExpect(jsonPath("$.studentId").value(1))
                .andExpect(jsonPath("$.courseId").value(2));

        verify(enrollmentService).addEnrollment(argThat(enrollment -> enrollment.getStudentId() == 1
                && enrollment.getCourseId() == 2));
    }

    @Test
    void addEnrollment_shouldReturnBadRequest_whenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.errors.studentId")
                        .value("Student ID is required"))
                .andExpect(jsonPath("$.errors.courseId")
                        .value("Course ID is required"));

        verifyNoInteractions(enrollmentService);
    }

    @Test
    void addEnrollment_shouldReturnBadRequest_whenIdsAreNotPositive() throws Exception {
        mockMvc.perform(post("/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "studentId": -1,
                            "courseId": 0
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.errors.studentId")
                        .value("Student ID must be positive"))
                .andExpect(jsonPath("$.errors.courseId")
                        .value("Course ID must be positive"));

        verifyNoInteractions(enrollmentService);
    }

    @Test
    void addEnrollment_shouldReturnConflict_whenEnrollmentAlreadyExists() throws Exception {
        when(enrollmentService.addEnrollment(any(Enrollment.class)))
                .thenThrow(new EnrollmentAlreadyExistsException(
                        1,
                        2,
                        new DuplicateKeyException("Duplicate enrollment")));

        mockMvc.perform(post("/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "studentId": 1,
                            "courseId": 2
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail")
                        .value("Student with id 1 is already enrolled in course with id 2"));

        verify(enrollmentService).addEnrollment(any(Enrollment.class));
    }

    @Test
    void addEnrollment_shouldReturnNotFound_whenStudentDoesNotExist() throws Exception {
        when(enrollmentService.addEnrollment(any(Enrollment.class)))
                .thenThrow(new ResourceNotFoundException("Student", 999));

        mockMvc.perform(post("/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "studentId": 999,
                            "courseId": 2
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Student with id 999 not found"));

        verify(enrollmentService).addEnrollment(any(Enrollment.class));
    }

    @Test
    void addEnrollment_shouldReturnNotFound_whenCourseDoesNotExist() throws Exception {
        when(enrollmentService.addEnrollment(any(Enrollment.class)))
                .thenThrow(new ResourceNotFoundException("Course", 999));

        mockMvc.perform(post("/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "studentId": 1,
                            "courseId": 999
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Course with id 999 not found"));

        verify(enrollmentService).addEnrollment(any(Enrollment.class));
    }

    @Test
    void deleteEnrollment_shouldReturnNoContent_whenEnrollmentExists() throws Exception {
        when(enrollmentService.deleteEnrollment(1)).thenReturn(true);

        mockMvc.perform(delete("/enrollments/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(enrollmentService).deleteEnrollment(1);
    }

    @Test
    void deleteEnrollment_shouldReturnNotFound_whenEnrollmentDoesNotExist() throws Exception {
        when(enrollmentService.deleteEnrollment(999)).thenReturn(false);

        mockMvc.perform(delete("/enrollments/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Enrollment with id 999 not found"));

        verify(enrollmentService).deleteEnrollment(999);
    }
}
