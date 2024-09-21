package com.padaks.todaktodak.reservation.dto;

import com.padaks.todaktodak.reservation.domain.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CheckHospitalListReservationReqDto {
    private String memberEmail;
    private String doctorEmail;
    private Status status;
}
