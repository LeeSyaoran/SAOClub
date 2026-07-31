import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockClearSession = vi.fn();
const mockShowToast = vi.fn();
const mockT = vi.fn((key) => key);
const mockResetAllStores = vi.fn();

vi.mock('../../stores/index.js', () => ({ clearSession: mockClearSession }));
vi.mock('../../stores/toast.js', () => ({ showToast: mockShowToast }));
vi.mock('../../i18n/index.js', () => ({ t: mockT }));
vi.mock('../../stores/resetAll.js', () => ({ resetAllStores: mockResetAllStores }));

const mockFetch = vi.fn();
global.fetch = mockFetch;

beforeEach(() => {
  mockFetch.mockReset();
  mockClearSession.mockReset();
  mockShowToast.mockReset();
  mockT.mockReset();
  mockResetAllStores.mockReset();
  vi.useFakeTimers();
  vi.resetModules();
});

afterEach(() => {
  vi.useRealTimers();
});

describe('api service', () => {
  it('should make GET request and return JSON', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({ data: 'test' }),
    });

    const { get } = await import('../../services/api.js');
    const result = await get('/api/test');
    expect(result).toEqual({ data: 'test' });
    expect(mockFetch).toHaveBeenCalledWith('/api/test', expect.objectContaining({}));
  });

  it('should throw on non-ok GET response', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 404,
      text: () => Promise.resolve('Not found'),
    });

    const { get } = await import('../../services/api.js');
    await expect(get('/api/test')).rejects.toThrow('HTTP 404: Not found');
  });

  it('should make POST request with JSON body', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true });

    const { post } = await import('../../services/api.js');
    const body = { name: 'test' };
    await post('/api/test', body);

    expect(mockFetch).toHaveBeenCalledWith('/api/test', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
  });

  it('should call clearSession on 401 response', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 401,
      text: () => Promise.resolve('Unauthorized'),
    });

    const { get } = await import('../../services/api.js');
    await expect(get('/api/protected')).rejects.toThrow('HTTP 401: Unauthorized');
    expect(mockClearSession).toHaveBeenCalled();
    expect(mockResetAllStores).toHaveBeenCalled();
  });

  it('should only call clearSession once for concurrent 401s', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 401,
      text: () => Promise.resolve('Unauthorized'),
    });

    const { get } = await import('../../services/api.js');
    await expect(Promise.all([get('/api/a'), get('/api/b'), get('/api/c')])).rejects.toThrow();
    expect(mockClearSession).toHaveBeenCalledTimes(1);
  });

  it('should re-arm session expiry flag after 2 seconds', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 401,
      text: () => Promise.resolve('Unauthorized'),
    });

    const { get } = await import('../../services/api.js');
    await expect(get('/api/protected')).rejects.toThrow();
    expect(mockClearSession).toHaveBeenCalledTimes(1);

    vi.advanceTimersByTime(2001);

    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 401,
      text: () => Promise.resolve('Unauthorized'),
    });
    await expect(get('/api/protected')).rejects.toThrow();
    expect(mockClearSession).toHaveBeenCalledTimes(2);
  });

  it('should include auth header when session exists', async () => {
    const token = 'test-token-123';
    sessionStorage.setItem('saophone_session', JSON.stringify({ token }));

    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({}),
    });

    const { get } = await import('../../services/api.js');
    await get('/api/test');

    expect(mockFetch).toHaveBeenCalledWith('/api/test', {
      headers: { Authorization: 'Bearer test-token-123' },
    });
  });

  it('should not include auth header when session is missing', async () => {
    sessionStorage.removeItem('saophone_session');

    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({}),
    });

    const { get } = await import('../../services/api.js');
    await get('/api/test');

    const [, options] = mockFetch.mock.calls[0];
    expect(options.headers.Authorization).toBeUndefined();
  });

  it('should handle GET with empty error body', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 500,
      text: () => Promise.resolve(''),
    });

    const { get } = await import('../../services/api.js');
    await expect(get('/api/error')).rejects.toThrow('HTTP 500');
  });
});
