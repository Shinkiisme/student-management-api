
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

import com.phuc.studentmanagement.exception.CourseCodeAlreadyExistsException;
import com.phuc.studentmanagement.exception.ResourceInUseException;
import com.phuc.studentmanagement.model.Course;
import com.phuc.studentmanagement.service.CourseService;

@WebMvcTest(CourseController.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @Test
    void getAllCourses_shouldReturnAllCourses() throws Exception {
        Course course1 = new Course(1, "CS101", "Java Programming", 4);
        Course course2 = new Course(2, "CS102", "Data Structures", 3);

        when(courseService.getAllCourses())
                .thenReturn(List.of(course1, course2));

        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("CS101"))
                .andExpect(jsonPath("$[0].name").value("Java Programming"))
                .andExpect(jsonPath("$[0].credits").value(4))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].code").value("CS102"))
                .andExpect(jsonPath("$[1].name").value("Data Structures"))
                .andExpect(jsonPath("$[1].credits").value(3));

        verify(courseService).getAllCourses();
    }

    @Test
    void getAllCourses_shouldReturnEmptyList_whenNoCoursesExist() throws Exception {
        when(courseService.getAllCourses()).thenReturn(List.of());

        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(courseService).getAllCourses();
    }

    @Test
    void getCourseById_shouldReturnCourse_whenCourseExists() throws Exception {
        Course course = new Course(1, "CS101", "Java Programming", 4);

        when(courseService.getCourseById(1))
                .thenReturn(Optional.of(course));

        mockMvc.perform(get("/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("CS101"))
                .andExpect(jsonPath("$.name").value("Java Programming"))
                .andExpect(jsonPath("$.credits").value(4));

        verify(courseService).getCourseById(1);
    }

    @Test
    void getCourseById_shouldReturnNotFound_whenCourseDoesNotExist() throws Exception {
        when(courseService.getCourseById(999))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/courses/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Course with id 999 not found"));

        verify(courseService).getCourseById(999);
    }

    @Test
    void addCourse_shouldReturnCreatedCourse_whenRequestIsValid() throws Exception {
        Course createdCourse = new Course(1, "CS101", "Java Programming", 4);

        when(courseService.addCourse(any(Course.class)))
                .thenReturn(createdCourse);

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "code": "CS101",
                            "name": "Java Programming",
                            "credits": 4
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("CS101"))
                .andExpect(jsonPath("$.name").value("Java Programming"))
                .andExpect(jsonPath("$.credits").value(4));

        verify(courseService).addCourse(argThat(course -> course.getCode().equals("CS101")
                && course.getName().equals("Java Programming")
                && course.getCredits() == 4));
    }

    @Test
    void addCourse_shouldReturnBadRequest_whenFieldsAreInvalid() throws Exception {
        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "code": "",
                            "name": "",
                            "credits": 0
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.code").value("Code cannot be empty"))
                .andExpect(jsonPath("$.errors.name").value("Name cannot be empty"))
                .andExpect(jsonPath("$.errors.credits").value("Credits must be greater than 0"));

        verifyNoInteractions(courseService);
    }

    @Test
    void addCourse_shouldReturnBadRequest_whenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.code").value("Code cannot be empty"))
                .andExpect(jsonPath("$.errors.name").value("Name cannot be empty"))
                .andExpect(jsonPath("$.errors.credits").value("Credits cannot be empty"));

        verifyNoInteractions(courseService);
    }

    @Test
    void addCourse_shouldReturnBadRequest_whenCodeIsTooLong() throws Exception {
        String code = "A".repeat(21);

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "code": "%s",
                            "name": "Java Programming",
                            "credits": 4
                        }
                        """.formatted(code)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.code")
                        .value("Code must not exceed 20 characters"));

        verifyNoInteractions(courseService);
    }

    @Test
    void addCourse_shouldReturnBadRequest_whenNameIsTooLong() throws Exception {
        String name = "A".repeat(101);

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "code": "CS101",
                            "name": "%s",
                            "credits": 4
                        }
                        """.formatted(name)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name")
                        .value("Name must not exceed 100 characters"));

        verifyNoInteractions(courseService);
    }

    @Test
    void addCourse_shouldReturnConflict_whenCodeAlreadyExists() throws Exception {
        when(courseService.addCourse(any(Course.class)))
                .thenThrow(new CourseCodeAlreadyExistsException(
                        "CS101",
                        new DuplicateKeyException("Duplicate course code")));

        mockMvc.perform(post("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "code": "CS101",
                            "name": "Java Programming",
                            "credits": 4
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail")
                        .value("Course code CS101 already exists"));

        verify(courseService).addCourse(any(Course.class));
    }

    @Test
    void updateCourse_shouldReturnUpdatedCourse_whenCourseExists() throws Exception {
        Course updatedCourse = new Course(1, "CS101", "Advanced Java", 4);

        when(courseService.updateCourse(eq(1), any(Course.class)))
                .thenReturn(Optional.of(updatedCourse));

        mockMvc.perform(put("/courses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "code": "CS101",
                            "name": "Advanced Java",
                            "credits": 4
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("CS101"))
                .andExpect(jsonPath("$.name").value("Advanced Java"))
                .andExpect(jsonPath("$.credits").value(4));

        verify(courseService).updateCourse(eq(1), argThat(course -> course.getCode().equals("CS101")
                && course.getName().equals("Advanced Java")
                && course.getCredits() == 4));
    }

    @Test
    void updateCourse_shouldReturnNotFound_whenCourseDoesNotExist() throws Exception {
        when(courseService.updateCourse(eq(999), any(Course.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/courses/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "code": "CS101",
                            "name": "Java Programming",
                            "credits": 4
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Course with id 999 not found"));

        verify(courseService).updateCourse(eq(999), any(Course.class));
    }

    @Test
    void updateCourse_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        mockMvc.perform(put("/courses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "code": "",
                            "name": "",
                            "credits": -1
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.code").value("Code cannot be empty"))
                .andExpect(jsonPath("$.errors.name").value("Name cannot be empty"))
                .andExpect(jsonPath("$.errors.credits")
                        .value("Credits must be greater than 0"));

        verifyNoInteractions(courseService);
    }

    @Test
    void updateCourse_shouldReturnConflict_whenCodeAlreadyExists() throws Exception {
        when(courseService.updateCourse(eq(2), any(Course.class)))
                .thenThrow(new CourseCodeAlreadyExistsException(
                        "CS101",
                        new DuplicateKeyException("Duplicate course code")));

        mockMvc.perform(put("/courses/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "code": "CS101",
                            "name": "Data Structures",
                            "credits": 4
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail")
                        .value("Course code CS101 already exists"));

        verify(courseService).updateCourse(eq(2), any(Course.class));
    }

    @Test
    void deleteCourse_shouldReturnNoContent_whenCourseExists() throws Exception {
        when(courseService.deleteCourse(1)).thenReturn(true);

        mockMvc.perform(delete("/courses/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(courseService).deleteCourse(1);
    }

    @Test
    void deleteCourse_shouldReturnNotFound_whenCourseDoesNotExist() throws Exception {
        when(courseService.deleteCourse(999)).thenReturn(false);

        mockMvc.perform(delete("/courses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Course with id 999 not found"));

        verify(courseService).deleteCourse(999);
    }

    @Test
    void deleteCourse_shouldReturnConflict_whenCourseIsReferenced() throws Exception {
        when(courseService.deleteCourse(1))
                .thenThrow(new ResourceInUseException(
                        "Course",
                        1,
                        new RuntimeException("Foreign key violation")));

        mockMvc.perform(delete("/courses/1"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail")
                        .value("Course with id 1 cannot be deleted because it is in use"));

        verify(courseService).deleteCourse(1);
    }
}