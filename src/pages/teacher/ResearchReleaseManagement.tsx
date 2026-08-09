import React from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Copy,
  Download,
  ExternalLink,
  KeyRound,
  Printer,
  QrCode,
  ShieldOff,
} from 'lucide-react';
import QRCode from 'qrcode';
import { SectionEyebrow, StatusBadge } from '@/components/common';
import { ConfirmationDialog } from '@/components/common/ConfirmationDialog';
import { Pagination } from '@/components/common/Pagination';
import { useLeaveProtection } from '@/features/session-runtime/useLeaveProtection';
import { getApiErrorMessage, saveBlob } from '@/lib/api';
import type {
  ParticipationCodeBatchCreatedVO,
  ParticipationCodeStatus,
  PublicAssessmentReleaseSummaryVO,
} from '@/lib/contracts';
import { formatDateTime } from '@/lib/format';
import { assessmentService } from '@/lib/services';

const statusLabels: Record<string, string> = {
  UNUSED: '未使用',
  IN_PROGRESS: '作答中',
  SUBMITTED: '已提交',
  REVOKED: '已停用',
};

const statusTone = (status: string) => {
  if (status === 'SUBMITTED') return 'success' as const;
  if (status === 'IN_PROGRESS') return 'info' as const;
  if (status === 'REVOKED') return 'danger' as const;
  return 'warning' as const;
};

type RevokeTarget =
  | { type: 'code'; codeId: number; label: string }
  | { type: 'batch'; batchId: string; label: string };

type QrToggleTarget = { enabled: boolean };

const metricCards = (release: PublicAssessmentReleaseSummaryVO) => [
  ['参与码', release.codeCount],
  ['未使用', release.unusedCount],
  ['作答中', release.inProgressCount],
  ['已提交', release.submittedCount],
  ['已停用', release.revokedCount],
  ['二维码参与', release.qrParticipantCount],
] as const;

export const ResearchReleaseManagement: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedPublishId, setSelectedPublishId] = React.useState<number | null>(null);
  const [status, setStatus] = React.useState<'' | ParticipationCodeStatus>('');
  const [batchId, setBatchId] = React.useState('');
  const [page, setPage] = React.useState(1);
  const [batchCount, setBatchCount] = React.useState(20);
  const [createdBatch, setCreatedBatch] = React.useState<ParticipationCodeBatchCreatedVO | null>(null);
  const [qrDataUrl, setQrDataUrl] = React.useState('');
  const [actionError, setActionError] = React.useState<string | null>(null);
  const [actionMessage, setActionMessage] = React.useState<string | null>(null);
  const [pendingAction, setPendingAction] = React.useState<string | null>(null);
  const [revokeTarget, setRevokeTarget] = React.useState<RevokeTarget | null>(null);
  const [qrToggleTarget, setQrToggleTarget] = React.useState<QrToggleTarget | null>(null);
  const [pendingSelectedPublishId, setPendingSelectedPublishId] = React.useState<number | null>(null);
  const [clearCreatedBatchOpen, setClearCreatedBatchOpen] = React.useState(false);
  const [qrGenerationError, setQrGenerationError] = React.useState<string | null>(null);

  useLeaveProtection({
    active: createdBatch != null,
    leaveConfirm: '本页仍显示仅可查看一次的参与码明文。确认已经复制或下载，并离开当前页面吗？',
    onRouteLeave: async () => {
      setCreatedBatch(null);
      return true;
    },
    blockSamePathNavigation: false,
  });

  const releasesQuery = useQuery({
    queryKey: ['teacher-public-assessment-releases'],
    queryFn: ({ signal }) => assessmentService.listPublicReleases({ signal }),
    retry: false,
  });

  const releases = React.useMemo(() => releasesQuery.data || [], [releasesQuery.data]);
  React.useEffect(() => {
    if (!selectedPublishId && releases.length) setSelectedPublishId(releases[0].publishId);
    if (selectedPublishId && releases.length && !releases.some((release) => release.publishId === selectedPublishId)) {
      setSelectedPublishId(releases[0].publishId);
    }
  }, [releases, selectedPublishId]);

  const selectedRelease = releases.find((release) => release.publishId === selectedPublishId) || null;
  const qrUrl = selectedRelease
    ? `${window.location.origin}/research/${selectedRelease.releaseCode}?entry=qr`
    : '';

  React.useEffect(() => {
    let active = true;
    if (!qrUrl) {
      setQrDataUrl('');
      setQrGenerationError(null);
      return () => { active = false; };
    }
    setQrGenerationError(null);
    void QRCode.toDataURL(qrUrl, { width: 360, margin: 2, errorCorrectionLevel: 'M' })
      .then((value) => { if (active) setQrDataUrl(value); })
      .catch(() => {
        if (active) {
          setQrDataUrl('');
          setQrGenerationError('二维码生成失败，请刷新页面后重试。问卷链接仍可正常复制使用。');
        }
      });
    return () => { active = false; };
  }, [qrUrl]);

  const codesQuery = useQuery({
    queryKey: ['teacher-participation-codes', selectedPublishId, status, batchId, page],
    queryFn: ({ signal }) => assessmentService.listParticipationCodes(
      selectedPublishId as number,
      { pageNo: page, pageSize: 20, status: status || undefined, batchId: batchId || undefined },
      { signal }
    ),
    enabled: selectedPublishId != null,
    retry: false,
  });

  React.useEffect(() => setPage(1), [status, batchId]);
  React.useEffect(() => {
    setStatus('');
    setBatchId('');
    setPage(1);
  }, [selectedPublishId]);
  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['teacher-public-assessment-releases'] }),
      queryClient.invalidateQueries({ queryKey: ['teacher-participation-codes', selectedPublishId] }),
    ]);
  };

  const runAction = async (key: string, action: () => Promise<void>) => {
    setPendingAction(key);
    setActionError(null);
    setActionMessage(null);
    try {
      await action();
    } catch (error) {
      setActionError(getApiErrorMessage(error, '操作失败，请稍后重试。'));
    } finally {
      setPendingAction(null);
    }
  };

  const createBatch = () => {
    if (!selectedRelease || createdBatch) return;
    void runAction('create-batch', async () => {
      const created = await assessmentService.createParticipationCodeBatch(selectedRelease.publishId, batchCount);
      setCreatedBatch(created);
      setActionMessage(`已生成 ${created.participationCodes.length} 个参与码。明文仅在本页本次显示，请立即复制或下载。`);
      await refresh();
    });
  };

  const copyCodes = async () => {
    if (!createdBatch) return;
    await runAction('copy-codes', async () => {
      await navigator.clipboard.writeText(createdBatch.participationCodes.join('\n'));
      setActionMessage('本批参与码已复制到剪贴板。');
    });
  };

  const downloadCodes = () => {
    if (!createdBatch || !selectedRelease) return;
    const rows = ['participation_code', ...createdBatch.participationCodes]
      .map((value) => `"${value.replaceAll('"', '""')}"`).join('\r\n');
    saveBlob(new Blob([`\uFEFF${rows}`], { type: 'text/csv;charset=utf-8' }),
      `${selectedRelease.releaseCode}-${createdBatch.batchId}.csv`);
    setActionMessage('参与码 CSV 已下载。');
  };

  const confirmToggleQr = () => {
    if (!selectedRelease) return;
    const next = qrToggleTarget?.enabled;
    if (next == null) return;
    void runAction('toggle-qr', async () => {
      await assessmentService.updatePublicRelease(selectedRelease.publishId, next);
      setActionMessage(next ? '二维码免码参与已开启。' : '二维码免码参与已关闭，已有答卷会话不受影响。');
      setQrToggleTarget(null);
      await refresh();
    });
  };

  const copyQrUrl = async () => {
    await runAction('copy-qr-url', async () => {
      await navigator.clipboard.writeText(qrUrl);
      setActionMessage('二维码问卷链接已复制。');
    });
  };

  const downloadQr = () => {
    if (!qrDataUrl || !selectedRelease) return;
    const anchor = document.createElement('a');
    anchor.href = qrDataUrl;
    anchor.download = `${selectedRelease.releaseCode}-qr.png`;
    anchor.click();
  };

  const printQr = () => {
    if (!qrDataUrl || !selectedRelease) return;
    const popup = window.open('', '_blank', 'width=720,height=820');
    if (!popup) {
      setActionError('浏览器阻止了打印窗口，请允许弹窗后重试。');
      return;
    }
    popup.opener = null;
    const document = popup.document;
    document.title = selectedRelease.paperTitle;
    const style = document.createElement('style');
    style.textContent = 'body{font-family:system-ui;text-align:center;padding:40px;color:#0f172a}img{width:360px;height:360px}p{word-break:break-all;color:#475569}';
    document.head.replaceChildren(style);
    const heading = document.createElement('h1');
    heading.textContent = selectedRelease.paperTitle;
    const image = document.createElement('img');
    image.src = qrDataUrl;
    image.alt = '问卷二维码';
    const link = document.createElement('p');
    link.textContent = qrUrl;
    document.body.replaceChildren(heading, image, link);
    const print = () => popup.setTimeout(() => popup.print(), 0);
    if (image.complete) print(); else image.addEventListener('load', print, { once: true });
  };

  const selectRelease = (publishId: number) => {
    if (publishId === selectedPublishId) return;
    if (createdBatch) {
      setPendingSelectedPublishId(publishId);
      return;
    }
    setSelectedPublishId(publishId);
  };

  const confirmSelectRelease = () => {
    if (pendingSelectedPublishId == null) return;
    setCreatedBatch(null);
    setSelectedPublishId(pendingSelectedPublishId);
    setPendingSelectedPublishId(null);
  };

  const confirmRevoke = () => {
    if (!selectedRelease || !revokeTarget) return;
    const target = revokeTarget;
    void runAction('revoke', async () => {
      const result = target.type === 'code'
        ? await assessmentService.revokeParticipationCode(selectedRelease.publishId, target.codeId)
        : await assessmentService.revokeParticipationCodeBatch(selectedRelease.publishId, target.batchId);
      setActionMessage(`已停用 ${result.revokedCount} 个未使用参与码。`);
      setRevokeTarget(null);
      await refresh();
    });
  };

  if (releasesQuery.isLoading) {
    return <div className="rounded-2xl liquid-glass-panel p-8 text-sm text-slate-500">正在加载研究发布记录…</div>;
  }
  if (releasesQuery.error) {
    return <div className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-sm text-rose-700">{getApiErrorMessage(releasesQuery.error, '发布记录加载失败。')}</div>;
  }
  if (!releases.length) {
    return <div className="rounded-2xl border border-dashed border-slate-300 bg-white/60 p-10 text-center text-sm text-slate-500">暂无公开参与码发布。请先在问卷详情中完成发布。</div>;
  }

  const codePage = codesQuery.data;
  const pageCount = Math.max(1, Math.ceil((codePage?.total || 0) / 20));

  return (
    <div className="space-y-5">
      <div className="grid min-w-0 grid-cols-1 gap-4 lg:grid-cols-2">
        {releases.map((release) => (
          <button
            key={release.publishId}
            type="button"
            onClick={() => selectRelease(release.publishId)}
            className={`min-w-0 rounded-2xl border p-5 text-left transition ${selectedPublishId === release.publishId ? 'border-primary bg-primary/[0.06]' : 'border-slate-200 bg-white/70 hover:border-primary/40 dark:border-white/10 dark:bg-white/[0.03]'}`}
          >
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="min-w-0"><SectionEyebrow>{release.paperCode}</SectionEyebrow><h2 className="mt-2 break-words text-lg font-black">{release.paperTitle}</h2></div>
              <StatusBadge label={release.qrEntryEnabled ? '二维码已开启' : '仅参与码'} tone={release.qrEntryEnabled ? 'success' : 'warning'} />
            </div>
            <p className="mt-3 text-xs text-slate-500">发布于 {formatDateTime(release.publishedAt)} · {release.releaseCode}</p>
            <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-600 dark:text-white/55"><span>{release.unusedCount} 个未使用</span><span>·</span><span>{release.submittedCount} 份已提交</span><span>·</span><span>{release.qrParticipantCount} 份二维码参与</span></div>
          </button>
        ))}
      </div>

      {selectedRelease ? (
        <section className="space-y-6 rounded-2xl liquid-glass-panel p-4 sm:p-6">
          <div className="flex min-w-0 flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div className="min-w-0"><SectionEyebrow>PUBLIC ACCESS MANAGEMENT</SectionEyebrow><h2 className="mt-2 break-words text-2xl font-black">{selectedRelease.paperTitle}</h2><p className="mt-2 break-all text-sm text-slate-500">{`${window.location.origin}/research/${selectedRelease.releaseCode}`}</p></div>
            <a href={`/research/${selectedRelease.releaseCode}`} target="_blank" rel="noreferrer" className="inline-flex min-h-11 items-center justify-center gap-2 rounded-2xl border border-slate-200 px-4 py-2 text-sm font-bold dark:border-white/10">打开学生页 <ExternalLink size={15} /></a>
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-6">
            {metricCards(selectedRelease).map(([label, value]) => <div key={label} className="rounded-2xl border border-slate-200/80 bg-white/70 p-4 dark:border-white/10 dark:bg-white/[0.03]"><div className="text-xs text-slate-500">{label}</div><div className="mt-2 text-2xl font-black tabular-nums">{value}</div></div>)}
          </div>

          {actionError ? <div role="alert" className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">{actionError}</div> : null}
          {actionMessage ? <div role="status" className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">{actionMessage}</div> : null}

          <div className="grid gap-5 xl:grid-cols-[minmax(0,1.15fr)_minmax(20rem,0.85fr)]">
            <div className="rounded-2xl border border-slate-200 p-5 dark:border-white/10">
              <div className="flex flex-wrap items-center justify-between gap-3"><div><SectionEyebrow>CODE BATCH</SectionEyebrow><h3 className="mt-2 font-black">批量新增参与码</h3></div><KeyRound className="text-primary" /></div>
              <p className="mt-3 text-sm leading-6 text-slate-500">每批可生成 1–5000 个。系统只保存摘要，明文仅在生成成功后显示一次。</p>
              <div className="mt-4 flex flex-col gap-3 sm:flex-row">
                <input type="number" min={1} max={5000} value={batchCount} disabled={createdBatch != null || pendingAction != null} onChange={(event) => setBatchCount(Math.max(1, Math.min(5000, Number(event.target.value) || 1)))} className="min-h-11 w-full rounded-2xl border border-slate-200 bg-white px-4 disabled:opacity-50 sm:w-40 dark:border-white/10 dark:bg-slate-900" />
                <button type="button" disabled={createdBatch != null || pendingAction != null} onClick={createBatch} className="btn-liquid min-h-11 px-5 text-sm text-white disabled:opacity-50">{pendingAction === 'create-batch' ? '生成中…' : createdBatch ? '请先保存当前批次' : '生成新批次'}</button>
              </div>
              {createdBatch ? (
                <div className="mt-5 rounded-2xl border border-amber-300 bg-amber-50 p-4 text-amber-950">
                  <p className="font-black">请立即保存：刷新或离开后无法再次查看明文</p>
                  <p className="mt-1 text-xs">批次 {createdBatch.batchId} · {createdBatch.participationCodes.length} 个</p>
                  <textarea readOnly rows={7} value={createdBatch.participationCodes.join('\n')} className="mt-3 w-full rounded-xl border border-amber-200 bg-white p-3 font-mono text-xs" />
                  <div className="mt-3 flex flex-wrap gap-2"><button type="button" disabled={pendingAction != null} onClick={() => void copyCodes()} className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-amber-300 px-3 text-sm font-bold disabled:opacity-50"><Copy size={15} />{pendingAction === 'copy-codes' ? '复制中…' : '复制全部'}</button><button type="button" disabled={pendingAction != null} onClick={downloadCodes} className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-amber-300 px-3 text-sm font-bold disabled:opacity-50"><Download size={15} />下载 CSV</button><button type="button" disabled={pendingAction != null} onClick={() => setClearCreatedBatchOpen(true)} className="min-h-10 rounded-xl border border-amber-300 px-3 text-sm font-bold disabled:opacity-50">已保存，清除明文</button></div>
                </div>
              ) : null}
            </div>

            <div className="rounded-2xl border border-slate-200 p-5 dark:border-white/10">
              <div className="flex flex-wrap items-center justify-between gap-3"><div><SectionEyebrow>QR ENTRY</SectionEyebrow><h3 className="mt-2 font-black">二维码免码参与</h3></div><StatusBadge label={selectedRelease.qrEntryEnabled ? '已开启' : '未开启'} tone={selectedRelease.qrEntryEnabled ? 'success' : 'warning'} /></div>
              <div className="mt-4 flex flex-col items-center gap-4 sm:flex-row sm:items-start">
                {qrDataUrl ? <img src={qrDataUrl} alt={`${selectedRelease.paperTitle}二维码`} className="h-40 w-40 rounded-xl border border-slate-200 bg-white p-2" /> : <div className="flex h-40 w-40 items-center justify-center rounded-xl bg-slate-100"><QrCode /></div>}
                <div className="min-w-0 flex-1"><p className="break-all text-xs leading-5 text-slate-500">{qrUrl}</p>{qrGenerationError ? <p role="alert" className="mt-2 text-xs text-rose-700">{qrGenerationError}</p> : null}<div className="mt-3 grid grid-cols-2 gap-2"><button type="button" disabled={pendingAction != null} onClick={() => void copyQrUrl()} className="min-h-10 rounded-xl border border-slate-200 text-xs font-bold disabled:opacity-50 dark:border-white/10">{pendingAction === 'copy-qr-url' ? '复制中…' : '复制链接'}</button><button type="button" disabled={!qrDataUrl || pendingAction != null} onClick={downloadQr} className="min-h-10 rounded-xl border border-slate-200 text-xs font-bold disabled:opacity-50 dark:border-white/10">下载 PNG</button><button type="button" disabled={!qrDataUrl || pendingAction != null} onClick={printQr} className="inline-flex min-h-10 items-center justify-center gap-1 rounded-xl border border-slate-200 text-xs font-bold disabled:opacity-50 dark:border-white/10"><Printer size={14} />打印</button><button type="button" disabled={pendingAction != null} onClick={() => setQrToggleTarget({ enabled: !selectedRelease.qrEntryEnabled })} className={`min-h-10 rounded-xl text-xs font-bold text-white ${selectedRelease.qrEntryEnabled ? 'bg-rose-600' : 'bg-primary'}`}>{pendingAction === 'toggle-qr' ? '保存中…' : selectedRelease.qrEntryEnabled ? '关闭免码' : '开启免码'}</button></div></div>
              </div>
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 p-4 dark:border-white/10 sm:p-5">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between"><div><SectionEyebrow>PARTICIPATION CODES</SectionEyebrow><h3 className="mt-2 font-black">参与码状态与兑换 IP</h3></div><div className="flex flex-col gap-2 sm:flex-row"><select value={status} onChange={(event) => setStatus(event.target.value as '' | ParticipationCodeStatus)} className="min-h-11 rounded-xl border border-slate-200 bg-white px-3 text-sm dark:border-white/10 dark:bg-slate-900"><option value="">全部状态</option>{Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select><select value={batchId} onChange={(event) => setBatchId(event.target.value)} className="min-h-11 max-w-full rounded-xl border border-slate-200 bg-white px-3 text-sm dark:border-white/10 dark:bg-slate-900"><option value="">全部批次</option>{selectedRelease.batches.map((batch) => <option key={batch.batchId} value={batch.batchId}>{batch.batchId === 'legacy' ? '历史批次' : batch.batchId.slice(0, 8)} · {batch.totalCount} 个</option>)}</select></div></div>

            {selectedRelease.batches.some((batch) => batch.unusedCount > 0) ? <div className="mt-4 flex flex-wrap gap-2">{selectedRelease.batches.filter((batch) => batch.unusedCount > 0 && batch.batchId !== 'legacy').map((batch) => <button key={batch.batchId} type="button" onClick={() => setRevokeTarget({ type: 'batch', batchId: batch.batchId, label: `${batch.batchId.slice(0, 8)}（${batch.unusedCount} 个未使用）` })} className="inline-flex min-h-9 items-center gap-1 rounded-xl border border-rose-200 px-3 text-xs font-bold text-rose-700"><ShieldOff size={14} />停用批次 {batch.batchId.slice(0, 8)}</button>)}</div> : null}

            <div className="mt-5 overflow-x-auto">
              {codesQuery.error ? <div role="alert" className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700"><span>{getApiErrorMessage(codesQuery.error, '参与码列表加载失败。')}</span><button type="button" onClick={() => void codesQuery.refetch()} className="min-h-9 rounded-xl border border-rose-300 px-3 text-xs font-bold">重试</button></div> : null}
              <table className="min-w-[58rem] w-full text-left text-sm">
                <thead className="text-xs text-slate-500"><tr><th className="px-3 py-3">参与码</th><th className="px-3 py-3">状态</th><th className="px-3 py-3">批次</th><th className="px-3 py-3">最后兑换 IP</th><th className="px-3 py-3">首次兑换</th><th className="px-3 py-3">提交时间</th><th className="px-3 py-3">操作</th></tr></thead>
                <tbody>{(codePage?.records || []).map((code) => <tr key={code.codeId} className="border-t border-slate-200/80 dark:border-white/10"><td className="px-3 py-3 font-mono">{code.codeHint ? `••••-${code.codeHint}` : `历史参与码 #${code.codeId}`}</td><td className="px-3 py-3"><StatusBadge label={statusLabels[code.status] || code.status} tone={statusTone(code.status)} /></td><td className="px-3 py-3 font-mono text-xs">{code.exportBatchId ? code.exportBatchId.slice(0, 8) : 'legacy'}</td><td className="px-3 py-3 font-mono text-xs">{code.lastVerifiedIp || '—'}</td><td className="px-3 py-3 text-xs text-slate-500">{formatDateTime(code.firstVerifiedAt)}</td><td className="px-3 py-3 text-xs text-slate-500">{formatDateTime(code.submittedAt)}</td><td className="px-3 py-3">{code.status === 'UNUSED' ? <button type="button" onClick={() => setRevokeTarget({ type: 'code', codeId: code.codeId, label: code.codeHint ? `末四位 ${code.codeHint}` : `历史参与码 #${code.codeId}` })} className="text-xs font-bold text-rose-700">停用</button> : '—'}</td></tr>)}</tbody>
              </table>
              {!codesQuery.isLoading && !codePage?.records.length ? <div className="py-8 text-center text-sm text-slate-500">当前筛选下没有参与码。</div> : null}
            </div>
            <Pagination page={page} pageCount={pageCount} onPageChange={setPage} total={codePage?.total || 0} pageSize={20} itemLabel="个参与码" className="mt-4" disabled={codesQuery.isFetching} />
          </div>
        </section>
      ) : null}

      <ConfirmationDialog
        open={clearCreatedBatchOpen}
        title="清除本批参与码明文"
        description="清除后，本批参与码仍然有效，但无法再从服务器查看或导出明文。"
        safety="如果尚未复制或下载 CSV，请先返回保存；清除动作无法撤销。"
        nextStep="确认已妥善保存后再清除，即可继续生成下一批参与码。"
        confirmLabel="已保存，确认清除"
        cancelLabel="返回保存"
        onConfirm={() => {
          setCreatedBatch(null);
          setClearCreatedBatchOpen(false);
        }}
        onCancel={() => setClearCreatedBatchOpen(false)}
      />
      <ConfirmationDialog
        open={revokeTarget != null}
        title="停用未使用参与码"
        description={revokeTarget ? `即将停用 ${revokeTarget.label}。` : ''}
        safety="停用后该参与码不能用于创建答卷；进行中和已提交的答卷不会受到影响。"
        nextStep="确认目标无误后停用。此操作不会删除已有研究数据。"
        confirmLabel="确认停用"
        cancelLabel="取消"
        pending={pendingAction === 'revoke'}
        pendingTitle="正在停用…"
        onConfirm={confirmRevoke}
        onCancel={() => setRevokeTarget(null)}
      />
      <ConfirmationDialog
        open={qrToggleTarget != null}
        title={qrToggleTarget?.enabled ? '开启二维码免码参与' : '关闭二维码免码参与'}
        description={qrToggleTarget?.enabled
          ? '开启后，任何获得二维码链接的人都可以通过本设备浏览器指纹创建或恢复匿名答卷。'
          : '关闭后，新的二维码免码进入将被拒绝。'}
        safety={qrToggleTarget?.enabled
          ? '二维码不包含参与码或会话令牌；手工参与码仍可继续使用。'
          : '已经取得会话的进行中答卷仍可继续作答，不会删除已有数据。'}
        nextStep="确认发布范围和使用场景无误后继续。"
        confirmLabel={qrToggleTarget?.enabled ? '确认开启' : '确认关闭'}
        cancelLabel="取消"
        pending={pendingAction === 'toggle-qr'}
        pendingTitle="正在保存…"
        onConfirm={confirmToggleQr}
        onCancel={() => setQrToggleTarget(null)}
      />
      <ConfirmationDialog
        open={pendingSelectedPublishId != null}
        title="离开一次性参与码明文"
        description="切换发布后，本批参与码明文将从页面清除，之后无法从服务器重新导出。"
        safety="已经生成的参与码仍然有效，但未复制或下载的明文无法恢复。"
        nextStep="请确认已经复制或下载 CSV，再切换到其他发布。"
        confirmLabel="已保存，继续切换"
        cancelLabel="返回保存"
        onConfirm={confirmSelectRelease}
        onCancel={() => setPendingSelectedPublishId(null)}
      />
    </div>
  );
};
