package com.example.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
@RestControllerAdvice
public class GlobalExceptionHandler {
    // custom  cac loai loi o day => hien thi ra message than thien vs ng dung
    // loi 1 => @valid

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handlerValidateErrors(MethodArgumentNotValidException e){
        // custom loi hien thi than thien voi nguoi dung
        Map<String,String> errorMap = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(s -> errorMap.put(s.getField(),s.getDefaultMessage()));
        return new  ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
    }

    // loi 2 => 400 nhung do minh tu tao(APIException)
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handlerAll(ApiException e){
        Map<String,String> errorMap = new HashMap<>();
        errorMap.put("status","FAIL");
        errorMap.put("code",e.getCode());
        errorMap.put("message",e.getMessage());
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // loi 3 => 500 cac loi khac
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handlerValidateApiException(Exception e){
        Map<String,String> errorMap = new HashMap<>();
        errorMap.put("status","FAIL");
        errorMap.put("code","INTERNAL_SERVER_ERROR");
        errorMap.put("message",e.getMessage());
        return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
