import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import '@/lib/i18n';
import { teacherClassService } from '@/lib/services';
import TeacherClassesPage from './Classes';

vi.mock('@/components/common/RegistrationQrCode', () => ({
  RegistrationQrCode: ({ value }: { value: string }) => <div data-testid="registration-qr">{value}</div>,
}));

vi.mock('@/lib/services', () => ({
  teacherClassService: {
    listClasses: vi.fn(),
  },
}));

const queryClients: QueryClient[] = [];

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  queryClients.push(queryClient);
  return render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>
        <TeacherClassesPage />
      </QueryClientProvider>
    </MemoryRouter>
  );
}

afterEach(() => {
  vi.clearAllMocks();
});

describe('TeacherClassesPage registration QR code', () => {
  it('opens a QR dialog with the register URL for a class', async () => {
    vi.mocked(teacherClassService.listClasses).mockResolvedValue([
      { classId: 42, classCode: 'CLS-NJ3R68', className: '大创成员内部测试', gradeName: 'ccnu24、25级法语专业', studentCount: 0 },
    ]);

    const user = userEvent.setup();
    renderPage();

    await screen.findByText('大创成员内部测试');

    const qrButton = screen.getByRole('button', { name: '注册二维码 大创成员内部测试' });
    await user.click(qrButton);

    const qr = await screen.findByTestId('registration-qr');
    expect(qr).toHaveTextContent(`/register?code=CLS-NJ3R68`);
  });
});
