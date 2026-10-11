package com.phuc.studentmanagement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.phuc.studentmanagement.exception.InvalidPaginationException;
import com.phuc.studentmanagement.exception.ResourceInUseException;
import com.phuc.studentmanagement.exception.StudentCodeAlreadyExistsException;
import com.phuc.studentmanagement.model.Student;
import com.phuc.studentmanagement.pagination.PageRequest;
import com.phuc.studentmanagement.pagination.PageResult;
import com.phuc.studentmanagement.pagination.SortDirection;
import com.phuc.studentmanagement.service.StudentService;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private StudentService studentService;

        @Test
        void getAllStudents_shouldReturnDefaultPage_whenStudentsExist() throws Exception {
                List<Student> students = List.of(
                                new Student(1, "24120001", "Phuc", 20),
                                new Student(2, "24120002", "Huy", 19));
                PageRequest request = new PageRequest(0, 10, "id", SortDirection.ASC);
                PageResult<Student> result = new PageResult<>(students, 0, 10, 2);

                when(studentService.getStudentsPage(request, null)).thenReturn(result);

                mockMvc.perform(get("/students"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(2))
                                .andExpect(jsonPath("$.content[0].id").value(1))
                                .andExpect(jsonPath("$.content[0].studentCode").value("24120001"))
                                .andExpect(jsonPath("$.content[0].name").value("Phuc"))
                                .andExpect(jsonPath("$.content[0].age").value(20))
                                .andExpect(jsonPath("$.content[1].id").value(2))
                                .andExpect(jsonPath("$.content[1].studentCode").value("24120002"))
                                .andExpect(jsonPath("$.content[1].name").value("Huy"))
                                .andExpect(jsonPath("$.content[1].age").value(19))
                                .andExpect(jsonPath("$.page").value(0))
                                .andExpect(jsonPath("$.size").value(10))
                                .andExpect(jsonPath("$.totalElements").value(2))
                                .andExpect(jsonPath("$.totalPages").value(1));

                verify(studentService).getStudentsPage(request, null);
        }

        @Test
        void getAllStudents_shouldReturnEmptyPage_whenNoStudentsExist() throws Exception {
                PageRequest request = new PageRequest(0, 10, "id", SortDirection.ASC);
                PageResult<Student> result = new PageResult<>(List.of(), 0, 10, 0);

                when(studentService.getStudentsPage(request, null)).thenReturn(result);

                mockMvc.perform(get("/students"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(0))
                                .andExpect(jsonPath("$.page").value(0))
                                .andExpect(jsonPath("$.size").value(10))
                                .andExpect(jsonPath("$.totalElements").value(0))
                                .andExpect(jsonPath("$.totalPages").value(0));

                verify(studentService).getStudentsPage(request, null);
        }

        @Test
        void getAllStudents_shouldFilterByAge() throws Exception {
                List<Student> students = List.of(
                                new Student(1, "24120001", "Phuc", 20),
                                new Student(3, "24120003", "Nam", 20));
                PageRequest request = new PageRequest(0, 10, "id", SortDirection.ASC);
                PageResult<Student> result = new PageResult<>(students, 0, 10, 2);

                when(studentService.getStudentsPage(request, 20)).thenReturn(result);

                mockMvc.perform(get("/students").param("age", "20"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(2))
                                .andExpect(jsonPath("$.content[0].studentCode").value("24120001"))
                                .andExpect(jsonPath("$.content[0].age").value(20))
                                .andExpect(jsonPath("$.content[1].studentCode").value("24120003"))
                                .andExpect(jsonPath("$.content[1].age").value(20))
                                .andExpect(jsonPath("$.totalElements").value(2))
                                .andExpect(jsonPath("$.totalPages").value(1));

                verify(studentService).getStudentsPage(request, 20);
        }

        @Test
        void getAllStudents_shouldReturnRequestedPageAndSize() throws Exception {
                PageRequest request = new PageRequest(1, 2, "id", SortDirection.ASC);
                List<Student> students = List.of(
                                new Student(3, "24120003", "Nam", 20),
                                new Student(4, "24120004", "An", 21));
                PageResult<Student> result = new PageResult<>(students, 1, 2, 5);

                when(studentService.getStudentsPage(request, null)).thenReturn(result);

                mockMvc.perform(get("/students")
                                .param("page", "1")
                                .param("size", "2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(2))
                                .andExpect(jsonPath("$.content[0].id").value(3))
                                .andExpect(jsonPath("$.content[1].id").value(4))
                                .andExpect(jsonPath("$.page").value(1))
                                .andExpect(jsonPath("$.size").value(2))
                                .andExpect(jsonPath("$.totalElements").value(5))
                                .andExpect(jsonPath("$.totalPages").value(3));

                verify(studentService).getStudentsPage(request, null);
        }

        @Test
        void getAllStudents_shouldUseRequestedSortFieldAndDirection() throws Exception {
                PageRequest request = new PageRequest(0, 10, "name", SortDirection.DESC);
                PageResult<Student> result = new PageResult<>(
                                List.of(new Student(1, "24120001", "Phuc", 20)), 0, 10, 1);

                when(studentService.getStudentsPage(request, null)).thenReturn(result);

                mockMvc.perform(get("/students")
                                .param("sortBy", "name")
                                .param("direction", "desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].name").value("Phuc"))
                                .andExpect(jsonPath("$.totalElements").value(1));

                verify(studentService).getStudentsPage(request, null);
        }

        @Test
        void getAllStudents_shouldAcceptUppercaseSortDirection() throws Exception {
                PageRequest request = new PageRequest(0, 10, "id", SortDirection.DESC);
                PageResult<Student> result = new PageResult<>(List.of(), 0, 10, 0);

                when(studentService.getStudentsPage(request, null)).thenReturn(result);

                mockMvc.perform(get("/students").param("direction", "DESC"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(0));

                verify(studentService).getStudentsPage(request, null);
        }

        @Test
        void getAllStudents_shouldFilterByAgeAndPaginate() throws Exception {
                PageRequest request = new PageRequest(1, 1, "age", SortDirection.ASC);
                PageResult<Student> result = new PageResult<>(
                                List.of(new Student(3, "24120003", "Nam", 20)), 1, 1, 3);

                when(studentService.getStudentsPage(request, 20)).thenReturn(result);

                mockMvc.perform(get("/students")
                                .param("page", "1")
                                .param("size", "1")
                                .param("sortBy", "age")
                                .param("age", "20"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.content[0].age").value(20))
                                .andExpect(jsonPath("$.page").value(1))
                                .andExpect(jsonPath("$.size").value(1))
                                .andExpect(jsonPath("$.totalElements").value(3))
                                .andExpect(jsonPath("$.totalPages").value(3));

                verify(studentService).getStudentsPage(request, 20);
        }

        @Test
        void getAllStudents_shouldAcceptMaximumPageSize() throws Exception {
                PageRequest request = new PageRequest(0, 100, "id", SortDirection.ASC);
                PageResult<Student> result = new PageResult<>(List.of(), 0, 100, 0);

                when(studentService.getStudentsPage(request, null)).thenReturn(result);

                mockMvc.perform(get("/students").param("size", "100"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.size").value(100));

                verify(studentService).getStudentsPage(request, null);
        }

        @Test
        void getAllStudents_shouldReturnEmptyContent_whenPageIsBeyondLastPage() throws Exception {
                PageRequest request = new PageRequest(9, 2, "id", SortDirection.ASC);
                PageResult<Student> result = new PageResult<>(List.of(), 9, 2, 5);

                when(studentService.getStudentsPage(request, null)).thenReturn(result);

                mockMvc.perform(get("/students")
                                .param("page", "9")
                                .param("size", "2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(0))
                                .andExpect(jsonPath("$.page").value(9))
                                .andExpect(jsonPath("$.totalElements").value(5))
                                .andExpect(jsonPath("$.totalPages").value(3));

                verify(studentService).getStudentsPage(request, null);
        }

        @Test
        void getAllStudents_shouldReturnBadRequest_whenPageIsNegative() throws Exception {
                mockMvc.perform(get("/students").param("page", "-1"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.detail").value("Page must not be negative"));

                verifyNoInteractions(studentService);
        }

        @Test
        void getAllStudents_shouldReturnBadRequest_whenSizeIsZero() throws Exception {
                mockMvc.perform(get("/students").param("size", "0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.detail").value("Size must be between 1 and 100"));

                verifyNoInteractions(studentService);
        }

        @Test
        void getAllStudents_shouldReturnBadRequest_whenSizeExceedsMaximum() throws Exception {
                mockMvc.perform(get("/students").param("size", "101"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.detail").value("Size must be between 1 and 100"));

                verifyNoInteractions(studentService);
        }

        @Test
        void getAllStudents_shouldReturnBadRequest_whenSortFieldIsBlank() throws Exception {
                mockMvc.perform(get("/students").param("sortBy", "   "))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.detail").value("Sort by is required"));

                verifyNoInteractions(studentService);
        }

        @Test
        void getAllStudents_shouldReturnBadRequest_whenSortFieldIsNotAllowed() throws Exception {
                PageRequest request = new PageRequest(0, 10, "password", SortDirection.ASC);

                when(studentService.getStudentsPage(request, null))
                                .thenThrow(new InvalidPaginationException("Invalid sort field: password"));

                mockMvc.perform(get("/students").param("sortBy", "password"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.detail").value("Invalid sort field: password"));

                verify(studentService).getStudentsPage(request, null);
        }

        @Test
        void getAllStudents_shouldReturnBadRequest_whenDirectionIsInvalid() throws Exception {
                mockMvc.perform(get("/students").param("direction", "up"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.detail").value("Direction must be asc or desc"));

                verifyNoInteractions(studentService);
        }

        @Test
        void getAllStudents_shouldReturnBadRequest_whenAgeIsNegative() throws Exception {
                mockMvc.perform(get("/students").param("age", "-1"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.detail").value("Age cannot be negative"));

                verifyNoInteractions(studentService);
        }

        @Test
        void getAllStudents_shouldReturnBadRequest_whenPageIsNotNumeric() throws Exception {
                mockMvc.perform(get("/students").param("page", "abc"))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(studentService);
        }

        @Test
        void getAllStudents_shouldReturnBadRequest_whenAgeIsNotNumeric() throws Exception {
                mockMvc.perform(get("/students").param("age", "abc"))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(studentService);
        }

        @Test
        void getStudentById_shouldReturnStudent_whenStudentExists() throws Exception {
                Student student = new Student(2, "24120002", "Huy", 19);
                when(studentService.getStudentById(2))
                                .thenReturn(Optional.of(student));
                mockMvc.perform(get("/students/2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(2))
                                .andExpect(jsonPath("$.studentCode").value("24120002"))
                                .andExpect(jsonPath("$.name").value("Huy"))
                                .andExpect(jsonPath("$.age").value(19));
                verify(studentService).getStudentById(2);
        }

        @Test
        void getStudentById_shouldReturnNotFound_whenStudentDoesNotExist() throws Exception {
                when(studentService.getStudentById(999))
                                .thenReturn(Optional.empty());
                mockMvc.perform(get("/students/999"))
                                .andExpect(status().isNotFound())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.title").value("Not Found"))
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.detail")
                                                .value("Student with id 999 not found"));
                verify(studentService).getStudentById(999);
        }

        @Test
        void addStudent_shouldReturnCreatedStudent_whenRequestIsValid() throws Exception {
                Student createdStudent = new Student(4, "24120004", "Nam", 22);
                when(studentService.addStudent(any(Student.class)))
                                .thenReturn(createdStudent);
                mockMvc.perform(post("/students")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "studentCode": "24120004",
                                                  "name": "Nam",
                                                  "age": 22
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(4))
                                .andExpect(jsonPath("$.studentCode").value("24120004"))
                                .andExpect(jsonPath("$.name").value("Nam"))
                                .andExpect(jsonPath("$.age").value(22));
                verify(studentService).addStudent(argThat(student -> "24120004".equals(student.getStudentCode())
                                && "Nam".equals(student.getName())
                                && student.getAge() == 22));
        }

        @Test
        void addStudent_shouldReturnBadRequest_whenFieldsAreInvalid() throws Exception {
                mockMvc.perform(post("/students")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "studentCode": "",
                                                  "name": "",
                                                  "age": -5
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.title").value("Bad Request"))
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.detail")
                                                .value("Request validation failed"))
                                .andExpect(jsonPath("$.errors.studentCode")
                                                .value("Student code is required"))
                                .andExpect(jsonPath("$.errors.name")
                                                .value("Name is required"))
                                .andExpect(jsonPath("$.errors.age")
                                                .value("Age cannot be negative"));
                verifyNoInteractions(studentService);
        }

        @Test
        void addStudent_shouldReturnBadRequest_whenAgeIsMissing() throws Exception {
                mockMvc.perform(post("/students")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "studentCode": "24120001",
                                                  "name": "Phuc"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.errors.age")
                                                .value("Age is required"));
                verifyNoInteractions(studentService);
        }

        @Test
        void addStudent_shouldReturnBadRequest_whenStudentCodeIsMissing() throws Exception {
                mockMvc.perform(post("/students")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "name": "Phuc",
                                                  "age": 20
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.errors.studentCode")
                                                .value("Student code is required"));
                verifyNoInteractions(studentService);
        }

        @Test
        void addStudent_shouldReturnBadRequest_whenStudentCodeIsTooLong() throws Exception {
                String studentCode = "1".repeat(21);
                mockMvc.perform(post("/students")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "studentCode": "%s",
                                                  "name": "Phuc",
                                                  "age": 20
                                                }
                                                """.formatted(studentCode)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.errors.studentCode")
                                                .value("Student code must not exceed 20 characters"));
                verifyNoInteractions(studentService);
        }

        @Test
        void addStudent_shouldReturnConflict_whenStudentCodeAlreadyExists() throws Exception {
                when(studentService.addStudent(any(Student.class)))
                                .thenThrow(new StudentCodeAlreadyExistsException(
                                                "24120001",
                                                new DuplicateKeyException("Duplicate student code")));
                mockMvc.perform(post("/students")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "studentCode": "24120001",
                                                  "name": "Phuc",
                                                  "age": 20
                                                }
                                                """))
                                .andExpect(status().isConflict())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.title").value("Conflict"))
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.detail")
                                                .value("Student code 24120001 already exists"));
                verify(studentService).addStudent(any(Student.class));
        }

        @Test
        void updateStudent_shouldReturnUpdatedStudent_whenStudentExists() throws Exception {
                Student updatedStudent = new Student(2, "24120002", "Huy Nguyen", 22);
                when(studentService.updateStudent(eq(2), any(Student.class)))
                                .thenReturn(Optional.of(updatedStudent));
                mockMvc.perform(put("/students/2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "studentCode": "24120002",
                                                  "name": "Huy Nguyen",
                                                  "age": 22
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(2))
                                .andExpect(jsonPath("$.studentCode").value("24120002"))
                                .andExpect(jsonPath("$.name").value("Huy Nguyen"))
                                .andExpect(jsonPath("$.age").value(22));
                verify(studentService).updateStudent(
                                eq(2),
                                argThat(student -> "24120002".equals(student.getStudentCode())
                                                && "Huy Nguyen".equals(student.getName())
                                                && student.getAge() == 22));
        }

        @Test
        void updateStudent_shouldReturnNotFound_whenStudentDoesNotExist() throws Exception {
                when(studentService.updateStudent(eq(999), any(Student.class)))
                                .thenReturn(Optional.empty());
                mockMvc.perform(put("/students/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "studentCode": "24120003",
                                                  "name": "Nam",
                                                  "age": 22
                                                }
                                                """))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.title").value("Not Found"))
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.detail")
                                                .value("Student with id 999 not found"));
                verify(studentService).updateStudent(eq(999), any(Student.class));
        }

        @Test
        void updateStudent_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
                mockMvc.perform(put("/students/2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "studentCode": "",
                                                  "name": "",
                                                  "age": -1
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.detail")
                                                .value("Request validation failed"))
                                .andExpect(jsonPath("$.errors.studentCode")
                                                .value("Student code is required"))
                                .andExpect(jsonPath("$.errors.name")
                                                .value("Name is required"))
                                .andExpect(jsonPath("$.errors.age")
                                                .value("Age cannot be negative"));
                verifyNoInteractions(studentService);
        }

        @Test
        void updateStudent_shouldReturnConflict_whenStudentCodeAlreadyExists() throws Exception {
                when(studentService.updateStudent(eq(2), any(Student.class)))
                                .thenThrow(new StudentCodeAlreadyExistsException(
                                                "24120001",
                                                new DuplicateKeyException("Duplicate student code")));
                mockMvc.perform(put("/students/2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "studentCode": "24120001",
                                                  "name": "Huy",
                                                  "age": 20
                                                }
                                                """))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.title").value("Conflict"))
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.detail")
                                                .value("Student code 24120001 already exists"));
                verify(studentService).updateStudent(eq(2), any(Student.class));
        }

        @Test
        void deleteStudent_shouldReturnNoContent_whenStudentExists() throws Exception {
                when(studentService.deleteStudent(2)).thenReturn(true);
                mockMvc.perform(delete("/students/2"))
                                .andExpect(status().isNoContent())
                                .andExpect(content().string(""));
                verify(studentService).deleteStudent(2);
        }

        @Test
        void deleteStudent_shouldReturnNotFound_whenStudentDoesNotExist() throws Exception {
                when(studentService.deleteStudent(999)).thenReturn(false);
                mockMvc.perform(delete("/students/999"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.title").value("Not Found"))
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.detail")
                                                .value("Student with id 999 not found"));
                verify(studentService).deleteStudent(999);
        }

        @Test
        void deleteStudent_shouldReturnConflict_whenStudentIsReferenced() throws Exception {
                when(studentService.deleteStudent(1))
                                .thenThrow(new ResourceInUseException(
                                                "Student",
                                                1,
                                                new RuntimeException("Foreign key violation")));
                mockMvc.perform(delete("/students/1"))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.title").value("Conflict"))
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.detail")
                                                .value("Student with id 1 cannot be deleted because it is in use"));
                verify(studentService).deleteStudent(1);
        }
}