import { createRouter, createWebHashHistory } from "vue-router";
import CustomerPage from "@/pages/CustomerPage.vue";
const AdminPage = () => import("@/pages/AdminPage.vue");
const StaffPage = () => import("@/pages/StaffPage.vue");
const WarehouseManagementPage = () => import("@/pages/WarehouseManagementPage.vue");
const AccountPage = () => import("@/pages/AccountPage.vue");
const NotFoundPage = () => import("@/pages/NotFoundPage.vue");

const routes = [
  { path: "/", name: "home", component: CustomerPage },
  { path: "/admin", name: "admin", component: AdminPage, meta: { requiresAuth: true, roles: ["admin"] } },
  { path: "/admin/san-pham/:id", name: "admin-san-pham-detail", component: AdminPage, meta: { requiresAuth: true, roles: ["admin"] } },
  { path: "/staff", name: "staff", component: StaffPage, meta: { requiresAuth: true, roles: ["nhan_vien"] } },
  { path: "/kho", name: "warehouse", component: WarehouseManagementPage, meta: { requiresAuth: true, roles: ["quan_kho"] } },
  { path: "/account", name: "account", component: AccountPage, meta: { requiresAuth: true, roles: ["khach_hang"] } },
  { path: "/:pathMatch(.*)*", name: "not-found", component: NotFoundPage },
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

const STAFF_ROLES = ["admin", "nhan_vien", "quan_kho"];

const OLD_HASH_PATHS = { admin: "/admin", staff: "/staff", kho: "/kho", account: "/account", "": "/" };

router.beforeEach((to, from, next) => {
  if (to.path in OLD_HASH_PATHS) {
    next(OLD_HASH_PATHS[to.path]);
    return;
  }
  const saved = (() => {
    try { return JSON.parse(sessionStorage.getItem("saophone_session")); }
    catch { return null; }
  })();

  if (to.meta.requiresAuth) {
    if (!saved) {
      next({ path: "/" });
      return;
    }
    if (to.meta.roles) {
      const role = saved.role;
      if (to.meta.roles.includes("khach_hang") && STAFF_ROLES.includes(role)) {
        next({ path: "/" });
        return;
      }
      if (to.meta.roles.includes(role)) {
        next();
        return;
      }
      next({ path: "/" });
      return;
    }
  }
  next();
});

export default router;
