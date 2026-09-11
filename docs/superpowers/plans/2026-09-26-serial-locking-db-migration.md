# Serial Locking — DB Migration Note

**Ngày:** 2026-09-26  
**Trạng thái:** ✅ Đã apply (ddl-auto=update)

---

## Thêm columns vào `chi_tiet_san_pham`

```sql
-- 1. Thêm columns locking
ALTER TABLE chi_tiet_san_pham ADD COLUMN locked_by INT NULL;
ALTER TABLE chi_tiet_san_pham ADD COLUMN locked_at TIMESTAMP NULL;
ALTER TABLE chi_tiet_san_pham ADD COLUMN lock_session VARCHAR(64) NULL;

-- 2. Thêm foreign key (optional - khớp với Hibernate auto)
-- ALTER TABLE chi_tiet_san_pham ADD CONSTRAINT fk_serial_locked_by
--     FOREIGN KEY (locked_by) REFERENCES nhan_vien(id);

-- 3. Index để lookup lock nhanh
CREATE INDEX idx_serial_lock ON chi_tiet_san_pham(locked_at, lock_session);
```

## Kiểm tra

```sql
-- Verify columns tồn tại
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'chi_tiet_san_pham'
  AND column_name IN ('locked_by', 'locked_at', 'lock_session');
```

## Rollback (nếu cần)

```sql
ALTER TABLE chi_tiet_san_pham DROP COLUMN IF EXISTS locked_by;
ALTER TABLE chi_tiet_san_pham DROP COLUMN IF EXISTS locked_at;
ALTER TABLE chi_tiet_san_pham DROP COLUMN IF EXISTS lock_session;
```
