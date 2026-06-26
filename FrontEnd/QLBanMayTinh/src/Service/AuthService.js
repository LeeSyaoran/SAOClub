import axios from "axios";

const API_URL = "http://localhost:8080/api/auth";

const api = axios.create({
    baseURL: API_URL,
    headers: {
        "Content-Type": "application/json"
    }
});

// Đăng ký
export const register = (data) => {
    return api.post("/register", data);
};

// Đăng nhập
export const login = (data) => {
    return api.post("/login", data);
};

// Đăng xuất
export const logout = () => {
    localStorage.removeItem("user");
};

// Lấy user hiện tại
export const getCurrentUser = () => {
    const user = localStorage.getItem("user");
    return user ? JSON.parse(user) : null;
};