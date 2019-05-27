package com.chess.chessapi.controller;

import com.chess.chessapi.constant.AppRole;
import com.chess.chessapi.entities.Notification;
import com.chess.chessapi.exception.ResourceNotFoundException;
import com.chess.chessapi.model.JsonResult;
import com.chess.chessapi.model.PagedList;
import com.chess.chessapi.security.UserPrincipal;
import com.chess.chessapi.services.NotificationService;
import com.chess.chessapi.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class NotificationController {
    @Autowired
    private UserService userService;
    @Autowired
    private NotificationService notificationService;

    @GetMapping("/notification/getNotification")
    @PreAuthorize("hasAnyAuthority('ROLE_INSTRUCTOR','ROLE_LEARNER','ROLE_ADMIN')")
    public JsonResult getNotification(@RequestParam("page") int page,@RequestParam("pageSize") int pageSize,
                                      boolean sortIsViewed){
        UserPrincipal currentUser = userService.getCurrentUser();

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(currentUser.getAuthorities());
        String role = AppRole.ROLE_LEARNER;
        for (GrantedAuthority authority:
             authorities) {
            role = authority.toString();
        }
        Page<Notification> listNofication = null;
        try{
            listNofication = notificationService
                    .getPagination(page,pageSize, role,currentUser.getId().toString(),sortIsViewed);
        }catch (IllegalArgumentException ex){
            throw new ResourceNotFoundException("Page","number",page);
        }
        PagedList<Notification> data = new PagedList<>(listNofication.getTotalPages()
                ,listNofication.getTotalElements(),listNofication.getContent());
        return new JsonResult("",data);
    }
}