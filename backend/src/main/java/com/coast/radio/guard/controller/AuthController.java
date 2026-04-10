package com.coast.radio.guard.controller;

import com.coast.radio.guard.common.Result;
import com.coast.radio.guard.dto.auth.LoginDTO;
import com.coast.radio.guard.service.UserService;
import com.coast.radio.guard.vo.auth.TokenVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.ok("登录成功", userService.login(loginDTO));
    }
}
