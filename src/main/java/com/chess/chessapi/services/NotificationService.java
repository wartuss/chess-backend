package com.chess.chessapi.services;

import com.chess.chessapi.constants.AppRole;
import com.chess.chessapi.constants.EntitiesFieldName;
import com.chess.chessapi.entities.Notification;
import com.chess.chessapi.repositories.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    //public method
    public Notification create(Notification notification){
        return notificationRepository.save(notification);
    }

    public Page<Notification> getPagination(int page,int pageSize,long role_id,String userId,boolean sortIsViewed){
        PageRequest pageable =  null;

        if(sortIsViewed){
            pageable = PageRequest.of(page - 1,pageSize, Sort.by(EntitiesFieldName.NOTIFICATION_IS_VIEWED).ascending());
        }else {
            pageable = PageRequest.of(page - 1,pageSize, Sort.by(EntitiesFieldName.NOTIFICATION_CREATED_DATE).descending());
        }

        Page<Notification> notificationPage;
        if(role_id == AppRole.ROLE_ADMIN){
            notificationPage = this.notificationRepository.findAllByRoleWithPagination(pageable,AppRole.ROLE_ADMIN);
        }else if(role_id == AppRole.ROLE_INSTRUCTOR){
            notificationPage = this.notificationRepository.findAllByRoleAndObjectIdWithPagination(pageable,AppRole.ROLE_INSTRUCTOR,userId);
        }else{
            notificationPage = this.notificationRepository.findAllByRoleAndObjectIdWithPagination(pageable,AppRole.ROLE_LEARNER,userId);
        }

        return notificationPage;
    }
    //end public method
}
