package com.student.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.student.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Integer> {

	List<Student> findByNameContainingIgnoreCaseAndApprovedTrue(String name);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, int id);

	boolean existsByPhoneAndIdNot(String phone, int id);

	// Row = [domain (String), count (Long)] grouped straight from the DB
	@Query("SELECT s.domain, COUNT(s) FROM Student s WHERE s.approved = true GROUP BY s.domain ORDER BY COUNT(s) DESC")
	List<Object[]> countGroupedByDomain();

	// Row = [course (String), count (Long)] grouped straight from the DB
	@Query("SELECT s.course, COUNT(s) FROM Student s WHERE s.approved = true GROUP BY s.course ORDER BY COUNT(s) DESC")
	List<Object[]> countGroupedByCourse();

	@Query("SELECT DISTINCT s.domain FROM Student s WHERE s.approved = true AND s.domain IS NOT NULL AND s.domain <> '' ORDER BY s.domain")
	List<String> findDistinctDomains();

	// Same rule as Student.getFeeStatus(): PAID only when both amounts exist, total > 0 and paid >= total
	@Query("SELECT COUNT(s) FROM Student s WHERE s.approved = true AND s.totalFees IS NOT NULL "
			+ "AND s.totalFees > 0 AND s.feesPaid >= s.totalFees")
	long countPaid();

	// Empty string = "don't filter on this". fee is "", "PAID" or "UNPAID".
	@Query("SELECT s FROM Student s WHERE s.approved = true AND "
			+ "(:course = '' OR s.course = :course) AND "
			+ "(:domain = '' OR s.domain = :domain) AND "
			+ "(:q = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) "
			+ "OR LOWER(s.course) LIKE LOWER(CONCAT('%', :q, '%')) "
			+ "OR LOWER(s.domain) LIKE LOWER(CONCAT('%', :q, '%')) "
			+ "OR STR(s.id) LIKE CONCAT('%', :q, '%')) AND "
			+ "(:fee = '' "
			+ "OR (:fee = 'PAID' AND s.totalFees IS NOT NULL AND s.feesPaid IS NOT NULL AND s.totalFees > 0 AND s.feesPaid >= s.totalFees) "
			+ "OR (:fee = 'UNPAID' AND (s.totalFees IS NULL OR s.feesPaid IS NULL OR s.totalFees <= 0 OR s.feesPaid < s.totalFees)))")
	Page<Student> search(@Param("q") String q, @Param("course") String course, @Param("domain") String domain,
			@Param("fee") String fee, Pageable pageable);
	
	List<Student> findByApprovedFalseOrderByIdAsc();
	long countByApproved(boolean approved);
}
