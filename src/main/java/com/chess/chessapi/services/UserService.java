package com.chess.chessapi.services;

import com.chess.chessapi.constants.*;
import com.chess.chessapi.entities.Certificate;
import com.chess.chessapi.entities.Notification;
import com.chess.chessapi.entities.User;
import com.chess.chessapi.models.PagedList;
import com.chess.chessapi.repositories.NotificationRepository;
import com.chess.chessapi.repositories.UserRepository;
import com.chess.chessapi.security.UserPrincipal;
import com.chess.chessapi.utils.ManualCastUtils;
import com.chess.chessapi.utils.TimeUtils;
import com.chess.chessapi.viewmodels.CourseDetailViewModel;
import com.chess.chessapi.viewmodels.UserDetailViewModel;
import com.chess.chessapi.viewmodels.UserPaginationViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;
import java.util.*;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CertificatesService certificatesService;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CourseService courseService;

    public UserPrincipal getCurrentUser(){
        UserPrincipal user = null;
        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            user = (UserPrincipal) authentication.getPrincipal();
        }catch (ClassCastException ex){

        }
        return user;
    }

    public Optional<User> getUserById(long id){
        return this.userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email){return userRepository.findByEmail(email);}

    public String register(User user, String redirectClient){
        String redirectUri = "";
        if(user.getRoleId() == AppRole.ROLE_INSTRUCTOR){
            this.registerInstructor(user);
        }else {
            this.registerLearner(user);
        }

        redirectUri = redirectClient != null ? redirectClient : "/";

        this.setUserRoleAuthentication(user);

        this.userRepository.save(user);

        return redirectUri;
    }

    public void updateProfile(User user){
        this.userRepository.updateProfile(user.getUserId(),user.getFullName(),user.getAchievement());

        //handle cetificate update
        List<Certificate> oldCetificates = this.certificatesService.findAllByUserId(user.getUserId());

        this.certificatesService.updateCertifications(oldCetificates,user.getCetificates());
    }

    public PagedList<UserPaginationViewModel> getPagination(int page, int pageSize, String email, String role, String isActive){
        PageRequest pageable =  null;
        pageable = PageRequest.of(page - 1,pageSize, Sort.by(EntitiesFieldName.USER_CREATED_DATE).descending());

        Page<Object> rawData = null;
        if(!role.isEmpty()){
            rawData = this.userRepository.findAllByFullNameFilterRole(pageable,email,'%' + role + '%');
        }else if(!isActive.isEmpty()){
            rawData = this.userRepository.findAllByFullNameFilterStatus(pageable,email,Boolean.valueOf(isActive));
        }else if(!role.isEmpty() && !isActive.isEmpty()){
            rawData = this.userRepository.findAllByFullNameFilterRoleAndStatus(pageable,email,'%' + role + '%',Boolean.valueOf(isActive));
        }else{
            rawData = this.userRepository.findAllByFullNameCustom(pageable,email);
        }

        return this.fillDataToPaginationCustom(rawData);
    }

    public void updateStatus(User user,long userId,boolean isActive){
        //notification send to user
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setViewed(false);
        notification.setObjectTypeId(ObjectType.USER);
        notification.setContent(isActive ? AppMessage.UPDATE_USER_STATUS_ACTIVE : AppMessage.UPDATE_USER_STATUS_INACTIVE);
        notification.setCreateDate(TimeUtils.getCurrentTime());
        notification.setObjectId(userId);
        notification.setRoleTarget(user.getRoleId());
        notification.setObjectName(user.getEmail());
        this.notificationRepository.save(notification);
        this.userRepository.updateStatus(userId,isActive);
    }

    public void getUserDetails(User user){
        if(user != null){
            user.setCourseDetailViewModels(this.courseService.getCourseDetailsByUserId(user.getUserId()));
        }
    }

    public boolean checkPermissionModify(long userId){
        UserPrincipal currentUser = this.getCurrentUser();
        if(userId == currentUser.getId()){
            return true;
        }
        return false;
    }

    public List<UserDetailViewModel> getUserDetailsByCourseId(long courseId){
        //getting users by courseid only get the in-process
        StoredProcedureQuery query = this.em.createNamedStoredProcedureQuery("getUsersByCourseid");
        query.setParameter("courseId",courseId);
        query.setParameter("userHasCourseStatusId",Status.USER_HAS_COURSE_STATUS_IN_PROCESS);
        query.execute();

        //end getting users by courseid
        return ManualCastUtils.castListObjectToUserDetailsFromGetUsersByCourseid(query.getResultList());
    }

    // private method
    private void setUserRoleAuthentication(User user){
        List<GrantedAuthority> authorities = Collections.
                singletonList(new SimpleGrantedAuthority(Long.toString(user.getRoleId())));
        UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        authentication.setDetails(authentication);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void registerLearner(User user){
        user.setActive(Status.ACTIVE);
        user.setRoleId(AppRole.ROLE_LEARNER);
    }

    private void registerInstructor(User user){
        user.setActive(Status.INACTIVE);
        user.setRoleId(AppRole.ROLE_INSTRUCTOR);
        // create notification for admin
        Notification notification = new Notification();
        notification.setObjectTypeId(ObjectType.USER);
        notification.setObjectName(user.getEmail());
        notification.setObjectId(user.getUserId());
        notification.setContent(AppMessage.CREATE_NEW_USER_AS_INSTRUCTOR);
        notification.setCreateDate(TimeUtils.getCurrentTime());
        notification.setViewed(false);
        notification.setRoleTarget(AppRole.ROLE_ADMIN);
        this.notificationRepository.save(notification);
    }

    private PagedList<UserPaginationViewModel> fillDataToPaginationCustom(Page<Object> rawData){
        final List<UserPaginationViewModel> content = ManualCastUtils.castPageObjectsToUser(rawData);
        final int totalPages = rawData.getTotalPages();
        final long totalElements = rawData.getTotalElements();
        return new PagedList<UserPaginationViewModel>(totalPages,totalElements,content);
    }
    // end private method
}
