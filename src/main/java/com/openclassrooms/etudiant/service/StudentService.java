package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;

    public Student create(Student student) {
        Assert.notNull(student, "Student must not be null");
        log.info("Creating student");

        Optional<Student> optionalStudent = studentRepository.findByEmail(student.getEmail());
        if (optionalStudent.isPresent()) {
            throw new IllegalArgumentException("Student with email " + student.getEmail() + " already exists");
        }

        return studentRepository.save(student);
    }

    public List<Student> findAll() {
        log.info("Retrieving students");
        return studentRepository.findAll();
    }

    public Student findById(Long id) {
        Assert.notNull(id, "Student id must not be null");
        log.info("Retrieving student");

        return studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student with id " + id + " does not exist"));
    }

    public Student update(Long id, Student student) {
        Assert.notNull(id, "Student id must not be null");
        Assert.notNull(student, "Student must not be null");
        log.info("Updating student");

        Student existingStudent = findById(id);
        if (!existingStudent.getEmail().equals(student.getEmail())) {
            Optional<Student> optionalStudent = studentRepository.findByEmail(student.getEmail());
            if (optionalStudent.isPresent()) {
                throw new IllegalArgumentException("Student with email " + student.getEmail() + " already exists");
            }
        }

        existingStudent.setFirstName(student.getFirstName());
        existingStudent.setLastName(student.getLastName());
        existingStudent.setEmail(student.getEmail());

        return studentRepository.save(existingStudent);
    }

    public void delete(Long id) {
        Assert.notNull(id, "Student id must not be null");
        log.info("Deleting student");

        if (!studentRepository.existsById(id)) {
            throw new IllegalArgumentException("Student with id " + id + " does not exist");
        }
        studentRepository.deleteById(id);
    }
}
