import { initializeApp } from 'firebase/app';
import {
  getAuth,
  GoogleAuthProvider,
  FacebookAuthProvider,
  signInWithRedirect,
  getRedirectResult,
  getAdditionalUserInfo,
} from 'firebase/auth';

// Firebase config từ Firebase Console
const firebaseConfig = {
  apiKey: "AIzaSyATzpLxaWfo3EAPPu0QCDW7BDZDipDjkuE",
  authDomain: "saoclub-b9b96.firebaseapp.com",
  projectId: "saoclub-b9b96",
  storageBucket: "saoclub-b9b96.firebasestorage.app",
  messagingSenderId: "613373340176",
  appId: "1:613373340176:web:b9eafcd18b19645a60f895",
  measurementId: "G-8N2ME19MVQ"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

// Auth Providers
const googleProvider = new GoogleAuthProvider();
const facebookProvider = new FacebookAuthProvider();

// Đăng nhập Google — redirect flow (OAuth2 standard)
export async function signInWithGoogle() {
  try {
    await signInWithRedirect(auth, googleProvider);
    // Redirect xảy ra ngay, code bên dưới chạy sau khi redirect về
    return null;
  } catch (error) {
    console.error('Google sign-in error:', error);
    throw error;
  }
}

// Đăng nhập Facebook — redirect flow (OAuth2 standard)
export async function signInWithFacebook() {
  try {
    await signInWithRedirect(auth, facebookProvider);
    // Redirect xảy ra ngay, code bên dưới chạy sau khi redirect về
    return null;
  } catch (error) {
    console.error('Facebook sign-in error:', error);
    throw error;
  }
}

// Lấy kết quả sau khi redirect về — gọi trong App.vue onMounted
export async function handleRedirectResult() {
  try {
    const result = await getRedirectResult(auth);
    if (!result) return null;

    const { user, _tokenResponse } = result;
    let idToken, provider;

    // Xác định provider từ tokenResponse
    if (_tokenResponse?.oauthIdToken) {
      // Google
      idToken = _tokenResponse.oauthIdToken;
      provider = 'google';
    } else if (_tokenResponse?.accessToken) {
      // Facebook — Firebase đã exchange token rồi, lấy idToken từ user
      idToken = await user.getIdToken();
      provider = 'facebook';
    } else {
      throw new Error('Unknown OAuth provider');
    }

    return {
      idToken,
      provider,
      user,
      isNewUser: getAdditionalUserInfo(result)?.isNewUser ?? false,
    };
  } catch (error) {
    console.error('Handle redirect result error:', error);
    throw error;
  }
}

export { auth };
