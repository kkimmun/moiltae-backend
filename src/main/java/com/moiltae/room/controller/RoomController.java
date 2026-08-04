package com.moiltae.room.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.moiltae.global.common.ApiResponse;
import com.moiltae.global.security.CustomUserDetails;
import com.moiltae.room.dto.RoomDto;
import com.moiltae.room.service.RoomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoomDto.DetailResponse>> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RoomDto.CreateRequest request
    ) {
        RoomDto.DetailResponse data = roomService.create(userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ROOM_CREATED", "방을 생성했습니다.", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomDto.SummaryResponse>>> findMyRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<RoomDto.SummaryResponse> data = roomService.findMyRooms(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("ROOMS_FOUND", "내 방 목록을 조회했습니다.", data));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomDto.DetailResponse>> findDetail(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        RoomDto.DetailResponse data = roomService.findDetail(roomId, userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("ROOM_FOUND", "방을 조회했습니다.", data));
    }

    @PatchMapping("/{roomId}/close")
    public ResponseEntity<ApiResponse<RoomDto.CloseResponse>> close(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        RoomDto.CloseResponse data = roomService.closeManually(roomId, userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("ROOM_CLOSED", "방을 마감했습니다.", data));
    }
}
