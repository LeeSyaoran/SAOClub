package com.example.backend.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiException extends RuntimeException{

    private String code; // ma loi tu dinh nghia
    // vd: CO1 => truong id khong duoc de trong

    public ApiException(String message, String code) {
        super(message);
        this.code = code;
    }

}
