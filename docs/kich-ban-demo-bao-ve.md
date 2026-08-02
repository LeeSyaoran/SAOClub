# Kịch bản Demo — Buổi bảo vệ đồ án SAOClub

Tổng thời lượng: **30 phút**, chia theo bảng phân công (đã cập nhật vào báo cáo, mục 3.6). Mỗi phần bên dưới có mốc thời gian con `[mm:ss–mm:ss]` cộng đúng bằng thời lượng được giao — cứ theo mốc mà đi, tránh lố giờ hoặc nói xong quá sớm rồi đứng im.

Mỗi bước gồm **Làm** (thao tác tay) và **Nói** (câu dẫn gợi ý — không cần học thuộc, đọc ý chính rồi diễn giải tự nhiên).

## Tài khoản dùng để demo

Mật khẩu chung: `123456`

| Vai trò | Username | Dùng ở phần |
|---|---|---|
| Khách hàng | `khachhang` | 2, 5, 6.1, 6.4 |
| Nhân viên bán hàng | `nhanvienan` | 6.2, 6.3 |
| Quản kho | `nhanviencuong` | 4 (nếu muốn demo đúng vai) |
| Quản lý (admin) | `admin` | 2, 3, 4, 5, 7 |

## Chuẩn bị trước 15 phút

- Chạy `docker compose up -d` ở thư mục gốc dự án (SQL Server + backend :8080 + frontend :5173) — kiểm tra cả 3 container `Up` bằng `docker compose ps`.
- Mở sẵn **3 tab trình duyệt**: (1) ẩn danh/chưa đăng nhập cho phần mở đầu, (2) đăng nhập sẵn `khachhang`, (3) đăng nhập sẵn `admin` — chuyển tab bằng `Ctrl+Tab` thay vì đăng nhập lại giữa các phần để không mất thời gian.
- Dọn sạch giỏ hàng/đơn hàng demo cũ nếu đã test nhiều lần, tránh đơn "rác" làm rối màn hình khi trình bày trước hội đồng.
- Ghi sẵn ra giấy 1 mã sản phẩm và 1 SĐT khách hàng có thật trong dữ liệu mẫu để không phải gõ/nhớ giữa chừng.
- Dự phòng: nếu mạng/máy gặp sự cố, có sẵn ảnh chụp màn hình các bước chính trong báo cáo (mục 4.3 Mockup giao diện) để chiếu tạm và nói qua kịch bản bằng lời.

---

## 1. Mở đầu — Lê Huy Đỗ (1,5 phút = 90 giây)

Không demo, chỉ trình bày trên slide/lời nói.

- **[0:00–0:20] Chào hỏi:** giới thiệu nhóm SD-16, tên đề tài "Xây dựng Website bán máy tính – laptop SAOClub".
- **[0:20–0:50] Vấn đề thực tế:** *"Khác với quần áo hay đồ gia dụng, máy tính là hàng có giá trị cao và mỗi máy là một đơn vị vật lý riêng biệt — có số serial, có thời hạn bảo hành tính từ ngày bán. Cửa hàng truyền thống quản lý tồn kho theo số lượng, nên khi khách mang máy tới bảo hành, không thể tra ngay được máy đó bán ngày nào, cho ai, thuộc lô nào."*
- **[0:50–1:20] Giải pháp:** *"Nhóm xây dựng một hệ thống duy nhất, hai mặt: website bán hàng cho khách và công cụ quản trị nội bộ cho cửa hàng, dùng chung một cơ sở dữ liệu — tránh việc nhân viên phải nhập liệu hai lần. Hệ thống có bốn không gian làm việc theo bốn vai trò: khách hàng, nhân viên bán hàng, quản kho, quản lý."*
- **[1:20–1:30] Chuyển tiếp:** *"Sau đây các bạn trong nhóm sẽ demo trực tiếp từng phần. Xin mời bạn Việt Anh."*

## 2. Tài khoản, bảo mật, khách hàng, nhân sự — Nghiêm Việt Anh (4,5 phút = 270 giây)

- **[0:00–0:40] Đăng ký tài khoản.** Làm: trang chủ → **Đăng nhập** → **Đăng ký ngay** → điền form (họ tên, SĐT, email, mật khẩu) → gửi. Nói: *"Sau khi đăng ký, hệ thống mã hóa mật khẩu bằng BCrypt và cấp token JWT ngay, khách hàng vào thẳng tài khoản mà không cần đăng nhập lại lần nữa."*
- **[0:40–1:20] Đăng nhập sai/đúng.** Làm: đăng xuất → nhập sai mật khẩu → hệ thống báo lỗi. Nói: *"Thông báo lỗi cố tình chung chung — 'Tài khoản hoặc mật khẩu không đúng' — không nói rõ sai ở tên đăng nhập hay mật khẩu, để tránh lộ thông tin tài khoản nào tồn tại trong hệ thống."* → đăng nhập lại đúng bằng `khachhang`/`123456`.
- **[1:20–2:10] Chặn truy cập trái phép.** Làm: từ tài khoản khách hàng, gõ thẳng URL `/#/admin` → bị điều hướng về trang chủ. Nói: *"Đây là lớp chặn ở phía giao diện. Nhưng lớp thật sự quan trọng nằm ở phía máy chủ — kể cả khi ai đó cố gọi thẳng API quản trị bằng Postman với token của khách hàng, máy chủ vẫn trả về lỗi 403 Forbidden. Trong quá trình kiểm thử, nhóm đã gọi trực tiếp API để xác nhận cơ chế này hoạt động đúng."*
- **[2:10–2:30] Kiến trúc tài khoản.** Nói (không cần demo thêm, giải thích trong lúc chuyển màn hình): *"Cả nhân viên và khách hàng dùng chung một bảng tài khoản duy nhất, tách riêng khỏi bảng hồ sơ — giúp thống nhất một cơ chế đăng nhập và phân quyền cho toàn hệ thống."*
- **[2:30–3:20] Quản lý khách hàng (admin).** Làm: đăng nhập `admin` → **Khách hàng** → mở 1 khách hàng bất kỳ. Nói: *"Trang chi tiết cho thấy tổng chi tiêu, số đơn đã mua, lịch sử đơn hàng và điểm tích lũy — phục vụ chăm sóc khách hàng và đối chiếu khi có tranh chấp."*
- **[3:20–4:10] Quản lý nhân viên.** Làm: **Nhân viên** → danh sách → mở 1 nhân viên xem chức vụ, trạng thái làm việc. Nói: *"Nhân viên nghỉ việc chỉ bị chuyển trạng thái chứ không xóa khỏi hệ thống, để lịch sử đơn hàng họ từng xử lý không bị mất."*
- **[4:10–4:30] Chuyển tiếp:** *"Tiếp theo, bạn Việt sẽ demo phần quản lý sản phẩm."*

## 3. Sản phẩm, biến thể, cấu hình chuẩn hóa, cài đặt — Nguyễn Xuân Việt (3 phút = 180 giây)

- **[0:00–1:00] Thêm sản phẩm + biến thể.** Làm: `/admin` → **Sản phẩm** → **+ Thêm sản phẩm** → mở form. Nói: *"Chú ý các trường CPU, RAM, GPU, Ổ cứng đều là danh sách chọn sẵn, không phải ô gõ tự do. Đây là điểm khác biệt cốt lõi: nếu cho nhập tự do, nhân viên có thể gõ 'i5 12500H', 'Core i5-12500H', 'Intel i5 12500H' cho cùng một con chip, khiến bộ lọc phía khách hàng mất tác dụng."* → đóng form, không lưu.
- **[1:00–1:50] Danh mục thông số chuẩn hóa.** Làm: **Kho hàng** → mở tab CPU (hoặc RAM/GPU/Ổ cứng). Nói: *"Đây chính là bốn danh mục đã chuẩn hóa mà biến thể sản phẩm tham chiếu tới. Quản lý thêm một dòng CPU mới ở đây một lần, mọi sản phẩm dùng chung con chip đó đều lọc và so sánh được chính xác."*
- **[1:50–2:40] Cài đặt hệ thống.** Làm: **Cài đặt** → lướt qua các khối: ngưỡng cảnh báo tồn kho mặc định, thông tin cửa hàng, ngôn ngữ và định dạng số mặc định. Nói: *"Đây là các tham số vận hành có thể đổi ngay mà không cần sửa mã nguồn hay build lại hệ thống — ví dụ đổi ngưỡng cảnh báo hết hàng từ 5 xuống 10 máy khi mùa cao điểm."*
- **[2:40–3:00] Chuyển tiếp:** *"Xin mời bạn Đạt demo phần kho hàng."*

## 4. Kho, serial, tồn kho — Nguyễn Thành Đạt (3 phút = 180 giây)

- **[0:00–0:40] Đặt vấn đề.** Nói: *"Đây là phần giải quyết trực tiếp bài toán đã nêu ở đầu buổi — quản lý tới từng chiếc máy, không chỉ theo số lượng."*
- **[0:40–1:30] Tồn kho theo biến thể.** Làm: **Kho hàng** → tab **Tồn kho** → chỉ vào 4 chỉ số tổng SKU / tổng tồn / sắp hết / hết hàng → mở 1 sản phẩm xem tồn theo từng biến thể cấu hình. Nói: *"Mỗi biến thể — ví dụ cùng dòng máy nhưng khác RAM, ổ cứng — có số tồn riêng, tự đồng bộ với số máy vật lý còn nằm trong kho."*
- **[1:30–2:20] Serial từng máy.** Làm: mở chi tiết 1 biến thể (hoặc tab **Serial**) → cho xem danh sách số serial/IMEI kèm trạng thái (còn trong kho / đã bán / lỗi). Nói: *"Mỗi dòng ở đây là một chiếc máy thật. Khi khách mang máy tới bảo hành, nhân viên chỉ cần gõ đúng số serial này là tra ra ngay đơn hàng gốc, ngày bán, và thời hạn bảo hành còn lại — đây chính là điểm khác biệt lớn nhất so với phần mềm bán hàng phổ thông."*
- **[2:20–2:55] Phiếu nhập kho.** Làm: tab **Phiếu nhập** → mở 1 phiếu đã có → **+ Tạo phiếu nhập** → cho xem form nhập hàng loạt kèm khai báo danh sách serial của lô hàng mới. Nói: *"Hệ thống kiểm tra chống trùng serial ngay khi nhập, tránh gán nhầm hai máy khác nhau cùng một số serial."* → đóng lại không lưu.
- **[2:55–3:00] Chuyển tiếp:** *"Tiếp theo, bạn Ngữ demo phần khuyến mãi và tích điểm."*

## 5. Duyệt sản phẩm, khuyến mãi, tích điểm, vòng quay — Lê Anh Ngữ (2,5 phút = 150 giây)

- **[0:00–0:40] Lọc nâng cao.** Làm: trang khách hàng → **Lọc nâng cao** → chọn hãng + CPU + khoảng giá cùng lúc. Nói: *"Nhờ danh mục CPU/RAM/GPU đã chuẩn hóa ở phần trước, bộ lọc đa tiêu chí này trả kết quả chính xác tuyệt đối, không bị lệch do cách gõ khác nhau."*
- **[0:40–1:20] So sánh sản phẩm.** Làm: tick **So sánh** trên 2 sản phẩm khác dòng → **So sánh ngay**. Nói: *"Tính năng này rất cần với mặt hàng máy tính — khách thường phân vân giữa nhiều cấu hình trước khi xuống tiền."*
- **[1:20–2:00] Tích điểm & vòng quay.** Làm: đăng nhập `khachhang` → `/account` → tab **Vòng quay may mắn**. Nói: *"Mỗi đơn hàng hoàn tất được cộng điểm tự động theo quy tắc cấu hình sẵn. Khách dùng điểm để quay thưởng hoặc đổi trực tiếp lấy phiếu giảm giá — toàn bộ biến động điểm đều được ghi log kèm lý do để đối chiếu khi khách thắc mắc."*
- **[2:00–2:30] Khuyến mãi (admin).** Làm: `/admin` → **Khuyến mãi** → danh sách chương trình đang chạy. Nói: *"Quản lý cấu hình được phạm vi áp dụng theo sản phẩm, danh mục hoặc toàn shop, cùng thời gian hiệu lực — không cần sửa mã nguồn khi ra chương trình mới."*
- **[2:30–2:30] Chuyển tiếp:** *"Phần tiếp theo là phần trọng tâm — quy trình bán hàng và phân quyền, do bạn Đỗ trình bày."*

## 6. Phân quyền + bán online + bán offline — Lê Huy Đỗ (11 phút = 660 giây, phần trọng tâm)

*Đây là phần dài nhất vì nó xâu chuỗi toàn bộ vòng đời một đơn hàng qua đủ vai trò — đúng thứ mà toàn bộ kiến trúc dữ liệu (serial, tồn kho, JWT, SSE) ở các phần trước phục vụ cho.*

### 6.1. Mua hàng trực tuyến — vai khách hàng (0:00–4:00, 4 phút)

- **[0:00–0:30] Thêm giỏ hàng.** Làm: tab `khachhang` → chọn 1 sản phẩm → thêm vào giỏ. Nói: *"Hệ thống kiểm tra tồn kho ngay tại bước này — nếu biến thể vừa hết hàng, nút thêm giỏ sẽ bị vô hiệu hóa."*
- **[0:30–1:10] Xem giỏ hàng.** Làm: mở giỏ hàng, chỉ vào tạm tính + phí vận chuyển. Nói: *"Miễn phí vận chuyển từ mốc 300 nghìn, tính tự động theo cấu hình đã xem ở phần Cài đặt."*
- **[1:10–2:20] Đặt hàng.** Làm: **Thanh toán** → điền thông tin giao hàng → thử nhập mã giảm giá sai → hệ thống báo lỗi, giữ nguyên tổng tiền → nhập đúng (nếu có mã demo) hoặc bỏ qua → **Tiếp tục** → xác nhận đặt hàng. Nói: *"Ngay trước khi tạo đơn, hệ thống kiểm tra lại tồn kho lần cuối cho toàn bộ giỏ hàng. Toàn bộ bước tạo đơn — trừ tồn kho tạm giữ, ghi nhật ký đơn hàng — nằm trong một giao dịch (transaction) duy nhất, nên nếu một bước lỗi giữa chừng, hệ thống hoàn tác tất cả để không xảy ra tình huống đơn đã lưu nhưng kho chưa trừ."*
- **[2:20–3:10] Theo dõi đơn.** Làm: vào `/account` → tab **Đang xử lý** → mở chi tiết đơn vừa đặt. Nói: *"Đơn xuất hiện ngay với timeline trạng thái đầu tiên — 'Đơn hàng đã đặt'. Đồng thời, một sự kiện thời gian thực vừa được gửi tới toàn bộ màn hình quản trị đang mở, để nhân viên biết ngay có đơn mới mà không cần refresh."*
- **[3:10–4:00] Chuyển vai.** Nói: *"Bây giờ mình chuyển sang tài khoản nhân viên để xử lý chính đơn hàng vừa đặt."* → chuyển tab.

### 6.2. Xử lý đơn — vai nhân viên (4:00–7:00, 3 phút)

- **[4:00–4:20] Tìm đơn.** Làm: tab `nhanvienan` → `/staff` (không phải `/admin` — nhân viên có route và trang riêng, router chặn thẳng nếu cố vào `/admin`) → **Đơn hàng** → tìm đơn vừa đặt bằng mã đơn hoặc tên khách.
- **[4:20–5:40] Xác nhận đơn + chọn serial — cùng một bước.** Làm: mở đơn đang "Chờ xác nhận" → hệ thống bắt chọn đủ số serial cho từng dòng sản phẩm ngay trong màn hình xác nhận → chọn đúng máy theo số IMEI còn trong kho cho từng dòng → bấm **Xác nhận**. Nói: *"Chọn serial không phải một bước tách riêng ở khâu đóng gói — nó nằm ngay trong hành động xác nhận đơn. Chỉ khi đã gán đủ serial, đơn mới chuyển được từ 'Chờ xác nhận' sang 'Đã xác nhận', và các serial đó lập tức chuyển trạng thái 'đã bán' — chính liên kết này giúp sau này tra cứu bảo hành theo serial hoạt động được. Ở hậu trường, mỗi serial được khoá ghi trước khi kiểm tra khả dụng, để hai nhân viên mở hai tab không thể cùng chọn trúng một máy cho hai đơn khác nhau."*
- **[5:40–6:20] Đóng gói & giao.** Làm: chuyển tiếp trạng thái "Đã xác nhận" → "Đang xử lý" → "Đang giao", nhập mã vận đơn demo. Nói: *"Từ đây đơn chỉ đi một chiều theo đúng thứ tự đã khai cứng ở backend — không thể nhảy cóc hay lùi lại. Mã vận đơn nhập tay vì hệ thống chưa tích hợp API đơn vị vận chuyển thật, giới hạn nhóm đã nêu rõ trong báo cáo."*
- **[6:20–7:00] Nhấn mạnh.** Nói: *"Mọi lần đổi trạng thái vừa rồi đều được ghi vào bảng lịch sử đơn hàng kèm người thực hiện và thời điểm, phục vụ đối soát sau này."*

### 6.3. Bán hàng tại quầy — vai nhân viên (7:00–10:00, 3 phút)

- **[7:00–7:30] Tìm/tạo khách.** Làm: vẫn tài khoản nhân viên → **Bán hàng** → tìm khách theo số điện thoại đã ghi sẵn (hoặc chọn khách vãng lai nếu không tìm thấy). Nói: *"Đây là màn hình được thiết kế cho tốc độ — nhân viên thao tác trong lúc khách đang đứng chờ tại quầy, nên mọi bước đều tối giản."*
- **[7:30–8:30] Thêm sản phẩm + chọn serial.** Làm: thêm 1 sản phẩm vào phiếu bán → chọn cấu hình/màu → chọn ngay số serial tại quầy. Nói: *"Chọn serial ngay từ bước bán hàng tại quầy, không phải đợi tới bước đóng gói riêng như đơn online — vì khách nhận máy ngay tại chỗ."*
- **[8:30–9:30] Thanh toán.** Làm: áp thử 1 mã khuyến mãi nếu có → chọn phương thức thanh toán → nhập số tiền khách đưa → xem hệ thống tự tính tiền thừa → **Thanh toán**. Nói: *"Toàn bộ xảy ra trong một lần bấm: đơn được tạo ở trạng thái hoàn tất, tồn kho trừ, điểm tích lũy cộng cho khách — không cần qua các bước duyệt trung gian như đơn online, vì giao dịch tại quầy diễn ra tức thời."*
- **[9:30–10:00] Chốt phần bán hàng.** Nói: *"Như vậy hai kênh bán — online và tại quầy — dùng chung một cơ sở dữ liệu đơn hàng, tồn kho và điểm tích lũy, chỉ khác nhau ở quy trình xử lý phù hợp với ngữ cảnh."*

### 6.4. Cập nhật thời gian thực (10:00–11:00, 1 phút)

- **[10:00–10:40] Demo song song 2 tab.** Làm: mở 2 tab cạnh nhau — 1 tab khách hàng đang xem trang chi tiết đơn hàng của họ, 1 tab nhân viên/admin đổi trạng thái đúng đơn đó → quan sát tab khách hàng tự cập nhật ngay lập tức, không cần tải lại trang. Nói: *"Đây là Server-Sent Events — máy chủ chủ động đẩy sự kiện xuống trình duyệt đang mở. Việc phát sự kiện được xử lý bất đồng bộ, nên nếu một kết nối bị treo cũng không làm chậm các yêu cầu khác của hệ thống."*
- **[10:40–11:00] Chuyển tiếp:** *"Xin mời bạn Huy trình bày phần sau bán hàng và báo cáo thống kê."*

## 7. Trả hàng, bảo hành, thống kê — Vũ Quang Huy (3,5 phút = 210 giây)

- **[0:00–0:50] Trả hàng.** Làm: `/admin` → **Trả hàng** → danh sách yêu cầu → mở 1 phiếu xem chi tiết. Nói: *"Phiếu trả hàng luôn gắn với đơn gốc và đúng số serial đã giao — khi xác nhận, hệ thống tự nhập lại máy vào kho, cập nhật trạng thái serial và tính lại số tiền hoàn."*
- **[0:50–1:40] Bảo hành.** Làm: **Bảo hành** → tra cứu 1 số serial máy đã bán → xem ngày mua, thời hạn bảo hành còn lại. Nói: *"Đây chính là nơi toàn bộ dữ liệu serial đã thiết lập từ đầu buổi phát huy tác dụng: chỉ cần một số serial, nhân viên biết ngay máy này bán ngày nào, thuộc đơn nào, còn bảo hành hay không — không cần khách xuất trình phiếu giấy."* → minh họa nhanh việc lập phiếu tiếp nhận (không cần lưu).
- **[1:40–2:50] Báo cáo thống kê.** Làm: **Báo cáo** → Dashboard tổng quan → đổi bộ lọc Ngày/Tháng/Năm để biểu đồ doanh thu thay đổi theo → cuộn xuống xem Top 5 sản phẩm bán chạy/bán chậm, khách hàng nổi bật. Nói: *"Toàn bộ số liệu được tính bằng SUM/GROUP BY ngay ở tầng cơ sở dữ liệu, thay vì tải hết đơn hàng về trình duyệt rồi cộng dồn bằng JavaScript — quan trọng khi dữ liệu lên tới hàng trăm đơn."*
- **[2:50–3:30] Chốt phần kiểm thử.** Nói: *"Toàn bộ luồng vừa demo — từ đặt hàng, đóng gói theo serial, tới bảo hành — đều nằm trong 58 ca kiểm thử chức năng nhóm đã thực hiện thủ công, đạt tỉ lệ 96,6%, bên cạnh 186 ca kiểm thử tự động chạy qua GitHub Actions mỗi lần cập nhật mã nguồn."*

## 8. Kết luận — Lê Huy Đỗ (1 phút = 60 giây)

- **[0:00–0:20] Tóm tắt kết quả:** *"Nhóm đã hoàn thành 100% chức năng trong phạm vi đề ra ban đầu — từ bán hàng online, bán tại quầy, quản lý kho theo serial, tới sau bán hàng và báo cáo thống kê."*
- **[0:20–0:35] Hạn chế trung thực:** *"Trong quá trình kiểm thử, nhóm cũng phát hiện và ghi nhận trung thực hai điểm còn thiếu: chức năng quên mật khẩu chưa được triển khai ở phía máy chủ, và sổ địa chỉ giao hàng của khách hàng chưa có giao diện quản lý riêng."*
- **[0:35–0:50] Hướng phát triển:** *"Nếu tiếp tục phát triển, nhóm dự định tích hợp cổng thanh toán thật như VNPay/MoMo, kết nối API đơn vị vận chuyển, và xây dựng ứng dụng di động hỗ trợ quản kho quét mã vạch serial bằng camera điện thoại."*
- **[0:50–1:00] Kết:** *"Nhóm SD-16 xin cảm ơn thầy/cô hội đồng đã lắng nghe. Chúng em xin mời các câu hỏi."*
