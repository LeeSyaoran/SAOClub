import axios from "axios";

const authApi = axios.create({
    baseURL: "http://localhost:8080/api/auth",
    headers: {
        "Content-Type": "application/json"
    }
});

const customerApi = axios.create({
    baseURL: "http://localhost:8080/api/khach-hang",
    headers: {
        "Content-Type": "application/json"
    }
});

// Đăng ký
export const register = (data) => {
    return customerApi.post("/register", data);
};

// Đăng nhập
export const login = (data) => {
    return authApi.post("/login", data);
};