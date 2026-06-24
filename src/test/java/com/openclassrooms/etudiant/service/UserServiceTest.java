package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class UserServiceTest {
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String LOGIN = "LOGIN";
    private static final String PASSWORD = "PASSWORD";
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private UserService userService;

    @Test
    public void test_create_null_user_throws_IllegalArgumentException() {
        // GIVEN

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.register(null));
    }

    @Test
    public void test_create_already_exist_user_throws_IllegalArgumentException() {
        // GIVEN
        User user = new User();
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setLogin(LOGIN);
        user.setPassword(PASSWORD);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD);
        when(userRepository.findByLogin(any())).thenReturn(Optional.of(user));

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.register(user));
    }

    @Test
    public void test_create_user() {
        // GIVEN
        User user = new User();
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setLogin(LOGIN);
        user.setPassword(PASSWORD);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD);
        when(userRepository.findByLogin(any())).thenReturn(Optional.empty());

        // WHEN
        userService.register(user);

        // THEN
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue()).isEqualTo(user);
    }

    @Test
    public void test_find_by_id_unknown_user_throws_IllegalArgumentException() {
        // GIVEN
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.findById(userId));
    }

    @Test
    public void test_find_by_id_user() {
        // GIVEN
        Long userId = 1L;
        User user = getUser(LOGIN);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // WHEN
        User result = userService.findById(userId);

        // THEN
        assertThat(result).isEqualTo(user);
    }

    @Test
    public void test_update_unknown_user_throws_IllegalArgumentException() {
        // GIVEN
        Long userId = 1L;
        User user = getUser(LOGIN);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.update(userId, user, null));
    }

    @Test
    public void test_update_with_existing_login_throws_IllegalArgumentException() {
        // GIVEN
        Long userId = 1L;
        User existingUser = getUser(LOGIN);
        User userToUpdate = getUser("OTHER_LOGIN");
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByLogin("OTHER_LOGIN")).thenReturn(Optional.of(getUser("OTHER_LOGIN")));

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.update(userId, userToUpdate, null));
    }

    @Test
    public void test_update_user() {
        // GIVEN
        Long userId = 1L;
        User existingUser = getUser(LOGIN);
        User userToUpdate = getUser("OTHER_LOGIN");
        userToUpdate.setFirstName("Jane");
        userToUpdate.setLastName("Smith");
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByLogin("OTHER_LOGIN")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("NEW_PASSWORD")).thenReturn("ENCODED_NEW_PASSWORD");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        User result = userService.update(userId, userToUpdate, "NEW_PASSWORD");

        // THEN
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLastName()).isEqualTo("Smith");
        assertThat(result.getLogin()).isEqualTo("OTHER_LOGIN");
        assertThat(result.getPassword()).isEqualTo("ENCODED_NEW_PASSWORD");
    }

    private User getUser(String login) {
        User user = new User();
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setLogin(login);
        user.setPassword(PASSWORD);
        return user;
    }
}
