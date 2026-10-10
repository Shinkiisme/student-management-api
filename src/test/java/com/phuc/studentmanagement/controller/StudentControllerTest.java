
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

import com.phuc.studentmanagement.exception.ResourceInUseException;
import com.phuc.studentmanagement.exception.StudentCodeAlreadyExistsException;
import com.phuc.studentmanagement.model.Student;
import com.phuc.studentmanagement.service.StudentService;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private StudentService studentService;

        @Test
        void getAllStudents_shouldReturnAllStudents_whenStudentsExist() throws Exception {
                List<Student> students = List.of(
                                new Student(1, "24120001", "Phuc", 20),
                                new Student(2, "24120002", "Huy", 19));

                when(studentService.getAllStudents(null)).thenReturn(students);

                mockMvc.perform(get("/students"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[0].studentCode").value("24120001"))
                                .andExpect(jsonPath("$[0].name").value("Phuc"))
                                .andExpect(jsonPath("$[0].age").value(20))
                                .andExpect(jsonPath("$[1].id").value(2))
                                .andExpect(jsonPath("$[1].studentCode").value("24120002"))
                                .andExpect(jsonPath("$[1].name").value("Huy"))
                                .andExpect(jsonPath("$[1].age").value(19));

                verify(studentService).getAllStudents(null);
        }

        @Test
        void getAllStudents_shouldReturnEmptyList_whenNoStudentsExist() throws Exception {
                when(studentService.getAllStudents(null)).thenReturn(List.of());

                mockMvc.perform(get("/students"))
                                .andExpect(status().isOk())
                                .andExpect(content().json("[]"));

                verify(studentService).getAllStudents(null);
        }

        @Test
        void getAllStudents_shouldFilterByAge() throws Exception {
                List<Student> students = List.of(
                                new Student(1, "24120001", "Phuc", 20),
                                new Student(3, "24120003", "Nam", 20));

                when(studentService.getAllStudents(20)).thenReturn(students);

                mockMvc.perform(get("/students").param("age", "20"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].studentCode").value("24120001"))
                                .andExpect(jsonPath("$[0].age").value(20))
                                .andExpect(jsonPath("$[1].studentCode").value("24120003"))
                                .andExpect(jsonPath("$[1].age").value(20));

                verify(studentService).getAllStudents(20);
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
                                                .value("Student code cannot be empty"))
                                .andExpect(jsonPath("$.errors.name")
                                                .value("Name cannot be empty"))
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
                                                .value("Student code cannot be empty"));

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
                                                .value("Student code cannot be empty"))
                                .andExpect(jsonPath("$.errors.name")
                                                .value("Name cannot be empty"))
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
