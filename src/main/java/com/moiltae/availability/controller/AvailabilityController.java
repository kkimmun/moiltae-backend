package com.moiltae.availability.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moiltae.availability.dto.AvailabilityDto;
import com.moiltae.availability.service.AvailabilityService;
import com.moiltae.global.common.ApiResponse;
import com.moiltae.global.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rooms/{roomId}")
@RequiredArgsConstructor
public class AvailabilityController {
    private final AvailabilityService availabilityService;

    @GetMapping("/availabilities/me")
    public ResponseEntity<ApiResponse<AvailabilityDto.MyResponse>> findMine(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AvailabilityDto.MyResponse data = availabilityService.findMine(roomId, userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("AVAILABILITIES_FOUND", "내 가능 시간을 조회했습니다.", data));
    }

    @PutMapping("/availabilities/me")
    public ResponseEntity<ApiResponse<AvailabilityDto.MyResponse>> saveMine(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AvailabilityDto.SaveRequest request
    ) {
        AvailabilityDto.MyResponse data = availabilityService.saveMine(roomId, userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.success("AVAILABILITIES_SAVED", "가능 시간을 저장했습니다.", data));
    }

    @GetMapping("/availability-submissions")
    public ResponseEntity<ApiResponse<AvailabilityDto.SubmissionStatusResponse>> findSubmissionStatus(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AvailabilityDto.SubmissionStatusResponse data = availabilityService.findSubmissionStatus(
                roomId,
                userDetails.getMemberId()
        );
        return ResponseEntity.ok(ApiResponse.success(
                "AVAILABILITY_SUBMISSIONS_FOUND",
                "시간 등록 현황을 조회했습니다.",
                data
        ));
    }

    @GetMapping("/overlaps")
    public ResponseEntity<ApiResponse<AvailabilityDto.ResultResponse>> findResult(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AvailabilityDto.ResultResponse data = availabilityService.findResult(roomId, userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("ROOM_RESULT_FOUND", "마감 결과를 조회했습니다.", data));
    }
}
