package com.chess.chessapi.services;

import com.chess.chessapi.entities.CourseHasLesson;
import com.chess.chessapi.repositories.CourseHasLessonRepository;
import com.chess.chessapi.utils.ManualCastUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseHasLessonService {
    @Autowired
    private CourseHasLessonRepository courseHasLessonRepository;

    //PUBLIC METHOD DEFINED
    public void create(long lessonId,long courseId,int lessonOrdered){
        this.courseHasLessonRepository.create(lessonId,courseId,lessonOrdered);
    }

    public CourseHasLesson getByLessonIdAndCourseId(long lessonId,long courseId){
        return this.courseHasLessonRepository.findByLessonIdAndCourseId(lessonId,courseId);
    }

    public void delete(CourseHasLesson courseHasLesson){
        this.courseHasLessonRepository.delete(courseHasLesson);
    }

    public void removeLessonFromCourse(long lessonId,long courseId){
        CourseHasLesson courseHasLesson = this.getByLessonIdAndCourseId(lessonId,courseId);
        if(courseHasLesson != null){
            this.delete(courseHasLesson);
        }
    }

    public int getLastestLessonOrder(long courseId){
        try{
            return this.courseHasLessonRepository.findLastestLessonOrderedByCourseId(courseId);
        }catch (NullPointerException ex){
            return 0;
        }

    }

    public void deleteAllByLessonId(long lessonId){
        this.courseHasLessonRepository.deleteAllByLessonId(lessonId);
    }

    public List<Long> getListCourseIdByLessonId(long lessonId){
        return this.courseHasLessonRepository.findListCourseIdByLessonId(lessonId);
    }
    //END PUBLIC METHOD DEFINED
}