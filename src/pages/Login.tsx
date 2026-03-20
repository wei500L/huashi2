import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { GraduationCap, ShieldCheck, Users } from 'lucide-react';
import { useAuthStore } from '@/store';
import { homePathForCapabilities } from '@/lib/format';

const loginSchema = z.object({
  usernameOrEmail: z.string().min(1, '请输入用户名或邮箱'),
  password: z.string().min(1, '请输入密码'),
});

type LoginFormData = z.infer<typeof loginSchema>;

const workspaceCards: Array<{
  label: string;
  hint: string;
  icon: React.ComponentType<{ size?: number; className?: string }>;
}> = [
  {
    label: '学生工作区',
    hint: '诊断 session、训练恢复、学情分析与错题复习',
    icon: GraduationCap,
  },
  {
    label: '教师工作区',
    hint: '班级总览、模板管理、词汇知识维护与干预工作台',
    icon: Users,
  },
  {
    label: '管理员控制台',
    hint: '用户管理、AI 配置与跨工作区联调',
    icon: ShieldCheck,
  },
];

const Login: React.FC = () => {
  const navigate = useNavigate();
  const { login, user, error, clearError } = useAuthStore();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      usernameOrEmail: '',
      password: '',
    },
  });

  React.useEffect(() => {
    if (user) {
      navigate(homePathForCapabilities(user.capabilities), { replace: true });
    }
  }, [navigate, user]);

  const onSubmit = async (values: LoginFormData) => {
    clearError();
    await login(values);
  };

  return (
    <div className="min-h-screen bg-background relative overflow-hidden">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(59,130,246,0.16),transparent_38%),radial-gradient(circle_at_bottom_right,rgba(14,165,233,0.12),transparent_32%)]" />
      <div className="relative z-10 min-h-screen flex items-center justify-center p-6">
        <div className="w-full max-w-6xl grid lg:grid-cols-[1.15fr_0.85fr] gap-8 items-stretch">
          <section className="liquid-glass-panel rounded-[3rem] edge-light p-10 md:p-14 flex flex-col justify-between min-h-[620px]">
            <div>
              <div className="inline-flex items-center gap-3 px-4 py-2 rounded-full border border-slate-200/80 dark:border-white/10 bg-white/60 dark:bg-white/5 text-xs font-black uppercase tracking-[0.24em] text-slate-500 dark:text-white/40">
                EF.Transfer
              </div>
              <h1 className="mt-8 text-5xl md:text-6xl font-black tracking-tight text-slate-900 dark:text-white leading-[1.05]">
                英法词汇迁移学习平台
              </h1>
              <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-500 dark:text-white/50">
                登录后系统会基于后端下发的 capability 进入对应工作区。开发环境如需演示账号，需要显式启用 demo data 开关。
              </p>
            </div>

            <div className="grid md:grid-cols-3 gap-4 mt-10">
              {workspaceCards.map((card) => (
                <div
                  key={card.label}
                  className="text-left rounded-[2rem] border border-slate-200/80 dark:border-white/10 bg-white/55 dark:bg-white/5 p-5"
                >
                  <card.icon size={18} className="text-primary mb-4" />
                  <div className="font-black text-slate-900 dark:text-white">{card.label}</div>
                  <div className="text-sm mt-2 text-slate-500 dark:text-white/45 leading-6">{card.hint}</div>
                </div>
              ))}
            </div>
          </section>

          <section className="liquid-glass rounded-[3rem] edge-light p-8 md:p-10 self-center">
            <div className="text-[10px] uppercase tracking-[0.3em] text-slate-400 dark:text-white/30">Account Login</div>
            <h2 className="mt-3 text-3xl font-black text-slate-900 dark:text-white">使用真实 JWT 会话登录</h2>
            <form className="mt-8 space-y-5" onSubmit={handleSubmit(onSubmit)}>
              <label className="block">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">用户名或邮箱</div>
                <input
                  {...register('usernameOrEmail')}
                  className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                  placeholder="输入用户名或邮箱"
                />
                {errors.usernameOrEmail && <div className="mt-2 text-sm text-rose-500">{errors.usernameOrEmail.message}</div>}
              </label>

              <label className="block">
                <div className="mb-2 text-sm font-bold text-slate-700 dark:text-white/70">密码</div>
                <input
                  type="password"
                  {...register('password')}
                  className="w-full rounded-2xl border border-slate-200 dark:border-white/10 bg-white/75 dark:bg-slate-950/40 px-4 py-3 outline-none focus:border-primary/50"
                  placeholder="输入密码"
                />
                {errors.password && <div className="mt-2 text-sm text-rose-500">{errors.password.message}</div>}
              </label>

              {error && <div className="rounded-2xl border border-rose-500/20 bg-rose-500/5 px-4 py-3 text-sm text-rose-500">{error}</div>}

              <button type="submit" disabled={isSubmitting} className="btn-liquid w-full py-4 text-white disabled:opacity-70">
                {isSubmitting ? '正在登录...' : '进入工作台'}
              </button>
            </form>
          </section>
        </div>
      </div>
    </div>
  );
};

export default Login;
