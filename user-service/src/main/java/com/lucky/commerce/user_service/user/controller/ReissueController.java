package com.lucky.commerce.user_service.user.controller;

import com.lucky.commerce.user_service.user.service.ReissueService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReissueController {

    private final ReissueService reissueService;

    @Autowired
    public ReissueController(ReissueService reissueService) {
        this.reissueService = reissueService;
    }

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        String refresh = null;

        Cookie[] cookies = request.getCookies();

        if(cookies == null){
            return ResponseEntity.badRequest().build();
        }

        for(Cookie cookie : cookies){
            if(cookie.getName().equals("refresh")){
                refresh = cookie.getValue();
                break;
            }
        }

        String access = reissueService.reissue(refresh);

        if(access == null){
            return ResponseEntity.badRequest().build();
        }

        response.addHeader("Authorization", "Bearer " + access);

        return ResponseEntity.ok().build();
    }
}
