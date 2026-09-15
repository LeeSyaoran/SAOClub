# -*- coding: utf-8 -*-
import sys
import os

# Set UTF-8 encoding for stdout
sys.stdout.reconfigure(encoding='utf-8')

from docx import Document
from docx.shared import Inches, Pt, RGBColor, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.style import WD_STYLE_TYPE

# Paths
DOCX_PATH = 'scripts/BaoCao_SD16_SAOClub.docx'
SCREENSHOTS_DIR = 'docs/screenshots'

def add_heading(doc, text, level=1):
    """Add a heading to document"""
    heading = doc.add_heading(text, level=level)
    return heading

def add_paragraph(doc, text):
    """Add a paragraph"""
    p = doc.add_paragraph()
    run = p.add_run(text)
    return p

def add_screenshot(doc, image_path, caption=""):
    """Add a screenshot with caption"""
    if os.path.exists(image_path):
        try:
            # Add image
            img = doc.add_picture(image_path, width=Inches(6))
            last_paragraph = doc.paragraphs[-1]
            last_paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER

            # Add caption
            if caption:
                cap_p = doc.add_paragraph()
                run = cap_p.add_run(caption)
                run.italic = True
                run.font.size = Pt(10)
                cap_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        except Exception as e:
            print(f"Error adding image {image_path}: {e}")
            doc.add_paragraph(f"[Hinh anh: {caption}]")
    else:
        doc.add_paragraph(f"[Hinh anh khong tim thay: {caption}]")

def add_table_with_header(doc, headers, rows):
    """Add a table with headers"""
    table = doc.add_table(rows=1 + len(rows), cols=len(headers))

    # Header row
    header_cells = table.rows[0].cells
    for i, header in enumerate(headers):
        header_cells[i].text = header
        for paragraph in header_cells[i].paragraphs:
            for run in paragraph.runs:
                run.bold = True

    # Data rows
    for row_idx, row_data in enumerate(rows):
        row_cells = table.rows[row_idx + 1].cells
        for col_idx, cell_data in enumerate(row_data):
            row_cells[col_idx].text = str(cell_data)

    return table

def update_document():
    print("Bat dau cap nhat tai lieu...")

    # Open existing document
    doc = Document(DOCX_PATH)

    # ============================================
    # PHAN MOI: TONG KET VA DANH GIA
    # ============================================
    print("Dang them phan Tong ket va Danh gia...")

    # Add page break and conclusion
    doc.add_page_break()
    add_heading(doc, "PHAN 6: TONG KET VA DANH GIA", 1)

    # 6.1 Thoi gian phat trien
    add_heading(doc, "6.1. Thoi gian phat trien du an", 2)
    add_paragraph(doc, "Du an duoc thuc hien tu ngay 01/06/2026 den ngay 02/08/2026, trai qua cac giai doan: khao sat va xac dinh yeu cau, phan tich thiet ke, trien khai theo tung nhom tinh nang, kiem thu va hoan thien tai lieu.")

    # 6.2 Muc do hoan thanh
    add_heading(doc, "6.2. Muc do hoan thanh du an", 2)
    add_paragraph(doc, "Nhom da hoan thanh toan bo cac nhom chuc nang chinh de ra ban dau, dat 56/58 ca kiem thu (96,6%). Hai diem con that dang duoc ghi nhan la chuc nang quen mat khau phia may chu va viec khach hang yeu cau doi tra tu tai khoan.")

    # Add completion table
    add_paragraph(doc, "Bang: Danh gia muc do hoan thanh theo nhom chuc nang")
    headers = ["Nhom chuc nang", "Muc do hoan thanh"]
    rows = [
        ["Tai khoan & xac thuc", "95%"],
        ["Mua sam truc tuyen", "100%"],
        ["Ban hang & don hang", "95%"],
        ["Quan ly san pham", "100%"],
        ["Quan ly kho", "100%"],
        ["Tra hang & bao hanh", "100%"],
        ["Khuyen mai", "100%"],
        ["Quan tri he thong", "90%"],
    ]
    add_table_with_header(doc, headers, rows)

    # 6.3 Kho khan
    add_heading(doc, "6.3. Nhung kho khan gap phai va cach giai quyet", 2)

    add_paragraph(doc, "Bang: Tong ket kho khan va cach giai quyet")
    headers = ["STT", "Kho khan", "Cach giai quyet"]
    rows = [
        ["1", "Thiet ke schema cho serial tracking", "Tach bang chi_tiet_san_pham rieng, dung bang noi"],
        ["2", "Race condition khi nhieu POS cung ban", "Them khoa bi quan PESSIMISTIC_WRITE tren ton kho"],
        ["3", "Phan quyen chi tiet cho 4 vai tro", "Thiet ke bang chuc_vu voi cap_do, dung @PreAuthorize"],
        ["4", "Chuan hoa thong so ky thuat", "Tach thanh 4 bang dm_cpu, dm_ram, dm_o_cung, dm_gpu"],
        ["5", "SSE bi ngat ket noi khi chuyen tab", "Implement reconnect voi EventSource polyfill"],
        ["6", "Da ngon ngu voi do dai chuoi khac nhau", "Test voi 5 ngon ngu; dung flex layout"],
    ]
    add_table_with_header(doc, headers, rows)

    # 6.4 Bai hoc
    add_heading(doc, "6.4. Nhung bai hoc rut ra", 2)

    lessons = [
        "Phan tich nghiep vu ky truoc khi thiet ke co so du lieu. Mot quyet dinh sai o tang du lieu se keo theo chi phi sua chua rat lon o cac tang phia tren.",
        "Viet tai lieu dac ta truoc khi lap trinh giup ca nhom hieu thong nhat ve tinh nang, giam han viec phai lam di lam lai.",
        "Quy uoc dat ten thong nhat giua co so du lieu, may chu va giao dien giup viec tim va sua loi nhanh hon nhieu.",
        "Kiem thu tu dong la khoan dau tu xung dang: nhieu loi phat sinh do sua ma o module nay lam hong module khac da duoc phat hien ngay thay vi toi luc demo.",
        "Kiem tra quyen phai luon thuc hien o phia may chu; viec an nut tren giao dien chi la lop trai nghiem, khong phai lop bao mat.",
        "Chia nho cong viec va lam phan rui ro cao truoc giup nhom co thoi gian xu ly khi gap van de ngoai du kien.",
        "Giao tiep trong nhom quan trong khong kem ky thuat: hop ngan dinh ky giup phat hien som thanh vien dang bi vuong.",
        "Khong nen xoa cung du lieu da phat sinh nghiep vu; xoa mem (da_xoa = 1) giu duoc lich su va tranh lam hong cac bao cao cu."
    ]

    for lesson in lessons:
        p = doc.add_paragraph("  - " + lesson)

    # 6.5 Ke hoach tuong lai
    add_heading(doc, "6.5. Ke hoach phat trien trong tuong lai", 2)
    add_paragraph(doc, "Trong khuon khong thoi gian cua ky do an, nhom da hoan thanh cac chuc nang cot loi. Neu tiep tuc phat trien, nhom du kien bo sung:")

    future_plans = [
        "Tich hop cong thanh toan truc tuyen (VNPay, MoMo) va tu dong doi soat giao dich",
        "Ket noi API cua don vi van chuyen de tu dong lay ma van don va cap nhat trang thai giao hang",
        "Xay dung tinh nang chat truc tuyen giua khach hang va nhan vien tu van",
        "Bo sung chuc nang dung cau hinh may tinh theo linh kien (build PC) voi kiem tra tuong thich",
        "Goi y san pham dua tren lich su xem va lich su mua cua khach hang",
        "Phat trien ung dung di dong cho nhan vien kho, ho tro quet ma vach serial bang camera dien thoai",
        "Bo sung bao cao phan tich chuyen sau: ti suat loi nhuan theo dong san pham, du bao nhu cau nhap hang",
        "Mo rong he thong cho mo hinh nhieu chi nhanh, ho tro chuyen hang giua cac kho"
    ]

    for i, plan in enumerate(future_plans, 1):
        p = doc.add_paragraph(f"  {i}. " + plan)

    # ============================================
    # PHAN HINH ANH GIAO DIEN THUC TE
    # ============================================
    print("Dang them hinh anh giao dien...")

    doc.add_page_break()
    add_heading(doc, "PHU LUC E: HINH ANH GIAO DIEN THUC TE", 1)
    add_paragraph(doc, "Phan nay trinh bay cac hinh anh chup thuc te tu he thong SAOClub da duoc trien khai. Cac hinh anh duoc chup trong qua trinh kiem thu va demo voi giang vien huong dan.")

    # Giao dien khach hang
    add_heading(doc, "E.1. Giao dien khach hang", 2)

    screenshots_customer = [
        ("01-trang-chu.png", "Hinh E.1: Trang chu - Homepage"),
        ("02-danh-sach-san-pham.png", "Hinh E.2: Danh sach san pham"),
        ("03-chi-tiet-san-pham.png", "Hinh E.3: Chi tiet san pham"),
        ("04-so-sanh-san-pham.png", "Hinh E.4: So sanh san pham"),
        ("05-gio-hang.png", "Hinh E.5: Gio hang"),
        ("06-dat-hang.png", "Hinh E.6: Dat hang"),
        ("07-tai-khoan-don-hang.png", "Hinh E.7: Tai khoan - Don hang"),
        ("08-tich-diem-vong-quay.png", "Hinh E.8: Tich diem va vong quay"),
        ("09-dang-nhap.png", "Hinh E.9: Trang dang nhap"),
    ]

    for filename, caption in screenshots_customer:
        img_path = os.path.join(SCREENSHOTS_DIR, filename)
        if os.path.exists(img_path):
            add_screenshot(doc, img_path, caption)
            doc.add_paragraph()  # spacing

    # Giao dien quan tri
    add_heading(doc, "E.2. Giao dien quan tri", 2)

    screenshots_admin = [
        ("10-admin-dashboard.png", "Hinh E.10: Dashboard - Tong quan"),
        ("11-admin-san-pham.png", "Hinh E.11: Quan ly san pham"),
        ("12-admin-them-san-pham.png", "Hinh E.12: Them/Sua san pham"),
        ("13-admin-don-hang.png", "Hinh E.13: Quan ly don hang"),
        ("14-admin-pos.png", "Hinh E.14: Ban hang tai quay (POS)"),
        ("15-admin-chon-serial.png", "Hinh E.15: Chon serial khi dong goi"),
        ("16-admin-ton-kho.png", "Hinh E.16: Quan ly ton kho"),
        ("17-admin-nhap-kho.png", "Hinh E.17: Phieu nhap kho"),
        ("18-admin-tra-hang.png", "Hinh E.18: Quan ly tra hang"),
        ("19-admin-bao-hanh.png", "Hinh E.19: Quan ly bao hanh"),
        ("20-admin-khuyen-mai.png", "Hinh E.20: Quan ly khuyen mai"),
        ("21-admin-khach-hang.png", "Hinh E.21: Quan ly khach hang"),
        ("22-admin-nhan-vien.png", "Hinh E.22: Quan ly nhan vien"),
        ("23-admin-bao-cao.png", "Hinh E.23: Bao cao thong ke"),
        ("24-admin-cai-dat.png", "Hinh E.24: Cai dat he thong"),
    ]

    for filename, caption in screenshots_admin:
        img_path = os.path.join(SCREENSHOTS_DIR, filename)
        if os.path.exists(img_path):
            add_screenshot(doc, img_path, caption)
            doc.add_paragraph()  # spacing

    # ============================================
    # Save document
    # ============================================
    output_path = DOCX_PATH
    doc.save(output_path)
    print(f"\nDa luu tai lieu thanh cong: {output_path}")

    return output_path

if __name__ == "__main__":
    update_document()
