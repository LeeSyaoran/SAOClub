<template>
  <div class="hh">
    <!-- ══════════ CARD LỚN: toolbar + filter + bảng ══════════ -->
    <div class="hh-card">
      <!-- ══════════════ TOOLBAR ══════════════ -->
      <div class="hh-toolbar">
        <div class="hh-toolbar__left">
          <span class="hh-toolbar__count">{{ groupsDaLoc.length }} sản phẩm · {{ bienTheDaLoc.length }} phiên bản</span>
          <div class="hh-search">
            <Search :size="14" class="hh-search__icon" />
            <input v-model="searchKeyword" type="text" placeholder="Tìm theo mã sản phẩm, tên, SKU, mã vạch" />
            <button v-if="searchKeyword" class="hh-search__clear" title="Xóa tìm kiếm" @click="searchKeyword = ''">
              <X :size="13" />
            </button>
          </div>
        </div>

        <div class="hh-toolbar__right">
          <button class="hh-btn hh-btn--ghost" :class="{ 'is-on': isFilterOpen }" @click="isFilterOpen = !isFilterOpen">
            <Filter :size="14" />
            <span>Bộ lọc</span>
            <span v-if="soBoLocDangDung" class="hh-chip">{{ soBoLocDangDung }}</span>
            <ChevronDown :size="13" class="hh-caret" :class="{ 'is-open': isFilterOpen }" />
          </button>

          <button class="hh-btn hh-btn--primary" @click="openCreate">
            <Plus :size="14" /> Tạo mới
          </button>

          <button class="hh-btn hh-btn--ghost" :disabled="!bienTheDaLoc.length" @click="openExportModal">
            <Download :size="14" /> Xuất file
          </button>

          <button class="hh-icon-btn" title="Tải lại dữ liệu" @click="fetchData">
            <RefreshCw :size="14" :class="{ 'fa-spin': isLoading }" />
          </button>
        </div>
      </div>

      <!-- ══════════════ BỘ LỌC ══════════════ -->
      <div class="hh-filter" :class="{ 'is-open': isFilterOpen }">
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

            <label class="hh-field">
              <span>Sắp xếp</span>
              <select v-model="sortKey">
                <option value="stt_desc">STT: Lớn → Nhỏ</option>
                <option value="stt_asc">STT: Nhỏ → Lớn</option>
                <option value="name_asc">Tên: A → Z</option>
                <option value="name_desc">Tên: Z → A</option>
              </select>
            </label>
          </div>

          <div class="hh-filter__foot">
            <div class="hh-filter__btns">
              <button class="hh-btn hh-btn--ghost hh-btn--sm" @click="resetFilters"><Eraser :size="14" /> Xóa lọc</button>
              <button class="hh-btn hh-btn--primary hh-btn--sm" @click="isFilterOpen = false">Xong</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ══════════════ BẢNG DỮ LIỆU ══════════════ -->
      <p v-if="loadError" class="hh-alert">
        {{ loadError }}
        <button class="hh-link" @click="fetchData">Thử lại</button>
      </p>

      <div class="hh-table-wrap">
        <table class="hh-table">
          <thead>
            <tr>
              <th class="hh-col-ma" style="width: 12%;"><span class="d-inline-flex align-items-center gap-1.5"><Tag :size="12" /> Mã sản phẩm</span></th>
              <th class="hh-col-ten" style="width: 30%;"><span class="d-inline-flex align-items-center gap-1.5"><Laptop :size="12" /> Tên sản phẩm</span></th>
              <th class="ta-r" style="width: 13%;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-end"><DollarSign :size="12" /> Giá bán</span></th>
              <th class="ta-r" style="width: 13%;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-end"><Coins :size="12" /> Giá vốn</span></th>
              <th class="ta-c" style="width: 10%; text-align: center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Activity :size="12" /> Trạng thái</span></th>
              <th class="ta-c" style="width: 10%; text-align: center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><Calendar :size="12" /> Ngày tạo</span></th>
              <th class="ta-c" style="width: 10%; text-align: center;"><span class="d-inline-flex align-items-center gap-1.5 justify-content-center w-100"><CalendarCheck :size="12" /> Ngày cập nhật</span></th>
              <th class="hh-col-go ta-c" style="width: 2%; text-align: center;"></th>
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
              <td class="ta-c">
                <span class="hh-tag" :class="tagClass(group.trangThai)">{{ nhanTrangThai(group.trangThai) }}</span>
              </td>
              <td class="ta-c hh-muted hh-td-ngay">{{ formatDate(group.ngayTao) }}</td>
              <td class="ta-c hh-muted hh-td-ngay">{{ formatDate(group.ngayCapNhat) }}</td>
              <td class="hh-col-go ta-c"><ChevronRight :size="14" /></td>
            </tr>
          </tbody>
        </table>

        <div v-if="isLoading" class="hh-overlay"><span class="hh-spinner"></span></div>
      </div>

      <div v-if="!isLoading && !pagedGroups.length" class="hh-empty">
        <Inbox :size="48" />
        <p v-if="coBoLoc">Không có sản phẩm nào khớp với bộ lọc hiện tại.</p>
        <p v-else>Chưa có sản phẩm nào. Bấm "Tạo mới" để thêm sản phẩm đầu tiên.</p>
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
          <button class="hh-icon-btn" :disabled="page === 1" @click="page--"><ChevronLeft :size="14" /></button>
          <span class="hh-pager__page">{{ page }} / {{ totalPages || 1 }}</span>
          <button class="hh-icon-btn" :disabled="page >= totalPages" @click="page++"><ChevronRight :size="14" /></button>
        </div>
      </footer>
    </div>

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
            <button class="hh-icon-btn" aria-label="Đóng" @click="dongChiTiet"><X :size="14" /></button>
          </header>

          <nav class="hh-tabs">
            <button class="hh-tab" :class="{ 'is-on': tabCT === 'info' }" @click="tabCT = 'info'">Thông tin</button>
            <button class="hh-tab" :class="{ 'is-on': tabCT === 'bienthe' }" @click="tabCT = 'bienthe'">
              Biến thể <span class="hh-chip">{{ chiTiet?.variants?.length ?? 0 }}</span>
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
                    <div class="hh-ct-item"><dt>Số phiên bản</dt><dd>{{ chiTiet?.variants?.length ?? 0 }}</dd></div>
                    <div class="hh-ct-item"><dt>Khách đặt</dt><dd>{{ chiTiet.khachDat }}</dd></div>
                    <div class="hh-ct-item"><dt>Giá bán</dt><dd class="hh-ct-item__manh">{{ chiTiet.khoangGia ? chiTiet.khoangGia + ' ₫' : 'Liên hệ' }}</dd></div>
                    <div class="hh-ct-item"><dt>Thương hiệu</dt><dd>{{ chiTiet.tenThuongHieu || 'Chưa có' }}</dd></div>
                    <div class="hh-ct-item"><dt>Nhà cung cấp</dt><dd>{{ chiTiet.tenNhaCungCap || 'Chưa có' }}</dd></div>
                    <div class="hh-ct-item"><dt>Danh mục</dt><dd>{{ chiTiet.tenDanhMuc || 'Chưa có' }}</dd></div>
                    <div class="hh-ct-item"><dt>Bảo hành</dt><dd>{{ chiTiet.baoHanhThang ? chiTiet.baoHanhThang + ' tháng' : 'Chưa có' }}</dd></div>
                    <div class="hh-ct-item"><dt>Ngày tạo</dt><dd>{{ formatDate(chiTiet.ngayTao) }}</dd></div>
                    <div class="hh-ct-item"><dt>Ngày cập nhật</dt><dd>{{ formatDate(chiTiet.ngayCapNhat) }}</dd></div>
                  </dl>
                </div>
              </div>

              <section class="hh-ct-block">
                <h3>Thông số chung</h3>
                <dl class="hh-ct-grid">
                  <div class="hh-ct-item"><dt>Màn hình</dt><dd>{{ chiTiet.kichThuocManHinh || 'Chưa có' }}</dd></div>
                  <div class="hh-ct-item"><dt>Hệ điều hành</dt><dd>{{ chiTiet.heDieuHanh || 'Chưa có' }}</dd></div>
                  <div class="hh-ct-item"><dt>Pin</dt><dd>{{ chiTiet.pin || 'Chưa có' }}</dd></div>
                  <div class="hh-ct-item"><dt>Trọng lượng</dt><dd>{{ chiTiet.trongLuongKg ? chiTiet.trongLuongKg + ' kg' : 'Chưa có' }}</dd></div>
                </dl>
              </section>

              <section class="hh-ct-block">
                <h3>Mô tả</h3>
                <div v-if="chiTiet.moTa" class="hh-ct-mota" v-html="chiTiet.moTa"></div>
                <p v-else class="hh-muted">Chưa có mô tả. Bấm "Chỉnh sửa" để bổ sung.</p>
              </section>
            </div>

            <!-- ─────────── CHI TIẾT · BIẾN THỂ ─────────── -->
            <div v-show="tabCT === 'bienthe'" class="hh-pane">
              <p class="hh-note hh-note--plain">
                <Hand :size="14" />
                Bấm vào một dòng để chọn phiên bản, các nút thao tác nằm ở cuối cửa sổ.
              </p>

              <div class="hh-vt-wrap">
                <table class="hh-vt">
                  <thead>
                    <tr>
                      <th><span class="d-inline-flex align-items-center gap-1.5"><Tag :size="12" /> Mã SKU</span></th>
                      <th><span class="d-inline-flex align-items-center gap-1.5"><Barcode :size="12" /> Mã vạch</span></th>
                      <th><span class="d-inline-flex align-items-center gap-1.5"><Cpu :size="12" /> Cấu hình</span></th>
                      <th class="ta-r"><span class="d-inline-flex align-items-center gap-1.5 justify-content-end"><Coins :size="12" /> Giá vốn</span></th>
                      <th class="ta-r"><span class="d-inline-flex align-items-center gap-1.5 justify-content-end"><DollarSign :size="12" /> Giá bán</span></th>
                      <th><span class="d-inline-flex align-items-center gap-1.5"><Activity :size="12" /> Trạng thái</span></th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="v in (chiTiet?.variants ?? [])" :key="v.bienTheId"
                      class="hh-vt__row" :class="{ 'is-on': String(v.bienTheId) === String(bienTheChonId) }"
                      @click="bienTheChonId = v.bienTheId"
                    >
                      <td class="hh-vt__sku">{{ v.maSku }}</td>
                      <td class="hh-vt__barcode">
                        <div v-if="v.barcode" class="hh-barcode-card" :title="'Mã vạch: ' + v.barcode">
                          <svg :key="v.barcode" :ref="(el) => renderBarcode(el, v.barcode)"></svg>
                        </div>
                        <span v-else class="hh-muted">Chưa có</span>
                      </td>
                      <td class="hh-vt__cfg">
                        <div v-if="coThongSoBienThe(v)" class="hh-cfg-chips">
                          <span v-if="layCpu(v)" class="hh-cfg-chip hh-cfg-chip--cpu" :title="'CPU: ' + layCpu(v)">
                            <Cpu :size="12" />
                            <span>{{ layCpu(v) }}</span>
                          </span>
                          <span v-if="layRam(v)" class="hh-cfg-chip hh-cfg-chip--ram" :title="'RAM: ' + layRam(v)">
                            <MemoryStick :size="12" />
                            <span>{{ layRam(v) }}</span>
                          </span>
                          <span v-if="layOCung(v)" class="hh-cfg-chip hh-cfg-chip--disk" :title="'Ổ cứng: ' + layOCung(v)">
                            <HardDrive :size="12" />
                            <span>{{ layOCung(v) }}</span>
                          </span>
                          <span v-if="layGpu(v)" class="hh-cfg-chip hh-cfg-chip--gpu" :title="'Card đồ họa: ' + layGpu(v)">
                            <Monitor :size="12" />
                            <span>{{ layGpu(v) }}</span>
                          </span>
                          <span v-if="v.mauSac" class="hh-cfg-chip hh-cfg-chip--color" :title="'Màu sắc: ' + v.mauSac">
                            <Palette :size="12" />
                            <span>{{ v.mauSac }}</span>
                          </span>
                        </div>
                        <span v-else class="hh-muted">{{ moTaBienThe(v) || 'Phiên bản tiêu chuẩn' }}</span>
                      </td>
                      <td class="ta-r hh-muted">{{ formatNumber(v.giaNhap) }}</td>
                      <td class="ta-r hh-vt__gia">{{ formatNumber(v.giaBan) }}</td>
                      <td>
                        <span class="hh-tag" :class="tagClass(v.trangThai)">{{ nhanTrangThai(v.trangThai) }}</span>
                      </td>
                    </tr>
                    <tr v-if="!chiTiet?.variants?.length" class="hh-vt__empty">
                      <td colspan="6">
                        <div class="hh-empty">
                          <Layers :size="14" />
                          <p>Sản phẩm này chưa có biến thể nào. Bấm <strong>“Thêm phiên bản”</strong> ở thanh dưới cùng để tạo phiên bản đầu tiên.</p>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- ─────────── CHI TIẾT · LỊCH SỬ THAY ĐỔI ─────────── -->
            <div v-show="tabCT === 'lichsu'" class="hh-pane">
              <div v-if="nhatKyLoading" class="hh-empty">
                <Loader2 :size="14" class="fa-spin" />
                <p>Đang tải nhật ký…</p>
              </div>
              <ol v-else-if="lichSuHienTai.length" class="hh-ls">
                <li v-for="(m, i) in lichSuHienTai" :key="i" class="hh-ls__item">
                  <span
                    class="hh-ls__dot"
                    :class="{ 'is-them': m.doiTuong === 'bien_the', 'is-tao': m.doiTuong === 'san_pham' && m.giaTriCu === null }"
                  ></span>
                  <div class="hh-ls__body">
                    <div class="hh-ls__head">
                      <strong>{{ tenHanhDong(m) }}</strong>
                      <span class="hh-muted">{{ formatDate(m.thoiGian) }}</span>
                    </div>
                    <div v-if="m.maSku" class="hh-ls__target">Phiên bản {{ m.maSku }}</div>
                    <ul class="hh-ls__changes">
                      <li>
                        <span class="hh-ls__field">{{ m.tenTruong }}</span>
                        <em>{{ m.giaTriCu || '—' }}</em>
                        <ArrowRight :size="14" />
                        <b>{{ m.giaTriMoi || '—' }}</b>
                      </li>
                    </ul>
                    <div class="hh-ls__by">Người thực hiện: {{ m.tenNhanVien || '—' }}</div>
                  </div>
                </li>
              </ol>

              <div v-else class="hh-empty">
                <History :size="14" />
                <p>Chưa ghi nhận thay đổi nào cho sản phẩm này.</p>
              </div>
            </div>
          </div>

          <footer class="hh-modal__foot">
            <div class="hh-modal__foot-left">
              <button class="hh-btn hh-btn--ghost" @click="dongChiTiet">Đóng</button>
              <button v-if="tabCT === 'bienthe'" class="hh-btn hh-btn--ghost" @click="themPhienBan(chiTiet)">
                <Plus :size="14" /> Thêm phiên bản
              </button>
            </div>

            <div class="hh-modal__foot-right">
              <template v-if="tabCT === 'info'">
                <button class="hh-btn hh-btn--soft" @click="saoChepSanPham(chiTiet)"><Copy :size="14" /> Sao chép</button>
                <button class="hh-btn hh-btn--primary" @click="suaSanPham(chiTiet)"><Pencil :size="14" /> Chỉnh sửa</button>
              </template>

              <template v-else-if="tabCT === 'bienthe'">
                <span v-if="bienTheDangChon" class="hh-foot-hint">Đang chọn: <b>{{ bienTheDangChon.maSku }}</b></span>
                <span v-else class="hh-foot-hint">Chọn một phiên bản để thao tác</span>
                <button class="hh-btn hh-btn--ghost" :disabled="!bienTheDangChon" @click="inTemMa(bienTheDangChon)">
                  <Barcode :size="14" /> In tem mã
                </button>
                <button class="hh-btn hh-btn--soft" :disabled="!bienTheDangChon || dangSaoChepBienThe" @click="saoChepBienThe(bienTheDangChon)">
                  <Copy :size="14" /> {{ dangSaoChepBienThe ? 'Đang sao chép…' : 'Sao chép' }}
                </button>
                <button class="hh-btn hh-btn--primary" :disabled="!bienTheDangChon" @click="suaBienTheTuChiTiet(bienTheDangChon)">
                  <Pencil :size="14" /> Chỉnh sửa
                </button>
              </template>

              <template v-else>
                <span class="hh-foot-hint">Nhật ký thay đổi được hệ thống ghi tự động mỗi lần lưu sản phẩm hoặc phiên bản.</span>
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
            <button class="hh-icon-btn" aria-label="Đóng" @click="closeModal"><X :size="14" /></button>
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
                            <Star :size="14" />
                          </button>
                          <button type="button" class="hh-icon-btn hh-icon-btn--sm" title="Xóa ảnh" @click="xoaAnhTaiViTri(i)">
                            <Trash2 :size="14" />
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
                  <Clock :size="14" />
                  Ngày tạo và ngày cập nhật do hệ thống tự ghi tại thời điểm bấm Lưu — hiện là {{ dongHo }}.
                </p>
              </fieldset>
            </div>

            <!-- ─────────── TAB 2: PHIÊN BẢN ─────────── -->
            <div v-show="tab === 'bienthe'" class="hh-pane">
              <!-- Sửa một phiên bản (khi modalMode === 'edit') -->
              <template v-if="modalMode === 'edit'">
                <!-- Danh sách biến thể để chọn -->
                <div class="hh-bienthe-list">
                  <div class="hh-bienthe-list__title">Danh sách phiên bản</div>
                  <div class="hh-bienthe-list__items">
                    <div
                      v-for="v in bienTheRows"
                      :key="v.bienTheId"
                      class="hh-bienthe-item"
                      :class="{ 'is-on': String(form.bienTheId) === String(v.bienTheId) }"
                      @click="chonBienTheDeSua(v)"
                    >
                      <div class="hh-bienthe-item__info">
                        <span class="hh-bienthe-item__sku">{{ v.maSku }}</span>
                        <span class="hh-bienthe-item__cfg">{{ moTaBienThe(v) || 'Phiên bản tiêu chuẩn' }}</span>
                      </div>
                      <div class="hh-bienthe-item__price">{{ formatNumber(v.giaBan) }} đ</div>
                    </div>
                  </div>
                </div>

                <!-- Form sửa biến thể đã chọn -->
                <fieldset v-if="form.bienTheId" class="hh-block">
                  <legend>Đang sửa: {{ form.maSku }}</legend>
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
                          <RefreshCw :size="14" />
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
                <p v-else class="hh-note"><MousePointer2 :size="14" /> Chọn một phiên bản trong danh sách bên trái để sửa.</p>
              </template>

              <!-- Sinh nhiều phiên bản (khi modalMode === 'create' hoặc 'variant') -->
              <template v-else>
                <fieldset class="hh-block">
                  <legend>Phiên bản <span class="hh-chip">{{ bienTheRows.length }}</span></legend>
                  <p class="hh-note hh-note--plain">
                    Mỗi dòng là một phiên bản hoàn chỉnh. Bấm <b>"+ Thêm dòng"</b> để tạo thêm.
                  </p>

                  <em v-if="errors.bienThe" class="hh-err hh-mb8">{{ errors.bienThe }}</em>

                  <div class="hh-rows-wrap">
                    <table class="hh-rows">
                      <thead>
                        <tr>
                          <th class="hh-rows__stt">#</th>
                          <th><span class="d-inline-flex align-items-center gap-1.5"><Tag :size="12" /> Mã SKU</span></th>
                          <th><span class="d-inline-flex align-items-center gap-1.5"><Barcode :size="12" /> Mã vạch</span></th>
                          <th><span class="d-inline-flex align-items-center gap-1.5"><Palette :size="12" /> Màu sắc</span></th>
                          <th><span class="d-inline-flex align-items-center gap-1.5"><Cpu :size="12" /> CPU</span></th>
                          <th><span class="d-inline-flex align-items-center gap-1.5"><MemoryStick :size="12" /> RAM</span></th>
                          <th><span class="d-inline-flex align-items-center gap-1.5"><HardDrive :size="12" /> Ổ cứng</span></th>
                          <th><span class="d-inline-flex align-items-center gap-1.5"><Monitor :size="12" /> GPU</span></th>
                          <th></th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(row, i) in bienTheRows" :key="row._key">
                          <td class="hh-muted hh-rows__stt">{{ i + 1 }}</td>
                          <td><input v-model.trim="row.maSku" class="hh-cell hh-cell--sku" placeholder="Tự sinh nếu trống" /></td>
                          <td><input v-model.trim="row.barcode" class="hh-cell hh-cell--ma" placeholder="8–13 số" /></td>
                          <td>
                            <select v-model="row.mauSac" class="hh-cell hh-cell--sel">
                              <option value="">— Không —</option>
                              <option v-for="m in optMauSac.value" :key="m" :value="m">{{ m }}</option>
                            </select>
                          </td>
                          <td>
                            <select v-model="row.cpuId" class="hh-cell hh-cell--sel">
                              <option value="">— Không —</option>
                              <option v-for="c in danhSachCpu" :key="idOf(c,'cpuId')" :value="idOf(c,'cpuId')">{{ c.tenCpu }}</option>
                            </select>
                          </td>
                          <td>
                            <select v-model="row.ramId" class="hh-cell hh-cell--sel">
                              <option value="">— Không —</option>
                              <option v-for="r in danhSachRam" :key="idOf(r,'ramId')" :value="idOf(r,'ramId')">{{ r.dungLuong || r.tenRam }}</option>
                            </select>
                          </td>
                          <td>
                            <select v-model="row.oCungId" class="hh-cell hh-cell--sel">
                              <option value="">— Không —</option>
                              <option v-for="o in danhSachOCung" :key="idOf(o,'oCungId')" :value="idOf(o,'oCungId')">{{ tenOCung(o) }}</option>
                            </select>
                          </td>
                          <td>
                            <select v-model="row.gpuId" class="hh-cell hh-cell--sel">
                              <option value="">— Không —</option>
                              <option v-for="g in danhSachGpu" :key="idOf(g,'gpuId')" :value="idOf(g,'gpuId')">{{ g.tenGpu }}</option>
                            </select>
                          </td>
                          <td class="ta-c">
                            <button type="button" class="hh-icon-btn" title="Xóa dòng này" @click="xoaDong(row._key)">
                              <X :size="14" />
                            </button>
                          </td>
                        </tr>
                        <tr v-if="!bienTheRows.length">
                          <td colspan="9" class="hh-rows__empty">Chưa có phiên bản nào — bấm "+ Thêm dòng" để tạo.</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>

                  <button type="button" class="hh-btn hh-btn--ghost hh-btn--sm" style="margin-top:10px" @click="themDong">
                    <Plus :size="14" /> Thêm dòng
                  </button>
                </fieldset>

                <fieldset class="hh-block">
                  <legend>Thông số chung <span class="hh-chip hh-chip--soft">áp dụng mọi phiên bản</span></legend>
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
                      <input v-model.trim="form.skuPrefix" placeholder="Để trống lấy theo mã sản phẩm" />
                      <em class="hh-hint">Dùng khi cần SKU theo quy tắc riêng.</em>
                    </label>
                  </div>
                  <p class="hh-note">
                    <Info :size="14" />
                    Giá vốn và giá bán đặt sau — mở chi tiết sản phẩm, chọn phiên bản rồi bấm "Chỉnh sửa", hoặc dùng phiếu nhập kho.
                  </p>
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
                    <button type="button" title="Danh sách chấm" @click="dinhDang('insertUnorderedList')"><ListIcon :size="14" /></button>
                    <button type="button" title="Danh sách số" @click="dinhDang('insertOrderedList')"><ListOrdered :size="14" /></button>
                    <span class="hh-editor__sep"></span>
                    <button type="button" title="Chèn liên kết" @click="chenLink"><Link :size="14" /></button>
                    <button type="button" title="Xóa định dạng" @click="dinhDang('removeFormat')"><Eraser :size="14" /></button>
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
            <button class="hh-icon-btn" aria-label="Đóng" @click="showExportModal = false"><X :size="14" /></button>
          </header>

          <div class="hh-modal__body">
            <div class="hh-export-toolbar">
              <label class="hh-export-checkall">
                <input type="checkbox" :checked="allChecked" @change="toggleAll" />
                <span>Chọn tất cả</span>
                <span class="hh-export-count">{{ selectedIds.length }}/{{ bienTheDaLoc.length }}</span>
              </label>
              <div class="hh-search hh-export-search">
                <Search :size="14" class="hh-search__icon" />
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
                <Download :size="14" /> Xuất file ({{ selectedIds.length }})
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
import { ThuocTinhService } from '@/services/ThuocTinhService.js'
import * as bienTheApi from '@/services/bienTheSanPhamService.js'
import * as sanPhamApi from '@/services/sanPhamService.js'
import { getLichSu } from '@/services/SanPhamService.js'
import { Cpu, MemoryStick, HardDrive, Palette, Monitor, Barcode, Search, X, Filter, ChevronDown, Plus, Download, RefreshCw, ChevronLeft, ChevronRight, Inbox, Hand, Edit, Pencil, Trash2, Copy, History, Star, BarChart2, Loader2, Save, ExternalLink, Clock, ArrowRight, Eraser, ShoppingBag, Phone, Mail, MapPin, Building2, Tag, Briefcase, FileText, Settings, ToggleLeft, User, IdCard, Headphones, Send, AlertCircle, Info, Link, ListOrdered, ListIcon, MousePointer2, Laptop, DollarSign, Coins, Activity, Calendar, CalendarCheck, Hash, Layers } from '@lucide/vue'
import JsBarcode from 'jsbarcode'

// Gọi hàm API tương ứng của service
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
  console.log('[DEBUG goiSua] api keys:', Object.keys(api || {}), 'fn found:', typeof fn, 'fn.length:', fn?.length)
  if (fn) return fn.length >= 2 ? fn(id, payload) : fn(payload)
  throw new Error(`${tenApi} không có hàm cập nhật (update / save)`)
}

const apiTaoSanPham = (payload) => goiTao(sanPhamApi, payload, 'sanPhamService')
const apiSuaSanPham = (id, payload) => goiSua(sanPhamApi, id, payload, 'sanPhamService')
const apiTaoBienThe = (payload) => goiTao(bienTheApi, payload, 'bienTheSanPhamService')

// Tải ảnh sản phẩm lên server
const UPLOAD_URL = '/api/upload'
const THU_MUC_ANH = '/images/'

// Trạng thái kinh doanh sản phẩm
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

// Thuộc tính từ API (P3 - động thay hardcoded)
const HE_THONG_THUOC_TINH = {
  mau_sac: {
    field: 'mauSac',
    default: ['Đen', 'Trắng', 'Bạc', 'Xám', 'Xanh Dương', 'Xanh Lá', 'Đỏ', 'Vàng', 'Hồng', 'Tím', 'Cam', 'Nâu']
  },
  man_hinh: {
    field: 'kichThuocManHinh',
    default: ['15.6" FHD 60Hz', '15.6" FHD 144Hz', '15.6" QHD 240Hz', '16" 2.5K 120Hz', '16" FHD 165Hz', '16" WQXGA 165Hz', '16" 2.8K OLED 120Hz']
  },
  pin: {
    field: 'pin',
    default: ['41Wh', '48Wh', '50Wh', '52Wh', '54Wh', '57Wh', '75Wh', '80Wh', '86Wh', '90Wh']
  },
  he_dieu_hanh: {
    field: 'heDieuHanh',
    default: ['Windows 11 Home', 'Windows 11 Pro', 'macOS', 'Không kèm HĐH']
  }
}
const BAO_HANH_GOI_Y = [6, 12, 18, 24, 36]
const TRONG_LUONG_GOI_Y = [1.2, 1.3, 1.5, 1.7, 1.8, 2.0, 2.3, 2.5]

// Cache thuộc tính động từ API
const thuocTinhDong = ref({})
const loadThuocTinh = async () => {
  try {
    const list = await ThuocTinhService.getAll()
    const map = {}
    for (const tt of list) {
      const cfg = HE_THONG_THUOC_TINH[tt.tenTruong]
      if (cfg) {
        // Gộp giá trị API + giá trị đã có trong data + default
        const apiValues = (tt.giaTriList || []).map(g => g.giaTri)
        const existingValues = bienTheChuan.value.map(v => v[cfg.field]).filter(Boolean)
        map[tt.tenTruong] = {
          field: cfg.field,
          tenHienThi: tt.tenHienThi,
          loaiDuLieu: tt.loaiDuLieu,
          batBuoc: tt.batBuoc,
          values: [...new Set([...cfg.default, ...existingValues, ...apiValues])].sort()
        }
      }
    }
    thuocTinhDong.value = map
  } catch (e) {
    // Fallback to hardcoded
    console.warn('Không load được thuộc tính từ API, dùng mặc định', e)
  }
}
onMounted(async () => {
  dongHo.value = bayGio()
  await fetchMasterData()
  await fetchData()
  loadThuocTinh()
})

const MAN_HINH_GOI_Y = computed(() => thuocTinhDong.value.man_hinh?.values || ['15.6" FHD 60Hz', '15.6" FHD 144Hz', '15.6" QHD 240Hz', '16" 2.5K 120Hz', '16" FHD 165Hz', '16" WQXGA 165Hz', '16" 2.8K OLED 120Hz'])
const PIN_GOI_Y = computed(() => thuocTinhDong.value.pin?.values || ['41Wh', '48Wh', '50Wh', '52Wh', '54Wh', '57Wh', '75Wh', '80Wh', '86Wh', '90Wh'])
const HDH_GOI_Y = computed(() => thuocTinhDong.value.he_dieu_hanh?.values || ['Windows 11 Home', 'Windows 11 Pro', 'macOS', 'Không kèm HĐH'])
const MAU_SAC_GOI_Y = computed(() => thuocTinhDong.value.mau_sac?.values || ['Đen', 'Trắng', 'Bạc', 'Xám', 'Xanh Dương', 'Xanh Lá', 'Đỏ', 'Vàng', 'Hồng', 'Tím', 'Cam', 'Nâu'])

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

// Lấy mốc thời gian hiện tại
const bayGio = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

/* ─── Trạng thái màn hình ─── */
// Computed: nhóm sản phẩm đã lọc — dùng arrow function để Vue track tốt hơn
const groupsDaLoc = computed(() => {
  // Ép reactive access bằng IIFE để đảm bảo Vue track tất cả filter properties
  return (() => {
    let ds = [...danhSachSanPham.value]
    // Search keyword
    if (searchKeyword.value) {
      const kw = khongDau(searchKeyword.value)
      ds = ds.filter(p =>
        khongDau(p.maSanPham).includes(kw) ||
        khongDau(p.tenSanPham).includes(kw) ||
        (p.variants || []).some(v =>
          khongDau(v.maSku || '').includes(kw) ||
          khongDau(v.barcode || '').includes(kw)
        )
      )
    }
    // Filter: trạng thái
    const fTrangThai = filters.trangThai
    if (fTrangThai) ds = ds.filter(p => p.trangThai === fTrangThai)
    // Filter: thương hiệu
    const fThuongHieu = filters.thuongHieuId
    if (fThuongHieu) ds = ds.filter(p => String(idOf(p, 'thuongHieuId')) === String(fThuongHieu))
    // Filter: nhà cung cấp
    const fNCC = filters.nhaCungCapId
    if (fNCC) ds = ds.filter(p => String(idOf(p, 'nhaCungCapId')) === String(fNCC))
    // Filter: phân loại
    const fPhanLoai = filters.phanLoai
    if (fPhanLoai) ds = ds.filter(p => (p.phanLoai || []).includes(fPhanLoai))
    // Filter: CPU
    const fCpu = filters.cpuId
    if (fCpu) ds = ds.filter(p => (p.variants || []).some(v => String(idOf(v, 'cpuId')) === String(fCpu)))
    // Filter: RAM
    const fRam = filters.ramId
    if (fRam) ds = ds.filter(p => (p.variants || []).some(v => String(idOf(v, 'ramId')) === String(fRam)))
    // Filter: màu sắc
    const fMauSac = filters.mauSac
    if (fMauSac) ds = ds.filter(p => (p.variants || []).some(v => v.mauSac === fMauSac))
    // Filter: giá
    const fGiaTu = filters.giaTu
    if (fGiaTu) ds = ds.filter(p => (p.giaBanMin || 0) >= Number(fGiaTu))
    const fGiaDen = filters.giaDen
    if (fGiaDen) ds = ds.filter(p => (p.giaBanMax || Infinity) <= Number(fGiaDen))
    // Sort
    if (sortKey.value === 'name_asc') ds.sort((a, b) => a.tenSanPham.localeCompare(b.tenSanPham))
    if (sortKey.value === 'name_desc') ds.sort((a, b) => b.tenSanPham.localeCompare(a.tenSanPham))
    if (sortKey.value === 'stt_asc') ds.sort((a, b) => (a.sanPhamId || 0) - (b.sanPhamId || 0))
    if (sortKey.value === 'stt_desc') ds.sort((a, b) => (b.sanPhamId || 0) - (a.sanPhamId || 0))
    return ds
  })()
})

// Computed: tất cả biến thể đã lọc
const bienTheDaLoc = computed(() => {
  return groupsDaLoc.value.flatMap(g => (g.variants || []).map(v => ({ ...v, sanPhamId: g.sanPhamId, tenSanPham: g.tenSanPham })))
})

// Ref: biến thể "chuẩn" dùng để lấy existing values khi load thuộc tính
const bienTheChuan = computed(() => bienThe.value.length ? bienThe.value : bienTheDaLoc.value)

// Computed: phân trang
const pagedGroups = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return groupsDaLoc.value.slice(start, start + pageSize.value)
})
const totalPages = computed(() => Math.ceil(groupsDaLoc.value.length / pageSize.value) || 1)
const coBoLoc = computed(() => {
  const f = filters
  return !!(f.trangThai || f.thuongHieuId || f.nhaCungCapId || f.phanLoai || f.cpuId || f.ramId || f.mauSac || f.giaTu || f.giaDen)
})
const soBoLocDangDung = computed(() => {
  const f = filters
  let n = 0
  if (f.trangThai) n++
  if (f.thuongHieuId) n++
  if (f.nhaCungCapId) n++
  if (f.phanLoai) n++
  if (f.cpuId) n++
  if (f.ramId) n++
  if (f.mauSac) n++
  if (f.giaTu) n++
  if (f.giaDen) n++
  return n
})

// Phân loại options
const phanLoaiOptions = computed(() => {
  const fromApi = danhSachPhanLoai.value || []
  if (fromApi.length) return fromApi
  return PHAN_LOAI_DU_PHONG
})

// Màu sắc options
const danhSachMauSac = computed(() => MAU_SAC_GOI_Y.value || [])
const optMauSac = computed(() => ({ value: MAU_SAC_GOI_Y.value || [] }))
const optMauSacSelect = computed(() => (MAU_SAC_GOI_Y.value || []).map(v => ({ label: v, value: v })))

// Thuộc tính select options
const optManHinhSelect = computed(() => MAN_HINH_GOI_Y.value.map(v => ({ label: v, value: v })))
const optPinSelect = computed(() => PIN_GOI_Y.value.map(v => ({ label: v, value: v })))
const optHeDieuHanhSelect = computed(() => HDH_GOI_Y.value.map(v => ({ label: v, value: v })))
const optTrongLuongSelect = computed(() => TRONG_LUONG_GOI_Y.map(v => ({ label: v + ' kg', value: v })))
const optBaoHanhSelect = computed(() => BAO_HANH_GOI_Y.map(v => ({ label: v + ' tháng', value: v })))

// CPU/RAM/GPU select options
const cpuOptionsSel = computed(() => (danhSachCpu.value || []).map(c => ({ label: c.tenCpu, value: idOf(c, 'cpuId') })))
const ramOptionsSel = computed(() => (danhSachRam.value || []).map(r => ({ label: r.dungLuong || r.tenRam, value: idOf(r, 'ramId') })))
const oCungOptionsSel = computed(() => (danhSachOCung.value || []).map(o => ({ label: tenOCung(o), value: idOf(o, 'oCungId') })))
const gpuOptionsSel = computed(() => (danhSachGpu.value || []).map(g => ({ label: g.tenGpu, value: idOf(g, 'gpuId') })))

// Reverse lookup: tên -> id (vì API trả tên, form cần id)
const cpuIdByName = computed(() => {
  const m = {}
  for (const c of (danhSachCpu.value || [])) {
    const id = idOf(c, 'cpuId')
    if (id != null) m[c.tenCpu?.trim()] = id
  }
  return m
})
const ramIdByName = computed(() => {
  const m = {}
  for (const r of (danhSachRam.value || [])) {
    const id = idOf(r, 'ramId')
    if (id != null) m[(r.dungLuong || r.tenRam)?.trim()] = id
  }
  return m
})
const oCungIdByName = computed(() => {
  const m = {}
  for (const o of (danhSachOCung.value || [])) {
    const id = idOf(o, 'oCungId')
    if (id != null) m[tenOCung(o)?.trim()] = id
  }
  return m
})
const gpuIdByName = computed(() => {
  const m = {}
  for (const g of (danhSachGpu.value || [])) {
    const id = idOf(g, 'gpuId')
    if (id != null) m[g.tenGpu?.trim()] = id
  }
  return m
})

const isSaving = ref(false)
const saveError = ref('')
const dongHo = ref(bayGio())

// Modal chi tiết
const showDetail = ref(false)
const chiTiet = ref(null)
const tabCT = ref('info')
const bienTheChonId = ref(null)
const bienTheDangChon = computed(() => chiTiet.value?.variants?.find(v => String(v.bienTheId) === String(bienTheChonId.value)))
const lichSuHienTai = ref([])
const nhatKyLoading = ref(false)
const moLaiChiTiet = ref(null)

// Modal form
const showModal = ref(false)
const modalMode = ref('create') // 'create' | 'edit' | 'variant'
const tieuDeModal = ref('')
const tab = ref('info')
const tabs = [
  { key: 'info', label: 'Thông tin' },
  { key: 'bienthe', label: 'Phiên bản' },
  { key: 'mota', label: 'Mô tả' }
]
const soPhienBan = computed(() => bienTheRows.value.length || (chiTiet.value?.variants?.length || 0))

// Form data
const form = ref({
  sanPhamId: null, maSanPham: '', tenSanPham: '',
  thuongHieuId: '', danhMucId: '', nhaCungCapId: '',
  loaiSanPham: 'LAPTOP', trangThaiSanPham: 'active',
  phanLoaiIds: [], phanLoaiTags: null, phanLoaiTen: null,
  hinhAnhList: [], moTa: '',
  baoHanhThang: 12, kichThuocManHinh: '', pin: '', heDieuHanh: '', trongLuongKg: '',
  skuPrefix: '',
  // bien the single edit
  bienTheId: null, maSku: '', barcode: '', mauSac: '', cpuId: '', ramId: '', oCungId: '', gpuId: '',
  giaNhap: 0, giaBan: 0, hinhAnhBienThe: null
})

const bienTheRows = ref([])
const errors = ref({})
const barcodeDaDung = ref(new Set())

// Ảnh
const anhSanPham = ref([])
const anhDangXem = ref('')
const dangTaiAnh = ref(false)
const ghiChuAnh = ref('Ảnh đầu tiên là ảnh chính. Kéo thả để đổi thứ tự.')

// Nhật ký
const banGoc = ref(null)
const moTaEl = ref(null)

// Export
const showExportModal = ref(false)
const exportSearch = ref('')
const exportGroups = computed(() => {
  let ds = groupsDaLoc.value
  if (exportSearch.value) {
    const kw = khongDau(exportSearch.value)
    ds = ds.filter(g =>
      khongDau(g.tenSanPham).includes(kw) ||
      khongDau(g.maSanPham).includes(kw) ||
      (g.variants || []).some(v => khongDau(v.maSku || '').includes(kw))
    )
  }
  return ds
})
const allChecked = computed(() => bienTheDaLoc.value.length > 0 && selectedIds.value.length === bienTheDaLoc.value.length)

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

const filters = reactive({
  trangThai: '', thuongHieuId: '', nhaCungCapId: '', phanLoai: '',
  cpuId: '', ramId: '', mauSac: '', giaTu: '', giaDen: ''
})

const selectedIds = ref([])
const page = ref(1)
const pageSize = ref(10)
const sortKey = ref('stt_desc') // 'stt_desc' | 'stt_asc' | 'name_asc' | 'name_desc'

const hienToast = (msg) => {
  toast.value = msg
  setTimeout(() => (toast.value = ''), 3000)
}

/* ════════════ HÀM THAO TÁC ════════════ */
const resetFilters = () => {
  filters.trangThai = ''
  filters.thuongHieuId = ''
  filters.nhaCungCapId = ''
  filters.phanLoai = ''
  filters.cpuId = ''
  filters.ramId = ''
  filters.mauSac = ''
  filters.giaTu = ''
  filters.giaDen = ''
  searchKeyword.value = ''
  page.value = 1
}

const openCreate = () => {
  modalMode.value = 'create'
  tieuDeModal.value = 'Tạo sản phẩm mới'
  tab.value = 'info'
  resetForm()
  showModal.value = true
}

const resetForm = () => {
  form.value = {
    sanPhamId: null, maSanPham: '', tenSanPham: '',
    thuongHieuId: '', danhMucId: '', nhaCungCapId: '',
    loaiSanPham: 'LAPTOP', trangThaiSanPham: 'active',
    phanLoaiIds: [], phanLoaiTags: null, phanLoaiTen: null,
    hinhAnhList: [], moTa: '',
    baoHanhThang: 12, kichThuocManHinh: '', pin: '', heDieuHanh: '', trongLuongKg: '',
    skuPrefix: '',
    bienTheId: null, maSku: '', barcode: '', mauSac: '', cpuId: '', ramId: '', oCungId: '', gpuId: '',
    giaNhap: 0, giaBan: 0, hinhAnhBienThe: null
  }
  bienTheRows.value = [taoDongBienThe()]
  errors.value = {}
  saveError.value = ''
  moTaEl.value = null
  setTimeout(() => { if (moTaEl.value) moTaEl.value.innerHTML = '' }, 50)
}

const taoDongBienThe = () => ({
  _key: Date.now() + Math.random(), bienTheId: null,
  maSku: '', barcode: '', mauSac: '', cpuId: '', ramId: '', oCungId: '', gpuId: '',
  giaNhap: 0, giaBan: 0
})

const moChiTiet = async (group) => {
  chiTiet.value = null
  showDetail.value = true
  tabCT.value = 'info'
  bienTheChonId.value = null
  lichSuHienTai.value = []
  try {
    const spRes = await get(`/api/san-pham/${group.sanPhamId}`)
    chiTiet.value = spRes.data || spRes
    // Load images
    anhSanPham.value = []
    if (chiTiet.value.hinhAnhChinh) anhSanPham.value.push(chiTiet.value.hinhAnhChinh)
    if (chiTiet.value.hinhAnhList) {
      for (const url of chiTiet.value.hinhAnhList) if (!anhSanPham.value.includes(url)) anhSanPham.value.push(url)
    }
    if (!anhSanPham.value.length) anhSanPham.value.push(ANH_MAC_DINH)
    anhDangXem.value = anhSanPham.value[0]
    // Load history
    nhatKyLoading.value = true
    const ls = await getLichSu(group.sanPhamId)
    lichSuHienTai.value = Array.isArray(ls) ? ls : (ls?.content || [])
  } catch (e) {
    console.error('[HangHoa] loi mo chi tiet', e)
  } finally {
    nhatKyLoading.value = false
  }
}

const dongChiTiet = () => { showDetail.value = false; chiTiet.value = null }

const openExportModal = () => { selectedIds.value = []; showExportModal.value = true }

const suaSanPham = async (sp) => {
  modalMode.value = 'edit'
  tieuDeModal.value = 'Sửa sản phẩm'
  tab.value = 'info'

  // Luôn fetch fresh data từ API để tránh stale cache
  let data
  try {
    const res = await get(`/api/san-pham/${sp.sanPhamId}`)
    data = res.data || res
  } catch (e) {
    console.error('[HangHoa] suaSanPham fetch error:', e)
    return
  }

  // Fill form từ API response (data mới nhất từ DB)
  form.value.sanPhamId = data.sanPhamId
  form.value.maSanPham = data.maSanPham
  form.value.tenSanPham = data.tenSanPham
  form.value.thuongHieuId = data.thuongHieuId
  form.value.danhMucId = data.danhMucId
  form.value.nhaCungCapId = data.nhaCungCapId
  form.value.loaiSanPham = data.loaiSanPham || 'LAPTOP'
  form.value.trangThaiSanPham = data.trangThai || 'active'
  form.value.hinhAnhList = data.hinhAnhList || (data.hinhAnhChinh ? [data.hinhAnhChinh] : [])
  form.value.moTa = data.moTa || ''
  form.value.phanLoaiIds = data.phanLoaiIds || []
  form.value.baoHanhThang = data.baoHanhThang ?? 12
  form.value.kichThuocManHinh = data.kichThuocManHinh || ''
  form.value.pin = data.pin || ''
  form.value.heDieuHanh = data.heDieuHanh || ''
  form.value.trongLuongKg = data.trongLuongKg != null ? data.trongLuongKg : ''

  // Bien the — response trả cpu/ram/oCung/gpu là string (tên), cần reverse lookup sang id
  bienTheRows.value = (data.variants || []).map(v => ({
    bienTheId: v.bienTheId,
    maSku: v.maSku,
    barcode: v.barcode,
    mauSac: v.mauSac,
    cpuId: v.cpu ? cpuIdByName.value[v.cpu.trim()] : null,
    ramId: v.ram ? ramIdByName.value[v.ram.trim()] : null,
    oCungId: v.oCung ? oCungIdByName.value[v.oCung.trim()] : null,
    gpuId: v.gpu ? gpuIdByName.value[v.gpu.trim()] : null,
    giaNhap: v.giaNhap,
    giaBan: v.giaBan,
    hinhAnhBienThe: v.hinhAnhBienThe,
    _key: v.bienTheId || Date.now() + Math.random()
  }))
  if (bienTheRows.value.length > 0) {
    suaBienThe(bienTheRows.value[0])
  } else {
    form.value.bienTheId = null
    form.value.maSku = ''
    form.value.barcode = ''
    form.value.mauSac = ''
    form.value.cpuId = ''
    form.value.ramId = ''
    form.value.oCungId = ''
    form.value.gpuId = ''
    form.value.giaNhap = 0
    form.value.giaBan = 0
    form.value.hinhAnhBienThe = null
  }
  banGoc.value = anhChupForm()
  errors.value = {}
  saveError.value = ''
  showModal.value = true
  moLaiChiTiet.value = data.sanPhamId
  // Set moTa editor
  setTimeout(() => { if (moTaEl.value) moTaEl.value.innerHTML = data.moTa || '' }, 50)
}

const suaBienThe = (v) => {
  if (!v) return
  form.value.bienTheId = v.bienTheId
  form.value.maSku = v.maSku || ''
  form.value.barcode = v.barcode || ''
  form.value.mauSac = v.mauSac || ''
  form.value.cpuId = idOf(v, 'cpuId') ?? cpuIdByName.value[v.cpu?.trim()]
  form.value.ramId = idOf(v, 'ramId') ?? ramIdByName.value[(v.ram || '')?.trim()]
  form.value.oCungId = idOf(v, 'oCungId') ?? oCungIdByName.value[(v.oCung || '')?.trim()]
  form.value.gpuId = idOf(v, 'gpuId') ?? gpuIdByName.value[v.gpu?.trim()]
  form.value.giaNhap = v.giaNhap || 0
  form.value.giaBan = v.giaBan || 0
  form.value.hinhAnhBienThe = v.hinhAnhBienThe || null
}

const chonBienTheDeSua = (v) => {
  if (form.value.bienTheId) {
    const prevIdx = bienTheRows.value.findIndex(r => String(r.bienTheId) === String(form.value.bienTheId))
    if (prevIdx !== -1) {
      bienTheRows.value[prevIdx] = {
        ...bienTheRows.value[prevIdx],
        maSku: form.value.maSku,
        barcode: form.value.barcode,
        mauSac: form.value.mauSac,
        cpuId: form.value.cpuId,
        ramId: form.value.ramId,
        oCungId: form.value.oCungId,
        gpuId: form.value.gpuId,
        giaNhap: form.value.giaNhap,
        giaBan: form.value.giaBan,
        hinhAnhBienThe: form.value.hinhAnhBienThe
      }
    }
  }
  suaBienThe(v)
}

const suaBienTheTuChiTiet = (v) => {
  if (!chiTiet.value || !v) return
  suaSanPham(chiTiet.value)
  tab.value = 'bienthe'
  suaBienThe(v)
}

const themPhienBan = (sp) => {
  modalMode.value = 'variant'
  tieuDeModal.value = 'Thêm phiên bản'
  tab.value = 'bienthe'
  form.value.sanPhamId = sp.sanPhamId
  form.value.maSanPham = sp.maSanPham
  form.value.tenSanPham = sp.tenSanPham || ''
  form.value.thuongHieuId = idOf(sp, 'thuongHieuId')
  form.value.danhMucId = idOf(sp, 'danhMucId')
  form.value.nhaCungCapId = idOf(sp, 'nhaCungCapId')
  form.value.loaiSanPham = sp.loaiSanPham || 'LAPTOP'
  form.value.trangThaiSanPham = sp.trangThai || 'active'
  form.value.hinhAnhList = sp.hinhAnhList || (sp.hinhAnhChinh ? [sp.hinhAnhChinh] : [])
  form.value.moTa = sp.moTa || ''
  form.value.phanLoaiIds = sp.phanLoai || []
  form.value.baoHanhThang = sp.baoHanhThang ?? 12
  form.value.kichThuocManHinh = sp.kichThuocManHinh || ''
  form.value.pin = sp.pin || ''
  form.value.heDieuHanh = sp.heDieuHanh || ''
  form.value.trongLuongKg = sp.trongLuongKg != null ? sp.trongLuongKg : ''
  bienTheRows.value = [taoDongBienThe()]
  errors.value = {}
  saveError.value = ''
  showModal.value = true
}

const saoChepSanPham = (sp) => {
  modalMode.value = 'create'
  tieuDeModal.value = 'Sao chép sản phẩm'
  tab.value = 'info'
  form.value = {
    sanPhamId: null, maSanPham: '', tenSanPham: sp.tenSanPham + ' (Copy)',
    thuongHieuId: idOf(sp, 'thuongHieuId'), danhMucId: idOf(sp, 'danhMucId'),
    nhaCungCapId: idOf(sp, 'nhaCungCapId'),
    loaiSanPham: sp.loaiSanPham || 'LAPTOP', trangThaiSanPham: 'active',
    phanLoaiIds: sp.phanLoai || [], phanLoaiTags: null, phanLoaiTen: null,
    hinhAnhList: [...(sp.hinhAnhList || [])],
    moTa: sp.moTa || '', baoHanhThang: 12, kichThuocManHinh: '', pin: '', heDieuHanh: '', trongLuongKg: '', skuPrefix: '',
    bienTheId: null, maSku: '', barcode: '', mauSac: '', cpuId: '', ramId: '', oCungId: '', gpuId: '',
    giaNhap: 0, giaBan: 0, hinhAnhBienThe: null
  }
  bienTheRows.value = (sp.variants || []).map(v => ({
    ...v, _key: Date.now() + Math.random(), bienTheId: null,
    cpuId: idOf(v, 'cpuId'), ramId: idOf(v, 'ramId'),
    oCungId: idOf(v, 'oCungId'), gpuId: idOf(v, 'gpuId')
  }))
  errors.value = {}
  saveError.value = ''
  showModal.value = true
}

const dangSaoChepBienThe = ref(false)
const saoChepBienThe = async (v) => {
  if (!v) return
  dangSaoChepBienThe.value = true
  try {
    const payload = { ...payloadBienThe(chiTiet.value.sanPhamId, { ...v, _key: Date.now(), bienTheId: null, maSku: '' }) }
    payload.maSku = v.maSku + '-copy'
    const res = await apiTaoBienThe(payload)
    if (res.ok) {
      hienToast('Đã sao chép phiên bản')
      await fetchData()
      if (chiTiet.value) moChiTiet({ sanPhamId: chiTiet.value.sanPhamId })
    }
  } catch (e) {
    hienToast('Lỗi: ' + thongBaoLoi(e))
  } finally {
    dangSaoChepBienThe.value = false
  }
}

const inTemMa = (v) => {
  if (!v) return
  const w = window.open('', '_blank')
  if (!w) return
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
  try {
    JsBarcode(svg, v.barcode || v.maSku, { format: 'EAN13', displayValue: true, fontSize: 14, margin: 10 })
    w.document.write(`<html><head><title>Tem mã: ${v.maSku}</title></head><body style="text-align:center;padding:20px;font-family:sans-serif">${svg.outerHTML}<p style="margin-top:10px">${v.maSku}</p></body></html>`)
    w.document.close()
    setTimeout(() => w.print(), 300)
  } catch (e) {
    w.document.write(`<p>Không tạo được mã vạch: ${e.message}</p>`)
    w.document.close()
  }
}

// Validate form
const validate = () => {
  errors.value = {}
  if (!form.value.tenSanPham) errors.value.tenSanPham = 'Tên sản phẩm bắt buộc'
  if (!form.value.thuongHieuId) errors.value.thuongHieuId = 'Chọn thương hiệu'
  if (!form.value.danhMucId) errors.value.danhMucId = 'Chọn danh mục'
  if (!form.value.baoHanhThang) errors.value.baoHanhThang = 'Chọn thời gian bảo hành'

  // Validate bien the
  if (modalMode.value === 'edit') {
    if (form.value.bienTheId) {
      if (!form.value.maSku?.trim()) errors.value.maSku = 'Mã SKU bắt buộc'
      if (form.value.giaBan === '' || form.value.giaBan == null) errors.value.giaBan = 'Giá bán bắt buộc'
      if (form.value.giaNhap === '' || form.value.giaNhap == null) errors.value.giaNhap = 'Giá nhập bắt buộc'
      if (Number(form.value.giaBan) < 0) errors.value.giaBan = 'Giá bán không được âm'
      if (Number(form.value.giaNhap) < 0) errors.value.giaNhap = 'Giá nhập không được âm'
    }
  } else {
    if (bienTheRows.value.length === 0) {
      errors.value.bienThe = 'Phải có ít nhất 1 phiên bản'
    }
    for (const row of bienTheRows.value) {
      if (!row.giaBan && row.giaBan !== 0) errors.value.giaBan = 'Giá bán bắt buộc'
      break
    }
  }
  return Object.keys(errors.value).length === 0
}

const closeModal = (keepDetail = false) => {
  showModal.value = false
  form.value.sanPhamId = null
  bienTheRows.value = []
  errors.value = {}
  saveError.value = ''
  // Chỉ reset moLaiChiTiet khi không giữ lại (tức là đóng hẳn, không phải sau khi lưu)
  if (!keepDetail) {
    moLaiChiTiet.value = null
  }
}

const themDong = () => bienTheRows.value.push(taoDongBienThe())
const xoaDong = (key) => { bienTheRows.value = bienTheRows.value.filter(r => r._key !== key) }

// Toggle phan loai
const togglePhanLoai = (id) => {
  const idx = form.value.phanLoaiIds.indexOf(id)
  if (idx === -1) form.value.phanLoaiIds.push(id)
  else form.value.phanLoaiIds.splice(idx, 1)
}
const tenPhanLoai = (id) => phanLoaiOptions.value.find(p => p.phanLoaiId == id)?.tenPhanLoai || ''
const tenTheoMaPhanLoai = (ma) => phanLoaiOptions.value.find(p => p.maPhanLoai === ma)?.tenPhanLoai || ma

// Ảnh
const chonAnhSanPham = async (e) => {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  dangTaiAnh.value = true
  try {
    for (const file of files) {
      const fd = new FormData()
      fd.append('file', file)
      const res = await fetch(UPLOAD_URL, { method: 'POST', body: fd })
      if (res.ok) {
        const data = await res.json()
        const url = data.url || data.path || data.filename || (THU_MUC_ANH + data.name)
        if (!form.value.hinhAnhList.includes(url)) form.value.hinhAnhList.push(url)
      }
    }
  } catch (e) {
    console.error('[HangHoa] loi tai anh', e)
  } finally {
    dangTaiAnh.value = false
    e.target.value = ''
  }
}
const datLamAnhChinh = (i) => {
  const url = form.value.hinhAnhList.splice(i, 1)[0]
  form.value.hinhAnhList.unshift(url)
}
const xoaAnhTaiViTri = (i) => form.value.hinhAnhList.splice(i, 1)
const themAnhTuUrl = (e) => {
  const url = e.target.value.trim()
  if (url && !form.value.hinhAnhList.includes(url)) form.value.hinhAnhList.push(url)
  e.target.value = ''
}

// Bien the helper
const coThongSoBienThe = (v) => v?.mauSac || layCpu(v) || layRam(v) || layOCung(v) || layGpu(v)
const layCpu = (v) => danhSachCpu.value.find(c => idOf(c, 'cpuId') == v?.cpuId)?.tenCpu || ''
const layRam = (v) => danhSachRam.value.find(r => idOf(r, 'ramId') == v?.ramId)?.dungLuong || ''
const layOCung = (v) => {
  const o = danhSachOCung.value.find(o => idOf(o, 'oCungId') == v?.oCungId)
  return o ? tenOCung(o) : ''
}
const layGpu = (v) => danhSachGpu.value.find(g => idOf(g, 'gpuId') == v?.gpuId)?.tenGpu || ''
const moTaBienThe = (v) => [layCpu(v), layRam(v), layOCung(v), v?.mauSac].filter(Boolean).join(' · ')

// Rich text
const dinhDang = (cmd) => {
  if (!moTaEl.value) return
  moTaEl.value.focus()
  document.execCommand(cmd, false, null)
}
const chenLink = () => {
  const url = prompt('Nhập URL:')
  if (url) dinhDang('createLink', url)
}

// Barcode
const renderBarcode = (el, code) => {
  if (!el || !code) return
  try { JsBarcode(el, code, { format: 'EAN13', displayValue: false, margin: 2 }) } catch (e) {}
}

// Nhat ky: backend đã tự ghi log qua LichSuThayDoiSanPhamService khi SanPhamService.updateSanPham
// nên FE chỉ cần đọc, không cần ghi/xóa.
// Giữ stub ghiNhatKy() để các chỗ gọi cũ không phải sửa; nó không gọi API nào cả.
const ghiNhatKy = async () => {}

// Bản đồ tên trường backend -> tiếng Việt hiển thị
const TEN_TRUONG_LABEL = {
  tenSanPham: 'Tên sản phẩm',
  thuongHieuId: 'Thương hiệu',
  danhMucId: 'Danh mục',
  nhaCungCapId: 'Nhà cung cấp',
  loaiSanPham: 'Loại sản phẩm',
  moTa: 'Mô tả',
  hinhAnhChinh: 'Ảnh đại diện',
  trangThai: 'Trạng thái',
  giaNhap: 'Giá nhập',
  giaBan: 'Giá bán',
  barcode: 'Barcode',
}

// Sinh câu mô tả hành động dựa trên đối tượng + trường thay đổi
const tenHanhDong = (m) => {
  if (!m) return '—'
  const doiTuong = m.doiTuong === 'bien_the' ? 'phiên bản' : 'sản phẩm'
  const truong = TEN_TRUONG_LABEL[m.tenTruong] || m.tenTruong || 'trường'
  // Tạo sản phẩm: giaTriCu rỗng/null
  const isCreate = m.doiTuong === 'san_pham' && (m.giaTriCu === null || m.giaTriCu === '')
  if (isCreate) return `Tạo ${doiTuong} - ${truong}`
  return `Cập nhật ${doiTuong} - ${truong}`
}

// Compare form snapshot
const anhChupForm = () => ({ ...form.value })
const soSanhAnhChup = (a, b) => {
  if (!a || !b) return []
  const changes = []
  const fields = ['tenSanPham', 'moTa', 'trangThaiSanPham']
  for (const f of fields) {
    if (String(a[f] || '') !== String(b[f] || '')) {
      changes.push({ truong: f, cu: a[f], moi: b[f] })
    }
  }
  return changes
}

// Export
const exportCsv = () => {
  const rows = bienTheDaLoc.value.filter(v => selectedIds.value.includes(v.bienTheId))
  if (!rows.length) return
  const csv = [
    ['Mã SP', 'Tên SP', 'SKU', 'Mã vạch', 'Màu sắc', 'CPU', 'RAM', 'Ổ cứng', 'Giá nhập', 'Giá bán', 'Trạng thái'].join(','),
    ...rows.map(v => [v.maSanPham, v.tenSanPham, v.maSku, v.barcode, v.mauSac, layCpu(v), layRam(v), layOCung(v), v.giaNhap, v.giaBan, v.trangThai].map(x => `"${x || ''}"`).join(','))
  ].join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = 'hang_hoa.csv'
  a.click()
  URL.revokeObjectURL(a.href)
  hienToast(`Đã xuất ${rows.length} dòng`)
  showExportModal.value = false
}

const isGroupChecked = (group) => {
  const vIds = (group.variants || []).map(v => v.bienTheId)
  return vIds.length > 0 && vIds.every(id => selectedIds.value.includes(id))
}

const toggleGroupCheck = (group) => {
  const vIds = (group.variants || []).map(v => v.bienTheId)
  const allOn = vIds.every(id => selectedIds.value.includes(id))
  if (allOn) selectedIds.value = selectedIds.value.filter(id => !vIds.includes(id))
  else selectedIds.value = [...new Set([...selectedIds.value, ...vIds])]
}

const toggleVariantCheck = (id) => {
  const idx = selectedIds.value.indexOf(id)
  if (idx === -1) selectedIds.value.push(id)
  else selectedIds.value.splice(idx, 1)
}

const toggleAll = (e) => {
  if (e.target.checked) selectedIds.value = bienTheDaLoc.value.map(v => v.bienTheId)
  else selectedIds.value = []
}

const setIndeterminate = (el, group) => {
  if (!el) return
  const vIds = (group.variants || []).map(v => v.bienTheId)
  const checked = vIds.filter(id => selectedIds.value.includes(id)).length
  el.indeterminate = checked > 0 && checked < vIds.length
}

// formHopLe
const formHopLe = computed(() => {
  if (!form.value.tenSanPham) return false
  if (!form.value.thuongHieuId) return false
  if (!form.value.danhMucId) return false
  if (!form.value.baoHanhThang) return false
  if (modalMode.value !== 'edit' && bienTheRows.value.length === 0) return false
  return true
})

/* ════════════ TẢI DỮ LIỆU ════════════ */
const fetchData = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    // Lấy sản phẩm và biến thể
    const [spRes, btRes] = await Promise.all([
      get('/api/san-pham/hien-thi?size=500'),
      get('/api/bien-the-san-pham/staff')
    ])
    // Xử lý response
    const spRaw = Array.isArray(spRes) ? spRes : (spRes?.content || [])
    bienThe.value = Array.isArray(btRes) ? btRes : []

    // Gộp nhóm theo sanPhamId: backend /api/san-pham/hien-thi trả 1 dòng / biến thể
    // (vì JOIN BienTheSanPham), nên cần dedup để bảng chỉ hiển thị mỗi sản phẩm 1 dòng.
    const seen = new Map()
    for (const row of spRaw) {
      const id = row.sanPhamId
      if (id == null) continue
      if (!seen.has(id)) {
        // Giữ nguyên dòng đầu tiên làm đại diện sản phẩm (mang thông tin chung)
        seen.set(id, { ...row, _mauSacSet: row.mauSac ? new Set([row.mauSac]) : null })
      } else {
        const existed = seen.get(id)
        // Gom màu sắc từ các biến thể
        if (row.mauSac) {
          existed._mauSacSet = existed._mauSacSet || new Set()
          existed._mauSacSet.add(row.mauSac)
        }
      }
    }
    danhSachSanPham.value = Array.from(seen.values()).map((sp) => {
      sp.mauSacList = sp._mauSacSet ? Array.from(sp._mauSacSet) : []
      return sp
    })

    // Merge variants vào groups
    const btMap = {}
    for (const bt of bienThe.value) {
      const spId = bt.sanPhamId
      if (!btMap[spId]) btMap[spId] = []
      btMap[spId].push(bt)
    }
    for (const sp of danhSachSanPham.value) {
      sp.variants = btMap[sp.sanPhamId] || []
      // Tính khoang gia
      const gias = sp.variants.map(v => v.giaBan).filter(Boolean)
      if (gias.length) {
        sp.giaBanMin = Math.min(...gias)
        sp.giaBanMax = Math.max(...gias)
        if (sp.giaBanMin === sp.giaBanMax) sp.khoangGia = formatNumber(sp.giaBanMin)
        else sp.khoangGia = `${formatNumber(sp.giaBanMin)} – ${formatNumber(sp.giaBanMax)}`
      }
      const giaVons = sp.variants.map(v => v.giaNhap).filter(Boolean)
      if (giaVons.length) {
        const gvMin = Math.min(...giaVons), gvMax = Math.max(...giaVons)
        if (gvMin === gvMax) sp.khoangGiaVon = formatNumber(gvMin)
        else sp.khoangGiaVon = `${formatNumber(gvMin)} – ${formatNumber(gvMax)}`
      }
      // Lay mau sac
      sp.mauSacList = [...new Set(sp.variants.map(v => v.mauSac).filter(Boolean))]
      // Lay hinh anh
      sp.hinhAnh = sp.hinhAnhChinh || (sp.variants[0]?.hinhAnhBienThe) || ANH_MAC_DINH
    }
    page.value = 1
  } catch (e) {
    console.error('[HangHoa] fetchData error:', e)
    loadError.value = 'Tải danh sách thất bại: ' + thongBaoLoi(e)
  } finally {
    isLoading.value = false
  }
}

const fetchMasterData = async () => {
  const an = (p) => p.then((r) => r ?? []).catch(() => [])
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

// Gom nhóm danh sách biến thể theo sản phẩm

// Chuẩn bị thuộc tính chung của sản phẩm
const phanChungBienThe = () => ({
  baoHanhThang: Number(form.value.baoHanhThang || 0),
  kichThuocManHinh: form.value.kichThuocManHinh || null,
  heDieuHanh: form.value.heDieuHanh || null,
  pin: form.value.pin || null,
  trongLuongKg: soHoacNull(form.value.trongLuongKg),
  phanLoaiTags: form.value.phanLoaiTags || null,
  phanLoaiTen: form.value.phanLoaiTen || null,
  trangThai: form.value.trangThaiSanPham
})

// Dữ liệu tạo sản phẩm và biến thể chính
const payloadSanPham = (row) => ({
  ...(form.value.sanPhamId ? { sanPhamId: Number(form.value.sanPhamId) } : {}),
  maSanPham: form.value.maSanPham || null,
  tenSanPham: form.value.tenSanPham,
  thuongHieuId: soHoacNull(form.value.thuongHieuId),
  danhMucId: soHoacNull(form.value.danhMucId),
  nhaCungCapId: soHoacNull(form.value.nhaCungCapId),
  loaiSanPham: form.value.loaiSanPham,
  moTa: form.value.moTa || null,
  hinhAnhChinh: form.value.hinhAnhList?.[0] || null,
  hinhAnhList: form.value.hinhAnhList?.length ? form.value.hinhAnhList : null,
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
        hinhAnhBienThe: row.hinhAnhBienThe || form.value.hinhAnhBienThe || null
      }
    : {})
})

// Dữ liệu tạo các biến thể tiếp theo
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
  hinhAnhBienThe: row.hinhAnhBienThe || form.value.hinhAnhBienThe || null,
  ...phanChungBienThe()
})

const layId = (res, key) => res?.[key] ?? res?.id ?? res?.data?.[key] ?? res?.data?.id ?? null

// Tìm lại ID sản phẩm vừa tạo
const timIdVuaTao = async () => {
  for (const kw of [form.value.maSanPham, form.value.tenSanPham]) {
    if (!kw) continue
    try {
      const rows = toArray(await sanPhamApi.getPage({ page: 0, size: 50, keyword: kw }))
      const khop =
        rows.find((r) => r.maSanPham && r.maSanPham === form.value.maSanPham) ||
        rows.find((r) => r.tenSanPham === form.value.tenSanPham)
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

// Cập nhật bảng liên kết phân loại sản phẩm
const luuPhanLoai = async (sanPhamId) => {
  if (!sanPhamId) return
  try {
    await put(`/api/phan-loai/san-pham/${sanPhamId}`, (form.value.phanLoaiIds || []).map(Number))
  } catch (e) {
    console.warn('[Hàng hóa] không lưu được phân loại:', e?.response?.data ?? e)
  }
}

// Lưu thông tin sản phẩm và các biến thể
const submitForm = async () => {
  saveError.value = ''
  console.log('[DEBUG submitForm] mode=', modalMode.value, 'formHopLe=', formHopLe.value, 'sanPhamId=', form.value.sanPhamId, 'bienTheId=', form.value.bienTheId, 'nhaCungCapId=', form.value.nhaCungCapId)
  if (!validate()) {
    console.log('[DEBUG submitForm] validate fail', errors.value)
    saveError.value = 'Vui lòng sửa các ô được đánh dấu.'
    tab.value = errors.value.bienThe || errors.value.maSku || errors.value.barcode || errors.value.giaBan || errors.value.giaNhap ? 'bienthe' : 'info'
    return
  }

  isSaving.value = true
  let buoc = 'chuẩn bị dữ liệu'
  let daTao = 0

  try {
    if (modalMode.value === 'edit') {
      buoc = 'cập nhật sản phẩm'
      const rowBienThe = form.value.bienTheId
        ? {
            bienTheId: form.value.bienTheId,
            maSku: form.value.maSku,
            barcode: form.value.barcode,
            mauSac: form.value.mauSac,
            cpuId: form.value.cpuId,
            ramId: form.value.ramId,
            oCungId: form.value.oCungId,
            gpuId: form.value.gpuId,
            giaNhap: form.value.giaNhap,
            giaBan: form.value.giaBan,
            hinhAnhBienThe: form.value.hinhAnhBienThe
          }
        : (bienTheRows.value[0] || {})

      const payload = payloadSanPham(rowBienThe)
      console.log('[DEBUG submitForm] PUT payload', JSON.stringify(payload, null, 2))
      let resSp
      try {
        resSp = await apiSuaSanPham(form.value.sanPhamId, payload)
        console.log('[DEBUG submitForm] PUT response ok=', resSp?.ok, 'status=', resSp?.status, 'body type=', typeof resSp?.json, typeof resSp?.text)
      } catch(e) {
        console.error('[DEBUG submitForm] PUT error:', e)
        throw e
      }
      console.log('[DEBUG submitForm] after check')
      if (resSp && typeof resSp.ok === 'boolean' && !resSp.ok) {
        const errText = await resSp.text().catch(() => '')
        let msg = errText
        try {
          const json = JSON.parse(errText)
          msg = json.message || json.error || json.detail || (Array.isArray(json.errors) ? json.errors.map(x => x.defaultMessage || x.message).join('; ') : '') || errText
        } catch {}
        throw new Error(`HTTP ${resSp.status}: ${msg || resSp.statusText}`)
      }
      console.log('[DEBUG submitForm] Saving to DB, calling luuPhanLoai')
      await luuPhanLoai(form.value.sanPhamId)
      console.log('[DEBUG submitForm] luuPhanLoai done')

      // Cập nhật lại bienTheRows sau khi lưu
      const idx = bienTheRows.value.findIndex(r => String(r.bienTheId) === String(form.value.bienTheId))
      if (idx !== -1) {
        bienTheRows.value[idx] = {
          ...bienTheRows.value[idx],
          maSku: form.value.maSku,
          barcode: form.value.barcode,
          mauSac: form.value.mauSac,
          cpuId: form.value.cpuId,
          ramId: form.value.ramId,
          oCungId: form.value.oCungId,
          gpuId: form.value.gpuId,
          giaNhap: form.value.giaNhap,
          giaBan: form.value.giaBan,
          hinhAnhBienThe: form.value.hinhAnhBienThe
        }
      }

      const thayDoi = soSanhAnhChup(banGoc.value, anhChupForm())
      ghiNhatKy(form.value.sanPhamId, {
        loai: 'sua',
        hanhDong: thayDoi.length ? 'Cập nhật sản phẩm' : 'Lưu lại (không đổi nội dung)',
        doiTuong: `Phiên bản ${form.value.maSku || (rowBienThe && rowBienThe.maSku) || ''}`,
        thayDoi
      })
      console.log('[DEBUG submitForm] Calling hienToast and closeModal')
      hienToast('Đã lưu thay đổi')
      // Giữ moLaiChiTiet để fetchData xong rồi mở lại chi tiết
      const sanPhamIdVuaLuu = moLaiChiTiet.value
      closeModal()
      // Sau khi fetch data thành công, mở lại chi tiết
      moLaiChiTiet.value = sanPhamIdVuaLuu
    } else if (modalMode.value === 'variant') {
      // Sinh SKU tự động cho các dòng để trống
      bienTheRows.value.forEach((row, i) => {
        if (!row.maSku) {
          const prefix = (form.value.skuPrefix || form.value.maSanPham || vietTat(form.value.tenSanPham, 6) || 'SP').toUpperCase()
          row.maSku = `${prefix}-${String(i + 1).padStart(3, '0')}`
        }
      })
      const dsSku = []
      for (const row of bienTheRows.value) {
        buoc = `thêm phiên bản ${row.maSku}`
        const resBt = await apiTaoBienThe(payloadBienThe(form.value.sanPhamId, row))
        if (!resBt.ok) {
          throw new Error(`HTTP ${resBt.status}: ${await resBt.text().catch(() => resBt.statusText)}`)
        }
        dsSku.push(row.maSku)
        daTao++
      }
      ghiNhatKy(form.value.sanPhamId, {
        loai: 'them',
        hanhDong: `Thêm ${daTao} phiên bản`,
        doiTuong: dsSku.join(', ')
      })
      hienToast(`Đã thêm ${daTao} phiên bản`)
      closeModal()
      moLaiChiTiet.value = null // Đảm bảo reset cho variant mode
    } else {
      // Sinh SKU tự động cho các dòng để trống
      bienTheRows.value.forEach((row, i) => {
        if (!row.maSku) {
          const prefix = (form.value.skuPrefix || form.value.maSanPham || vietTat(form.value.tenSanPham, 6) || 'SP').toUpperCase()
          row.maSku = `${prefix}-${String(i + 1).padStart(3, '0')}`
        }
      })
      const [dauTien, ...conLai] = bienTheRows.value

      buoc = 'tạo sản phẩm chính'
      const resSp = await apiTaoSanPham(payloadSanPham(dauTien))
      if (resSp && typeof resSp.ok === 'boolean' && !resSp.ok) {
        const errText = await resSp.text().catch(() => '')
        let msg = errText
        try {
          const json = JSON.parse(errText)
          msg = json.message || json.error || json.detail || (Array.isArray(json.errors) ? json.errors.map(x => x.defaultMessage || x.message).join('; ') : '') || errText
        } catch {}
        throw new Error(`HTTP ${resSp.status}: ${msg || resSp.statusText}`)
      }
      const spMoi = resSp && typeof resSp.json === 'function' ? await resSp.json() : resSp
      daTao = 1

      let spId = layId(spMoi, 'sanPhamId')
      if (!spId && (conLai.length || form.value.phanLoaiIds.length)) {
        buoc = 'tra lại mã sản phẩm vừa tạo'
        spId = await timIdVuaTao()
        if (!spId && conLai.length) {
          throw new Error(
            'Sản phẩm và phiên bản đầu tiên đã lưu, nhưng không lấy được sanPhamId nên các phiên bản ' +
            'còn lại chưa tạo được. Mở lại sản phẩm rồi dùng nút "Thêm phiên bản" để bổ sung, ' +
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
        doiTuong: `${form.value.maSanPham} · ${daTao} phiên bản`,
        thayDoi: [{ truong: 'Tên sản phẩm', cu: '', moi: form.value.tenSanPham }]
      })
      // Đóng cửa sổ chi tiết sau khi tạo mới
      hienToast(`Đã lưu sản phẩm cùng ${daTao} phiên bản`)
      closeModal()
    }

    await fetchData()

    // Mở lại cửa sổ chi tiết để xem ngay kết quả vừa lưu
    if (moLaiChiTiet.value) {
      const g = danhSachSanPham.value.find((x) => String(x.sanPhamId) === String(moLaiChiTiet.value))
      if (g) {
        moChiTiet(g)
        tabCT.value = 'lichsu'
      }
      moLaiChiTiet.value = null
    }

    // Đồng bộ dữ liệu vào ProductsStore
    lamMoiKhoDuLieuChung().catch(() => {})

    // Đồng bộ dữ liệu vào InventoryStore
    lamMoiTonKhoDuLieuChung().catch(() => {})
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

  /* 3D Shadow Variables */
  --sh-1: 0 1px 2px rgba(168, 27, 93, .08), 0 1px 3px rgba(168, 27, 93, .05);
  --sh-2: 0 4px 6px rgba(168, 27, 93, .1), 0 2px 4px rgba(168, 27, 93, .06);
  --sh-3: 0 10px 15px rgba(168, 27, 93, .12), 0 4px 6px rgba(168, 27, 93, .08);
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
  transition: all 0.15s ease;
}
.hh-btn--sm { padding: 5px 11px; font-size: 12.5px; }
.hh-btn--primary {
  background: var(--pink-600); color: #fff;
  box-shadow: 0 3px 0 #9b1d5c, 0 4px 8px rgba(168, 27, 93, 0.3);
  border-bottom-width: 3px;
}
.hh-btn--primary:hover:not(:disabled) {
  background: var(--pink-700);
  box-shadow: 0 4px 0 #7a1550, 0 6px 12px rgba(168, 27, 93, 0.35);
  transform: translateY(-1px);
}
.hh-btn--primary:active:not(:disabled) {
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.2);
  transform: translateY(1px);
}
.hh-btn--soft { background: var(--pink-100); color: var(--pink-700); border-color: var(--pink-200); }
.hh-btn--soft:hover:not(:disabled) {
  background: var(--pink-200);
  box-shadow: 0 2px 4px rgba(168, 27, 93, 0.15);
}
.hh-btn--ghost { background: #fff; color: var(--pink-700); border-color: var(--pink-200); }
.hh-btn--ghost:hover:not(:disabled) {
  background: var(--pink-50); border-color: var(--pink-300);
  box-shadow: 0 2px 4px rgba(168, 27, 93, 0.15);
}
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

/* Thanh công cụ */
.hh-sticky-head {
  position: sticky; top: 0; z-index: 5;
  transition: transform .25s ease;
}
.hh-sticky-head.is-hidden { transform: translateY(-100%); }

.hh-bar {
  display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
  background: #fff; border: 1px solid var(--line); border-radius: 14px;
  padding: 12px 16px; margin-bottom: 12px; box-shadow: var(--sh-2);
}
.hh-bar__left { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.hh-bar__actions { display: flex; align-items: center; gap: 8px; margin-left: auto; flex-wrap: wrap; }
.hh-title { margin: 0; font-size: 20px; font-weight: 800; letter-spacing: -.2px; color: var(--pink-700); white-space: nowrap; }

.hh-search { position: relative; width: 320px; max-width: 100%; }
.hh-search input {
  width: 100%; padding: 8px 32px 8px 34px;
  border: 1px solid var(--pink-200); border-radius: 999px;
  font-size: 13px; background: var(--pink-50); font-family: inherit; color: var(--ink);
  box-shadow: inset 0 2px 4px rgba(168, 27, 93, 0.1);
  transition: all 0.2s ease;
}
.hh-search input:focus {
  outline: none; border-color: var(--pink-500); background: #fff;
  box-shadow: inset 0 2px 4px rgba(168, 27, 93, 0.1), 0 0 0 3px var(--pink-100);
}
.hh-search__icon { position: absolute; left: 13px; top: 50%; transform: translateY(-50%); color: var(--pink-500); }
.hh-search__clear { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); background: none; border: none; color: var(--muted); cursor: pointer; }

/* ═══════════ BỘ LỌC (nằm trong card) ═══════════ */
.hh-filter { display: grid; grid-template-rows: 0fr; transition: grid-template-rows .25s ease; }
.hh-filter.is-open { grid-template-rows: 1fr; }
.hh-filter__panel {
  overflow: hidden; background: var(--pink-50);
  padding: 0 16px; transition: padding .25s ease;
}
.hh-filter.is-open .hh-filter__panel { padding: 14px 16px; }

/* Thanh tác vụ */
.hh-toolbar {
  display: flex !important;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 16px !important;
  background: #fff !important;
  border-bottom: 1px solid var(--pink-50) !important;
}

.hh-toolbar__left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1 1 auto;
  min-width: 0;
}

.hh-toolbar__right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-left: auto;
}

.hh-toolbar__count {
  font-size: 12.5px;
  color: var(--muted);
  font-weight: 600;
  white-space: nowrap;
}

.hh-toolbar .hh-search {
  width: 260px;
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
.hh-card { background: #fff; border: 1px solid var(--line); border-radius: 14px; overflow: hidden; box-shadow: var(--sh-2); }
.hh-table-wrap { position: relative; overflow-x: auto; min-height: 140px; }
.hh-table { width: 100%; border-collapse: collapse; table-layout: auto; }
.hh-table th {
  background: var(--pink-50); color: var(--pink-700);
  font-size: 11.5px; font-weight: 800; text-align: left; text-transform: uppercase; letter-spacing: .4px;
  padding: 11px 12px; white-space: nowrap; border-bottom: none;
}
/* Bo góc viền bảng */
.hh-table thead th:first-child { border-top-left-radius: 13px; }
.hh-table thead th:last-child { border-top-right-radius: 13px; }
.hh-table td { padding: 11px 12px; border-bottom: 1px solid var(--line); vertical-align: middle; white-space: nowrap; }
.hh-table tbody tr:last-child td { border-bottom: none; }

.hh-row { cursor: pointer; transition: background-color .12s; }
.hh-row:hover { background: var(--pink-50); }
.hh-row:focus-visible { outline: 2px solid var(--pink-500); outline-offset: -2px; }
.hh-row:hover .hh-col-go { color: var(--pink-600); }

.hh-col-ma { width: 12%; }
.hh-col-ten { width: 30%; }
.hh-col-go { width: 2%; text-align: center; color: var(--pink-200); }
.hh-td-ma { font-weight: 700; }
.hh-td-gia { font-variant-numeric: tabular-nums; font-weight: 600; }
.hh-td-ngay { font-size: 12.5px; }

/* tên dài thì xuống dòng, dãn tự nhiên theo khung */
.hh-td-ten { white-space: normal; }
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
.hh-tag--wait { background: #fef3c7; color: #92400e; }

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
/* Căn lề trên cố định cho modal */
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
.hh-vt__barcode { white-space: nowrap; width: 140px; }
.hh-barcode-card {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3px 8px 3px;
  background: #fff;
  border: 1px solid var(--pink-200);
  border-radius: 9px;
  box-shadow: 0 1px 2px rgba(168, 27, 93, 0.04);
  transition: border-color .15s, box-shadow .15s, transform .12s;
  user-select: none;
}
.hh-barcode-card:hover {
  border-color: var(--pink-300);
  box-shadow: 0 2px 6px rgba(168, 27, 93, 0.08);
  transform: translateY(-1px);
}
.hh-barcode-card svg {
  display: block;
  max-width: 100%;
  height: auto;
}
.hh-vt__cfg { color: var(--muted); min-width: 260px; }
.hh-cfg-chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  max-width: 520px;
}
.hh-cfg-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--pink-100);
  color: var(--pink-700);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
  white-space: nowrap;
  transition: background .15s, transform .12s;
  user-select: none;
}
.hh-cfg-chip:hover {
  background: var(--pink-200);
  transform: translateY(-1px);
}
.hh-cfg-chip svg {
  flex-shrink: 0;
  opacity: 0.9;
}
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

/* Danh sách biến thể trong form sửa */
.hh-bienthe-list {
  display: flex; flex-direction: column; gap: 10px;
}
.hh-bienthe-list__title {
  font-size: 12.5px; font-weight: 700; color: var(--pink-700);
  text-transform: uppercase; letter-spacing: .5px;
}
.hh-bienthe-list__items {
  border: 1px solid var(--line); border-radius: 12px; overflow: hidden; background: #fff;
}
.hh-bienthe-item {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 10px 14px; cursor: pointer; transition: background-color .12s;
  border-bottom: 1px solid var(--line);
}
.hh-bienthe-item:last-child { border-bottom: none; }
.hh-bienthe-item:hover { background: var(--pink-50); }
.hh-bienthe-item.is-on { background: var(--pink-100); }
.hh-bienthe-item.is-on .hh-bienthe-item__sku { color: var(--pink-700); }
.hh-bienthe-item__info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.hh-bienthe-item__sku {
  font-family: ui-monospace, "SFMono-Regular", Menlo, monospace;
  font-size: 12.5px; font-weight: 700; color: var(--ink);
}
.hh-bienthe-item__cfg { font-size: 11.5px; color: var(--muted); }
.hh-bienthe-item__price { font-weight: 600; color: var(--ink); white-space: nowrap; font-size: 13px; }
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
/* ma trận phiên thể (bảng mỗi dòng = 1 biến thể) */
.hh-rows-wrap { border: 1px solid var(--line); border-radius: 12px; overflow: auto; max-height: 380px; }
.hh-rows { width: 100%; border-collapse: collapse; }
.hh-rows th {
  position: sticky; top: 0; background: var(--pink-50); color: var(--pink-700); z-index: 1;
  font-size: 11px; font-weight: 800; text-align: left; padding: 9px 10px; white-space: nowrap;
  text-transform: uppercase; letter-spacing: .4px; border-bottom: none;
}
.hh-rows thead th:first-child { border-top-left-radius: 11px; }
.hh-rows thead th:last-child { border-top-right-radius: 11px; }
.hh-rows td { padding: 6px 8px; border-bottom: 1px solid var(--line); font-size: 13px; vertical-align: middle; }
.hh-rows tr:last-child td { border-bottom: none; }
.hh-rows__stt { width: 36px; color: var(--muted); }
.hh-rows__empty { text-align: center; color: var(--muted); padding: 28px; }
.hh-cell--sel { padding: 6px 8px; font-size: 12.5px; }

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