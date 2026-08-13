import React from 'react';
import { describe, expect, it } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ResearchLandingPage from './landing';
import { ResearchEntry } from './index';

describe('research access form accessibility', () => {
  it('binds release-code validation errors to the input', () => {
    render(
      <MemoryRouter>
        <ResearchLandingPage />
      </MemoryRouter>
    );

    fireEvent.submit(screen.getByRole('button', { name: /进入研究问卷/ }).closest('form')!);

    const input = screen.getByLabelText(/发布编号/);
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAttribute('aria-describedby', 'release-code-error');
    expect(document.getElementById('release-code-error')).toHaveTextContent('请输入以 RES- 开头的有效发布编号。');
  });

  it('binds participation-code errors to the input', () => {
    render(
      <ResearchEntry
        metadata={null}
        participationCode="BAD"
        verifying={false}
        qrEntering={false}
        qrRequested={false}
        errorMessage="参与码无效或已失效，请检查后重试。"
        onCodeChange={() => undefined}
        onVerify={(event) => event.preventDefault()}
      />
    );

    const input = screen.getByLabelText(/参与码/);
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAttribute('aria-describedby', 'participation-code-error');
    expect(document.getElementById('participation-code-error')).toHaveTextContent('参与码无效或已失效，请检查后重试。');
  });

  it('clears participation-code invalid state after the user edits the code', () => {
    const Harness = () => {
      const [code, setCode] = React.useState('');
      const [error, setError] = React.useState<string | null>('参与码无效或已失效，请检查后重试。');
      return (
        <ResearchEntry
          metadata={null}
          participationCode={code}
          verifying={false}
          qrEntering={false}
          qrRequested={false}
          errorMessage={error}
          onCodeChange={(value) => {
            setCode(value);
            setError(null);
          }}
          onVerify={(event) => event.preventDefault()}
        />
      );
    };
    render(<Harness />);

    const input = screen.getByLabelText(/参与码/);
    expect(input).toHaveAttribute('aria-invalid', 'true');
    fireEvent.change(input, { target: { value: 'ABCD-EFGH-IJKL' } });
    expect(input).not.toHaveAttribute('aria-invalid');
    expect(document.getElementById('participation-code-error')).toBeNull();
  });
});
