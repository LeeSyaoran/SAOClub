package com.example.backend.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KhachHangRegisterRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private KhachHangRegisterRequest hopLe() {
        KhachHangRegisterRequest req = new KhachHangRegisterRequest();
        req.setHoTen("Nguyễn Văn A");
        req.setSoDienThoai("0900000000");
        req.setUsername("nguyenvana");
        req.setPassword("matkhau123");
        return req;
    }

    @Test
    void password_duoi8KyTu_biTuChoi() {
        KhachHangRegisterRequest req = hopLe();
        req.setPassword("abc1234"); // 7 ký tự

        Set<ConstraintViolation<KhachHangRegisterRequest>> loi = validator.validate(req);

        assertThat(loi).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void password_du8KyTu_hopLe() {
        KhachHangRegisterRequest req = hopLe();
        req.setPassword("abc12345"); // đúng 8 ký tự

        Set<ConstraintViolation<KhachHangRegisterRequest>> loi = validator.validate(req);

        assertThat(loi).noneMatch(v -> v.getPropertyPath().toString().equals("password"));
    }
}
