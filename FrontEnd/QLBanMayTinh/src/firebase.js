import { initializeApp } from 'firebase/app';
import {
  getAuth,
  GoogleAuthProvider,
  FacebookAuthProvider,
  signInWithPopup,
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

// Đăng nhập Google — popup flow (không cần redirect)
export async function signInWithGoogle() {
  try {
    const result = await signInWithPopup(auth, googleProvider);
    const { user: firebaseUser } = result;
    const idToken = await firebaseUser.getIdToken();
    const isNewUser = getAdditionalUserInfo(result)?.isNewUser ?? false;
    return { idToken, provider: 'google', user: firebaseUser, isNewUser };
  } catch (error) {
    console.error('Google sign-in error:', error);
    throw error;
  }
}

// Đăng nhập Facebook — popup flow (không cần redirect)
export async function signInWithFacebook() {
  try {
    const result = await signInWithPopup(auth, facebookProvider);
    const { user: firebaseUser } = result;
    const idToken = await firebaseUser.getIdToken();
    const isNewUser = getAdditionalUserInfo(result)?.isNewUser ?? false;
    return { idToken, provider: 'facebook', user: firebaseUser, isNewUser };
  } catch (error) {
    console.error('Facebook sign-in error:', error);
    throw error;
  }
}

export { auth };
