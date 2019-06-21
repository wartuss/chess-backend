package com.chess.chessapi.viewmodels;

import com.chess.chessapi.entities.UninteractiveLesson;

import javax.validation.constraints.NotNull;
import java.sql.Timestamp;

public class UninteractiveLessonCreateViewModel {
    private long lessonId;
    @NotNull(message = "Name must not be null")
    private String name;

    private Timestamp createdDate;

    @NotNull(message = "Uninteractive Lesson must not be null")
    private UninteractiveLesson uninteractiveLesson;

    private long courseId;

    public long getLessonId() {
        return lessonId;
    }

    public void setLessonId(long lessonId) {
        this.lessonId = lessonId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public UninteractiveLesson getUninteractiveLesson() {
        return uninteractiveLesson;
    }

    public void setUninteractiveLesson(UninteractiveLesson uninteractiveLesson) {
        this.uninteractiveLesson = uninteractiveLesson;
    }

    public long getCourseId() {
        return courseId;
    }

    public void setCourseId(long courseId) {
        this.courseId = courseId;
    }
}
