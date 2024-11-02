package com.padaks.todaktodak.member.service;

import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CustomMemberDetailsService implements UserDetailsService {

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String memberEmail) throws UsernameNotFoundException {
        System.out.println("로드라는 것은 " + memberEmail);

        Member member = memberRepository.findByMemberEmail(memberEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + memberEmail));
        System.out.println("멤버 로드라는 것은 " + member.getMemberEmail());

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(member.getRole().getKey()));
// loadUserByUsername 메서드에 로깅 추가
        System.out.println("Granted Authorities: " + authorities);

        return new org.springframework.security.core.userdetails.User(member.getMemberEmail(), member.getPassword(), authorities);
    }
}
