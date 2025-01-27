package com.padaks.todaktodak.member.domain;

import com.padaks.todaktodak.common.domain.BaseTimeEntity;
import com.padaks.todaktodak.doctoroperatinghours.dto.DoctorOperatingHoursSimpleResDto;
import com.padaks.todaktodak.member.dto.DoctorListResDto;
import com.padaks.todaktodak.member.dto.MemberListResDto;
import lombok.*;
import com.padaks.todaktodak.childparentsrelationship.domain.ChildParentsRelationship;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    private String name;

    @Column(nullable = false, unique = true)
    private String memberEmail;

    //    @Column(nullable = false)
    private String password;

    @Column
    private String profileImgUrl;

    //    @Column(nullable = false, unique = true)
    private String phoneNumber;

    //    @Column(nullable = false)
    private String ssn;

    //    @Column(nullable = false)
    @Column(length = 255)
    private Address address;

    @ColumnDefault("0")
    private int noShowCount;

    @ColumnDefault("0")
    private int reportCount;

    @Column
    @Enumerated(EnumType.STRING)
    private Role role;

    private String bio;

    private Long hospitalId;


    private String fcmToken;

    @Builder.Default
    private boolean isVerified = false;


    @Builder
    public Member(String name, String email, Role role) {
        this.name = name;
        this.memberEmail = email;
        this.role = role;
    }

    public Member update(String name) {
        this.name = name;
        return this;
    }

    public Member updatePass(String uuid) {
        this.password = uuid;
        return this;
    }

    public Member updateAddress(String temp) {
        this.address = new Address(temp, "", "");
        return this;
    }

    public Member updateName(String name) {
        this.name = name;
        return this;
    }

    public Member updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
        return this;
    }

    // 프로필 이미지 URL 변경
    public void changeProfileImgUrl(String newUrl) {
        this.profileImgUrl = newUrl;
    }

    public void updateVerified() {
        this.isVerified = true;
    }

    // 이름 변경
    public void changeName(String newName) {
        this.name = newName;
    }

    // 전화번호 변경
    public void changePhoneNumber(String newPhoneNumber) {
        this.phoneNumber = newPhoneNumber;
    }

    // 주소 변경
    public void changeAddress(Address newAddress) {
        this.address = newAddress;
    }

    // 비밀번호 변경
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    // 의사 약력 변경
    public void changeBio(String newBio) {
        this.bio = newBio;
    }

    //의사 소속된 병원 변경
    public void changeHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getRoleKey() {
        return this.role.getKey();
    }

    // 탈퇴 처리 메서드
    public void deleteAccount() {
        this.setDeletedTimeAt(LocalDateTime.now());  // 현재 시간을 삭제 시간으로 설정
    }

    //    노쇼 카운트 초기화
    public void clearNoShowCount() {
        this.noShowCount = 0;
    }

    // 유저 목록 조회
    public MemberListResDto listFromEntity() {
        return MemberListResDto.builder()
                .id(this.id)
                .name(this.name)
                .phone(this.phoneNumber)
                .address(this.address)
                .memberEmail(this.memberEmail)
                .profileImgUrl(this.profileImgUrl)
                .isVerified(this.isVerified)
                .deletedAt(this.getDeletedAt())
                .role(this.role)
                .build();
    }

    public DoctorListResDto doctorListFromEntity(List<DoctorOperatingHoursSimpleResDto> operatingHours) {
        return DoctorListResDto.builder()
                .id(this.id)
                .name(this.name)
                .doctorEmail(this.memberEmail)
                .profileImgUrl(this.profileImgUrl)
                .role(this.role)
                .bio(this.bio)
                .isVerified(this.isVerified)
                .operatingHours(operatingHours)
                .build();
    }

    public void resetPassword(String password) {
        this.password = password;
    }

    public void incressNoShowCount() {
        this.noShowCount++;
    }

    // 신고 카운트 증가시키는 메서드
    public int reportCountUp() {
        return this.reportCount++;
    }

    public void updatePhoneNumber(String phone) {
        this.phoneNumber = phone;
    }

    public void updateRole(Role role) {
        this.role = role;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

}
