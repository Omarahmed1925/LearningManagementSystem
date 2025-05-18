package com.lms.LearningManagementSystem.Service;

import com.lms.LearningManagementSystem.Model.User.Admin;

import com.lms.LearningManagementSystem.Model.Course;
import com.lms.LearningManagementSystem.Model.Lesson;
import com.lms.LearningManagementSystem.Model.User.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.lms.LearningManagementSystem.Service.UserService.UserService.userStore;

@Service
public class CourseService {

    private final AtomicLong courseIdGenerator = new AtomicLong(1);   // 🆕 For Course IDs
    private final AtomicLong lessonIdGenerator = new AtomicLong(1);   // 🆕 For Lesson IDs
    private final List<Course> courses = new ArrayList<>();
    private final NotificationService notificationService;

    @Autowired
    public CourseService(@Lazy NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private String generateCourseId() {
        return "COURSE-" + courseIdGenerator.getAndIncrement();
    }

    private String generateLessonId() {
        return "LESSON-" + lessonIdGenerator.getAndIncrement();
    }

    public Course createCourse(Long adminId, String title, String description, int duration) {
        User user = userStore.get(adminId);
        if (user == null || !(user instanceof Admin)) {
            throw new IllegalArgumentException("Only admins can create courses.");
        }
        String courseId = generateCourseId();
        Course course = new Course(courseId, title, description, duration);
        courses.add(course);
        return course;
    }

    public Course findCourseById(String courseId) {
        return courses.stream()
                .filter(course -> course.getId().equals(courseId))
                .findFirst()
                .orElse(null);
    }

    public boolean addMediaFile(String courseId, String mediaFile) {
        Course course = findCourseById(courseId);
        if (course != null) {
            course.getMediaFiles().add(mediaFile);
            return true;
        }
        return false;
    }

    public Lesson addLesson(String courseId, String title, String content) {
        Course course = findCourseById(courseId);
        if (course != null) {
            String lessonId = generateLessonId();
            Lesson lesson = new Lesson(lessonId, title, content);
            course.getLessons().add(lesson);
            return lesson;
        }
        return null;
    }

    public String generateOtp(String courseId, String lessonId) {
        Course course = findCourseById(courseId);
        if (course != null) {
            for (Lesson lesson : course.getLessons()) {
                if (lesson.getId().equals(lessonId)) {
                    String otp = UUID.randomUUID().toString().substring(0, 6);
                    lesson.setOtp(otp);
                    return otp;
                }
            }
        }
        return null;
    }

    public boolean markAttendance(String courseId, String lessonId, String studentId, boolean present) {
        Course course = findCourseById(courseId);
        if (course != null && course.getEnrolledStudents().contains(Long.valueOf(studentId))) {
            for (Lesson lesson : course.getLessons()) {
                if (lesson.getId().equals(lessonId)) {
                    lesson.markAttendance(studentId, present);
                    return true;
                }
            }
        }
        return false;
    }

    public Map<String, Boolean> getLessonAttendance(String courseId, String lessonId) {
        Course course = findCourseById(courseId);
        if (course == null) {
            throw new IllegalArgumentException("Course with ID " + courseId + " not found.");
        }

        for (Lesson lesson : course.getLessons()) {
            if (lesson.getId().equals(lessonId)) {
                return lesson.getAttendance();
            }
        }

        throw new IllegalArgumentException("Lesson with ID " + lessonId + " not found in course " + courseId + ".");
    }

    public List<Course> getAllCourses() {
        return courses;
    }

    public List<Long> getEnrolledStudents(String courseId) {
        Course course = findCourseById(courseId);
        return course != null ? course.getEnrolledStudents() : null;
    }

    public Course updateCourse(Long adminId, String courseId, String title, String description, int duration) {
        User user = userStore.get(adminId);
        if (user == null || !(user instanceof Admin)) {
            throw new IllegalArgumentException("Only admins can update courses.");
        }

        Course course = findCourseById(courseId);
        if (course != null) {
            course.setTitle(title);
            course.setDescription(description);
            course.setDuration(duration);

            for (Long studentId : course.getEnrolledStudents()) {
                notificationService.notifyUser(studentId,
                        "The course " + course.getTitle() + " has been updated. Please check for new details.");
            }
            return course;
        }
        return null;
    }

    public void deleteCourse(Long adminId, String courseId) {
        User user = userStore.get(adminId);
        if (user == null || !(user instanceof Admin)) {
            throw new IllegalArgumentException("Only admins can delete courses.");
        }

        Course course = findCourseById(courseId);
        if (course == null) {
            throw new IllegalArgumentException("Course with ID " + courseId + " does not exist.");
        }

        for (Long studentId : course.getEnrolledStudents()) {
            notificationService.notifyUser(studentId,
                    "The course " + course.getTitle() + " has been deleted.");
        }

        if (course.getInstructor() != null) {
            notificationService.notifyUser(course.getInstructor().getId(),
                    "The course " + course.getTitle() + " you were assigned to teach has been deleted.");
        }

        courses.remove(course);
    }
    public boolean bookmarkCourse(Long studentId, String courseId) {
        User user = userStore.get(studentId);
        Course course = findCourseById(courseId);

        if (user == null || !(user.getRole().equalsIgnoreCase("student"))) {
            throw new IllegalArgumentException("Only students can bookmark courses.");
        }
        if (course == null) {
            throw new IllegalArgumentException("Course not found.");
        }

        return user.getBookmarkedCourses().add(courseId);
    }

    public Set<Course> getBookmarkedCourses(Long studentId) {
        User user = userStore.get(studentId);
        if (user == null || !(user.getRole().equalsIgnoreCase("student"))) {
            throw new IllegalArgumentException("Student not found.");
        }

        return user.getBookmarkedCourses().stream()
                .map(this::findCourseById)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public List<Course> searchCourses(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return courses.stream()
                .filter(course ->
                        course.getTitle().toLowerCase().contains(lowerKeyword) ||
                                course.getDescription().toLowerCase().contains(lowerKeyword) ||
                                (course.getInstructor() != null &&
                                        course.getInstructor().getName().toLowerCase().contains(lowerKeyword))
                )
                .collect(Collectors.toList());
    }



}
