import React from 'react';
import QRCode from 'qrcode';
import { Copy, Download, Printer, QrCode } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface RegistrationQrCodeProps {
  value: string;
  fileName?: string;
  alt?: string;
  size?: number;
  className?: string;
  printTitle?: string;
  extraActions?: React.ReactNode;
}

export const RegistrationQrCode: React.FC<RegistrationQrCodeProps> = ({
  value,
  fileName = 'registration-qr',
  alt = '二维码',
  size = 160,
  className,
  printTitle,
  extraActions,
}) => {
  const { t } = useTranslation();
  const [qrDataUrl, setQrDataUrl] = React.useState('');
  const [qrError, setQrError] = React.useState<string | null>(null);
  const [copied, setCopied] = React.useState(false);

  React.useEffect(() => {
    let active = true;
    if (!value) {
      setQrDataUrl('');
      setQrError(null);
      return () => { active = false; };
    }
    setQrError(null);
    void QRCode.toDataURL(value, { width: 360, margin: 2, errorCorrectionLevel: 'M' })
      .then((dataUrl) => { if (active) setQrDataUrl(dataUrl); })
      .catch(() => {
        if (active) {
          setQrDataUrl('');
          setQrError(t('ui.registrationQr.generationError'));
        }
      });
    return () => { active = false; };
  }, [value, t]);

  const copyLink = React.useCallback(async () => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1600);
    } catch {
      setCopied(false);
    }
  }, [value]);

  const downloadPng = React.useCallback(() => {
    if (!qrDataUrl) return;
    const anchor = document.createElement('a');
    anchor.href = qrDataUrl;
    anchor.download = `${fileName}.png`;
    anchor.click();
  }, [qrDataUrl, fileName]);

  const print = React.useCallback(() => {
    if (!qrDataUrl) return;
    const popup = window.open('', '_blank', 'width=720,height=820');
    if (!popup) {
      return;
    }
    popup.opener = null;
    const document = popup.document;
    document.title = printTitle || alt;
    const style = document.createElement('style');
    style.textContent = 'body{font-family:system-ui;text-align:center;padding:40px;color:#0f172a}img{width:360px;height:360px}p{word-break:break-all;color:#475569}';
    document.head.replaceChildren(style);
    const heading = document.createElement('h1');
    heading.textContent = printTitle || alt;
    const image = document.createElement('img');
    image.src = qrDataUrl;
    image.alt = alt;
    const link = document.createElement('p');
    link.textContent = value;
    document.body.replaceChildren(heading, image, link);
    const triggerPrint = () => popup.setTimeout(() => popup.print(), 0);
    if (image.complete) triggerPrint(); else image.addEventListener('load', triggerPrint, { once: true });
  }, [qrDataUrl, printTitle, alt, value]);

  return (
    <div className={className}>
      <div className="flex flex-col items-center gap-4 sm:flex-row sm:items-start">
        {qrDataUrl ? (
          <img src={qrDataUrl} alt={alt} style={{ width: size, height: size }} className="shrink-0 rounded-xl border border-slate-200 bg-white p-2 dark:border-white/10" />
        ) : (
          <div
            style={{ width: size, height: size }}
            className="flex shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-slate-100 text-slate-300 dark:border-white/10 dark:bg-white/5"
          >
            <QrCode size={size * 0.4} />
          </div>
        )}
        <div className="min-w-0 flex-1">
          <p className="break-all text-xs leading-5 text-slate-500 dark:text-white/45">{value}</p>
          {qrError ? <p role="alert" className="mt-2 text-xs text-rose-700 dark:text-rose-400">{qrError}</p> : null}
          <div className="mt-3 flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => void copyLink()}
              className="inline-flex min-h-10 items-center gap-1.5 rounded-xl border border-slate-200 px-3 text-xs font-bold disabled:opacity-50 dark:border-white/10"
            >
              <Copy size={14} />
              {copied ? t('ui.registrationQr.copied') : t('ui.registrationQr.copyLink')}
            </button>
            <button
              type="button"
              onClick={downloadPng}
              disabled={!qrDataUrl}
              className="inline-flex min-h-10 items-center gap-1.5 rounded-xl border border-slate-200 px-3 text-xs font-bold disabled:opacity-50 dark:border-white/10"
            >
              <Download size={14} />
              {t('ui.registrationQr.downloadPng')}
            </button>
            {printTitle ? (
              <button
                type="button"
                onClick={print}
                disabled={!qrDataUrl}
                className="inline-flex min-h-10 items-center gap-1.5 rounded-xl border border-slate-200 px-3 text-xs font-bold disabled:opacity-50 dark:border-white/10"
              >
                <Printer size={14} />
                {t('ui.registrationQr.print')}
              </button>
            ) : null}
            {extraActions}
          </div>
        </div>
      </div>
    </div>
  );
};
