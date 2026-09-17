# Plan: Thêm ảnh minh hoạ cho danh mục CPU/RAM/GPU/Ổ cứng

## Context

User báo: 4 bảng **CPU / RAM / GPU / Ổ cứng** trong trang Kho hàng hiển thị icon ảnh bị gãy (broken image placeholder) ở cột "Hình ảnh".

**Nguyên nhân thật** (xác minh qua code + schema):
- Schema DB ở [Database/QLBanMayTinh.sql:246-253](Database/QLBanMayTinh.sql#L246-L253) tạo 4 bảng `dm_cpu`, `dm_ram`, `dm_o_cung`, `dm_gpu` **không có cột `hinh_anh`**:
  ```sql
  CREATE TABLE dm_cpu    ( cpu_id INT IDENTITY(1,1) PRIMARY KEY, ten_cpu NVARCHAR(100) NOT NULL UNIQUE );
  ```
- 4 entity Java ([DmCpu.java:23-24](BackEnd/src/main/java/com/example/backend/entity/DmCpu.java#L23-L24), [DmRam.java:23-24](BackEnd/src/main/java/com/example/backend/entity/DmRam.java#L23-L24), [DmGpu.java:23-24](BackEnd/src/main/java/com/example/backend/entity/DmGpu.java#L23-L24), [DmOcung.java:23-24](BackEnd/src/main/java/com/example/backend/entity/DmOcung.java#L23-L24)) đã có field `hinhAnh` — nhưng `ddl-auto=none` ở [application.properties:13](BackEnd/src/main/resources/application.properties#L13) nên JPA không tự sinh cột.
- Kết quả: `SELECT` không trả `hinh_anh` → field luôn `null` → FE render `<ImageIcon>` placeholder (xem [DmCategoryTable.vue:436-439](FrontEnd/QLBanMayTinh/src/components/admin/DmCategoryTable.vue#L436-L439)).
- Form thêm/sửa hiện có 1 ô nhập URL ([DmCategoryTable.vue:504-508](FrontEnd/QLBanMayTinh/src/components/admin/DmCategoryTable.vue#L504-L508)) — không có nút upload → user không có URL ảnh để dán.

**Đã xác nhận với user**: dùng phương án **upload file** (dùng lại endpoint `/api/upload/image` đã có sẵn). User sẽ tự upload ảnh cho từng linh kiện.

## Phương án

Tận dụng tối đa code đã có:
- **Backend `/api/upload/image`** ([UploadController.java](BackEnd/src/main/java/com/example/backend/controller/UploadController.java)) đã hoạt động — lưu file vào `FrontEnd/QLBanMayTinh/public/images/`, trả `{url: "/images/<uuid>.<ext>"}`. Vite serve `/images/...` trực tiếp.
- **Frontend `uploadImage()`** ([CaiDatService.js:17-31](FrontEnd/QLBanMayTinh/src/services/CaiDatService.js#L17-L31)) đã viết sẵn hàm gọi đúng endpoint, kèm `authHeaders()` + parse JSON. Pattern tương tự đã dùng trong `HangHoa.vue` cho sản phẩm.
- **Entity + JSON request/response**: 4 controller đang trả/thu `DmCpu/Ram/Gpu/Ocung` **entity trực tiếp** (xem [DmCpuController.java](BackEnd/src/main/java/com/example/backend/backend/controller/DmCpuController.java)), Jackson tự serialize toàn bộ field — nên khi schema có cột `hinh_anh`, FE sẽ tự nhận được. **Không cần sửa controller hay thêm DTO mới.**
- **`DmCpuRequest`/`DmRamRequest`/...** ([DmCpuRequest.java](BackEnd/src/main/java/com/example/backend/request/DmCpuRequest.java)) — đã có sẵn nhưng **không được dùng** trong controller (controller dùng entity). Không đụng.

## Files thay đổi

### 1. SQL — `Database/QLBanMayTinh.sql`

**Vị trí chèn**: ngay sau dòng 254 (sau `GO` đóng block CREATE 4 bảng dm_*), thêm 1 block ALTER idempotent riêng cho 4 cột `hinh_anh`. Theo đúng pattern `IF NOT EXISTS (SELECT 1 FROM sys.columns ...)` đã có ở dòng 165-170, 2370-2372, 2415-2417, 2577-2579.

```sql
-- ============================================================
--  Ảnh minh hoạ cho danh mục CPU/RAM/GPU/Ổ cứng — thêm sau vì schema gốc
--  chưa có cột này. Đặt sau CREATE TABLE các bảng dm_* và là kiểu NULL để không
--  vi phạm constraint NOT NULL của các dòng seed ở mục 13 đã có sẵn.
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dm_cpu')     AND name = 'hinh_anh')
    ALTER TABLE dm_cpu     ADD hinh_anh NVARCHAR(500) NULL;
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dm_ram')     AND name = 'hinh_anh')
    ALTER TABLE dm_ram     ADD hinh_anh NVARCHAR(500) NULL;
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dm_o_cung')  AND name = 'hinh_anh')
    ALTER TABLE dm_o_cung  ADD hinh_anh NVARCHAR(500) NULL;
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dm_gpu')     AND name = 'hinh_anh')
    ALTER TABLE dm_gpu     ADD hinh_anh NVARCHAR(500) NULL;
GO
```

Không cần thêm seed data — user tự upload sau khi deploy.

### 2. Frontend — `FrontEnd/QLBanMayTinh/src/components/admin/DmCategoryTable.vue`

**Import** (đầu file `<script setup>`):
```js
import { uploadImage } from "../../services/CaiDatService.js";
```

**State mới** cho upload spinner:
```js
const dangTaiAnh = ref(false);
const loiUploadAnh = ref("");
```

**Handler** upload:
```js
const chonFileAnh = async (e) => {
  const file = e.target.files?.[0];
  if (!file) return;
  dangTaiAnh.value = true;
  loiUploadAnh.value = "";
  try {
    const { url } = await uploadImage(file);
    formValue.value.hinhAnh = url;
  } catch (err) {
    loiUploadAnh.value = err.message || "Tải ảnh thất bại";
  } finally {
    dangTaiAnh.value = false;
    e.target.value = "";
  }
};
```

**Template modal thêm/sửa** — thay khối input URL hiện tại (dòng 495-510) bằng: preview ảnh + nút "Tải ảnh lên" + ô URL tùy chọn (giữ lại để user có thể dán URL ngoài nếu muốn):

```vue
<div class="mb-4">
  <label class="form-label" style="font-size:0.8rem;color:var(--text-secondary);">
    Hình ảnh
  </label>
  <div class="d-flex gap-3 align-items-start">
    <div style="width:60px;height:60px;border-radius:8px;overflow:hidden;background:var(--bg-input);flex-shrink:0;display:flex;align-items:center;justify-content:center;border:1px solid var(--border-color);">
      <img v-if="formValue?.hinhAnh && !formValue.imgError" :src="formValue.hinhAnh" @error="formValue.imgError = true" style="width:100%;height:100%;object-fit:contain;" />
      <ImageIcon v-else class="text-secondary" :size="28" />
    </div>
    <div class="flex-grow-1 d-flex flex-column gap-2">
      <label class="btn btn-sm btn-outline-info" style="padding:2px 10px;font-size:0.72rem;cursor:pointer;width:fit-content;">
        <FolderOpen :size="14" style="vertical-align:-2px;" />
        {{ dangTaiAnh ? 'Đang tải…' : 'Tải ảnh lên' }}
        <input type="file" accept="image/*" class="d-none" :disabled="dangTaiAnh" @change="chonFileAnh" />
      </label>
      <input
        v-model="formValue.hinhAnh"
        class="form-control admin-input"
        placeholder="Hoặc dán đường dẫn ảnh..."
      />
      <small v-if="loiUploadAnh" class="text-danger">{{ loiUploadAnh }}</small>
    </div>
  </div>
</div>
```

**Không cần sửa controller backend** — 4 controller `DmCpu/Ram/Gpu/Ocung` đã làm việc với entity trực tiếp, Jackson tự đọc field `hinhAnh` từ JSON request và trả về trong JSON response sau khi ALTER cột chạy.

**Không cần sửa `WarehouseManagementPage.vue`** — đã truyền đủ props, `DmCategoryTable` tự xử lý.

**Không cần sửa `DmCpuRequest`/`DmCpuResponse`** — đã có sẵn nhưng **không được dùng** trong controller (controller gọi trực tiếp entity). Jackson tự serialize field `hinhAnh` của entity.

## Tái sử dụng từ code hiện có

- **Endpoint upload**: [UploadController.java](BackEnd/src/main/java/com/example/backend/controller/UploadController.java) — POST `/api/upload/image`, validate magic bytes, trả `{url, filename}`.
- **Hàm FE `uploadImage`**: [CaiDatService.js:17-31](FrontEnd/QLBanMayTinh/src/services/CaiDatService.js#L17-L31) — `FormData` + `authHeaders()` + parse JSON lỗi. Pattern y hệt đã dùng trong `HangHoa.vue:1748-1763` cho sản phẩm.
- **Pattern icon `FolderOpen` + input file ẩn**: y hệt modal import serial ở [DmCategoryTable.vue:526-529](FrontEnd/QLBanMayTinh/src/components/admin/DmCategoryTable.vue#L526-L529).
- **CSS reuse**: dùng `admin-input` (đã có), `text-secondary`, `text-danger` — không thêm class mới.

## Phạm vi KHÔNG đụng

- Không sửa 4 entity Java (đã có field `hinhAnh` đúng tên cột).
- Không sửa 4 controller (đã trả/thu entity — Jackson tự lo).
- Không sửa `DmCpuRequest`/`DmCpuResponse` (đã có/không dùng — không cần đồng bộ).
- Không seed ảnh mẫu trong SQL (user tự upload sau khi deploy).
- Không đụng các bảng khác (CPU/RAM/GPU/Ổ cứng là phạm vi duy nhất).

## Verification

### Backend
1. Restart backend sau khi sửa SQL.
2. `curl -X POST http://localhost:8080/api/upload/image -H "Authorization: Bearer <jwt>" -F "file=@test.jpg"` → trả `{url, filename}`.
3. `curl http://localhost:8080/api/dm-cpu` → response có field `hinhAnh` (sẽ là `null` cho dữ liệu cũ).

### Frontend
1. Mở trang Kho hàng, vào tab CPU/RAM/GPU/Ổ cứng.
   - Ảnh cũ vẫn là icon placeholder (vì DB chưa có ảnh).
2. Bấm "Thêm CPU" → modal mở → bấm "Tải ảnh lên" → chọn file ảnh.
   - Spinner hiện "Đang tải…".
   - Upload xong → preview ảnh hiện ở ô bên trái, URL điền vào ô text bên phải.
3. Nhập tên CPU → bấm "Lưu".
   - Quay lại bảng → dòng vừa thêm có ảnh hiển thị ở cột "Hình ảnh".
4. Bấm "Sửa" một dòng đã có ảnh → modal mở với preview ảnh đúng.
   - Upload ảnh mới → preview đổi → bấm "Lưu" → bảng cập nhật ảnh mới.
5. Tab RAM/GPU/Ổ cứng làm tương tự — không cần sửa gì thêm (cùng component `DmCategoryTable`).
6. Test trên cả dark/light theme + 5 ngôn ngữ để đảm bảo contrast ảnh OK.

### Rollback nếu cần
- SQL: xóa block ALTER (4 dòng `ALTER TABLE ... DROP COLUMN hinh_anh;` — nhưng DB cũ chưa có cột nên DROP sẽ lỗi; rollback thực sự là restore bản SQL cũ).
- FE: xóa nút "Tải ảnh lên" + handler `chonFileAnh` → giữ lại ô URL như trước.
