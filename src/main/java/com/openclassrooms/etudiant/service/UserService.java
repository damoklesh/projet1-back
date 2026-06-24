package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void register(User user) {
        Assert.notNull(user, "User must not be null");
        log.info("Registering new user");

        Optional<User> optionalUser = userRepository.findByLogin(user.getLogin());
        if (optionalUser.isPresent()) {
            throw new IllegalArgumentException("User with login " + user.getLogin() + " already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public List<User> findAll() {
        log.info("Retrieving users");
        return userRepository.findAll();
    }

    public User findById(Long id) {
        Assert.notNull(id, "User id must not be null");
        log.info("Retrieving user");

        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + id + " does not exist"));
    }

    public User update(Long id, User user, String password) {
        Assert.notNull(id, "User id must not be null");
        Assert.notNull(user, "User must not be null");
        log.info("Updating user");

        User existingUser = findById(id);
        if (!existingUser.getLogin().equals(user.getLogin())) {
            Optional<User> optionalUser = userRepository.findByLogin(user.getLogin());
            if (optionalUser.isPresent()) {
                throw new IllegalArgumentException("User with login " + user.getLogin() + " already exists");
            }
        }

        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setLogin(user.getLogin());

        if (StringUtils.hasText(password)) {
            existingUser.setPassword(passwordEncoder.encode(password));
        }

        return userRepository.save(existingUser);
    }

    public void delete(Long id) {
        Assert.notNull(id, "User id must not be null");
        log.info("Deleting user");

        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User with id " + id + " does not exist");
        }
        userRepository.deleteById(id);
    }

    public String login(String login, String password) {
        Assert.notNull(login, "Login must not be null");
        Assert.notNull(password, "Password must not be null");
        Optional<User> user = userRepository.findByLogin(login);
        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
            return jwtService.generateToken(user.get());
        } else {
            throw new IllegalArgumentException("Invalid credentials");
        }
    }


}
