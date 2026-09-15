# -*- coding: utf-8 -*-
"""
Insert images into BaoCao paragraphs
"""
import sys
import os
import zipfile
import shutil
from lxml import etree

sys.stdout.reconfigure(encoding='utf-8')

DOCX_PATH = 'scripts/BaoCao_SD16_SAOClub.docx'
OUTPUT_PATH = 'scripts/BaoCao_SD16_SAOClub_v2.docx'
SCREENSHOTS_DIR = 'docs/screenshots'

# Image mappings
SCREENSHOTS = [
    ('Hinh E.1', '01-trang-chu.png'),
    ('Hinh E.2', '02-danh-sach-san-pham.png'),
    ('Hinh E.3', '03-chi-tiet-san-pham.png'),
    ('Hinh E.4', '04-so-sanh-san-pham.png'),
    ('Hinh E.5', '05-gio-hang.png'),
    ('Hinh E.6', '06-dat-hang.png'),
    ('Hinh E.7', '07-tai-khoan-don-hang.png'),
    ('Hinh E.8', '08-tich-diem-vong-quay.png'),
    ('Hinh E.9', '09-dang-nhap.png'),
    ('Hinh E.10', '10-admin-dashboard.png'),
    ('Hinh E.11', '11-admin-san-pham.png'),
    ('Hinh E.12', '12-admin-them-san-pham.png'),
    ('Hinh E.13', '13-admin-don-hang.png'),
    ('Hinh E.14', '14-admin-pos.png'),
    ('Hinh E.15', '15-admin-chon-serial.png'),
    ('Hinh E.16', '16-admin-ton-kho.png'),
    ('Hinh E.17', '17-admin-nhap-kho.png'),
    ('Hinh E.18', '18-admin-tra-hang.png'),
    ('Hinh E.19', '19-admin-bao-hanh.png'),
    ('Hinh E.20', '20-admin-khuyen-mai.png'),
    ('Hinh E.21', '21-admin-khach-hang.png'),
    ('Hinh E.22', '22-admin-nhan-vien.png'),
    ('Hinh E.23', '23-admin-bao-cao.png'),
    ('Hinh E.24', '24-admin-cai-dat.png'),
]

def create_drawing_xml(image_rel_id, width_inches=6.0):
    """Create Word Drawing XML for image"""
    # Image dimensions: 1920x1080
    width_px = 1920
    height_px = 1080

    # Convert to EMU (English Metric Units) - 914400 EMU per inch
    width_emu = int(width_inches * 914400)
    height_emu = int(width_emu * height_px / width_px)

    xml = f'''<w:drawing xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing/inline" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
      <wp:inline distT="0" distB="0" distL="0" distR="0">
        <wp:extent cx="{width_emu}" cy="{height_emu}"/>
        <wp:docPr id="1" name="Picture {image_rel_id}"/>
        <a:graphic>
          <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
            <pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
              <pic:nvPicPr>
                <pic:cNvPr id="1" name="Picture {image_rel_id}"/>
                <pic:cNvPicPr/>
              </pic:nvPicPr>
              <pic:blipFill>
                <a:blip r:embed="rId{image_rel_id}"/>
                <a:stretch>
                  <a:fillRect/>
                </a:stretch>
              </pic:blipFill>
              <pic:spPr>
                <a:xfrm>
                  <a:off x="0" y="0"/>
                  <a:ext cx="{width_emu}" cy="{height_emu}"/>
                </a:xfrm>
                <a:prstGeom prst="rect">
                  <a:avLst/>
                </a:prstGeom>
              </pic:spPr>
            </pic:pic>
          </a:graphicData>
        </a:graphic>
      </wp:inline>
    </w:drawing>'''
    return etree.fromstring(xml)

def create_paragraph_with_center_align():
    """Create a paragraph with center alignment"""
    w = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
    p = etree.Element(f'{w}p')
    pPr = etree.SubElement(p, f'{w}pPr')
    jc = etree.SubElement(pPr, f'{w}jc')
    jc.set(f'{w}val', 'center')
    return p

def create_image_paragraph(image_rel_id, width_inches=6.0):
    """Create a paragraph containing an image"""
    w = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
    p = create_paragraph_with_center_align()

    # Add run
    r = etree.SubElement(p, f'{w}r')
    rPr = etree.SubElement(r, f'{w}rPr')

    # Add drawing
    drawing = create_drawing_xml(image_rel_id, width_inches)
    r.append(drawing)

    return p

def create_caption_paragraph(text):
    """Create a centered italic caption paragraph"""
    w = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
    p = create_paragraph_with_center_align()

    r = etree.SubElement(p, f'{w}r')
    rPr = etree.SubElement(r, f'{w}rPr')
    i = etree.SubElement(rPr, f'{w}i')
    sz = etree.SubElement(rPr, f'{w}sz')
    sz.set(f'{w}val', '20')  # 10pt

    t = etree.SubElement(r, f'{w}t')
    t.text = text

    return p

def main():
    print("=" * 60)
    print("CHEN HINH ANH VAO BAO CAO")
    print("=" * 60)

    # Copy original file
    print("\n1. Copying original document...")
    shutil.copy(DOCX_PATH, OUTPUT_PATH)

    # Extract
    extract_dir = 'temp_docx'
    if os.path.exists(extract_dir):
        shutil.rmtree(extract_dir)
    os.makedirs(extract_dir)

    print("2. Extracting document...")
    with zipfile.ZipFile(OUTPUT_PATH, 'r') as zf:
        zf.extractall(extract_dir)

    # Parse document.xml
    doc_xml = os.path.join(extract_dir, 'word', 'document.xml')
    tree = etree.parse(doc_xml)
    root = tree.getroot()

    w = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
    r_ns = '{http://schemas.openxmlformats.org/officeDocument/2006/relationships}'

    # Find body element
    body = root.find('.//w:body', {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'})
    if body is None:
        body = root

    # Find all paragraphs
    all_elements = list(body)

    # Find paragraphs with "Hinh E.X" text and their indices
    figure_positions = []
    for i, elem in enumerate(all_elements):
        if elem.tag == f'{w}p':
            # Get text
            texts = elem.findall(f'.//{w}t')
            para_text = ''.join([t.text for t in texts if t.text])
            for fig_key, _ in SCREENSHOTS:
                if fig_key in para_text and ':' in para_text:
                    figure_positions.append((i, fig_key, para_text))
                    break

    print(f"3. Found {len(figure_positions)} figure paragraphs")
    for pos, fig, text in figure_positions[:5]:
        print(f"   {pos}: {fig} - {text[:50]}")

    # Create media folder and copy images
    media_dir = os.path.join(extract_dir, 'word', 'media')
    os.makedirs(media_dir, exist_ok=True)

    # Build image info
    image_files = {}
    for rel_id, (fig_key, img_file) in enumerate(SCREENSHOTS, start=1):
        img_path = os.path.join(SCREENSHOTS_DIR, img_file)
        if os.path.exists(img_path):
            ext = os.path.splitext(img_file)[1]
            media_name = f'image{rel_id}{ext}'
            dest_path = os.path.join(media_dir, media_name)
            shutil.copy(img_path, dest_path)
            image_files[fig_key] = (rel_id, media_name)
            print(f"   Copied: {img_file} -> {media_name}")
        else:
            print(f"   WARNING: {img_file} not found!")

    # Update document.xml.rels
    rels_file = os.path.join(extract_dir, 'word', '_rels', 'document.xml.rels')
    rels_tree = etree.parse(rels_file)
    rels_root = rels_tree.getroot()

    # Find max existing rId
    max_rid = 0
    for rel in rels_root:
        rid = rel.get('Id', '')
        if rid.startswith('rId'):
            try:
                num = int(rid[3:])
                max_rid = max(max_rid, num)
            except:
                pass

    # Add image relationships
    rid_offset = max_rid + 1
    for fig_key, (rel_id, media_name) in image_files.items():
        rel = etree.SubElement(rels_root, 'Relationship')
        rel.set('Id', f'rId{rid_offset + rel_id - 1}')
        rel.set('Type', 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/image')
        rel.set('Target', f'media/{media_name}')

    rels_tree.write(rels_file, xml_declaration=True, encoding='UTF-8', standalone=True)

    # Now insert images into document - work backwards to not mess up indices
    print("\n4. Inserting images into document...")
    inserted_count = 0

    for i in range(len(all_elements) - 1, -1, -1):
        elem = all_elements[i]
        if elem.tag == f'{w}p':
            texts = elem.findall(f'.//{w}t')
            para_text = ''.join([t.text for t in texts if t.text])

            for fig_key, img_file in SCREENSHOTS:
                if fig_key in para_text and ':' in para_text:
                    # Insert image BEFORE this paragraph
                    if fig_key in image_files:
                        rel_id, _ = image_files[fig_key]
                        actual_rid = rid_offset + rel_id - 1

                        # Create image paragraph
                        img_para = create_image_paragraph(actual_rid)

                        # Insert at position
                        body.insert(i, img_para)
                        inserted_count += 1
                        print(f"   Inserted image at position {i}: {fig_key}")

    print(f"\n5. Inserted {inserted_count} images")

    # Save document.xml
    tree.write(doc_xml, xml_declaration=True, encoding='UTF-8', standalone=True)

    # Repack
    print("\n6. Repacking document...")
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

    print(f"\n7. Done!")
    print(f"   Output: {OUTPUT_PATH}")
    print(f"   Size: {os.path.getsize(OUTPUT_PATH) / 1024 / 1024:.2f} MB")

if __name__ == "__main__":
    main()
