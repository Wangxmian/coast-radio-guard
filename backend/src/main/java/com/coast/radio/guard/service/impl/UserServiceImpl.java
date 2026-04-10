package com.coast.radio.guard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coast.radio.guard.common.constants.ResultCode;
import com.coast.radio.guard.dto.auth.LoginDTO;
import com.coast.radio.guard.entity.SysUser;
import com.coast.radio.guard.exception.BusinessException;
import com.coast.radio.guard.mapper.UserMapper;
import com.coast.radio.guard.security.JwtProperties;
import com.coast.radio.guard.security.JwtTokenProvider;
import com.coast.radio.guard.service.UserService;
import com.coast.radio.guard.vo.auth.TokenVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public UserServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           JwtProperties jwtProperties) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public TokenVO login(LoginDTO loginDTO) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUsername, loginDTO.getUsername())
            .last("LIMIT 1"));

        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_FAILED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED, "用户已禁用");
        }

        String storedPassword = user.getPassword();
        boolean matched;
        if (storedPassword != null && storedPassword.startsWith("$2")) {
            matched = passwordEncoder.matches(loginDTO.getPassword(), storedPassword);
        } else {
            matched = loginDTO.getPassword().equals(storedPassword);
            if (matched) {
                user.setPassword(passwordEncoder.encode(loginDTO.getPassword()));
                userMapper.updateById(user);
                log.info("User {} password upgraded to BCrypt", user.getUsername());
            }
        }

        if (!matched) {
            throw new BusinessException(ResultCode.LOGIN_FAILED, "用户名或密码错误");
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getRole());
        return new TokenVO(token, jwtProperties.getExpireSeconds(), "Bearer", user.getUsername(), user.getRole());
    }
}
