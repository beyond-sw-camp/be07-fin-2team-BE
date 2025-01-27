package com.padaks.todaktodak.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.padaks.todaktodak.common.dto.CommonErrorDto;
import com.padaks.todaktodak.common.dto.CommonResDto;
import com.padaks.todaktodak.common.enumdir.DayOfHoliday;
import com.padaks.todaktodak.member.dto.DoctorInfoDto;
import com.padaks.todaktodak.member.domain.Address;
import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.dto.*;
import com.padaks.todaktodak.member.service.MemberAuthService;
import com.padaks.todaktodak.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
@Slf4j
public class MemberController {

    private final MemberService memberService;
    private final MemberAuthService memberAuthService;

    @GetMapping("/get/member")
    public MemberPayDto getMember() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberService.findByMemberEmail(email);
        log.info("Member data: {}", member);
        return new MemberPayDto(member.getMemberEmail(), member.getName(), member.getPhoneNumber(), member.getRole(), member.getReportCount(), member.getHospitalId(), member.getProfileImgUrl());
    }

    @GetMapping("/get/{email}")
    public MemberResDto getMemberByEmail(@PathVariable String email) {
        Member member = memberService.findByMemberEmail(email);
        return new MemberResDto().fromEntity(member);
    }

    @GetMapping("/doctor/{email}")
    public ResponseEntity<CommonResDto> getDoctorInfo(@PathVariable String email){
        DoctorInfoDto dto = memberService.doctorInfo(email);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "의사정보 조회 성공", dto), HttpStatus.OK);
    }

    @GetMapping("/id/{memberId}")
    public ResponseEntity<?> getMemberById(@PathVariable Long memberId){
        MemberDetailResDto memberDetailResDto = memberService.findByMemberId(memberId);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK,"회원정보 조회성공", memberDetailResDto), HttpStatus.OK);
    }

    // 회원가입
    @PostMapping("/create")
    public ResponseEntity<?> register(MemberSaveReqDto saveReqDto,
                                      @RequestPart(value = "image", required = false) MultipartFile imageSsr) {
        try {
            memberService.create(saveReqDto, imageSsr);
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "회원가입 성공", null));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonResDto(HttpStatus.BAD_REQUEST, "회원가입에 실패했습니다: " + e.getMessage(), null));
        }
    }

    @PostMapping("/doctor/register")
    @PreAuthorize("hasAnyRole('HOSPITAL', 'ADMIN')")
    public ResponseEntity<?> registerDoctor(@ModelAttribute DoctorSaveReqDto doctorSaveReqDto) {
        Member member = memberService.createDoctor(doctorSaveReqDto);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "의사등록성공", member.getId()), HttpStatus.OK);
    }

    @PostMapping("/hospital-admin/register")
    public ResponseEntity<?> registerHospitalAdmin(@RequestBody HospitalAdminSaveReqDto dto) {
        Member unAcceptHospitalAdmin = memberService.registerHospitalAdmin(dto);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "병원 admin 등록성공(미승인)", unAcceptHospitalAdmin.getId()), HttpStatus.OK);
    }

    @PutMapping("/hospital-admin/accept")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> acceptHospitalAdmin(@RequestBody String email) {
        memberService.acceptHospitalAdmin(email);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "병원 admin 회원가입 승인완료", null), HttpStatus.OK);
    }

    @PostMapping("/doctor-admin/register")
    @PreAuthorize("hasAnyRole('HOSPITAL', 'ADMIN')")
    public ResponseEntity<?> doctorAdmin(@RequestBody DoctorAdminSaveReqDto dto) {
        Member doctorAdminCreate = memberService.doctorAdminCreate(dto);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "해당 병원의 의사 등록 성공", doctorAdminCreate.getId()), HttpStatus.OK);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MemberLoginDto loginDto) {
        try {
            String token = memberService.login(loginDto);
            System.out.println("Generated JWT Token: " + token);
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "로그인 성공", token));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new CommonErrorDto(HttpStatus.FORBIDDEN, "토닥 관계자만 로그인이 가능합니다. 필요합니다."));
        } catch (RuntimeException e) {
            e.printStackTrace();
            if (e.getMessage().contains("비활성화 상태")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new CommonErrorDto(HttpStatus.UNAUTHORIZED, e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new CommonErrorDto(HttpStatus.UNPROCESSABLE_ENTITY, "잘못된 이메일/비밀번호입니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR, "로그인 중 오류가 발생했습니다."));
        }
    }
    @PostMapping("/hospital/login")
    public ResponseEntity<?> hospitalLogin(@RequestBody MemberLoginDto loginDto) {
        try {
            String token = memberService.hospitalLogin(loginDto);
            System.out.println("Generated JWT Token: " + token);
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "로그인 성공", token));
        } catch (SecurityException e) {
            // 관계자만 접근 가능한 경우 - 403 Forbidden
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new CommonErrorDto(HttpStatus.FORBIDDEN, "관계자만 로그인이 가능합니다."));
        } catch (AuthenticationException e) {
            // 인증되지 않은 회원 - 401 Unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonErrorDto(HttpStatus.UNAUTHORIZED, "인증되지 않은 회원입니다."));
        } catch (RuntimeException e) {
            e.printStackTrace();
            if (e.getMessage().contains("비활성화 상태")) {
                // 계정이 비활성화된 경우 - 423 Locked
                return ResponseEntity.status(HttpStatus.LOCKED)
                        .body(new CommonErrorDto(HttpStatus.LOCKED, e.getMessage()));
            }
            // 잘못된 이메일/비밀번호 - 422 Unprocessable Entity
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new CommonErrorDto(HttpStatus.UNPROCESSABLE_ENTITY, "잘못된 이메일/비밀번호입니다."));
        } catch (Exception e) {
            e.printStackTrace();
            // 기타 오류 - 500 Internal Server Error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR, "로그인 중 오류가 발생했습니다."));
        }
    }

    // 이메일 인증 코드 발송
    @PostMapping("/verification/email/send")
    public ResponseEntity<?> sendVerificationEmail(@RequestBody SignUpVerificationSendEmailDtoRequest requestDto) {
        try {
            EmailVerificationDtoResponse response = memberAuthService.sendSignUpVerificationEmail(requestDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonErrorDto(HttpStatus.BAD_REQUEST, "이메일 인증 번호 발송에 실패했습니다: " + e.getMessage()));
        }
    }

    // 이메일 인증 확인 로직
    @PostMapping("/verification/email/check")
    public ResponseEntity<?> checkVerificationEmail(@RequestBody EmailVerificationDto requestDto) {
        try {
            EmailVerificationDto emailVerificationDto = memberAuthService.checkSignUpVerificationEmail(
                    requestDto.getVerificationToken(), requestDto.getVerificationNumber());
            // 인증 성공 응답
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "이메일 인증이 성공적으로 완료되었습니다.", emailVerificationDto));
        } catch (IllegalArgumentException e) {
            // 인증 실패 응답
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonErrorDto(HttpStatus.BAD_REQUEST, "이메일 인증에 실패했습니다: " + e.getMessage()));
        } catch (Exception e) {
            // 기타 오류 응답
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 인증 처리 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/edit-info")
    public ResponseEntity<?> getEditUserInfo(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Member member = memberService.findByMemberEmail(userDetails.getUsername());
            MemberUpdateReqDto memberUpdateReqDto = MemberUpdateReqDto.fromEntity(member);
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "회원 정보 수정 조회 성공", memberUpdateReqDto));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonResDto(HttpStatus.UNAUTHORIZED, "Invalid token", null));
        }
    }

    // 프로필 이미지 업데이트
    @PostMapping("/update/profileImage")
    public ResponseEntity<String> updateProfileImage(@RequestParam("profileImage") MultipartFile profileImage) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Member member = memberService.findByMemberEmail(email);
            memberService.updateProfileImage(member, profileImage);

            return ResponseEntity.ok("프로필 이미지가 성공적으로 업데이트되었습니다.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("프로필 이미지 업데이트에 실패했습니다: " + e.getMessage());
        }
    }

    // 프로필 이미지 업데이트
    @PostMapping("/update/doctor/profileImage")
    public ResponseEntity<String> updateProfileImage(@RequestParam("email") String email, @RequestParam("profileImage") MultipartFile profileImage) {
        try {
            Member member = memberService.findByMemberEmail(email);
            memberService.updateProfileImage(member, profileImage);

            return ResponseEntity.ok("프로필 이미지가 성공적으로 업데이트되었습니다.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("프로필 이미지 업데이트에 실패했습니다: " + e.getMessage());
        }
    }

    // 회원 정보 수정
    @PostMapping("/edit-info")
    public ResponseEntity<?> editMemberInfo(
            @RequestParam("name") String name,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("address") String addressJson, // JSON 문자열로 받은 주소
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword) {
        try {
            // JSON 문자열을 Address 객체로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            Address address = objectMapper.readValue(addressJson, Address.class);

            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Member member = memberService.findByMemberEmail(email);

            // DTO 생성 및 값 설정
            MemberUpdateReqDto updateReqDto = MemberUpdateReqDto.builder()
                    .name(name)
                    .phoneNumber(phoneNumber)
                    .address(address)
                    .profileImage(profileImage)
                    .password(password)
                    .confirmPassword(confirmPassword)
                    .build();

            // 회원 정보 업데이트
            memberService.updateMember(member, updateReqDto); // profileImage 포함 업데이트

            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "회원 정보를 수정하였습니다.", member));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonResDto(HttpStatus.NOT_FOUND, "회원 정보가 존재하지 않습니다. -> " + e.getMessage(), null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonResDto(HttpStatus.BAD_REQUEST, "회원 정보 수정에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 의사 정보 수정
    @PostMapping("/edit-doctor")
    public ResponseEntity<?> editDoctorInfo(@ModelAttribute DoctorUpdateReqdto dto) {
        try {
//            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Member doctor = memberService.findByMemberEmail(dto.getMemberEmail());
            memberService.updateDoctor(doctor, dto);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "의사 정보를 수정하였습니다.", doctor);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.NOT_FOUND, "의사 정보가 존재하지 않습니다. -> " + e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.NOT_FOUND);
        }
    }

    // 회원 탈퇴
    @PostMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal UserDetails userDetails, @RequestBody Map<String, String> request) {
        try {
            String confirmation = request.get("confirmation");
            if (!"토닥 회원 탈퇴에 동의합니다".equals(confirmation)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new CommonResDto(HttpStatus.BAD_REQUEST, "회원 탈퇴 문구가 올바르지 않습니다.", null));
            }
            memberService.deleteAccount(userDetails.getUsername());
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "회원 탈퇴에 성공하였습니다.", userDetails.getUsername()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonResDto(HttpStatus.BAD_REQUEST, "회원 탈퇴에 실패하였습니다", null));
        }
    }

    //병원 관리자 의사 삭제
    @PostMapping("/delete-doctor")
    @PreAuthorize("hasAnyRole('HOSPITAL', 'ADMIN')")
    public ResponseEntity<?> deleteDoctor(@ModelAttribute DoctorDeleteDto dto) {
        try {
            if (!"의사 삭제 후 동일 이메일로 등록할 수 없음을 동의합니다".equals(dto.getConfirmation())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new CommonResDto(HttpStatus.BAD_REQUEST, "회원 탈퇴 문구가 올바르지 않습니다.", null));
            }
            memberService.deleteDoctor(dto.getDoctorEmail());
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "의사 삭제에 성공하였습니다.",null));
        }catch (EntityNotFoundException e){
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonResDto(HttpStatus.NOT_FOUND, "선택한 의사를 찾을 수 없습니다", e.getMessage()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonResDto(HttpStatus.BAD_REQUEST, "의사 삭제에 실패하였습니다", e.getMessage()));
        }
    }

    // java 라이브러리 메일 서비스 : 인증 코드 전송
    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody EmailVerificationDto verificationDto) {
        memberService.sendVerificationEmail(verificationDto.getMemberEmail());
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "인증 코드 전송에 성공하였습니다.", null));
    }

    // java 라이브러리 메일 서비스 : 인증 코드 확인
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody JavaEmailVerificationDto verificationDto) {
        try {
            boolean isVerified = memberService.verifyEmail(verificationDto.getMemberEmail(), verificationDto.getCode());
            if (isVerified) {
                return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "이메일 인증에 성공하였습니다.", isVerified));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new CommonResDto(HttpStatus.BAD_REQUEST, "이메일 인증에 실패했습니다.", null));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonResDto(HttpStatus.BAD_REQUEST, "이메일 인증에 실패했습니다: " + e.getMessage(), null));
        }
    }
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody JavaEmailVerificationDto verificationDto) {
        try {
            boolean isVerified = memberService.verifyCode(verificationDto.getMemberEmail(), verificationDto.getCode());
            if (isVerified) {
                return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "이메일 인증에 성공하였습니다.", isVerified));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new CommonResDto(HttpStatus.BAD_REQUEST, "이메일 인증에 실패했습니다.", null));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonResDto(HttpStatus.BAD_REQUEST, "이메일 인증에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // member list
//    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> memberList(
            @RequestParam(required = false) Boolean verified, // true/false
            @RequestParam(required = false) Boolean deleted,  // true: 탈퇴, false: 정상
            @RequestParam(required = false) String role,  // true: 탈퇴, false: 정상
            Pageable pageable) {
        System.out.println("관리" + SecurityContextHolder.getContext().getAuthentication());
        Page<MemberListResDto> memberListResDtos = memberService.memberList(verified, deleted, role, pageable);
        CommonResDto dto = new CommonResDto(HttpStatus.OK, "회원목록을 조회합니다.", memberListResDtos);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }


    // 관리자 멤버 검색
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchMembers(
            @RequestParam String query,
            Pageable pageable) {
        Page<MemberListResDto> members = memberService.adminSearchMembers(query, pageable);
        CommonResDto dto = new CommonResDto(HttpStatus.OK, "검색 결과", members);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }


    @GetMapping("/doctorList")
    public ResponseEntity<Object> doctorList(Pageable pageable) {
        Page<DoctorListResDto> dtos = memberService.doctorList(pageable);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "의사목록을 조회합니다.", dtos);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

    //병원 별 의사 목록
    @GetMapping("/doctorList/{hospitalId}")
    public ResponseEntity<Object> doctorListByHospital(@PathVariable Long hospitalId, Pageable pageable) {
        Page<DoctorListResDto> dtos = memberService.doctorListByHospital(hospitalId, pageable);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "병원별 의사목록을 조회합니다.", dtos);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

    //병원 별 의사 목록
    @GetMapping("/doctors")
    public ResponseEntity<Object> doctorListByHospitalForAdmin(Pageable pageable) {
        try{
            Page<DoctorListResDto> dtos = memberService.doctorListByHospitalForAdmin(pageable);
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "병원 관리자 용 병원별 의사목록을 조회합니다.", dtos);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.BAD_REQUEST);
        }

    }

    // 비대면 의사 리스트
    @GetMapping("/untact/list/{today}")
    public ResponseEntity<CommonResDto> untactList(@PathVariable DayOfHoliday today, @RequestParam(required = false) String search, @RequestParam(required = false) String sortBy) {
        List<DoctorUntactListDto> dtoList = memberService.untactDoctorList(today, search, sortBy);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "의사리스트 조회 성공", dtoList), HttpStatus.OK);
    }

    // 인기 비대면 의사 리스트
    @GetMapping("/untact/good/list/{today}")
    public ResponseEntity<CommonResDto> famousUntactList(@PathVariable DayOfHoliday today) {
        List<DoctorInfoDto> dtoList = memberService.famousUntactList(today);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "의사리스트 조회 성공", dtoList), HttpStatus.OK);
    }

    @GetMapping("/detail/{email}")
    public Object memberDetail(@PathVariable String email) {
        return memberService.memberDetail(email);
    }

    @GetMapping("/get/hospital")
    private ResponseEntity<?> getMemberTest() {
        try {
            HospitalFeignDto hospitalFeignDto = memberService.getHospital();
            return ResponseEntity.ok(hospitalFeignDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // email 찾기
    @PostMapping("/find/email")
    public ResponseEntity<?> findEmail(@RequestBody MemberFindIdDto findIdDto) {
        try {
            String maskedEmail = memberService.findId(findIdDto);
            if (maskedEmail == null) {
                throw new EntityNotFoundException("해당 이름과 전화번호로 등록된 사용자가 없습니다.");
            }
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "아이디 찾기가 완료되었습니다.", maskedEmail);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            // 존재하지 않는 사용자에 대한 명확한 예외 처리
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.");
            return new ResponseEntity<>(commonErrorDto, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, "이메일 찾기에 실패했습니다.");
            return new ResponseEntity<>(commonErrorDto, HttpStatus.BAD_REQUEST);
        }
    }

    // 비밀번호 재설정 링크 전송
    @PostMapping("/find/password")
    public ResponseEntity<?> findPassword(@RequestBody MemberFindPasswordDto dto) {
        try {
            memberService.sendPasswordResetLink(dto);
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "비밀번호 재설정 링크를 전송하였습니다.", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonResDto(HttpStatus.BAD_REQUEST, "올바른 이메일을 입력해주세요.", null));
        }
    }

    @GetMapping("/reset/password")
    public ResponseEntity<?> showResetPasswordPage(@RequestParam("token") String token) {
        // 토큰 유효성 검사 등 추가 로직 수행 가능

        // 이 단계에서 Vue.js로의 페이지 렌더링을 의도
        // ResponseEntity는 JSON 응답을 반환하는 대신 Vue.js 페이지로 리디렉션합니다.
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/reset/password?token=" + token);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    // 비밀번호 재설정
    @PostMapping("/reset/password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetDto dto) {
        try {
            memberService.resetPassword(dto);
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "비밀번호 재설정에 성공하였습니다.", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommonResDto(HttpStatus.BAD_REQUEST, "비밀번호 재설정에 실패했습니다: " + e.getMessage(), null));
        }
    }
    // 신고 카운트 증가
    @PostMapping("/report/count/{memberEmail}")
    public int reportCountUp(@PathVariable String memberEmail) {
        log.info(memberEmail);
        return memberService.reportCountUp(memberEmail);
    }

    @PostMapping("/member/test")
    public void updateNoShowCount(){
        memberService.updateNoShowCount();
    }

    @GetMapping("/reportCount")
    public ResponseEntity<?> memberReportCount(){
        try {
            int reportCount = memberService.memberReportCount();
            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "사용자의 현재 신고 횟수: " , reportCount);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
        }catch (EntityNotFoundException e){
            e.printStackTrace();
            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.NOT_FOUND, e.getMessage());
            return new ResponseEntity<>(commonErrorDto, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/search/{keyword}")
    public ResponseEntity<CommonResDto> search(@PathVariable String keyword) {
        List<MemberDetailResDto> members = memberService.searchMembers(keyword);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK,"검색 성공",members), HttpStatus.OK);
    }

    @GetMapping("/doctor/detail/{email}")
    public ResponseEntity<CommonResDto> doctorDetail(@PathVariable String email) {
        DoctorDetailDto doctorResDto = memberService.doctorDetail(email);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK,"조회 성공",doctorResDto), HttpStatus.OK);
    }

    @GetMapping("/hospitalName")
    public ResponseEntity<CommonResDto> hospitalName() {
        HospitalInfoDto dto = memberService.getHospitalName();
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK,"조회 성공",dto), HttpStatus.OK);
    }

    // 결제 성공 -> 멤버 role 업데이트 및 토큰 재발급 요청
    @PostMapping("/success")
    public ResponseEntity<?> updateRoleAndGenerateNewToken(@RequestBody PaymentSuccessDto paymentSuccessDto) {
        try {
            // 멤버 역할 업데이트 및 새로운 토큰 생성
            String newToken = memberService.updateMemberRoleAndGenerateNewToken(paymentSuccessDto.getMemberId());
            return ResponseEntity.ok(newToken);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("토큰 갱신 실패");
        }
    }

    @GetMapping("/growup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getMemberCountByMonth() {

        return ResponseEntity.ok(memberService.getMonthlyMemberCount());
    }
}
