package com.chess.chessapi.services;

import com.chess.chessapi.constants.AppRole;
import com.chess.chessapi.constants.Common;
import com.chess.chessapi.constants.EntitiesFieldName;
import com.chess.chessapi.constants.ObjectType;
import com.chess.chessapi.entities.*;
import com.chess.chessapi.models.PagedList;
import com.chess.chessapi.repositories.LessonRepository;
import com.chess.chessapi.security.UserPrincipal;
import com.chess.chessapi.utils.ManualCastUtils;
import com.chess.chessapi.utils.TimeUtils;
import com.chess.chessapi.viewmodels.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;
import java.util.List;
import java.util.Optional;

@Service
public class LessonService {
    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private InteractiveLessonService interactiveLessonService;

    @Autowired
    private CourseHasLessonService courseHasLessonService;

    @Autowired
    private UserService userService;

    @Autowired
    private UninteractiveLessonService uninteractiveLessonService;

    @Autowired
    private LearningLogService learningLogService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserHasCourseService userHasCourseService;

    @PersistenceContext
    private EntityManager em;

    private final String CREATE_LESSON_NOTIFICATION_MESSAGE = " đã thêm bài học ";
    private final String UPDATE_LESSON_NOTIFICATION_MESSAGE = " đã cập nhật bài học ";

    //PUBLIC METHOD DEFINED
    public Optional<Lesson> getById(long id){
        return this.lessonRepository.findById(id);
    }

    public List<LessonViewModel> getLessonByCourseId(long courseId){
        //getting courses by userId
        StoredProcedureQuery query = this.em.createNamedStoredProcedureQuery("getLessonByCourseId");
        query.setParameter("courseId",courseId);

        query.execute();
        //end getting courses by userid

        return ManualCastUtils.castListObjectToLessonViewModel(query.getResultList());
    }

    public long getAuthorLessonByLessonId(long lessonId){
        return this.lessonRepository.findLessonAuthorByLessonId(lessonId);
    }

    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public long createInteractiveLesson(InteractiveLessonCreateViewModel lessonViewModel, long userId){
        //Create Lesson
        Lesson savedLesson = this.createLesson(lessonViewModel.getName(),lessonViewModel.getDescription(),userId,ObjectType.INTERACTIVE_LESSON);
        //create interactive lesson
        InteractiveLesson interactiveLesson = new InteractiveLesson();
        interactiveLesson.setInteractiveLessonId(0);
        interactiveLesson.setLesson(savedLesson);
        interactiveLesson.setSteps(lessonViewModel.getInteractiveLesson().getSteps());
        interactiveLesson.setInitCode(lessonViewModel.getInteractiveLesson().getInitCode());

        this.interactiveLessonService.create(interactiveLesson);

        //create mapping course has lesson in case has course id
        this.createLessonCourseMapping(lessonViewModel.getCourseId(),savedLesson.getLessonId()
                ,savedLesson.getName(),ObjectType.INTERACTIVE_LESSON);
        return savedLesson.getLessonId();
    }

    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public long createUninteractiveLesson(UninteractiveLessonCreateViewModel uninteractiveLessonCreateViewModel,long userId){
        //Create Lesson
        Lesson savedLesson = this.createLesson(uninteractiveLessonCreateViewModel.getName(),
                uninteractiveLessonCreateViewModel.getDescription(),userId, ObjectType.UNINTERACTIVE_LESSON);
        //create uninteractive lesson
        UninteractiveLesson uninteractiveLesson = new UninteractiveLesson();
        uninteractiveLesson.setUninteractiveLessonId(0);
        uninteractiveLesson.setContent(uninteractiveLessonCreateViewModel.getContent());
        uninteractiveLesson.setLesson(savedLesson);
        this.uninteractiveLessonService.create(uninteractiveLesson);

        //create mapping course has lesson in case has course id
        this.createLessonCourseMapping(uninteractiveLessonCreateViewModel.getCourseId(),savedLesson.getLessonId()
                ,savedLesson.getName(),ObjectType.UNINTERACTIVE_LESSON);
        return savedLesson.getLessonId();
    }

    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void updateInteractiveLesson(InteractiveLessonUpdateViewModel lessonViewModel){
        //update lesson
        this.updateLesson(lessonViewModel.getLessonId(),lessonViewModel.getName(),lessonViewModel.getDescription(),ObjectType.INTERACTIVE_LESSON);

        //update interactive lesson info
        this.interactiveLessonService.update(lessonViewModel.getInteractiveLesson().getInteractiveLessonId()
                ,lessonViewModel.getInteractiveLesson().getInitCode(),ManualCastUtils.castListStepToJson(lessonViewModel.getInteractiveLesson().getSteps()));

    }

    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void updateUninteractiveLesson(UninteractiveLessonUpdateViewModel uninteractiveLessonUpdateViewModel){
        //update lesson
        this.updateLesson(uninteractiveLessonUpdateViewModel.getLessonId(),
                uninteractiveLessonUpdateViewModel.getName(),uninteractiveLessonUpdateViewModel.getDescription(),ObjectType.UNINTERACTIVE_LESSON);

        //update uninteractive lesson info
        this.uninteractiveLessonService.update(uninteractiveLessonUpdateViewModel.getUninteractiveLesson().getUninteractiveLessonId(),
                uninteractiveLessonUpdateViewModel.getUninteractiveLesson().getContent());

    }

    public void updateLesson(long lessonId,String name,String description,int lessonType){
        this.lessonRepository.update(lessonId,name,description);
        this.sendNotificationForLearner(lessonId
                ,UPDATE_LESSON_NOTIFICATION_MESSAGE + name,lessonType);
    }

    public PagedList<LessonViewModel> getAllByOwner(int pageIndex, int pageSize, String name, long userId,String sortBy,String sortDirection){
        StoredProcedureQuery storedProcedureQuery = this.em.createNamedStoredProcedureQuery("getLessonPaginationByUserid");
        Common.storedProcedureQueryPaginationSetup(storedProcedureQuery,pageIndex,pageSize,sortBy,sortDirection);
        storedProcedureQuery.setParameter("userId",userId);
        storedProcedureQuery.setParameter("lessonName",name);


        storedProcedureQuery.execute();

        List<Object[]> rawData = storedProcedureQuery.getResultList();
        final long totalElements = Long.parseLong(storedProcedureQuery.getOutputParameterValue("totalElements").toString());
        return this.fillDataToPagination(rawData,totalElements,pageSize);
    }

    public boolean checkPermissionModifyLesson(long lessonId){
        UserPrincipal userPrincipal = this.userService.getCurrentUser();
        if(userPrincipal != null){
            long ownerLessonId = this.lessonRepository.findLessonAuthorByLessonId(lessonId);
            if(ownerLessonId == userPrincipal.getId()){
                return true;
            }
        }

        return false;
    }

    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void removeLesson(Lesson lesson){
        //remove all learning log
        this.learningLogService.deleteAllByLessonId(lesson.getLessonId());

        //delete mapping with course
        this.courseHasLessonService.deleteAllByLessonId(lesson.getLessonId());
        //delete lesson
        this.lessonRepository.delete(lesson);
    }

    public boolean checkPermissionViewLesson(long userId,long lessonId){
        StoredProcedureQuery storedProcedureQuery = this.em.createNamedStoredProcedureQuery("checkPermssionToViewLesson");
        storedProcedureQuery.setParameter("userId",userId);
        storedProcedureQuery.setParameter("lessonId",lessonId);
        storedProcedureQuery.setParameter("hasPermission",true);

        storedProcedureQuery.execute();

        return Boolean.parseBoolean(storedProcedureQuery.getOutputParameterValue("hasPermission").toString());
    }

    public boolean isExist(long lessonId){
        return this.lessonRepository.existsById(lessonId);
    }
    //END PUBLIC METHOD DEFINED

    //PRIVATE METHOD DEFINED
    private PagedList<LessonViewModel> fillDataToPagination(List<Object[]> rawData,long totalElements,int pageSize){
        final List<LessonViewModel> data = ManualCastUtils.castPageObjectToLessonViewModel(rawData);
        long totalPages = (long) Math.ceil(totalElements / (double) pageSize);
        return new PagedList<LessonViewModel>(Math.toIntExact(totalPages),totalElements,data);
    }

    private Lesson createLesson(String name,String description,long userId,int type){
        Lesson lesson = new Lesson();
        lesson.setName(name);
        lesson.setCreatedDate(TimeUtils.getCurrentTime());
        lesson.setLessonType(type);
        lesson.setDescription(description);
        User user = new User();
        user.setUserId(userId);
        lesson.setUser(user);
        return this.lessonRepository.save(lesson);
    }

    private void sendNotificationForLearner(long lessonId,String content,int lessonType){
        //allow send continue even errors occurs
        List<CourseForNotificationViewModel> courseForNotificationViewModels
                = this.courseService.getCourseForNotificationByListCourseId(this.courseHasLessonService.getListCourseIdByLessonId(lessonId));
        if(courseForNotificationViewModels != null){
            try{
                for(CourseForNotificationViewModel courseForNotificationViewModel:
                courseForNotificationViewModels){
                    List<Long> listUserIds = this.userHasCourseService.getAllLearnerByCourseId
                            (courseForNotificationViewModel.getCourseId(), AppRole.ROLE_LEARNER);
                    for(Long userId: listUserIds){
                        this.notificationService.sendNotificationToUser(content,courseForNotificationViewModel.getCourseName()
                                ,courseForNotificationViewModel.getCourseImage(),lessonType,lessonId,userId,AppRole.ROLE_LEARNER);
                    }
                }
            }catch (Exception ex){
                //will write in logger later
            }
        }
    }

    private void createLessonCourseMapping(long courseId,long lessonId,String lessonName,int lessonType){
        if(courseId != 0){
            int lessonOrder = this.courseHasLessonService.getLastestLessonOrder(courseId);
            lessonOrder++;
            this.courseHasLessonService.create(lessonId
                    ,courseId,lessonOrder);

            this.sendNotificationForLearner(lessonId
                    ,CREATE_LESSON_NOTIFICATION_MESSAGE + lessonName,lessonType);
        }
    }
    //END PRIVATE METHOD DEFINED
}