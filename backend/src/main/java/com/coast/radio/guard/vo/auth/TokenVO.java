package com.coast.radio.guard.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenVO {

    private String token;
    private Long expireSeconds;
    private String tokenType;
    private String username;
    private String role;
}
