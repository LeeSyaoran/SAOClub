# -*- coding: utf-8 -*-
"""
Create a fresh docx with images properly inserted
"""
import sys
import os
import zipfile
import shutil
from lxml import etree

sys.stdout.reconfigure(encoding='utf-8')

OUTPUT_PATH = 'scripts/BaoCao_SD16_SAOClub_FINAL.docx'
SCREENSHOTS_DIR = 'docs/screenshots'

# Image mappings - correct order
IMAGES = [
    ('Hinh E.1', '01-trang-chu.png', 'Hinh E.1: Trang chu - Homepage'),
    ('Hinh E.2', '02-danh-sach-san-pham.png', 'Hinh E.2: Danh sach san pham'),
    ('Hinh E.3', '03-chi-tiet-san-pham.png', 'Hinh E.3: Chi tiet san pham'),
    ('Hinh E.4', '04-so-sanh-san-pham.png', 'Hinh E.4: So sanh san pham'),
    ('Hinh E.5', '05-gio-hang.png', 'Hinh E.5: Gio hang'),
    ('Hinh E.6', '06-dat-hang.png', 'Hinh E.6: Dat hang'),
    ('Hinh E.7', '07-tai-khoan-don-hang.png', 'Hinh E.7: Tai khoan - Don hang'),
    ('Hinh E.8', '08-tich-diem-vong-quay.png', 'Hinh E.8: Tich diem va vong quay'),
    ('Hinh E.9', '09-dang-nhap.png', 'Hinh E.9: Trang dang nhap'),
    ('Hinh E.10', '10-admin-dashboard.png', 'Hinh E.10: Dashboard - Tong quan'),
    ('Hinh E.11', '11-admin-san-pham.png', 'Hinh E.11: Quan ly san pham'),
    ('Hinh E.12', '12-admin-them-san-pham.png', 'Hinh E.12: Them/Sua san pham'),
    ('Hinh E.13', '13-admin-don-hang.png', 'Hinh E.13: Quan ly don hang'),
    ('Hinh E.14', '14-admin-pos.png', 'Hinh E.14: Ban hang tai quay (POS)'),
    ('Hinh E.15', '15-admin-chon-serial.png', 'Hinh E.15: Chon serial khi dong goi'),
    ('Hinh E.16', '16-admin-ton-kho.png', 'Hinh E.16: Quan ly ton kho'),
    ('Hinh E.17', '17-admin-nhap-kho.png', 'Hinh E.17: Phieu nhap kho'),
    ('Hinh E.18', '18-admin-tra-hang.png', 'Hinh E.18: Quan ly tra hang'),
    ('Hinh E.19', '19-admin-bao-hanh.png', 'Hinh E.19: Quan ly bao hanh'),
    ('Hinh E.20', '20-admin-khuyen-mai.png', 'Hinh E.20: Quan ly khuyen mai'),
    ('Hinh E.21', '21-admin-khach-hang.png', 'Hinh E.21: Quan ly khach hang'),
    ('Hinh E.22', '22-admin-nhan-vien.png', 'Hinh E.22: Quan ly nhan vien'),
    ('Hinh E.23', '23-admin-bao-cao.png', 'Hinh E.23: Bao cao thong ke'),
    ('Hinh E.24', '24-admin-cai-dat.png', 'Hinh E.24: Cai dat he thong'),
]

def create_image_paragraph(rel_id, width_inches=6.0):
    """Create paragraph with centered image"""
    w = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
    r = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships'

    width_emu = int(width_inches * 914400)
    height_emu = int(width_emu * 1080 / 1920)

    p = etree.Element(f'{{{w}}}p')

    # Center alignment
    pPr = etree.SubElement(p, f'{{{w}}}pPr')
    jc = etree.SubElement(pPr, f'{{{w}}}jc')
    jc.set(f'{{{w}}}val', 'center')

    # Run with drawing
    r_elem = etree.SubElement(p, f'{{{w}}}r')

    # Drawing element
    drawing = etree.SubElement(r_elem, f'{{{w}}}drawing')

    # Inline element
    inline = etree.SubElement(drawing,
        f'{{{w}}}inline',
        {
            f'{{{w}}}distT': '0',
            f'{{{w}}}distB': '0',
            f'{{{w}}}distL': '0',
            f'{{{w}}}distR': '0'
        }
    )

    # Extent
    extent = etree.SubElement(inline, f'{{{w}}}extent')
    extent.set('cx', str(width_emu))
    extent.set('cy', str(height_emu))

    # DocProperties
    docPr = etree.SubElement(inline, f'{{{w}}}docPr')
    docPr.set('id', str(rel_id))
    docPr.set('name', f'Picture {rel_id}')

    # Graphic
    graphic = etree.SubElement(inline, f'{{{w}}}graphic')
    graphicData = etree.SubElement(graphic, f'{{{w}}}graphicData')
    graphicData.set('uri', 'http://schemas.openxmlformats.org/drawingml/2006/picture')

    # Picture
    pic = etree.SubElement(graphicData, f'{{{w}}}pic')

    # nvPicPr
    nvPicPr = etree.SubElement(pic, f'{{{w}}}nvPicPr')
    cNvPr = etree.SubElement(nvPicPr, f'{{{w}}}cNvPr')
    cNvPr.set('id', str(rel_id))
    cNvPr.set('name', f'Picture {rel_id}')
    cNvPicPr = etree.SubElement(nvPicPr, f'{{{w}}}cNvPicPr')

    # blipFill
    blipFill = etree.SubElement(pic, f'{{{w}}}blipFill')
    blip = etree.SubElement(blipFill, f'{{{w}}}blip')
    blip.set(f'{{{r}}}embed', f'rId{rel_id}')
    stretch = etree.SubElement(blipFill, f'{{{w}}}stretch')
    fillRect = etree.SubElement(stretch, f'{{{w}}}fillRect')

    # spPr
    spPr = etree.SubElement(pic, f'{{{w}}}spPr')
    xfrm = etree.SubElement(spPr, f'{{{w}}}xfrm')
    off = etree.SubElement(xfrm, f'{{{w}}}off')
    off.set('x', '0')
    off.set('y', '0')
    ext = etree.SubElement(xfrm, f'{{{w}}}ext')
    ext.set('cx', str(width_emu))
    ext.set('cy', str(height_emu))
    prstGeom = etree.SubElement(spPr, f'{{{w}}}prstGeom')
    prstGeom.set('prst', 'rect')
    avLst = etree.SubElement(prstGeom, f'{{{w}}}avLst')

    return p

def create_caption_paragraph(text):
    """Create centered italic caption"""
    w = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'

    p = etree.Element(f'{{{w}}}p')

    # Center alignment
    pPr = etree.SubElement(p, f'{{{w}}}pPr')
    jc = etree.SubElement(pPr, f'{{{w}}}jc')
    jc.set(f'{{{w}}}val', 'center')

    # Run with italic text
    r_elem = etree.SubElement(p, f'{{{w}}}r')
    rPr = etree.SubElement(r_elem, f'{{{w}}}rPr')
    i = etree.SubElement(rPr, f'{{{w}}}i')
    sz = etree.SubElement(rPr, f'{{{w}}}sz')
    sz.set(f'{{{w}}}val', '20')

    t = etree.SubElement(r_elem, f'{{{w}}}t')
    t.text = text

    return p

def main():
    print("=" * 60)
    print("TAO BAO CAO VOI HINH ANH")
    print("=" * 60)

    # Create fresh docx from original
    source = 'scripts/BaoCao_SD16_SAOClub.docx'

    if os.path.exists(OUTPUT_PATH):
        os.remove(OUTPUT_PATH)

    # Copy source
    print("\n1. Copying source document...")
    shutil.copy(source, OUTPUT_PATH)

    # Extract
    extract_dir = 'temp_build'
    if os.path.exists(extract_dir):
        shutil.rmtree(extract_dir)

    print("2. Extracting...")
    with zipfile.ZipFile(OUTPUT_PATH, 'r') as zf:
        zf.extractall(extract_dir)

    # Read document.xml
    doc_path = os.path.join(extract_dir, 'word', 'document.xml')
    tree = etree.parse(doc_path)
    root = tree.getroot()

    w = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'

    # Find body
    body = root.find(f'.//{{{w}}}body')
    if body is None:
        body = root

    # Find all paragraph elements
    all_elems = list(body)

    # Find "Hinh E.X" paragraph positions
    figure_positions = []
    for i, elem in enumerate(all_elems):
        if elem.tag == f'{{{w}}}p':
            texts = elem.findall(f'.//{{{w}}}t')
            text = ''.join([t.text or '' for t in texts])
            for idx, (fig_key, _, _) in enumerate(IMAGES):
                if f'{fig_key}:' in text:
                    figure_positions.append((i, idx))
                    break

    print(f"3. Found {len(figure_positions)} figure positions")

    # Copy images to media folder
    media_dir = os.path.join(extract_dir, 'word', 'media')
    os.makedirs(media_dir, exist_ok=True)

    for rel_id, (fig_key, img_file, _) in enumerate(IMAGES, 1):
        src = os.path.join(SCREENSHOTS_DIR, img_file)
        dst = os.path.join(media_dir, f'image{rel_id}.png')
        if os.path.exists(src):
            shutil.copy(src, dst)
            print(f"   Copied: image{rel_id}.png")
        else:
            print(f"   MISSING: {img_file}")

    # Update relationships
    rels_path = os.path.join(extract_dir, 'word', '_rels', 'document.xml.rels')
    rels_tree = etree.parse(rels_path)
    rels_root = rels_tree.getroot()

    # Find max rId
    max_rid = 0
    for rel in rels_root:
        rid = rel.get('Id', '')
        if rid.startswith('rId'):
            try:
                max_rid = max(max_rid, int(rid[3:]))
            except:
                pass

    # Add image relationships starting after max_rid
    base_rid = max_rid + 1
    for rel_id, (fig_key, img_file, _) in enumerate(IMAGES, 1):
        rel = etree.SubElement(rels_root, 'Relationship')
        rel.set('Id', f'rId{base_rid + rel_id - 1}')
        rel.set('Type', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/image')
        rel.set('Target', f'media/image{rel_id}.png')

    rels_tree.write(rels_path, xml_declaration=True, encoding='UTF-8', standalone=True)

    # Insert images into document - process in reverse order
    print("\n4. Inserting images...")
    inserted = 0

    for pos, img_idx in reversed(figure_positions):
        rel_id = base_rid + img_idx

        # Create image paragraph
        img_para = create_image_paragraph(rel_id)

        # Insert BEFORE the caption paragraph
        body.insert(pos, img_para)
        inserted += 1
        print(f"   Inserted image at pos {pos}: {IMAGES[img_idx][0]}")

    print(f"\n   Total inserted: {inserted}")

    # Save
    print("\n5. Saving document.xml...")
    tree.write(doc_path, xml_declaration=True, encoding='UTF-8', standalone=True)

    # Repack
    print("6. Repacking...")
    if os.path.exists(OUTPUT_PATH):
        os.remove(OUTPUT_PATH)

    with zipfile.ZipFile(OUTPUT_PATH, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root_dir, dirs, files in os.walk(extract_dir):
            for file in files:
                file_path = os.path.join(root_dir, file)
                arc_name = os.path.relpath(file_path, extract_dir)
                zf.write(file_path, arc_name)

    # Cleanup
    shutil.rmtree(extract_dir)

    print(f"\n7. HOAN THANH!")
    print(f"   Output: {OUTPUT_PATH}")
    print(f"   Size: {os.path.getsize(OUTPUT_PATH) / 1024 / 1024:.2f} MB")

if __name__ == "__main__":
    main()
