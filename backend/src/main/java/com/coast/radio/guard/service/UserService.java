package com.coast.radio.guard.service;

import com.coast.radio.guard.dto.auth.LoginDTO;
import com.coast.radio.guard.vo.auth.TokenVO;

public interface UserService {

    TokenVO login(LoginDTO loginDTO);
}
