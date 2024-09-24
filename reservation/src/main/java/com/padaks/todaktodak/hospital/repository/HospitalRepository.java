package com.padaks.todaktodak.hospital.repository;

import com.padaks.todaktodak.common.exception.BaseException;
import com.padaks.todaktodak.hospital.domain.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.padaks.todaktodak.common.exception.exceptionType.HospitalExceptionType.HOSPITAL_NOT_FOUND;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    // deleteAt이 null인 객체 찾음(삭제되지 않은 객체)
    Optional<Hospital> findByIdAndDeletedAtIsNull(Long id);

    default Hospital findByIdOrThrow(Long id) {
        return findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BaseException(HOSPITAL_NOT_FOUND));
    }

    // 삭제되지 않은 병원리스트 조회
    List<Hospital> findByDeletedAtIsNull();

    // 삭제되지 않은 병원리스트 중 '~~동'으로 병원찾기
    List<Hospital> findByDongAndDeletedAtIsNull(String dong);
}