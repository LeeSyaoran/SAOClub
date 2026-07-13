package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

// VIA_DTO: bọc Page<> trả về từ controller (san-pham, don-hang) qua PagedModel thay vì
// serialize PageImpl thẳng — PageImpl không có JSON schema ổn định, Spring Data khuyến cáo
// dùng PagedModel để đảm bảo cấu trúc {content, page:{size,number,totalElements,totalPages}}.
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@SpringBootApplication
public class BackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackEndApplication.class, args);
    }

}
