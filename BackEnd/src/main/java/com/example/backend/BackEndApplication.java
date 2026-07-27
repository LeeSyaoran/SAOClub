package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;

// VIA_DTO: bọc Page<> trả về từ controller (san-pham, don-hang) qua PagedModel thay vì
// serialize PageImpl thẳng — PageImpl không có JSON schema ổn định, Spring Data khuyến cáo
// dùng PagedModel để đảm bảo cấu trúc {content, page:{size,number,totalElements,totalPages}}.
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
// Bắt buộc cho SseService — broadcast() gọi emitter.send() đồng bộ; nếu chạy trên chính
// request thread (vd DonHangService.create() gọi notifyNewOrder() ngay trong lúc tạo đơn),
// 1 kết nối SSE chết/treo (tab đóng không sạch, qua nhiều lần restart backend) sẽ chặn ghi
// socket và treo luôn cả request gốc tới khi OS timeout. @EnableAsync tách việc broadcast
// sang thread pool riêng, request gốc trả lời ngay không phải chờ.
@EnableAsync
@SpringBootApplication
public class BackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackEndApplication.class, args);
    }

}
