# Hướng dẫn tạo Google API Key và Search Engine ID (CX) cho Custom Search API

## 1️⃣ Tạo Google API Key

### Bước 1: Truy cập Google Cloud Console
- Truy cập: [https://console.cloud.google.com/](https://console.cloud.google.com/)
- Đăng nhập bằng tài khoản Google của bạn.

---

### Bước 2: Tạo (hoặc chọn) một Project
1. Ở góc trên cùng, chọn **Select a project** → **New Project**
2. Nhập tên ví dụ: `customsearch-demo`
3. Nhấn **Create**

---

### Bước 3: Bật Custom Search API
1. Vào menu: **APIs & Services → Library**
2. Gõ vào ô tìm kiếm: `Custom Search API`
3. Nhấn **Enable**

---

### Bước 4: Tạo API Key
1. Vào **APIs & Services → Credentials**
2. Nhấn **Create Credentials → API Key**
3. Google sẽ hiển thị chuỗi key như:
AIzaSyD4...XYZ

## 2️⃣ Tạo Search Engine ID (CX)

### Bước 1: Truy cập Google Programmable Search Engine
 [https://programmablesearchengine.google.com/](https://programmablesearchengine.google.com/)

---

### Bước 2: Tạo Search Engine mới
1. Nhấn **“Add”** hoặc **“Create a search engine”**
2. Ở ô **Sites to search**, nhập:
- `www.google.com` (nếu muốn test nhanh)
- hoặc `*` nếu muốn cho phép tìm trên toàn web (bật ở bước sau)
3. Nhấn **Create**

---

### Bước 3: Lấy Search Engine ID
1. Sau khi tạo xong, vào trang quản lý search engine
2. Mở **Control Panel → Details**
3. Ở mục **Search engine ID**, bạn sẽ thấy:
   1234567890abcdefg:abcde12345
