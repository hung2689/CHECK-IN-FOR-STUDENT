import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { ClassItem, Student, AttendanceSession, ImportResult } from '../types';
import {
  Users,
  Calendar,
  Plus,
  Upload,
  Download,
  Search,
  ArrowLeft,
  Loader2,
  QrCode,
  FileSpreadsheet,
  CheckCircle2,
  AlertCircle,
  Clock,
  ChevronRight,
  X,
  Edit2,
  Trash2,
} from 'lucide-react';

export const ClassDetailPage: React.FC = () => {
  const { classId } = useParams<{ classId: string }>();

  const [activeTab, setActiveTab] = useState<'students' | 'sessions'>('students');
  const [classData, setClassData] = useState<ClassItem | null>(null);
  const [students, setStudents] = useState<Student[]>([]);
  const [sessions, setSessions] = useState<AttendanceSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  // Add student modal
  const [isAddStudentOpen, setIsAddStudentOpen] = useState(false);
  const [newStudent, setNewStudent] = useState({ studentCode: '', fullName: '', email: '' });
  const [addingStudent, setAddingStudent] = useState(false);
  const [addStudentError, setAddStudentError] = useState<string | null>(null);

  // Edit student modal
  const [editingStudent, setEditingStudent] = useState<Student | null>(null);
  const [editForm, setEditForm] = useState({ studentCode: '', fullName: '', email: '' });
  const [savingEdit, setSavingEdit] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  // Import Excel modal & state
  const [isImportOpen, setIsImportOpen] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState<ImportResult | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Create session modal
  const [isCreateSessionOpen, setIsCreateSessionOpen] = useState(false);
  const [newSession, setNewSession] = useState({
    name: '',
    attendanceDate: new Date().toISOString().split('T')[0],
    description: '',
  });
  const [creatingSession, setCreatingSession] = useState(false);
  const [createSessionError, setCreateSessionError] = useState<string | null>(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [classRes, studentsRes, sessionsRes] = await Promise.all([
        api.get<ClassItem>(`/api/classes/${classId}`),
        api.get<Student[]>(`/api/classes/${classId}/students`),
        api.get<AttendanceSession[]>(`/api/classes/${classId}/attendance-sessions`),
      ]);
      setClassData(classRes.data);
      setStudents(studentsRes.data);
      setSessions(sessionsRes.data);
    } catch (err) {
      console.error('Failed to load class details', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (classId) fetchData();
  }, [classId]);

  // Handle Add Student
  const handleAddStudent = async (e: React.FormEvent) => {
    e.preventDefault();
    setAddingStudent(true);
    setAddStudentError(null);
    try {
      const res = await api.post<Student>(`/api/classes/${classId}/students`, {
        studentCode: newStudent.studentCode.trim().toUpperCase(),
        fullName: newStudent.fullName.trim(),
        email: newStudent.email.trim() || undefined,
      });
      setStudents([...students, res.data]);
      setIsAddStudentOpen(false);
      setNewStudent({ studentCode: '', fullName: '', email: '' });
    } catch (err: any) {
      setAddStudentError(err.response?.data?.message || 'Không thể thêm sinh viên. Mã SV có thể đã tồn tại.');
    } finally {
      setAddingStudent(false);
    }
  };

  // Open Edit Student Modal
  const openEditModal = (student: Student) => {
    setEditingStudent(student);
    setEditForm({
      studentCode: student.studentCode,
      fullName: student.fullName,
      email: student.email || '',
    });
    setEditError(null);
  };

  // Handle Save Edit Student
  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingStudent) return;
    setSavingEdit(true);
    setEditError(null);

    try {
      const res = await api.put<Student>(`/api/classes/${classId}/students/${editingStudent.id}`, {
        studentCode: editForm.studentCode.trim().toUpperCase(),
        fullName: editForm.fullName.trim(),
        email: editForm.email.trim() || undefined,
      });
      setStudents(students.map((s) => (s.id === editingStudent.id ? res.data : s)));
      setEditingStudent(null);
    } catch (err: any) {
      setEditError(err.response?.data?.message || 'Không thể cập nhật thông tin sinh viên.');
    } finally {
      setSavingEdit(false);
    }
  };

  // Handle Delete Student from Class
  const handleDeleteStudent = async (student: Student) => {
    const confirmed = window.confirm(
      `Bạn có chắc chắn muốn xóa sinh viên ${student.fullName} (MSSV: ${student.studentCode}) khỏi lớp này?`
    );
    if (!confirmed) return;

    try {
      await api.delete(`/api/classes/${classId}/students/${student.id}`);
      setStudents(students.filter((s) => s.id !== student.id));
    } catch (err: any) {
      alert(err.response?.data?.message || 'Không thể xóa sinh viên khỏi lớp.');
    }
  };

  // Handle Delete Attendance Session
  const handleDeleteSession = async (session: AttendanceSession) => {
    const confirmed = window.confirm(
      `CẢNH BÁO: Bạn có chắc muốn xóa vĩnh viễn buổi điểm danh "${session.name}" ngày ${session.attendanceDate}?\nToàn bộ lịch sử quét mã và dữ liệu điểm danh của buổi này sẽ bị xóa.`
    );
    if (!confirmed) return;

    try {
      await api.delete(`/api/attendance-sessions/${session.id}`);
      setSessions(sessions.filter((s) => s.id !== session.id));
    } catch (err: any) {
      alert(err.response?.data?.message || 'Không thể xóa buổi điểm danh.');
    }
  };

  // Handle Excel Template Download
  const handleDownloadTemplate = async () => {
    try {
      const response = await api.get(`/api/classes/${classId}/students/template`, {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `danh_sach_sinh_vien_mau_${classData?.classCode || 'class'}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Không thể tải file mẫu. Vui lòng thử lại.');
    }
  };

  // Handle Excel Upload
  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setImporting(true);
    setImportResult(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const res = await api.post<ImportResult>(`/api/classes/${classId}/students/import`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setImportResult(res.data);
      const updated = await api.get<Student[]>(`/api/classes/${classId}/students`);
      setStudents(updated.data);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Lỗi khi nhập file Excel. Hãy đảm bảo đúng định dạng .xlsx/.xls');
    } finally {
      setImporting(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  // Handle Create Session
  const handleCreateSession = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreatingSession(true);
    setCreateSessionError(null);

    try {
      const res = await api.post<AttendanceSession>(`/api/classes/${classId}/attendance-sessions`, {
        name: newSession.name.trim(),
        attendanceDate: newSession.attendanceDate,
        description: newSession.description.trim() || undefined,
      });
      setSessions([res.data, ...sessions]);
      setIsCreateSessionOpen(false);
      setNewSession({
        name: '',
        attendanceDate: new Date().toISOString().split('T')[0],
        description: '',
      });
    } catch (err: any) {
      setCreateSessionError(err.response?.data?.message || 'Không thể tạo buổi điểm danh.');
    } finally {
      setCreatingSession(false);
    }
  };

  // Export report Excel
  const handleExportReport = async (sessionId: number, name: string) => {
    try {
      const response = await api.get(`/api/attendance-sessions/${sessionId}/export`, {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `diem_danh_${classData?.classCode}_${name}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Không thể xuất file báo cáo.');
    }
  };

  const filteredStudents = students.filter(
    (s) =>
      s.studentCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.fullName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (loading) {
    return (
      <div className="min-h-screen bg-[#0b0f19] flex flex-col">
        <Navbar />
        <div className="flex-1 flex flex-col items-center justify-center text-slate-400">
          <Loader2 className="w-8 h-8 animate-spin text-indigo-500 mb-3" />
          <p className="text-sm">Đang tải dữ liệu lớp học...</p>
        </div>
      </div>
    );
  }

  if (!classData) {
    return (
      <div className="min-h-screen bg-[#0b0f19] flex flex-col">
        <Navbar />
        <div className="flex-1 flex flex-col items-center justify-center text-slate-400">
          <p className="text-base text-red-400 mb-4">Không tìm thấy thông tin lớp học</p>
          <Link to="/classes" className="text-indigo-400 hover:underline flex items-center space-x-1 text-sm">
            <ArrowLeft className="w-4 h-4" />
            <span>Quay lại danh sách lớp</span>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0b0f19] text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Breadcrumb */}
        <div className="flex items-center space-x-2 text-xs text-slate-400 mb-4">
          <Link to="/classes" className="hover:text-white transition-colors">
            Lớp học
          </Link>
          <ChevronRight className="w-3.5 h-3.5 text-slate-600" />
          <span className="text-indigo-400 font-semibold">{classData.classCode}</span>
        </div>

        {/* Class Banner Card */}
        <div className="glass-panel rounded-2xl p-6 sm:p-8 border border-slate-800 mb-8 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div>
            <div className="flex items-center space-x-3 mb-2">
              <span className="px-3 py-1 rounded-lg text-xs font-bold uppercase tracking-wider bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                {classData.classCode}
              </span>
            </div>

            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              {classData.name}
            </h1>

            {classData.description && (
              <p className="text-sm text-slate-400 mt-2 max-w-2xl">{classData.description}</p>
            )}

            <div className="flex items-center space-x-6 mt-4 pt-4 border-t border-slate-800/80 text-xs text-slate-300">
              <div className="flex items-center space-x-2">
                <Users className="w-4 h-4 text-indigo-400" />
                <span>
                  Sĩ số: <strong className="text-white font-bold">{students.length}</strong> sinh viên
                </span>
              </div>
              <div className="flex items-center space-x-2">
                <Clock className="w-4 h-4 text-indigo-400" />
                <span>
                  Đã tạo: <strong className="text-white font-bold">{sessions.length}</strong> buổi điểm danh
                </span>
              </div>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <button
              onClick={() => setIsCreateSessionOpen(true)}
              className="inline-flex items-center space-x-2 px-5 py-2.5 rounded-xl font-semibold text-sm text-white bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 shadow-lg shadow-indigo-600/30 transition-all hover:scale-[1.02] active:scale-[0.98]"
            >
              <QrCode className="w-4 h-4" />
              <span>Tạo Buổi Điểm Danh Mới</span>
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex border-b border-slate-800 mb-6 space-x-8">
          <button
            onClick={() => setActiveTab('students')}
            className={`pb-3 text-sm font-semibold flex items-center space-x-2 relative transition-colors ${
              activeTab === 'students' ? 'text-indigo-400' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Users className="w-4 h-4" />
            <span>Danh Sách Sinh Viên ({students.length})</span>
            {activeTab === 'students' && (
              <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-500 rounded-full" />
            )}
          </button>

          <button
            onClick={() => setActiveTab('sessions')}
            className={`pb-3 text-sm font-semibold flex items-center space-x-2 relative transition-colors ${
              activeTab === 'sessions' ? 'text-indigo-400' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Clock className="w-4 h-4" />
            <span>Lịch Sử Buổi Điểm Danh ({sessions.length})</span>
            {activeTab === 'sessions' && (
              <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-500 rounded-full" />
            )}
          </button>
        </div>

        {/* TAB 1: STUDENTS */}
        {activeTab === 'students' && (
          <div>
            {/* Action Bar */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
              <div className="relative max-w-sm w-full">
                <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
                <input
                  type="text"
                  placeholder="Tìm theo MSSV hoặc Họ tên..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="glass-input w-full pl-10 pr-4 py-2 rounded-xl text-xs text-white placeholder-slate-500"
                />
              </div>

              <div className="flex flex-wrap items-center gap-2.5">
                <button
                  onClick={handleDownloadTemplate}
                  className="inline-flex items-center space-x-1.5 px-3 py-2 rounded-xl text-xs font-medium text-slate-300 hover:text-white glass-card hover:bg-slate-800 transition-colors"
                >
                  <Download className="w-3.5 h-3.5 text-indigo-400" />
                  <span>File Excel Mẫu</span>
                </button>

                <button
                  onClick={() => setIsImportOpen(true)}
                  className="inline-flex items-center space-x-1.5 px-3 py-2 rounded-xl text-xs font-semibold text-emerald-300 bg-emerald-500/10 hover:bg-emerald-500/20 border border-emerald-500/30 transition-colors"
                >
                  <Upload className="w-3.5 h-3.5" />
                  <span>Nhập Từ Excel</span>
                </button>

                <button
                  onClick={() => setIsAddStudentOpen(true)}
                  className="inline-flex items-center space-x-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-500 shadow-md shadow-indigo-600/20 transition-all"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Thêm Sinh Viên</span>
                </button>
              </div>
            </div>

            {/* Students Table */}
            {filteredStudents.length === 0 ? (
              <div className="text-center py-12 px-4 rounded-2xl glass-card border border-dashed border-slate-800">
                <Users className="w-10 h-10 text-slate-600 mx-auto mb-2" />
                <p className="text-sm text-slate-400">
                  {searchQuery ? 'Không tìm thấy sinh viên phù hợp.' : 'Chưa có sinh viên nào trong lớp.'}
                </p>
                {!searchQuery && (
                  <button
                    onClick={() => setIsImportOpen(true)}
                    className="mt-4 px-4 py-2 rounded-xl text-xs font-semibold text-indigo-300 bg-indigo-500/10 hover:bg-indigo-500/20 border border-indigo-500/30"
                  >
                    Nhập danh sách bằng file Excel ngay
                  </button>
                )}
              </div>
            ) : (
              <div className="glass-panel rounded-2xl border border-slate-800/80 overflow-hidden shadow-xl">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-900/90 text-slate-400 uppercase tracking-wider text-[11px] border-b border-slate-800">
                      <tr>
                        <th className="py-3.5 px-4 font-semibold w-12 text-center">STT</th>
                        <th className="py-3.5 px-4 font-semibold">Mã Sinh Viên (MSSV)</th>
                        <th className="py-3.5 px-4 font-semibold">Họ và Tên</th>
                        <th className="py-3.5 px-4 font-semibold">Email / Khoa</th>
                        <th className="py-3.5 px-4 font-semibold text-center w-28">Thao Tác</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/50">
                      {filteredStudents.map((s, idx) => (
                        <tr key={s.id} className="hover:bg-slate-800/40 transition-colors group">
                          <td className="py-3 px-4 text-center text-slate-500 font-mono">{idx + 1}</td>
                          <td className="py-3 px-4 font-mono font-bold text-indigo-400">{s.studentCode}</td>
                          <td className="py-3 px-4 font-medium text-white">{s.fullName}</td>
                          <td className="py-3 px-4 text-slate-400">{s.email || '-'}</td>
                          <td className="py-3 px-4 text-center">
                            <div className="flex items-center justify-center space-x-2">
                              <button
                                onClick={() => openEditModal(s)}
                                className="p-1.5 rounded-lg text-slate-400 hover:text-indigo-300 hover:bg-indigo-500/10 transition-colors"
                                title="Sửa thông tin sinh viên"
                              >
                                <Edit2 className="w-3.5 h-3.5" />
                              </button>
                              <button
                                onClick={() => handleDeleteStudent(s)}
                                className="p-1.5 rounded-lg text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-colors"
                                title="Xóa sinh viên khỏi lớp"
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        )}

        {/* TAB 2: SESSIONS */}
        {activeTab === 'sessions' && (
          <div>
            {sessions.length === 0 ? (
              <div className="text-center py-16 px-4 rounded-2xl glass-card border border-dashed border-slate-800 max-w-md mx-auto">
                <QrCode className="w-12 h-12 text-slate-600 mx-auto mb-3" />
                <h3 className="text-base font-bold text-white mb-1">Chưa có buổi điểm danh nào</h3>
                <p className="text-xs text-slate-400 mb-6">
                  Tạo buổi điểm danh để trình chiếu mã QR cho sinh viên quét lúc vào lớp (Check-in) hoặc lúc ra về (Check-out).
                </p>
                <button
                  onClick={() => setIsCreateSessionOpen(true)}
                  className="inline-flex items-center space-x-2 px-4 py-2.5 rounded-xl bg-indigo-600 text-white font-medium text-xs hover:bg-indigo-500 shadow-lg shadow-indigo-600/30 transition-all"
                >
                  <Plus className="w-4 h-4" />
                  <span>Tạo Buổi Điểm Danh Ngay</span>
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                {sessions.map((sess) => {
                  const isCheckInLive = sess.checkInStatus === 'OPEN';
                  const isCheckOutLive = sess.checkOutStatus === 'OPEN';
                  const isLive = isCheckInLive || isCheckOutLive;

                  return (
                    <div
                      key={sess.id}
                      className={`glass-card rounded-2xl p-5 border transition-all ${
                        isLive
                          ? 'border-indigo-500/50 shadow-lg shadow-indigo-500/10'
                          : 'border-slate-800 hover:border-slate-700'
                      }`}
                    >
                      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                        <div>
                          <div className="flex items-center space-x-2.5 mb-1.5">
                            <span className="text-xs text-slate-400 font-medium flex items-center gap-1">
                              <Calendar className="w-3.5 h-3.5 text-slate-500" />
                              {sess.attendanceDate}
                            </span>

                            {/* Status badge */}
                            {isCheckInLive && (
                              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 animate-pulse">
                                ● Đang mở Check-in
                              </span>
                            )}
                            {isCheckOutLive && (
                              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/20 text-amber-400 border border-amber-500/30 animate-pulse">
                                ● Đang mở Check-out
                              </span>
                            )}
                            {!isLive && sess.status === 'COMPLETED' && (
                              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-medium bg-slate-700/60 text-slate-300">
                                Đã kết thúc
                              </span>
                            )}
                            {!isLive && sess.status === 'PENDING' && (
                              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-medium bg-indigo-500/20 text-indigo-300">
                                Chưa bắt đầu
                              </span>
                            )}
                          </div>

                          <h3 className="text-base font-bold text-white">{sess.name}</h3>

                          {/* Stats counters */}
                          <div className="flex items-center space-x-4 mt-3 text-xs text-slate-400">
                            <div>
                              Đã Check-in:{' '}
                              <strong className="text-emerald-400 font-bold">{sess.checkedInCount}</strong>/
                              {sess.totalStudents}
                            </div>
                            <div>
                              Đã Check-out:{' '}
                              <strong className="text-amber-400 font-bold">{sess.checkedOutCount}</strong>/
                              {sess.totalStudents}
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center space-x-2.5">
                          <button
                            onClick={() => handleExportReport(sess.id, sess.name)}
                            className="inline-flex items-center space-x-1.5 px-3 py-2 rounded-xl text-xs font-semibold text-slate-300 hover:text-white glass-panel hover:bg-slate-800 transition-colors"
                          >
                            <FileSpreadsheet className="w-3.5 h-3.5 text-emerald-400" />
                            <span>Xuất Excel</span>
                          </button>

                          <Link
                            to={`/sessions/${sess.id}`}
                            className="inline-flex items-center space-x-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-500 shadow-md shadow-indigo-600/25 transition-all"
                          >
                            <QrCode className="w-3.5 h-3.5" />
                            <span>Phòng Live</span>
                          </Link>

                          <button
                            onClick={() => handleDeleteSession(sess)}
                            className="p-2 rounded-xl text-slate-400 hover:text-red-400 hover:bg-red-500/10 border border-transparent hover:border-red-500/20 transition-colors"
                            title="Xóa lịch sử buổi điểm danh này"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </main>

      {/* Modal Add Student */}
      {isAddStudentOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div className="glass-panel w-full max-w-md rounded-2xl p-6 border border-slate-700 shadow-2xl relative">
            <button
              onClick={() => setIsAddStudentOpen(false)}
              className="absolute top-4 right-4 p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
            >
              <X className="w-5 h-5" />
            </button>

            <h3 className="text-lg font-bold text-white mb-1">Thêm Sinh Viên Mới</h3>
            <p className="text-xs text-slate-400 mb-5">Thêm sinh viên vào danh sách lớp {classData.classCode}</p>

            {addStudentError && (
              <div className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-xs text-red-400 flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{addStudentError}</span>
              </div>
            )}

            <form onSubmit={handleAddStudent} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Mã Số Sinh Viên (MSSV) *
                </label>
                <input
                  type="text"
                  required
                  placeholder="20210001"
                  value={newStudent.studentCode}
                  onChange={(e) => setNewStudent({ ...newStudent, studentCode: e.target.value.trim().toUpperCase() })}
                  className="glass-input w-full px-3.5 py-2 rounded-xl text-sm text-white placeholder-slate-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Họ và Tên *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Nguyễn Văn A"
                  value={newStudent.fullName}
                  onChange={(e) => setNewStudent({ ...newStudent, fullName: e.target.value })}
                  className="glass-input w-full px-3.5 py-2 rounded-xl text-sm text-white placeholder-slate-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Email / Ngành học
                </label>
                <input
                  type="text"
                  placeholder="student@university.edu.vn"
                  value={newStudent.email}
                  onChange={(e) => setNewStudent({ ...newStudent, email: e.target.value })}
                  className="glass-input w-full px-3.5 py-2 rounded-xl text-sm text-white placeholder-slate-500"
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsAddStudentOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={addingStudent}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-500 shadow-md shadow-indigo-600/30 flex items-center space-x-2"
                >
                  {addingStudent ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <span>Thêm Sinh Viên</span>}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Edit Student */}
      {editingStudent && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div className="glass-panel w-full max-w-md rounded-2xl p-6 border border-slate-700 shadow-2xl relative">
            <button
              onClick={() => setEditingStudent(null)}
              className="absolute top-4 right-4 p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
            >
              <X className="w-5 h-5" />
            </button>

            <h3 className="text-lg font-bold text-white mb-1">Chỉnh Sửa Sinh Viên</h3>
            <p className="text-xs text-slate-400 mb-5">
              Cập nhật thông tin sinh viên {editingStudent.fullName}
            </p>

            {editError && (
              <div className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-xs text-red-400 flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{editError}</span>
              </div>
            )}

            <form onSubmit={handleSaveEdit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Mã Số Sinh Viên (MSSV) *
                </label>
                <input
                  type="text"
                  required
                  value={editForm.studentCode}
                  onChange={(e) => setEditForm({ ...editForm, studentCode: e.target.value.trim().toUpperCase() })}
                  className="glass-input w-full px-3.5 py-2 rounded-xl text-sm text-white placeholder-slate-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Họ và Tên *
                </label>
                <input
                  type="text"
                  required
                  value={editForm.fullName}
                  onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })}
                  className="glass-input w-full px-3.5 py-2 rounded-xl text-sm text-white placeholder-slate-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Email / Ngành học
                </label>
                <input
                  type="text"
                  value={editForm.email}
                  onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                  className="glass-input w-full px-3.5 py-2 rounded-xl text-sm text-white placeholder-slate-500"
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setEditingStudent(null)}
                  className="px-4 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={savingEdit}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-500 shadow-md shadow-indigo-600/30 flex items-center space-x-2"
                >
                  {savingEdit ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <span>Lưu Thay Đổi</span>}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Import Excel */}
      {isImportOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div className="glass-panel w-full max-w-lg rounded-2xl p-6 border border-slate-700 shadow-2xl relative">
            <button
              onClick={() => {
                setIsImportOpen(false);
                setImportResult(null);
              }}
              className="absolute top-4 right-4 p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
            >
              <X className="w-5 h-5" />
            </button>

            <h3 className="text-lg font-bold text-white mb-1">Nhập Danh Sách Từ Excel</h3>
            <p className="text-xs text-slate-400 mb-5">
              Hỗ trợ file .xlsx hoặc .xls với danh sách khoảng 50 sinh viên
            </p>

            {/* Instruction Box */}
            <div className="p-3.5 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-xs text-indigo-300 mb-5 space-y-1">
              <p className="font-semibold text-white">Cột trong file Excel (theo file mẫu):</p>
              <p>• Cột 1: studentCode (MSSV)</p>
              <p>• Cột 2: fullName (Họ và tên)</p>
              <p>• Cột 3: email (Email)</p>
            </div>

            {/* Upload Area */}
            <div className="border-2 border-dashed border-slate-700 rounded-2xl p-6 text-center hover:border-indigo-500 transition-colors mb-4">
              <input
                type="file"
                ref={fileInputRef}
                accept=".xlsx, .xls"
                onChange={handleFileUpload}
                disabled={importing}
                className="hidden"
                id="excel-upload"
              />
              <label htmlFor="excel-upload" className="cursor-pointer block">
                {importing ? (
                  <div className="flex flex-col items-center">
                    <Loader2 className="w-8 h-8 animate-spin text-indigo-400 mb-2" />
                    <span className="text-xs text-slate-300">Đang đọc và xử lý file Excel...</span>
                  </div>
                ) : (
                  <div className="flex flex-col items-center">
                    <Upload className="w-8 h-8 text-indigo-400 mb-2" />
                    <span className="text-xs font-semibold text-white">Bấm để chọn file Excel từ máy tính</span>
                    <span className="text-[11px] text-slate-500 mt-1">.xlsx, .xls (có thể chọn sample_students_50.xlsx)</span>
                  </div>
                )}
              </label>
            </div>

            {/* Import Result Stats */}
            {importResult && (
              <div className="p-4 rounded-xl bg-slate-800/80 border border-slate-700 space-y-2 text-xs">
                <div className="flex items-center space-x-2 text-emerald-400 font-semibold">
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Xử lý hoàn tất!</span>
                </div>
                <div className="grid grid-cols-3 gap-2 text-center pt-2">
                  <div className="p-2 rounded-lg bg-emerald-500/10 border border-emerald-500/20">
                    <div className="text-base font-bold text-emerald-400">{importResult.successCount}</div>
                    <div className="text-[10px] text-slate-400">Đã thêm mới</div>
                  </div>
                  <div className="p-2 rounded-lg bg-amber-500/10 border border-amber-500/20">
                    <div className="text-base font-bold text-amber-400">{importResult.skippedCount}</div>
                    <div className="text-[10px] text-slate-400">Bỏ qua / Trùng</div>
                  </div>
                  <div className="p-2 rounded-lg bg-red-500/10 border border-red-500/20">
                    <div className="text-base font-bold text-red-400">{importResult.errorCount}</div>
                    <div className="text-[10px] text-slate-400">Lỗi dòng</div>
                  </div>
                </div>

                {importResult.errors && importResult.errors.length > 0 && (
                  <div className="mt-3 text-[11px] text-red-400 max-h-24 overflow-y-auto space-y-1">
                    {importResult.errors.map((err, i) => (
                      <div key={i}>• Dòng {err.row} ({err.studentCode}): {err.message}</div>
                    ))}
                  </div>
                )}
              </div>
            )}

            <div className="flex items-center justify-between pt-4 border-t border-slate-800 mt-4">
              <button
                type="button"
                onClick={handleDownloadTemplate}
                className="text-xs text-indigo-400 hover:underline flex items-center space-x-1"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Tải file mẫu .xlsx</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  setIsImportOpen(false);
                  setImportResult(null);
                }}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-white bg-slate-800 hover:bg-slate-700"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Create Session */}
      {isCreateSessionOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div className="glass-panel w-full max-w-md rounded-2xl p-6 border border-slate-700 shadow-2xl relative">
            <button
              onClick={() => setIsCreateSessionOpen(false)}
              className="absolute top-4 right-4 p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
            >
              <X className="w-5 h-5" />
            </button>

            <h3 className="text-lg font-bold text-white mb-1">Tạo Buổi Điểm Danh Mới</h3>
            <p className="text-xs text-slate-400 mb-5">
              Thiết lập buổi học cho lớp {classData.classCode} ({students.length} SV)
            </p>

            {createSessionError && (
              <div className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-xs text-red-400 flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{createSessionError}</span>
              </div>
            )}

            <form onSubmit={handleCreateSession} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Tiêu Đề Buổi Học *
                </label>
                <input
                  type="text"
                  required
                  placeholder={`Buổi ${sessions.length + 1} - Giới thiệu môn học`}
                  value={newSession.name}
                  onChange={(e) => setNewSession({ ...newSession, name: e.target.value })}
                  className="glass-input w-full px-3.5 py-2 rounded-xl text-sm text-white placeholder-slate-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Ngày Học *
                </label>
                <input
                  type="date"
                  required
                  value={newSession.attendanceDate}
                  onChange={(e) => setNewSession({ ...newSession, attendanceDate: e.target.value })}
                  className="glass-input w-full px-3.5 py-2 rounded-xl text-sm text-white placeholder-slate-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
                  Mô Tả / Nội Dung
                </label>
                <textarea
                  rows={2}
                  placeholder="Kiểm tra kiến thức tuần trước, làm bài tập nhóm..."
                  value={newSession.description}
                  onChange={(e) => setNewSession({ ...newSession, description: e.target.value })}
                  className="glass-input w-full px-3.5 py-2 rounded-xl text-sm text-white placeholder-slate-500"
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsCreateSessionOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={creatingSession}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-500 shadow-md shadow-indigo-600/30 flex items-center space-x-2"
                >
                  {creatingSession ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <span>Tạo Buổi Điểm Danh</span>}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ClassDetailPage;
