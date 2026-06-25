package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.repository.StudentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class StudentServiceTest {
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String EMAIL = "john.doe@email.com";
    private static final String OTHER_EMAIL = "jane.doe@email.com";

    @Mock
    private StudentRepository studentRepository;
    @InjectMocks
    private StudentService studentService;

    @Test
    public void test_create_null_student_throws_IllegalArgumentException() {
        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> studentService.create(null));
    }

    @Test
    public void test_create_already_exist_student_throws_IllegalArgumentException() {
        // GIVEN
        Student student = getStudent(EMAIL);
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.of(student));

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> studentService.create(student));
    }

    @Test
    public void test_create_student() {
        // GIVEN
        Student student = getStudent(EMAIL);
        when(studentRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // WHEN
        studentService.create(student);

        // THEN
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        assertThat(studentCaptor.getValue()).isEqualTo(student);
    }

    @Test
    public void test_find_all_students() {
        // GIVEN
        Student student = getStudent(EMAIL);
        when(studentRepository.findAll()).thenReturn(List.of(student));

        // WHEN
        List<Student> students = studentService.findAll();

        // THEN
        assertThat(students).containsExactly(student);
    }

    @Test
    public void test_find_by_id_unknown_student_throws_IllegalArgumentException() {
        // GIVEN
        Long studentId = 1L;
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> studentService.findById(studentId));
    }

    @Test
    public void test_find_by_id_student() {
        // GIVEN
        Long studentId = 1L;
        Student student = getStudent(EMAIL);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        // WHEN
        Student result = studentService.findById(studentId);

        // THEN
        assertThat(result).isEqualTo(student);
    }

    @Test
    public void test_update_unknown_student_throws_IllegalArgumentException() {
        // GIVEN
        Long studentId = 1L;
        Student student = getStudent(EMAIL);
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> studentService.update(studentId, student));
    }

    @Test
    public void test_update_with_existing_email_throws_IllegalArgumentException() {
        // GIVEN
        Long studentId = 1L;
        Student existingStudent = getStudent(EMAIL);
        Student studentToUpdate = getStudent(OTHER_EMAIL);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(existingStudent));
        when(studentRepository.findByEmail(OTHER_EMAIL)).thenReturn(Optional.of(getStudent(OTHER_EMAIL)));

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> studentService.update(studentId, studentToUpdate));
    }

    @Test
    public void test_update_student() {
        // GIVEN
        Long studentId = 1L;
        Student existingStudent = getStudent(EMAIL);
        Student studentToUpdate = getStudent(OTHER_EMAIL);
        studentToUpdate.setFirstName("Jane");
        studentToUpdate.setLastName("Smith");
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(existingStudent));
        when(studentRepository.findByEmail(OTHER_EMAIL)).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Student result = studentService.update(studentId, studentToUpdate);

        // THEN
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLastName()).isEqualTo("Smith");
        assertThat(result.getEmail()).isEqualTo(OTHER_EMAIL);
    }

    @Test
    public void test_delete_unknown_student_throws_IllegalArgumentException() {
        // GIVEN
        Long studentId = 1L;
        when(studentRepository.existsById(studentId)).thenReturn(false);

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> studentService.delete(studentId));
    }

    @Test
    public void test_delete_student() {
        // GIVEN
        Long studentId = 1L;
        when(studentRepository.existsById(studentId)).thenReturn(true);

        // WHEN
        studentService.delete(studentId);

        // THEN
        verify(studentRepository).deleteById(studentId);
    }

    private Student getStudent(String email) {
        Student student = new Student();
        student.setFirstName(FIRST_NAME);
        student.setLastName(LAST_NAME);
        student.setEmail(email);
        return student;
    }
}
