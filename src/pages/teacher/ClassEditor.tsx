import React from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, RefreshCw, Save } from 'lucide-react';
import { PageHeader, SectionEyebrow } from '@/components/common';
import type { TeacherClassUpsertRequest } from '@/lib/contracts';
import { teacherClassService } from '@/lib/services';

const emptyForm: TeacherClassUpsertRequest = {
  classCode: '',
  className: '',
  gradeName: '',
};

const TeacherClassEditorPage: React.FC = () => {
  const navigate = useNavigate();
  const params = useParams();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const source = searchParams.get('source');
  const classId = Number(params.classId);
  const isEditing = Number.isFinite(classId) && classId > 0;

  const [form, setForm] = React.useState<TeacherClassUpsertRequest>(emptyForm);
  const [feedback, setFeedback] = React.useState<string | null>(null);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const hasAutoFilledInviteCodeRef = React.useRef(false);
  const formRef = React.useRef<HTMLFormElement | null>(null);

  React.useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, []);

  const detailQuery = useQuery({
    queryKey: ['teacher-class-detail', classId],
    queryFn: ({ signal }) => teacherClassService.getDetail(classId, { signal }),
    enabled: isEditing,
  });

  React.useEffect(() => {
    if (!detailQuery.data) {
      return;
    }
    setForm({
      classCode: detailQuery.data.classCode,
      className: detailQuery.data.className,
      gradeName: detailQuery.data.gradeName,
    });
  }, [detailQuery.data]);

  const inviteCodeMutation = useMutation({
    mutationFn: () => teacherClassService.generateInviteCode(),
    onSuccess: ({ classCode }) => {
      setForm((current) => ({ ...current, classCode }));
      setErrorMessage(null);
      hasAutoFilledInviteCodeRef.current = true;
    },
    onError: (error) => {
      setErrorMessage(error instanceof Error ? error.message : '邀请码生成失败');
    },
  });

  const handleGenerateInviteCode = React.useCallback(() => {
    inviteCodeMutation.mutate();
  }, [inviteCodeMutation]);

  React.useEffect(() => {
    if (isEditing || hasAutoFilledInviteCodeRef.current || form.classCode.trim()) {
      return;
    }
    hasAutoFilledInviteCodeRef.current = true;
    handleGenerateInviteCode();
  }, [form.classCode, handleGenerateInviteCode, isEditing]);

  const mutation = useMutation({
    mutationFn: () =>
      isEditing
        ? teacherClassService.updateClass(classId, {
            classCode: form.classCode.trim(),
            className: form.className.trim(),
            gradeName: form.gradeName.trim(),
          })
        : teacherClassService.createClass({
            classCode: form.classCode.trim(),
            className: form.className.trim(),
            gradeName: form.gradeName.trim(),
          }),
    onSuccess: async (detail) => {
      setFeedback(isEditing ? '班级信息已更新。' : '班级已创建。接下来可以去分配学生。');
      setErrorMessage(null);
      await queryClient.invalidateQueries({ queryKey: ['teacher-classes-management'] });
      await queryClient.invalidateQueries({ queryKey: ['teacher-class-detail', detail.classId] });
      const nextPath = source
        ? `/teacher/classes/${detail.classId}?source=${encodeURIComponent(source)}`
        : `/teacher/classes/${detail.classId}`;
      navigate(nextPath);
    },
    onError: (error) => {
      setFeedback(null);
      setErrorMessage(error instanceof Error ? error.message : '班级保存失败');
    },
  });

  const backPath = React.useMemo(() => {
    if (isEditing) {
      return source ? `/teacher/classes/${classId}?source=${encodeURIComponent(source)}` : `/teacher/classes/${classId}`;
    }
    return source ? `/teacher/classes?source=${encodeURIComponent(source)}` : '/teacher/classes';
  }, [classId, isEditing, source]);

  const canSubmit = form.classCode.trim() && form.className.trim() && form.gradeName.trim();
  const isGeneratingInviteCode = inviteCodeMutation.isPending;

  const handleSubmit = React.useCallback((event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!canSubmit || mutation.isPending) {
      return;
    }
    mutation.mutate();
  }, [canSubmit, mutation]);

  return (
    <div className="space-y-8 pb-20">
      <PageHeader
        eyebrow="班级管理"
        title={isEditing ? '编辑班级' : '新建班级'}
        subtitle={isEditing ? '先把班级基本信息改准确，再回到详情页继续调整学生名册。' : '班级是教师工作流的入口。先建立班级，再补齐学生名册和后续教学动作。'}
        actions={
          <button
            type="button"
            onClick={() => formRef.current?.requestSubmit()}
            disabled={!canSubmit || mutation.isPending}
            className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white disabled:cursor-not-allowed disabled:opacity-60"
          >
            <Save size={16} />
            {mutation.isPending ? '保存中…' : '保存班级'}
          </button>
        }
      />

      <Link
        to={backPath}
        className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 transition hover:text-slate-900 dark:text-white/45 dark:hover:text-white"
      >
        <ArrowLeft size={14} />
        返回班级
      </Link>

      <form ref={formRef} className="space-y-8" onSubmit={handleSubmit}>
        {(detailQuery.error || errorMessage) && (
          <div className="rounded-[2rem] border border-rose-500/20 bg-rose-500/5 p-6 text-rose-500">
            {errorMessage || detailQuery.error?.message}
          </div>
        )}

        {feedback && (
          <div className="rounded-[2rem] border border-emerald-500/20 bg-emerald-500/10 p-6 text-emerald-700 dark:text-emerald-300">
            {feedback}
          </div>
        )}

        <section className="rounded-[2.5rem] liquid-glass-panel p-8">
          <SectionEyebrow className="mb-6">基础信息</SectionEyebrow>
          <div className="grid gap-6 md:grid-cols-2">
            <label className="space-y-3">
              <div className="flex items-center justify-between gap-3">
                <div className="text-sm font-semibold text-slate-700 dark:text-white/80">班级邀请码</div>
                <button
                  type="button"
                  onClick={handleGenerateInviteCode}
                  disabled={isGeneratingInviteCode || mutation.isPending}
                  className="inline-flex items-center gap-2 rounded-full border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-600 transition hover:border-primary/40 hover:text-primary disabled:cursor-not-allowed disabled:opacity-60 dark:border-white/10 dark:text-white/70"
                >
                  <RefreshCw size={12} className={isGeneratingInviteCode ? 'animate-spin' : undefined} />
                  {form.classCode.trim() ? '重新生成' : '生成邀请码'}
                </button>
              </div>
              <input
                value={form.classCode}
                onChange={(event) => setForm((current) => ({ ...current, classCode: event.target.value }))}
                placeholder="点击右侧按钮生成，也可手动调整"
                className="w-full rounded-[1.4rem] border border-slate-200 bg-white/70 px-4 py-3 text-sm outline-none transition focus:border-primary dark:border-white/10 dark:bg-white/5"
              />
              <div className="text-xs leading-5 text-slate-500 dark:text-white/45">
                系统会生成可分享的邀请码。学生在 `/register` 输入后，会自动加入当前班级。
              </div>
            </label>

            <label className="space-y-3">
              <div className="text-sm font-semibold text-slate-700 dark:text-white/80">班级名称</div>
              <input
                value={form.className}
                onChange={(event) => setForm((current) => ({ ...current, className: event.target.value }))}
                placeholder="例如 法语迁移实验班"
                className="w-full rounded-[1.4rem] border border-slate-200 bg-white/70 px-4 py-3 text-sm outline-none transition focus:border-primary dark:border-white/10 dark:bg-white/5"
              />
            </label>

            <label className="space-y-3 md:col-span-2">
              <div className="text-sm font-semibold text-slate-700 dark:text-white/80">年级 / 学段</div>
              <input
                value={form.gradeName}
                onChange={(event) => setForm((current) => ({ ...current, gradeName: event.target.value }))}
                placeholder="例如 2026 春法语二外"
                className="w-full rounded-[1.4rem] border border-slate-200 bg-white/70 px-4 py-3 text-sm outline-none transition focus:border-primary dark:border-white/10 dark:bg-white/5"
              />
            </label>
          </div>
        </section>

        <section className="rounded-[2rem] border border-slate-200/70 bg-white/60 p-6 dark:border-white/10 dark:bg-white/[0.03]">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="text-sm text-slate-500 dark:text-white/45">
              保存后会直接进入班级详情页，继续分配学生或发布测评。
            </div>
            <button
              type="submit"
              disabled={!canSubmit || mutation.isPending}
              className="btn-liquid inline-flex items-center gap-2 px-5 py-3 text-white disabled:cursor-not-allowed disabled:opacity-60"
            >
              <Save size={16} />
              {mutation.isPending ? '保存中…' : '保存班级'}
            </button>
          </div>
        </section>
      </form>
    </div>
  );
};

export default TeacherClassEditorPage;
