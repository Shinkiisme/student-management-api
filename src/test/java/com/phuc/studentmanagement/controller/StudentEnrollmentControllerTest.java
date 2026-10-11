
package com.phuc.studentmanagement.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.phuc.studentmanagement.exception.ResourceNotFoundException;
import com.phuc.studentmanagement.model.Enrollment;
import com.phuc.studentmanagement.service.EnrollmentService;

@WebMvcTest(StudentEnrollmentController.class)
class StudentEnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @Test
    void getEnrollmentsByStudentId_shouldReturnEnrollments_whenStudentHasEnrollments() throws Exception {
        Enrollment enrollment1 = new Enrollment(1, 1, 2);
        Enrollment enrollment2 = new Enrollment(2, 1, 3);

        when(enrollmentService.getEnrollmentsByStudentId(1))
                .thenReturn(List.of(enrollment1, enrollment2));

        mockMvc.perform(get("/students/1/enrollments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].studentId").value(1))
                .andExpect(jsonPath("$[0].courseId").value(2))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].studentId").value(1))
                .andExpect(jsonPath("$[1].courseId").value(3));

        verify(enrollmentService).getEnrollmentsByStudentId(1);
    }

    @Test
    void getEnrollmentsByStudentId_shouldReturnEmptyList_whenStudentHasNoEnrollments() throws Exception {
        when(enrollmentService.getEnrollmentsByStudentId(1))
                .thenReturn(List.of());

        mockMvc.perform(get("/students/1/enrollments"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(enrollmentService).getEnrollmentsByStudentId(1);
    }

    @Test
    void getEnrollmentsByStudentId_shouldReturnNotFound_whenStudentDoesNotExist() throws Exception {
        when(enrollmentService.getEnrollmentsByStudentId(999))
                .thenThrow(new ResourceNotFoundException("Student", 999));

        mockMvc.perform(get("/students/999/enrollments"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Student with id 999 not found"));

        verify(enrollmentService).getEnrollmentsByStudentId(999);
    }
}
