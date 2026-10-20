package com.phuc.studentmanagement.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.phuc.studentmanagement.exception.InvalidPaginationException;
import com.phuc.studentmanagement.exception.ResourceNotFoundException;
import com.phuc.studentmanagement.model.Enrollment;
import com.phuc.studentmanagement.pagination.PageRequest;
import com.phuc.studentmanagement.pagination.PageResult;
import com.phuc.studentmanagement.pagination.SortDirection;
import com.phuc.studentmanagement.service.EnrollmentService;

@WebMvcTest(StudentEnrollmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMIN")
class StudentEnrollmentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private EnrollmentService enrollmentService;

        @Test
        void getEnrollmentsByStudentId_shouldReturnEnrollments_whenStudentHasEnrollments() throws Exception {
                Enrollment enrollment1 = new Enrollment(1, 1, 2);
                Enrollment enrollment2 = new Enrollment(2, 1, 3);
                PageRequest request = new PageRequest(0, 10, "id", SortDirection.ASC);

                when(enrollmentService.getEnrollmentsPage(request, 1))
                                .thenReturn(new PageResult<>(List.of(enrollment1, enrollment2), 0, 10, 2));

                mockMvc.perform(get("/students/1/enrollments"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(2))
                                .andExpect(jsonPath("$.content[0].id").value(1))
                                .andExpect(jsonPath("$.content[0].studentId").value(1))
                                .andExpect(jsonPath("$.content[0].courseId").value(2))
                                .andExpect(jsonPath("$.content[1].id").value(2))
                                .andExpect(jsonPath("$.content[1].studentId").value(1))
                                .andExpect(jsonPath("$.content[1].courseId").value(3))
                                .andExpect(jsonPath("$.page").value(0))
                                .andExpect(jsonPath("$.size").value(10))
                                .andExpect(jsonPath("$.totalElements").value(2))
                                .andExpect(jsonPath("$.totalPages").value(1));

                verify(enrollmentService).getEnrollmentsPage(request, 1);
        }

        @Test
        void getEnrollmentsByStudentId_shouldReturnEmptyList_whenStudentHasNoEnrollments() throws Exception {
                PageRequest request = new PageRequest(0, 10, "id", SortDirection.ASC);

                when(enrollmentService.getEnrollmentsPage(request, 1))
                                .thenReturn(new PageResult<>(List.of(), 0, 10, 0));

                mockMvc.perform(get("/students/1/enrollments"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(0))
                                .andExpect(jsonPath("$.page").value(0))
                                .andExpect(jsonPath("$.size").value(10))
                                .andExpect(jsonPath("$.totalElements").value(0));

                verify(enrollmentService).getEnrollmentsPage(request, 1);
        }

        @Test
        void getEnrollmentsByStudentId_shouldReturnNotFound_whenStudentDoesNotExist() throws Exception {
                PageRequest request = new PageRequest(0, 10, "id", SortDirection.ASC);

                when(enrollmentService.getEnrollmentsPage(request, 999))
                                .thenThrow(new ResourceNotFoundException("Student", 999));

                mockMvc.perform(get("/students/999/enrollments"))
                                .andExpect(status().isNotFound())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.title").value("Not Found"))
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.detail")
                                                .value("Student with id 999 not found"));

                verify(enrollmentService).getEnrollmentsPage(request, 999);
        }

        @Test
        void getEnrollmentsByStudentId_shouldApplyCustomPagination() throws Exception {
                PageRequest request = new PageRequest(2, 3, "id", SortDirection.ASC);
                Enrollment enrollment = new Enrollment(21, 5, 7);

                when(enrollmentService.getEnrollmentsPage(request, 5))
                                .thenReturn(new PageResult<>(List.of(enrollment), 2, 3, 8));

                mockMvc.perform(get("/students/5/enrollments")
                                .param("page", "2")
                                .param("size", "3"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.content[0].id").value(21))
                                .andExpect(jsonPath("$.content[0].studentId").value(5))
                                .andExpect(jsonPath("$.page").value(2))
                                .andExpect(jsonPath("$.size").value(3))
                                .andExpect(jsonPath("$.totalElements").value(8))
                                .andExpect(jsonPath("$.totalPages").value(3));

                verify(enrollmentService).getEnrollmentsPage(request, 5);
        }

        @Test
        void getEnrollmentsByStudentId_shouldPassCourseIdDescendingSortToService() throws Exception {
                PageRequest request = new PageRequest(0, 10, "courseId", SortDirection.DESC);

                when(enrollmentService.getEnrollmentsPage(request, 1))
                                .thenReturn(new PageResult<>(List.of(), 0, 10, 0));

                mockMvc.perform(get("/students/1/enrollments")
                                .param("sortBy", "courseId")
                                .param("direction", "desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray());

                verify(enrollmentService).getEnrollmentsPage(request, 1);
        }

        @Test
        void getEnrollmentsByStudentId_shouldPassStudentIdSortToService() throws Exception {
                PageRequest request = new PageRequest(0, 10, "studentId", SortDirection.ASC);

                when(enrollmentService.getEnrollmentsPage(request, 1))
                                .thenReturn(new PageResult<>(List.of(), 0, 10, 0));

                mockMvc.perform(get("/students/1/enrollments")
                                .param("sortBy", "studentId"))
                                .andExpect(status().isOk());

                verify(enrollmentService).getEnrollmentsPage(request, 1);
        }

        @Test
        void getEnrollmentsByStudentId_shouldAcceptUppercaseDirection() throws Exception {
                PageRequest request = new PageRequest(0, 10, "id", SortDirection.DESC);

                when(enrollmentService.getEnrollmentsPage(request, 1))
                                .thenReturn(new PageResult<>(List.of(), 0, 10, 0));

                mockMvc.perform(get("/students/1/enrollments")
                                .param("direction", "DESC"))
                                .andExpect(status().isOk());

                verify(enrollmentService).getEnrollmentsPage(request, 1);
        }

        @Test
        void getEnrollmentsByStudentId_shouldReturnBadRequest_whenPageIsNegative() throws Exception {
                mockMvc.perform(get("/students/1/enrollments")
                                .param("page", "-1"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.detail").value("Page must not be negative"));

                verifyNoInteractions(enrollmentService);
        }

        @Test
        void getEnrollmentsByStudentId_shouldReturnBadRequest_whenSizeIsZero() throws Exception {
                mockMvc.perform(get("/students/1/enrollments")
                                .param("size", "0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.detail").value("Size must be between 1 and 100"));

                verifyNoInteractions(enrollmentService);
        }

        @Test
        void getEnrollmentsByStudentId_shouldReturnBadRequest_whenSizeExceedsMaximum() throws Exception {
                mockMvc.perform(get("/students/1/enrollments")
                                .param("size", "101"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.detail").value("Size must be between 1 and 100"));

                verifyNoInteractions(enrollmentService);
        }

        @Test
        void getEnrollmentsByStudentId_shouldReturnBadRequest_whenDirectionIsInvalid() throws Exception {
                mockMvc.perform(get("/students/1/enrollments")
                                .param("direction", "invalid"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.detail").value("Direction must be asc or desc"));

                verifyNoInteractions(enrollmentService);
        }

        @Test
        void getEnrollmentsByStudentId_shouldReturnBadRequest_whenSortByIsBlank() throws Exception {
                mockMvc.perform(get("/students/1/enrollments")
                                .param("sortBy", " "))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.detail").value("Sort by is required"));

                verifyNoInteractions(enrollmentService);
        }

        @Test
        void getEnrollmentsByStudentId_shouldReturnBadRequest_whenSortByIsInvalid() throws Exception {
                PageRequest request = new PageRequest(0, 10, "invalid", SortDirection.ASC);

                when(enrollmentService.getEnrollmentsPage(request, 1))
                                .thenThrow(new InvalidPaginationException("Invalid sort field: invalid"));

                mockMvc.perform(get("/students/1/enrollments")
                                .param("sortBy", "invalid"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.detail").value("Invalid sort field: invalid"));

                verify(enrollmentService).getEnrollmentsPage(request, 1);
        }

        @Test
        void getEnrollmentsByStudentId_shouldReturnBadRequest_whenStudentIdIsNotANumber() throws Exception {
                mockMvc.perform(get("/students/invalid/enrollments"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.detail").value("Invalid request parameter"));

                verifyNoInteractions(enrollmentService);
        }
}