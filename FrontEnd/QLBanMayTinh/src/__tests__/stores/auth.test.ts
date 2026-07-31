import { describe, it, expect, vi, beforeEach } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

const mockLocalStorage = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => { store[key] = value; }),
    removeItem: vi.fn((key: string) => { delete store[key]; }),
    clear: vi.fn(() => { store = {}; }),
    get length() { return Object.keys(store).length; },
    key: vi.fn((i: number) => Object.keys(store)[i] ?? null),
  };
})();

Object.defineProperty(globalThis, 'localStorage', {
  value: mockLocalStorage,
  configurable: true,
  writable: true,
});

let currentPinia = createPinia();

vi.mock('../../stores/pinia.js', () => ({
  get pinia() { return currentPinia; },
}));

beforeEach(() => {
  vi.resetModules();
  currentPinia = createPinia();
  setActivePinia(currentPinia);
  mockLocalStorage.clear();
  mockLocalStorage.setItem('saophone_dev_boot_id', (globalThis as any).__DEV_BOOT_ID__ || 'test-boot-id');
  sessionStorage.clear();
});
afterEach(() => {
  sessionStorage.clear();
});

describe('auth store', () => {
  it('should start with null user', async () => {
    const { AuthStore } = await import('../../stores/index.js');
    expect(AuthStore.user).toBeNull();
    expect(AuthStore.isAdmin).toBe(false);
  });

  it('should set session and mark admin for admin role', async () => {
    const { setSession, AuthStore } = await import('../../stores/index.js');
    setSession({ id: 1, hoTen: 'Admin', username: 'admin', role: 'admin', token: 'abc' });
    expect(AuthStore.user).toEqual({ id: 1, hoTen: 'Admin', username: 'admin', role: 'admin', token: 'abc' });
    expect(AuthStore.isAdmin).toBe(true);
  });

  it('should mark as admin for nhan_vien role', async () => {
    const { setSession, AuthStore } = await import('../../stores/index.js');
    setSession({ id: 2, hoTen: 'Staff', username: 'staff', role: 'nhan_vien', token: 'def' });
    expect(AuthStore.isAdmin).toBe(true);
  });

  it('should mark as admin for quan_kho role', async () => {
    const { setSession, AuthStore } = await import('../../stores/index.js');
    setSession({ id: 3, hoTen: 'Kho', username: 'kho', role: 'quan_kho', token: 'ghi' });
    expect(AuthStore.isAdmin).toBe(true);
  });

  it('should not mark as admin for customer role', async () => {
    const { setSession, AuthStore } = await import('../../stores/index.js');
    setSession({ id: 4, hoTen: 'Customer', username: 'cust', role: 'khach_hang', token: 'jkl' });
    expect(AuthStore.isAdmin).toBe(false);
  });

  it('should persist user to sessionStorage on setSession', async () => {
    const { setSession } = await import('../../stores/index.js');
    const user = { id: 1, hoTen: 'Test', username: 'test', role: 'admin', token: 'xyz' };
    setSession(user);
    const saved = JSON.parse(sessionStorage.getItem('saophone_session')!);
    expect(saved).toEqual(user);
  });

  it('should clear user and remove from sessionStorage', async () => {
    const { setSession, clearSession, AuthStore } = await import('../../stores/index.js');
    setSession({ id: 1, hoTen: 'Test', username: 'test', role: 'admin', token: 'xyz' });
    clearSession();
    expect(AuthStore.user).toBeNull();
    expect(AuthStore.isAdmin).toBe(false);
    expect(sessionStorage.getItem('saophone_session')).toBeNull();
  });

  it('should restore user from sessionStorage on load', async () => {
    const user = { id: 5, hoTen: 'Loaded', username: 'loaded', role: 'admin', token: 'saved' };
    sessionStorage.setItem('saophone_session', JSON.stringify(user));

    const { AuthStore } = await import('../../stores/index.js');
    expect(AuthStore.user).toEqual(user);
    expect(AuthStore.isAdmin).toBe(true);
  });

  it('should handle invalid sessionStorage gracefully', async () => {
    sessionStorage.setItem('saophone_session', 'not-json');

    const { AuthStore } = await import('../../stores/index.js');
    expect(AuthStore.user).toBeNull();
    expect(AuthStore.isAdmin).toBe(false);
  });
});
