package com.padaks.todaktodak.member.repository;

import com.padaks.todaktodak.member.domain.Member;
import com.padaks.todaktodak.member.domain.Role;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByMemberEmail(String memberEmail);
    Optional<Member> findByMemberEmail(String memberEmail);
    Page<Member> findAll(Pageable pageable);
    Page<Member> findByRole(Role role, Pageable pageable);
    Page<Member>findByRoleAndHospitalId(Role role, Long hospitalId, Pageable pageable);

    Optional<Member> findByNameAndPhoneNumber(String name, String phoneNumber);

    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    default Member findByIdOrThrow(Long id){
        return findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 member를 찾을 수 없습니다."));
    }

    // 탈퇴하지 않은 멤버 이메일찾기
    Optional<Member> findByMemberEmailAndDeletedAtIsNull(String memberEmail);

    default Member findByMemberEmailOrThrow(String memberEmail){
        return findByMemberEmailAndDeletedAtIsNull(memberEmail)
                .orElseThrow(() -> new EntityNotFoundException("email에 해당하는 회원을 찾을 수 없습니다."));
    }

    List<Member> findAllByNoShowCountGreaterThanEqualAndDeletedAtIsNull(int noShowCount);

    @Query("SELECT m FROM Member m WHERE (LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(m.memberEmail) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND m.role = 'Member'")
    List<Member> searchMembers(@Param("keyword") String keyword);

    Page<Member> findByNameContainingOrMemberEmailContaining(String name, String memberEmail, Pageable pageable);

    // 정상 회원 중 인증 여부 필터링
    Page<Member> findByIsVerifiedAndDeletedAtIsNull(boolean isVerified, Pageable pageable);

    // 탈퇴 회원 중 인증 여부 필터링
    Page<Member> findByIsVerifiedAndDeletedAtIsNotNull(boolean isVerified, Pageable pageable);

    // 정상 회원만 조회
    Page<Member> findByDeletedAtIsNull(Pageable pageable);

    // 탈퇴 회원만 조회
    Page<Member> findByDeletedAtIsNotNull(Pageable pageable);

    // 이름이나 이메일로 검색하면서 인증 여부와 삭제 여부를 함께 필터링
    Page<Member> findByNameContainingOrMemberEmailContainingAndIsVerifiedAndDeletedAtIsNull(
            String name, String email, boolean isVerified, Pageable pageable);

    Page<Member> findByNameContainingOrMemberEmailContainingAndIsVerifiedAndDeletedAtIsNotNull(
            String name, String email, boolean isVerified, Pageable pageable);

    Page<Member> findByNameContainingOrMemberEmailContainingAndDeletedAtIsNull(
            String name, String email, Pageable pageable);

    Page<Member> findByNameContainingOrMemberEmailContainingAndDeletedAtIsNotNull(
            String name, String email, Pageable pageable);
}
