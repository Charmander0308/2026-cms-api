package com.malgn.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "로그인 요청")
@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @Schema(description = "사용자 아이디", example = "admin")
    @NotBlank(message = "아이디는 필수입니다.")
    private String username;

    @Schema(description = "비밀번호", example = "admin123")
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
