import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { ClassItem } from '../types';
import { Plus, Users, ArrowRight, Loader2, Sparkles, BookOpen, AlertCircle, X } from 'lucide-react';

export const ClassesPage: React.FC = () => {
  const navigate = useNavigate();
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modal create class state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [createForm, setCreateForm] = useState({
    classCode: '',
    name: '',
    description: '',
  });
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const fetchClasses = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await api.get<ClassItem[]>('/api/classes');
      setClasses(res.data);
    } catch (err: any) {
      setError('Không thể tải danh sách lớp học. Vui lòng kiểm tra lại backend.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClasses();
  }, []);

  const handleCreateClass = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreating(true);
    setCreateError(null);

    try {
      const res = await api.post<ClassItem>('/api/classes', {
        classCode: createForm.classCode.trim().toUpperCase(),
        name: createForm.name.trim(),
        description: createForm.description.trim() || undefined,
      });
      setIsModalOpen(false);
      setCreateForm({ classCode: '', name: '', description: '' });
      setClasses([res.data, ...classes]);
      navigate(`/classes/${res.data.id}`);
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Có lỗi xảy ra khi tạo lớp học. Mã lớp có thể đã tồn tại.';
      setCreateError(msg);
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#0b0f19] text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header Section */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-6 border-b border-slate-800">
          <div>
            <div className="flex items-center space-x-2 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-1">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Quản lý học phần</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              Danh Sách Lớp Học
            </h1>
            <p className="text-sm text-slate-400 mt-1">
              Theo dõi sĩ số, tạo buổi điểm danh QR tức thì và xuất báo cáo điểm danh Excel
            </p>
          </div>

          <button
            onClick={() => setIsModalOpen(true)}
            className="inline-flex items-center justify-center space-x-2 px-4 py-2.5 rounded-xl font-semibold text-sm text-white bg-indigo-600 hover:bg-indigo-500 shadow-lg shadow-indigo-600/30 transition-all hover:scale-[1.02] active:scale-[0.98]"
          >
            <Plus className="w-4 h-4" />
            <span>Thêm Lớp Học Mới</span>
          </button>
        </div>

        {/* Content Section */}
        {loading ? (
          <div className="flex flex-col items-center justify-center py-20 text-slate-400">
            <Loader2 className="w-8 h-8 animate-spin text-indigo-500 mb-3" />
            <p className="text-sm">Đang tải danh sách lớp học...</p>
          </div>
        ) : error ? (
          <div className="mt-8 p-6 rounded-2xl bg-red-500/10 border border-red-500/20 text-center max-w-lg mx-auto">
            <AlertCircle className="w-8 h-8 text-red-400 mx-auto mb-2" />
            <p className="text-red-300 font-medium">{error}</p>
            <button
              onClick={fetchClasses}
              className="mt-4 px-4 py-2 rounded-lg bg-red-500/20 text-red-300 text-sm hover:bg-red-500/30 transition-colors"
            >
              Thử lại
            </button>
          </div>
        ) : classes.length === 0 ? (
          <div className="mt-12 text-center py-16 px-4 rounded-3xl glass-card border border-dashed border-slate-700 max-w-xl mx-auto">
            <div className="w-16 h-16 rounded-2xl bg-indigo-500/10 text-indigo-400 flex items-center justify-center mx-auto mb-4 border border-indigo-500/20">
              <BookOpen className="w-8 h-8" />
            </div>
            <h3 className="text-lg font-bold text-white mb-1">Chưa có lớp học nào</h3>
            <p className="text-sm text-slate-400 mb-6 max-w-sm mx-auto">
              Hãy tạo lớp học đầu tiên của bạn để nhập danh sách 50 sinh viên và bắt đầu điểm danh qua QR.
            </p>
            <button
              onClick={() => setIsModalOpen(true)}
              className="inline-flex items-center space-x-2 px-4 py-2 rounded-xl bg-indigo-600 text-white font-medium text-sm hover:bg-indigo-500 shadow-lg shadow-indigo-600/25 transition-all"
            >
              <Plus className="w-4 h-4" />
              <span>Tạo Lớp Học Ngay</span>
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-8">
            {classes.map((cls) => (
              <div
                key={cls.id}
                className="glass-card rounded-2xl p-6 border border-slate-800/80 hover:border-indigo-500/50 hover:shadow-xl hover:shadow-indigo-500/10 transition-all duration-300 flex flex-col justify-between group"
              >
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <span className="px-2.5 py-1 rounded-md text-xs font-bold uppercase tracking-wider bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                      {cls.classCode}
                    </span>
                  </div>

                  <h3 className="text-lg font-bold text-white group-hover:text-indigo-300 transition-colors line-clamp-1">
                    {cls.name}
                  </h3>

                  <p className="text-xs text-slate-400 mt-2 line-clamp-2 min-h-[32px]">
                    {cls.description || 'Không có mô tả chi tiết cho lớp học này.'}
                  </p>
                </div>

                <div className="mt-6 pt-4 border-t border-slate-800 flex items-center justify-between">
                  <div className="flex items-center text-xs text-slate-300">
                    <Users className="w-4 h-4 text-indigo-400 mr-1.5" />
                    <span>
                      <strong className="text-white font-bold">{cls.studentCount || 0}</strong> sinh viên
                    </span>
                  </div>

                  <Link
                    to={`/classes/${cls.id}`}
                    className="inline-flex items-center space-x-1 text-xs font-semibold text-indigo-400 hover:text-indigo-300 group-hover:translate-x-0.5 transition-all"
                  >
                    <span>Quản lý lớp</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Modal Create Class */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in">
          <div className="glass-panel w-full max-w-md rounded-2xl p-6 border border-slate-700 shadow-2xl relative">
            <button
              onClick={() => setIsModalOpen(false)}
              className="absolute top-4 right-4 p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>

            <h3 className="text-lg font-bold text-white mb-1">Tạo Lớp Học Mới</h3>
            <p className="text-xs text-slate-400 mb-5">
              Nhập mã lớp và thông tin cơ bản để quản lý danh sách sinh viên
            </p>

            {createError && (
              <div className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-xs text-red-400 flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{createError}</span>
              </div>
            )}

            <form onSubmit={handleCreateClass} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Mã Lớp Học (Ví dụ: IT101, CS202) *
                </label>
                <input
                  type="text"
                  required
                  placeholder="CS101"
                  value={createForm.classCode}
                  onChange={(e) => setCreateForm({ ...createForm, classCode: e.target.value.toUpperCase() })}
                  className="glass-input w-full px-3.5 py-2 rounded-xl text-sm text-white placeholder-slate-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Tên Môn Học / Lớp Học *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Nhập môn Lập trình Web"
                  value={createForm.name}
                  onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
                  className="glass-input w-full px-3.5 py-2 rounded-xl text-sm text-white placeholder-slate-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                  Mô Tả Bổ Sung
                </label>
                <textarea
                  rows={2}
                  placeholder="Lớp thứ 2 tiết 1-3, phòng Lab 402..."
                  value={createForm.description}
                  onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
                  className="glass-input w-full px-3.5 py-2 rounded-xl text-sm text-white placeholder-slate-500"
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-sm font-medium text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="px-4 py-2 rounded-xl text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-500 shadow-lg shadow-indigo-600/30 transition-all flex items-center space-x-2 disabled:opacity-70"
                >
                  {creating ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      <span>Đang tạo...</span>
                    </>
                  ) : (
                    <span>Tạo Lớp</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ClassesPage;
