package com.student.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.student.config.ClientIp;
import com.student.entity.Student;
import com.student.exception.UserFacingException;
import com.student.service.AuditService;
import com.student.service.FileStorageService;
import com.student.service.RequestThrottle;
import com.student.service.StudentService;
import com.student.service.StudentValidator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class StudentController {

    private static final Logger log = LoggerFactory.getLogger(StudentController.class);

    private static final int PAGE_SIZE = 10;
    
    private static final int MAX_PENDING = 200;

    @Autowired
    private StudentService studentService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private StudentValidator studentValidator;

    @Autowired
    private RequestThrottle throttle;

    // Home Page - the public welcome page for visitors, the admin
    // dashboard for anyone already logged in.
    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "0") int page, Model model, HttpSession session) {

        if (session.getAttribute("adminUsername") == null) {
            return "redirect:/welcome";
        }

        Page<Student> studentPage = loadPage("", "", "", "", page);

        model.addAttribute("studentPage", studentPage);
        model.addAttribute("students", studentPage.getContent());
        model.addAttribute("pagerBase", "/?");
        model.addAttribute("pendingCount", studentService.countPending());

        return "index";
    }

    // Welcome Page (public) - landing page with info about ABiS and
    // links to Login / Register.
    @GetMapping("/welcome")
    public String welcome(@RequestParam(value = "registered", required = false) Boolean registered,
            HttpSession session, Model model) {

        if (session.getAttribute("adminUsername") != null) {
            return "redirect:/";
        }

        if (Boolean.TRUE.equals(registered)) {
            model.addAttribute("registered", true);
        }

        return "welcome";
    }

    // Registration Page (public) - a self-service version of the Add
    // Student form. No login required, and no admin-style controls.
    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("student")) {
            model.addAttribute("student", new Student());
        }
        return "register";
    }

    // Save Registration - stores the student the same way admin's
    // "Add Student" does, so it shows up immediately in the admin
    // dashboard / student list, then sends the visitor back to the
    // welcome page.
    @PostMapping("/register/save")
    public String registerSave(@ModelAttribute Student student,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String ip = ClientIp.of(request);

        if (throttle.isBlocked(RequestThrottle.REGISTER_IP, ip)) {
            redirectAttributes.addFlashAttribute("error",
                    "Too many registrations from your network. Please try again in "
                            + throttle.retryAfterMinutes(RequestThrottle.REGISTER_IP, ip) + " minute(s).");
            redirectAttributes.addFlashAttribute("student", student);
            return "redirect:/register";
        }
        if (studentService.countPending() >= MAX_PENDING) {
            redirectAttributes.addFlashAttribute("error",
                    "Registrations are temporarily full. Please try again later.");
            redirectAttributes.addFlashAttribute("student", student);
            return "redirect:/register";
        }
        throttle.record(RequestThrottle.REGISTER_IP, ip);

        try {
            // Never trust an id or photo URL sent by the browser
            student.setId(0);
            student.setPhotoUrl(null);
            student.setApproved(false);   // always pending - a visitor can never approve themselves

            studentValidator.validate(student);

            String photoUrl = fileStorageService.storePhoto(photoFile);
            if (photoUrl != null) {
                student.setPhotoUrl(photoUrl);
            }

            Student saved = studentService.saveStudent(student);
            auditService.record(AuditService.STUDENT_REGISTERED, "Student", saved.getId(),
            		"Self-registration (pending approval): " + describe(saved));
            return "redirect:/welcome?registered=true";

        } catch (Exception ex) {
            log.error("Failed to register student", ex);
            redirectAttributes.addFlashAttribute("error",
                    "Could not complete registration. " + rootMessage(ex));
            redirectAttributes.addFlashAttribute("student", student);
            return "redirect:/register";
        }
    }

    // Add Student Page
    @GetMapping("/add")
    public String addStudentForm(Model model) {
        if (!model.containsAttribute("student")) {
            model.addAttribute("student", new Student());
        }
        return "add-student";
    }

    // Save Student
    @PostMapping("/save")
    public String saveStudent(@ModelAttribute Student student,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            RedirectAttributes redirectAttributes) {

        try {
            // A new student can never overwrite an existing record, and the photo URL
            // only ever comes from the upload below
            student.setId(0);
            student.setPhotoUrl(null);
            student.setApproved(true);

            studentValidator.validate(student);

            String photoUrl = fileStorageService.storePhoto(photoFile);
            if (photoUrl != null) {
                student.setPhotoUrl(photoUrl);
            }

            Student saved = studentService.saveStudent(student);
            auditService.record(AuditService.STUDENT_CREATED, "Student", saved.getId(),
                    "Added student: " + describe(saved));
            return "redirect:/students?added=true";

        } catch (Exception ex) {
            // Never let a photo/DB problem crash the whole request with a
            // whitelabel error page - send the admin back to the form instead.
            log.error("Failed to save student", ex);
            redirectAttributes.addFlashAttribute("error",
                    "Could not save the student. " + rootMessage(ex));
            redirectAttributes.addFlashAttribute("student", student);
            return "redirect:/add";
        }
    }

    // View Students (server-side filters + pagination)
    @GetMapping("/students")
    public String viewStudents(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String course,
            @RequestParam(defaultValue = "") String domain,
            @RequestParam(defaultValue = "") String fee,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        String qf = q.trim();
        String cf = course.trim();
        String df = domain.trim();
        String ff = normalizeFee(fee);

        Page<Student> studentPage = loadPage(qf, cf, df, ff, page);
        List<String> domains = studentService.getDomains();

        String listUrl = withParams("/students", qf, cf, df, ff);

        model.addAttribute("studentPage", studentPage);
        model.addAttribute("students", studentPage.getContent());
        model.addAttribute("pagerBase", listUrl + (listUrl.contains("?") ? "&" : "?"));
        model.addAttribute("exportUrl", withParams("/students/export", qf, cf, df, ff));
        model.addAttribute("domains", domains);
        model.addAttribute("totalStudents", studentService.totalStudents());
        model.addAttribute("paidCount", studentService.countPaid());
        model.addAttribute("unpaidCount", studentService.countUnpaid());
        return "view-students";
    }

    // Export the students matching the current filters as a CSV file (opens in Excel)
    @GetMapping("/students/export")
    public void exportStudents(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String course,
            @RequestParam(defaultValue = "") String domain,
            @RequestParam(defaultValue = "") String fee,
            HttpServletResponse response) throws IOException {

        String qf = q.trim();
        String cf = course.trim();
        String df = domain.trim();
        String ff = normalizeFee(fee);

        List<Student> students = studentService.search(qf, cf, df, ff, Pageable.unpaged()).getContent().stream()
                .sorted(Comparator.comparingInt(Student::getId))
                .collect(Collectors.toList());

        auditService.record(AuditService.STUDENTS_EXPORTED, "Student", null,
                "Exported " + students.size() + " student(s) to CSV (search='" + qf + "', course='" + cf
                        + "', domain='" + df + "', fees='" + ff + "')");

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"students.csv\"");

        PrintWriter out = response.getWriter();
        out.write('\uFEFF'); // lets Excel detect UTF-8
        out.write("ID,Name,Age,Course,Domain,CGPA,Passout Year,Email,Phone,College,Joining Date,"
                + "Completion Date,Total Fees,Fees Paid,Fees Status\r\n");
        for (Student s : students) {
            out.write(csv(s.getId(), s.getName(), s.getAge(), s.getCourse(), s.getDomain(), s.getCgpa(),
                    s.getPassoutYear(), s.getEmail(), s.getPhone(), s.getCollegeName(), s.getJoiningDate(),
                    s.getCompletionDate(), plain(s.getTotalFees()), plain(s.getFeesPaid()), s.getFeeStatus()));
        }
        out.flush();
    }

    // View Single Student Profile (ID Card + full profile)
    @GetMapping("/view/{id}")
    public String viewStudentProfile(@PathVariable int id, Model model) {
        Student student = studentService.getStudentOrThrow(id);
        auditService.record(AuditService.STUDENT_VIEWED, "Student", id, "Viewed profile of '" + student.getName() + "'");
        model.addAttribute("student", student);
        return "profile";
    }

    // Edit Student
    @GetMapping("/edit/{id}")
    public String editStudent(@PathVariable int id, Model model) {
        if (!model.containsAttribute("student")) {
            model.addAttribute("student", studentService.getStudentOrThrow(id));
        }
        return "edit-student";
    }

    // Update Student
    @PostMapping("/update")
    public String updateStudent(@ModelAttribute Student student,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
            RedirectAttributes redirectAttributes) {

        // The record as it is stored right now (before this edit is saved).
        // A missing record is a 404, not a form error.
        Student existing = studentService.getStudentOrThrow(student.getId());

        try {
        	student.setApproved(existing.isApproved());   // the edit form must never change approval
        	
            studentValidator.validate(student);

            String photoUrl = fileStorageService.storePhoto(photoFile);
            if (photoUrl != null) {
                // A new photo was uploaded - use it
                student.setPhotoUrl(photoUrl);
            } else {
                // No new photo chosen - keep whatever photo the student already had
                student.setPhotoUrl(existing.getPhotoUrl());
            }

            // Work out what changed BEFORE saving, while 'existing' still has the old values
            String changes = auditService.describeChanges(existing, student);
            String studentName = existing.getName() != null ? existing.getName() : student.getName();

            studentService.updateStudent(student);
            if (photoUrl != null) {
                fileStorageService.deletePhoto(existing.getPhotoUrl());
            }
            auditService.record(AuditService.STUDENT_UPDATED, "Student", student.getId(),
                    "Updated student '" + studentName + "' - " + changes);
            return "redirect:/students?updated=true";

        } catch (Exception ex) {
            log.error("Failed to update student", ex);
            redirectAttributes.addFlashAttribute("error",
                    "Could not update the student. " + rootMessage(ex));
            redirectAttributes.addFlashAttribute("student", student);
            return "redirect:/edit/" + student.getId();
        }
    }

    // Delete Student (POST only - a plain link or a browser prefetch can no longer delete anything)
    @PostMapping("/delete")
    public String deleteStudent(@RequestParam int id) {
        // Look the student up first - after deleting, their details are gone
        Student existing = studentService.getStudentOrThrow(id);

        studentService.deleteStudent(id);
        fileStorageService.deletePhoto(existing.getPhotoUrl());
        auditService.record(AuditService.STUDENT_DELETED, "Student", id, "Deleted student: " + describe(existing));
        return "redirect:/students?deleted=true";
    }
    
 // Pending registrations (admin only - not in the login-free list in WebConfig)
    @GetMapping("/pending")
    public String pending(Model model) {
        model.addAttribute("pending", studentService.getPending());
        return "pending";
    }

    @PostMapping("/pending/approve")
    public String approve(@RequestParam int id) {
        Student s = studentService.getStudentOrThrow(id);
        if (s.isApproved()) {
            throw new UserFacingException("That registration is not pending.");
        }
        studentService.approve(id);
        auditService.record(AuditService.STUDENT_APPROVED, "Student", id,
                "Approved registration: " + describe(s));
        return "redirect:/pending?approved=true";
    }

    @PostMapping("/pending/reject")
    public String reject(@RequestParam int id) {
        Student s = studentService.getStudentOrThrow(id);
        if (s.isApproved()) {
            throw new UserFacingException("That registration is not pending.");
        }
        studentService.deleteStudent(id);
        fileStorageService.deletePhoto(s.getPhotoUrl());
        auditService.record(AuditService.STUDENT_REJECTED, "Student", id,
                "Rejected registration: " + describe(s));
        return "redirect:/pending?rejected=true";
    }
    
    // Search Student
    @GetMapping("/search")
    public String searchStudent(@RequestParam(required = false) String name,
                                Model model) {

        if (name != null && !name.trim().isEmpty()) {
            model.addAttribute("students", studentService.searchByName(name));
        }

        return "search-student";
    }

    // Live search suggestions (used by the autocomplete dropdown on the
    // Search Student page). Returns a small JSON list of matching students.
    @GetMapping("/search/suggestions")
    @ResponseBody
    public List<Map<String, Object>> searchSuggestions(@RequestParam(required = false) String query) {

        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        return studentService.searchByName(query.trim()).stream()
                .limit(8)
                .map(s -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", s.getId());
                    row.put("name", s.getName());
                    row.put("course", s.getCourse());
                    row.put("domain", s.getDomain());
                    return row;
                })
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ helpers

    // One page of students; a page number past the end falls back to the last page
    private Page<Student> loadPage(String q, String course, String domain, String fee, int page) {
        int wanted = Math.max(page, 0);
        Page<Student> result = studentService.search(q, course, domain, fee, pageRequest(wanted));
        if (result.getTotalPages() > 0 && wanted >= result.getTotalPages()) {
            result = studentService.search(q, course, domain, fee, pageRequest(result.getTotalPages() - 1));
        }
        return result;
    }

    private Pageable pageRequest(int page) {
        return PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id"));
    }

    private String normalizeFee(String fee) {
        String f = fee == null ? "" : fee.trim().toUpperCase();
        return ("PAID".equals(f) || "UNPAID".equals(f)) ? f : "";
    }

    // "/students?q=abc&course=BCA" - only filters that are set are added
    private String withParams(String path, String q, String course, String domain, String fee) {
        String[][] params = { { "q", q }, { "course", course }, { "domain", domain }, { "fee", fee } };
        StringBuilder sb = new StringBuilder(path);
        String separator = "?";
        for (String[] p : params) {
            if (p[1] != null && !p[1].isEmpty()) {
                sb.append(separator).append(p[0]).append('=').append(URLEncoder.encode(p[1], StandardCharsets.UTF_8));
                separator = "&";
            }
        }
        return sb.toString();
    }

    private String plain(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    // One CSV line. Cells starting with = + - @ get a leading apostrophe so Excel
    // never runs them as a formula.
    private String csv(Object... cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            String value = cells[i] == null ? "" : cells[i].toString();
            if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
                value = "'" + value;
            }
            sb.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        return sb.append("\r\n").toString();
    }

    // One-line summary of a student for the audit log
    private String describe(Student s) {
        return "name='" + s.getName() + "', course='" + s.getCourse() + "', domain='" + s.getDomain()
                + "', email='" + s.getEmail() + "'";
    }

    // Text that is safe to show in the flash error message: only our own validation /
    // upload messages are shown; anything else (database errors, ...) stays in the log.
    private String rootMessage(Exception ex) {
        if (ex instanceof UserFacingException) {
            return ex.getMessage();
        }
        return "Please try again.";
    }
}
