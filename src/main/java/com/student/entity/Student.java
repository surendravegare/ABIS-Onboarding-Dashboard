package com.student.entity;

import java.time.LocalDate;
import java.math.BigDecimal;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.persistence.Column;

@Entity
@Table(name = "students")
public class Student {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	@NotBlank(message = "Name is required")
	@Size(max = 100, message = "Name must be at most 100 characters")
	private String name;
	@Min(value = 15, message = "Age must be between 15 and 60")
	@Max(value = 60, message = "Age must be between 15 and 60")
	private int age;
	@NotBlank(message = "Course is required")
	private String course;
	@NotNull(message = "CGPA is required")
	@DecimalMin(value = "0.0", message = "CGPA must be between 0 and 10")
	@DecimalMax(value = "10.0", message = "CGPA must be between 0 and 10")
	private Double cgpa;
	@NotNull(message = "Passout year is required")
	@Min(value = 1950, message = "Passout year must be between 1950 and 2100")
	@Max(value = 2100, message = "Passout year must be between 1950 and 2100")
	private Integer passoutYear;
	@NotBlank(message = "Email is required")
	@Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Enter a valid email address")
	private String email;
	@NotBlank(message = "Phone number is required")
	@Pattern(regexp = "[0-9]{10}", message = "Phone number must be exactly 10 digits")
	private String phone;
	@NotBlank(message = "Domain is required")
	private String domain;
	@NotNull(message = "Joining date is required")
	private LocalDate joiningDate;
	@NotNull(message = "Completion date is required")
	private LocalDate completionDate;
    @NotBlank(message = "College name is required")
    @Size(max = 200, message = "College name must be at most 200 characters")
    private String collegeName;
	@NotNull(message = "Total fees is required")
	@DecimalMin(value = "0", message = "Total fees cannot be negative")
	private BigDecimal totalFees;
	@NotNull(message = "Fees paid is required")
	@DecimalMin(value = "0", message = "Fees paid cannot be negative")
	private BigDecimal feesPaid;
	private String photoUrl;
	private String workMode;
	
	@Column(nullable = false)
	private boolean approved = true;
	
	public Student() {
		super();
	}

	public Student(int id, String name, int age, String course, Double cgpa, Integer passoutYear, String email, String phone, String domain,
			LocalDate joiningDate, LocalDate completionDate, String collegeName, BigDecimal totalFees, BigDecimal feesPaid, String photoUrl) {
		super();
		this.id = id;
		this.name = name;
		this.age = age;
		this.course = course;
		this.cgpa = cgpa;
		this.passoutYear = passoutYear;
		this.email = email;
		this.phone = phone;
		this.domain = domain;
		this.joiningDate = joiningDate;
		this.completionDate = completionDate;
		this.collegeName = collegeName;
		this.totalFees = totalFees;
		this.feesPaid = feesPaid;
		this.photoUrl = photoUrl;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getCourse() {
		return course;
	}

	public void setCourse(String course) {
		this.course = course;
	}

	public Double getCgpa() {
		return cgpa;
	}

	public void setCgpa(Double cgpa) {
		this.cgpa = cgpa;
	}

	public Integer getPassoutYear() {
		return passoutYear;
	}

	public void setPassoutYear(Integer passoutYear) {
		this.passoutYear = passoutYear;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public LocalDate getJoiningDate() {
		return joiningDate;
	}

	public void setJoiningDate(LocalDate joiningDate) {
		this.joiningDate = joiningDate;
	}

	public LocalDate getCompletionDate() {
		return completionDate;
	}

	public void setCompletionDate(LocalDate completionDate) {
		this.completionDate = completionDate;
	}

	public String getCollegeName() {
		return collegeName;
	}

	public void setCollegeName(String collegeName) {
		this.collegeName = collegeName;
	}

	public BigDecimal getTotalFees() {
		return totalFees;
	}

	public void setTotalFees(BigDecimal totalFees) {
		this.totalFees = totalFees;
	}

	public BigDecimal getFeesPaid() {
		return feesPaid;
	}

	public void setFeesPaid(BigDecimal feesPaid) {
		this.feesPaid = feesPaid;
	}

	public String getFeeStatus() {
		if (totalFees == null || feesPaid == null) {
			return "UNPAID";
		}
		if (feesPaid.compareTo(totalFees) >= 0) {
			return "PAID";
		}
		if (feesPaid.signum() > 0) {
			return "PARTIAL";
		}
		return "UNPAID";
	}

	public BigDecimal getRemainingDue() {
		if (totalFees == null) {
			return BigDecimal.ZERO;
		}
		BigDecimal paid = (feesPaid == null) ? BigDecimal.ZERO : feesPaid;
		BigDecimal due = totalFees.subtract(paid);
		return due.signum() > 0 ? due : BigDecimal.ZERO;
	}

	public String getWorkMode() {
		return workMode;
	}

	public void setWorkMode(String workMode) {
		this.workMode = workMode;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
	}
	
	public boolean isApproved() {
	    return approved;
	}

	public void setApproved(boolean approved) {
	    this.approved = approved;
	}
}
