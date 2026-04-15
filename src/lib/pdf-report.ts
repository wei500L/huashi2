import html2canvas from 'html2canvas';
import { jsPDF } from 'jspdf';

const PAGE_SELECTOR = '[data-pdf-page="true"]';

function waitForNextFrame(): Promise<void> {
  return new Promise((resolve) => {
    window.requestAnimationFrame(() => {
      window.requestAnimationFrame(() => resolve());
    });
  });
}

async function waitForReportStability(): Promise<void> {
  if (typeof document !== 'undefined' && 'fonts' in document) {
    try {
      await document.fonts.ready;
    } catch {
      // Ignore font readiness failures and continue with best-effort export.
    }
  }
  await waitForNextFrame();
  await new Promise((resolve) => window.setTimeout(resolve, 450));
}

export async function exportReportPagesToPdf(root: HTMLElement | null, filename: string): Promise<void> {
  if (!root) {
    throw new Error('PDF 报告容器未准备好。');
  }

  await waitForReportStability();

  const pages = Array.from(root.querySelectorAll<HTMLElement>(PAGE_SELECTOR));
  if (!pages.length) {
    throw new Error('PDF 报告页面未生成。');
  }

  const pdf = new jsPDF({
    orientation: 'portrait',
    unit: 'pt',
    format: 'a4',
    compress: true,
  });
  const pageWidth = pdf.internal.pageSize.getWidth();
  const pageHeight = pdf.internal.pageSize.getHeight();

  for (const [index, page] of pages.entries()) {
    const canvas = await html2canvas(page, {
      backgroundColor: '#ffffff',
      scale: Math.max(2, Math.min(3, window.devicePixelRatio || 2)),
      useCORS: true,
      logging: false,
      windowWidth: page.scrollWidth,
      windowHeight: page.scrollHeight,
    });

    if (index > 0) {
      pdf.addPage('a4', 'portrait');
    }

    pdf.addImage(canvas.toDataURL('image/png'), 'PNG', 0, 0, pageWidth, pageHeight, undefined, 'FAST');
  }

  pdf.save(filename);
}
