package com.openclassrooms.etudiant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.dto.StudentCreateDTO;
import com.openclassrooms.etudiant.dto.StudentUpdateDTO;
import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.StudentRepository;
import com.openclassrooms.etudiant.repository.UserRepository;
import com.openclassrooms.etudiant.service.JwtService;
import com.openclassrooms.etudiant.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class StudentControllerTest {

    private static final String STUDENTS_URL = "/api/students";
    private static final String STUDENT_BY_ID_URL = "/api/students/{id}";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String EMAIL = "john.doe@email.com";
    private static final String UPDATED_FIRST_NAME = "Jane";
    private static final String UPDATED_LAST_NAME = "Smith";
    private static final String UPDATED_EMAIL = "jane.smith@email.com";
    private static final String LOGIN = "librarian";
    private static final String PASSWORD = "password";
    private static final String AUTHORIZATION = "Authorization";

    @Container
    static MySQLContainer mySQLContainer = new MySQLContainer("mysql:8.0.36");

    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mySQLContainer.getJdbcUrl());
        registry.add("spring.datasource.username", () -> mySQLContainer.getUsername());
        registry.add("spring.datasource.password", () -> mySQLContainer.getPassword());
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("application.security.jwt.secret-key", () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        registry.add("application.security.jwt.expiration", () -> "3600000");
    }

    @AfterEach
    public void afterEach() {
        studentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void createStudentWithoutAuthentication() throws Exception {
        // GIVEN
        StudentCreateDTO studentCreateDTO = getStudentCreateDTO(EMAIL);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post(STUDENTS_URL)
                        .content(objectMapper.writeValueAsString(studentCreateDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void createStudentWithoutRequiredData() throws Exception {
        // GIVEN
        StudentCreateDTO studentCreateDTO = new StudentCreateDTO();

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post(STUDENTS_URL)
                        .header(AUTHORIZATION, getBearerToken())
                        .content(objectMapper.writeValueAsString(studentCreateDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void createAlreadyExistStudent() throws Exception {
        // GIVEN
        studentRepository.save(getStudent(EMAIL));
        StudentCreateDTO studentCreateDTO = getStudentCreateDTO(EMAIL);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post(STUDENTS_URL)
                        .header(AUTHORIZATION, getBearerToken())
                        .content(objectMapper.writeValueAsString(studentCreateDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void createStudentSuccessful() throws Exception {
        // GIVEN
        StudentCreateDTO studentCreateDTO = getStudentCreateDTO(EMAIL);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post(STUDENTS_URL)
                        .header(AUTHORIZATION, getBearerToken())
                        .content(objectMapper.writeValueAsString(studentCreateDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").isNumber())
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value(FIRST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value(LAST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(EMAIL));
    }

    @Test
    public void findAllStudentsSuccessful() throws Exception {
        // GIVEN
        Student savedStudent = studentRepository.save(getStudent(EMAIL));

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(STUDENTS_URL)
                        .header(AUTHORIZATION, getBearerToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(savedStudent.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].firstName").value(FIRST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].lastName").value(LAST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].email").value(EMAIL));
    }

    @Test
    public void findStudentByIdSuccessful() throws Exception {
        // GIVEN
        Student savedStudent = studentRepository.save(getStudent(EMAIL));

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(STUDENT_BY_ID_URL, savedStudent.getId())
                        .header(AUTHORIZATION, getBearerToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(savedStudent.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value(FIRST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value(LAST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(EMAIL));
    }

    @Test
    public void findUnknownStudentById() throws Exception {
        // GIVEN
        Long unknownStudentId = 1L;

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(STUDENT_BY_ID_URL, unknownStudentId)
                        .header(AUTHORIZATION, getBearerToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void updateStudentSuccessful() throws Exception {
        // GIVEN
        Student savedStudent = studentRepository.save(getStudent(EMAIL));
        StudentUpdateDTO studentUpdateDTO = getStudentUpdateDTO(UPDATED_EMAIL);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.put(STUDENT_BY_ID_URL, savedStudent.getId())
                        .header(AUTHORIZATION, getBearerToken())
                        .content(objectMapper.writeValueAsString(studentUpdateDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(savedStudent.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value(UPDATED_FIRST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value(UPDATED_LAST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(UPDATED_EMAIL));

        // THEN
        Student updatedStudent = studentRepository.findById(savedStudent.getId()).orElseThrow();
        assertThat(updatedStudent.getFirstName()).isEqualTo(UPDATED_FIRST_NAME);
        assertThat(updatedStudent.getLastName()).isEqualTo(UPDATED_LAST_NAME);
        assertThat(updatedStudent.getEmail()).isEqualTo(UPDATED_EMAIL);
    }

    @Test
    public void updateStudentWithoutRequiredData() throws Exception {
        // GIVEN
        Student savedStudent = studentRepository.save(getStudent(EMAIL));
        StudentUpdateDTO studentUpdateDTO = new StudentUpdateDTO();

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.put(STUDENT_BY_ID_URL, savedStudent.getId())
                        .header(AUTHORIZATION, getBearerToken())
                        .content(objectMapper.writeValueAsString(studentUpdateDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void updateStudentWithExistingEmail() throws Exception {
        // GIVEN
        Student savedStudent = studentRepository.save(getStudent(EMAIL));
        Student otherStudent = studentRepository.save(getStudent(UPDATED_EMAIL));
        StudentUpdateDTO studentUpdateDTO = getStudentUpdateDTO(otherStudent.getEmail());

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.put(STUDENT_BY_ID_URL, savedStudent.getId())
                        .header(AUTHORIZATION, getBearerToken())
                        .content(objectMapper.writeValueAsString(studentUpdateDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void deleteStudentSuccessful() throws Exception {
        // GIVEN
        Student savedStudent = studentRepository.save(getStudent(EMAIL));

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.delete(STUDENT_BY_ID_URL, savedStudent.getId())
                        .header(AUTHORIZATION, getBearerToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        // THEN
        assertThat(studentRepository.existsById(savedStudent.getId())).isFalse();
    }

    @Test
    public void deleteUnknownStudent() throws Exception {
        // GIVEN
        Long unknownStudentId = 1L;

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.delete(STUDENT_BY_ID_URL, unknownStudentId)
                        .header(AUTHORIZATION, getBearerToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    private StudentCreateDTO getStudentCreateDTO(String email) {
        StudentCreateDTO studentCreateDTO = new StudentCreateDTO();
        studentCreateDTO.setFirstName(FIRST_NAME);
        studentCreateDTO.setLastName(LAST_NAME);
        studentCreateDTO.setEmail(email);
        return studentCreateDTO;
    }

    private StudentUpdateDTO getStudentUpdateDTO(String email) {
        StudentUpdateDTO studentUpdateDTO = new StudentUpdateDTO();
        studentUpdateDTO.setFirstName(UPDATED_FIRST_NAME);
        studentUpdateDTO.setLastName(UPDATED_LAST_NAME);
        studentUpdateDTO.setEmail(email);
        return studentUpdateDTO;
    }

    private Student getStudent(String email) {
        Student student = new Student();
        student.setFirstName(FIRST_NAME);
        student.setLastName(LAST_NAME);
        student.setEmail(email);
        return student;
    }

    private User getUser() {
        User user = new User();
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setLogin(LOGIN);
        user.setPassword(PASSWORD);
        return user;
    }

    private String getBearerToken() {
        if (userRepository.findByLogin(LOGIN).isEmpty()) {
            userService.register(getUser());
        }
        User user = userRepository.findByLogin(LOGIN).orElseThrow();
        return "Bearer " + jwtService.generateToken(user);
    }
}
