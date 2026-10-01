package com.tcc.gestaorural.data.remote.dto;

/** Corpo de POST /api/auth/refresh e POST /api/auth/logout. */
public class RefreshRequestDto {
    public String refreshToken;

    public RefreshRequestDto() {}
    public RefreshRequestDto(String refreshToken) { this.refreshToken = refreshToken; }
}
