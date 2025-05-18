package com.lms.LearningManagementSystem.Controller;
import com.lms.LearningManagementSystem.Model.Course;
import com.lms.LearningManagementSystem.Model.Lesson;
import com.lms.LearningManagementSystem.Service.CourseService;
import com.lms.LearningManagementSystem.Service.UserService.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/courses")
public class CourseController {
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String ADMIN_ERROR_MESSAGE = "Operation failed: You are not an admin.";
    private static final String COURSE_BOOKMARKED = "Course bookmarked.";
    private static final String COURSE_ALREADY_BOOKMARKED = "Course already bookmarked.";
    private static final String MEDIA_ADD_SUCCESS = "Media file added successfully.";
    private static final String MEDIA_ADD_FAILURE = "Failed to add media file.";
    private static final String COURSE_DELETED = "Course deleted successfully.";

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    // Create a course
    @PostMapping("/{AdminId}/create")
    public ResponseEntity<?> createCourse(
            @PathVariable Long AdminId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam int duration)
    {
        try {
            Course course = AdminService.createCourse(AdminId,title, description, duration);
            return new ResponseEntity<>(course, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(ADMIN_ERROR_MESSAGE, HttpStatus.BAD_REQUEST);
        }
    }

    // Update a course
    @PutMapping("/{AdminId}/{courseId}/update")
    public ResponseEntity<?> updateCourse(
            @PathVariable Long AdminId,
            @PathVariable String courseId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam int duration) {
        try {
            Course updatedCourse = AdminService.updateCourse(AdminId ,courseId, title, description, duration);
            return ResponseEntity.ok(updatedCourse);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(ADMIN_ERROR_MESSAGE, HttpStatus.BAD_REQUEST);
        }
    }

    // Delete a course
    @DeleteMapping("/{adminId}/{courseId}/delete")
    public ResponseEntity<String> deleteCourse(@PathVariable String courseId, @PathVariable Long adminId) {
        try {
            AdminService.deleteCourse(adminId, courseId);
            return ResponseEntity.ok(COURSE_DELETED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Operation failed: " + e.getMessage());
        }
    }

    // Add media to a course
    @PostMapping("/{courseId}/media")
    public String addMediaFile(@PathVariable String courseId, @RequestParam String mediaFile) {
        boolean success = courseService.addMediaFile(courseId, mediaFile);
        return success ? MEDIA_ADD_SUCCESS : MEDIA_ADD_FAILURE;
    }

    // Add a lesson to a course
    @PostMapping("/{courseId}/lessons")
    public Lesson addLesson(@PathVariable String courseId, @RequestParam String title, @RequestParam String content) {
        return courseService.addLesson(courseId, title, content);
    }

    // View attendance for a lesson
    @GetMapping("/{courseId}/lessons/{lessonId}/attendance")
    public ResponseEntity<Object> getLessonAttendance(@PathVariable String courseId, @PathVariable String lessonId) {
        try {
            Map<String, Boolean> attendance = courseService.getLessonAttendance(courseId, lessonId);
            return ResponseEntity.ok(attendance);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    // View all courses
    @GetMapping
    public List<Course> getAllCourses() {
        return courseService.getAllCourses();
    }

    // View enrolled students
    @GetMapping("/{courseId}/students")
    public List<Long> getEnrolledStudents(@PathVariable String courseId) {
        return courseService.getEnrolledStudents(courseId);
    }

    @GetMapping("/{courseId}")
    public Course getCourseById(@PathVariable String courseId) {
        Course course = courseService.findCourseById(courseId);
        if (course != null) {
            return course;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
    }

    @PostMapping("/{studentId}/bookmark/{courseId}")
    public ResponseEntity<Object> bookmarkCourse(@PathVariable Long studentId, @PathVariable String courseId) {
        try {
            boolean added = courseService.bookmarkCourse(studentId, courseId);
            if (added) {
                return ResponseEntity.ok(Map.of(MESSAGE_KEY, COURSE_BOOKMARKED));
            } else {
                return ResponseEntity.ok(Map.of(MESSAGE_KEY, COURSE_ALREADY_BOOKMARKED));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    @GetMapping("/{studentId}/bookmarks")
    public ResponseEntity<Object> getBookmarkedCourses(@PathVariable Long studentId) {
        try {
            Set<Course> bookmarks = courseService.getBookmarkedCourses(studentId);
            return ResponseEntity.ok(bookmarks);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Course>> searchCourses(@RequestParam String keyword) {
        List<Course> results = courseService.searchCourses(keyword);
        return ResponseEntity.ok(results);
    }
}
