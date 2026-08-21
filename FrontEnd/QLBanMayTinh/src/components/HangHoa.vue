<template>
  <div class="hh">
    <!-- ══════════ THANH CÔNG CỤ + BỘ LỌC — khóa trên đầu, tự ẩn khi cuộn xuống ══════════ -->
    <div ref="stickyHeadEl" class="hh-sticky-head" :class="{ 'is-hidden': hhBarHidden }">
      <!-- ══════════════ THANH CÔNG CỤ ══════════════ -->
      <header class="hh-bar">
        <div class="hh-bar__left">
         

          <div class="hh-search">
            <i class="fa fa-search hh-search__icon"></i>
            <input v-model="searchKeyword" type="text" placeholder="Tìm theo mã sản phẩm, tên, SKU, mã vạch" />
            <button v-if="searchKeyword" class="hh-search__clear" title="Xóa tìm kiếm" @click="searchKeyword = ''">
              <i class="fa fa-times"></i>
            </button>
          </div>
        </div>

        <div class="hh-bar__actions">
          <button class="hh-btn hh-btn--ghost" :class="{ 'is-on': isFilterOpen }" @click="isFilterOpen = !isFilterOpen">
            <i class="fa fa-filter"></i>
            <span>Bộ lọc</span>
            <span v-if="soBoLocDangDung" class="hh-chip">{{ soBoLocDangDung }}</span>
            <i class="fa fa-chevron-down hh-caret" :class="{ 'is-open': isFilterOpen }"></i>
          </button>

          <button class="hh-btn hh-btn--primary" @click="openCreate">
            <i class="fa fa-plus"></i> Tạo mới
          </button>

          <button class="hh-btn hh-btn--ghost" :disabled="!bienTheDaLoc.length" @click="openExportModal">
            <i class="fa fa-download"></i> Xuất file
          </button>

          <button class="hh-icon-btn" title="Tải lại dữ liệu" @click="fetchData">
            <i class="fa fa-refresh" :class="{ 'fa-spin': isLoading }"></i>
          </button>
        </div>
      </header>

      <!-- ══════════════ BỘ LỌC ══════════════ -->
      <section class="hh-filter" :class="{ 'is-open': isFilterOpen }">
        <div class="hh-filter__panel">
          <div class="hh-filter__grid">
            <label class="hh-field">
              <span>Trạng thái</span>
              <select v-model="filters.trangThai">
                <option value="">Tất cả</option>
                <option v-for="t in TRANG_THAI_SAN_PHAM" :key="t.value" :value="t.value">{{ t.label }}</option>
              </select>
            </label>

            <label class="hh-field">
              <span>Thương hiệu</span>
              <select v-model="filters.thuongHieuId">
                <option value="">Tất cả</option>
                <option v-for="th in danhSachThuongHieu" :key="idOf(th, 'thuongHieuId')" :value="idOf(th, 'thuongHieuId')">
                  {{ th.tenThuongHieu }}
                </option>
              </select>
            </label>

            <label class="hh-field">
              <span>Nhà cung cấp</span>
              <select v-model="filters.nhaCungCapId">
                <option value="">Tất cả</option>
                <option v-for="ncc in danhSachNhaCungCap" :key="idOf(ncc, 'nhaCungCapId')" :value="idOf(ncc, 'nhaCungCapId')">
                  {{ ncc.tenNhaCungCap }}
                </option>
              </select>
            </label>

            <label class="hh-field">
              <span>Phân loại</span>
              <select v-model="filters.phanLoai">
                <option value="">Tất cả</option>
                <option v-for="pl in phanLoaiOptions" :key="pl.maPhanLoai" :value="pl.maPhanLoai">{{ pl.tenPhanLoai }}</option>
              </select>
            </label>

            <label class="hh-field">
              <span>CPU</span>
              <select v-model="filters.cpuId">
                <option value="">Tất cả</option>
                <option v-for="cpu in danhSachCpu" :key="idOf(cpu, 'cpuId')" :value="idOf(cpu, 'cpuId')">{{ cpu.tenCpu }}</option>
              </select>
            </label>

            <label class="hh-field">
              <span>RAM</span>
              <select v-model="filters.ramId">
                <option value="">Tất cả</option>
                <option v-for="ram in danhSachRam" :key="idOf(ram, 'ramId')" :value="idOf(ram, 'ramId')">
                  {{ ram.dungLuong || ram.tenRam }}
                </option>
              </select>
            </label>

            <label class="hh-field">
              <span>Màu sắc</span>
              <select v-model="filters.mauSac">
                <option value="">Tất cả</option>
                <option v-for="mau in danhSachMauSac" :key="mau" :value="mau">{{ mau }}</option>
              </select>
            </label>

            <label class="hh-field">
              <span>Giá bán từ</span>
              <input v-model="filters.giaTu" type="number" min="0" step="100000" placeholder="0" />
            </label>

            <label class="hh-field">
              <span>Giá bán đến</span>
              <input v-model="filters.giaDen" type="number" min="0" step="100000" placeholder="Không giới hạn" />
            </label>
          </div>

          <div class="hh-filter__foot">
            <span class="hh-filter__count">{{ groupsDaLoc.length }} sản phẩm · {{ bienTheDaLoc.length }} phiên bản</span>
            <div class="hh-filter__btns">
              <button class="hh-btn hh-btn--ghost hh-btn--sm" @click="resetFilters"><i class="fa fa-eraser"></i> Xóa lọc</button>
              <button class="hh-btn hh-btn--primary hh-btn--sm" @click="isFilterOpen = false">Xong</button>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- ══════════════ BẢNG DỮ LIỆU — bấm vào dòng để xem chi tiết ══════════════ -->
    <section class="hh-card">
      <p v-if="loadError" class="hh-alert">
        {{ loadError }}
        <button class="hh-link" @click="fetchData">Thử lại</button>
      </p>

      <div class="hh-table-wrap">
        <table class="hh-table">
          <thead>
            <tr>
              <th class="hh-col-ma">Mã sản phẩm</th>
              <th class="hh-col-ten">Tên sản phẩm</th>
              <th class="ta-r">Giá bán</th>
              <th class="ta-r">Giá vốn</th>
              <th>Trạng thái</th>
              <th>Ngày tạo</th>
              <th>Ngày cập nhật</th>
              <th class="hh-col-go"></th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="group in pagedGroups" :key="group.sanPhamId"
              class="hh-row" tabindex="0"
              :title="'Xem chi tiết ' + group.tenSanPham"
              @click="moChiTiet(group)" @keydown.enter.prevent="moChiTiet(group)"
            >
              <td class="hh-td-ma">
                <span class="hh-code__main">{{ group.maSanPham }}</span>
              </td>
              <td class="hh-td-ten">
                <div class="hh-name">
                  <img :src="group.hinhAnh" class="hh-thumb" alt="" @error="onImgError" />
                  <div class="hh-name__text">
                    <div class="hh-name__main">{{ group.tenSanPham }}</div>
                    <div class="hh-name__sub">
                      {{ group.tenThuongHieu || '—' }}<template v-if="group.variants.length"> · {{ group.variants.length }} phiên bản</template>
                    </div>
                  </div>
                </div>
              </td>
              <td class="ta-r hh-td-gia">{{ group.khoangGia }}</td>
              <td class="ta-r hh-td-gia hh-muted">{{ group.khoangGiaVon }}</td>
              <td><span class="hh-tag" :class="tagClass(group.trangThai)">{{ nhanTrangThai(group.trangThai) }}</span></td>
              <td class="hh-muted hh-td-ngay">{{ formatDate(group.ngayTao) }}</td>
              <td class="hh-muted hh-td-ngay">{{ formatDate(group.ngayCapNhat) }}</td>
              <td class="hh-col-go"><i class="fa fa-angle-right"></i></td>
            </tr>
          </tbody>
        </table>

        <div v-if="isLoading" class="hh-overlay"><span class="hh-spinner"></span></div>
      </div>

      <div v-if="!isLoading && !pagedGroups.length" class="hh-empty">
        <i class="fa fa-inbox"></i>
        <p v-if="coBoLoc">Không có sản phẩm nào khớp với bộ lọc hiện tại.</p>
        <p v-else>Chưa có sản phẩm nào. Bấm “Tạo mới” để thêm sản phẩm đầu tiên.</p>
        <button v-if="coBoLoc" class="hh-btn hh-btn--ghost hh-btn--sm" @click="resetFilters">Xóa lọc</button>
        <button v-else class="hh-btn hh-btn--primary hh-btn--sm" @click="openCreate">Tạo mới</button>
      </div>

      <footer v-if="groupsDaLoc.length" class="hh-pager">
        <span class="hh-pager__info">
          {{ (page - 1) * pageSize + 1 }}–{{ Math.min(page * pageSize, groupsDaLoc.length) }} trên {{ groupsDaLoc.length }} sản phẩm
        </span>
        <div class="hh-pager__nav">
          <select v-model.number="pageSize" class="hh-pager__size">
            <option :value="10">10 / trang</option>
            <option :value="20">20 / trang</option>
            <option :value="50">50 / trang</option>
          </select>
          <button class="hh-icon-btn" :disabled="page === 1" @click="page--"><i class="fa fa-chevron-left"></i></button>
          <span class="hh-pager__page">{{ page }} / {{ totalPages || 1 }}</span>
          <button class="hh-icon-btn" :disabled="page >= totalPages" @click="page++"><i class="fa fa-chevron-right"></i></button>
        </div>
      </footer>
    </section>

    <!-- ══════════════ MODAL CHI TIẾT SẢN PHẨM ══════════════ -->
    <teleport to="body">
      <div v-if="showDetail && chiTiet" class="hh-modal-mask" @click.self="dongChiTiet">
        <div class="hh-modal hh-modal--rong" role="dialog" aria-modal="true">
          <header class="hh-modal__head">
            <div class="hh-head-main">
              <h2>{{ chiTiet.tenSanPham }}</h2>
              <p>
                <span class="hh-tag hh-tag--soft">{{ chiTiet.maSanPham }}</span>
                <span class="hh-head-path">
                  Nhóm hàng: {{ chiTiet.tenDanhMuc || 'Chưa phân nhóm' }} » {{ chiTiet.tenThuongHieu || 'Chưa có thương hiệu' }}
                </span>
              </p>
            </div>
            <button class="hh-icon-btn" aria-label="Đóng" @click="dongChiTiet"><i class="fa fa-times"></i></button>
          </header>

          <nav class="hh-tabs">
            <button class="hh-tab" :class="{ 'is-on': tabCT === 'info' }" @click="tabCT = 'info'">Thông tin</button>
            <button class="hh-tab" :class="{ 'is-on': tabCT === 'bienthe' }" @click="tabCT = 'bienthe'">
              Biến thể <span class="hh-chip">{{ chiTiet.variants.length }}</span>
            </button>
            <button class="hh-tab" :class="{ 'is-on': tabCT === 'lichsu' }" @click="tabCT = 'lichsu'">
              Lịch sử thay đổi <span v-if="lichSuHienTai.length" class="hh-chip">{{ lichSuHienTai.length }}</span>
            </button>
          </nav>

          <div class="hh-modal__body">
            <!-- ─────────── CHI TIẾT · THÔNG TIN ─────────── -->
            <div v-show="tabCT === 'info'" class="hh-pane">
              <div class="hh-ct-top">
                <div class="hh-ct-media">
                  <img :src="anhDangXem" class="hh-ct-media__main" alt="" @error="onImgError" />
                  <div v-if="anhSanPham.length > 1" class="hh-ct-media__strip">
                    <button
                      v-for="(a, i) in anhSanPham" :key="i" type="button"
                      class="hh-ct-media__thumb" :class="{ 'is-on': a === anhDangXem }" @click="anhDangXem = a"
                    >
                      <img :src="a" alt="" @error="onImgError" />
                    </button>
                  </div>
                </div>

                <div class="hh-ct-main">
                  <div class="hh-ct-tags">
                    <span class="hh-tag" :class="tagClass(chiTiet.trangThai)">{{ nhanTrangThai(chiTiet.trangThai) }}</span>
                    <span class="hh-tag hh-tag--soft">{{ nhanLoaiSanPham(chiTiet.loaiSanPham) }}</span>
                    <span v-for="ma in chiTiet.phanLoai" :key="ma" class="hh-tag hh-tag--outline">{{ tenTheoMaPhanLoai(ma) }}</span>
                  </div>

                  <dl class="hh-ct-grid">
                    <div class="hh-ct-item"><dt>Mã sản phẩm</dt><dd>{{ chiTiet.maSanPham }}</dd></div>
                    <div class="hh-ct-item"><dt>Số phiên bản</dt><dd>{{ chiTiet.variants.length }}</dd></div>
                    <div class="hh-ct-item"><dt>Tồn kho</dt><dd>{{ chiTiet.tonKho }}</dd></div>
                    <div class="hh-ct-item"><dt>Khách đặt</dt><dd>{{ chiTiet.khachDat }}</dd></div>
                    <div class="hh-ct-item"><dt>Giá vốn</dt><dd>{{ chiTiet.khoangGiaVon }} ₫</dd></div>
                    <div class="hh-ct-item"><dt>Giá bán</dt><dd class="hh-ct-item__manh">{{ chiTiet.khoangGia }} ₫</dd></div>
                    <div class="hh-ct-item"><dt>Thương hiệu</dt><dd>{{ chiTiet.tenThuongHieu || 'Chưa có' }}</dd></div>
                    <div class="hh-ct-item"><dt>Nhà cung cấp</dt><dd>{{ chiTiet.tenNhaCungCap || 'Chưa có' }}</dd></div>
                    <div class="hh-ct-item"><dt>Danh mục</dt><dd>{{ chiTiet.tenDanhMuc || 'Chưa có' }}</dd></div>
                    <div class="hh-ct-item"><dt>Bảo hành</dt><dd>{{ chiTiet.baoHanh }}</dd></div>
                    <div class="hh-ct-item"><dt>Ngày tạo</dt><dd>{{ formatDate(chiTiet.ngayTao) }}</dd></div>
                    <div class="hh-ct-item"><dt>Ngày cập nhật</dt><dd>{{ formatDate(chiTiet.ngayCapNhat) }}</dd></div>
                  </dl>
                </div>
              </div>

              <section class="hh-ct-block">
                <h3>Thông số chung</h3>
                <dl class="hh-ct-grid">
                  <div class="hh-ct-item"><dt>Màn hình</dt><dd>{{ chiTiet.thongSo.kichThuocManHinh || 'Chưa có' }}</dd></div>
                  <div class="hh-ct-item"><dt>Hệ điều hành</dt><dd>{{ chiTiet.thongSo.heDieuHanh || 'Chưa có' }}</dd></div>
                  <div class="hh-ct-item"><dt>Pin</dt><dd>{{ chiTiet.thongSo.pin || 'Chưa có' }}</dd></div>
                  <div class="hh-ct-item"><dt>Trọng lượng</dt><dd>{{ chiTiet.thongSo.trongLuongKg ? chiTiet.thongSo.trongLuongKg + ' kg' : 'Chưa có' }}</dd></div>
                </dl>
              </section>

              <section class="hh-ct-block">
                <h3>Mô tả</h3>
                <div v-if="chiTiet.moTa" class="hh-ct-mota" v-html="chiTiet.moTa"></div>
                <p v-else class="hh-muted">Chưa có mô tả. Bấm “Chỉnh sửa” để bổ sung.</p>
              </section>
            </div>

            <!-- ─────────── CHI TIẾT · BIẾN THỂ ─────────── -->
            <div v-show="tabCT === 'bienthe'" class="hh-pane">
              <p class="hh-note hh-note--plain">
                <i class="fa fa-hand-o-up"></i>
                Bấm vào một dòng để chọn phiên bản, các nút thao tác nằm ở cuối cửa sổ.
              </p>

              <div class="hh-vt-wrap">
                <table class="hh-vt">
                  <thead>
                    <tr>
                      <th>Mã SKU</th>
                      <th>Mã vạch</th>
                      <th>Cấu hình</th>
                      <th class="ta-r">Giá vốn</th>
                      <th class="ta-r">Giá bán</th>
                      <th class="ta-c">Tồn</th>
                      <th>Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="v in chiTiet.variants" :key="v.bienTheId"
                      class="hh-vt__row" :class="{ 'is-on': String(v.bienTheId) === String(bienTheChonId) }"
                      @click="bienTheChonId = v.bienTheId"
                    >
                      <td class="hh-vt__sku">{{ v.maSku }}</td>
                      <td class="hh-vt__barcode">
                        <template v-if="v.barcode"><i class="fa fa-barcode"></i> {{ v.barcode }}</template>
                        <span v-else class="hh-muted">Chưa có</span>
                      </td>
                      <td class="hh-vt__cfg">{{ moTaBienThe(v) || 'Phiên bản tiêu chuẩn' }}</td>
                      <td class="ta-r hh-muted">{{ formatNumber(v.giaVon) }}</td>
                      <td class="ta-r hh-vt__gia">{{ formatNumber(v.giaBan) }}</td>
                      <td class="ta-c"><span class="hh-ton" :class="{ 'is-het': v.tonKho === 0 }">{{ v.tonKho }}</span></td>
                      <td><span class="hh-tag" :class="tagClass(v.trangThai)">{{ nhanTrangThai(v.trangThai) }}</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- ─────────── CHI TIẾT · LỊCH SỬ THAY ĐỔI ─────────── -->
            <div v-show="tabCT === 'lichsu'" class="hh-pane">
              <div v-if="nhatKyLoading" class="hh-empty">
                <i class="fa fa-spinner fa-spin"></i>
                <p>Đang tải nhật ký…</p>
              </div>
              <ol v-else-if="lichSuHienTai.length" class="hh-ls">
                <li v-for="(m, i) in lichSuHienTai" :key="i" class="hh-ls__item">
                  <span class="hh-ls__dot" :class="'is-' + m.loai"></span>
                  <div class="hh-ls__body">
                    <div class="hh-ls__head">
                      <strong>{{ m.hanhDong }}</strong>
                      <span class="hh-muted">{{ formatDate(m.thoiGian) }}</span>
                    </div>
                    <div v-if="m.doiTuong" class="hh-ls__target">{{ m.doiTuong }}</div>
                    <ul v-if="m.thayDoi && m.thayDoi.length" class="hh-ls__changes">
                      <li v-for="(t, j) in m.thayDoi" :key="j">
                        <span class="hh-ls__field">{{ t.truong }}</span>
                        <em>{{ t.cu || '—' }}</em>
                        <i class="fa fa-long-arrow-right"></i>
                        <b>{{ t.moi || '—' }}</b>
                      </li>
                    </ul>
                    <div class="hh-ls__by">Người thực hiện: {{ m.nguoiDung }}</div>
                  </div>
                </li>
              </ol>

              <div v-else class="hh-empty">
                <i class="fa fa-history"></i>
                <p>Chưa ghi nhận thay đổi nào cho sản phẩm này.</p>
              </div>
            </div>
          </div>

          <footer class="hh-modal__foot">
            <div class="hh-modal__foot-left">
              <button class="hh-btn hh-btn--ghost" @click="dongChiTiet">Đóng</button>
              <button v-if="tabCT === 'bienthe'" class="hh-btn hh-btn--ghost" @click="themPhienBan(chiTiet)">
                <i class="fa fa-plus"></i> Thêm phiên bản
              </button>
            </div>

            <div class="hh-modal__foot-right">
              <template v-if="tabCT === 'info'">
                <button class="hh-btn hh-btn--soft" @click="saoChepSanPham(chiTiet)"><i class="fa fa-clone"></i> Sao chép</button>
                <button class="hh-btn hh-btn--primary" @click="suaSanPham(chiTiet)"><i class="fa fa-pencil"></i> Chỉnh sửa</button>
              </template>

              <template v-else-if="tabCT === 'bienthe'">
                <span v-if="bienTheDangChon" class="hh-foot-hint">Đang chọn: <b>{{ bienTheDangChon.maSku }}</b></span>
                <span v-else class="hh-foot-hint">Chọn một phiên bản để thao tác</span>
                <button class="hh-btn hh-btn--ghost" :disabled="!bienTheDangChon" @click="inTemMa(bienTheDangChon)">
                  <i class="fa fa-barcode"></i> In tem mã
                </button>
                <button class="hh-btn hh-btn--soft" :disabled="!bienTheDangChon || dangSaoChepBienThe" @click="saoChepBienThe(bienTheDangChon)">
                  <i class="fa fa-clone"></i> {{ dangSaoChepBienThe ? 'Đang sao chép…' : 'Sao chép' }}
                </button>
                <button class="hh-btn hh-btn--primary" :disabled="!bienTheDangChon" @click="suaBienThe(bienTheDangChon)">
                  <i class="fa fa-pencil"></i> Chỉnh sửa
                </button>
              </template>

              <template v-else>
                <span class="hh-foot-hint">Nhật ký ghi lại mỗi lần lưu thành công trên máy này.</span>
                <button class="hh-btn hh-btn--ghost" :disabled="!lichSuHienTai.length" @click="xoaLichSu">
                  <i class="fa fa-eraser"></i> Xóa nhật ký
                </button>
              </template>
            </div>
          </footer>
        </div>
      </div>
    </teleport>

    <!-- ══════════════ MODAL THÊM / SỬA ══════════════ -->
    <teleport to="body">
      <div v-if="showModal" class="hh-modal-mask" @click.self="closeModal">
        <div class="hh-modal" role="dialog" aria-modal="true">
          <header class="hh-modal__head">
            <div class="hh-head-main">
              <h2>{{ tieuDeModal }}</h2>
              <p>
                <span class="hh-tag hh-tag--soft">{{ form.maSanPham || 'Chưa có mã' }}</span>
                <span v-if="form.tenSanPham" class="hh-head-path">{{ form.tenSanPham }}</span>
              </p>
            </div>
            <button class="hh-icon-btn" aria-label="Đóng" @click="closeModal"><i class="fa fa-times"></i></button>
          </header>

          <nav class="hh-tabs">
            <button v-for="t in tabs" :key="t.key" class="hh-tab" :class="{ 'is-on': tab === t.key }" @click="tab = t.key">
              {{ t.label }}
              <span v-if="t.key === 'bienthe' && soPhienBan" class="hh-chip">{{ soPhienBan }}</span>
            </button>
          </nav>

          <div class="hh-modal__body">
            <p v-if="saveError" class="hh-alert">{{ saveError }}</p>

            <!-- ─────────── TAB 1: THÔNG TIN ─────────── -->
            <div v-show="tab === 'info'" class="hh-pane">
              <fieldset class="hh-block" :disabled="modalMode === 'variant'">
                <legend>Thông tin sản phẩm chính</legend>

                <div class="hh-grid">
                  <label class="hh-field">
                    <span>Mã sản phẩm</span>
                    <input v-model.trim="form.maSanPham" disabled />
                    <em class="hh-hint">Hệ thống tự sinh, không sửa tay. Mã vạch nằm ở từng phiên bản.</em>
                  </label>

                  <label class="hh-field hh-field--wide">
                    <span>Tên sản phẩm <b>*</b></span>
                    <input v-model.trim="form.tenSanPham" placeholder="VD: Dell Inspiron 15 3520" />
                    <em v-if="errors.tenSanPham" class="hh-err">{{ errors.tenSanPham }}</em>
                  </label>

                  <label class="hh-field">
                    <span>Thương hiệu <b>*</b></span>
                    <select v-model="form.thuongHieuId">
                      <option value="">-- Chọn thương hiệu --</option>
                      <option v-for="th in danhSachThuongHieu" :key="idOf(th, 'thuongHieuId')" :value="idOf(th, 'thuongHieuId')">
                        {{ th.tenThuongHieu }}
                      </option>
                    </select>
                    <em v-if="errors.thuongHieuId" class="hh-err">{{ errors.thuongHieuId }}</em>
                  </label>

                  <label class="hh-field">
                    <span>Danh mục <b>*</b></span>
                    <select v-model="form.danhMucId">
                      <option value="">-- Chọn danh mục --</option>
                      <option v-for="dm in danhSachDanhMuc" :key="idOf(dm, 'danhMucId')" :value="idOf(dm, 'danhMucId')">
                        {{ dm.tenDanhMuc }}
                      </option>
                    </select>
                    <em v-if="errors.danhMucId" class="hh-err">{{ errors.danhMucId }}</em>
                  </label>

                  <label class="hh-field">
                    <span>Nhà cung cấp</span>
                    <select v-model="form.nhaCungCapId">
                      <option value="">-- Không chọn --</option>
                      <option v-for="ncc in danhSachNhaCungCap" :key="idOf(ncc, 'nhaCungCapId')" :value="idOf(ncc, 'nhaCungCapId')">
                        {{ ncc.tenNhaCungCap }}
                      </option>
                    </select>
                  </label>

                  <label class="hh-field">
                    <span>Loại sản phẩm <b>*</b></span>
                    <select v-model="form.loaiSanPham">
                      <option v-for="l in LOAI_SAN_PHAM" :key="l.value" :value="l.value">{{ l.label }}</option>
                    </select>
                  </label>

                  <label class="hh-field">
                    <span>Trạng thái</span>
                    <select v-model="form.trangThaiSanPham">
                      <option v-for="t in TRANG_THAI_SAN_PHAM" :key="t.value" :value="t.value">{{ t.label }}</option>
                    </select>
                    <em class="hh-hint">Áp dụng cho cả sản phẩm và các phiên bản của nó.</em>
                  </label>

                  <div class="hh-field hh-field--wide">
                    <span>Phân loại sử dụng</span>
                    <div class="hh-chip-select">
                      <button
                        v-for="pl in phanLoaiOptions" :key="pl.phanLoaiId" type="button"
                        class="hh-chip-toggle" :class="{ 'is-on': form.phanLoaiIds.includes(pl.phanLoaiId) }"
                        @click="togglePhanLoai(pl.phanLoaiId)"
                      >
                        {{ tenPhanLoai(pl.phanLoaiId) }}
                      </button>
                    </div>
                  </div>

                  <div class="hh-field hh-field--wide">
                    <span>Ảnh sản phẩm</span>
                    <div class="hh-gallery">
                      <div v-for="(url, i) in form.hinhAnhList" :key="i" class="hh-gallery__item">
                        <img :src="url" alt="" @error="onImgError" />
                        <span v-if="i === 0" class="hh-gallery__badge">Ảnh chính</span>
                        <div class="hh-gallery__actions">
                          <button v-if="i !== 0" type="button" class="hh-icon-btn hh-icon-btn--sm" title="Đặt làm ảnh chính" @click="datLamAnhChinh(i)">
                            <i class="fa fa-star"></i>
                          </button>
                          <button type="button" class="hh-icon-btn hh-icon-btn--sm" title="Xóa ảnh" @click="xoaAnhTaiViTri(i)">
                            <i class="fa fa-trash"></i>
                          </button>
                        </div>
                      </div>
                      <label class="hh-gallery__add">
                        <input type="file" accept="image/*" multiple class="hh-hidden" @change="chonAnhSanPham" />
                        <i class="fa" :class="dangTaiAnh ? 'fa-spinner fa-spin' : 'fa-plus'"></i>
                        <span>{{ dangTaiAnh ? 'Đang tải…' : 'Thêm ảnh' }}</span>
                      </label>
                    </div>
                    <input class="hh-mt6" placeholder="Hoặc dán đường dẫn ảnh rồi nhấn Enter" @keydown.enter.prevent="themAnhTuUrl" />
                    <em class="hh-hint">{{ ghiChuAnh }}</em>
                  </div>
                </div>

                <p class="hh-note">
                  <i class="fa fa-clock-o"></i>
                  Ngày tạo và ngày cập nhật do hệ thống tự ghi tại thời điểm bấm Lưu — hiện là {{ dongHo }}.
                </p>
              </fieldset>
            </div>

            <!-- ─────────── TAB 2: PHIÊN BẢN ─────────── -->
            <div v-show="tab === 'bienthe'" class="hh-pane">
              <!-- Sửa một phiên bản -->
              <fieldset v-if="modalMode === 'edit'" class="hh-block">
                <legend>Phiên bản đang sửa</legend>
                <div class="hh-grid">
                  <label class="hh-field">
                    <span>Mã SKU <b>*</b></span>
                    <input v-model.trim="form.maSku" placeholder="VD: DELL-3520-I5-8G" />
                    <em v-if="errors.maSku" class="hh-err">{{ errors.maSku }}</em>
                  </label>
                  <label class="hh-field">
                    <span>Mã vạch</span>
                    <div class="hh-inline">
                      <input v-model.trim="form.barcode" placeholder="8–13 chữ số" />
                      <button type="button" class="hh-btn hh-btn--ghost hh-btn--sm" title="Sinh mã vạch EAN-13" @click="form.barcode = sinhBarcode(barcodeDaDung)">
                        <i class="fa fa-refresh"></i>
                      </button>
                    </div>
                    <em v-if="errors.barcode" class="hh-err">{{ errors.barcode }}</em>
                  </label>
                  <label class="hh-field">
                    <span>Màu sắc</span>
                    <SearchSelect v-model="form.mauSac" :options="optMauSacSelect" placeholder="VD: Đen" />
                  </label>
                  <label class="hh-field">
                    <span>CPU</span>
                    <SearchSelect v-model="form.cpuId" :options="cpuOptionsSel" placeholder="-- Không chọn --" />
                  </label>
                  <label class="hh-field">
                    <span>RAM</span>
                    <SearchSelect v-model="form.ramId" :options="ramOptionsSel" placeholder="-- Không chọn --" />
                  </label>
                  <label class="hh-field">
                    <span>Ổ cứng</span>
                    <SearchSelect v-model="form.oCungId" :options="oCungOptionsSel" placeholder="-- Không chọn --" />
                  </label>
                  <label class="hh-field">
                    <span>GPU</span>
                    <SearchSelect v-model="form.gpuId" :options="gpuOptionsSel" placeholder="-- Không chọn --" />
                  </label>
                  <label class="hh-field">
                    <span>Giá nhập (₫) <b>*</b></span>
                    <input v-model="form.giaNhap" type="number" min="0" step="1000" />
                    <em v-if="errors.giaNhap" class="hh-err">{{ errors.giaNhap }}</em>
                  </label>
                  <label class="hh-field">
                    <span>Giá bán (₫) <b>*</b></span>
                    <input v-model="form.giaBan" type="number" min="0" step="1000" />
                    <em v-if="errors.giaBan" class="hh-err">{{ errors.giaBan }}</em>
                  </label>
                </div>
              </fieldset>

              <!-- Sinh nhiều phiên bản -->
              <template v-else>
                <fieldset class="hh-block">
                  <legend>Bước 1 · Thuộc tính để ghép ra phiên bản</legend>
                  <p class="hh-note hh-note--plain">
                    Gõ hoặc chọn gợi ý rồi <b>nhấn Enter</b> để thêm thành thẻ. Mỗi thuộc tính thêm được nhiều giá trị,
                    hệ thống tự ghép thành danh sách phiên bản bên dưới. Không thêm gì thì sản phẩm chỉ có một phiên bản.
                  </p>

                  <div class="hh-grid">
                    <div class="hh-field">
                      <span>Màu sắc</span>
                      <TagComboInput
                        v-model="chon.mauSac" :options="optMauSacSelect"
                        placeholder="Gõ màu rồi nhấn Enter"
                        @enter="themMau()" @pick="(v) => { chon.mauSac = v; themMau(true) }"
                      />
                      <div v-if="form.mauSacList.length" class="hh-tags">
                        <span v-for="m in form.mauSacList" :key="m" class="hh-tag-pill">
                          {{ m }}
                          <button type="button" aria-label="Bỏ màu" @click="xoaMau(m)">&times;</button>
                        </span>
                      </div>
                    </div>

                    <div v-for="attr in thuocTinhTron" :key="attr.field" class="hh-field">
                      <span>{{ attr.label }}</span>
                      <TagComboInput
                        v-model="chon[attr.field]" :options="attr.options().map((o) => ({ value: o.ten, label: o.ten }))"
                        :allow-custom="false"
                        :placeholder="'Gõ ' + attr.label.toLowerCase() + ' rồi nhấn Enter'"
                        @enter="themThuocTinh(attr.field)" @pick="(v) => { chon[attr.field] = v; themThuocTinh(attr.field, true) }"
                      />
                      <div v-if="form[attr.field].length" class="hh-tags">
                        <span v-for="id in form[attr.field]" :key="id" class="hh-tag-pill">
                          {{ attr.ten(id) }}
                          <button type="button" aria-label="Bỏ giá trị" @click="xoaThuocTinh(attr.field, id)">&times;</button>
                        </span>
                      </div>
                    </div>
                  </div>
                </fieldset>

                <fieldset class="hh-block">
                  <legend>Bước 2 · Thông số chung cho mọi phiên bản</legend>
                  <div class="hh-grid">
                    <label class="hh-field">
                      <span>Màn hình</span>
                      <SearchSelect v-model="form.kichThuocManHinh" :options="optManHinhSelect" placeholder="-- Không chọn --" />
                    </label>
                    <label class="hh-field">
                      <span>Pin</span>
                      <SearchSelect v-model="form.pin" :options="optPinSelect" placeholder="-- Không chọn --" />
                    </label>
                    <label class="hh-field">
                      <span>Hệ điều hành</span>
                      <SearchSelect v-model="form.heDieuHanh" :options="optHeDieuHanhSelect" placeholder="-- Không chọn --" />
                    </label>
                    <label class="hh-field">
                      <span>Trọng lượng (kg)</span>
                      <SearchSelect v-model="form.trongLuongKg" :options="optTrongLuongSelect" placeholder="-- Không chọn --" />
                    </label>
                    <label class="hh-field">
                      <span>Bảo hành (tháng) <b>*</b></span>
                      <SearchSelect v-model="form.baoHanhThang" :options="optBaoHanhSelect" placeholder="-- Chọn --" />
                      <em v-if="errors.baoHanhThang" class="hh-err">{{ errors.baoHanhThang }}</em>
                    </label>
                    <label class="hh-field">
                      <span>Tiền tố mã SKU</span>
                      <input v-model.trim="form.skuPrefix" placeholder="Để trống sẽ lấy theo mã sản phẩm" />
                    </label>
                  </div>
                  <p class="hh-note">
                    <i class="fa fa-info-circle"></i>
                    Giá vốn và giá bán đặt sau — mở chi tiết sản phẩm, chọn phiên bản rồi bấm “Chỉnh sửa”, hoặc để phiếu nhập kho ghi giá vốn.
                  </p>
                </fieldset>

                <fieldset class="hh-block">
                  <legend>
                    Bước 3 · Danh sách phiên bản sẽ tạo
                    <span class="hh-chip">{{ bienTheRows.length }}</span>
                  </legend>

                  <em v-if="errors.bienThe" class="hh-err hh-mb8">{{ errors.bienThe }}</em>

                  <div class="hh-matrix-wrap">
                    <table class="hh-matrix">
                      <thead>
                        <tr>
                          <th class="hh-matrix__stt">#</th>
                          <th>Mã SKU</th>
                          <th>Mã vạch</th>
                          <th>Cấu hình</th>
                          <th></th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(row, i) in bienTheRows" :key="row.key">
                          <td class="hh-muted hh-matrix__stt">{{ i + 1 }}</td>
                          <td><input v-model.trim="row.maSku" class="hh-cell hh-cell--sku" /></td>
                          <td><input v-model.trim="row.barcode" class="hh-cell hh-cell--ma" placeholder="8–13 số" /></td>
                          <td class="hh-matrix__cfg">{{ moTaCauHinh(row) || 'Phiên bản tiêu chuẩn' }}</td>
                          <td class="ta-c">
                            <button type="button" class="hh-icon-btn" title="Bỏ phiên bản này" @click="xoaDong(row.key)">
                              <i class="fa fa-times"></i>
                            </button>
                          </td>
                        </tr>
                        <tr v-if="!bienTheRows.length">
                          <td colspan="5" class="hh-matrix__empty">Chưa có phiên bản nào — thêm thuộc tính ở Bước 1.</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </fieldset>
              </template>
            </div>

            <!-- ─────────── TAB 3: MÔ TẢ ─────────── -->
            <div v-show="tab === 'mota'" class="hh-pane">
              <fieldset class="hh-block" :disabled="modalMode === 'variant'">
                <legend>Mô tả sản phẩm</legend>

                <div class="hh-editor">
                  <div class="hh-editor__bar">
                    <button type="button" title="In đậm" @click="dinhDang('bold')"><b>B</b></button>
                    <button type="button" title="In nghiêng" @click="dinhDang('italic')"><i>I</i></button>
                    <button type="button" title="Gạch chân" @click="dinhDang('underline')"><u>U</u></button>
                    <span class="hh-editor__sep"></span>
                    <button type="button" title="Danh sách chấm" @click="dinhDang('insertUnorderedList')"><i class="fa fa-list-ul"></i></button>
                    <button type="button" title="Danh sách số" @click="dinhDang('insertOrderedList')"><i class="fa fa-list-ol"></i></button>
                    <span class="hh-editor__sep"></span>
                    <button type="button" title="Chèn liên kết" @click="chenLink"><i class="fa fa-link"></i></button>
                    <button type="button" title="Xóa định dạng" @click="dinhDang('removeFormat')"><i class="fa fa-eraser"></i></button>
                  </div>
                  <div
                    ref="moTaEl"
                    class="hh-editor__area"
                    contenteditable="true"
                    data-placeholder="Điểm nổi bật, đối tượng sử dụng, phụ kiện đi kèm…"
                    @input="form.moTa = $event.target.innerHTML"
                  ></div>
                </div>
              </fieldset>
            </div>
          </div>

          <footer class="hh-modal__foot">
            <div class="hh-modal__foot-left">
              <button type="button" class="hh-btn hh-btn--ghost" @click="closeModal">Bỏ qua</button>
            </div>
            <div class="hh-modal__foot-right">
              <button
                type="button" class="hh-btn hh-btn--primary"
                :disabled="isSaving || !formHopLe" :title="!formHopLe ? 'Điền đủ các ô bắt buộc (*) để lưu' : ''"
                @click="submitForm"
              >
                <i class="fa" :class="isSaving ? 'fa-spinner fa-spin' : 'fa-check'"></i>
                {{ isSaving ? 'Đang lưu…' : 'Lưu' }}
              </button>
            </div>
          </footer>
        </div>
      </div>
    </teleport>

    <!-- ══════════════ MODAL XUẤT FILE ══════════════ -->
    <teleport to="body">
      <div v-if="showExportModal" class="hh-modal-mask" @click.self="showExportModal = false">
        <div class="hh-modal hh-modal--hep" role="dialog" aria-modal="true">
          <header class="hh-modal__head">
            <div class="hh-head-main">
              <h2>Xuất file</h2>
              <p><span class="hh-head-path">Chọn sản phẩm / phiên bản muốn xuất</span></p>
            </div>
            <button class="hh-icon-btn" aria-label="Đóng" @click="showExportModal = false"><i class="fa fa-times"></i></button>
          </header>

          <div class="hh-modal__body">
            <div class="hh-export-toolbar">
              <label class="hh-export-checkall">
                <input type="checkbox" :checked="allChecked" @change="toggleAll" />
                <span>Chọn tất cả</span>
                <span class="hh-export-count">{{ selectedIds.length }}/{{ bienTheDaLoc.length }}</span>
              </label>
              <div class="hh-search hh-export-search">
                <i class="fa fa-search hh-search__icon"></i>
                <input v-model="exportSearch" type="text" placeholder="Tìm sản phẩm, SKU..." />
              </div>
            </div>

            <div class="hh-export-list">
              <div v-for="group in exportGroups" :key="group.sanPhamId" class="hh-export-group">
                <label class="hh-export-group__head">
                  <input
                    :ref="(el) => setIndeterminate(el, group)"
                    type="checkbox"
                    :checked="isGroupChecked(group)"
                    @change="toggleGroupCheck(group)"
                  />
                  <img :src="group.hinhAnh" class="hh-export-group__thumb" alt="" @error="onImgError" />
                  <div class="hh-export-group__info">
                    <div class="hh-export-group__name">{{ group.tenSanPham }}</div>
                    <div class="hh-export-group__meta">{{ group.maSanPham }} · {{ group.variants.length }} phiên bản</div>
                  </div>
                  <div class="hh-export-group__price">{{ group.khoangGia }}</div>
                </label>
                <label v-for="item in group.variants" :key="item.bienTheId" class="hh-export-variant">
                  <input type="checkbox" :checked="selectedIds.includes(item.bienTheId)" @change="toggleVariantCheck(item.bienTheId)" />
                  <span class="hh-export-variant__sku">{{ item.maSku }}</span>
                  <span class="hh-export-variant__spec">{{ [item.mauSac, item.tenCpu, item.tenRam].filter(Boolean).join(' · ') || '—' }}</span>
                  <span class="hh-export-variant__price">{{ formatNumber(item.giaBan) }} ₫</span>
                </label>
              </div>
              <div v-if="!exportGroups.length" class="hh-empty-cell">Không tìm thấy sản phẩm/phiên bản nào khớp</div>
            </div>
          </div>

          <footer class="hh-modal__foot">
            <div class="hh-modal__foot-left">
              <button type="button" class="hh-btn hh-btn--ghost" @click="showExportModal = false">Hủy</button>
            </div>
            <div class="hh-modal__foot-right">
              <button type="button" class="hh-btn hh-btn--primary" :disabled="!selectedIds.length" @click="exportCsv">
                <i class="fa fa-download"></i> Xuất file ({{ selectedIds.length }})
              </button>
            </div>
          </footer>
        </div>
      </div>
    </teleport>

    <teleport to="body">
      <div v-if="toast" class="hh-toast">{{ toast }}</div>
    </teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'

import { useAutoHideOnScroll } from '@/composables/useAutoHideOnScroll.js'
import SearchSelect from '@/components/common/SearchSelect.vue'
import TagComboInput from '@/components/common/TagComboInput.vue'
import { get, put } from '@/services/api.js'
import { refreshProducts as lamMoiKhoDuLieuChung } from '@/stores/products.js'
import { refreshInventory as lamMoiTonKhoDuLieuChung } from '@/stores/inventory.js'
import { getThuongHieu, getNhaCungCap, getCpu, getRam, getOCung, getGpu } from '@/services/DmService.js'
import * as bienTheApi from '@/services/bienTheSanPhamService.js'
import * as sanPhamApi from '@/services/sanPhamService.js'
import { getLichSu } from '@/services/SanPhamService.js'

/* ════════════════════════════════════════════════════════════
 * LỚP GỌI API
 * Mỗi service đặt tên hàm một kiểu (save / create / add) và chữ ký cũng
 * khác nhau — save(id, data) hay save(data). Gọi cứng save(null, payload)
 * mà service chỉ nhận 1 tham số thì body gửi lên là null và backend trả
 * 400 ngay. Hai hàm dưới dò đúng hàm có thật rồi gọi cho khớp chữ ký.
 * ══════════════════════════════════════════════════════════ */
const timHam = (api, ...tenList) => {
  for (const ten of tenList) if (typeof api?.[ten] === 'function') return api[ten]
  return null
}

const goiTao = (api, payload, tenApi) => {
  const fn = timHam(api, 'create', 'add', 'insert', 'post')
  if (fn) return fn(payload)
  const save = timHam(api, 'save')
  if (save) return save.length >= 2 ? save(null, payload) : save(payload)
  throw new Error(`${tenApi} không có hàm tạo mới (create / add / save)`)
}

const goiSua = (api, id, payload, tenApi) => {
  const fn = timHam(api, 'update', 'put', 'edit', 'save')
  if (fn) return fn.length >= 2 ? fn(id, payload) : fn(payload)
  throw new Error(`${tenApi} không có hàm cập nhật (update / save)`)
}

const apiTaoSanPham = (payload) => goiTao(sanPhamApi, payload, 'sanPhamService')
const apiSuaSanPham = (id, payload) => goiSua(sanPhamApi, id, payload, 'sanPhamService')
const apiTaoBienThe = (payload) => goiTao(bienTheApi, payload, 'bienTheSanPhamService')

/** Endpoint upload ảnh. Nếu backend chưa có, form tự chuyển sang
 *  chế độ "lấy tên file" → đường dẫn /images/<tên file>. */
const UPLOAD_URL = '/api/upload'
const THU_MUC_ANH = '/images/'

/* ─── Hằng số khớp CHECK constraint trong CSDL ─── */
/* SanPhamRequest chỉ có MỘT trường trangThai, service copy nó sang cả SanPham lẫn
   BienTheSanPham. Mà CK_bt_trangthai của bảng biến thể chỉ nhận active/inactive —
   gửi 'ngung_kinh_doanh' là insert biến thể đổ, nên bỏ hẳn lựa chọn đó. */
const TRANG_THAI_SAN_PHAM = [
  { value: 'active', label: 'Đang kinh doanh' },
  { value: 'inactive', label: 'Ngừng kinh doanh' }
]
const LOAI_SAN_PHAM = [
  { value: 'LAPTOP', label: 'Laptop' },
  { value: 'PHU_KIEN', label: 'Phụ kiện' },
  { value: 'DIEN_THOAI', label: 'Điện thoại' }
]
const PHAN_LOAI_DU_PHONG = [
  { phanLoaiId: 1, maPhanLoai: 'van_phong', tenPhanLoai: 'Văn phòng' },
  { phanLoaiId: 2, maPhanLoai: 'sinh_vien', tenPhanLoai: 'Sinh viên' },
  { phanLoaiId: 3, maPhanLoai: 'gaming', tenPhanLoai: 'Gaming' },
  { phanLoaiId: 4, maPhanLoai: 'do_hoa', tenPhanLoai: 'Đồ họa' },
  { phanLoaiId: 5, maPhanLoai: 'ky_thuat', tenPhanLoai: 'Kỹ thuật - AI' },
  { phanLoaiId: 6, maPhanLoai: 'macbook', tenPhanLoai: 'MacBook' },
  { phanLoaiId: 7, maPhanLoai: 'laptop_cu', tenPhanLoai: 'Laptop cũ' }
]
const MAN_HINH_GOI_Y = ['15.6" FHD 60Hz', '15.6" FHD 144Hz', '15.6" QHD 240Hz', '16" 2.5K 120Hz', '16" FHD 165Hz', '16" WQXGA 165Hz', '16" 2.8K OLED 120Hz']
const PIN_GOI_Y = ['41Wh', '48Wh', '50Wh', '52Wh', '54Wh', '57Wh', '75Wh', '80Wh', '86Wh', '90Wh']
const HDH_GOI_Y = ['Windows 11 Home', 'Windows 11 Pro', 'macOS', 'Không kèm HĐH']
const MAU_SAC_GOI_Y = ['Đen', 'Trắng', 'Bạc', 'Xám', 'Xanh Dương', 'Xanh Lá', 'Đỏ', 'Vàng', 'Hồng', 'Tím', 'Cam', 'Nâu']
const BAO_HANH_GOI_Y = [6, 12, 18, 24, 36]
const TRONG_LUONG_GOI_Y = [1.2, 1.3, 1.5, 1.7, 1.8, 2.0, 2.3, 2.5]

const ANH_MAC_DINH = 'https://cdn-icons-png.flaticon.com/512/664/664457.png'
const TOI_DA_BIEN_THE = 60

/* ─── Tiện ích ─── */
const toArray = (res) => (Array.isArray(res) ? res : (res?.content ?? res?.data?.content ?? res?.data ?? []))

const idOf = (obj, ...keys) => {
  for (const k of [...keys, 'id']) if (obj?.[k] != null) return obj[k]
  return null
}
const soHoacNull = (v) => (v === '' || v === null || v === undefined ? null : Number(v))
const formatNumber = (n) => Number(n || 0).toLocaleString('vi-VN')
const formatDate = (v) => {
  if (!v) return '—'
  const d = new Date(v)
  return Number.isNaN(d.getTime())
    ? '—'
    : d.toLocaleDateString('vi-VN') + ' ' + d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}
const nhanTrangThai = (tt) => ({ active: 'Đang bán', inactive: 'Tạm ngừng', ngung_kinh_doanh: 'Ngừng KD' }[tt] || tt || '—')
const nhanLoaiSanPham = (l) => LOAI_SAN_PHAM.find((x) => x.value === l)?.label || l || '—'
const tagClass = (tt) => (tt === 'active' ? 'hh-tag--ok' : 'hh-tag--off')
const onImgError = (e) => { e.target.src = ANH_MAC_DINH }
const khongDau = (s) =>
  String(s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[đĐ]/g, 'd').toLowerCase()
const vietTat = (s, n = 4) => khongDau(s).replace(/[^a-z0-9]/g, '').toUpperCase().slice(0, n)
const tenOCung = (oc) => oc?.loaiOCung || oc?.loaiOcung || oc?.ten || ''
const chuThuong = (s) => String(s || '').replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()

/* ─── Mã vạch EAN-13: chữ số kiểm tra + bộ sinh mã ─── */
const chuSoKiemTra = (base12) => {
  let tong = 0
  for (let i = 0; i < 12; i++) tong += Number(base12[i] || 0) * (i % 2 === 0 ? 1 : 3)
  return String((10 - (tong % 10)) % 10)
}
const sinhBarcode = (daDung = new Set()) => {
  for (let i = 0; i < 60; i++) {
    const base = '893' + String(Math.floor(Math.random() * 1e9)).padStart(9, '0')
    const ma = base + chuSoKiemTra(base)
    if (!daDung.has(ma)) { daDung.add(ma); return ma }
  }
  return ''
}

/* ─── Trạng thái màn hình ─── */
const isLoading = ref(false)
const loadError = ref('')
const toast = ref('')

const danhSachSanPham = ref([])
const bienThe = ref([])
const danhSachThuongHieu = ref([])
const danhSachDanhMuc = ref([])
const danhSachNhaCungCap = ref([])
const danhSachCpu = ref([])
const danhSachRam = ref([])
const danhSachOCung = ref([])
const danhSachGpu = ref([])
const danhSachPhanLoai = ref([])

const searchKeyword = ref('')
const isFilterOpen = ref(false)

// Thanh cong cu + bo loc dinh sticky tren dau trang, tu an khi cuon xuong / hien lai
// khi cuon len — chi khoa phan tren, phan duoi (bang du lieu) van cuon binh thuong.
const stickyHeadEl = ref(null)
const { hidden: hhBarHidden } = useAutoHideOnScroll(stickyHeadEl)
const filters = reactive({
  trangThai: '', thuongHieuId: '', nhaCungCapId: '', phanLoai: '',
  cpuId: '', ramId: '', mauSac: '', giaTu: '', giaDen: ''
})

const selectedIds = ref([])
const page = ref(1)
const pageSize = ref(10)

const hienToast = (msg) => {
  toast.value = msg
  setTimeout(() => (toast.value = ''), 3000)
}

/* ════════════ TẢI DỮ LIỆU ════════════ */
const fetchMasterData = async () => {
  const an = (p) => p.catch(() => [])
  const [th, dm, ncc, cpu, ram, oc, gpu, pl] = await Promise.all([
    an(getThuongHieu()), an(get('/api/danh-muc')), an(getNhaCungCap()),
    an(getCpu()), an(getRam()), an(getOCung()), an(getGpu()), an(get('/api/phan-loai'))
  ])
  danhSachThuongHieu.value = toArray(th)
  danhSachDanhMuc.value = toArray(dm)
  danhSachNhaCungCap.value = toArray(ncc)
  danhSachCpu.value = toArray(cpu)
  danhSachRam.value = toArray(ram)
  danhSachOCung.value = toArray(oc)
  danhSachGpu.value = toArray(gpu)
  danhSachPhanLoai.value = toArray(pl)
}

/* GET /api/san-pham/hien-thi trả Page<SanPhamResponse>: mỗi dòng là MỘT phiên bản,
   kèm sẵn thông tin sản phẩm cha, tên CPU/RAM/ổ cứng/GPU và số lượng tồn. Dùng đúng
   một nguồn này thay vì ghép hai API — không còn cảnh hai danh sách lệch nhau. */
const fetchAllRows = async () => {
  const all = []
  let p = 0
  let totalPages = 1
  while (p < totalPages && p < 20) {
    const res = await sanPhamApi.getPage({ page: p, size: 200 })
    all.push(...toArray(res))
    totalPages = res?.totalPages ?? 1
    p++
  }
  return all
}

const fetchData = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const rows = await fetchAllRows()
    danhSachSanPham.value = rows
    bienThe.value = rows
  } catch (e) {
    console.error('Lỗi tải dữ liệu hàng hóa:', e)
    loadError.value = 'Không tải được danh sách hàng hóa. Kiểm tra backend có đang chạy và bạn đã đăng nhập chưa.'
  } finally {
    isLoading.value = false
  }
}

onMounted(async () => {
  await fetchMasterData()
  await fetchData()
})

/* ════════════ TRA CỨU TÊN THEO ID ════════════ */
const lapMap = (list, idKey, nameKeys) => {
  const m = new Map()
  list.forEach((o) => {
    const key = idOf(o, idKey)
    const name = nameKeys.map((k) => o[k]).find((v) => v)
    if (key != null) m.set(String(key), name)
  })
  return m
}
const mapCpu = computed(() => lapMap(danhSachCpu.value, 'cpuId', ['tenCpu', 'ten']))
const mapRam = computed(() => lapMap(danhSachRam.value, 'ramId', ['dungLuong', 'tenRam', 'ten']))
const mapOCung = computed(() => lapMap(danhSachOCung.value, 'oCungId', ['loaiOCung', 'loaiOcung', 'ten']))
const mapGpu = computed(() => lapMap(danhSachGpu.value, 'gpuId', ['tenGpu', 'ten']))
const mapThuongHieu = computed(() => lapMap(danhSachThuongHieu.value, 'thuongHieuId', ['tenThuongHieu']))
const mapNhaCungCap = computed(() => lapMap(danhSachNhaCungCap.value, 'nhaCungCapId', ['tenNhaCungCap']))
const mapDanhMuc = computed(() => lapMap(danhSachDanhMuc.value, 'danhMucId', ['tenDanhMuc']))
const tra = (map, id) => (id == null || id === '' ? '' : map.get(String(id)) || '')

const phanLoaiOptions = computed(() => (danhSachPhanLoai.value.length ? danhSachPhanLoai.value : PHAN_LOAI_DU_PHONG))
const tenTheoMaPhanLoai = (ma) => phanLoaiOptions.value.find((p) => p.maPhanLoai === ma)?.tenPhanLoai || ma

/* ════════════ CHUẨN HÓA + GOM NHÓM ════════════ */

/** SanPhamResponse chỉ có tên linh kiện (cpu, ram, oCung, gpu là String).
 *  Tra ngược ra id để bộ lọc và form sửa vẫn chọn đúng mục. */
const traId = (map, ten) => {
  if (!ten) return null
  for (const [id, name] of map) if (name === ten) return Number(id)
  return null
}
const tenCua = (v) => (typeof v === 'string' ? v : '')

const bienTheChuan = computed(() =>
  bienThe.value.map((raw) => {
    const sanPhamId = raw.sanPhamId ?? raw.sanPham?.sanPhamId ?? raw.sanPham?.id ?? null
    const tenCpu = raw.tenCpu || tenCua(raw.cpu)
    const tenRam = raw.dungLuong || tenCua(raw.ram)
    const tenOCung = raw.loaiOCung || raw.loaiOcung || tenCua(raw.oCung)
    const tenGpu = raw.tenGpu || tenCua(raw.gpu)
    const cpuId = raw.cpuId ?? raw.cpu?.cpuId ?? traId(mapCpu.value, tenCpu)
    const ramId = raw.ramId ?? raw.ram?.ramId ?? traId(mapRam.value, tenRam)
    const oCungId = raw.oCungId ?? raw.oCung?.oCungId ?? traId(mapOCung.value, tenOCung)
    const gpuId = raw.gpuId ?? raw.gpu?.gpuId ?? traId(mapGpu.value, tenGpu)
    return {
      bienTheId: idOf(raw, 'bienTheId'),
      sanPhamId,
      maSku: raw.maSku || '—',
      // Mã vạch nay thuộc về TỪNG PHIÊN BẢN (bien_the_san_pham.barcode), không còn ở san_pham.
      barcode: raw.barcode || raw.barcodeBienThe || '',
      mauSac: raw.mauSac || '',
      giaBan: Number(raw.giaBan || 0),
      giaVon: Number(raw.giaNhap ?? raw.giaVon ?? 0),
      baoHanhThang: raw.baoHanhThang,
      tonKho: Number(raw.soLuongTon ?? raw.tonKho ?? raw.soLuongTonThucTe ?? 0),
      khachDat: Number(raw.khachDat ?? raw.soLuongGiu ?? 0),
      trangThai: raw.trangThai || 'active',
      ngayTao: raw.ngayTao,
      hinhAnh: raw.hinhAnhBienThe || '',
      kichThuocManHinh: raw.kichThuocManHinh || '',
      heDieuHanh: raw.heDieuHanh || '',
      pin: raw.pin || '',
      trongLuongKg: raw.trongLuongKg,
      cpuId, ramId, oCungId, gpuId,
      tenCpu: tenCpu || tra(mapCpu.value, cpuId),
      tenRam: tenRam || tra(mapRam.value, ramId),
      tenOCung: tenOCung || tra(mapOCung.value, oCungId),
      tenGpu: tenGpu || tra(mapGpu.value, gpuId),
      phanLoaiTags: raw.phanLoaiTags || ''
    }
  })
)

const barcodeDaDung = computed(() => new Set(bienTheChuan.value.map((v) => v.barcode).filter(Boolean)))
const moTaBienThe = (v) => [v.mauSac, v.tenCpu, v.tenRam, v.tenOCung, v.tenGpu].filter(Boolean).join(' · ')

const khoangGia = (ds) => {
  if (!ds.length) return '—'
  const min = Math.min(...ds)
  const max = Math.max(...ds)
  return min === max ? formatNumber(min) : `${formatNumber(min)} – ${formatNumber(max)}`
}

/** Mã hiển thị: ưu tiên ma_san_pham; sản phẩm cũ chưa gán mã thì
 *  hiện tạm SP + id để cột này không bao giờ trống. */
const maHienThi = (sp, id) => sp?.maSanPham || 'SP' + String(id).padStart(4, '0')

const groups = computed(() => {
  const map = new Map()
  bienTheChuan.value.forEach((v) => {
    const key = String(v.sanPhamId ?? 'khac')
    if (!map.has(key)) {
      const sp = danhSachSanPham.value.find((p) => String(idOf(p, 'sanPhamId')) === key) || {}
      map.set(key, {
        sanPhamId: key,
        maSanPham: maHienThi(sp, key),
        coMaThat: !!sp.maSanPham,
        tenSanPham: sp.tenSanPham || 'Sản phẩm chưa đặt tên',
        moTa: sp.moTa || '',
        hinhAnh: sp.hinhAnhChinh || ANH_MAC_DINH,
        thuongHieuId: sp.thuongHieuId ?? sp.thuongHieu?.thuongHieuId ?? null,
        danhMucId: sp.danhMucId ?? sp.danhMuc?.danhMucId ?? null,
        nhaCungCapId: sp.nhaCungCapId ?? sp.nhaCungCap?.nhaCungCapId ?? null,
        loaiSanPham: sp.loaiSanPham || 'LAPTOP',
        trangThai: sp.trangThai || 'active',
        ngayTao: sp.ngayTao,
        ngayCapNhat: sp.ngayCapNhat,
        tenThuongHieu: sp.tenThuongHieu || tra(mapThuongHieu.value, sp.thuongHieuId),
        tenNhaCungCap: sp.tenNhaCungCap || tra(mapNhaCungCap.value, sp.nhaCungCapId),
        tenDanhMuc: sp.tenDanhMuc || tra(mapDanhMuc.value, sp.danhMucId),
        variants: []
      })
    }
    const g = map.get(key)
    g.variants.push({ ...v, tenPhienBan: [g.tenSanPham, v.mauSac, v.tenCpu, v.tenRam].filter(Boolean).join(' · ') })
  })

  return [...map.values()].map((g) => {
    const dau = g.variants[0] || {}
    const tags = dau.phanLoaiTags
    const soThang = [...new Set(g.variants.map((v) => v.baoHanhThang).filter((x) => x != null))]
    return {
      ...g,
      phanLoai: tags ? tags.split(',').map((t) => t.trim()).filter(Boolean) : [],
      khoangGia: khoangGia(g.variants.map((v) => v.giaBan)),
      khoangGiaVon: khoangGia(g.variants.map((v) => v.giaVon)),
      tonKho: g.variants.reduce((s, v) => s + v.tonKho, 0),
      khachDat: g.variants.reduce((s, v) => s + v.khachDat, 0),
      baoHanh: soThang.length ? soThang.join(' / ') + ' tháng' : 'Chưa có',
      thongSo: {
        kichThuocManHinh: dau.kichThuocManHinh,
        heDieuHanh: dau.heDieuHanh,
        pin: dau.pin,
        trongLuongKg: dau.trongLuongKg
      }
    }
  })
})

/* ════════════ TÌM KIẾM + LỌC ════════════ */
const soBoLocDangDung = computed(() => Object.values(filters).filter((v) => v !== '' && v !== null).length)
const coBoLoc = computed(() => !!searchKeyword.value || soBoLocDangDung.value > 0)

const khopTuKhoa = (group, v) => {
  const kw = khongDau(searchKeyword.value.trim())
  if (!kw) return true
  return [group.maSanPham, group.tenSanPham, v.maSku, v.barcode, v.mauSac, v.tenCpu].some((f) => khongDau(f).includes(kw))
}

const khopBoLoc = (group, v) => {
  if (filters.trangThai && group.trangThai !== filters.trangThai) return false
  if (filters.thuongHieuId && String(group.thuongHieuId) !== String(filters.thuongHieuId)) return false
  if (filters.nhaCungCapId && String(group.nhaCungCapId) !== String(filters.nhaCungCapId)) return false
  if (filters.phanLoai && !v.phanLoaiTags.split(',').includes(filters.phanLoai)) return false
  if (filters.cpuId && String(v.cpuId) !== String(filters.cpuId)) return false
  if (filters.ramId && String(v.ramId) !== String(filters.ramId)) return false
  if (filters.mauSac && v.mauSac !== filters.mauSac) return false
  if (filters.giaTu !== '' && v.giaBan < Number(filters.giaTu)) return false
  if (filters.giaDen !== '' && v.giaBan > Number(filters.giaDen)) return false
  return true
}

const groupsDaLoc = computed(() =>
  groups.value
    .map((g) => ({ ...g, variants: g.variants.filter((v) => khopTuKhoa(g, v) && khopBoLoc(g, v)) }))
    .filter((g) => g.variants.length)
)
const bienTheDaLoc = computed(() => groupsDaLoc.value.flatMap((g) => g.variants.map((v) => ({ g, v }))))
const danhSachMauSac = computed(() => [...new Set(bienTheChuan.value.map((v) => v.mauSac).filter(Boolean))].sort())

/* ════════════ PHÂN TRANG ════════════ */
const totalPages = computed(() => Math.ceil(groupsDaLoc.value.length / pageSize.value))
const pagedGroups = computed(() => groupsDaLoc.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
watch([searchKeyword, filters, pageSize], () => { page.value = 1 }, { deep: true })

const resetFilters = () => {
  Object.keys(filters).forEach((k) => (filters[k] = ''))
  searchKeyword.value = ''
}

/* ════════════ MODAL XUẤT FILE ════════════ */
const showExportModal = ref(false)
const exportSearch = ref('')

const openExportModal = () => {
  selectedIds.value = bienTheDaLoc.value.map(({ v }) => v.bienTheId)
  exportSearch.value = ''
  showExportModal.value = true
}

const exportGroups = computed(() => {
  const q = khongDau(exportSearch.value.trim())
  if (!q) return groupsDaLoc.value
  return groupsDaLoc.value.filter((g) =>
    khongDau(g.tenSanPham).includes(q) || g.variants.some((v) => khongDau(v.maSku).includes(q))
  )
})

const isGroupChecked = (g) => g.variants.length > 0 && g.variants.every((v) => selectedIds.value.includes(v.bienTheId))
const toggleGroupCheck = (g) => {
  const ids = g.variants.map((v) => v.bienTheId)
  selectedIds.value = isGroupChecked(g)
    ? selectedIds.value.filter((id) => !ids.includes(id))
    : [...new Set([...selectedIds.value, ...ids])]
}
const toggleVariantCheck = (id) => {
  const i = selectedIds.value.indexOf(id)
  if (i === -1) selectedIds.value.push(id)
  else selectedIds.value.splice(i, 1)
}
const setIndeterminate = (el, g) => {
  if (!el) return
  const checkedCount = g.variants.filter((v) => selectedIds.value.includes(v.bienTheId)).length
  el.indeterminate = checkedCount > 0 && checkedCount < g.variants.length
}
const allChecked = computed(() => bienTheDaLoc.value.length > 0 && bienTheDaLoc.value.every(({ v }) => selectedIds.value.includes(v.bienTheId)))
const toggleAll = () => { selectedIds.value = allChecked.value ? [] : bienTheDaLoc.value.map(({ v }) => v.bienTheId) }

const exportCsv = () => {
  const rows = bienTheDaLoc.value.filter(({ v }) => selectedIds.value.includes(v.bienTheId))
  showExportModal.value = false

  const cols = ['Mã sản phẩm', 'Tên sản phẩm', 'Mã SKU', 'Mã vạch', 'Màu sắc', 'CPU', 'RAM', 'Ổ cứng', 'GPU',
    'Màn hình', 'Giá vốn', 'Giá bán', 'Tồn kho', 'Bảo hành (tháng)', 'Trạng thái', 'Thương hiệu', 'Nhà cung cấp']
  const esc = (val) => `"${String(val ?? '').replace(/"/g, '""')}"`
  const lines = [cols.map(esc).join(',')]

  rows.forEach(({ g, v }) => {
    lines.push([g.maSanPham, g.tenSanPham, v.maSku, v.barcode, v.mauSac, v.tenCpu, v.tenRam, v.tenOCung, v.tenGpu,
      v.kichThuocManHinh, v.giaVon, v.giaBan, v.tonKho, v.baoHanhThang,
      nhanTrangThai(v.trangThai), g.tenThuongHieu, g.tenNhaCungCap].map(esc).join(','))
  })

  const blob = new Blob(['\uFEFF' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `hang-hoa-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  hienToast(`Đã xuất ${rows.length} dòng ra file CSV`)
}

/* ════════════════════════════════════════════════════════════
 *              NHẬT KÝ THAY ĐỔI (tab "Lịch sử thay đổi")
 * CSDL chưa có bảng lịch sử cho hàng hóa, nên nhật ký lưu ở localStorage của
 * máy đang dùng: mỗi lần LƯU THÀNH CÔNG mới ghi một dòng. Khi backend có bảng
 * riêng, chỉ cần thay 2 hàm docNhatKy/ghiNhatKy bằng lời gọi API là xong.
 * ══════════════════════════════════════════════════════════ */
const KHOA_NHAT_KY = 'saophone_nhatky_hang_hoa'
const docNhatKyCu = () => {
  try { return JSON.parse(localStorage.getItem(KHOA_NHAT_KY) || '{}') } catch { return {} }
}
const nhatKy = ref(docNhatKyCu())
const nhatKyLoading = ref(false)

// Transform backend response → format template
const transformLichSu = (backendList) =>
  (backendList || []).map((m) => ({
    thoiGian: m.thoiGian,
    nguoiDung: m.tenNhanVien || 'không rõ',
    hanhDong: m.doiTuong || 'Thay đổi',
    doiTuong: m.tenTruong ? `${m.tenTruong} · SKU ${m.maSku || '?'}` : m.maSku || null,
    loai: 'sua',
    thayDoi: (m.tenTruong && m.giaTriCu !== m.giaTriMoi)
      ? [{ truong: m.tenTruong, cu: m.giaTriCu, moi: m.giaTriMoi }]
      : [],
  }))

// Gọi API khi user mở tab lichsu
const taiLichSu = async (sanPhamId) => {
  if (!sanPhamId) return
  nhatKyLoading.value = true
  try {
    const apiData = await getLichSu(sanPhamId)
    const apiList = transformLichSu(apiData)
    // Merge: bản ghi cũ từ localStorage (key là string sanPhamId) + bản ghi mới từ API
    const kho = { ...nhatKy.value }
    const local = kho[String(sanPhamId)] || []
    // API trả theo thứ tự mới → cũ, giống format local — trộn và de-dupe theo thoiGian
    const seenTimes = new Set(local.map((l) => l.thoiGian))
    const moiTuApi = apiList.filter((a) => !seenTimes.has(a.thoiGian))
    kho[String(sanPhamId)] = [...local, ...moiTuApi].sort(
      (a, b) => new Date(b.thoiGian) - new Date(a.thoiGian),
    )
    nhatKy.value = kho
    try { localStorage.setItem(KHOA_NHAT_KY, JSON.stringify(kho)) } catch { /* bỏ qua */ }
  } catch {
    // không ảnh hưởng UX nếu lỗi mạng
  } finally {
    nhatKyLoading.value = false
  }
}

const nguoiDangDangNhap = () => {
  try {
    const j = JSON.parse(sessionStorage.getItem('saophone_session') || '{}')
    return j?.hoTen || j?.username || j?.user?.username || 'không rõ'
  } catch { return 'không rõ' }
}

const ghiNhatKy = (sanPhamId, muc) => {
  if (!sanPhamId) return
  const key = String(sanPhamId)
  const kho = { ...nhatKy.value }
  kho[key] = [{ thoiGian: new Date().toISOString(), nguoiDung: nguoiDangDangNhap(), ...muc }, ...(kho[key] || [])].slice(0, 50)
  nhatKy.value = kho
  try { localStorage.setItem(KHOA_NHAT_KY, JSON.stringify(kho)) } catch (e) { console.warn('[Hàng hóa] không ghi được nhật ký:', e) }
}

/* ════════════════════════════════════════════════════════════
 *                    MODAL CHI TIẾT SẢN PHẨM
 * ══════════════════════════════════════════════════════════ */
const showDetail = ref(false)
const tabCT = ref('info')
const chiTietId = ref(null)
const bienTheChonId = ref(null)
const anhDangXem = ref('')
const dangSaoChepBienThe = ref(false)

watch(tabCT, (tab) => { if (tab === 'lichsu' && chiTietId.value) taiLichSu(chiTietId.value) })

/* Lấy thẳng từ groups nên sau mỗi lần lưu + fetchData, cửa sổ chi tiết tự cập nhật */
const chiTiet = computed(() => groups.value.find((g) => String(g.sanPhamId) === String(chiTietId.value)) || null)
const anhSanPham = computed(() => {
  if (!chiTiet.value) return []
  const ds = [chiTiet.value.hinhAnh, ...chiTiet.value.variants.map((v) => v.hinhAnh)].filter(Boolean)
  return [...new Set(ds)]
})
const bienTheDangChon = computed(() =>
  chiTiet.value?.variants.find((v) => String(v.bienTheId) === String(bienTheChonId.value)) || null
)
const lichSuHienTai = computed(() => (chiTiet.value ? nhatKy.value[String(chiTiet.value.sanPhamId)] || [] : []))

const moChiTiet = (group) => {
  chiTietId.value = group.sanPhamId
  bienTheChonId.value = group.variants[0]?.bienTheId ?? null
  anhDangXem.value = group.hinhAnh || ANH_MAC_DINH
  tabCT.value = 'info'
  showDetail.value = true
}
const dongChiTiet = () => { showDetail.value = false }

const xoaLichSu = () => {
  if (!chiTiet.value) return
  const kho = { ...nhatKy.value }
  delete kho[String(chiTiet.value.sanPhamId)]
  nhatKy.value = kho
  try { localStorage.setItem(KHOA_NHAT_KY, JSON.stringify(kho)) } catch { /* bỏ qua */ }
  hienToast('Đã xóa nhật ký của sản phẩm này')
}

/* ─── In tem mã: vẽ mã vạch EAN-13 bằng SVG rồi mở cửa sổ in ─── */
const EAN_L = ['0001101', '0011001', '0010011', '0111101', '0100011', '0110001', '0101111', '0111011', '0110111', '0001011']
const EAN_G = ['0100111', '0110011', '0011011', '0100001', '0011101', '0111001', '0000101', '0010001', '0001001', '0010111']
const EAN_R = ['1110010', '1100110', '1101100', '1000010', '1011100', '1001110', '1010000', '1000100', '1001000', '1110100']
const EAN_PARITY = ['LLLLLL', 'LLGLGG', 'LLGGLG', 'LLGGGL', 'LGLLGG', 'LGGLLG', 'LGGGLL', 'LGLGLG', 'LGLGGL', 'LGGLGL']

const veMaVach = (ma) => {
  const s = String(ma || '')
  if (!/^\d{13}$/.test(s)) return ''
  const parity = EAN_PARITY[Number(s[0])]
  let bits = '101'
  for (let i = 1; i <= 6; i++) bits += (parity[i - 1] === 'L' ? EAN_L : EAN_G)[Number(s[i])]
  bits += '01010'
  for (let i = 7; i <= 12; i++) bits += EAN_R[Number(s[i])]
  bits += '101'

  const w = 2
  const h = 56
  let rects = ''
  for (let i = 0; i < bits.length; i++) if (bits[i] === '1') rects += `<rect x="${i * w}" y="0" width="${w}" height="${h}"/>`
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${bits.length * w}" height="${h}" viewBox="0 0 ${bits.length * w} ${h}" fill="#000">${rects}</svg>`
}

const thoat = (s) => String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

const inTemMa = (v) => {
  if (!v || !chiTiet.value) return
  const g = chiTiet.value
  const svg = veMaVach(v.barcode)
  const cua = window.open('', '_blank', 'width=460,height=600')
  if (!cua) { hienToast('Trình duyệt đang chặn cửa sổ in — cho phép pop-up rồi thử lại'); return }

  cua.document.write(`<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8" />
    <title>Tem ${thoat(v.maSku)}</title>
    <style>
      *{box-sizing:border-box} body{margin:0;padding:16px;font-family:"Segoe UI",Roboto,Arial,sans-serif;color:#111}
      .tem{width:58mm;border:1px dashed #bbb;border-radius:6px;padding:8px 10px;text-align:center}
      .ten{font-size:11px;font-weight:700;line-height:1.3;margin-bottom:2px;word-break:break-word}
      .cfg{font-size:9px;color:#555;line-height:1.3;margin-bottom:4px;word-break:break-word}
      .sku{font-size:9px;font-family:ui-monospace,Menlo,monospace;margin-bottom:4px}
      .so{font-size:11px;font-family:ui-monospace,Menlo,monospace;letter-spacing:2px;margin-top:2px}
      .gia{font-size:13px;font-weight:700;margin-top:4px}
      @media print{ body{padding:0} .tem{border:none} }
    </style></head><body>
    <div class="tem">
      <div class="ten">${thoat(g.tenSanPham)}</div>
      <div class="cfg">${thoat(moTaBienThe(v) || 'Phiên bản tiêu chuẩn')}</div>
      <div class="sku">SKU: ${thoat(v.maSku)}</div>
      ${svg || '<div class="cfg">Phiên bản này chưa có mã vạch EAN-13</div>'}
      <div class="so">${thoat(v.barcode || '')}</div>
      <div class="gia">${formatNumber(v.giaBan)} đ</div>
    </div></body></html>`)
  cua.document.close()
  cua.focus()
  setTimeout(() => cua.print(), 350)
}

/* ════════════════════════════════════════════════════════════
 *                    MODAL THÊM / SỬA
 * ══════════════════════════════════════════════════════════ */
const showModal = ref(false)
const modalMode = ref('create') // 'create' | 'edit' | 'variant'
const tab = ref('info')
const isSaving = ref(false)
const saveError = ref('')
const errors = reactive({})
const moTaEl = ref(null)
const dangTaiAnh = ref(false)
const ghiChuAnh = ref('Ảnh tải lên sẽ được lưu về server; nếu backend chưa có API upload, hệ thống dùng đường dẫn /images/<tên file>. Ảnh đầu tiên trong danh sách là ảnh đại diện.')
const moLaiChiTiet = ref(null) // sanPhamId cần mở lại cửa sổ chi tiết sau khi lưu

const tabs = [
  { key: 'info', label: 'Thông tin' },
  { key: 'bienthe', label: 'Phiên bản' },
  { key: 'mota', label: 'Mô tả' }
]

const formRong = () => ({
  sanPhamId: null,
  bienTheId: null,
  // san_pham (không còn barcode ở cấp sản phẩm)
  maSanPham: '', tenSanPham: '',
  thuongHieuId: '', danhMucId: '', nhaCungCapId: '',
  loaiSanPham: 'LAPTOP', trangThaiSanPham: 'active',
  moTa: '', hinhAnhList: [], phanLoaiIds: [], phanLoaiTags: '', phanLoaiTen: '',
  // thông số chung của phiên bản
  giaNhap: 0, giaBan: 0, baoHanhThang: 24,
  kichThuocManHinh: '', heDieuHanh: 'Windows 11 Home', pin: '', trongLuongKg: '',
  skuPrefix: '',
  // thuộc tính trộn ra phiên bản
  mauSacList: [], ramIds: [], cpuIds: [], oCungIds: [], gpuIds: [],
  // chỉ dùng khi sửa 1 phiên bản
  maSku: '', barcode: '', mauSac: '', cpuId: '', ramId: '', oCungId: '', gpuId: '', hinhAnhBienThe: ''
})

const form = reactive(formRong())
const chon = reactive({ mauSac: '', ramIds: '', cpuIds: '', oCungIds: '', gpuIds: '' })
const bienTheRows = ref([])

const tieuDeModal = computed(() =>
  ({ create: 'Tạo hàng hóa', edit: 'Chỉnh sửa hàng hóa', variant: 'Thêm phiên bản cho sản phẩm' }[modalMode.value])
)
const soPhienBan = computed(() => (modalMode.value === 'edit' ? 0 : bienTheRows.value.length))

/* ─── Đồng hồ hiển thị thời điểm sẽ ghi vào ngày tạo ─── */
const dongHo = ref('')
let dongHoTimer = null
const capNhatDongHo = () => { dongHo.value = new Date().toLocaleString('vi-VN') }
watch(showModal, (mo) => {
  clearInterval(dongHoTimer)
  if (mo) {
    capNhatDongHo()
    dongHoTimer = setInterval(capNhatDongHo, 1000)
  }
})
onBeforeUnmount(() => clearInterval(dongHoTimer))

/* ─── Gợi ý cho các select dạng văn bản: giá trị đã có + danh sách chuẩn ─── */
const gopGoiY = (key, goiY) =>
  [...new Set([...goiY, ...bienTheChuan.value.map((v) => v[key]).filter(Boolean)])].sort()
const optManHinh = computed(() => gopGoiY('kichThuocManHinh', MAN_HINH_GOI_Y))
const optPin = computed(() => gopGoiY('pin', PIN_GOI_Y))
const optHeDieuHanh = computed(() => gopGoiY('heDieuHanh', HDH_GOI_Y))
const optMauSac = computed(() => gopGoiY('mauSac', MAU_SAC_GOI_Y))
// Bảo hành/trọng lượng là số — gopGoiY() sắp theo kiểu chuỗi sẽ sai thứ tự (vd 12 đứng
// trước 6), nên gộp + sắp số rieng cho 2 truong nay.
const gopGoiYSo = (key, goiY) =>
  [...new Set([...goiY, ...bienTheChuan.value.map((v) => Number(v[key])).filter((x) => Number.isFinite(x) && x > 0)])]
    .sort((a, b) => a - b)
const optBaoHanh = computed(() => gopGoiYSo('baoHanhThang', BAO_HANH_GOI_Y))
const optTrongLuong = computed(() => gopGoiYSo('trongLuongKg', TRONG_LUONG_GOI_Y))

/* ─── Bản {value,label} cho SearchSelect (combobox tự vẽ, không dùng datalist trình
   duyệt — popup datalist không style được, mỗi máy/trình duyệt hiện 1 kiểu xấu khác
   nhau). Giữ nguyên các opt* gốc ở trên vì nơi khác (chuẩn hóa màu, ô gõ-Enter-thêm-thẻ
   ở Bước 1) đang cần mảng giá trị thô, không phải mảng {value,label}. ─── */
const asOptions = (arr) => arr.map((v) => ({ value: v, label: String(v) }))
const optMauSacSelect = computed(() => asOptions(optMauSac.value))
const optManHinhSelect = computed(() => asOptions(optManHinh.value))
const optPinSelect = computed(() => asOptions(optPin.value))
const optHeDieuHanhSelect = computed(() => asOptions(optHeDieuHanh.value))
const optBaoHanhSelect = computed(() => optBaoHanh.value.map((v) => ({ value: v, label: `${v} tháng` })))
const optTrongLuongSelect = computed(() => optTrongLuong.value.map((v) => ({ value: v, label: `${v} kg` })))

const KHONG_CHON = { value: '', label: '-- Không chọn --' }
const cpuOptionsSel = computed(() => [KHONG_CHON, ...danhSachCpu.value.map((c) => ({ value: idOf(c, 'cpuId'), label: c.tenCpu }))])
const ramOptionsSel = computed(() => [KHONG_CHON, ...danhSachRam.value.map((r) => ({ value: idOf(r, 'ramId'), label: r.dungLuong || r.tenRam }))])
const oCungOptionsSel = computed(() => [KHONG_CHON, ...danhSachOCung.value.map((o) => ({ value: idOf(o, 'oCungId'), label: tenOCung(o) }))])
const gpuOptionsSel = computed(() => [KHONG_CHON, ...danhSachGpu.value.map((g) => ({ value: idOf(g, 'gpuId'), label: g.tenGpu }))])

/* ─── Thuộc tính dùng để trộn — gõ rồi Enter là ra thẻ ─── */
const thuocTinhTron = [
  {
    field: 'cpuIds', label: 'CPU',
    options: () => danhSachCpu.value.map((o) => ({ id: idOf(o, 'cpuId'), ten: o.tenCpu })),
    ten: (id) => tra(mapCpu.value, id)
  },
  {
    field: 'ramIds', label: 'RAM',
    options: () => danhSachRam.value.map((o) => ({ id: idOf(o, 'ramId'), ten: o.dungLuong || o.tenRam })),
    ten: (id) => tra(mapRam.value, id)
  },
  {
    field: 'oCungIds', label: 'Ổ cứng',
    options: () => danhSachOCung.value.map((o) => ({ id: idOf(o, 'oCungId'), ten: tenOCung(o) })),
    ten: (id) => tra(mapOCung.value, id)
  },
  {
    field: 'gpuIds', label: 'GPU',
    options: () => danhSachGpu.value.map((o) => ({ id: idOf(o, 'gpuId'), ten: o.tenGpu })),
    ten: (id) => tra(mapGpu.value, id)
  }
]

/** Thêm giá trị đang gõ vào danh sách thẻ.
 *  imLang = true khi gọi từ sự kiện change (người dùng bấm chọn trong gợi ý hoặc rời ô)
 *  — chỉ nhận giá trị khớp chính xác, không báo lỗi để khỏi làm phiền khi gõ dở. */
const themThuocTinh = (field, imLang = false) => {
  const attr = thuocTinhTron.find((a) => a.field === field)
  if (!attr) return
  const raw = String(chon[field] || '').trim()
  if (!raw) return

  const ds = attr.options().filter((o) => o.id != null && o.ten)
  const chinhXac = ds.find((o) => khongDau(o.ten) === khongDau(raw))
  const gan = chinhXac || (imLang ? null : ds.find((o) => khongDau(o.ten).includes(khongDau(raw))))

  if (!gan) {
    if (!imLang) hienToast(`Không có ${attr.label} nào tên “${raw}” — chọn trong danh sách gợi ý`)
    return
  }
  if (!form[field].includes(gan.id)) form[field].push(gan.id)
  chon[field] = ''
}
const xoaThuocTinh = (field, id) => { form[field] = form[field].filter((x) => x !== id) }

const themMau = (imLang = false) => {
  const raw = String(chon.mauSac || '').trim()
  if (!raw) return
  // Màu là chữ tự do (CSDL không có bảng màu) — gõ gì nhận nấy, chỉ chuẩn hóa nếu trùng gợi ý
  const chuan = optMauSac.value.find((m) => khongDau(m) === khongDau(raw)) || raw
  if (imLang && !optMauSac.value.some((m) => khongDau(m) === khongDau(raw))) return
  if (!form.mauSacList.includes(chuan)) form.mauSacList.push(chuan)
  chon.mauSac = ''
}
const xoaMau = (m) => { form.mauSacList = form.mauSacList.filter((x) => x !== m) }

/* ─── Phân loại (chọn nhiều bằng chip bật/tắt) ─── */
const tenPhanLoai = (id) => phanLoaiOptions.value.find((p) => String(p.phanLoaiId) === String(id))?.tenPhanLoai || id
const maPhanLoai = (id) => phanLoaiOptions.value.find((p) => String(p.phanLoaiId) === String(id))?.maPhanLoai || ''
const idPhanLoaiTuMa = (dsMa = []) =>
  dsMa.map((ma) => phanLoaiOptions.value.find((p) => p.maPhanLoai === ma)?.phanLoaiId).filter((x) => x != null)
const togglePhanLoai = (id) => {
  form.phanLoaiIds = form.phanLoaiIds.includes(id)
    ? form.phanLoaiIds.filter((x) => x !== id)
    : [...form.phanLoaiIds, id]
}

/* Hai cột cache phan_loai_tags / phan_loai_ten trong CSDL */
watch(() => form.phanLoaiIds.slice(), (ids) => {
  form.phanLoaiTags = ids.map(maPhanLoai).filter(Boolean).join(',')
  form.phanLoaiTen = ids.map(tenPhanLoai).filter(Boolean).join(', ')
}, { deep: true })

/* ─── Ảnh: chọn từ máy ─── */
const layToken = () => {
  const raw = sessionStorage.getItem('saophone_session') || ''
  try {
    const j = JSON.parse(raw)
    return j?.token || j?.accessToken || ''
  } catch {
    return raw
  }
}

const uploadAnh = async (file) => {
  const fd = new FormData()
  fd.append('file', file)
  const token = layToken()
  const res = await fetch(UPLOAD_URL, {
    method: 'POST',
    body: fd,
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!res.ok) throw new Error('upload thất bại')
  const data = await res.json().catch(() => ({}))
  const url = data?.url || data?.path || data?.duongDan || data?.data
  if (!url) throw new Error('API upload không trả về đường dẫn')
  return url
}

const chonAnhSanPham = async (e) => {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  dangTaiAnh.value = true
  for (const file of files) {
    try {
      form.hinhAnhList.push(await uploadAnh(file))
    } catch {
      const duongDan = THU_MUC_ANH + file.name
      form.hinhAnhList.push(duongDan)
      ghiChuAnh.value = `Chưa có API upload — đã đặt đường dẫn ${duongDan}. Hãy chép file ảnh vào thư mục public${THU_MUC_ANH} của FrontEnd.`
    }
  }
  dangTaiAnh.value = false
  e.target.value = ''
}

const themAnhTuUrl = (e) => {
  const url = e.target.value.trim()
  if (url) form.hinhAnhList.push(url)
  e.target.value = ''
}
const xoaAnhTaiViTri = (i) => { form.hinhAnhList.splice(i, 1) }
const datLamAnhChinh = (i) => {
  const [anh] = form.hinhAnhList.splice(i, 1)
  form.hinhAnhList.unshift(anh)
}

/* ─── Trình soạn mô tả ─── */
const dinhDang = (cmd) => {
  moTaEl.value?.focus()
  document.execCommand(cmd, false, null)
  form.moTa = moTaEl.value?.innerHTML || ''
}
const chenLink = () => {
  const url = window.prompt('Nhập đường dẫn:')
  if (!url) return
  moTaEl.value?.focus()
  document.execCommand('createLink', false, url)
  form.moTa = moTaEl.value?.innerHTML || ''
}
watch([showModal, tab], async () => {
  if (!showModal.value || tab.value !== 'mota') return
  await nextTick()
  if (moTaEl.value && moTaEl.value.innerHTML !== form.moTa) moTaEl.value.innerHTML = form.moTa || ''
})

/* ─── Sinh mã sản phẩm (ô này chỉ đọc, không cho gõ tay) ─── */
const sinhMaSanPham = () => {
  const soTuMa = danhSachSanPham.value
    .map((p) => Number(String(p.maSanPham || '').replace(/\D/g, '')))
    .filter((n) => !Number.isNaN(n) && n > 0)
  // Cộng thêm sanPhamId vào phép tính max — sản phẩm CHƯA gán mã (maSanPham NULL) vẫn hiện
  // tạm "SPxxxx" theo id trên UI (xem maHienThi()), dù không lưu vào cột maSanPham nên
  // không đụng UNIQUE index thật. Bỏ qua các id này khi sinh mã mới sẽ có lúc trùng NHÌN
  // GIỐNG hệt một mã tạm đang hiển thị của sản phẩm khác — trông như trùng mã dù CSDL
  // không báo lỗi gì. Tính cả id vào max để mã thật luôn vượt qua mọi mã tạm hiện có.
  const soTuId = danhSachSanPham.value
    .map((p) => Number(idOf(p, 'sanPhamId')))
    .filter((n) => !Number.isNaN(n) && n > 0)
  const max = Math.max(0, ...soTuMa, ...soTuId)
  return 'SP' + String(max + 1).padStart(4, '0')
}

/* ─── Ma trận phiên bản ─── */
const moTaCauHinh = (row) =>
  [row.mauSac, tra(mapCpu.value, row.cpuId), tra(mapRam.value, row.ramId), tra(mapOCung.value, row.oCungId), tra(mapGpu.value, row.gpuId)]
    .filter(Boolean)
    .join(' · ')

const khoaDong = (c) => [c.mauSac, c.cpuId, c.ramId, c.oCungId, c.gpuId].join('|')

const sinhSku = (c, index) => {
  const prefix = (form.skuPrefix || form.maSanPham || vietTat(form.tenSanPham, 6) || 'SP').toUpperCase()
  // Thiếu GPU trong chuỗi này là lý do 2 phiên bản khác GPU (vd cùng CPU/RAM/ổ cứng/màu,
  // chỉ khác card đồ họa) sinh ra CÙNG một mã SKU — thêm GPU vào để phân biệt đúng.
  const phan = [
    vietTat(tra(mapCpu.value, c.cpuId).split(' ').pop(), 6),
    vietTat(tra(mapRam.value, c.ramId), 5),
    vietTat(tra(mapOCung.value, c.oCungId), 5),
    vietTat(tra(mapGpu.value, c.gpuId), 5),
    vietTat(c.mauSac, 3)
  ].filter(Boolean)
  return [prefix, ...phan].join('-') || `${prefix}-${index + 1}`
}

/** SKU phải là duy nhất toàn hệ thống — nếu trùng thì thêm hậu tố -2, -3…
 *  (hay gặp khi bấm "Sao chép" một phiên bản: cấu hình giống hệt nên SKU sinh ra y như cũ) */
const skuKhongTrung = (goc, daDung) => {
  let ma = goc
  let i = 2
  while (daDung.has(ma)) { ma = `${goc}-${i}`; i++ }
  daDung.add(ma)
  return ma
}

/** Trộn các thuộc tính đã chọn thành danh sách phiên bản.
 *  Giữ nguyên SKU/mã vạch mà người dùng đã sửa tay (khớp theo khóa cấu hình). */
const dungLaiMaTran = () => {
  const chieu = [
    form.mauSacList.length ? form.mauSacList.map((m) => ({ mauSac: m })) : [{ mauSac: '' }],
    form.cpuIds.length ? form.cpuIds.map((id) => ({ cpuId: id })) : [{ cpuId: '' }],
    form.ramIds.length ? form.ramIds.map((id) => ({ ramId: id })) : [{ ramId: '' }],
    form.oCungIds.length ? form.oCungIds.map((id) => ({ oCungId: id })) : [{ oCungId: '' }],
    form.gpuIds.length ? form.gpuIds.map((id) => ({ gpuId: id })) : [{ gpuId: '' }]
  ]

  let tohop = chieu.reduce((acc, dim) => acc.flatMap((a) => dim.map((d) => ({ ...a, ...d }))), [{}])
  let vuot = false
  if (tohop.length > TOI_DA_BIEN_THE) {
    tohop = tohop.slice(0, TOI_DA_BIEN_THE)
    vuot = true
  }

  const cu = new Map(bienTheRows.value.map((r) => [r.key, r]))
  const skuDaDung = new Set(bienTheChuan.value.map((v) => v.maSku).filter((s) => s && s !== '—'))
  // Nạp trước SKU của những dòng được GIỮ LẠI (khớp key với lần dựng ma trận trước, có thể
  // đã bị người dùng sửa tay) vào skuDaDung trước khi sinh SKU cho dòng mới — nếu không, một
  // dòng mới (vd thêm GPU thứ 2) có thể sinh trùng y hệt SKU của dòng giữ lại mà không bị
  // phát hiện, vì skuDaDung trước đây chỉ biết SKU đã có thật trong CSDL, không biết các
  // dòng đang giữ lại ngay trong chính lần dựng này.
  tohop.forEach((c) => {
    const sku = cu.get(khoaDong(c))?.maSku
    if (sku) skuDaDung.add(sku)
  })
  const barcodeDangDung = new Set(barcodeDaDung.value)

  bienTheRows.value = tohop.map((c, i) => {
    const key = khoaDong(c)
    const truoc = cu.get(key)
    const sku = truoc?.maSku || skuKhongTrung(sinhSku(c, i), skuDaDung)
    if (truoc?.barcode) barcodeDangDung.add(truoc.barcode)
    return {
      key,
      mauSac: c.mauSac,
      cpuId: c.cpuId,
      ramId: c.ramId,
      oCungId: c.oCungId,
      gpuId: c.gpuId,
      maSku: sku,
      barcode: truoc?.barcode || sinhBarcode(barcodeDangDung)
    }
  })

  if (vuot) hienToast(`Chỉ giữ ${TOI_DA_BIEN_THE} phiên bản đầu — bớt bớt thuộc tính lại nhé`)
}

const xoaDong = (key) => { bienTheRows.value = bienTheRows.value.filter((r) => r.key !== key) }

/* Danh sách phiên bản tự dựng lại mỗi khi thuộc tính đổi — không cần bấm nút nào */
watch(
  () => [form.mauSacList.slice(), form.cpuIds.slice(), form.ramIds.slice(), form.oCungIds.slice(), form.gpuIds.slice()],
  () => { if (showModal.value && modalMode.value !== 'edit') dungLaiMaTran() },
  { deep: true }
)

/* ─── Mở / đóng modal ─── */
const resetForm = (patch = {}) => {
  Object.assign(form, formRong(), patch)
  Object.keys(errors).forEach((k) => delete errors[k])
  Object.keys(chon).forEach((k) => (chon[k] = ''))
  bienTheRows.value = []
  saveError.value = ''
  tab.value = 'info'
  if (moTaEl.value) moTaEl.value.innerHTML = form.moTa || ''
}

/** Phần thông tin sản phẩm chính dùng chung cho sửa / sao chép / thêm phiên bản */
const duLieuSanPham = (g) => ({
  tenSanPham: g.tenSanPham,
  thuongHieuId: g.thuongHieuId ?? '',
  danhMucId: g.danhMucId ?? '',
  nhaCungCapId: g.nhaCungCapId ?? '',
  loaiSanPham: g.loaiSanPham || 'LAPTOP',
  trangThaiSanPham: g.trangThai === 'inactive' ? 'inactive' : 'active',
  moTa: g.moTa || '',
  hinhAnhList: g.hinhAnh && g.hinhAnh !== ANH_MAC_DINH ? [g.hinhAnh] : [],
  phanLoaiIds: idPhanLoaiTuMa(g.phanLoai)
})

/** Thông số chung lấy theo một phiên bản cụ thể */
const duLieuThongSo = (v = {}) => ({
  baoHanhThang: v.baoHanhThang ?? 24,
  kichThuocManHinh: v.kichThuocManHinh || '',
  heDieuHanh: v.heDieuHanh || 'Windows 11 Home',
  pin: v.pin || '',
  trongLuongKg: v.trongLuongKg ?? ''
})

const openCreate = () => {
  moLaiChiTiet.value = null
  resetForm({ maSanPham: sinhMaSanPham() })
  modalMode.value = 'create'
  showDetail.value = false
  showModal.value = true
  dungLaiMaTran()
}

/* ─── Ảnh chụp form để so sánh trước/sau khi sửa (dựng ra tab Lịch sử thay đổi) ─── */
let banGoc = null
const anhChupForm = () => ({
  'Tên sản phẩm': form.tenSanPham,
  'Thương hiệu': tra(mapThuongHieu.value, form.thuongHieuId),
  'Danh mục': tra(mapDanhMuc.value, form.danhMucId),
  'Nhà cung cấp': tra(mapNhaCungCap.value, form.nhaCungCapId),
  'Loại sản phẩm': nhanLoaiSanPham(form.loaiSanPham),
  'Trạng thái': nhanTrangThai(form.trangThaiSanPham),
  'Phân loại': form.phanLoaiTen,
  'Ảnh chính': form.hinhAnhList[0] || '',
  'Mô tả': chuThuong(form.moTa).slice(0, 60),
  'Mã SKU': form.maSku,
  'Mã vạch': form.barcode,
  'Màu sắc': form.mauSac,
  CPU: tra(mapCpu.value, form.cpuId),
  RAM: tra(mapRam.value, form.ramId),
  'Ổ cứng': tra(mapOCung.value, form.oCungId),
  GPU: tra(mapGpu.value, form.gpuId),
  'Màn hình': form.kichThuocManHinh,
  'Hệ điều hành': form.heDieuHanh,
  Pin: form.pin,
  'Trọng lượng (kg)': form.trongLuongKg,
  'Bảo hành (tháng)': form.baoHanhThang,
  'Giá nhập': formatNumber(form.giaNhap),
  'Giá bán': formatNumber(form.giaBan)
})
const soSanhAnhChup = (cu, moi) =>
  Object.keys(moi)
    .filter((k) => String(cu?.[k] ?? '') !== String(moi[k] ?? ''))
    .map((k) => ({ truong: k, cu: cu?.[k], moi: moi[k] }))

const openEdit = (g, v) => {
  if (!g || !v) return
  moLaiChiTiet.value = g.sanPhamId
  resetForm({
    sanPhamId: g.sanPhamId,
    bienTheId: v.bienTheId,
    maSanPham: g.maSanPham,
    ...duLieuSanPham(g),
    ...duLieuThongSo(v),
    maSku: v.maSku === '—' ? '' : v.maSku,
    barcode: v.barcode || '',
    mauSac: v.mauSac || '',
    cpuId: v.cpuId ?? '',
    ramId: v.ramId ?? '',
    oCungId: v.oCungId ?? '',
    gpuId: v.gpuId ?? '',
    hinhAnhBienThe: v.hinhAnh || '',
    giaNhap: v.giaVon,
    giaBan: v.giaBan
  })
  modalMode.value = 'edit'
  banGoc = anhChupForm()
  showDetail.value = false
  showModal.value = true
}

const suaSanPham = (g) => openEdit(g, g.variants[0])
const suaBienThe = (v) => openEdit(chiTiet.value, v)

const themPhienBan = (g) => {
  if (!g) return
  moLaiChiTiet.value = g.sanPhamId
  resetForm({
    sanPhamId: g.sanPhamId,
    maSanPham: g.maSanPham,
    ...duLieuSanPham(g),
    ...duLieuThongSo(g.variants[0])
  })
  modalMode.value = 'variant'
  tab.value = 'bienthe'
  showDetail.value = false
  showModal.value = true
  dungLaiMaTran()
}

/** Sao chép NGAY 1 phiên bản — nhân bản toàn bộ dữ liệu (giá, cấu hình, thông số...),
 *  chỉ tự sinh lại SKU + mã vạch (2 trường bắt buộc duy nhất toàn hệ thống, giữ nguyên
 *  sẽ trùng bản gốc). Tạo thẳng qua API, không mở form ma trận — khác "Thêm phiên bản"
 *  (phải chọn cấu hình MỚI nên vẫn cần qua form Bước 2). Xong là thấy ngay trong danh
 *  sách Biến thể, không cần bấm Lưu ở đâu nữa. */
const saoChepBienThe = async (v) => {
  const g = chiTiet.value
  if (!g || !v || dangSaoChepBienThe.value) return
  dangSaoChepBienThe.value = true
  try {
    const skuDaDung = new Set(bienTheChuan.value.map((x) => x.maSku).filter((s) => s && s !== '—'))
    const maSku = skuKhongTrung(v.maSku, skuDaDung)
    const barcode = v.barcode ? sinhBarcode(new Set(barcodeDaDung.value)) : null
    const body = {
      sanPhamId: g.sanPhamId,
      maSku,
      barcode,
      giaNhap: v.giaVon,
      giaBan: v.giaBan,
      baoHanhThang: v.baoHanhThang,
      hinhAnhBienThe: v.hinhAnh || null,
      trangThai: v.trangThai,
      mauSac: v.mauSac || null,
      cpuId: v.cpuId,
      ramId: v.ramId,
      oCungId: v.oCungId,
      gpuId: v.gpuId,
      kichThuocManHinh: v.kichThuocManHinh || null,
      heDieuHanh: v.heDieuHanh || null,
      pin: v.pin || null,
      trongLuongKg: v.trongLuongKg
    }
    const res = await apiTaoBienThe(body)
    if (!res.ok) {
      hienToast(`Sao chép thất bại: ${await res.text().catch(() => res.statusText)}`)
      return
    }
    await fetchData()
    lamMoiKhoDuLieuChung().catch(() => {})
    lamMoiTonKhoDuLieuChung().catch(() => {})
    // chiTiet tự cập nhật theo groups (computed) — chỉ cần trỏ lại dòng đang chọn sang
    // phiên bản vừa tạo để người dùng thấy ngay kết quả.
    const updated = groups.value.find((x) => String(x.sanPhamId) === String(g.sanPhamId))
    const moi = updated?.variants.find((x) => x.maSku === maSku)
    if (moi) bienTheChonId.value = moi.bienTheId
    hienToast(`Đã tạo phiên bản mới ${maSku}`)
  } catch (e) {
    hienToast(`Sao chép thất bại: ${e.message}`)
  } finally {
    dangSaoChepBienThe.value = false
  }
}

/** Sao chép cả sản phẩm: tạo sản phẩm mới với đủ thuộc tính của các phiên bản cũ */
const saoChepSanPham = (g) => {
  if (!g) return
  const v0 = g.variants[0] || {}
  moLaiChiTiet.value = null
  resetForm({
    maSanPham: sinhMaSanPham(),
    ...duLieuSanPham(g),
    ...duLieuThongSo(v0),
    tenSanPham: `${g.tenSanPham} (bản sao)`,
    mauSacList: [...new Set(g.variants.map((v) => v.mauSac).filter(Boolean))],
    cpuIds: [...new Set(g.variants.map((v) => v.cpuId).filter(Boolean))],
    ramIds: [...new Set(g.variants.map((v) => v.ramId).filter(Boolean))],
    oCungIds: [...new Set(g.variants.map((v) => v.oCungId).filter(Boolean))],
    gpuIds: [...new Set(g.variants.map((v) => v.gpuId).filter(Boolean))]
  })
  modalMode.value = 'create'
  showDetail.value = false
  showModal.value = true
  dungLaiMaTran()
  hienToast('Đã sao chép — đổi tên sản phẩm rồi bấm Lưu')
}

const closeModal = () => {
  showModal.value = false
  resetForm()
}

/* ─── Form đã đủ điều kiện lưu chưa (chỉ soi điều kiện, không set lỗi) ─── */
const formHopLe = computed(() => {
  const coCoBan = !!(form.tenSanPham && form.maSanPham && form.thuongHieuId && form.danhMucId)
  const coBaoHanh = Number(form.baoHanhThang) >= 0
  const coBienThe = bienTheRows.value.length > 0 && bienTheRows.value.every((r) => r.maSku)

  if (modalMode.value === 'edit') {
    const nhap = Number(form.giaNhap)
    const ban = Number(form.giaBan)
    return coCoBan && coBaoHanh && !!form.maSku && nhap >= 0 && ban >= 0 && ban >= nhap * 0.5
  }
  if (modalMode.value === 'variant') return coBaoHanh && coBienThe
  return coCoBan && coBaoHanh && coBienThe
})

/* ─── Kiểm tra trước khi gửi ─── */
const laMaVachHopLe = (ma) => /^\d{8,13}$/.test(ma)

const validate = () => {
  Object.keys(errors).forEach((k) => delete errors[k])
  const laVariant = modalMode.value === 'variant'
  const khacBienThe = (v) => String(v.bienTheId) !== String(form.bienTheId)
  const skuDaCo = new Set(bienTheChuan.value.filter(khacBienThe).map((v) => v.maSku))
  const barcodeDaCo = new Set(bienTheChuan.value.filter(khacBienThe).map((v) => v.barcode).filter(Boolean))

  if (!laVariant) {
    if (!form.tenSanPham) errors.tenSanPham = 'Nhập tên sản phẩm'
    if (!form.thuongHieuId) errors.thuongHieuId = 'Chọn thương hiệu'
    if (!form.danhMucId) errors.danhMucId = 'Chọn danh mục'
  }
  if (!(Number(form.baoHanhThang) >= 0)) errors.baoHanhThang = 'Số tháng bảo hành không hợp lệ'

  if (modalMode.value === 'edit') {
    const nhap = Number(form.giaNhap)
    const ban = Number(form.giaBan)
    if (!(nhap >= 0)) errors.giaNhap = 'Giá nhập không hợp lệ'
    if (!(ban >= 0)) errors.giaBan = 'Giá bán không hợp lệ'
    else if (ban < nhap * 0.5) errors.giaBan = 'Giá bán phải ≥ 50% giá nhập (ràng buộc của CSDL)'

    if (!form.maSku) errors.maSku = 'Nhập mã SKU'
    else if (skuDaCo.has(form.maSku)) errors.maSku = 'SKU này đã tồn tại'

    if (form.barcode && !laMaVachHopLe(form.barcode)) errors.barcode = 'Mã vạch chỉ gồm 8–13 chữ số'
    else if (form.barcode && barcodeDaCo.has(form.barcode)) errors.barcode = 'Mã vạch này đã có phiên bản khác dùng'
  } else if (!bienTheRows.value.length) {
    errors.bienThe = 'Chưa có phiên bản nào để lưu'
  } else {
    const skuTrong = bienTheRows.value.filter((r) => !r.maSku || skuDaCo.has(r.maSku))
    const trungTrongForm = bienTheRows.value.length !== new Set(bienTheRows.value.map((r) => r.maSku)).size
    const maVachXau = bienTheRows.value.find((r) => r.barcode && !laMaVachHopLe(r.barcode))
    const maVachTrung = bienTheRows.value.find((r) => r.barcode && barcodeDaCo.has(r.barcode))
    const maVachTrungForm = (() => {
      const ds = bienTheRows.value.map((r) => r.barcode).filter(Boolean)
      return ds.length !== new Set(ds).size
    })()

    if (skuTrong.length) errors.bienThe = `SKU trống hoặc đã tồn tại: ${skuTrong.map((r) => r.maSku || '(trống)').join(', ')}`
    else if (trungTrongForm) errors.bienThe = 'Có hai phiên bản trùng mã SKU'
    else if (maVachXau) errors.bienThe = `Phiên bản ${maVachXau.maSku}: mã vạch phải gồm 8–13 chữ số`
    else if (maVachTrung) errors.bienThe = `Phiên bản ${maVachTrung.maSku}: mã vạch đã có phiên bản khác dùng`
    else if (maVachTrungForm) errors.bienThe = 'Có hai phiên bản trùng mã vạch'
  }

  return Object.keys(errors).length === 0
}

/* ════════════════════════════════════════════════════════════
 *                        PAYLOAD
 * SanPhamController.create() nhận SanPhamRequest và tạo LUÔN cả SanPham lẫn
 * BienTheSanPham đầu tiên trong một request (xem SanPhamService.createSanPham:
 * BeanUtils copy chung request sang cả hai entity). Vì vậy POST /api/san-pham
 * bắt buộc phải kèm maSku, giaNhap, giaBan — thiếu là ma_sku/gia_nhap/gia_ban
 * nhận NULL và cả giao dịch bị rollback.
 * Các phiên bản còn lại đi bằng POST /api/bien-the-san-pham với sanPhamId.
 * LƯU Ý: barcode nay là cột của bien_the_san_pham → gửi kèm theo từng phiên bản,
 * không còn trường barcode ở cấp sản phẩm nữa.
 * ══════════════════════════════════════════════════════════ */

/** Thời điểm hoàn tất thao tác, dạng ISO giờ địa phương (không lệch UTC). */
const bayGio = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

/** Phần dùng chung cho mọi phiên bản (thông số + trạng thái + phân loại). */
const phanChungBienThe = () => ({
  baoHanhThang: Number(form.baoHanhThang || 0),
  kichThuocManHinh: form.kichThuocManHinh || null,
  heDieuHanh: form.heDieuHanh || null,
  pin: form.pin || null,
  trongLuongKg: soHoacNull(form.trongLuongKg),
  phanLoaiTags: form.phanLoaiTags || null,
  phanLoaiTen: form.phanLoaiTen || null,
  trangThai: form.trangThaiSanPham
})

/** Body cho POST/PUT /api/san-pham — gộp sản phẩm chính + một phiên bản. */
const payloadSanPham = (row) => ({
  ...(form.sanPhamId ? { sanPhamId: Number(form.sanPhamId) } : {}),
  maSanPham: form.maSanPham || null,
  tenSanPham: form.tenSanPham,
  thuongHieuId: soHoacNull(form.thuongHieuId),
  danhMucId: soHoacNull(form.danhMucId),
  nhaCungCapId: soHoacNull(form.nhaCungCapId),
  loaiSanPham: form.loaiSanPham,
  moTa: form.moTa || null,
  hinhAnhChinh: form.hinhAnhList[0] || null,
  hinhAnhList: form.hinhAnhList.length ? form.hinhAnhList : null,
  ngayTao: bayGio(),
  ...phanChungBienThe(),
  ...(row
    ? {
        ...(row.bienTheId ? { bienTheId: Number(row.bienTheId) } : {}),
        maSku: row.maSku,
        // gửi cả hai tên trường để khớp dù DTO backend đặt tên nào
        barcode: row.barcode || null,
        barcodeBienThe: row.barcode || null,
        giaNhap: Number(row.giaNhap || 0),
        giaBan: Number(row.giaBan || 0),
        mauSac: row.mauSac || null,
        cpuId: soHoacNull(row.cpuId),
        ramId: soHoacNull(row.ramId),
        oCungId: soHoacNull(row.oCungId),
        gpuId: soHoacNull(row.gpuId),
        hinhAnhBienThe: row.hinhAnhBienThe || form.hinhAnhBienThe || null
      }
    : {})
})

/** Body cho POST /api/bien-the-san-pham — các phiên bản thứ 2 trở đi. */
const payloadBienThe = (sanPhamId, row) => ({
  sanPhamId: soHoacNull(sanPhamId),
  maSku: row.maSku,
  barcode: row.barcode || null,
  giaNhap: Number(row.giaNhap || 0),
  giaBan: Number(row.giaBan || 0),
  mauSac: row.mauSac || null,
  cpuId: soHoacNull(row.cpuId),
  ramId: soHoacNull(row.ramId),
  oCungId: soHoacNull(row.oCungId),
  gpuId: soHoacNull(row.gpuId),
  hinhAnhBienThe: row.hinhAnhBienThe || form.hinhAnhBienThe || null,
  ...phanChungBienThe()
})

const layId = (res, key) => res?.[key] ?? res?.id ?? res?.data?.[key] ?? res?.data?.id ?? null

/** Controller trả entity SanPham có quan hệ LAZY — Jackson có thể vỡ khi ghi body,
 *  lúc đó frontend không đọc được id dù bản ghi đã lưu xong. Tìm lại bằng chính API
 *  danh sách (nó lọc theo maSanPham / tenSanPham). */
const timIdVuaTao = async () => {
  for (const kw of [form.maSanPham, form.tenSanPham]) {
    if (!kw) continue
    try {
      const rows = toArray(await sanPhamApi.getPage({ page: 0, size: 50, keyword: kw }))
      const khop =
        rows.find((r) => r.maSanPham && r.maSanPham === form.maSanPham) ||
        rows.find((r) => r.tenSanPham === form.tenSanPham)
      if (khop) return idOf(khop, 'sanPhamId')
    } catch (e) {
      console.warn('[Hàng hóa] không tra lại được sanPhamId:', e)
    }
  }
  return null
}

/* ─── Đọc lỗi từ backend cho ra tiếng người ─── */
const thongBaoLoi = (e) => {
  const res = e?.response
  const d = res?.data
  const chiTietLoi =
    (typeof d === 'string' && d) ||
    d?.message || d?.error || d?.detail ||
    (Array.isArray(d?.errors) ? d.errors.map((x) => x.defaultMessage || x.message).join('; ') : '') ||
    e?.message || 'Không rõ nguyên nhân'
  return (res?.status ? `HTTP ${res.status} — ` : '') + chiTietLoi
}

/** Dịch lỗi SQL/JPA hay gặp thành việc cần làm. */
const goiYSua = (msg) => {
  const m = khongDau(msg)
  if (m.includes('401') || m.includes('403') || m.includes('unauthorized') || m.includes('denied'))
    return 'API tạo sản phẩm yêu cầu quyền ADMIN / NHAN_VIEN / QUAN_KHO — đăng nhập lại bằng tài khoản nhân viên.'
  if (m.includes('ma_sku')) return 'Mã SKU trống hoặc trùng — mỗi phiên bản phải có SKU riêng.'
  if (m.includes('barcode')) return 'Mã vạch trùng với phiên bản khác (cột bien_the_san_pham.barcode là duy nhất).'
  if (m.includes('gia_nhap') || m.includes('gia_ban')) return 'Giá nhập/giá bán chưa được gửi lên hoặc âm.'
  if (m.includes('ck_bt_giaban_hop_ly')) return 'Giá bán phải ≥ 50% giá nhập.'
  if (m.includes('ck_sp_loaisanpham')) return 'Loại sản phẩm chỉ nhận LAPTOP, PHU_KIEN, DIEN_THOAI.'
  if (m.includes('trangthai')) return 'Trạng thái biến thể chỉ nhận active hoặc inactive.'
  if (m.includes('unique') || m.includes('duplicate')) return 'Mã sản phẩm, mã vạch hoặc SKU bị trùng với bản ghi đã có.'
  if (m.includes('lazy') || m.includes('proxy') || m.includes('bytebuddy'))
    return 'Controller đang trả entity có quan hệ LAZY nên Jackson vỡ khi ghi body — cho create() trả về DTO thay vì entity.'
  return ''
}

/** Bảng nối san_pham_phan_loai được ghi bằng một lệnh riêng (SanPhamRequest không có
 *  trường phanLoaiIds). Lỗi ở bước này không làm hỏng việc lưu sản phẩm — chỉ ghi log. */
const luuPhanLoai = async (sanPhamId) => {
  if (!sanPhamId) return
  try {
    await put(`/api/phan-loai/san-pham/${sanPhamId}`, form.phanLoaiIds.map(Number))
  } catch (e) {
    console.warn('[Hàng hóa] không lưu được phân loại:', e?.response?.data ?? e)
  }
}

/* ─── Lưu ───
 * Tạo mới:  POST /api/san-pham (sản phẩm + phiên bản #1)  →  lấy sanPhamId
 *           →  POST /api/bien-the-san-pham cho phiên bản #2..n
 * Sửa:      PUT  /api/san-pham/update/{id} kèm bienTheId — service tự cập nhật cả hai
 * Thêm phiên bản: chỉ POST /api/bien-the-san-pham
 */
const submitForm = async () => {
  saveError.value = ''
  if (!validate()) {
    saveError.value = 'Vui lòng sửa các ô được đánh dấu.'
    tab.value = errors.bienThe || errors.maSku || errors.barcode || errors.giaBan || errors.giaNhap ? 'bienthe' : 'info'
    return
  }

  isSaving.value = true
  let buoc = 'chuẩn bị dữ liệu'
  let daTao = 0

  try {
    if (modalMode.value === 'edit') {
      buoc = 'cập nhật sản phẩm'
      await apiSuaSanPham(form.sanPhamId, payloadSanPham({
        bienTheId: form.bienTheId,
        maSku: form.maSku,
        barcode: form.barcode,
        mauSac: form.mauSac,
        cpuId: form.cpuId,
        ramId: form.ramId,
        oCungId: form.oCungId,
        gpuId: form.gpuId,
        giaNhap: form.giaNhap,
        giaBan: form.giaBan,
        hinhAnhBienThe: form.hinhAnhBienThe
      }))
      await luuPhanLoai(form.sanPhamId)

      const thayDoi = soSanhAnhChup(banGoc, anhChupForm())
      ghiNhatKy(form.sanPhamId, {
        loai: 'sua',
        hanhDong: thayDoi.length ? 'Cập nhật sản phẩm' : 'Lưu lại (không đổi nội dung)',
        doiTuong: `Phiên bản ${form.maSku}`,
        thayDoi
      })
      hienToast('Đã lưu thay đổi')
      closeModal()
    } else if (modalMode.value === 'variant') {
      const dsSku = []
      for (const row of bienTheRows.value) {
        buoc = `thêm phiên bản ${row.maSku}`
        const resBt = await apiTaoBienThe(payloadBienThe(form.sanPhamId, row))
        if (!resBt.ok) {
          throw new Error(`HTTP ${resBt.status}: ${await resBt.text().catch(() => resBt.statusText)}`)
        }
        dsSku.push(row.maSku)
        daTao++
      }
      ghiNhatKy(form.sanPhamId, {
        loai: 'them',
        hanhDong: `Thêm ${daTao} phiên bản`,
        doiTuong: dsSku.join(', ')
      })
      hienToast(`Đã thêm ${daTao} phiên bản`)
      closeModal()
    } else {
      const [dauTien, ...conLai] = bienTheRows.value

      buoc = 'tạo sản phẩm chính'
      const spMoi = await apiTaoSanPham(payloadSanPham(dauTien))
      daTao = 1

      let spId = layId(spMoi, 'sanPhamId')
      if (!spId && (conLai.length || form.phanLoaiIds.length)) {
        buoc = 'tra lại mã sản phẩm vừa tạo'
        spId = await timIdVuaTao()
        if (!spId && conLai.length) {
          throw new Error(
            'Sản phẩm và phiên bản đầu tiên đã lưu, nhưng không lấy được sanPhamId nên các phiên bản ' +
            'còn lại chưa tạo được. Mở lại sản phẩm rồi dùng nút “Thêm phiên bản” để bổ sung, ' +
            'hoặc sửa SanPhamController.create() cho trả về DTO thay vì entity.'
          )
        }
      }

      for (const row of conLai) {
        buoc = `tạo phiên bản ${row.maSku}`
        const resBt = await apiTaoBienThe(payloadBienThe(spId, row))
        if (!resBt.ok) {
          throw new Error(`HTTP ${resBt.status}: ${await resBt.text().catch(() => resBt.statusText)}`)
        }
        daTao++
      }

      await luuPhanLoai(spId)
      ghiNhatKy(spId, {
        loai: 'tao',
        hanhDong: 'Tạo sản phẩm mới',
        doiTuong: `${form.maSanPham} · ${daTao} phiên bản`,
        thayDoi: [{ truong: 'Tên sản phẩm', cu: '', moi: form.tenSanPham }]
      })
      // Không set moLaiChiTiet ở đây — tạo MỚI xong thì đóng về danh sách bình thường,
      // không tự mở lại cửa sổ chi tiết. openCreate()/saoChepSanPham() đã đặt sẵn giá trị
      // null cho đúng 2 luồng "tạo sản phẩm mới" này; dòng cũ ở đây từng ghi đè lại bằng
      // spId khiến sản phẩm vừa tạo luôn tự bật chi tiết, đúng thứ người dùng không muốn.
      hienToast(`Đã lưu sản phẩm cùng ${daTao} phiên bản`)
      closeModal()
    }

    await fetchData()
    // HangHoa.vue tự tải dữ liệu riêng (fetchData ở trên), KHÔNG đọc từ ProductsStore dùng
    // chung — nhưng tab "Biến thể" (BienTheTable.vue) và các nơi khác lại đọc từ đó. Không
    // làm mới ProductsStore ở đây thì các màn kia vẫn thấy dữ liệu cũ cho tới khi F5 (module
    // JS reset lại từ đầu). Làm mới song song, lỗi ở đây không nên chặn luồng lưu chính.
    lamMoiKhoDuLieuChung().catch(() => {})

    // Biến thể mới tạo phải hiện ngay ở "Hàng sắp về" bên Kho hàng — InventoryStore cũng là
    // dữ liệu dùng chung riêng biệt, tách rời fetchData()/ProductsStore ở trên.
    lamMoiTonKhoDuLieuChung().catch(() => {})

    // Mở lại cửa sổ chi tiết để xem ngay kết quả vừa lưu
    if (moLaiChiTiet.value) {
      const g = groups.value.find((x) => String(x.sanPhamId) === String(moLaiChiTiet.value))
      if (g) {
        moChiTiet(g)
        tabCT.value = 'lichsu'
      }
      moLaiChiTiet.value = null
    }
  } catch (e) {
    console.error(`[Hàng hóa] lỗi ở bước "${buoc}":`, e?.response?.data ?? e)
    const chiTietLoi = thongBaoLoi(e)
    const goiY = goiYSua(chiTietLoi)
    saveError.value =
      `Lưu thất bại ở bước ${buoc}: ${chiTietLoi}` +
      (goiY ? ` → ${goiY}` : '') +
      (daTao ? ` (đã lưu được ${daTao} bản ghi trước đó)` : '')
    if (buoc.includes('phiên bản')) tab.value = 'bienthe'
    await fetchData()
    if (daTao) { lamMoiKhoDuLieuChung().catch(() => {}); lamMoiTonKhoDuLieuChung().catch(() => {}) }
  } finally {
    isSaving.value = false
  }
}
</script>

<style scoped>
/* ═══════════ BẢNG MÀU (tông hồng) ═══════════ */
.hh, .hh-modal-mask, .hh-toast {
  --pink-50:  #fff5f9;
  --pink-100: #ffe6f0;
  --pink-200: #ffcfe1;
  --pink-300: #f7a8c8;
  --pink-500: #ec4899;
  --pink-600: #db2777;
  --pink-700: #a81b5d;

  --ink:     #1f2937;
  --ink-2:   #374151;
  --muted:   #6b7280;
  --line:    #f1dbe6;
  --line-2:  #ead0dd;
  --field:   #d9b3c6;
  --danger:  #dc2626;
  --ok-bg:   #ecfdf5;
  --ok-text: #047857;

  --sh-1: 0 1px 2px rgba(168, 27, 93, .06);
  --sh-2: 0 4px 14px rgba(168, 27, 93, .10);
  --sh-3: 0 22px 55px rgba(168, 27, 93, .25);
}
.hh { font-size: 14px; color: var(--ink); }

.ta-r { text-align: right; }
.ta-c { text-align: center; }
.hh-muted { color: var(--muted); }
.hh-hidden { display: none; }
.hh-mt6 { margin-top: 6px; }
.hh-mb8 { margin-bottom: 8px; }

/* ═══════════ NÚT ═══════════ */
.hh-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 14px; border-radius: 999px;
  border: 1px solid transparent;
  font-size: 13px; font-weight: 600; font-family: inherit;
  cursor: pointer; white-space: nowrap;
  transition: background-color .15s, border-color .15s, color .15s, box-shadow .15s;
}
.hh-btn--sm { padding: 5px 11px; font-size: 12.5px; }
.hh-btn--primary { background: var(--pink-600); color: #fff; box-shadow: var(--sh-1); }
.hh-btn--primary:hover:not(:disabled) { background: var(--pink-700); box-shadow: var(--sh-2); }
.hh-btn--soft { background: var(--pink-100); color: var(--pink-700); border-color: var(--pink-200); }
.hh-btn--soft:hover:not(:disabled) { background: var(--pink-200); }
.hh-btn--ghost { background: #fff; color: var(--pink-700); border-color: var(--pink-200); }
.hh-btn--ghost:hover:not(:disabled) { background: var(--pink-50); border-color: var(--pink-300); }
.hh-btn--ghost.is-on { background: var(--pink-100); border-color: var(--pink-300); }
.hh-btn:disabled { opacity: .45; cursor: not-allowed; }
.hh-btn:focus-visible, .hh-icon-btn:focus-visible { outline: 2px solid var(--pink-500); outline-offset: 2px; }

.hh-icon-btn {
  background: transparent; border: none; color: var(--muted);
  width: 32px; height: 32px; border-radius: 50%; cursor: pointer;
  display: inline-grid; place-items: center;
}
.hh-icon-btn:hover:not(:disabled) { background: var(--pink-50); color: var(--pink-600); }
.hh-icon-btn:disabled { opacity: .35; cursor: not-allowed; }

.hh-link { background: none; border: none; padding: 0 0 0 6px; color: var(--pink-700); text-decoration: underline; cursor: pointer; }

.hh-chip {
  background: var(--pink-600); color: #fff; border-radius: 999px;
  padding: 0 6px; font-size: 11px; line-height: 17px; min-width: 17px; text-align: center;
}
.hh-caret { font-size: 10px; transition: transform .2s; }
.hh-caret.is-open { transform: rotate(180deg); }

/* ═══════════ THANH CÔNG CỤ (dinh sticky, tu an khi cuon xuong) ═══════════ */
.hh-sticky-head {
  position: sticky; top: 0; z-index: 5;
  transition: transform .25s ease;
}
.hh-sticky-head.is-hidden { transform: translateY(-100%); }

.hh-bar {
  display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
  background: #fff; border: 1px solid var(--line); border-radius: 14px;
  padding: 12px 16px; margin-bottom: 12px; box-shadow: var(--sh-1);
}
.hh-bar__left { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.hh-bar__actions { display: flex; align-items: center; gap: 8px; margin-left: auto; flex-wrap: wrap; }
.hh-title { margin: 0; font-size: 20px; font-weight: 800; letter-spacing: -.2px; color: var(--pink-700); white-space: nowrap; }

.hh-search { position: relative; width: 320px; max-width: 100%; }
.hh-search input {
  width: 100%; padding: 8px 32px 8px 34px;
  border: 1px solid var(--pink-200); border-radius: 999px;
  font-size: 13px; background: var(--pink-50); font-family: inherit; color: var(--ink);
}
.hh-search input:focus { outline: none; border-color: var(--pink-500); background: #fff; box-shadow: 0 0 0 3px var(--pink-100); }
.hh-search__icon { position: absolute; left: 13px; top: 50%; transform: translateY(-50%); color: var(--pink-500); }
.hh-search__clear { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); background: none; border: none; color: var(--muted); cursor: pointer; }

/* ═══════════ BỘ LỌC ═══════════ */
.hh-filter { display: grid; grid-template-rows: 0fr; transition: grid-template-rows .25s ease, margin-bottom .25s ease; margin-bottom: 0; }
.hh-filter.is-open { grid-template-rows: 1fr; margin-bottom: 12px; }
.hh-filter__panel {
  overflow: hidden; background: #fff; border: 1px solid var(--line);
  border-radius: 14px; padding: 0 16px; transition: padding .25s ease; box-shadow: var(--sh-1);
}
.hh-filter.is-open .hh-filter__panel { padding: 16px; }
.hh-filter__grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 12px; }
.hh-filter__foot {
  display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap;
  margin-top: 14px; padding-top: 12px; border-top: 1px dashed var(--line);
}
.hh-filter__count { font-size: 12.5px; color: var(--muted); }
.hh-filter__btns { display: flex; gap: 8px; }

/* ═══════════ Ô NHẬP ═══════════ */
.hh-field { display: flex; flex-direction: column; gap: 5px; min-width: 0; }
.hh-field > span { font-size: 12px; font-weight: 700; color: var(--pink-700); letter-spacing: .1px; }
.hh-field > span b { color: var(--danger); }
.hh-field input,
.hh-field select,
.hh-field textarea,
.hh-cell {
  width: 100%; padding: 9px 11px;
  border: 1px solid var(--field); border-radius: 9px;
  font-size: 13px; color: var(--ink); background: #fff; font-family: inherit;
  transition: border-color .15s, box-shadow .15s;
}
.hh-field input::placeholder, .hh-cell::placeholder { color: #b9a3ae; }
.hh-field input:hover, .hh-field select:hover, .hh-cell:hover { border-color: var(--pink-300); }
.hh-field input:focus, .hh-field select:focus, .hh-field textarea:focus, .hh-cell:focus {
  outline: none; border-color: var(--pink-500); box-shadow: 0 0 0 3px var(--pink-100);
}
.hh-field input:disabled, .hh-field select:disabled { background: #f8f6f7; color: var(--muted); }
.hh-combo { background: var(--pink-50); }
.hh-inline { display: flex; gap: 6px; align-items: center; }
.hh-inline > select, .hh-inline > input { flex: 1; min-width: 0; }
.hh-err { font-size: 11.5px; color: var(--danger); font-style: normal; }
.hh-hint { font-size: 11.5px; color: var(--muted); font-style: normal; line-height: 1.45; }

/* thẻ tag có nút xóa */
.hh-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 4px; }
.hh-tag-pill {
  display: inline-flex; align-items: center; gap: 6px; max-width: 100%;
  background: var(--pink-100); color: var(--pink-700);
  border: 1px solid var(--pink-200); border-radius: 999px;
  padding: 3px 6px 3px 11px; font-size: 12.5px; font-weight: 600;
  word-break: break-word;
}
.hh-tag-pill button {
  background: var(--pink-200); border: none; color: var(--pink-700);
  width: 17px; height: 17px; border-radius: 50%; line-height: 1; flex-shrink: 0;
  font-size: 13px; cursor: pointer; display: grid; place-items: center;
}
.hh-tag-pill button:hover { background: var(--pink-600); color: #fff; }

/* ═══════════ BẢNG DANH SÁCH ═══════════ */
.hh-card { background: #fff; border: 1px solid var(--line); border-radius: 14px; overflow: hidden; box-shadow: var(--sh-1); }
.hh-table-wrap { position: relative; overflow-x: auto; min-height: 140px; }
.hh-table { width: 100%; border-collapse: collapse; table-layout: auto; }
.hh-table th {
  background: var(--pink-50); color: var(--pink-700);
  font-size: 11.5px; font-weight: 800; text-align: left; text-transform: uppercase; letter-spacing: .4px;
  padding: 11px 12px; white-space: nowrap; border-bottom: none;
}
/* border-collapse:collapse tren <table> khong duoc overflow:hidden cua .hh-card bo
   goc dung cach — nen o dau tien vuot goc tron, trong nhu 1 net ke de len vien card.
   Bo goc thang vao chinh o th dau/cuoi de nen hong di dung theo duong bo tron. */
.hh-table thead th:first-child { border-top-left-radius: 13px; }
.hh-table thead th:last-child { border-top-right-radius: 13px; }
.hh-table td { padding: 11px 12px; border-bottom: 1px solid var(--line); vertical-align: middle; white-space: nowrap; }
.hh-table tbody tr:last-child td { border-bottom: none; }

.hh-row { cursor: pointer; transition: background-color .12s; }
.hh-row:hover { background: var(--pink-50); }
.hh-row:focus-visible { outline: 2px solid var(--pink-500); outline-offset: -2px; }
.hh-row:hover .hh-col-go { color: var(--pink-600); }

.hh-col-ma { width: 130px; }
.hh-col-ten { min-width: 260px; }
.hh-col-go { width: 34px; text-align: center; color: var(--pink-200); }
.hh-td-ma { font-weight: 700; }
.hh-td-gia { font-variant-numeric: tabular-nums; font-weight: 600; }
.hh-td-ngay { font-size: 12.5px; }

/* tên dài thì xuống dòng, không phá khung */
.hh-td-ten { white-space: normal; max-width: 420px; }
.hh-code__main { color: var(--pink-700); font-weight: 700; letter-spacing: .3px; }

.hh-name { display: flex; align-items: center; gap: 10px; min-width: 0; }
.hh-name__text { min-width: 0; }
.hh-name__main { font-weight: 600; line-height: 1.35; word-break: break-word; }
.hh-name__sub { font-size: 11.5px; color: var(--muted); font-weight: 400; margin-top: 2px; }
.hh-thumb { width: 36px; height: 36px; object-fit: cover; border-radius: 9px; border: 1px solid var(--line); background: #fff; flex-shrink: 0; }

.hh-tag { display: inline-block; padding: 2px 9px; border-radius: 999px; font-size: 11.5px; font-weight: 700; white-space: nowrap; }
.hh-tag--ok { background: var(--ok-bg); color: var(--ok-text); }
.hh-tag--off { background: #f3f4f6; color: var(--muted); }
.hh-tag--soft { background: var(--pink-100); color: var(--pink-700); font-weight: 600; }
.hh-tag--outline { background: #fff; color: var(--pink-700); border: 1px solid var(--pink-200); font-weight: 600; }

.hh-ton { font-weight: 700; font-variant-numeric: tabular-nums; }
.hh-ton.is-het { color: var(--danger); }

/* ═══════════ RỖNG / LOADING / PHÂN TRANG ═══════════ */
.hh-overlay { position: absolute; inset: 0; background: rgba(255,255,255,.65); display: flex; align-items: center; justify-content: center; }
.hh-spinner { width: 26px; height: 26px; border-radius: 50%; border: 3px solid var(--pink-200); border-top-color: var(--pink-600); animation: hh-spin .7s linear infinite; }
@keyframes hh-spin { to { transform: rotate(360deg); } }

.hh-empty { padding: 44px 20px; text-align: center; color: var(--muted); }
.hh-empty i { font-size: 32px; color: var(--pink-300); }
.hh-empty p { margin: 12px 0; font-size: 13.5px; }

.hh-alert { margin: 0 0 12px; padding: 10px 14px; background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; font-size: 13px; border-radius: 9px; }

.hh-pager { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 16px; background: var(--pink-50); flex-wrap: wrap; border-top: 1px solid var(--line); }
.hh-pager__info { font-size: 12.5px; color: var(--muted); }
.hh-pager__nav { display: flex; align-items: center; gap: 8px; }
.hh-pager__page { font-size: 13px; font-weight: 700; min-width: 56px; text-align: center; }
.hh-pager__size { padding: 5px 8px; border: 1px solid var(--field); border-radius: 8px; font-size: 12.5px; background: #fff; color: var(--ink); }

/* ═══════════ MODAL ═══════════ */
/* align-items: flex-start (thay vi center) — mep tren cua modal dung im o 1 khoang
   co dinh cach dinh trang, doi tab (Thong tin/Phien ban/Mo ta) chi lam DAI/NGAN xuong
   duoi, khong bi tut/nhoi ca hop vao giua moi lan noi dung doi chieu cao.
   Khong overflow-y o day — de mask tu cuon se giu lai vi tri cuon cu khi chuyen sang
   tab NGAN hon (vd Mo ta), lam modal nhu bi "cuon mat"/lech khoi khung nhin. Noi dung
   dai da co .hh-modal__body tu cuon rieng, va .hh-modal da khoa max-height: 94vh. */
.hh-modal-mask {
  position: fixed; inset: 0; z-index: 1050;
  background: rgba(31,41,55,.5); display: flex; align-items: flex-start; justify-content: center;
  padding: 5vh 20px 20px;
  font-size: 14px; color: var(--ink);
}
.hh-modal {
  background: #fff; width: 1020px; max-width: 100%; max-height: 94vh;
  border-radius: 16px; display: flex; flex-direction: column; overflow: hidden;
  box-shadow: var(--sh-3);
}
.hh-modal--rong { width: 1100px; }
.hh-modal--hep { width: 620px; }
.hh-modal__head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 12px;
  padding: 16px 20px 12px; background: var(--pink-50); border-bottom: 1px solid var(--line);
}
.hh-head-main { min-width: 0; }
.hh-modal__head h2 { margin: 0; font-size: 17px; font-weight: 800; color: var(--pink-700); line-height: 1.35; word-break: break-word; }
.hh-modal__head p { margin: 6px 0 0; font-size: 12.5px; color: var(--muted); display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.hh-head-path { word-break: break-word; }

.hh-tabs { display: flex; gap: 4px; padding: 0 20px; background: var(--pink-50); border-bottom: 1px solid var(--line); overflow-x: auto; }
.hh-tab {
  background: none; border: none; border-bottom: 2px solid transparent;
  padding: 9px 14px; font-size: 13px; font-weight: 700; font-family: inherit;
  color: var(--muted); cursor: pointer; display: inline-flex; align-items: center; gap: 6px; white-space: nowrap;
}
.hh-tab:hover { color: var(--pink-600); }
.hh-tab.is-on { color: var(--pink-700); border-bottom-color: var(--pink-600); }

.hh-modal__body { padding: 20px; overflow-y: auto; background: #fffafc; }
.hh-pane { display: flex; flex-direction: column; gap: 16px; }

.hh-modal__foot {
  display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap;
  padding: 14px 20px; border-top: 1px solid var(--line); background: var(--pink-50);
}
.hh-modal__foot-left { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.hh-modal__foot-right { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.hh-foot-hint { font-size: 12.5px; color: var(--muted); margin-right: 4px; }
.hh-foot-hint b { color: var(--pink-700); }

/* ═══════════ MODAL CHI TIẾT ═══════════ */
.hh-ct-top { display: grid; grid-template-columns: 240px 1fr; gap: 20px; align-items: start; }
.hh-ct-media { display: flex; flex-direction: column; gap: 10px; }
.hh-ct-media__main {
  width: 100%; aspect-ratio: 1 / 1; object-fit: cover;
  border: 1px solid var(--line); border-radius: 14px; background: #fff;
}
.hh-ct-media__strip { display: flex; gap: 8px; flex-wrap: wrap; }
.hh-ct-media__thumb {
  width: 52px; height: 52px; padding: 0; overflow: hidden; cursor: pointer;
  border: 1px solid var(--line); border-radius: 10px; background: #fff;
}
.hh-ct-media__thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.hh-ct-media__thumb.is-on { border-color: var(--pink-500); box-shadow: 0 0 0 2px var(--pink-100); }

.hh-ct-main { min-width: 0; display: flex; flex-direction: column; gap: 14px; }
.hh-ct-tags { display: flex; flex-wrap: wrap; gap: 6px; }

.hh-ct-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 14px 20px; margin: 0; }
.hh-ct-item { min-width: 0; }
.hh-ct-item dt { font-size: 11.5px; font-weight: 700; color: var(--pink-700); margin-bottom: 3px; }
.hh-ct-item dd {
  margin: 0; padding-bottom: 5px; font-size: 13.5px; color: var(--ink-2);
  border-bottom: 1px solid var(--line); word-break: break-word;
}
.hh-ct-item__manh { color: var(--pink-600); font-weight: 700; }

.hh-ct-block { background: #fff; border: 1px solid var(--line); border-radius: 14px; padding: 16px; }
.hh-ct-block h3 {
  margin: 0 0 12px; font-size: 12.5px; font-weight: 800; color: var(--pink-700);
  text-transform: uppercase; letter-spacing: .5px;
}
.hh-ct-mota { font-size: 13.5px; line-height: 1.65; color: var(--ink-2); word-break: break-word; }
.hh-ct-mota :deep(img) { max-width: 100%; height: auto; border-radius: 8px; }

/* bảng biến thể trong chi tiết */
.hh-vt-wrap { border: 1px solid var(--line); border-radius: 12px; overflow: auto; background: #fff; }
.hh-vt { width: 100%; border-collapse: collapse; }
.hh-vt th {
  position: sticky; top: 0; background: var(--pink-50); color: var(--pink-700);
  font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: .4px;
  text-align: left; padding: 10px 12px; white-space: nowrap; border-bottom: none;
}
.hh-vt thead th:first-child { border-top-left-radius: 11px; }
.hh-vt thead th:last-child { border-top-right-radius: 11px; }
.hh-vt td { padding: 10px 12px; border-bottom: 1px solid var(--line); font-size: 13px; vertical-align: middle; }
.hh-vt tbody tr:last-child td { border-bottom: none; }
.hh-vt__row { cursor: pointer; transition: background-color .12s; }
.hh-vt__row:hover { background: var(--pink-50); }
.hh-vt__row.is-on { background: var(--pink-100); }
.hh-vt__row.is-on td:first-child { box-shadow: inset 3px 0 0 var(--pink-600); }
.hh-vt__sku { font-family: ui-monospace, "SFMono-Regular", Menlo, monospace; font-weight: 700; white-space: nowrap; }
.hh-vt__barcode { font-family: ui-monospace, "SFMono-Regular", Menlo, monospace; font-size: 12px; color: var(--ink-2); white-space: nowrap; }
.hh-vt__cfg { color: var(--muted); min-width: 200px; }
.hh-vt__gia { font-weight: 700; color: var(--pink-600); font-variant-numeric: tabular-nums; }

/* nhật ký thay đổi */
.hh-ls { list-style: none; margin: 0; padding: 0 0 0 6px; display: flex; flex-direction: column; }
.hh-ls__item { position: relative; display: flex; gap: 14px; padding: 0 0 18px 0; }
.hh-ls__item::before {
  content: ''; position: absolute; left: 5px; top: 16px; bottom: 0; width: 2px; background: var(--line-2);
}
.hh-ls__item:last-child::before { display: none; }
.hh-ls__dot {
  width: 12px; height: 12px; border-radius: 50%; margin-top: 4px; flex-shrink: 0;
  background: var(--pink-500); box-shadow: 0 0 0 3px var(--pink-100);
}
.hh-ls__dot.is-tao { background: #10b981; box-shadow: 0 0 0 3px #d1fae5; }
.hh-ls__dot.is-them { background: #3b82f6; box-shadow: 0 0 0 3px #dbeafe; }
.hh-ls__body {
  flex: 1; min-width: 0; background: #fff; border: 1px solid var(--line);
  border-radius: 12px; padding: 12px 14px;
}
.hh-ls__head { display: flex; justify-content: space-between; gap: 10px; flex-wrap: wrap; font-size: 13.5px; }
.hh-ls__head strong { color: var(--pink-700); }
.hh-ls__target { font-size: 12.5px; color: var(--ink-2); margin-top: 3px; word-break: break-word; }
.hh-ls__changes { list-style: none; margin: 8px 0 0; padding: 0; display: flex; flex-direction: column; gap: 5px; }
.hh-ls__changes li {
  display: flex; align-items: baseline; gap: 7px; flex-wrap: wrap;
  font-size: 12.5px; background: var(--pink-50); border-radius: 8px; padding: 5px 9px;
}
.hh-ls__field { font-weight: 700; color: var(--pink-700); }
.hh-ls__changes em { font-style: normal; color: var(--muted); text-decoration: line-through; word-break: break-word; }
.hh-ls__changes b { color: var(--ink); word-break: break-word; }
.hh-ls__changes i { color: var(--pink-300); }
.hh-ls__by { margin-top: 8px; font-size: 11.5px; color: var(--muted); }

/* ═══════════ MODAL XUẤT FILE ═══════════ */
.hh-export-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.hh-export-checkall { display: flex; align-items: center; gap: 8px; cursor: pointer; font-size: 13px; font-weight: 600; color: var(--ink); }
.hh-export-count { font-weight: 500; color: var(--muted); }
.hh-export-search { width: 240px; }
.hh-empty-cell { text-align: center; color: var(--muted); padding: 24px; font-size: 13px; }

.hh-export-list {
  margin-top: 12px; max-height: 50vh; overflow-y: auto;
  border: 1px solid var(--line); border-radius: 12px; background: #fff;
}
.hh-export-group { border-bottom: 1px solid var(--line); }
.hh-export-group:last-child { border-bottom: none; }

.hh-export-group__head {
  display: flex; align-items: center; gap: 10px; cursor: pointer;
  padding: 10px 12px; background: var(--pink-50);
}
.hh-export-group__thumb { width: 34px; height: 34px; border-radius: 8px; object-fit: cover; flex-shrink: 0; background: #fff; border: 1px solid var(--line); }
.hh-export-group__info { flex: 1; min-width: 0; }
.hh-export-group__name { font-weight: 700; font-size: 13.5px; color: var(--pink-700); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hh-export-group__meta { font-size: 12px; color: var(--muted); margin-top: 1px; }
.hh-export-group__price { font-size: 13px; font-weight: 600; color: var(--ink); white-space: nowrap; }

.hh-export-variant {
  display: flex; align-items: center; gap: 10px; cursor: pointer;
  padding: 8px 12px 8px 40px; border-top: 1px dashed var(--line); font-size: 13px;
}
.hh-export-variant__sku {
  font-family: ui-monospace, "SFMono-Regular", Menlo, monospace; font-size: 12.5px; font-weight: 600;
  min-width: 110px; color: var(--ink);
}
.hh-export-variant__spec { flex: 1; color: var(--muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hh-export-variant__price { white-space: nowrap; color: var(--pink-600); font-weight: 600; }

.hh-export-group__head input,
.hh-export-variant input,
.hh-export-checkall input { flex-shrink: 0; accent-color: var(--pink-600); cursor: pointer; }

/* ═══════════ FORM TRONG MODAL ═══════════ */
.hh-block {
  border: 1px solid var(--line); border-radius: 14px;
  padding: 16px; margin: 0; background: #fff; min-width: 0;
}
.hh-block:disabled { opacity: .75; }
.hh-block legend {
  font-size: 12.5px; font-weight: 800; color: var(--pink-700);
  background: var(--pink-100); border-radius: 999px; padding: 4px 12px;
  display: inline-flex; align-items: center; gap: 8px; width: auto;
}
.hh-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 14px; }
.hh-field--wide { grid-column: 1 / -1; }

.hh-note {
  display: flex; align-items: flex-start; gap: 8px; margin: 14px 0 0;
  padding: 9px 12px; background: var(--pink-50); border: 1px dashed var(--pink-200);
  border-radius: 9px; font-size: 12.5px; color: var(--muted); line-height: 1.55;
}
.hh-note--plain { margin: 0 0 14px; }
.hh-note b { color: var(--pink-700); }

/* chip bật/tắt — phân loại sử dụng (chọn nhiều) */
.hh-chip-select { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 2px; }
.hh-chip-toggle {
  padding: 7px 14px; border-radius: 999px; cursor: pointer; font-size: 12.5px; font-weight: 600;
  background: #fff; border: 1px solid var(--pink-200); color: var(--muted); font-family: inherit;
  transition: background-color .15s, border-color .15s, color .15s;
}
.hh-chip-toggle:hover { border-color: var(--pink-300); color: var(--pink-700); }
.hh-chip-toggle.is-on { background: var(--pink-600); border-color: var(--pink-600); color: #fff; }

/* gallery nhiều ảnh */
.hh-gallery { display: flex; flex-wrap: wrap; gap: 10px; }
.hh-gallery__item {
  position: relative; width: 92px; height: 92px; flex-shrink: 0;
  border: 1px solid var(--line); border-radius: 12px; overflow: hidden; background: #fff;
}
.hh-gallery__item img { width: 100%; height: 100%; object-fit: cover; }
.hh-gallery__badge {
  position: absolute; left: 4px; bottom: 4px; background: var(--pink-600); color: #fff;
  font-size: 9.5px; font-weight: 700; padding: 2px 6px; border-radius: 999px; line-height: 1.4;
}
.hh-gallery__actions {
  position: absolute; top: 0; right: 0; display: flex; gap: 2px; padding: 3px;
  background: linear-gradient(180deg, rgba(0,0,0,.45), transparent);
  opacity: 0; transition: opacity .15s;
}
.hh-gallery__item:hover .hh-gallery__actions { opacity: 1; }
.hh-icon-btn--sm { width: 22px; height: 22px; background: rgba(255,255,255,.9); color: var(--pink-700); }
.hh-gallery__add {
  width: 92px; height: 92px; flex-shrink: 0; cursor: pointer;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px;
  border: 1px dashed var(--pink-300); border-radius: 12px; background: var(--pink-50);
  color: var(--pink-600); font-size: 11px; font-weight: 600; text-align: center;
}
.hh-gallery__add:hover { background: var(--pink-100); }
.hh-gallery__add i { font-size: 18px; }

/* ma trận phiên bản */
.hh-matrix-wrap { border: 1px solid var(--line); border-radius: 12px; overflow: auto; max-height: 340px; }
.hh-matrix { width: 100%; border-collapse: collapse; }
.hh-matrix th {
  position: sticky; top: 0; background: var(--pink-50); color: var(--pink-700); z-index: 1;
  font-size: 11px; font-weight: 800; text-align: left; padding: 9px 10px; white-space: nowrap;
  text-transform: uppercase; letter-spacing: .4px; border-bottom: none;
}
.hh-matrix thead th:first-child { border-top-left-radius: 11px; }
.hh-matrix thead th:last-child { border-top-right-radius: 11px; }
.hh-matrix td { padding: 6px 10px; border-bottom: 1px solid var(--line); font-size: 13px; vertical-align: middle; }
.hh-matrix tr:last-child td { border-bottom: none; }
.hh-matrix__stt { width: 38px; }
.hh-matrix__cfg { color: var(--muted); font-size: 12.5px; min-width: 180px; white-space: normal; word-break: break-word; }
.hh-matrix__empty { text-align: center; color: var(--muted); padding: 20px; }
.hh-cell { padding: 6px 9px; font-size: 12.5px; }
.hh-cell--sku { font-family: ui-monospace, "SFMono-Regular", Menlo, monospace; min-width: 190px; }
.hh-cell--ma { font-family: ui-monospace, "SFMono-Regular", Menlo, monospace; min-width: 140px; }

/* trình soạn mô tả */
.hh-editor { border: 1px solid var(--field); border-radius: 12px; overflow: hidden; }
.hh-editor__bar { display: flex; align-items: center; gap: 2px; padding: 6px 8px; background: var(--pink-50); border-bottom: 1px solid var(--line); flex-wrap: wrap; }
.hh-editor__bar button {
  background: none; border: none; width: 30px; height: 28px; border-radius: 6px;
  color: var(--pink-700); cursor: pointer; font-size: 13px;
}
.hh-editor__bar button:hover { background: var(--pink-100); }
.hh-editor__sep { width: 1px; height: 18px; background: var(--pink-200); margin: 0 5px; }
.hh-editor__area { min-height: 220px; padding: 14px 16px; font-size: 13.5px; line-height: 1.6; outline: none; }
.hh-editor__area:empty::before { content: attr(data-placeholder); color: #b9a3ae; }
.hh-editor__area:focus { box-shadow: inset 0 0 0 2px var(--pink-100); }

/* ═══════════ TOAST ═══════════ */
.hh-toast {
  position: fixed; bottom: 26px; left: 50%; transform: translateX(-50%); z-index: 1100;
  background: var(--pink-700); color: #fff; padding: 10px 20px; border-radius: 999px;
  font-size: 13px; box-shadow: 0 8px 22px rgba(168,27,93,.35); max-width: calc(100% - 40px); text-align: center;
}

/* ═══════════ MÀN HÌNH NHỎ ═══════════ */
@media (max-width: 900px) {
  .hh-ct-top { grid-template-columns: 1fr; }
  .hh-ct-media__main { max-width: 260px; }
}
@media (max-width: 768px) {
  .hh-bar__actions { width: 100%; margin-left: 0; }
  .hh-search { width: 100%; }
  .hh-modal-mask { padding: 0; }
  .hh-modal { max-height: 100vh; border-radius: 0; }
  .hh-modal__foot { flex-direction: column-reverse; align-items: stretch; }
  .hh-modal__foot-left, .hh-modal__foot-right { justify-content: flex-end; }
  .hh-foot-hint { width: 100%; text-align: right; }
}

@media (prefers-reduced-motion: reduce) {
  .hh-btn, .hh-caret, .hh-filter, .hh-filter__panel, .hh-row, .hh-vt__row { transition: none; }
  .hh-spinner { animation-duration: 2s; }
}
</style>