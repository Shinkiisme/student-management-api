
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.phuc.studentmanagement.exception.ResourceInUseException;
import com.phuc.studentmanagement.model.Student;
import com.phuc.studentmanagement.service.StudentService;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private StudentService studentService;

        @Test
        void getAllStudents_shouldReturn200_whenStudentsExist() throws Exception {
                List<Student> students = List.of(
                                new Student(1, "Phuc", 20),
                                new Student(2, "Huy", 19));

                when(studentService.getAllStudents(null))
                                .thenReturn(students);

                mockMvc.perform(get("/students"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[0].name").value("Phuc"))
                                .andExpect(jsonPath("$[0].age").value(20))
                                .andExpect(jsonPath("$[1].id").value(2))
                                .andExpect(jsonPath("$[1].name").value("Huy"))
                                .andExpect(jsonPath("$[1].age").value(19));

                verify(studentService).getAllStudents(null);
        }

        @Test
        void getAllStudents_shouldFilterByAge() throws Exception {
                List<Student> students = List.of(
                                new Student(1, "Phuc", 20),
                                new Student(3, "Nam", 20));

                when(studentService.getAllStudents(20))
                                .thenReturn(students);

                mockMvc.perform(get("/students")
                                .param("age", "20"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].age").value(20))
                                .andExpect(jsonPath("$[1].age").value(20));

                verify(studentService).getAllStudents(20);
        }

        @Test
        void getStudentById_shouldReturn200_whenStudentExists() throws Exception {
                Student student = new Student(2, "Phuc", 20);

                when(studentService.getStudentById(2))
                                .thenReturn(Optional.of(student));

                mockMvc.perform(get("/students/2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(2))
                                .andExpect(jsonPath("$.name").value("Phuc"))
                                .andExpect(jsonPath("$.age").value(20));

                verify(studentService).getStudentById(2);
        }

        @Test
        void getStudentById_shouldReturn404_whenStudentDoesNotExist() throws Exception {
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
        void addStudent_shouldReturn201_whenRequestIsValid() throws Exception {
                Student createdStudent = new Student(4, "Nam", 22);

                when(studentService.addStudent(any(Student.class)))
                                .thenReturn(createdStudent);

                mockMvc.perform(post("/students")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Nam",
                                                    "age": 22
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(4))
                                .andExpect(jsonPath("$.name").value("Nam"))
                                .andExpect(jsonPath("$.age").value(22));

                verify(studentService).addStudent(argThat(student -> "Nam".equals(student.getName())
                                && student.getAge() == 22));
        }

        @Test
        void addStudent_shouldReturn400_whenRequestIsInvalid() throws Exception {
                mockMvc.perform(post("/students")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
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
                                .andExpect(jsonPath("$.errors.name")
                                                .value("Name cannot be empty"))
                                .andExpect(jsonPath("$.errors.age")
                                                .value("Age cannot be negative"));

                verifyNoInteractions(studentService);
        }

        @Test
        void addStudent_shouldReturn400_whenAgeIsMissing() throws Exception {
                mockMvc.perform(post("/students")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Phuc"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.title").value("Bad Request"))
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.detail")
                                                .value("Request validation failed"))
                                .andExpect(jsonPath("$.errors.age")
                                                .value("Age is required"));

                verifyNoInteractions(studentService);
        }

        @Test
        void updateStudent_shouldReturn200_whenStudentExists() throws Exception {
                Student updatedStudent = new Student(2, "Huy Nguyen", 22);

                when(studentService.updateStudent(eq(2), any(Student.class)))
                                .thenReturn(Optional.of(updatedStudent));

                mockMvc.perform(put("/students/2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Huy Nguyen",
                                                    "age": 22
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(2))
                                .andExpect(jsonPath("$.name").value("Huy Nguyen"))
                                .andExpect(jsonPath("$.age").value(22));

                verify(studentService).updateStudent(
                                eq(2),
                                argThat(student -> "Huy Nguyen".equals(student.getName())
                                                && student.getAge() == 22));
        }

        @Test
        void updateStudent_shouldReturn404_whenStudentDoesNotExist() throws Exception {
                when(studentService.updateStudent(eq(999), any(Student.class)))
                                .thenReturn(Optional.empty());

                mockMvc.perform(put("/students/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Nam",
                                                    "age": 22
                                                }
                                                """))
                                .andExpect(status().isNotFound())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.title").value("Not Found"))
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.detail")
                                                .value("Student with id 999 not found"));

                verify(studentService).updateStudent(eq(999), any(Student.class));
        }

        @Test
        void updateStudent_shouldReturn400_whenRequestIsInvalid() throws Exception {
                mockMvc.perform(put("/students/2")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "",
                                                    "age": -1
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.title").value("Bad Request"))
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.detail")
                                                .value("Request validation failed"))
                                .andExpect(jsonPath("$.errors.name")
                                                .value("Name cannot be empty"))
                                .andExpect(jsonPath("$.errors.age")
                                                .value("Age cannot be negative"));

                verifyNoInteractions(studentService);
        }

        @Test
        void deleteStudent_shouldReturn204_whenStudentExists() throws Exception {
                when(studentService.deleteStudent(2)).thenReturn(true);

                mockMvc.perform(delete("/students/2"))
                                .andExpect(status().isNoContent())
                                .andExpect(content().string(""));

                verify(studentService).deleteStudent(2);
        }

        @Test
        void deleteStudent_shouldReturn404_whenStudentDoesNotExist() throws Exception {
                when(studentService.deleteStudent(999)).thenReturn(false);

                mockMvc.perform(delete("/students/999"))
                                .andExpect(status().isNotFound())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.title").value("Not Found"))
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.detail")
                                                .value("Student with id 999 not found"));

                verify(studentService).deleteStudent(999);
        }

        @Test
        void deleteStudent_shouldReturn409_whenStudentIsReferenced() throws Exception {
                when(studentService.deleteStudent(1))
                                .thenThrow(new ResourceInUseException(
                                                "Student",
                                                1,
                                                new RuntimeException("Foreign key violation")));

                mockMvc.perform(delete("/students/1"))
                                .andExpect(status().isConflict())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.title").value("Conflict"))
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.detail")
                                                .value("Student with id 1 cannot be deleted because it is in use"));

                verify(studentService).deleteStudent(1);
        }
}