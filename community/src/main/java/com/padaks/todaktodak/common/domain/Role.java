package com.padaks.todaktodak.common.domain;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    ADMIN("ROLE_ADMIN", "관리자"),
    HOSPITAL("ROLE_HOSPITAL", "병원관리자"),
    DOCTOR("ROLE_DOCTOR", "의사"),
    NONUSER("ROLE_NONUSER", "미인증관리자"),
    MEMBER("ROLE_MEMBER", "회원");
    private final String key;
    private final String string;
}
