# Firebase Service Account

Để Firebase Admin SDK hoạt động ở backend, bạn cần tải file service account credentials:

1. Mở [Firebase Console](https://console.firebase.google.com/)
2. Chọn project **saoclub-b9b96**
3. Click **Project Settings** (biểu tượng ⚙️)
4. Chuyển tab **Service Accounts**
5. Click **Generate new private key**
6. Lưu file JSON và đổi tên thành **`firebase-service-account.json`**
7. Copy vào thư mục **`BackEnd/src/main/resources/`**

File JSON sẽ có dạng:
```json
{
  "type": "service_account",
  "project_id": "saoclub-b9b96",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...",
  "client_email": "...@saoclub-b9b96.iam.gserviceaccount.com",
  ...
}
```

**Lưu ý:** File này chứa credentials nhạy cảm - đã được thêm vào `.gitignore` để không bị commit lên git.
