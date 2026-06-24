package com.openclassrooms.etudiant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.dto.UserUpdateDTO;
import com.openclassrooms.etudiant.entities.User;
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
public class UserControllerTest {

    private static final String REGISTER_URL = "/api/register";
    private static final String FIND_ALL_URL = "/api/users";
    private static final String FIND_BY_ID_URL = "/api/users/{id}";
    private static final String UPDATE_URL = "/api/users/{id}";
    private static final String DELETE_URL = "/api/users/{id}";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String LOGIN = "login";
    private static final String PASSWORD = "password";
    private static final String UPDATED_FIRST_NAME = "Jane";
    private static final String UPDATED_LAST_NAME = "Smith";
    private static final String UPDATED_LOGIN = "updated-login";
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

    }

    @AfterEach
    public void afterEach() {
        userRepository.deleteAll();
    }

    @Test
    public void registerUserWithoutRequiredData() throws Exception {
        // GIVEN
        RegisterDTO registerDTO = new RegisterDTO();

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post(REGISTER_URL)
                        .content(objectMapper.writeValueAsString(registerDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void registerAlreadyExistUser() throws Exception {
        // GIVEN
        User user = new User();
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setLogin(LOGIN);
        user.setPassword(PASSWORD);
        userService.register(user);

        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setFirstName(FIRST_NAME);
        registerDTO.setLastName(LAST_NAME);
        registerDTO.setLogin(LOGIN);
        registerDTO.setPassword(PASSWORD);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post(REGISTER_URL)
                        .content(objectMapper.writeValueAsString(registerDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void registerUserSuccessful() throws Exception {
        // GIVEN
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setFirstName(FIRST_NAME);
        registerDTO.setLastName(LAST_NAME);
        registerDTO.setLogin(LOGIN);
        registerDTO.setPassword(PASSWORD);

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.post(REGISTER_URL)
                        .content(objectMapper.writeValueAsString(registerDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    public void findAllUsersWithoutAuthentication() throws Exception {
        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(FIND_ALL_URL)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    public void findAllUsersSuccessful() throws Exception {
        // GIVEN
        User savedUser = userRepository.save(getUser(LOGIN));

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(FIND_ALL_URL)
                        .header(AUTHORIZATION, getBearerToken(savedUser))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(savedUser.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].firstName").value(FIRST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].lastName").value(LAST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].login").value(LOGIN))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].password").doesNotExist());
    }

    @Test
    public void findUserByIdSuccessful() throws Exception {
        // GIVEN
        User savedUser = userRepository.save(getUser(LOGIN));

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(FIND_BY_ID_URL, savedUser.getId())
                        .header(AUTHORIZATION, getBearerToken(savedUser))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(savedUser.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value(FIRST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value(LAST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.login").value(LOGIN))
                .andExpect(MockMvcResultMatchers.jsonPath("$.password").doesNotExist());
    }

    @Test
    public void findUnknownUserById() throws Exception {
        // GIVEN
        User savedUser = userRepository.save(getUser(LOGIN));
        Long unknownUserId = savedUser.getId() + 1;

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.get(FIND_BY_ID_URL, unknownUserId)
                        .header(AUTHORIZATION, getBearerToken(savedUser))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void updateUserSuccessful() throws Exception {
        // GIVEN
        User savedUser = userRepository.save(getUser(LOGIN));
        UserUpdateDTO userUpdateDTO = new UserUpdateDTO();
        userUpdateDTO.setFirstName(UPDATED_FIRST_NAME);
        userUpdateDTO.setLastName(UPDATED_LAST_NAME);
        userUpdateDTO.setLogin(UPDATED_LOGIN);
        userUpdateDTO.setPassword("new-password");

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.put(UPDATE_URL, savedUser.getId())
                        .header(AUTHORIZATION, getBearerToken(savedUser))
                        .content(objectMapper.writeValueAsString(userUpdateDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(savedUser.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value(UPDATED_FIRST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value(UPDATED_LAST_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$.login").value(UPDATED_LOGIN))
                .andExpect(MockMvcResultMatchers.jsonPath("$.password").doesNotExist());

        // THEN
        User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getFirstName()).isEqualTo(UPDATED_FIRST_NAME);
        assertThat(updatedUser.getLastName()).isEqualTo(UPDATED_LAST_NAME);
        assertThat(updatedUser.getLogin()).isEqualTo(UPDATED_LOGIN);
        assertThat(updatedUser.getPassword()).isNotEqualTo("new-password");
    }

    @Test
    public void updateUserWithExistingLogin() throws Exception {
        // GIVEN
        User savedUser = userRepository.save(getUser(LOGIN));
        User otherUser = userRepository.save(getUser(UPDATED_LOGIN));
        UserUpdateDTO userUpdateDTO = new UserUpdateDTO();
        userUpdateDTO.setFirstName(UPDATED_FIRST_NAME);
        userUpdateDTO.setLastName(UPDATED_LAST_NAME);
        userUpdateDTO.setLogin(otherUser.getLogin());

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.put(UPDATE_URL, savedUser.getId())
                        .header(AUTHORIZATION, getBearerToken(savedUser))
                        .content(objectMapper.writeValueAsString(userUpdateDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void deleteUserSuccessful() throws Exception {
        // GIVEN
        User savedUser = userRepository.save(getUser(LOGIN));

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.delete(DELETE_URL, savedUser.getId())
                        .header(AUTHORIZATION, getBearerToken(savedUser))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        // THEN
        assertThat(userRepository.existsById(savedUser.getId())).isFalse();
    }

    @Test
    public void deleteUnknownUser() throws Exception {
        // GIVEN
        User savedUser = userRepository.save(getUser(LOGIN));
        Long unknownUserId = savedUser.getId() + 1;

        // WHEN
        mockMvc.perform(MockMvcRequestBuilders.delete(DELETE_URL, unknownUserId)
                        .header(AUTHORIZATION, getBearerToken(savedUser))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    private User getUser(String login) {
        User user = new User();
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setLogin(login);
        user.setPassword(PASSWORD);
        return user;
    }

    private String getBearerToken(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
