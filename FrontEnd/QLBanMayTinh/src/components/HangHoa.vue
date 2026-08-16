<template>
  <div class="hh">
    <!-- ══════════════ THANH CÔNG CỤ ══════════════ -->
    <header class="hh-bar">
      <div class="hh-bar__left">
        <h1 class="hh-title">Hàng hóa</h1>

        <div class="hh-search">
          <i class="fa fa-search hh-search__icon"></i>
          <input type="text" v-model="searchKeyword" placeholder="Tìm theo mã sản phẩm, tên, SKU, barcode" />
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

        <button class="hh-btn hh-btn--ghost" disabled title="Cần thêm API import ở backend">
          <i class="fa fa-upload"></i> Nhập file
        </button>

        <button class="hh-btn hh-btn--ghost" :disabled="!bienTheDaLoc.length" @click="exportCsv">
          <i class="fa fa-download"></i> Xuất file
          <span v-if="selectedIds.length" class="hh-chip">{{ selectedIds.length }}</span>
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
            <span>Tồn kho</span>
            <select v-model="filters.tonKho">
              <option value="">Tất cả</option>
              <option value="con">Còn hàng</option>
              <option value="sap_het">Sắp hết (≤ 5)</option>
              <option value="het">Hết hàng</option>
            </select>
          </label>

          <label class="hh-field">
            <span>Giá bán từ</span>
            <input type="number" min="0" step="100000" v-model="filters.giaTu" placeholder="0" />
          </label>

          <label class="hh-field">
            <span>Giá bán đến</span>
            <input type="number" min="0" step="100000" v-model="filters.giaDen" placeholder="Không giới hạn" />
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

    <!-- ══════════════ BẢNG DỮ LIỆU ══════════════ -->
    <section class="hh-card">
      <p v-if="loadError" class="hh-alert">
        {{ loadError }}
        <button class="hh-link" @click="fetchData">Thử lại</button>
      </p>

      <div class="hh-table-wrap">
        <table class="hh-table">
          <thead>
            <tr>
              <th class="hh-col-check"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
              <th class="hh-col-caret"></th>
              <th>Mã sản phẩm</th>
              <th>Tên sản phẩm</th>
              <th class="ta-r">Giá bán</th>
              <th class="ta-r">Giá vốn</th>
              <th class="ta-r">Tồn kho</th>
              <th class="ta-r">Khách đặt</th>
              <th>Trạng thái</th>
              <th>Ngày tạo</th>
              <th>Ngày cập nhật</th>
              <th></th>
            </tr>
          </thead>

          <tbody v-for="group in pagedGroups" :key="group.sanPhamId">
            <!-- ---- Dòng sản phẩm chính ---- -->
            <tr class="hh-row-group" :class="{ 'is-open': openedGroupId === group.sanPhamId }" @click="toggleGroup(group.sanPhamId)">
              <td @click.stop><input type="checkbox" :checked="isGroupChecked(group)" @change="toggleGroupCheck(group)" /></td>
              <td><i class="fa hh-caret" :class="openedGroupId === group.sanPhamId ? 'fa-chevron-down' : 'fa-chevron-right'"></i></td>
              <td>
                <div class="hh-code">
                  <span class="hh-code__main">{{ group.maSanPham }}</span>
                  <span v-if="group.barcode" class="hh-code__sub"><i class="fa fa-barcode"></i> {{ group.barcode }}</span>
                </div>
              </td>
              <td>
                <div class="hh-name">
                  <img :src="group.hinhAnh" class="hh-thumb" alt="" @error="onImgError" />
                  <div>
                    <div class="hh-name__main">{{ group.tenSanPham }}</div>
                    <div class="hh-name__sub">
                      {{ group.tenThuongHieu }}<template v-if="group.variants.length"> · {{ group.variants.length }} phiên bản</template>
                    </div>
                  </div>
                </div>
              </td>
              <td class="ta-r">{{ group.khoangGia }}</td>
              <td class="ta-r hh-muted">{{ group.khoangGiaVon }}</td>
              <td class="ta-r"><strong>{{ formatNumber(group.tongTon) }}</strong></td>
              <td class="ta-r">{{ formatNumber(group.tongDat) }}</td>
              <td><span class="hh-tag" :class="tagClass(group.trangThai)">{{ nhanTrangThai(group.trangThai) }}</span></td>
              <td class="hh-muted">{{ formatDate(group.ngayTao) }}</td>
              <td class="hh-muted">{{ formatDate(group.ngayCapNhat) }}</td>
              <td @click.stop>
                <button class="hh-btn hh-btn--ghost hh-btn--sm" @click="xemChiTietDayDu(group.sanPhamId)">
                  <i class="fa fa-arrow-up-right-from-square"></i> Chi tiết
                </button>
              </td>
            </tr>

            <!-- ---- Các phiên bản bên trong ---- -->
            <template v-if="openedGroupId === group.sanPhamId">
              <template v-for="item in group.variants" :key="item.bienTheId">
                <tr class="hh-row-variant" :class="{ 'is-open': openedVariantId === item.bienTheId }" @click="toggleVariant(item.bienTheId)">
                  <td @click.stop><input type="checkbox" :checked="selectedIds.includes(item.bienTheId)" @change="toggleOne(item.bienTheId)" /></td>
                  <td></td>
                  <td>
                    <div class="hh-code hh-code--indent">
                      <img :src="item.hinhAnh || group.hinhAnh" class="hh-thumb hh-thumb--sm" alt="" @error="onImgError" />
                      <span class="hh-code__sku">{{ item.maSku }}</span>
                    </div>
                  </td>
                  <td>{{ item.tenPhienBan }}</td>
                  <td class="ta-r"><strong>{{ formatNumber(item.giaBan) }}</strong></td>
                  <td class="ta-r hh-muted">{{ formatNumber(item.giaVon) }}</td>
                  <td class="ta-r" :class="{ 'hh-danger': item.tonKho === 0 }">{{ formatNumber(item.tonKho) }}</td>
                  <td class="ta-r">{{ formatNumber(item.khachDat) }}</td>
                  <td><span class="hh-tag" :class="tagClass(item.trangThai)">{{ nhanTrangThai(item.trangThai) }}</span></td>
                  <td class="hh-muted">{{ formatDate(item.ngayTao) }}</td>
                  <td></td>
                  <td></td>
                </tr>

                <!-- ---- Chi tiết phiên bản ---- -->
                <tr v-if="openedVariantId === item.bienTheId" class="hh-row-detail">
                  <td colspan="12">
                    <div class="hh-detail">
                      <div class="hh-detail__head">
                        <img :src="item.hinhAnh || group.hinhAnh" class="hh-detail__img" alt="" @error="onImgError" />
                        <div class="hh-detail__intro">
                          <h3>{{ group.tenSanPham }}</h3>
                          <div class="hh-detail__tags">
                            <span class="hh-tag hh-tag--soft">{{ group.maSanPham }}</span>
                            <span v-if="item.mauSac" class="hh-tag hh-tag--soft">{{ item.mauSac }}</span>
                            <span v-if="item.tenCpu" class="hh-tag hh-tag--soft">{{ item.tenCpu }}</span>
                            <span v-if="item.tenRam" class="hh-tag hh-tag--soft">{{ item.tenRam }}</span>
                            <span v-if="item.tenOCung" class="hh-tag hh-tag--soft">{{ item.tenOCung }}</span>
                            <span v-for="pl in group.phanLoai" :key="pl" class="hh-tag hh-tag--outline">{{ pl }}</span>
                          </div>
                          <div class="hh-detail__desc" v-html="group.moTa || '<em>Chưa có mô tả cho sản phẩm này.</em>'"></div>
                        </div>
                      </div>

                      <dl class="hh-specs">
                        <div><dt>Mã sản phẩm</dt><dd>{{ group.maSanPham }}</dd></div>
                        <div><dt>Barcode</dt><dd>{{ group.barcode || 'Chưa gán' }}</dd></div>
                        <div><dt>Mã SKU</dt><dd>{{ item.maSku }}</dd></div>
                        <div><dt>Giá vốn</dt><dd>{{ formatNumber(item.giaVon) }} ₫</dd></div>
                        <div><dt>Giá bán</dt><dd>{{ formatNumber(item.giaBan) }} ₫</dd></div>
                        <div><dt>Bảo hành</dt><dd>{{ item.baoHanhThang ?? '—' }} tháng</dd></div>
                        <div><dt>Tồn kho</dt><dd>{{ formatNumber(item.tonKho) }}</dd></div>
                        <div><dt>Khách đặt</dt><dd>{{ formatNumber(item.khachDat) }}</dd></div>
                        <div><dt>CPU</dt><dd>{{ item.tenCpu || '—' }}</dd></div>
                        <div><dt>RAM</dt><dd>{{ item.tenRam || '—' }}</dd></div>
                        <div><dt>Ổ cứng</dt><dd>{{ item.tenOCung || '—' }}</dd></div>
                        <div><dt>GPU</dt><dd>{{ item.tenGpu || '—' }}</dd></div>
                        <div><dt>Màn hình</dt><dd>{{ item.kichThuocManHinh || '—' }}</dd></div>
                        <div><dt>Hệ điều hành</dt><dd>{{ item.heDieuHanh || '—' }}</dd></div>
                        <div><dt>Pin</dt><dd>{{ item.pin || '—' }}</dd></div>
                        <div><dt>Trọng lượng</dt><dd>{{ item.trongLuongKg ? item.trongLuongKg + ' kg' : '—' }}</dd></div>
                        <div><dt>Thương hiệu</dt><dd>{{ group.tenThuongHieu || '—' }}</dd></div>
                        <div><dt>Nhà cung cấp</dt><dd>{{ group.tenNhaCungCap || '—' }}</dd></div>
                      </dl>

                      <div class="hh-detail__foot">
                        <button class="hh-btn hh-btn--ghost hh-btn--sm" @click.stop="openAddVariant(group)">
                          <i class="fa fa-plus"></i> Thêm phiên bản
                        </button>
                        <button class="hh-btn hh-btn--primary hh-btn--sm" @click.stop="openEdit(group, item)">
                          <i class="fa fa-edit"></i> Chỉnh sửa
                        </button>
                        <button class="hh-btn hh-btn--ghost hh-btn--sm" @click.stop="xemChiTietDayDu(group.sanPhamId)">
                          <i class="fa fa-arrow-up-right-from-square"></i> Trang chi tiết
                        </button>
                      </div>
                    </div>
                  </td>
                </tr>
              </template>
            </template>
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

    <!-- ══════════════ MODAL THÊM / SỬA ══════════════ -->
    <teleport to="body">
      <div v-if="showModal" class="hh-modal-mask" @click.self="closeModal">
        <div class="hh-modal" role="dialog" aria-modal="true">
          <header class="hh-modal__head">
            <div>
              <h2>{{ tieuDeModal }}</h2>
              <p>
                <span class="hh-tag hh-tag--soft">{{ form.maSanPham || 'Chưa có mã' }}</span>
                <span v-if="form.tenSanPham"> · {{ form.tenSanPham }}</span>
              </p>
            </div>
            <button class="hh-icon-btn" @click="closeModal" aria-label="Đóng"><i class="fa fa-times"></i></button>
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
                    <span>Mã sản phẩm <b>*</b></span>
                    <div class="hh-inline">
                      <input v-model.trim="form.maSanPham" placeholder="VD: SP0012" />
                      <button type="button" class="hh-btn hh-btn--ghost hh-btn--sm" @click="form.maSanPham = sinhMaSanPham()">
                        <i class="fa fa-magic"></i> Tự sinh
                      </button>
                    </div>
                    <em v-if="errors.maSanPham" class="hh-err">{{ errors.maSanPham }}</em>
                  </label>

                  <label class="hh-field">
                    <span>Barcode</span>
                    <input v-model.trim="form.barcode" placeholder="8–13 chữ số, VD: 8934567000121" />
                    <em v-if="errors.barcode" class="hh-err">{{ errors.barcode }}</em>
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
                    <div class="hh-inline">
                      <select v-model="chon.phanLoai">
                        <option value="">-- Chọn phân loại --</option>
                        <option v-for="pl in phanLoaiConLai" :key="pl.phanLoaiId" :value="pl.phanLoaiId">{{ pl.tenPhanLoai }}</option>
                      </select>
                      <button type="button" class="hh-btn hh-btn--ghost hh-btn--sm" :disabled="!chon.phanLoai" @click="themPhanLoai()">
                        <i class="fa fa-plus"></i> Thêm
                      </button>
                    </div>
                    <div v-if="form.phanLoaiIds.length" class="hh-tags">
                      <span v-for="id in form.phanLoaiIds" :key="id" class="hh-tag-pill">
                        {{ tenPhanLoai(id) }}
                        <button type="button" @click="xoaPhanLoai(id)" aria-label="Bỏ phân loại">&times;</button>
                      </span>
                    </div>
                    <em class="hh-hint">
                      Tag: {{ form.phanLoaiTags || '—' }} · Tên: {{ form.phanLoaiTen || '—' }}
                    </em>
                  </div>

                  <div class="hh-field hh-field--wide">
                    <span>Ảnh đại diện sản phẩm</span>
                    <div class="hh-upload">
                      <div class="hh-upload__preview">
                        <img v-if="anhXemTruoc || form.hinhAnhChinh" :src="anhXemTruoc || form.hinhAnhChinh" alt="" @error="onImgError" />
                        <i v-else class="fa fa-image"></i>
                      </div>
                      <div class="hh-upload__body">
                        <input ref="fileEl" type="file" accept="image/*" class="hh-hidden" @change="chonAnhSanPham" />
                        <div class="hh-inline">
                          <button type="button" class="hh-btn hh-btn--ghost hh-btn--sm" @click="fileEl?.click()">
                            <i class="fa fa-upload"></i> Chọn ảnh từ máy
                          </button>
                          <button v-if="form.hinhAnhChinh" type="button" class="hh-btn hh-btn--ghost hh-btn--sm" @click="xoaAnh">
                            <i class="fa fa-trash"></i> Bỏ ảnh
                          </button>
                        </div>
                        <input v-model.trim="form.hinhAnhChinh" class="hh-mt6" placeholder="Hoặc dán đường dẫn: /images/ten-anh.webp" />
                        <em class="hh-hint">{{ ghiChuAnh }}</em>
                      </div>
                    </div>
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
              <!-- Sửa 1 phiên bản -->
              <fieldset v-if="modalMode === 'edit'" class="hh-block">
                <legend>Phiên bản đang sửa</legend>
                <div class="hh-grid">
                  <label class="hh-field">
                    <span>Mã SKU <b>*</b></span>
                    <input v-model.trim="form.maSku" placeholder="VD: DELL-3520-I5-8G" />
                    <em v-if="errors.maSku" class="hh-err">{{ errors.maSku }}</em>
                  </label>
                  <label class="hh-field">
                    <span>Màu sắc</span>
                    <input v-model.trim="form.mauSac" placeholder="VD: Đen" />
                  </label>
                  <label class="hh-field">
                    <span>CPU</span>
                    <select v-model="form.cpuId">
                      <option value="">-- Không chọn --</option>
                      <option v-for="cpu in danhSachCpu" :key="idOf(cpu, 'cpuId')" :value="idOf(cpu, 'cpuId')">{{ cpu.tenCpu }}</option>
                    </select>
                  </label>
                  <label class="hh-field">
                    <span>RAM</span>
                    <select v-model="form.ramId">
                      <option value="">-- Không chọn --</option>
                      <option v-for="ram in danhSachRam" :key="idOf(ram, 'ramId')" :value="idOf(ram, 'ramId')">
                        {{ ram.dungLuong || ram.tenRam }}
                      </option>
                    </select>
                  </label>
                  <label class="hh-field">
                    <span>Ổ cứng</span>
                    <select v-model="form.oCungId">
                      <option value="">-- Không chọn --</option>
                      <option v-for="oc in danhSachOCung" :key="idOf(oc, 'oCungId')" :value="idOf(oc, 'oCungId')">{{ tenOCung(oc) }}</option>
                    </select>
                  </label>
                  <label class="hh-field">
                    <span>GPU</span>
                    <select v-model="form.gpuId">
                      <option value="">-- Không chọn --</option>
                      <option v-for="gpu in danhSachGpu" :key="idOf(gpu, 'gpuId')" :value="idOf(gpu, 'gpuId')">{{ gpu.tenGpu }}</option>
                    </select>
                  </label>
                  <label class="hh-field">
                    <span>Giá nhập (₫) <b>*</b></span>
                    <input type="number" min="0" step="1000" v-model="form.giaNhap" />
                    <em v-if="errors.giaNhap" class="hh-err">{{ errors.giaNhap }}</em>
                  </label>
                  <label class="hh-field">
                    <span>Giá bán (₫) <b>*</b></span>
                    <input type="number" min="0" step="1000" v-model="form.giaBan" />
                    <em v-if="errors.giaBan" class="hh-err">{{ errors.giaBan }}</em>
                  </label>
                </div>
              </fieldset>

              <!-- Sinh nhiều phiên bản -->
              <template v-else>
                <fieldset class="hh-block">
                  <legend>Thuộc tính dùng để trộn ra phiên bản</legend>
                  <p class="hh-note hh-note--plain">
                    Chọn nhiều giá trị cho mỗi thuộc tính, hệ thống ghép chúng lại thành các phiên bản bên dưới.
                    Không chọn gì thì sản phẩm chỉ có một phiên bản duy nhất.
                  </p>

                  <div class="hh-grid">
                    <div class="hh-field">
                      <span>Màu sắc — gõ rồi nhấn Enter</span>
                      <input
                        v-model.trim="chon.mauSac"
                        placeholder="VD: Đen ↵ Bạc ↵ Xanh Dương"
                        @keydown.enter.prevent="themMau()"
                        @keydown.delete="xoaMauCuoi"
                      />
                      <div v-if="form.mauSacList.length" class="hh-tags">
                        <span v-for="m in form.mauSacList" :key="m" class="hh-tag-pill">
                          {{ m }}
                          <button type="button" @click="xoaMau(m)" aria-label="Bỏ màu">&times;</button>
                        </span>
                      </div>
                    </div>

                    <div v-for="attr in thuocTinhTron" :key="attr.field" class="hh-field">
                      <span>{{ attr.label }}</span>
                      <div class="hh-inline">
                        <select v-model="chon[attr.field]">
                          <option value="">-- Chọn {{ attr.label.toLowerCase() }} --</option>
                          <option v-for="o in attr.options()" :key="o.id" :value="o.id">{{ o.ten }}</option>
                        </select>
                        <button type="button" class="hh-btn hh-btn--ghost hh-btn--sm" :disabled="!chon[attr.field]" @click="themThuocTinh(attr.field)">
                          <i class="fa fa-plus"></i>
                        </button>
                      </div>
                      <div v-if="form[attr.field].length" class="hh-tags">
                        <span v-for="id in form[attr.field]" :key="id" class="hh-tag-pill">
                          {{ attr.ten(id) }}
                          <button type="button" @click="xoaThuocTinh(attr.field, id)" aria-label="Bỏ giá trị">&times;</button>
                        </span>
                      </div>
                    </div>
                  </div>
                </fieldset>

                <fieldset class="hh-block">
                  <legend>Thông số chung cho mọi phiên bản</legend>
                  <div class="hh-grid">
                    <label class="hh-field">
                      <span>Màn hình</span>
                      <select v-model="form.kichThuocManHinh">
                        <option value="">-- Không chọn --</option>
                        <option v-for="v in optManHinh" :key="v" :value="v">{{ v }}</option>
                      </select>
                    </label>
                    <label class="hh-field">
                      <span>Pin</span>
                      <select v-model="form.pin">
                        <option value="">-- Không chọn --</option>
                        <option v-for="v in optPin" :key="v" :value="v">{{ v }}</option>
                      </select>
                    </label>
                    <label class="hh-field">
                      <span>Hệ điều hành</span>
                      <select v-model="form.heDieuHanh">
                        <option value="">-- Không chọn --</option>
                        <option v-for="v in optHeDieuHanh" :key="v" :value="v">{{ v }}</option>
                      </select>
                    </label>
                    <label class="hh-field">
                      <span>Trọng lượng (kg)</span>
                      <input type="number" step="0.01" min="0" v-model="form.trongLuongKg" placeholder="VD: 1.70" />
                    </label>
                    <label class="hh-field">
                      <span>Bảo hành (tháng) <b>*</b></span>
                      <input type="number" min="0" step="1" v-model="form.baoHanhThang" />
                      <em v-if="errors.baoHanhThang" class="hh-err">{{ errors.baoHanhThang }}</em>
                    </label>
                    <label class="hh-field">
                      <span>Giá nhập mặc định (₫) <b>*</b></span>
                      <input type="number" min="0" step="1000" v-model="form.giaNhap" />
                      <em v-if="errors.giaNhap" class="hh-err">{{ errors.giaNhap }}</em>
                    </label>
                    <label class="hh-field">
                      <span>Giá bán mặc định (₫) <b>*</b></span>
                      <input type="number" min="0" step="1000" v-model="form.giaBan" />
                      <em v-if="errors.giaBan" class="hh-err">{{ errors.giaBan }}</em>
                    </label>
                    <label class="hh-field">
                      <span>Tiền tố mã SKU</span>
                      <input v-model.trim="form.skuPrefix" placeholder="Để trống sẽ lấy theo mã sản phẩm" />
                    </label>
                  </div>
                </fieldset>

                <fieldset class="hh-block">
                  <legend>
                    Các phiên bản sẽ được tạo
                    <span class="hh-chip">{{ bienTheRows.length }}</span>
                  </legend>

                  <div class="hh-inline hh-mb8">
                    <button type="button" class="hh-btn hh-btn--ghost hh-btn--sm" @click="dungLaiMaTran">
                      <i class="fa fa-refresh"></i> Dựng lại danh sách
                    </button>
                    <button type="button" class="hh-btn hh-btn--ghost hh-btn--sm" @click="apGiaChoTatCa">
                      <i class="fa fa-money"></i> Áp giá mặc định cho tất cả
                    </button>
                  </div>

                  <em v-if="errors.bienThe" class="hh-err hh-mb8">{{ errors.bienThe }}</em>

                  <div class="hh-matrix-wrap">
                    <table class="hh-matrix">
                      <thead>
                        <tr>
                          <th>#</th>
                          <th>Mã SKU</th>
                          <th>Cấu hình</th>
                          <th class="ta-r">Giá nhập</th>
                          <th class="ta-r">Giá bán</th>
                          <th></th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(row, i) in bienTheRows" :key="row.key">
                          <td class="hh-muted">{{ i + 1 }}</td>
                          <td><input v-model.trim="row.maSku" class="hh-cell hh-cell--sku" /></td>
                          <td class="hh-matrix__cfg">{{ moTaCauHinh(row) || 'Phiên bản tiêu chuẩn' }}</td>
                          <td><input type="number" min="0" step="1000" v-model="row.giaNhap" class="hh-cell ta-r" /></td>
                          <td><input type="number" min="0" step="1000" v-model="row.giaBan" class="hh-cell ta-r" /></td>
                          <td>
                            <button type="button" class="hh-icon-btn" title="Bỏ phiên bản này" @click="xoaDong(row.key)">
                              <i class="fa fa-times"></i>
                            </button>
                          </td>
                        </tr>
                        <tr v-if="!bienTheRows.length">
                          <td colspan="6" class="hh-matrix__empty">Chưa có phiên bản nào — bấm “Dựng lại danh sách”.</td>
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
            <button type="button" class="hh-btn hh-btn--ghost" @click="closeModal">Bỏ qua</button>
            <div class="hh-modal__foot-right">
              <button
                v-if="modalMode !== 'edit'"
                type="button"
                class="hh-btn hh-btn--soft"
                :disabled="isSaving"
                @click="submitForm(true)"
              >
                <i class="fa fa-copy"></i> Lưu và sao chép
              </button>
              <button type="button" class="hh-btn hh-btn--primary" :disabled="isSaving" @click="submitForm(false)">
                <i class="fa" :class="isSaving ? 'fa-spinner fa-spin' : 'fa-check'"></i>
                {{ isSaving ? 'Đang lưu…' : 'Lưu' }}
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
import { useRouter } from 'vue-router'

import { get, put } from '@/services/api.js'
import { getThuongHieu, getNhaCungCap, getCpu, getRam, getOCung, getGpu } from '@/services/DmService.js'
import * as bienTheApi from '@/services/bienTheSanPhamService.js'
import * as sanPhamApi from '@/services/sanPhamService.js'

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

const ANH_MAC_DINH = 'https://cdn-icons-png.flaticon.com/512/664/664457.png'
const NGUONG_SAP_HET = 5
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
const tagClass = (tt) => (tt === 'active' ? 'hh-tag--ok' : 'hh-tag--off')
const onImgError = (e) => { e.target.src = ANH_MAC_DINH }
const khongDau = (s) =>
  String(s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[đĐ]/g, 'd').toLowerCase()
const vietTat = (s, n = 4) => khongDau(s).replace(/[^a-z0-9]/g, '').toUpperCase().slice(0, n)
const tenOCung = (oc) => oc?.loaiOCung || oc?.loaiOcung || oc?.ten || ''

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
const filters = reactive({
  trangThai: '', thuongHieuId: '', nhaCungCapId: '', phanLoai: '',
  cpuId: '', ramId: '', mauSac: '', tonKho: '', giaTu: '', giaDen: ''
})

const openedGroupId = ref(null)
const openedVariantId = ref(null)
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
const tra = (map, id) => (id == null || id === '' ? '' : map.get(String(id)) || '')

const phanLoaiOptions = computed(() => (danhSachPhanLoai.value.length ? danhSachPhanLoai.value : PHAN_LOAI_DU_PHONG))

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
        barcode: sp.barcode || '',
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
        variants: []
      })
    }
    const g = map.get(key)
    g.variants.push({ ...v, tenPhienBan: [g.tenSanPham, v.mauSac, v.tenCpu, v.tenRam].filter(Boolean).join(' · ') })
  })

  return [...map.values()].map((g) => {
    const tags = g.variants[0]?.phanLoaiTags
    return {
      ...g,
      phanLoai: tags ? tags.split(',').map((t) => t.trim()).filter(Boolean) : [],
      tongTon: g.variants.reduce((s, v) => s + v.tonKho, 0),
      tongDat: g.variants.reduce((s, v) => s + v.khachDat, 0),
      khoangGia: khoangGia(g.variants.map((v) => v.giaBan)),
      khoangGiaVon: khoangGia(g.variants.map((v) => v.giaVon))
    }
  })
})

/* ════════════ TÌM KIẾM + LỌC ════════════ */
const soBoLocDangDung = computed(() => Object.values(filters).filter((v) => v !== '' && v !== null).length)
const coBoLoc = computed(() => !!searchKeyword.value || soBoLocDangDung.value > 0)

const khopTuKhoa = (group, v) => {
  const kw = khongDau(searchKeyword.value.trim())
  if (!kw) return true
  return [group.maSanPham, group.barcode, group.tenSanPham, v.maSku, v.mauSac, v.tenCpu].some((f) => khongDau(f).includes(kw))
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
  if (filters.tonKho === 'het' && v.tonKho !== 0) return false
  if (filters.tonKho === 'sap_het' && (v.tonKho === 0 || v.tonKho > NGUONG_SAP_HET)) return false
  if (filters.tonKho === 'con' && v.tonKho <= 0) return false
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

/* ════════════ MỞ RỘNG DÒNG & CHỌN ════════════ */
const toggleGroup = (id) => {
  openedGroupId.value = openedGroupId.value === id ? null : id
  openedVariantId.value = null
}
const toggleVariant = (id) => { openedVariantId.value = openedVariantId.value === id ? null : id }

const toggleOne = (id) => {
  const i = selectedIds.value.indexOf(id)
  if (i === -1) selectedIds.value.push(id)
  else selectedIds.value.splice(i, 1)
}
const isGroupChecked = (g) => g.variants.length > 0 && g.variants.every((v) => selectedIds.value.includes(v.bienTheId))
const toggleGroupCheck = (g) => {
  const ids = g.variants.map((v) => v.bienTheId)
  selectedIds.value = isGroupChecked(g)
    ? selectedIds.value.filter((id) => !ids.includes(id))
    : [...new Set([...selectedIds.value, ...ids])]
}
const allChecked = computed(() => bienTheDaLoc.value.length > 0 && bienTheDaLoc.value.every(({ v }) => selectedIds.value.includes(v.bienTheId)))
const toggleAll = () => { selectedIds.value = allChecked.value ? [] : bienTheDaLoc.value.map(({ v }) => v.bienTheId) }

const resetFilters = () => {
  Object.keys(filters).forEach((k) => (filters[k] = ''))
  searchKeyword.value = ''
}

/* ════════════ XUẤT FILE CSV ════════════ */
const exportCsv = () => {
  const rows = selectedIds.value.length
    ? bienTheDaLoc.value.filter(({ v }) => selectedIds.value.includes(v.bienTheId))
    : bienTheDaLoc.value

  const cols = ['Mã sản phẩm', 'Barcode', 'Tên sản phẩm', 'Mã SKU', 'Màu sắc', 'CPU', 'RAM', 'Ổ cứng', 'GPU',
    'Màn hình', 'Giá vốn', 'Giá bán', 'Bảo hành (tháng)', 'Tồn kho', 'Khách đặt', 'Trạng thái', 'Thương hiệu', 'Nhà cung cấp']
  const esc = (val) => `"${String(val ?? '').replace(/"/g, '""')}"`
  const lines = [cols.map(esc).join(',')]

  rows.forEach(({ g, v }) => {
    lines.push([g.maSanPham, g.barcode, g.tenSanPham, v.maSku, v.mauSac, v.tenCpu, v.tenRam, v.tenOCung, v.tenGpu,
      v.kichThuocManHinh, v.giaVon, v.giaBan, v.baoHanhThang, v.tonKho, v.khachDat,
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
 *                    MODAL THÊM / SỬA
 * ══════════════════════════════════════════════════════════ */
const showModal = ref(false)
const modalMode = ref('create') // 'create' | 'edit' | 'variant'
const tab = ref('info')
const isSaving = ref(false)
const saveError = ref('')
const errors = reactive({})
const fileEl = ref(null)
const moTaEl = ref(null)
const anhXemTruoc = ref('')
const ghiChuAnh = ref('Ảnh tải lên sẽ được lưu về server; nếu backend chưa có API upload, hệ thống dùng đường dẫn /images/<tên file>.')

const tabs = [
  { key: 'info', label: 'Thông tin' },
  { key: 'bienthe', label: 'Phiên bản' },
  { key: 'mota', label: 'Mô tả' }
]

const formRong = () => ({
  sanPhamId: null,
  bienTheId: null,
  // san_pham
  maSanPham: '', barcode: '', tenSanPham: '',
  thuongHieuId: '', danhMucId: '', nhaCungCapId: '',
  loaiSanPham: 'LAPTOP', trangThaiSanPham: 'active',
  moTa: '', hinhAnhChinh: '', phanLoaiIds: [], phanLoaiTags: '', phanLoaiTen: '',
  // thông số chung của phiên bản
  giaNhap: 0, giaBan: 0, baoHanhThang: 24,
  kichThuocManHinh: '', heDieuHanh: 'Windows 11 Home', pin: '', trongLuongKg: '',
  skuPrefix: '',
  // thuộc tính trộn ra phiên bản
  mauSacList: [], ramIds: [], cpuIds: [], oCungIds: [], gpuIds: [],
  // chỉ dùng khi sửa 1 phiên bản
  maSku: '', mauSac: '', cpuId: '', ramId: '', oCungId: '', gpuId: '', hinhAnhBienThe: ''
})

const form = reactive(formRong())
const chon = reactive({ mauSac: '', ramIds: '', cpuIds: '', oCungIds: '', gpuIds: '', phanLoai: '' })
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

/* ─── Thuộc tính dùng để trộn ─── */
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

const themThuocTinh = (field) => {
  const val = chon[field]
  if (val !== '' && !form[field].includes(val)) form[field].push(val)
  chon[field] = ''
}
const xoaThuocTinh = (field, id) => { form[field] = form[field].filter((x) => x !== id) }

const themMau = () => {
  const v = chon.mauSac.trim()
  if (v && !form.mauSacList.includes(v)) form.mauSacList.push(v)
  chon.mauSac = ''
}
const xoaMau = (m) => { form.mauSacList = form.mauSacList.filter((x) => x !== m) }
const xoaMauCuoi = (e) => {
  if (!chon.mauSac && form.mauSacList.length && e.key === 'Backspace') form.mauSacList.pop()
}

/* ─── Phân loại ─── */
const tenPhanLoai = (id) => phanLoaiOptions.value.find((p) => String(p.phanLoaiId) === String(id))?.tenPhanLoai || id
const maPhanLoai = (id) => phanLoaiOptions.value.find((p) => String(p.phanLoaiId) === String(id))?.maPhanLoai || ''
const phanLoaiConLai = computed(() => phanLoaiOptions.value.filter((p) => !form.phanLoaiIds.includes(p.phanLoaiId)))
const themPhanLoai = () => {
  if (chon.phanLoai !== '' && !form.phanLoaiIds.includes(chon.phanLoai)) form.phanLoaiIds.push(chon.phanLoai)
  chon.phanLoai = ''
}
const xoaPhanLoai = (id) => { form.phanLoaiIds = form.phanLoaiIds.filter((x) => x !== id) }

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
  const file = e.target.files?.[0]
  if (!file) return
  anhXemTruoc.value = URL.createObjectURL(file)
  try {
    form.hinhAnhChinh = await uploadAnh(file)
    ghiChuAnh.value = 'Đã tải ảnh lên server.'
  } catch {
    form.hinhAnhChinh = THU_MUC_ANH + file.name
    ghiChuAnh.value = `Chưa có API upload — đã đặt đường dẫn ${form.hinhAnhChinh}. Hãy chép file ảnh vào thư mục public${THU_MUC_ANH} của FrontEnd.`
  }
  e.target.value = ''
}

const xoaAnh = () => {
  form.hinhAnhChinh = ''
  anhXemTruoc.value = ''
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

/* ─── Sinh mã sản phẩm ─── */
const sinhMaSanPham = () => {
  const so = danhSachSanPham.value
    .map((p) => Number(String(p.maSanPham || '').replace(/\D/g, '')))
    .filter((n) => !Number.isNaN(n) && n > 0)
  return 'SP' + String((so.length ? Math.max(...so) : 0) + 1).padStart(4, '0')
}

/* ─── Ma trận phiên bản ─── */
const moTaCauHinh = (row) =>
  [row.mauSac, tra(mapCpu.value, row.cpuId), tra(mapRam.value, row.ramId), tra(mapOCung.value, row.oCungId), tra(mapGpu.value, row.gpuId)]
    .filter(Boolean)
    .join(' · ')

const khoaDong = (c) => [c.mauSac, c.cpuId, c.ramId, c.oCungId, c.gpuId].join('|')

const sinhSku = (c, index) => {
  const prefix = (form.skuPrefix || form.maSanPham || vietTat(form.tenSanPham, 6) || 'SP').toUpperCase()
  const phan = [
    vietTat(tra(mapCpu.value, c.cpuId).split(' ').pop(), 6),
    vietTat(tra(mapRam.value, c.ramId), 5),
    vietTat(tra(mapOCung.value, c.oCungId), 5),
    vietTat(c.mauSac, 3)
  ].filter(Boolean)
  return [prefix, ...phan].join('-') || `${prefix}-${index + 1}`
}

/** Trộn các thuộc tính đã chọn thành danh sách phiên bản.
 *  Giữ nguyên SKU/giá mà người dùng đã sửa tay (khớp theo khóa cấu hình). */
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
  bienTheRows.value = tohop.map((c, i) => {
    const key = khoaDong(c)
    const truoc = cu.get(key)
    return {
      key,
      mauSac: c.mauSac,
      cpuId: c.cpuId,
      ramId: c.ramId,
      oCungId: c.oCungId,
      gpuId: c.gpuId,
      maSku: truoc?.maSku || sinhSku(c, i),
      giaNhap: truoc?.giaNhap ?? form.giaNhap,
      giaBan: truoc?.giaBan ?? form.giaBan
    }
  })

  if (vuot) hienToast(`Chỉ giữ ${TOI_DA_BIEN_THE} phiên bản đầu — bớt bớt thuộc tính lại nhé`)
}

const xoaDong = (key) => { bienTheRows.value = bienTheRows.value.filter((r) => r.key !== key) }
const apGiaChoTatCa = () => {
  bienTheRows.value.forEach((r) => {
    r.giaNhap = form.giaNhap
    r.giaBan = form.giaBan
  })
}

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
  anhXemTruoc.value = ''
  saveError.value = ''
  tab.value = 'info'
  if (moTaEl.value) moTaEl.value.innerHTML = form.moTa || ''
}

const openCreate = () => {
  resetForm({ maSanPham: sinhMaSanPham() })
  modalMode.value = 'create'
  showModal.value = true
  dungLaiMaTran()
}

const openAddVariant = (group) => {
  resetForm({
    sanPhamId: group.sanPhamId,
    maSanPham: group.maSanPham,
    barcode: group.barcode,
    tenSanPham: group.tenSanPham,
    thuongHieuId: group.thuongHieuId ?? '',
    danhMucId: group.danhMucId ?? '',
    nhaCungCapId: group.nhaCungCapId ?? '',
    loaiSanPham: group.loaiSanPham,
    trangThaiSanPham: group.trangThai,
    moTa: group.moTa,
    hinhAnhChinh: group.hinhAnh === ANH_MAC_DINH ? '' : group.hinhAnh,
    giaNhap: group.variants[0]?.giaVon ?? 0,
    giaBan: group.variants[0]?.giaBan ?? 0,
    baoHanhThang: group.variants[0]?.baoHanhThang ?? 24,
    kichThuocManHinh: group.variants[0]?.kichThuocManHinh || '',
    heDieuHanh: group.variants[0]?.heDieuHanh || 'Windows 11 Home',
    pin: group.variants[0]?.pin || ''
  })
  modalMode.value = 'variant'
  tab.value = 'bienthe'
  showModal.value = true
  dungLaiMaTran()
}

// Mo trang chi tiet san pham (route /admin/san-pham/:id) ngay trong tab hien tai.
const router = useRouter()
const xemChiTietDayDu = (sanPhamId) => {
  router.push(`/admin/san-pham/${sanPhamId}`);
};

const openEdit = (group, item) => {
  resetForm({
    sanPhamId: group.sanPhamId,
    bienTheId: item.bienTheId,
    maSanPham: group.coMaThat ? group.maSanPham : '',
    barcode: group.barcode,
    tenSanPham: group.tenSanPham,
    thuongHieuId: group.thuongHieuId ?? '',
    danhMucId: group.danhMucId ?? '',
    nhaCungCapId: group.nhaCungCapId ?? '',
    loaiSanPham: group.loaiSanPham,
    trangThaiSanPham: group.trangThai,
    moTa: group.moTa,
    hinhAnhChinh: group.hinhAnh === ANH_MAC_DINH ? '' : group.hinhAnh,
    maSku: item.maSku === '—' ? '' : item.maSku,
    mauSac: item.mauSac,
    giaNhap: item.giaVon,
    giaBan: item.giaBan,
    baoHanhThang: item.baoHanhThang ?? 24,
    cpuId: item.cpuId ?? '',
    ramId: item.ramId ?? '',
    oCungId: item.oCungId ?? '',
    gpuId: item.gpuId ?? '',
    kichThuocManHinh: item.kichThuocManHinh,
    heDieuHanh: item.heDieuHanh,
    pin: item.pin,
    trongLuongKg: item.trongLuongKg ?? '',
    hinhAnhBienThe: item.hinhAnh
  })
  modalMode.value = 'edit'
  showModal.value = true

  // Tích sẵn các phân loại sản phẩm đang có
  get(`/api/phan-loai/san-pham/${group.sanPhamId}`)
    .then((ids) => { form.phanLoaiIds = (Array.isArray(ids) ? ids : []).map(Number) })
    .catch(() => {})
}

const closeModal = () => {
  showModal.value = false
  resetForm()
}

/* ─── Kiểm tra trước khi gửi ─── */
const validate = () => {
  Object.keys(errors).forEach((k) => delete errors[k])
  const laVariant = modalMode.value === 'variant'
  const skuDaCo = new Set(bienTheChuan.value.filter((v) => String(v.bienTheId) !== String(form.bienTheId)).map((v) => v.maSku))

  if (!laVariant) {
    if (!form.tenSanPham) errors.tenSanPham = 'Nhập tên sản phẩm'
    if (!form.maSanPham) errors.maSanPham = 'Nhập hoặc bấm tự sinh mã'
    if (!form.thuongHieuId) errors.thuongHieuId = 'Chọn thương hiệu'
    if (!form.danhMucId) errors.danhMucId = 'Chọn danh mục'

    const khac = (p) => String(idOf(p, 'sanPhamId')) !== String(form.sanPhamId)
    if (form.maSanPham && danhSachSanPham.value.some((p) => p.maSanPham === form.maSanPham && khac(p)))
      errors.maSanPham = 'Mã này đã có sản phẩm khác dùng'
    if (form.barcode && danhSachSanPham.value.some((p) => p.barcode === form.barcode && khac(p)))
      errors.barcode = 'Barcode này đã có sản phẩm khác dùng'
    if (form.barcode && !/^\d{8,13}$/.test(form.barcode)) errors.barcode = 'Barcode chỉ gồm 8–13 chữ số'
  }

  const nhap = Number(form.giaNhap)
  const ban = Number(form.giaBan)
  if (!(nhap >= 0)) errors.giaNhap = 'Giá nhập không hợp lệ'
  if (!(ban >= 0)) errors.giaBan = 'Giá bán không hợp lệ'
  else if (ban < nhap * 0.5) errors.giaBan = 'Giá bán phải ≥ 50% giá nhập (ràng buộc của CSDL)'
  if (!(Number(form.baoHanhThang) >= 0)) errors.baoHanhThang = 'Số tháng bảo hành không hợp lệ'

  if (modalMode.value === 'edit') {
    if (!form.maSku) errors.maSku = 'Nhập mã SKU'
    else if (skuDaCo.has(form.maSku)) errors.maSku = 'SKU này đã tồn tại'
  } else {
    if (!bienTheRows.value.length) errors.bienThe = 'Chưa có phiên bản nào để lưu'
    else {
      const trung = bienTheRows.value.filter((r) => !r.maSku || skuDaCo.has(r.maSku))
      const trungTrongForm = bienTheRows.value.length !== new Set(bienTheRows.value.map((r) => r.maSku)).size
      if (trung.length) errors.bienThe = `SKU trống hoặc đã tồn tại: ${trung.map((r) => r.maSku || '(trống)').join(', ')}`
      else if (trungTrongForm) errors.bienThe = 'Có hai phiên bản trùng mã SKU'
      else {
        const saiGia = bienTheRows.value.find((r) => Number(r.giaBan) < Number(r.giaNhap) * 0.5)
        if (saiGia) errors.bienThe = `Phiên bản ${saiGia.maSku}: giá bán phải ≥ 50% giá nhập`
      }
    }
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
  barcode: form.barcode || null,
  tenSanPham: form.tenSanPham,
  thuongHieuId: soHoacNull(form.thuongHieuId),
  danhMucId: soHoacNull(form.danhMucId),
  nhaCungCapId: soHoacNull(form.nhaCungCapId),
  loaiSanPham: form.loaiSanPham,
  moTa: form.moTa || null,
  hinhAnhChinh: form.hinhAnhChinh || null,
  ngayTao: bayGio(),
  ...phanChungBienThe(),
  ...(row
    ? {
        ...(row.bienTheId ? { bienTheId: Number(row.bienTheId) } : {}),
        maSku: row.maSku,
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
  const chiTiet =
    (typeof d === 'string' && d) ||
    d?.message || d?.error || d?.detail ||
    (Array.isArray(d?.errors) ? d.errors.map((x) => x.defaultMessage || x.message).join('; ') : '') ||
    e?.message || 'Không rõ nguyên nhân'
  return (res?.status ? `HTTP ${res.status} — ` : '') + chiTiet
}

/** Dịch lỗi SQL/JPA hay gặp thành việc cần làm. */
const goiYSua = (msg) => {
  const m = khongDau(msg)
  if (m.includes('401') || m.includes('403') || m.includes('unauthorized') || m.includes('denied'))
    return 'API tạo sản phẩm yêu cầu quyền ADMIN / NHAN_VIEN / QUAN_KHO — đăng nhập lại bằng tài khoản nhân viên.'
  if (m.includes('ma_sku')) return 'Mã SKU trống hoặc trùng — mỗi phiên bản phải có SKU riêng.'
  if (m.includes('gia_nhap') || m.includes('gia_ban')) return 'Giá nhập/giá bán chưa được gửi lên hoặc âm.'
  if (m.includes('ck_bt_giaban_hop_ly')) return 'Giá bán phải ≥ 50% giá nhập.'
  if (m.includes('ck_sp_loaisanpham')) return 'Loại sản phẩm chỉ nhận LAPTOP, PHU_KIEN, DIEN_THOAI.'
  if (m.includes('trangthai')) return 'Trạng thái biến thể chỉ nhận active hoặc inactive.'
  if (m.includes('unique') || m.includes('duplicate')) return 'Mã sản phẩm, barcode hoặc SKU bị trùng với bản ghi đã có.'
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
const submitForm = async (saoChep) => {
  saveError.value = ''
  if (!validate()) {
    saveError.value = 'Vui lòng sửa các ô được đánh dấu.'
    tab.value = errors.bienThe || errors.maSku ? 'bienthe' : 'info'
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
      hienToast('Đã lưu thay đổi')
      closeModal()
    } else if (modalMode.value === 'variant') {
      for (const row of bienTheRows.value) {
        buoc = `thêm phiên bản ${row.maSku}`
        await apiTaoBienThe(payloadBienThe(form.sanPhamId, row))
        daTao++
      }
      hienToast(`Đã thêm ${daTao} phiên bản`)
      if (saoChep) chuanBiBanSao()
      else closeModal()
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
        await apiTaoBienThe(payloadBienThe(spId, row))
        daTao++
      }

      await luuPhanLoai(spId)
      hienToast(`Đã lưu sản phẩm cùng ${daTao} phiên bản`)
      if (saoChep) chuanBiBanSao()
      else closeModal()
    }

    await fetchData()
  } catch (e) {
    console.error(`[Hàng hóa] lỗi ở bước "${buoc}":`, e?.response?.data ?? e)
    const chiTiet = thongBaoLoi(e)
    const goiY = goiYSua(chiTiet)
    saveError.value =
      `Lưu thất bại ở bước ${buoc}: ${chiTiet}` +
      (goiY ? ` → ${goiY}` : '') +
      (daTao ? ` (đã lưu được ${daTao} bản ghi trước đó)` : '')
    if (buoc.includes('phiên bản')) tab.value = 'bienthe'
    await fetchData()
  } finally {
    isSaving.value = false
  }
}

/** Giữ lại toàn bộ dữ liệu vừa nhập, chỉ xóa những thứ buộc phải duy nhất:
 *  mã sản phẩm, barcode, id và mã SKU của từng phiên bản. */
const chuanBiBanSao = () => {
  modalMode.value = 'create'
  form.sanPhamId = null
  form.bienTheId = null
  form.barcode = ''
  form.maSanPham = sinhMaSanPham()
  form.tenSanPham = form.tenSanPham + ' (bản sao)'
  bienTheRows.value = bienTheRows.value.map((r, i) => ({ ...r, maSku: sinhSku(r, i) }))
  tab.value = 'info'
  saveError.value = ''
  hienToast('Đã sao chép dữ liệu sang form mới — nhớ đổi lại tên và mã cho đúng')
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
  --muted:   #6b7280;
  --line:    #f1dbe6;
  --field:   #d9b3c6;
  --danger:  #dc2626;
  --ok-bg:   #ecfdf5;
  --ok-text: #047857;
}
.hh { font-size: 14px; color: var(--ink); }

.ta-r { text-align: right; }
.hh-muted { color: var(--muted); }
.hh-danger { color: var(--danger); font-weight: 600; }
.hh-hidden { display: none; }
.hh-mt6 { margin-top: 6px; }
.hh-mb8 { margin-bottom: 8px; }

/* ═══════════ NÚT ═══════════ */
.hh-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 14px; border-radius: 999px;
  border: 1px solid transparent;
  font-size: 13px; font-weight: 500; font-family: inherit;
  cursor: pointer; white-space: nowrap;
  transition: background-color .15s, border-color .15s, color .15s;
}
.hh-btn--sm { padding: 5px 11px; font-size: 12.5px; }
.hh-btn--primary { background: var(--pink-600); color: #fff; }
.hh-btn--primary:hover:not(:disabled) { background: var(--pink-700); }
.hh-btn--soft { background: var(--pink-100); color: var(--pink-700); border-color: var(--pink-200); }
.hh-btn--soft:hover:not(:disabled) { background: var(--pink-200); }
.hh-btn--ghost { background: #fff; color: var(--pink-700); border-color: var(--pink-200); }
.hh-btn--ghost:hover:not(:disabled) { background: var(--pink-50); border-color: var(--pink-300); }
.hh-btn--ghost.is-on { background: var(--pink-100); border-color: var(--pink-300); }
.hh-btn:disabled { opacity: .5; cursor: not-allowed; }
.hh-btn:focus-visible, .hh-icon-btn:focus-visible { outline: 2px solid var(--pink-500); outline-offset: 2px; }

.hh-icon-btn {
  background: transparent; border: none; color: var(--muted);
  width: 32px; height: 32px; border-radius: 50%; cursor: pointer;
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

/* ═══════════ THANH CÔNG CỤ ═══════════ */
.hh-bar {
  display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
  background: #fff; border: 1px solid var(--line); border-radius: 12px;
  padding: 12px 16px; margin-bottom: 12px;
}
.hh-bar__left { display: flex; align-items: center; gap: 14px; }
.hh-bar__actions { display: flex; align-items: center; gap: 8px; margin-left: auto; flex-wrap: wrap; }
.hh-title { margin: 0; font-size: 20px; font-weight: 700; color: var(--pink-700); white-space: nowrap; }

.hh-search { position: relative; width: 300px; max-width: 100%; }
.hh-search input {
  width: 100%; padding: 8px 32px 8px 34px;
  border: 1px solid var(--pink-200); border-radius: 999px;
  font-size: 13px; background: var(--pink-50); font-family: inherit;
}
.hh-search input:focus { outline: none; border-color: var(--pink-500); background: #fff; }
.hh-search__icon { position: absolute; left: 13px; top: 50%; transform: translateY(-50%); color: var(--pink-500); }
.hh-search__clear { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); background: none; border: none; color: var(--muted); cursor: pointer; }

/* ═══════════ BỘ LỌC ═══════════ */
.hh-filter { display: grid; grid-template-rows: 0fr; transition: grid-template-rows .25s ease, margin-bottom .25s ease; margin-bottom: 0; }
.hh-filter.is-open { grid-template-rows: 1fr; margin-bottom: 12px; }
.hh-filter__panel {
  overflow: hidden; background: #fff; border: 1px solid var(--line);
  border-radius: 12px; padding: 0 16px; transition: padding .25s ease;
}
.hh-filter.is-open .hh-filter__panel { padding: 16px; }
.hh-filter__grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 12px; }
.hh-filter__foot {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  margin-top: 14px; padding-top: 12px; border-top: 1px dashed var(--line);
}
.hh-filter__count { font-size: 12.5px; color: var(--muted); }
.hh-filter__btns { display: flex; gap: 8px; }

/* ═══════════ Ô NHẬP ═══════════ */
.hh-field { display: flex; flex-direction: column; gap: 5px; min-width: 0; }
.hh-field > span { font-size: 12px; font-weight: 600; color: var(--pink-700); }
.hh-field > span b { color: var(--danger); }
.hh-field input,
.hh-field select,
.hh-field textarea,
.hh-cell {
  width: 100%; padding: 9px 11px;
  border: 1px solid var(--field); border-radius: 8px;
  font-size: 13px; color: var(--ink); background: #fff; font-family: inherit;
  transition: border-color .15s, box-shadow .15s;
}
.hh-field input::placeholder, .hh-cell::placeholder { color: #b9a3ae; }
.hh-field input:hover, .hh-field select:hover, .hh-cell:hover { border-color: var(--pink-300); }
.hh-field input:focus, .hh-field select:focus, .hh-field textarea:focus, .hh-cell:focus {
  outline: none; border-color: var(--pink-500); box-shadow: 0 0 0 3px var(--pink-100);
}
.hh-field input:disabled, .hh-field select:disabled { background: #f8f6f7; color: var(--muted); }
.hh-inline { display: flex; gap: 6px; align-items: center; }
.hh-inline > select, .hh-inline > input { flex: 1; min-width: 0; }
.hh-err { font-size: 11.5px; color: var(--danger); font-style: normal; }
.hh-hint { font-size: 11.5px; color: var(--muted); font-style: normal; }

/* thẻ tag có nút xóa */
.hh-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 2px; }
.hh-tag-pill {
  display: inline-flex; align-items: center; gap: 6px;
  background: var(--pink-100); color: var(--pink-700);
  border: 1px solid var(--pink-200); border-radius: 999px;
  padding: 3px 6px 3px 11px; font-size: 12.5px; font-weight: 500;
}
.hh-tag-pill button {
  background: var(--pink-200); border: none; color: var(--pink-700);
  width: 17px; height: 17px; border-radius: 50%; line-height: 1;
  font-size: 13px; cursor: pointer; display: grid; place-items: center;
}
.hh-tag-pill button:hover { background: var(--pink-600); color: #fff; }

/* ═══════════ BẢNG DANH SÁCH ═══════════ */
.hh-card { background: #fff; border: 1px solid var(--line); border-radius: 12px; overflow: hidden; }
.hh-table-wrap { position: relative; overflow-x: auto; min-height: 140px; }
.hh-table { width: 100%; border-collapse: collapse; }
.hh-table th {
  background: var(--pink-50); color: var(--pink-700);
  font-size: 12px; font-weight: 700; text-align: left;
  padding: 11px 12px; white-space: nowrap; border-bottom: 1px solid var(--line);
}
.hh-table td { padding: 10px 12px; border-bottom: 1px solid var(--line); white-space: nowrap; vertical-align: middle; }
.hh-col-check { width: 36px; }
.hh-col-caret { width: 28px; }
.hh-table input[type="checkbox"] { accent-color: var(--pink-600); cursor: pointer; }

.hh-row-group { cursor: pointer; }
.hh-row-group:hover { background: var(--pink-50); }
.hh-row-group.is-open { background: var(--pink-100); }
.hh-row-group td { font-weight: 600; }
.hh-row-group.is-open td:first-child { box-shadow: inset 3px 0 0 var(--pink-600); }

.hh-row-variant { cursor: pointer; background: #fff; }
.hh-row-variant:hover, .hh-row-variant.is-open { background: var(--pink-50); }
.hh-row-variant td { font-weight: 400; }

.hh-code { display: flex; flex-direction: column; gap: 2px; }
.hh-code--indent { flex-direction: row; align-items: center; gap: 8px; padding-left: 18px; }
.hh-code__main { color: var(--pink-700); font-weight: 700; letter-spacing: .3px; }
.hh-code__sub { font-size: 11px; color: var(--muted); font-weight: 400; }
.hh-code__sku { font-family: ui-monospace, "SFMono-Regular", Menlo, monospace; font-size: 12.5px; }

.hh-name { display: flex; align-items: center; gap: 10px; }
.hh-name__main { font-weight: 600; }
.hh-name__sub { font-size: 11.5px; color: var(--muted); font-weight: 400; }
.hh-thumb { width: 34px; height: 34px; object-fit: cover; border-radius: 8px; border: 1px solid var(--line); background: #fff; flex-shrink: 0; }
.hh-thumb--sm { width: 26px; height: 26px; border-radius: 6px; }

.hh-tag { display: inline-block; padding: 2px 9px; border-radius: 999px; font-size: 11.5px; font-weight: 600; }
.hh-tag--ok { background: var(--ok-bg); color: var(--ok-text); }
.hh-tag--off { background: #f3f4f6; color: var(--muted); }
.hh-tag--soft { background: var(--pink-100); color: var(--pink-700); font-weight: 500; }
.hh-tag--outline { background: #fff; color: var(--pink-700); border: 1px solid var(--pink-200); font-weight: 500; }

/* ═══════════ CHI TIẾT PHIÊN BẢN ═══════════ */
.hh-row-detail td { background: var(--pink-50); padding: 0; white-space: normal; }
.hh-detail { margin: 12px; padding: 16px; background: #fff; border: 1px solid var(--pink-200); border-radius: 12px; }
.hh-detail__head { display: flex; gap: 16px; align-items: flex-start; }
.hh-detail__img {
  width: 108px; height: 108px; object-fit: contain; padding: 6px;
  border: 1px solid var(--line); border-radius: 10px; background: var(--pink-50); flex-shrink: 0;
}
.hh-detail__intro h3 { margin: 0 0 6px; font-size: 16px; font-weight: 700; }
.hh-detail__tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
.hh-detail__desc { font-size: 13px; color: var(--muted); line-height: 1.55; }
.hh-detail__desc :deep(p) { margin: 0 0 6px; }
.hh-detail__desc :deep(ul), .hh-detail__desc :deep(ol) { margin: 0 0 6px; padding-left: 20px; }

.hh-specs {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 12px 16px; margin: 16px 0 0; padding-top: 14px; border-top: 1px dashed var(--line);
}
.hh-specs dt { font-size: 11.5px; color: var(--muted); margin-bottom: 2px; }
.hh-specs dd { margin: 0; font-size: 13px; font-weight: 600; }
.hh-detail__foot { display: flex; justify-content: flex-end; gap: 8px; margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--line); }

/* ═══════════ RỖNG / LOADING / PHÂN TRANG ═══════════ */
.hh-overlay { position: absolute; inset: 0; background: rgba(255,255,255,.65); display: flex; align-items: center; justify-content: center; }
.hh-spinner { width: 26px; height: 26px; border-radius: 50%; border: 3px solid var(--pink-200); border-top-color: var(--pink-600); animation: hh-spin .7s linear infinite; }
@keyframes hh-spin { to { transform: rotate(360deg); } }

.hh-empty { padding: 44px 20px; text-align: center; color: var(--muted); }
.hh-empty i { font-size: 32px; color: var(--pink-300); }
.hh-empty p { margin: 12px 0; font-size: 13.5px; }

.hh-alert { margin: 0 0 12px; padding: 10px 14px; background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; font-size: 13px; border-radius: 8px; }

.hh-pager { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 16px; background: var(--pink-50); flex-wrap: wrap; }
.hh-pager__info { font-size: 12.5px; color: var(--muted); }
.hh-pager__nav { display: flex; align-items: center; gap: 8px; }
.hh-pager__page { font-size: 13px; font-weight: 600; min-width: 56px; text-align: center; }
.hh-pager__size { padding: 5px 8px; border: 1px solid var(--field); border-radius: 8px; font-size: 12.5px; background: #fff; color: var(--ink); }

/* ═══════════ MODAL ═══════════ */
.hh-modal-mask {
  position: fixed; inset: 0; z-index: 1050;
  background: rgba(31,41,55,.5); display: flex; align-items: center; justify-content: center; padding: 20px;
  font-size: 14px; color: var(--ink);
}
.hh-modal {
  background: #fff; width: 1020px; max-width: 100%; max-height: 94vh;
  border-radius: 14px; display: flex; flex-direction: column; overflow: hidden;
  box-shadow: 0 22px 55px rgba(168,27,93,.25);
}
.hh-modal__head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 12px;
  padding: 16px 20px 12px; background: var(--pink-50); border-bottom: 1px solid var(--line);
}
.hh-modal__head h2 { margin: 0; font-size: 17px; font-weight: 700; color: var(--pink-700); }
.hh-modal__head p { margin: 6px 0 0; font-size: 12.5px; color: var(--muted); display: flex; align-items: center; gap: 6px; }

.hh-tabs { display: flex; gap: 4px; padding: 0 20px; background: var(--pink-50); border-bottom: 1px solid var(--line); }
.hh-tab {
  background: none; border: none; border-bottom: 2px solid transparent;
  padding: 9px 14px; font-size: 13px; font-weight: 600; font-family: inherit;
  color: var(--muted); cursor: pointer; display: inline-flex; align-items: center; gap: 6px;
}
.hh-tab:hover { color: var(--pink-600); }
.hh-tab.is-on { color: var(--pink-700); border-bottom-color: var(--pink-600); }

.hh-modal__body { padding: 20px; overflow-y: auto; background: #fffafc; }
.hh-pane { display: flex; flex-direction: column; gap: 16px; }

.hh-block {
  border: 1px solid var(--line); border-radius: 12px;
  padding: 16px; margin: 0; background: #fff;
}
.hh-block:disabled { opacity: .75; }
.hh-block legend {
  font-size: 12.5px; font-weight: 700; color: var(--pink-700);
  background: var(--pink-100); border-radius: 999px; padding: 4px 12px;
  display: inline-flex; align-items: center; gap: 8px; width: auto;
}
.hh-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 14px; }
.hh-field--wide { grid-column: 1 / -1; }

.hh-note {
  display: flex; align-items: center; gap: 8px; margin: 14px 0 0;
  padding: 9px 12px; background: var(--pink-50); border: 1px dashed var(--pink-200);
  border-radius: 8px; font-size: 12.5px; color: var(--muted);
}
.hh-note--plain { margin: 0 0 14px; }

/* upload ảnh */
.hh-upload { display: flex; gap: 14px; align-items: flex-start; }
.hh-upload__preview {
  width: 104px; height: 104px; flex-shrink: 0;
  border: 1px dashed var(--pink-300); border-radius: 10px; background: var(--pink-50);
  display: grid; place-items: center; overflow: hidden;
}
.hh-upload__preview img { width: 100%; height: 100%; object-fit: contain; }
.hh-upload__preview i { font-size: 26px; color: var(--pink-300); }
.hh-upload__body { flex: 1; min-width: 0; }
.hh-upload__body input[type="text"], .hh-upload__body input:not([type]) {
  width: 100%; padding: 9px 11px; border: 1px solid var(--field); border-radius: 8px; font-size: 13px; font-family: inherit;
}

/* ma trận phiên bản */
.hh-matrix-wrap { border: 1px solid var(--line); border-radius: 10px; overflow: auto; max-height: 320px; }
.hh-matrix { width: 100%; border-collapse: collapse; }
.hh-matrix th {
  position: sticky; top: 0; background: var(--pink-50); color: var(--pink-700);
  font-size: 11.5px; font-weight: 700; text-align: left; padding: 9px 10px; white-space: nowrap;
  border-bottom: 1px solid var(--line);
}
.hh-matrix td { padding: 6px 10px; border-bottom: 1px solid var(--line); font-size: 13px; vertical-align: middle; }
.hh-matrix tr:last-child td { border-bottom: none; }
.hh-matrix__cfg { color: var(--muted); font-size: 12.5px; }
.hh-matrix__empty { text-align: center; color: var(--muted); padding: 20px; }
.hh-cell { padding: 6px 9px; font-size: 12.5px; }
.hh-cell--sku { font-family: ui-monospace, "SFMono-Regular", Menlo, monospace; min-width: 190px; }

/* trình soạn mô tả */
.hh-editor { border: 1px solid var(--field); border-radius: 10px; overflow: hidden; }
.hh-editor__bar { display: flex; align-items: center; gap: 2px; padding: 6px 8px; background: var(--pink-50); border-bottom: 1px solid var(--line); }
.hh-editor__bar button {
  background: none; border: none; width: 30px; height: 28px; border-radius: 6px;
  color: var(--pink-700); cursor: pointer; font-size: 13px;
}
.hh-editor__bar button:hover { background: var(--pink-100); }
.hh-editor__sep { width: 1px; height: 18px; background: var(--pink-200); margin: 0 5px; }
.hh-editor__area { min-height: 220px; padding: 14px 16px; font-size: 13.5px; line-height: 1.6; outline: none; }
.hh-editor__area:empty::before { content: attr(data-placeholder); color: #b9a3ae; }
.hh-editor__area:focus { box-shadow: inset 0 0 0 2px var(--pink-100); }

.hh-modal__foot {
  display: flex; justify-content: space-between; align-items: center; gap: 10px;
  padding: 14px 20px; border-top: 1px solid var(--line); background: var(--pink-50);
}
.hh-modal__foot-right { display: flex; gap: 10px; }

/* ═══════════ TOAST ═══════════ */
.hh-toast {
  position: fixed; bottom: 26px; left: 50%; transform: translateX(-50%); z-index: 1100;
  background: var(--pink-700); color: #fff; padding: 10px 20px; border-radius: 999px;
  font-size: 13px; box-shadow: 0 8px 22px rgba(168,27,93,.35);
}

/* ═══════════ MÀN HÌNH NHỎ ═══════════ */
@media (max-width: 768px) {
  .hh-bar__actions { width: 100%; margin-left: 0; }
  .hh-search { width: 100%; }
  .hh-detail__head, .hh-upload { flex-direction: column; }
  .hh-modal__foot { flex-direction: column-reverse; align-items: stretch; }
  .hh-modal__foot-right { justify-content: flex-end; }
}

@media (prefers-reduced-motion: reduce) {
  .hh-btn, .hh-caret, .hh-filter, .hh-filter__panel { transition: none; }
  .hh-spinner { animation-duration: 2s; }
}
</style>