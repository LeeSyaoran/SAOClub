<template>
  <div class="kh">
    <!-- ══════════════ THANH CÔNG CỤ ══════════════ -->
    <header class="kh-bar">
      <div class="kh-bar__left">
        <h2 class="kh-title">Kho hàng</h2>
        <div class="kh-search">
          <i class="fa fa-search kh-search__icon"></i>
          <input v-model="tuKhoa" type="text" placeholder="Tìm theo SKU, tên sản phẩm, serial…" />
          <button v-if="tuKhoa" class="kh-search__clear" @click="tuKhoa = ''"><i class="fa fa-times"></i></button>
        </div>
      </div>

      <div class="kh-bar__actions">
        <button class="kh-btn kh-btn--ghost" :class="{ 'is-on': moBoLoc }" @click="moBoLoc = !moBoLoc">
          <i class="fa fa-filter"></i> Bộ lọc
          <span v-if="soBoLoc" class="kh-chip">{{ soBoLoc }}</span>
        </button>
        <button class="kh-btn kh-btn--primary" @click="moNhapHang">
          <i class="fa fa-download"></i> Nhập hàng
        </button>
        <button class="kh-btn kh-btn--ghost" @click="moPhieuNhap">
          <i class="fa fa-file-text-o"></i> Phiếu nhập
        </button>
        <button class="kh-btn kh-btn--ghost" :disabled="!dongDaLoc.length" @click="xuatCsv">
          <i class="fa fa-file-excel-o"></i> Xuất file
        </button>
        <button class="kh-icon-btn" title="Tải lại" @click="taiDuLieu">
          <i class="fa fa-refresh" :class="{ 'fa-spin': dangTai }"></i>
        </button>
      </div>
    </header>

    <!-- ══════════════ THẺ TỔNG QUAN ══════════════ -->
    <section class="kh-cards">
      <article class="kh-card">
        <span class="kh-card__label">Biến thể đang bán</span>
        <strong class="kh-card__value">{{ formatSo(thongKe.tongSku) }}</strong>
      </article>
      <article class="kh-card">
        <span class="kh-card__label">Tổng máy trong kho</span>
        <strong class="kh-card__value">{{ formatSo(thongKe.tongTon) }}</strong>
      </article>
      <article class="kh-card kh-card--warn">
        <span class="kh-card__label">Sắp hết hàng</span>
        <strong class="kh-card__value">{{ formatSo(thongKe.sapHet) }}</strong>
      </article>
      <article class="kh-card kh-card--danger">
        <span class="kh-card__label">Hết hàng</span>
        <strong class="kh-card__value">{{ formatSo(thongKe.hetHang) }}</strong>
      </article>
      <article class="kh-card">
        <span class="kh-card__label">Giá trị tồn (giá vốn)</span>
        <strong class="kh-card__value">{{ formatSo(thongKe.giaTriTon) }} ₫</strong>
      </article>
    </section>

    <!-- ══════════════ BỘ LỌC ══════════════ -->
    <section class="kh-filter" :class="{ 'is-open': moBoLoc }">
      <div class="kh-filter__panel">
        <div class="kh-filter__grid">
          <label class="kh-field">
            <span>Thương hiệu</span>
            <select v-model="loc.thuongHieu">
              <option value="">Tất cả</option>
              <option v-for="th in dsThuongHieu" :key="th" :value="th">{{ th }}</option>
            </select>
          </label>
          <label class="kh-field">
            <span>Tình trạng tồn</span>
            <select v-model="loc.tinhTrang">
              <option value="">Tất cả</option>
              <option value="con">Còn hàng</option>
              <option value="sap_het">Sắp hết (≤ tồn tối thiểu)</option>
              <option value="het">Hết hàng</option>
            </select>
          </label>
          <label class="kh-field">
            <span>Trạng thái biến thể</span>
            <select v-model="loc.trangThai">
              <option value="">Tất cả</option>
              <option value="active">Đang bán</option>
              <option value="inactive">Ngừng bán</option>
            </select>
          </label>
          <label class="kh-field">
            <span>Quản lý serial</span>
            <select v-model="loc.coSerial">
              <option value="">Tất cả</option>
              <option value="co">Có serial</option>
              <option value="khong">Chưa có serial</option>
            </select>
          </label>
        </div>
        <div class="kh-filter__foot">
          <span class="kh-muted">{{ dongDaLoc.length }} biến thể khớp bộ lọc</span>
          <div class="kh-inline">
            <button class="kh-btn kh-btn--ghost kh-btn--sm" @click="xoaLoc"><i class="fa fa-eraser"></i> Xóa lọc</button>
            <button class="kh-btn kh-btn--primary kh-btn--sm" @click="moBoLoc = false">Xong</button>
          </div>
        </div>
      </div>
    </section>

    <!-- ══════════════ BẢNG TỒN KHO ══════════════ -->
    <section class="kh-panel">
      <p v-if="loiTai" class="kh-alert">{{ loiTai }} <button class="kh-link" @click="taiDuLieu">Thử lại</button></p>

      <div class="kh-table-wrap">
        <table class="kh-table">
          <thead>
            <tr>
              <th class="kh-col-caret"></th>
              <th>Mã SKU</th>
              <th>Sản phẩm</th>
              <th>Cấu hình</th>
              <th class="ta-r">Giá vốn</th>
              <th class="ta-r">Giá bán</th>
              <th class="ta-r">Tồn</th>
              <th class="ta-r">Giữ</th>
              <th class="ta-r">Có thể bán</th>
              <th class="ta-r">Tối thiểu</th>
              <th>Tình trạng</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="d in dongDaLoc" :key="d.bienTheId">
              <tr class="kh-row" :class="{ 'is-open': dongMo === d.bienTheId }" @click="moDong(d)">
                <td><i class="fa" :class="dongMo === d.bienTheId ? 'fa-chevron-down' : 'fa-chevron-right'"></i></td>
                <td class="kh-sku">{{ d.maSku }}</td>
                <td>
                  <div class="kh-name">
                    <img :src="d.hinhAnhBienThe || d.hinhAnhChinh || ANH_MAC_DINH" class="kh-thumb" alt="" @error="anhLoi" />
                    <div>
                      <div class="kh-name__main">{{ d.tenSanPham }}</div>
                      <div class="kh-name__sub">{{ d.maSanPham }} · {{ d.tenThuongHieu || '—' }}</div>
                    </div>
                  </div>
                </td>
                <td class="kh-muted">{{ cauHinh(d) || '—' }}</td>
                <td class="ta-r kh-muted">{{ formatSo(d.giaNhap) }}</td>
                <td class="ta-r"><strong>{{ formatSo(d.giaBan) }}</strong></td>
                <td class="ta-r"><strong>{{ formatSo(d.tonThucTe) }}</strong></td>
                <td class="ta-r kh-muted">{{ formatSo(d.dangGiu) }}</td>
                <td class="ta-r">{{ formatSo(d.coTheBan) }}</td>
                <td class="ta-r kh-muted">{{ formatSo(d.tonToiThieu) }}</td>
                <td><span class="kh-tag" :class="lopTinhTrang(d)">{{ nhanTinhTrang(d) }}</span></td>
              </tr>

              <!-- ══ Chi tiết biến thể ══ -->
              <tr v-if="dongMo === d.bienTheId" class="kh-row-detail">
                <td colspan="11">
                  <div class="kh-detail">
                    <nav class="kh-tabs">
                      <button v-for="t in tabsChiTiet" :key="t.key" class="kh-tab"
                              :class="{ 'is-on': tabChiTiet === t.key }" @click="tabChiTiet = t.key">
                        {{ t.label }}
                        <span v-if="t.key === 'serial' && serials.length" class="kh-chip">{{ serials.length }}</span>
                      </button>
                    </nav>

                    <p v-if="loiChiTiet" class="kh-alert">{{ loiChiTiet }}</p>

                    <!-- ── Tab 1: chỉnh sửa mọi trường ── -->
                    <div v-show="tabChiTiet === 'sua'">
                      <div class="kh-grid">
                        <label class="kh-field">
                          <span>Mã SKU <b>*</b></span>
                          <input v-model.trim="formSua.maSku" />
                        </label>
                        <label class="kh-field">
                          <span>Màu sắc</span>
                          <input v-model.trim="formSua.mauSac" />
                        </label>
                        <label class="kh-field">
                          <span>Giá nhập (₫) <b>*</b></span>
                          <input type="number" min="0" step="1000" v-model="formSua.giaNhap" />
                        </label>
                        <label class="kh-field">
                          <span>Giá bán (₫) <b>*</b></span>
                          <input type="number" min="0" step="1000" v-model="formSua.giaBan" />
                          <em v-if="loiGia" class="kh-err">{{ loiGia }}</em>
                        </label>
                        <label class="kh-field">
                          <span>Bảo hành (tháng)</span>
                          <input type="number" min="0" v-model="formSua.baoHanhThang" />
                        </label>
                        <label class="kh-field">
                          <span>Trạng thái</span>
                          <select v-model="formSua.trangThai">
                            <option value="active">Đang bán</option>
                            <option value="inactive">Ngừng bán</option>
                          </select>
                        </label>
                        <label class="kh-field">
                          <span>CPU</span>
                          <select v-model="formSua.cpuId">
                            <option value="">-- Không chọn --</option>
                            <option v-for="o in dmCpu" :key="o.cpuId ?? o.id" :value="o.cpuId ?? o.id">{{ o.tenCpu }}</option>
                          </select>
                        </label>
                        <label class="kh-field">
                          <span>RAM</span>
                          <select v-model="formSua.ramId">
                            <option value="">-- Không chọn --</option>
                            <option v-for="o in dmRam" :key="o.ramId ?? o.id" :value="o.ramId ?? o.id">{{ o.dungLuong || o.tenRam }}</option>
                          </select>
                        </label>
                        <label class="kh-field">
                          <span>Ổ cứng</span>
                          <select v-model="formSua.oCungId">
                            <option value="">-- Không chọn --</option>
                            <option v-for="o in dmOCung" :key="o.oCungId ?? o.id" :value="o.oCungId ?? o.id">{{ o.loaiOCung || o.loaiOcung }}</option>
                          </select>
                        </label>
                        <label class="kh-field">
                          <span>GPU</span>
                          <select v-model="formSua.gpuId">
                            <option value="">-- Không chọn --</option>
                            <option v-for="o in dmGpu" :key="o.gpuId ?? o.id" :value="o.gpuId ?? o.id">{{ o.tenGpu }}</option>
                          </select>
                        </label>
                        <label class="kh-field">
                          <span>Màn hình</span>
                          <input v-model.trim="formSua.kichThuocManHinh" />
                        </label>
                        <label class="kh-field">
                          <span>Hệ điều hành</span>
                          <input v-model.trim="formSua.heDieuHanh" />
                        </label>
                        <label class="kh-field">
                          <span>Pin</span>
                          <input v-model.trim="formSua.pin" />
                        </label>
                        <label class="kh-field">
                          <span>Trọng lượng (kg)</span>
                          <input type="number" step="0.01" min="0" v-model="formSua.trongLuongKg" />
                        </label>
                        <label class="kh-field">
                          <span>Tồn tối thiểu</span>
                          <input type="number" min="0" v-model="formSua.tonToiThieu" />
                        </label>
                        <label class="kh-field">
                          <span>Phân loại (tags)</span>
                          <input v-model.trim="formSua.phanLoaiTags" placeholder="gaming,do_hoa" />
                        </label>
                        <label class="kh-field kh-field--wide">
                          <span>Ảnh phiên bản</span>
                          <input v-model.trim="formSua.hinhAnhBienThe" placeholder="/images/ten-anh.webp" />
                        </label>
                      </div>

                      <div class="kh-detail__foot">
                        <span class="kh-muted">Tồn kho không sửa tay ở đây — thay đổi qua nhập hàng hoặc trạng thái serial.</span>
                        <button class="kh-btn kh-btn--primary kh-btn--sm" :disabled="dangLuu" @click.stop="luuBienThe(d)">
                          <i class="fa" :class="dangLuu ? 'fa-spinner fa-spin' : 'fa-check'"></i> Lưu thay đổi
                        </button>
                      </div>
                    </div>

                    <!-- ── Tab 2: serial ── -->
                    <div v-show="tabChiTiet === 'serial'">
                      <div class="kh-serial-head">
                        <div class="kh-inline">
                          <span v-for="(so, tt) in demSerial" :key="tt" class="kh-tag kh-tag--soft">
                            {{ nhanSerial(tt) }}: {{ so }}
                          </span>
                        </div>
                        <input v-model.trim="timSerial" class="kh-input-sm" placeholder="Lọc serial…" />
                      </div>

                      <div class="kh-serial-wrap">
                        <table class="kh-mini">
                          <thead>
                            <tr>
                              <th>Số serial</th>
                              <th>Trạng thái</th>
                              <th>Ngày nhập</th>
                              <th>Ghi chú</th>
                              <th>Đổi trạng thái</th>
                            </tr>
                          </thead>
                          <tbody>
                            <tr v-for="s in serialDaLoc" :key="s.chiTietId">
                              <td class="kh-sku">{{ s.soSerial }}</td>
                              <td><span class="kh-tag" :class="lopSerial(s.trangThai)">{{ nhanSerial(s.trangThai) }}</span></td>
                              <td class="kh-muted">{{ formatNgay(s.ngayNhapKho) }}</td>
                              <td class="kh-muted">{{ s.ghiChu || '—' }}</td>
                              <td>
                                <select class="kh-input-sm" :value="s.trangThai" @change="doiSerial(s, $event.target.value, d)">
                                  <option v-for="tt in TRANG_THAI_SERIAL" :key="tt" :value="tt">{{ nhanSerial(tt) }}</option>
                                </select>
                              </td>
                            </tr>
                            <tr v-if="!serialDaLoc.length">
                              <td colspan="5" class="kh-empty-cell">
                                Biến thể này chưa có serial nào. Dùng nút “Nhập hàng” để thêm máy vào kho.
                              </td>
                            </tr>
                          </tbody>
                        </table>
                      </div>
                    </div>

                    <!-- ── Tab 3: lịch sử ── -->
                    <div v-show="tabChiTiet === 'lichsu'">
                      <div class="kh-serial-wrap">
                        <table class="kh-mini">
                          <thead>
                            <tr>
                              <th>Thời gian</th>
                              <th>Loại</th>
                              <th class="ta-r">Thay đổi</th>
                              <th>Phiếu</th>
                              <th>Nhân viên</th>
                              <th>Ghi chú</th>
                            </tr>
                          </thead>
                          <tbody>
                            <tr v-for="ls in lichSu" :key="ls.id">
                              <td class="kh-muted">{{ formatNgay(ls.ngayTao) }}</td>
                              <td><span class="kh-tag kh-tag--soft">{{ nhanBienDong(ls.loai) }}</span></td>
                              <td class="ta-r" :class="Number(ls.soLuong) < 0 ? 'kh-danger' : 'kh-ok'">
                                {{ Number(ls.soLuong) > 0 ? '+' : '' }}{{ ls.soLuong }}
                              </td>
                              <td class="kh-muted">{{ ls.maPhieuNhap || '—' }}</td>
                              <td class="kh-muted">{{ ls.nhanVien || '—' }}</td>
                              <td class="kh-muted">{{ ls.ghiChu || '—' }}</td>
                            </tr>
                            <tr v-if="!lichSu.length">
                              <td colspan="6" class="kh-empty-cell">Chưa có biến động kho nào được ghi nhận.</td>
                            </tr>
                          </tbody>
                        </table>
                      </div>
                    </div>
                  </div>
                </td>
              </tr>
            </template>

            <tr v-if="!dangTai && !dongDaLoc.length">
              <td colspan="11" class="kh-empty-cell">Không có biến thể nào khớp điều kiện lọc.</td>
            </tr>
          </tbody>
        </table>

        <div v-if="dangTai" class="kh-overlay"><span class="kh-spinner"></span></div>
      </div>
    </section>

    <!-- ══════════════ MODAL NHẬP HÀNG ══════════════ -->
    <teleport to="body">
      <div v-if="moModalNhap" class="kh-mask" @click.self="dongModalNhap">
        <div class="kh-modal">
          <header class="kh-modal__head">
            <div>
              <h2>Nhập hàng vào kho</h2>
              <p>Mỗi dòng là một biến thể; máy có serial thì phải quét đủ số serial bằng số lượng.</p>
            </div>
            <button class="kh-icon-btn" @click="dongModalNhap"><i class="fa fa-times"></i></button>
          </header>

          <div class="kh-modal__body">
            <p v-if="loiNhap" class="kh-alert">{{ loiNhap }}</p>

            <div class="kh-grid">
              <label class="kh-field">
                <span>Nhà cung cấp <b>*</b></span>
                <select v-model="phieu.nhaCungCapId">
                  <option value="">-- Chọn nhà cung cấp --</option>
                  <option v-for="n in dsNhaCungCap" :key="n.nhaCungCapId ?? n.id" :value="n.nhaCungCapId ?? n.id">
                    {{ n.tenNhaCungCap }}
                  </option>
                </select>
              </label>
              <label class="kh-field">
                <span>Nhân viên nhập</span>
                <select v-model="phieu.nhanVienId">
                  <option value="">-- Không chọn --</option>
                  <option v-for="nv in dsNhanVien" :key="nv.nhanVienId" :value="nv.nhanVienId">{{ nv.hoTen }}</option>
                </select>
              </label>
              <label class="kh-field">
                <span>Ngày nhập</span>
                <input type="datetime-local" v-model="phieu.ngayNhap" />
              </label>
              <label class="kh-field kh-field--wide">
                <span>Ghi chú phiếu</span>
                <input v-model.trim="phieu.ghiChu" placeholder="VD: Nhập đợt 3 từ Digiworld" />
              </label>
            </div>

            <!-- Các dòng hàng -->
            <div v-for="(d, i) in phieu.dongNhap" :key="d.key" class="kh-line">
              <div class="kh-line__head">
                <strong>Dòng {{ i + 1 }}</strong>
                <button class="kh-icon-btn" title="Bỏ dòng này" @click="xoaDong(d.key)"><i class="fa fa-times"></i></button>
              </div>

              <div class="kh-grid">
                <label class="kh-field kh-field--wide">
                  <span>Biến thể <b>*</b></span>
                  <select v-model="d.bienTheId" @change="chonBienThe(d)">
                    <option value="">-- Chọn biến thể --</option>
                    <option v-for="bt in dongs" :key="bt.bienTheId" :value="bt.bienTheId">
                      {{ bt.maSku }} — {{ bt.tenSanPham }}{{ bt.mauSac ? ' · ' + bt.mauSac : '' }} (tồn {{ bt.tonThucTe }})
                    </option>
                  </select>
                </label>
                <label class="kh-field">
                  <span>Số lượng <b>*</b></span>
                  <input type="number" min="1" v-model.number="d.soLuong" />
                </label>
                <label class="kh-field">
                  <span>Đơn giá nhập (₫) <b>*</b></span>
                  <input type="number" min="0" step="1000" v-model.number="d.donGiaNhap" />
                </label>
                <label class="kh-field">
                  <span>Thành tiền</span>
                  <input :value="formatSo(d.soLuong * d.donGiaNhap)" disabled />
                </label>
                <label class="kh-field kh-field--wide kh-check">
                  <input type="checkbox" v-model="d.theoSerial" />
                  <span>Quản lý theo số serial (laptop, điện thoại — bỏ tick nếu là phụ kiện nhập theo lô)</span>
                </label>
              </div>

              <div v-if="d.theoSerial" class="kh-serial-box">
                <div class="kh-serial-box__head">
                  <span>
                    Danh sách serial — mỗi dòng một mã
                    <b :class="soSerial(d) === d.soLuong ? 'kh-ok' : 'kh-danger'">
                      {{ soSerial(d) }}/{{ d.soLuong || 0 }}
                    </b>
                  </span>
                  <div class="kh-inline">
                    <button class="kh-btn kh-btn--ghost kh-btn--sm" @click="sinhSerial(d)">
                      <i class="fa fa-magic"></i> Tự sinh cho đủ
                    </button>
                    <button class="kh-btn kh-btn--ghost kh-btn--sm" @click="d.serialText = ''">
                      <i class="fa fa-eraser"></i> Xóa
                    </button>
                  </div>
                </div>
                <textarea v-model="d.serialText" rows="4" placeholder="SN-DELL-001&#10;SN-DELL-002"></textarea>
                <em v-if="loiSerial(d)" class="kh-err">{{ loiSerial(d) }}</em>
              </div>
            </div>

            <button class="kh-btn kh-btn--ghost kh-btn--sm" @click="themDong">
              <i class="fa fa-plus"></i> Thêm dòng hàng
            </button>

            <div class="kh-tong">
              <label class="kh-check">
                <input type="checkbox" v-model="phieu.capNhatGiaNhap" />
                <span>Cập nhật giá vốn của biến thể theo đơn giá nhập lần này</span>
              </label>
              <div class="kh-tong__tien">
                Tổng tiền: <strong>{{ formatSo(tongTienPhieu) }} ₫</strong>
              </div>
            </div>
          </div>

          <footer class="kh-modal__foot">
            <button class="kh-btn kh-btn--ghost" @click="dongModalNhap">Bỏ qua</button>
            <button class="kh-btn kh-btn--primary" :disabled="dangLuu" @click="luuPhieuNhap">
              <i class="fa" :class="dangLuu ? 'fa-spinner fa-spin' : 'fa-check'"></i>
              {{ dangLuu ? 'Đang ghi phiếu…' : 'Nhập kho' }}
            </button>
          </footer>
        </div>
      </div>
    </teleport>

    <!-- ══════════════ MODAL LỊCH SỬ PHIẾU NHẬP ══════════════ -->
    <teleport to="body">
      <div v-if="moModalPhieu" class="kh-mask" @click.self="moModalPhieu = false">
        <div class="kh-modal kh-modal--hep">
          <header class="kh-modal__head">
            <h2>Phiếu nhập gần đây</h2>
            <button class="kh-icon-btn" @click="moModalPhieu = false"><i class="fa fa-times"></i></button>
          </header>
          <div class="kh-modal__body">
            <table class="kh-mini">
              <thead>
                <tr>
                  <th>Mã phiếu</th>
                  <th>Ngày nhập</th>
                  <th>Nhà cung cấp</th>
                  <th>Nhân viên</th>
                  <th class="ta-r">SL</th>
                  <th class="ta-r">Tổng tiền</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in phieuNhap" :key="p.phieuNhapId">
                  <td class="kh-sku">{{ p.maPhieuNhap }}</td>
                  <td class="kh-muted">{{ formatNgay(p.ngayNhap) }}</td>
                  <td>{{ p.tenNhaCungCap || '—' }}</td>
                  <td class="kh-muted">{{ p.nhanVien || '—' }}</td>
                  <td class="ta-r">{{ p.tongSoLuong || 0 }}</td>
                  <td class="ta-r"><strong>{{ formatSo(p.tongTien) }}</strong></td>
                </tr>
                <tr v-if="!phieuNhap.length">
                  <td colspan="6" class="kh-empty-cell">Chưa có phiếu nhập nào.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </teleport>

    <teleport to="body">
      <div v-if="thongBao" class="kh-toast">{{ thongBao }}</div>
    </teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getNhaCungCap, getCpu, getRam, getOCung, getGpu } from '@/services/DmService.js'
import * as khoApi from '@/services/khoService.js'

const ANH_MAC_DINH = 'https://cdn-icons-png.flaticon.com/512/664/664457.png'
const TRANG_THAI_SERIAL = ['trong_kho', 'giu_hang', 'da_ban', 'loi_bao_hanh', 'da_tra_hang']

const NHAN_SERIAL = {
  trong_kho: 'Trong kho', giu_hang: 'Đang giữ', da_ban: 'Đã bán',
  loi_bao_hanh: 'Lỗi/bảo hành', da_tra_hang: 'Đã trả hàng'
}
const NHAN_BIEN_DONG = {
  nhap: 'Nhập kho', xuat_ban: 'Xuất bán', tra_hang: 'Trả hàng',
  dieu_chinh: 'Điều chỉnh', huy: 'Hủy', giu_hang: 'Giữ hàng'
}

/* ─── Tiện ích ─── */
const formatSo = (n) => Number(n || 0).toLocaleString('vi-VN')
const formatNgay = (v) => {
  if (!v) return '—'
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? '—'
    : d.toLocaleDateString('vi-VN') + ' ' + d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}
const khongDau = (s) =>
  String(s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[đĐ]/g, 'd').toLowerCase()
const anhLoi = (e) => { e.target.src = ANH_MAC_DINH }
const nhanSerial = (tt) => NHAN_SERIAL[tt] || tt
const nhanBienDong = (l) => NHAN_BIEN_DONG[l] || l
const doiSo = (v) => (v === '' || v === null || v === undefined ? null : Number(v))

const doiLoi = (e) => {
  const d = e?.response?.data
  return (typeof d === 'string' && d) || d?.message || d?.error || e?.message || 'Không rõ nguyên nhân'
}

/* ─── Trạng thái ─── */
const dangTai = ref(false)
const dangLuu = ref(false)
const loiTai = ref('')
const loiChiTiet = ref('')
const loiNhap = ref('')
const thongBao = ref('')

const dongs = ref([])
const serials = ref([])
const lichSu = ref([])
const phieuNhap = ref([])

const dsNhaCungCap = ref([])
const dsNhanVien = ref([])
const dmCpu = ref([])
const dmRam = ref([])
const dmOCung = ref([])
const dmGpu = ref([])

const tuKhoa = ref('')
const timSerial = ref('')
const moBoLoc = ref(false)
const loc = reactive({ thuongHieu: '', tinhTrang: '', trangThai: '', coSerial: '' })

const dongMo = ref(null)
const tabChiTiet = ref('sua')
const tabsChiTiet = [
  { key: 'sua', label: 'Thông tin & chỉnh sửa' },
  { key: 'serial', label: 'Serial' },
  { key: 'lichsu', label: 'Lịch sử kho' }
]

const bao = (msg) => {
  thongBao.value = msg
  setTimeout(() => (thongBao.value = ''), 3000)
}

const toArray = (res) => (Array.isArray(res) ? res : (res?.content ?? res?.data ?? []))

/* ════════════ TẢI DỮ LIỆU ════════════ */
const taiDuLieu = async () => {
  dangTai.value = true
  loiTai.value = ''
  try {
    dongs.value = toArray(await khoApi.getTonKho())
  } catch (e) {
    console.error('Lỗi tải tồn kho:', e)
    loiTai.value = 'Không tải được dữ liệu kho: ' + doiLoi(e)
  } finally {
    dangTai.value = false
  }
}

const taiDanhMuc = async () => {
  const an = (p) => p.catch(() => [])
  const [ncc, nv, cpu, ram, oc, gpu] = await Promise.all([
    an(getNhaCungCap()), an(khoApi.getNhanVien()),
    an(getCpu()), an(getRam()), an(getOCung()), an(getGpu())
  ])
  dsNhaCungCap.value = toArray(ncc)
  dsNhanVien.value = toArray(nv)
  dmCpu.value = toArray(cpu)
  dmRam.value = toArray(ram)
  dmOCung.value = toArray(oc)
  dmGpu.value = toArray(gpu)
}

onMounted(async () => {
  await Promise.all([taiDuLieu(), taiDanhMuc()])
})

/* ════════════ LỌC & THỐNG KÊ ════════════ */
const cauHinh = (d) => [d.tenCpu, d.tenRam, d.tenOCung, d.tenGpu, d.mauSac].filter(Boolean).join(' · ')

const sapHet = (d) => Number(d.tonThucTe) > 0 && Number(d.tonThucTe) <= Number(d.tonToiThieu)
const hetHang = (d) => Number(d.tonThucTe) === 0

const nhanTinhTrang = (d) => (hetHang(d) ? 'Hết hàng' : sapHet(d) ? 'Sắp hết' : 'Còn hàng')
const lopTinhTrang = (d) => (hetHang(d) ? 'kh-tag--danger' : sapHet(d) ? 'kh-tag--warn' : 'kh-tag--ok')
const lopSerial = (tt) =>
  tt === 'trong_kho' ? 'kh-tag--ok' : tt === 'loi_bao_hanh' ? 'kh-tag--danger' : 'kh-tag--soft'

const dsThuongHieu = computed(() =>
  [...new Set(dongs.value.map((d) => d.tenThuongHieu).filter(Boolean))].sort()
)

const soBoLoc = computed(() => Object.values(loc).filter((v) => v !== '').length)

const dongDaLoc = computed(() =>
  dongs.value.filter((d) => {
    const kw = khongDau(tuKhoa.value.trim())
    if (kw && ![d.maSku, d.tenSanPham, d.maSanPham, d.barcode, d.mauSac].some((f) => khongDau(f).includes(kw)))
      return false
    if (loc.thuongHieu && d.tenThuongHieu !== loc.thuongHieu) return false
    if (loc.trangThai && d.trangThai !== loc.trangThai) return false
    if (loc.tinhTrang === 'het' && !hetHang(d)) return false
    if (loc.tinhTrang === 'sap_het' && !sapHet(d)) return false
    if (loc.tinhTrang === 'con' && Number(d.tonThucTe) <= 0) return false
    if (loc.coSerial === 'co' && Number(d.tongSerial) === 0) return false
    if (loc.coSerial === 'khong' && Number(d.tongSerial) > 0) return false
    return true
  })
)

const thongKe = computed(() => ({
  tongSku: dongs.value.filter((d) => d.trangThai === 'active').length,
  tongTon: dongs.value.reduce((s, d) => s + Number(d.tonThucTe || 0), 0),
  sapHet: dongs.value.filter(sapHet).length,
  hetHang: dongs.value.filter(hetHang).length,
  giaTriTon: dongs.value.reduce((s, d) => s + Number(d.tonThucTe || 0) * Number(d.giaNhap || 0), 0)
}))

const xoaLoc = () => {
  Object.keys(loc).forEach((k) => (loc[k] = ''))
  tuKhoa.value = ''
}

/* ════════════ CHI TIẾT BIẾN THỂ ════════════ */
const formSua = reactive({})

const loiGia = computed(() => {
  const nhap = Number(formSua.giaNhap || 0)
  const ban = Number(formSua.giaBan || 0)
  return ban < nhap * 0.5 ? 'Giá bán phải ≥ 50% giá nhập (ràng buộc của CSDL)' : ''
})

const moDong = async (d) => {
  if (dongMo.value === d.bienTheId) {
    dongMo.value = null
    return
  }
  dongMo.value = d.bienTheId
  tabChiTiet.value = 'sua'
  loiChiTiet.value = ''
  timSerial.value = ''

  Object.assign(formSua, {
    maSku: d.maSku, mauSac: d.mauSac || '', giaNhap: Number(d.giaNhap || 0), giaBan: Number(d.giaBan || 0),
    baoHanhThang: d.baoHanhThang ?? 24, trangThai: d.trangThai || 'active',
    cpuId: d.cpuId ?? '', ramId: d.ramId ?? '', oCungId: d.oCungId ?? '', gpuId: d.gpuId ?? '',
    kichThuocManHinh: d.kichThuocManHinh || '', heDieuHanh: d.heDieuHanh || '', pin: d.pin || '',
    trongLuongKg: d.trongLuongKg ?? '', hinhAnhBienThe: d.hinhAnhBienThe || '',
    phanLoaiTags: d.phanLoaiTags || '', phanLoaiTen: d.phanLoaiTen || '',
    tonToiThieu: d.tonToiThieu ?? 5
  })

  serials.value = []
  lichSu.value = []
  try {
    const [sr, ls] = await Promise.all([khoApi.getSerial(d.bienTheId), khoApi.getLichSu(d.bienTheId)])
    serials.value = toArray(sr)
    lichSu.value = toArray(ls)
  } catch (e) {
    loiChiTiet.value = 'Không tải được serial / lịch sử: ' + doiLoi(e)
  }
}

const serialDaLoc = computed(() => {
  const kw = khongDau(timSerial.value.trim())
  return kw ? serials.value.filter((s) => khongDau(s.soSerial).includes(kw)) : serials.value
})

const demSerial = computed(() => {
  const dem = {}
  serials.value.forEach((s) => { dem[s.trangThai] = (dem[s.trangThai] || 0) + 1 })
  return dem
})

const luuBienThe = async (d) => {
  if (loiGia.value) {
    loiChiTiet.value = loiGia.value
    return
  }
  dangLuu.value = true
  loiChiTiet.value = ''
  try {
    await khoApi.capNhatBienThe(d.bienTheId, {
      maSku: formSua.maSku,
      mauSac: formSua.mauSac || null,
      giaNhap: Number(formSua.giaNhap || 0),
      giaBan: Number(formSua.giaBan || 0),
      baoHanhThang: Number(formSua.baoHanhThang || 0),
      trangThai: formSua.trangThai,
      cpuId: doiSo(formSua.cpuId), ramId: doiSo(formSua.ramId),
      oCungId: doiSo(formSua.oCungId), gpuId: doiSo(formSua.gpuId),
      kichThuocManHinh: formSua.kichThuocManHinh || null,
      heDieuHanh: formSua.heDieuHanh || null,
      pin: formSua.pin || null,
      trongLuongKg: doiSo(formSua.trongLuongKg),
      hinhAnhBienThe: formSua.hinhAnhBienThe || null,
      phanLoaiTags: formSua.phanLoaiTags || null,
      phanLoaiTen: formSua.phanLoaiTen || null
    })

    if (Number(formSua.tonToiThieu) !== Number(d.tonToiThieu)) {
      await khoApi.capNhatTonToiThieu(d.bienTheId, Number(formSua.tonToiThieu))
    }

    bao('Đã lưu thay đổi biến thể')
    await taiDuLieu()
  } catch (e) {
    loiChiTiet.value = 'Lưu thất bại: ' + doiLoi(e)
  } finally {
    dangLuu.value = false
  }
}

const doiSerial = async (s, trangThaiMoi, d) => {
  if (trangThaiMoi === s.trangThai) return
  try {
    await khoApi.doiTrangThaiSerial(s.chiTietId, { trangThai: trangThaiMoi, ghiChu: s.ghiChu || null })
    s.trangThai = trangThaiMoi
    bao(`Serial ${s.soSerial} → ${nhanSerial(trangThaiMoi)}`)
    const [sr, ls] = await Promise.all([khoApi.getSerial(d.bienTheId), khoApi.getLichSu(d.bienTheId)])
    serials.value = toArray(sr)
    lichSu.value = toArray(ls)
    await taiDuLieu()
  } catch (e) {
    loiChiTiet.value = 'Không đổi được trạng thái serial: ' + doiLoi(e)
  }
}

/* ════════════ NHẬP HÀNG ════════════ */
const moModalNhap = ref(false)
const moModalPhieu = ref(false)

const gioHienTai = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
}

const dongRong = () => ({
  key: Math.random().toString(36).slice(2),
  bienTheId: '', soLuong: 1, donGiaNhap: 0, theoSerial: true, serialText: ''
})

const phieu = reactive({
  nhaCungCapId: '', nhanVienId: '', ngayNhap: gioHienTai(), ghiChu: '',
  capNhatGiaNhap: false, dongNhap: [dongRong()]
})

const moNhapHang = () => {
  Object.assign(phieu, {
    nhaCungCapId: '', nhanVienId: '', ngayNhap: gioHienTai(), ghiChu: '',
    capNhatGiaNhap: false, dongNhap: [dongRong()]
  })
  loiNhap.value = ''
  moModalNhap.value = true
}

const dongModalNhap = () => { moModalNhap.value = false }

const themDong = () => phieu.dongNhap.push(dongRong())
const xoaDong = (key) => {
  phieu.dongNhap = phieu.dongNhap.filter((d) => d.key !== key)
  if (!phieu.dongNhap.length) phieu.dongNhap.push(dongRong())
}

/** Chọn biến thể nào thì lấy luôn giá vốn hiện tại làm đơn giá gợi ý. */
const chonBienThe = (d) => {
  const bt = dongs.value.find((x) => String(x.bienTheId) === String(d.bienTheId))
  if (bt && !d.donGiaNhap) d.donGiaNhap = Number(bt.giaNhap || 0)
}

const tachSerial = (text) =>
  String(text || '').split(/[\n,;]+/).map((s) => s.trim()).filter(Boolean)

const soSerial = (d) => tachSerial(d.serialText).length

const loiSerial = (d) => {
  const ds = tachSerial(d.serialText)
  if (ds.length !== Number(d.soLuong || 0))
    return `Cần đúng ${d.soLuong || 0} serial, đang có ${ds.length}`
  if (new Set(ds).size !== ds.length) return 'Có serial bị lặp lại trong danh sách'
  return ''
}

/** Sinh serial theo SKU + ngày để demo nhanh; nhập thật thì quét máy vào ô textarea. */
const sinhSerial = (d) => {
  const bt = dongs.value.find((x) => String(x.bienTheId) === String(d.bienTheId))
  if (!bt) {
    loiNhap.value = 'Chọn biến thể trước khi tự sinh serial'
    return
  }
  const dau = `${bt.maSku}-${new Date().toISOString().slice(2, 10).replace(/-/g, '')}`
  const daCo = tachSerial(d.serialText)
  const them = []
  for (let i = daCo.length; i < Number(d.soLuong || 0); i++) {
    them.push(`${dau}-${String(i + 1).padStart(3, '0')}`)
  }
  d.serialText = [...daCo, ...them].join('\n')
}

const tongTienPhieu = computed(() =>
  phieu.dongNhap.reduce((s, d) => s + Number(d.soLuong || 0) * Number(d.donGiaNhap || 0), 0)
)

const luuPhieuNhap = async () => {
  loiNhap.value = ''

  if (!phieu.nhaCungCapId) {
    loiNhap.value = 'Chưa chọn nhà cung cấp'
    return
  }
  const chuaChon = phieu.dongNhap.find((d) => !d.bienTheId)
  if (chuaChon) {
    loiNhap.value = 'Có dòng chưa chọn biến thể'
    return
  }
  const saiSoLuong = phieu.dongNhap.find((d) => !d.soLuong || d.soLuong < 1)
  if (saiSoLuong) {
    loiNhap.value = 'Số lượng mỗi dòng phải lớn hơn 0'
    return
  }
  const saiSerial = phieu.dongNhap.find((d) => d.theoSerial && loiSerial(d))
  if (saiSerial) {
    loiNhap.value = `Dòng có SKU ${tenBienThe(saiSerial.bienTheId)}: ${loiSerial(saiSerial)}`
    return
  }
  // Serial trùng giữa các dòng khác nhau trong cùng phiếu
  const tatCa = phieu.dongNhap.filter((d) => d.theoSerial).flatMap((d) => tachSerial(d.serialText))
  if (new Set(tatCa).size !== tatCa.length) {
    loiNhap.value = 'Có serial bị lặp giữa các dòng trong phiếu'
    return
  }

  dangLuu.value = true
  try {
    const ketQua = await khoApi.nhapHang({
      nhaCungCapId: Number(phieu.nhaCungCapId),
      nhanVienId: doiSo(phieu.nhanVienId),
      ngayNhap: phieu.ngayNhap ? `${phieu.ngayNhap}:00` : null,
      ghiChu: phieu.ghiChu || null,
      capNhatGiaNhap: phieu.capNhatGiaNhap,
      dongNhap: phieu.dongNhap.map((d) => ({
        bienTheId: Number(d.bienTheId),
        soLuong: Number(d.soLuong),
        donGiaNhap: Number(d.donGiaNhap || 0),
        theoSerial: d.theoSerial,
        serials: d.theoSerial ? tachSerial(d.serialText) : []
      }))
    })

    bao(`Đã nhập kho — phiếu ${ketQua?.maPhieuNhap || ''} (${ketQua?.soSerial || 0} serial)`)
    moModalNhap.value = false
    await taiDuLieu()
  } catch (e) {
    console.error('Lỗi nhập kho:', e?.response?.data ?? e)
    loiNhap.value = 'Nhập kho thất bại: ' + doiLoi(e)
  } finally {
    dangLuu.value = false
  }
}

const tenBienThe = (id) => dongs.value.find((d) => String(d.bienTheId) === String(id))?.maSku || id

const moPhieuNhap = async () => {
  moModalPhieu.value = true
  try {
    phieuNhap.value = toArray(await khoApi.getPhieuNhap())
  } catch (e) {
    console.error('Lỗi tải phiếu nhập:', e)
  }
}

/* ════════════ XUẤT FILE ════════════ */
const xuatCsv = () => {
  const cols = ['Mã SKU', 'Mã sản phẩm', 'Tên sản phẩm', 'Thương hiệu', 'Cấu hình', 'Màu sắc',
    'Giá vốn', 'Giá bán', 'Tồn thực tế', 'Đang giữ', 'Có thể bán', 'Tồn tối thiểu', 'Tổng serial', 'Tình trạng']
  const esc = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const lines = [cols.map(esc).join(',')]

  dongDaLoc.value.forEach((d) => {
    lines.push([d.maSku, d.maSanPham, d.tenSanPham, d.tenThuongHieu, cauHinh(d), d.mauSac,
      d.giaNhap, d.giaBan, d.tonThucTe, d.dangGiu, d.coTheBan, d.tonToiThieu, d.tongSerial,
      nhanTinhTrang(d)].map(esc).join(','))
  })

  const blob = new Blob(['\uFEFF' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `ton-kho-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  bao(`Đã xuất ${dongDaLoc.value.length} dòng tồn kho`)
}
</script>

<style scoped>
.kh, .kh-mask, .kh-toast {
  --pink-50: #fff5f9;
  --pink-100: #ffe6f0;
  --pink-200: #ffcfe1;
  --pink-300: #f7a8c8;
  --pink-500: #ec4899;
  --pink-600: #db2777;
  --pink-700: #a81b5d;
  --ink: #1f2937;
  --muted: #6b7280;
  --line: #f1dbe6;
  --field: #d9b3c6;
  --danger: #dc2626;
  --warn: #b45309;
  --ok: #047857;
}
.kh { font-size: 14px; color: var(--ink); }

.ta-r { text-align: right; }
.kh-muted { color: var(--muted); }
.kh-danger { color: var(--danger); font-weight: 600; }
.kh-ok { color: var(--ok); font-weight: 600; }
.kh-inline { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }

/* nút */
.kh-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 14px; border-radius: 999px; border: 1px solid transparent;
  font-size: 13px; font-weight: 500; font-family: inherit; cursor: pointer; white-space: nowrap;
  transition: background-color .15s, border-color .15s;
}
.kh-btn--sm { padding: 5px 11px; font-size: 12.5px; }
.kh-btn--primary { background: var(--pink-600); color: #fff; }
.kh-btn--primary:hover:not(:disabled) { background: var(--pink-700); }
.kh-btn--ghost { background: #fff; color: var(--pink-700); border-color: var(--pink-200); }
.kh-btn--ghost:hover:not(:disabled) { background: var(--pink-50); border-color: var(--pink-300); }
.kh-btn--ghost.is-on { background: var(--pink-100); border-color: var(--pink-300); }
.kh-btn:disabled { opacity: .5; cursor: not-allowed; }
.kh-icon-btn {
  background: none; border: none; color: var(--muted);
  width: 32px; height: 32px; border-radius: 50%; cursor: pointer;
}
.kh-icon-btn:hover { background: var(--pink-50); color: var(--pink-600); }
.kh-link { background: none; border: none; color: var(--pink-700); text-decoration: underline; cursor: pointer; }
.kh-chip {
  background: var(--pink-600); color: #fff; border-radius: 999px;
  padding: 0 6px; font-size: 11px; line-height: 17px; min-width: 17px; text-align: center;
}

/* thanh công cụ */
.kh-bar {
  display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
  background: #fff; border: 1px solid var(--line); border-radius: 12px;
  padding: 12px 16px; margin-bottom: 12px;
}
.kh-bar__left { display: flex; align-items: center; gap: 14px; }
.kh-bar__actions { display: flex; gap: 8px; margin-left: auto; flex-wrap: wrap; }
.kh-title { margin: 0; font-size: 19px; font-weight: 700; color: var(--pink-700); }
.kh-search { position: relative; width: 300px; max-width: 100%; }
.kh-search input {
  width: 100%; padding: 8px 32px 8px 34px; border: 1px solid var(--pink-200);
  border-radius: 999px; font-size: 13px; background: var(--pink-50); font-family: inherit;
}
.kh-search input:focus { outline: none; border-color: var(--pink-500); background: #fff; }
.kh-search__icon { position: absolute; left: 13px; top: 50%; transform: translateY(-50%); color: var(--pink-500); }
.kh-search__clear { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); background: none; border: none; color: var(--muted); cursor: pointer; }

/* thẻ tổng quan */
.kh-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 10px; margin-bottom: 12px; }
.kh-card {
  background: #fff; border: 1px solid var(--line); border-left: 3px solid var(--pink-300);
  border-radius: 10px; padding: 11px 14px; display: flex; flex-direction: column; gap: 3px;
}
.kh-card--warn { border-left-color: #f59e0b; }
.kh-card--danger { border-left-color: var(--danger); }
.kh-card__label { font-size: 11.5px; color: var(--muted); }
.kh-card__value { font-size: 18px; font-weight: 700; color: var(--pink-700); }
.kh-card--warn .kh-card__value { color: var(--warn); }
.kh-card--danger .kh-card__value { color: var(--danger); }

/* bộ lọc */
.kh-filter { display: grid; grid-template-rows: 0fr; transition: grid-template-rows .25s ease, margin-bottom .25s; margin-bottom: 0; }
.kh-filter.is-open { grid-template-rows: 1fr; margin-bottom: 12px; }
.kh-filter__panel {
  overflow: hidden; background: #fff; border: 1px solid var(--line);
  border-radius: 12px; padding: 0 16px; transition: padding .25s;
}
.kh-filter.is-open .kh-filter__panel { padding: 16px; }
.kh-filter__grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 12px; }
.kh-filter__foot {
  display: flex; justify-content: space-between; align-items: center; gap: 12px;
  margin-top: 14px; padding-top: 12px; border-top: 1px dashed var(--line); font-size: 12.5px;
}

/* ô nhập */
.kh-field { display: flex; flex-direction: column; gap: 5px; min-width: 0; }
.kh-field > span { font-size: 12px; font-weight: 600; color: var(--pink-700); }
.kh-field > span b { color: var(--danger); }
.kh-field input, .kh-field select, .kh-field textarea, .kh-input-sm, .kh-serial-box textarea {
  width: 100%; padding: 8px 11px; border: 1px solid var(--field); border-radius: 8px;
  font-size: 13px; color: var(--ink); background: #fff; font-family: inherit;
}
.kh-field input:focus, .kh-field select:focus, .kh-serial-box textarea:focus, .kh-input-sm:focus {
  outline: none; border-color: var(--pink-500); box-shadow: 0 0 0 3px var(--pink-100);
}
.kh-field input:disabled { background: #f8f6f7; color: var(--muted); }
.kh-input-sm { width: auto; padding: 5px 9px; font-size: 12.5px; }
.kh-err { font-size: 11.5px; color: var(--danger); font-style: normal; }
.kh-check { flex-direction: row; align-items: center; gap: 8px; font-size: 12.5px; }
.kh-check input { width: auto; }
.kh-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 13px; }
.kh-field--wide { grid-column: 1 / -1; }

/* bảng */
.kh-panel { background: #fff; border: 1px solid var(--line); border-radius: 12px; overflow: hidden; }
.kh-table-wrap { position: relative; overflow-x: auto; min-height: 120px; }
.kh-table { width: 100%; border-collapse: collapse; }
.kh-table th {
  background: var(--pink-50); color: var(--pink-700); font-size: 12px; font-weight: 700;
  text-align: left; padding: 11px 12px; white-space: nowrap; border-bottom: 1px solid var(--line);
}
.kh-table td { padding: 9px 12px; border-bottom: 1px solid var(--line); white-space: nowrap; }
.kh-col-caret { width: 28px; }
.kh-row { cursor: pointer; }
.kh-row:hover { background: var(--pink-50); }
.kh-row.is-open { background: var(--pink-100); }
.kh-row.is-open td:first-child { box-shadow: inset 3px 0 0 var(--pink-600); }
.kh-sku { font-family: ui-monospace, "SFMono-Regular", Menlo, monospace; font-size: 12.5px; font-weight: 600; }
.kh-name { display: flex; align-items: center; gap: 10px; }
.kh-name__main { font-weight: 600; }
.kh-name__sub { font-size: 11.5px; color: var(--muted); }
.kh-thumb { width: 32px; height: 32px; object-fit: cover; border-radius: 7px; border: 1px solid var(--line); flex-shrink: 0; }

.kh-tag { display: inline-block; padding: 2px 9px; border-radius: 999px; font-size: 11.5px; font-weight: 600; }
.kh-tag--ok { background: #ecfdf5; color: var(--ok); }
.kh-tag--warn { background: #fffbeb; color: var(--warn); }
.kh-tag--danger { background: #fef2f2; color: var(--danger); }
.kh-tag--soft { background: var(--pink-100); color: var(--pink-700); }

/* chi tiết */
.kh-row-detail td { background: var(--pink-50); padding: 0; white-space: normal; }
.kh-detail { margin: 12px; padding: 14px 16px; background: #fff; border: 1px solid var(--pink-200); border-radius: 12px; }
.kh-tabs { display: flex; gap: 4px; border-bottom: 1px solid var(--line); margin-bottom: 14px; }
.kh-tab {
  background: none; border: none; border-bottom: 2px solid transparent; padding: 8px 13px;
  font-size: 13px; font-weight: 600; font-family: inherit; color: var(--muted); cursor: pointer;
  display: inline-flex; align-items: center; gap: 6px;
}
.kh-tab.is-on { color: var(--pink-700); border-bottom-color: var(--pink-600); }
.kh-detail__foot {
  display: flex; justify-content: space-between; align-items: center; gap: 12px;
  margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--line); font-size: 12px;
}

.kh-serial-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 10px; flex-wrap: wrap; }
.kh-serial-wrap { border: 1px solid var(--line); border-radius: 10px; overflow: auto; max-height: 320px; }
.kh-mini { width: 100%; border-collapse: collapse; }
.kh-mini th {
  position: sticky; top: 0; background: var(--pink-50); color: var(--pink-700);
  font-size: 11.5px; font-weight: 700; text-align: left; padding: 8px 10px; border-bottom: 1px solid var(--line);
}
.kh-mini td { padding: 7px 10px; border-bottom: 1px solid var(--line); font-size: 12.5px; }
.kh-empty-cell { text-align: center; color: var(--muted); padding: 22px; font-size: 13px; }

/* trạng thái tải */
.kh-overlay { position: absolute; inset: 0; background: rgba(255,255,255,.65); display: flex; align-items: center; justify-content: center; }
.kh-spinner {
  width: 26px; height: 26px; border-radius: 50%;
  border: 3px solid var(--pink-200); border-top-color: var(--pink-600); animation: kh-spin .7s linear infinite;
}
@keyframes kh-spin { to { transform: rotate(360deg); } }
.kh-alert { margin: 0 0 12px; padding: 10px 14px; background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; font-size: 13px; border-radius: 8px; }

/* modal */
.kh-mask {
  position: fixed; inset: 0; z-index: 1050; background: rgba(31,41,55,.5);
  display: flex; align-items: center; justify-content: center; padding: 20px; color: var(--ink); font-size: 14px;
}
.kh-modal {
  background: #fff; width: 980px; max-width: 100%; max-height: 94vh; border-radius: 14px;
  display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 22px 55px rgba(168,27,93,.25);
}
.kh-modal--hep { width: 760px; }
.kh-modal__head {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 12px;
  padding: 16px 20px; background: var(--pink-50); border-bottom: 1px solid var(--line);
}
.kh-modal__head h2 { margin: 0; font-size: 17px; font-weight: 700; color: var(--pink-700); }
.kh-modal__head p { margin: 5px 0 0; font-size: 12.5px; color: var(--muted); }
.kh-modal__body { padding: 20px; overflow-y: auto; background: #fffafc; display: flex; flex-direction: column; gap: 14px; }
.kh-modal__foot {
  display: flex; justify-content: flex-end; gap: 10px; padding: 14px 20px;
  border-top: 1px solid var(--line); background: var(--pink-50);
}

/* dòng nhập hàng */
.kh-line { border: 1px solid var(--pink-200); border-radius: 12px; padding: 14px; background: #fff; }
.kh-line__head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; color: var(--pink-700); font-size: 13px; }
.kh-serial-box { margin-top: 12px; padding-top: 12px; border-top: 1px dashed var(--line); }
.kh-serial-box__head { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-bottom: 8px; font-size: 12.5px; flex-wrap: wrap; }
.kh-serial-box textarea { font-family: ui-monospace, "SFMono-Regular", Menlo, monospace; font-size: 12.5px; resize: vertical; }

.kh-tong {
  display: flex; justify-content: space-between; align-items: center; gap: 14px; flex-wrap: wrap;
  padding: 12px 14px; background: var(--pink-100); border-radius: 10px;
}
.kh-tong__tien { font-size: 15px; color: var(--pink-700); }

.kh-toast {
  position: fixed; bottom: 26px; left: 50%; transform: translateX(-50%); z-index: 1100;
  background: var(--pink-700); color: #fff; padding: 10px 20px; border-radius: 999px;
  font-size: 13px; box-shadow: 0 8px 22px rgba(168,27,93,.35);
}

@media (max-width: 768px) {
  .kh-bar__actions { width: 100%; margin-left: 0; }
  .kh-search { width: 100%; }
}
</style>