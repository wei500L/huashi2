import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { RegistrationQrCode } from './RegistrationQrCode';
import '@/lib/i18n';

const toDataURL = vi.hoisted(() => vi.fn());

vi.mock('qrcode', () => ({
  default: { toDataURL },
}));

afterEach(() => {
  vi.clearAllMocks();
});

describe('RegistrationQrCode', () => {
  it('renders the QR image, link, and copy/download actions', async () => {
    toDataURL.mockResolvedValue('data:image/png;base64,qr');
    render(<RegistrationQrCode value="https://example.com/register?code=CLS-1" fileName="class-1-qr" alt="测试班级注册二维码" />);

    await waitFor(() => expect(toDataURL).toHaveBeenCalledWith(
      'https://example.com/register?code=CLS-1',
      expect.objectContaining({ width: 360 })
    ));

    expect(await screen.findByAltText('测试班级注册二维码')).toBeInTheDocument();
    expect(screen.getByText('https://example.com/register?code=CLS-1')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '复制链接' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '下载 PNG' })).toBeEnabled();
  });

  it('shows a generation error and still allows copying the link', async () => {
    toDataURL.mockRejectedValue(new Error('fail'));
    const user = userEvent.setup();
    const writeText = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue(undefined);

    render(<RegistrationQrCode value="https://example.com/register?code=CLS-2" />);

    expect(await screen.findByRole('alert')).toHaveTextContent('二维码生成失败');
    expect(screen.getByRole('button', { name: '下载 PNG' })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: '复制链接' }));
    expect(writeText).toHaveBeenCalledWith('https://example.com/register?code=CLS-2');
  });
});
