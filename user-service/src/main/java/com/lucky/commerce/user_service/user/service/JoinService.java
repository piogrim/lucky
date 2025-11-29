package com.lucky.commerce.user_service.user.service;

import com.lucky.commerce.user_service.user.domain.Member;
import com.lucky.commerce.user_service.user.domain.MemberRepository;
import com.lucky.commerce.user_service.user.dto.JoinDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class JoinService {

    private final MemberRepository memberRepository;

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public JoinService(final MemberRepository memberRepository, final BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.memberRepository = memberRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public void joinProcess(JoinDTO joinDTO) {
        String username = joinDTO.getUsername();
        String password = joinDTO.getPassword();

        boolean isExist = memberRepository.existsByUsername(username);
        if (isExist) {
            return;
        }

        Member member = new Member();
        member.setUsername(username);
        member.setPassword(bCryptPasswordEncoder.encode(password));

        //TODO USER, ADMIN 나누기
        member.setRole("ROLE_ADMIN");

        memberRepository.save(member);
    }
}
