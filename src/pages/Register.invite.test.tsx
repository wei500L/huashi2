import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import '@/lib/i18n';
import { authService } from '@/lib/services';
import { useAuthStore } from '@/store';
import RegisterPage from './Register';

vi.mock('@/lib/services', () => ({
  authService: {
    resolveRegistrationContext: vi.fn(),
    registerStudent: vi.fn(),
  },
}));

function renderRegister() {
  return render(
    <MemoryRouter>
      <RegisterPage />
    </MemoryRouter>
  );
}

describe('Register invite code verification', () => {
  const originalAuth = useAuthStore.getState();

  beforeEach(() => {
    useAuthStore.setState({
      ...originalAuth,
      status: 'anonymous',
      session: null,
      user: null,
      error: null,
      registerStudent: vi.fn(),
      clearError: vi.fn(),
    });
    vi.mocked(authService.resolveRegistrationContext).mockResolvedValue({
      className: '大创成员内部测试',
      gradeName: 'ccnu24、25级法语专业',
      registrationToken: 'token-1',
      registrationTokenExpiresAt: '2030-01-01T00:00:00Z',
    });
  });

  afterEach(() => {
    cleanup();
    useAuthStore.setState(originalAuth);
    vi.clearAllMocks();
  });

  it('verifies a pasted class invite code and enables submit', async () => {
    const user = userEvent.setup();
    renderRegister();

    await user.type(document.getElementById('register-class-code') as HTMLInputElement, 'CLS-NJ3R68');

    await waitFor(() => {
      expect(authService.resolveRegistrationContext).toHaveBeenCalledWith(
        { classCode: 'CLS-NJ3R68' },
        expect.objectContaining({ signal: expect.any(AbortSignal) })
      );
    });

    expect(await screen.findByText('大创成员内部测试')).toBeInTheDocument();
    expect(screen.getByText('邀请码验证通过')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '注册并进入学习空间' })).toBeEnabled();
  });
});
