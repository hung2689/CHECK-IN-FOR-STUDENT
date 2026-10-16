import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { AttendanceSession, AttendanceRecord } from '../types';
import { QRCodeSVG } from 'qrcode.react';
import {
  QrCode,
  ArrowLeft,
  Maximize2,
  Minimize2,
  Play,
  Square,
  FileSpreadsheet,
  Users,
  CheckCircle,
  UserCheck,
  AlertTriangle,
  RotateCcw,
  Copy,
  Check,
  Loader2,
  Sparkles,
} from 'lucide-react';

export const SessionLivePage: React.FC = () => {
  const { sessionId } = useParams<{ sessionId: string }>();

  const [session, setSession] = useState<AttendanceSession | null>(null);
  const [records, setRecords] = useState<AttendanceRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [copied, setCopied] = useState(false);
  const [searchRoster, setSearchRoster] = useState('');

  const qrContainerRef = useRef<HTMLDivElement>(null);

  // Fetch session & records
  const fetchSessionData = async (silent = false) => {
    try {
      if (!silent) setLoading(true);
      const [sessionRes, recordsRes] = await Promise.all([
        api.get<AttendanceSession>(`/api/attendance-sessions/${sessionId}`),
        api.get<AttendanceRecord[]>(`/api/attendance-sessions/${sessionId}/records`),
      ]);
      setSession(sessionRes.data);
      setRecords(recordsRes.data);
    } catch (err) {
      console.error('Error fetching session data', err);
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    fetchSessionData();
  }, [sessionId]);

  // Polling every 3.5s if session is active & autoRefresh is true
  useEffect(() => {
    if (!autoRefresh) return;
    const isLive = session?.checkInStatus === 'OPEN' || session?.checkOutStatus === 'OPEN';
    if (!isLive) return;

    const interval = setInterval(() => {
      fetchSessionData(true);
    }, 3500);

    return () => clearInterval(interval);
  }, [sessionId, autoRefresh, session?.checkInStatus, session?.checkOutStatus]);

  // TA Action Handlers
  const handleOpenCheckIn = async () => {
    setActionLoading(true);
    try {
      const res = await api.post<AttendanceSession>(`/api/attendance-sessions/${sessionId}/check-in/open`);
      setSession(res.data);
      await fetchSessionData(true);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Không thể mở Check-in.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCloseCheckIn = async () => {
    if (!window.confirm('Bạn có chắc muốn chốt Check-in? Sinh viên sẽ không thể quét Check-in sau khi đóng.')) return;
    setActionLoading(true);
    try {
      const res = await api.post<AttendanceSession>(`/api/attendance-sessions/${sessionId}/check-in/close`);
      setSession(res.data);
      await fetchSessionData(true);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Không thể chốt Check-in.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleOpenCheckOut = async () => {
    setActionLoading(true);
    try {
      const res = await api.post<AttendanceSession>(`/api/attendance-sessions/${sessionId}/check-out/open`);
      setSession(res.data);
      await fetchSessionData(true);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Không thể mở Check-out.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCloseCheckOut = async () => {
    if (!window.confirm('Bạn có chắc muốn đóng Check-out và hoàn thành buổi điểm danh?')) return;
    setActionLoading(true);
    try {
      const res = await api.post<AttendanceSession>(`/api/attendance-sessions/${sessionId}/check-out/close`);
      setSession(res.data);
      await fetchSessionData(true);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Không thể chốt Check-out.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCancelSession = async () => {
    if (!window.confirm('CẢNH BÁO: Bạn có chắc muốn hủy buổi điểm danh này?')) return;
    setActionLoading(true);
    try {
      const res = await api.post<AttendanceSession>(`/api/attendance-sessions/${sessionId}/cancel`);
      setSession(res.data);
      await fetchSessionData(true);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Không thể hủy buổi điểm danh.');
    } finally {
      setActionLoading(false);
    }
  };

  // Export report Excel
  const handleExport = async () => {
    try {
      const response = await api.get(`/api/attendance-sessions/${sessionId}/export`, {
        responseType: 'blob',
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `bao_cao_diem_danh_${session?.classCode}_${session?.name}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Không thể xuất file báo cáo.');
    }
  };

  // Fullscreen toggle for QR presentation
  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      qrContainerRef.current?.requestFullscreen().catch((err) => console.error(err));
      setIsFullscreen(true);
    } else {
      document.exitFullscreen();
      setIsFullscreen(false);
    }
  };

  useEffect(() => {
    const onFsChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };
    document.addEventListener('fullscreenchange', onFsChange);
    return () => document.removeEventListener('fullscreenchange', onFsChange);
  }, []);

  const isCheckInOpen = session?.checkInStatus === 'OPEN';
  const isCheckOutOpen = session?.checkOutStatus === 'OPEN';
  const isLive = isCheckInOpen || isCheckOutOpen;
  const isCompleted = session?.status === 'COMPLETED';

  // Active token for the currently open stage
  const activeToken = isCheckInOpen ? session?.checkInToken : isCheckOutOpen ? session?.checkOutToken : '';
  const publicUrl = activeToken ? `${window.location.origin}/checkin/${activeToken}` : '';

  const handleCopyLink = () => {
    if (!publicUrl) return;
    navigator.clipboard.writeText(publicUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const filteredRecords = records.filter(
    (r) =>
      r.studentCode.toLowerCase().includes(searchRoster.toLowerCase()) ||
      r.fullName.toLowerCase().includes(searchRoster.toLowerCase())
  );

  const formatTime = (isoString?: string) => {
    if (!isoString) return '-';
    try {
      const date = new Date(isoString);
      return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } catch {
      return isoString;
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#0b0f19] flex flex-col">
        <Navbar />
        <div className="flex-1 flex flex-col items-center justify-center text-slate-400">
          <Loader2 className="w-8 h-8 animate-spin text-indigo-500 mb-3" />
          <p className="text-sm">Đang tải phòng điểm danh...</p>
        </div>
      </div>
    );
  }

  if (!session) {
    return (
      <div className="min-h-screen bg-[#0b0f19] flex flex-col">
        <Navbar />
        <div className="flex-1 flex flex-col items-center justify-center text-slate-400">
          <p className="text-red-400 mb-4">Không tìm thấy thông tin buổi điểm danh</p>
          <Link to="/classes" className="text-indigo-400 hover:underline">
            Quay lại danh sách lớp
          </Link>
        </div>
      </div>
    );
  }

  const absentCount = Math.max(0, session.totalStudents - session.checkedInCount);
  const checkInPercent = session.totalStudents > 0
    ? Math.round((session.checkedInCount / session.totalStudents) * 100)
    : 0;
  const checkOutPercent = session.totalStudents > 0
    ? Math.round((session.checkedOutCount / session.totalStudents) * 100)
    : 0;

  return (
    <div className="min-h-screen bg-[#0b0f19] text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Navigation & Title bar */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
          <div>
            <Link
              to={`/classes/${session.classId}`}
              className="inline-flex items-center space-x-1.5 text-xs text-indigo-400 hover:text-indigo-300 mb-2 transition-colors"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Quay lại lớp {session.classCode}</span>
            </Link>

            <div className="flex items-center space-x-3">
              <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
                {session.name}
              </h1>
              {isLive && (
                <span className="flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 animate-pulse">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 inline-block animate-ping" />
                  <span>TRỰC TIẾP</span>
                </span>
              )}
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Lớp: <strong className="text-slate-200">{session.className}</strong> ({session.classCode}) • Ngày:{' '}
              {session.attendanceDate}
            </p>
          </div>

          <div className="flex items-center space-x-3">
            <button
              onClick={handleExport}
              className="inline-flex items-center space-x-2 px-4 py-2.5 rounded-xl text-xs font-semibold text-emerald-300 bg-emerald-500/10 hover:bg-emerald-500/20 border border-emerald-500/30 transition-all"
            >
              <FileSpreadsheet className="w-4 h-4" />
              <span>Xuất Báo Cáo Excel</span>
            </button>
          </div>
        </div>

        {/* 4 KPI Summary Cards */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <div className="glass-card rounded-2xl p-4 border border-slate-800">
            <div className="flex items-center justify-between text-slate-400 mb-1">
              <span className="text-xs font-medium uppercase tracking-wider">Tổng Sinh Viên</span>
              <Users className="w-4 h-4 text-indigo-400" />
            </div>
            <div className="text-2xl font-black text-white">{session.totalStudents}</div>
            <div className="text-[11px] text-slate-500 mt-1">Sĩ số danh sách lớp</div>
          </div>

          <div className="glass-card rounded-2xl p-4 border border-slate-800">
            <div className="flex items-center justify-between text-slate-400 mb-1">
              <span className="text-xs font-medium uppercase tracking-wider">Đã Check-in</span>
              <CheckCircle className="w-4 h-4 text-emerald-400" />
            </div>
            <div className="text-2xl font-black text-emerald-400">
              {session.checkedInCount} <span className="text-xs font-normal text-slate-400">({checkInPercent}%)</span>
            </div>
            <div className="w-full bg-slate-800 h-1.5 rounded-full mt-2 overflow-hidden">
              <div
                className="bg-emerald-500 h-full rounded-full transition-all duration-500"
                style={{ width: `${checkInPercent}%` }}
              />
            </div>
          </div>

          <div className="glass-card rounded-2xl p-4 border border-slate-800">
            <div className="flex items-center justify-between text-slate-400 mb-1">
              <span className="text-xs font-medium uppercase tracking-wider">Đã Check-out</span>
              <UserCheck className="w-4 h-4 text-amber-400" />
            </div>
            <div className="text-2xl font-black text-amber-400">
              {session.checkedOutCount} <span className="text-xs font-normal text-slate-400">({checkOutPercent}%)</span>
            </div>
            <div className="w-full bg-slate-800 h-1.5 rounded-full mt-2 overflow-hidden">
              <div
                className="bg-amber-500 h-full rounded-full transition-all duration-500"
                style={{ width: `${checkOutPercent}%` }}
              />
            </div>
          </div>

          <div className="glass-card rounded-2xl p-4 border border-slate-800">
            <div className="flex items-center justify-between text-slate-400 mb-1">
              <span className="text-xs font-medium uppercase tracking-wider">Vắng Mặt</span>
              <AlertTriangle className="w-4 h-4 text-red-400" />
            </div>
            <div className="text-2xl font-black text-red-400">{absentCount}</div>
            <div className="text-[11px] text-slate-500 mt-1">Chưa quét mã</div>
          </div>
        </div>

        {/* Main Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* LEFT: QR Code Presenter & Controls */}
          <div className="lg:col-span-5 space-y-6">
            {/* QR Projection Card */}
            <div
              ref={qrContainerRef}
              className={`glass-panel rounded-3xl p-6 border border-slate-800 flex flex-col items-center justify-center text-center relative ${
                isFullscreen ? 'bg-slate-950 p-12 min-h-screen justify-center' : ''
              }`}
            >
              <button
                onClick={toggleFullscreen}
                className="absolute top-4 right-4 p-2 rounded-xl text-slate-400 hover:text-white glass-card hover:bg-slate-800 transition-colors"
                title={isFullscreen ? 'Thu nhỏ' : 'Toàn màn hình máy chiếu'}
              >
                {isFullscreen ? <Minimize2 className="w-5 h-5" /> : <Maximize2 className="w-5 h-5" />}
              </button>

              <div className="mb-4">
                <span className="text-xs font-bold uppercase tracking-wider text-indigo-400 block mb-1">
                  {session.className} ({session.classCode})
                </span>
                <h3 className="text-lg font-extrabold text-white">
                  {isCheckInOpen
                    ? 'QUÉT MÃ CHECK-IN ĐẦU GIỜ'
                    : isCheckOutOpen
                    ? 'QUÉT MÃ CHECK-OUT CUỐI GIỜ'
                    : isCompleted
                    ? 'BUỔI ĐIỂM DANH ĐÃ KẾT THÚC'
                    : 'CHƯA MỞ ĐIỂM DANH'}
                </h3>
              </div>

              {/* QR display */}
              {isLive && publicUrl ? (
                <div className="bg-white p-5 rounded-2xl shadow-2xl border-4 border-indigo-500/30 my-2 animate-fade-in">
                  <QRCodeSVG
                    value={publicUrl}
                    size={isFullscreen ? 360 : 230}
                    level="H"
                    includeMargin={false}
                  />
                </div>
              ) : (
                <div className="w-56 h-56 rounded-2xl border-2 border-dashed border-slate-700 flex flex-col items-center justify-center p-6 text-slate-500 my-2">
                  <QrCode className="w-12 h-12 mb-2 text-slate-600" />
                  <p className="text-xs text-center">
                    {isCompleted
                      ? 'Buổi học đã hoàn thành'
                      : 'Bấm nút "Mở Check-in" hoặc "Mở Check-out" bên dưới để phát sinh mã QR cho sinh viên'}
                  </p>
                </div>
              )}

              {isLive && (
                <div className="mt-4 text-center">
                  <p className="text-xs text-emerald-400 font-semibold flex items-center justify-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping inline-block" />
                    Sinh viên dùng camera điện thoại quét mã trên màn hình
                  </p>

                  <div className="mt-3 flex items-center justify-center gap-2">
                    <button
                      onClick={handleCopyLink}
                      className="inline-flex items-center space-x-1 px-3 py-1.5 rounded-lg text-xs font-medium text-slate-300 hover:text-white glass-card hover:bg-slate-800 transition-colors"
                    >
                      {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                      <span>{copied ? 'Đã sao chép link' : 'Sao chép link điểm danh'}</span>
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* TA Session Control Buttons */}
            <div className="glass-card rounded-2xl p-5 border border-slate-800">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-4 flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-indigo-400" />
                <span>Bảng Điều Khiển Trợ Giảng</span>
              </h4>

              <div className="space-y-3">
                {/* Check-In Controls */}
                <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
                  <div>
                    <div className="text-xs font-semibold text-white">1. Giai đoạn Check-in (Đầu giờ)</div>
                    <div className="text-[11px] text-slate-400">
                      {isCheckInOpen ? 'Đang mở phát mã' : 'Chưa mở / Đã đóng'}
                    </div>
                  </div>

                  {!isCheckInOpen ? (
                    <button
                      onClick={handleOpenCheckIn}
                      disabled={actionLoading || isCheckOutOpen}
                      className="inline-flex items-center space-x-1.5 px-3.5 py-1.5 rounded-lg text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 transition-all shadow-md shadow-emerald-600/20"
                    >
                      <Play className="w-3.5 h-3.5 fill-current" />
                      <span>Mở Check-in</span>
                    </button>
                  ) : (
                    <button
                      onClick={handleCloseCheckIn}
                      disabled={actionLoading}
                      className="inline-flex items-center space-x-1.5 px-3.5 py-1.5 rounded-lg text-xs font-bold text-white bg-amber-600 hover:bg-amber-500 disabled:opacity-50 transition-all"
                    >
                      <Square className="w-3.5 h-3.5 fill-current" />
                      <span>Chốt Check-in</span>
                    </button>
                  )}
                </div>

                {/* Check-Out Controls */}
                <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between">
                  <div>
                    <div className="text-xs font-semibold text-white">2. Giai đoạn Check-out (Cuối giờ)</div>
                    <div className="text-[11px] text-slate-400">
                      {isCheckOutOpen ? 'Đang mở phát mã' : 'Chưa mở / Đã đóng'}
                    </div>
                  </div>

                  {!isCheckOutOpen ? (
                    <button
                      onClick={handleOpenCheckOut}
                      disabled={actionLoading || isCheckInOpen || isCompleted}
                      className="inline-flex items-center space-x-1.5 px-3.5 py-1.5 rounded-lg text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 transition-all shadow-md shadow-indigo-600/20"
                    >
                      <Play className="w-3.5 h-3.5 fill-current" />
                      <span>Mở Check-out</span>
                    </button>
                  ) : (
                    <button
                      onClick={handleCloseCheckOut}
                      disabled={actionLoading}
                      className="inline-flex items-center space-x-1.5 px-3.5 py-1.5 rounded-lg text-xs font-bold text-white bg-purple-600 hover:bg-purple-500 disabled:opacity-50 transition-all"
                    >
                      <Square className="w-3.5 h-3.5 fill-current" />
                      <span>Chốt Check-out</span>
                    </button>
                  )}
                </div>

                {/* Cancel option */}
                {!isCompleted && session.status !== 'CANCELLED' && (
                  <div className="pt-2 text-right">
                    <button
                      onClick={handleCancelSession}
                      disabled={actionLoading}
                      className="text-[11px] text-red-400 hover:text-red-300 hover:underline"
                    >
                      Hủy buổi điểm danh này
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* RIGHT: Live Roster & Real-time Submissions */}
          <div className="lg:col-span-7">
            <div className="glass-panel rounded-3xl border border-slate-800 overflow-hidden flex flex-col h-full min-h-[560px]">
              {/* Header */}
              <div className="p-5 border-b border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <h3 className="text-base font-bold text-white flex items-center space-x-2">
                    <Users className="w-4 h-4 text-indigo-400" />
                    <span>Danh Sách Điểm Danh Chi Tiết ({records.length} SV)</span>
                  </h3>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Tự động cập nhật theo thời gian thực khi sinh viên gửi điểm danh
                  </p>
                </div>

                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => setAutoRefresh(!autoRefresh)}
                    className={`inline-flex items-center space-x-1 px-2.5 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                      autoRefresh
                        ? 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20'
                        : 'bg-slate-800 text-slate-400'
                    }`}
                    title="Tự động cập nhật sau mỗi 3.5s"
                  >
                    <RotateCcw className={`w-3.5 h-3.5 ${autoRefresh ? 'animate-spin' : ''}`} />
                    <span>{autoRefresh ? 'Live ON' : 'Live OFF'}</span>
                  </button>
                </div>
              </div>

              {/* Search input in roster */}
              <div className="px-5 py-3 border-b border-slate-800/80 bg-slate-900/40">
                <input
                  type="text"
                  placeholder="Lọc sinh viên theo tên hoặc MSSV..."
                  value={searchRoster}
                  onChange={(e) => setSearchRoster(e.target.value)}
                  className="glass-input w-full px-3 py-1.5 rounded-xl text-xs text-white placeholder-slate-500"
                />
              </div>

              {/* Roster Table */}
              <div className="flex-1 overflow-x-auto overflow-y-auto max-h-[600px]">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-900/80 sticky top-0 z-10 text-slate-400 uppercase tracking-wider text-[11px] border-b border-slate-800">
                    <tr>
                      <th className="py-3 px-4 font-semibold w-12 text-center">STT</th>
                      <th className="py-3 px-4 font-semibold">MSSV</th>
                      <th className="py-3 px-4 font-semibold">Họ và Tên</th>
                      <th className="py-3 px-4 font-semibold">Check-in</th>
                      <th className="py-3 px-4 font-semibold">Check-out</th>
                      <th className="py-3 px-4 font-semibold text-center">Trạng Thái</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/40">
                    {filteredRecords.map((r, index) => {
                      const hasCheckIn = !!r.checkInAt;
                      const hasCheckOut = !!r.checkOutAt;

                      return (
                        <tr
                          key={r.studentId}
                          className={`hover:bg-slate-800/40 transition-colors ${
                            hasCheckIn ? 'bg-emerald-500/[0.03]' : ''
                          }`}
                        >
                          <td className="py-2.5 px-4 text-center text-slate-500 font-mono">{index + 1}</td>
                          <td className="py-2.5 px-4 font-mono font-bold text-indigo-400">{r.studentCode}</td>
                          <td className="py-2.5 px-4 font-medium text-white">{r.fullName}</td>
                          <td className="py-2.5 px-4 font-mono text-[11px]">
                            {hasCheckIn ? (
                              <span className="text-emerald-400 font-semibold">{formatTime(r.checkInAt)}</span>
                            ) : (
                              <span className="text-slate-600">-</span>
                            )}
                          </td>
                          <td className="py-2.5 px-4 font-mono text-[11px]">
                            {hasCheckOut ? (
                              <span className="text-amber-400 font-semibold">{formatTime(r.checkOutAt)}</span>
                            ) : (
                              <span className="text-slate-600">-</span>
                            )}
                          </td>
                          <td className="py-2.5 px-4 text-center">
                            {hasCheckIn && hasCheckOut ? (
                              <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30">
                                Đủ 2 lượt
                              </span>
                            ) : hasCheckIn ? (
                              <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                                Check-in
                              </span>
                            ) : hasCheckOut ? (
                              <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-500/20 text-blue-300 border border-blue-500/30">
                                Check-out
                              </span>
                            ) : (
                              <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-medium bg-red-500/10 text-red-400 border border-red-500/20">
                                Vắng
                              </span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default SessionLivePage;
