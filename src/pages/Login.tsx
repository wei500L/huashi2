import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { ShieldCheck, User, GraduationCap } from 'lucide-react';

const Login: React.FC = () => {
  const { login } = useAuthStore();
  const navigate = useNavigate();

  const handleLogin = (role: 'STUDENT' | 'TEACHER' | 'ADMIN') => {
    login(role);
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-4">
      <div className="max-w-md w-full space-y-8 bg-white p-10 rounded-2xl shadow-xl shadow-slate-200 border border-slate-100">
        <div className="text-center">
          <h1 className="text-3xl font-black bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">
            EF-Transfer
          </h1>
          <p className="mt-2 text-slate-500">英法双语认知迁移学习平台</p>
        </div>

        <div className="space-y-4">
          <p className="text-sm font-medium text-slate-700 text-center mb-6">选择身份进入演示环境</p>
          
          <button 
            onClick={() => handleLogin('STUDENT')}
            className="w-full flex items-center justify-between p-4 border-2 border-slate-100 rounded-xl hover:border-blue-500 hover:bg-blue-50 transition-all group"
          >
            <div className="flex items-center gap-4">
              <div className="p-2 bg-blue-100 text-blue-600 rounded-lg group-hover:bg-blue-600 group-hover:text-white transition-colors">
                <GraduationCap size={24} />
              </div>
              <div className="text-left">
                <p className="font-bold">我是学生 (Student)</p>
                <p className="text-xs text-slate-500">参与诊断、个性化训练与分析</p>
              </div>
            </div>
          </button>

          <button 
            onClick={() => handleLogin('TEACHER')}
            className="w-full flex items-center justify-between p-4 border-2 border-slate-100 rounded-xl hover:border-indigo-500 hover:bg-indigo-50 transition-all group"
          >
            <div className="flex items-center gap-4">
              <div className="p-2 bg-indigo-100 text-indigo-600 rounded-lg group-hover:bg-indigo-600 group-hover:text-white transition-colors">
                <User size={24} />
              </div>
              <div className="text-left">
                <p className="font-bold">我是教师 (Teacher)</p>
                <p className="text-xs text-slate-500">查看班级学情与干预建议</p>
              </div>
            </div>
          </button>

          <button 
            onClick={() => handleLogin('ADMIN')}
            className="w-full flex items-center justify-between p-4 border-2 border-slate-100 rounded-xl hover:border-slate-500 hover:bg-slate-50 transition-all group"
          >
            <div className="flex items-center gap-4">
              <div className="p-2 bg-slate-100 text-slate-600 rounded-lg group-hover:bg-slate-600 group-hover:text-white transition-colors">
                <ShieldCheck size={24} />
              </div>
              <div className="text-left">
                <p className="font-bold">系统管理员 (Admin)</p>
                <p className="text-xs text-slate-500">词表管理与系统配置</p>
              </div>
            </div>
          </button>
        </div>

        <p className="text-center text-xs text-slate-400">
          基于 React 19 + TypeScript + DDD 构建的 MVP 版本
        </p>
      </div>
    </div>
  );
};

export default Login;
