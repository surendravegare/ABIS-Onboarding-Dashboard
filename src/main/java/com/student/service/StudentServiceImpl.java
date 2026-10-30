package com.student.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.student.entity.Student;
import com.student.exception.ResourceNotFoundException;
import com.student.repository.StudentRepository;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentRepository studentRepository;

    // Turns [ ["Java", 5], [null, 2], ... ] rows from the DB into an
    // ordered label -> count map, folding null/blank labels into "Unspecified".
    private Map<String, Long> toStatsMap(List<Object[]> rows) {
        Map<String, Long> stats = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String label = row[0] == null ? "" : row[0].toString().trim();
            if (label.isEmpty()) {
                label = "Unspecified";
            }
            long count = ((Number) row[1]).longValue();

            stats.merge(label, count, Long::sum);
        }

        return stats;
    }

    @Override
    public Student saveStudent(Student student) {
        return studentRepository.save(student);
    }

    @Override
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    public Student getStudentById(int id) {
        return studentRepository.findById(id).orElse(null);
    }

    @Override
    public Student getStudentOrThrow(int id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No student was found with ID " + id + "."));
    }

    @Override
    public Student updateStudent(Student student) {
        return studentRepository.save(student);
    }

    @Override
    public void deleteStudent(int id) {
        studentRepository.deleteById(id);
    }

    @Override
    public List<Student> searchByName(String name) {
        return studentRepository.findByNameContainingIgnoreCaseAndApprovedTrue(name);
    }

    @Override
    public long totalStudents() {
    	return studentRepository.countByApproved(true);
    }

    @Override
    public Page<Student> search(String q, String course, String domain, String fee, Pageable pageable) {
        return studentRepository.search(q, course, domain, fee, pageable);
    }

    @Override
    public List<String> getDomains() {
        return studentRepository.findDistinctDomains();
    }

    @Override
    public long countPaid() {
        return studentRepository.countPaid();
    }

    @Override
    public long countUnpaid() {
    	return totalStudents() - studentRepository.countPaid();
    }

    @Override
    public Map<String, Long> getDomainStats() {
        return toStatsMap(studentRepository.countGroupedByDomain());
    }

    @Override
    public Map<String, Long> getCourseStats() {
        return toStatsMap(studentRepository.countGroupedByCourse());
    }
    
    @Override
    public List<Student> getPending() {
        return studentRepository.findByApprovedFalseOrderByIdAsc();
    }

    @Override
    public long countPending() {
        return studentRepository.countByApproved(false);
    }

    @Override
    public void approve(int id) {
        Student s = getStudentOrThrow(id);
        s.setApproved(true);
        studentRepository.save(s);
    }
}