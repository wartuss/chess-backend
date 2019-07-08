package com.chess.chessapi.controllers;

import com.chess.chessapi.constants.AppMessage;
import com.chess.chessapi.constants.AppRole;
import com.chess.chessapi.entities.Notification;
import com.chess.chessapi.exceptions.ResourceNotFoundException;
import com.chess.chessapi.models.JsonResult;
import com.chess.chessapi.models.PagedList;
import com.chess.chessapi.security.UserPrincipal;
import com.chess.chessapi.services.NotificationService;
import com.chess.chessapi.services.UserService;
import com.chess.chessapi.viewmodels.UpdateIsViewedNotification;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/notification")
@Api(value = "Notification Management")
public class NotificationController {
    @Autowired
    private UserService userService;
    @Autowired
    private NotificationService notificationService;

    @ApiOperation(value = "Get current user notification pagings")
    @GetMapping("/get-current-user-notifications-pagination")
    @PreAuthorize("isAuthenticated()")
    public JsonResult getNotifications(@RequestParam("page") int page,@RequestParam("pageSize") int pageSize,
                                      boolean sortIsViewed){
        UserPrincipal currentUser = this.userService.getCurrentUser();

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(currentUser.getAuthorities());
        long role_id = AppRole.ROLE_LEARNER;
        for (GrantedAuthority authority:
             authorities) {
            role_id = Integer.parseInt(authority.toString());
        }
        Page<Notification> listNofication = null;
        try{
            listNofication = this.notificationService
                    .getPagination(page,pageSize, role_id,currentUser.getId().toString(),sortIsViewed);
        }catch (IllegalArgumentException ex){
            throw new ResourceNotFoundException("Page","number",page);
        }
        PagedList<Notification> data = new PagedList<>(listNofication.getTotalPages()
                ,listNofication.getTotalElements(),listNofication.getContent());
        return new JsonResult(null,data);
    }

    @ApiOperation(value = "update is view by list notification ids")
    @PutMapping("/update-is-viewed")
    @PreAuthorize("isAuthenticated()")
    public JsonResult updateIsViewed(@RequestBody @Valid UpdateIsViewedNotification updateIsViewedNotification, BindingResult bindingResult){
        String message = "";
        boolean isSuccess = true;
        if(bindingResult.hasErrors()){
            FieldError fieldError = (FieldError)bindingResult.getAllErrors().get(0);
            message = fieldError.getDefaultMessage();
            isSuccess = false;
        }else {
            try{
                this.notificationService.updateIsView(updateIsViewedNotification.getNotificationIds());
                message = AppMessage.getMessageSuccess(AppMessage.UPDATE,AppMessage.NOTIFICATION);
            }catch (DataIntegrityViolationException ex){
                isSuccess = false;
                message = AppMessage.getMessageFail(AppMessage.UPDATE,AppMessage.NOTIFICATION);
            }
        }

        return new JsonResult(message,isSuccess);
    }
}
