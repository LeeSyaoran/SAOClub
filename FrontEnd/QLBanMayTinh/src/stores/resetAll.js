import { resetProducts } from "./products.js";
import { resetCustomers } from "./customers.js";
import { resetOrders } from "./orders.js";
import { resetInventory } from "./inventory.js";
import { resetPromotions } from "./promotions.js";
import { resetStaff } from "./staff.js";
import { resetSuppliers } from "./suppliers.js";
import { resetReturns } from "./returns.js";
import { resetBaoHanh } from "./baoHanh.js";
import { resetDoiThuong } from "./doiThuong.js";

// Xóa cache của tất cả các store khi đăng xuất
export const resetAllStores = () => {
  resetProducts();
  resetCustomers();
  resetOrders();
  resetInventory();
  resetPromotions();
  resetStaff();
  resetSuppliers();
  resetReturns();
  resetBaoHanh();
  resetDoiThuong();
  // Xóa vị trí tab quản trị đã lưu trong phiên
  sessionStorage.removeItem("admin.lastPage");
  sessionStorage.removeItem("admin.lastInventoryTab");
};
