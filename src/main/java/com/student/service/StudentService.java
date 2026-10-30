package com.student.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.student.entity.Student;

public interface StudentService {

    Student saveStudent(Student student);

    List<Student> getAllStudents();

    Student getStudentById(int id);

    // Like getStudentById, but throws ResourceNotFoundException (HTTP 404) when there is no such student
    Student getStudentOrThrow(int id);

    Student updateStudent(Student student);

    void deleteStudent(int id);

    List<Student> searchByName(String name);

    long totalStudents();

    // One page of students; pass "" for any filter that should not apply
    Page<Student> search(String q, String course, String domain, String fee, Pageable pageable);

    // Distinct domain names for the filter drop-down
    List<String> getDomains();

    long countPaid();

    long countUnpaid();

    // Student counts grouped by domain, straight from the database
    Map<String, Long> getDomainStats();

    // Student counts grouped by course, straight from the database
    Map<String, Long> getCourseStats();
    
    List<Student> getPending();

    long countPending();

    void approve(int id);
}
