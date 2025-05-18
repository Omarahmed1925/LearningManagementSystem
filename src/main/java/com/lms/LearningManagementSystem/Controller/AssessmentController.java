package com.lms.LearningManagementSystem.Controller;
import java.util.List;
import com.lms.LearningManagementSystem.Model.Assessment.*;
import com.lms.LearningManagementSystem.Service.UserService.InstructorService;
import com.lms.LearningManagementSystem.Service.UserService.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/Assessments")
public class AssessmentController {
    private static final String STUDENT_ID_KEY = "studentId";
    private static final String TITLE_KEY = "title";
    private static final String TOTAL_MARKS_KEY = "totalMarks";
    private static final String NUM_QUESTIONS_KEY = "num";
    private static final String ANSWERS_KEY = "answers";
    private static final String DESCRIPTION_KEY = "description";
    private static final String FILE_NAME_KEY = "fileName";
    private static final String MARKS_KEY = "marks";
    private static final String FEEDBACK_KEY = "feedback";

    @PostMapping("/{InstructorId}/quiz")
    public ResponseEntity<Object> createQuiz(@PathVariable Long InstructorId, @RequestBody Map<String, Object> payload) {
        try {
            String title = (String) payload.get(TITLE_KEY);
            int totalMarks = (int) payload.get(TOTAL_MARKS_KEY);
            int num = (int) payload.get(NUM_QUESTIONS_KEY);

            Quiz quiz = InstructorService.createQuiz(InstructorId, title, num, totalMarks);
            return new ResponseEntity<>(quiz, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/quiz/{quizId}/submit")
    public ResponseEntity<?> submitQuizAnswers(@PathVariable Long quizId, @RequestBody Map<String, Object> payload) {
        // Check if the quiz exists
        try {
            Quiz quiz = StudentService.findQuizById(quizId);
            if (quiz == null) {
                return new ResponseEntity<>("Quiz not found", HttpStatus.NOT_FOUND); // Return 404 if quiz not found
            }
            Long studentId = ((Number) payload.get(STUDENT_ID_KEY)).longValue();
            Map<String, String> submission = (Map<String, String>) payload.get(ANSWERS_KEY);

            // Process submission
            StudentService.SubmitQuiz(studentId, quizId, submission);
            int correctAnswersCount = InstructorService.correctAnswersCount(quizId, studentId);
            return new ResponseEntity<>("You got " + correctAnswersCount + " correct answers!", HttpStatus.OK);
        }
        catch (IllegalArgumentException e) {
            // Handle known errors like invalid student or other validations
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Add Question to Quiz
    @PostMapping("/{InstructorId}/create/questions")
    public ResponseEntity<String> addQuestions( @PathVariable Long InstructorId,@RequestBody List<Question> questions) {
        try {
            if (questions == null || questions.isEmpty()) {
                return new ResponseEntity<>("No questions provided!", HttpStatus.BAD_REQUEST);
            }
            InstructorService.addQuestions(InstructorId, questions);
            return new ResponseEntity<>("Questions added successfully!", HttpStatus.OK);
        }
        catch (IllegalArgumentException e) {
            // Handle known errors like invalid instructor ID
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);

        }
    }

    @GetMapping("/quiz/{id}")
    public ResponseEntity<Quiz> getQuizById(@PathVariable Long id) {
        Quiz quiz = StudentService.findQuizById(id);
        if (quiz == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND); // Return 404 if no quiz is found
        }
        return new ResponseEntity<>(quiz, HttpStatus.OK);
    }

    @GetMapping("/{InstructorId}/questions")
    public  ResponseEntity<?> getAllQuestions(@PathVariable Long InstructorId) {
        try {
            List<Question> questions = InstructorService.GetQuestions(InstructorId);

            return new ResponseEntity<>(questions, HttpStatus.OK);

        }
        catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);

        }
    }

    @GetMapping("/quizzes")
    public List<Quiz> GetAllquizzes() {
        return StudentService.GetAllquizzes();
    }

    @PostMapping("/{InstructorId}/assignment")
    public ResponseEntity<?> createAssignment(@PathVariable Long InstructorId,@RequestBody Map<String, Object> payload) {
        try {
            String title = (String) payload.get(TITLE_KEY);
            String description = (String) payload.get(DESCRIPTION_KEY);
            if (title == null || title.isEmpty() || description == null || description.isEmpty()) {
                return new ResponseEntity<>("Title and description must be provided.", HttpStatus.BAD_REQUEST);
            }
            Assignment assignment = InstructorService.createAssignment(InstructorId, title, description);

            return new ResponseEntity<>(assignment, HttpStatus.CREATED);
        }
        catch (IllegalArgumentException e) {
            // Handle known errors like invalid instructor ID
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);

        }
    }

    @PostMapping("/assignment/{assignmentId}/submit")
    public ResponseEntity<?> submitAssignment(@PathVariable Long assignmentId, @RequestBody Map<String, Object> payload) {
        try {
            // Check if the assignment exists
            Assignment assignment = StudentService.findAssignmentById(assignmentId);
            if (assignment == null) {
                return new ResponseEntity<>("Assignment not found", HttpStatus.NOT_FOUND);
            }

            // Extract fileName and studentId from the payload
            String fileName = (String) payload.get(FILE_NAME_KEY);
            if (fileName == null || fileName.isEmpty()) {
                return new ResponseEntity<>("File name must be provided.", HttpStatus.BAD_REQUEST);
            }

            Long studentId = ((Number) payload.get(STUDENT_ID_KEY)).longValue();
            if (studentId == null) {
                return new ResponseEntity<>("Student ID must be provided.", HttpStatus.BAD_REQUEST);
            }

            // Submit the assignment
            StudentService.submitAssignment(assignmentId, fileName, studentId);

            return new ResponseEntity<>("Assignment submitted successfully!", HttpStatus.OK);
        }
        catch (IllegalArgumentException e) {
            // Handle known validation errors
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<Assignment> getAssignmentById(@PathVariable Long assignmentId) {
        Assignment assignment = StudentService.findAssignmentById(assignmentId);
        if (assignment == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND); // Return 404 if assignment not found
        }
        return new ResponseEntity<>(assignment, HttpStatus.OK);
    }


    @GetMapping("/assignments")
    public List<Assignment> GetAllAssignments() {
        return StudentService.GetAllAssignments();
    }


    // Grade Assessment
    @PostMapping("/{InstructorId}/grade")
    public ResponseEntity<String> gradeAssignment(@PathVariable Long InstructorId,@RequestBody Map<String, Object> payload) {
        try {
            // Extract and validate inputs
            Long studentId = payload.get(STUDENT_ID_KEY) != null ? ((Number) payload.get(STUDENT_ID_KEY)).longValue() : null;
            if (studentId == null) {
                return new ResponseEntity<>("Student ID must be provided.", HttpStatus.BAD_REQUEST);
            }
            String marks = (String) payload.get(MARKS_KEY);
            if (marks == null || marks.isEmpty()) {
                return new ResponseEntity<>("Marks must be provided.", HttpStatus.BAD_REQUEST);
            }
            String feedback = (String) payload.get(FEEDBACK_KEY);
            if (feedback == null || feedback.isEmpty()) {
                return new ResponseEntity<>("Feedback must be provided.", HttpStatus.BAD_REQUEST);
            }
            // Grade the assignment
            InstructorService.gradeAssignment(InstructorId, studentId, "Assignment", marks, feedback);
            return new ResponseEntity<>("Assignment graded successfully!", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            // Handle known validation errors
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


}
