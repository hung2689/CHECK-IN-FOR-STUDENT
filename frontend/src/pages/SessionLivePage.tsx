import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import api from '../services/api';
import { AttendanceSession, AttendanceRecord, AttendanceReviewAttempt, ReviewStats } from '../types';
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
  MapPin,
  ShieldAlert,
  Clock,
  XCircle,
  CheckCircle2,
  Info,
  Compass,
  Filter,
} from 'lucide-react';

export const SessionLivePage: React.FC = () => {
  const { sessionId } = useParams<{ sessionId: string }>();

  const [session, setSession] = useState<AttendanceSession | null>(null);
  const [records, setRecords] = useState<AttendanceRecord[]>([]);
  const [reviewAttempts, setReviewAttempts] = useState<AttendanceReviewAttempt[]>([]);
  const [reviewStats, setReviewStats] = useState<ReviewStats | null>(null);

  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [copied, setCopied] = useState(false);
  const [searchRoster, setSearchRoster] = useState('');

  // Active right-side tab: 'roster' | 'review'
  const [activeTab, setActiveTab] = useState<'roster' | 'review'>('roster');

  // Review queue filters
  const [reviewStatusFilter, setReviewStatusFilter] = useState<string>('ALL');
  const [reviewStageFilter, setReviewStageFilter] = useState<string>('ALL');

  // Geofence configuration state
  const [geofenceLat, setGeofenceLat] = useState<string>('');
  const [geofenceLng, setGeofenceLng] = useState<string>('');
  const [geofenceRadius, setGeofenceRadius] = useState<number>(150);
  const [gpsEnforcement, setGpsEnforcement] = useState<boolean>(false);
  const [savingGeofence, setSavingGeofence] = useState<boolean>(false);
  const [geofenceMessage, setGeofenceMessage] = useState<string | null>(null);

  // Modals state
  const [rejectModalAttempt, setRejectModalAttempt] = useState<AttendanceReviewAttempt | null>(null);
  const [rejectionReason, setRejectionReason] = useState<string>('');
  const [detailModalAttempt, setDetailModalAttempt] = useState<AttendanceReviewAttempt | null>(null);

  const qrContainerRef = useRef<HTMLDivElement>(null);

  // Fetch session, records, review attempts, and review stats
  const fetchSessionData = async (silent = false) => {
    try {
      if (!silent) setLoading(true);
      const [sessionRes, recordsRes, reviewRes, statsRes] = await Promise.all([
        api.get<AttendanceSession>(`/api/attendance-sessions/${sessionId}`),
        api.get<AttendanceRecord[]>(`/api/attendance-sessions/${sessionId}/records`),
        api.get<AttendanceReviewAttempt[]>(`/api/attendance-sessions/${sessionId}/review-attempts`),
        api.get<ReviewStats>(`/api/attendance-sessions/${sessionId}/review-stats`),
      ]);

      setSession(sessionRes.data);
      setRecords(recordsRes.data);
      setReviewAttempts(reviewRes.data);
      setReviewStats(statsRes.data);

      if (!silent) {
        setGeofenceLat(sessionRes.data.geofenceLat != null ? sessionRes.data.geofenceLat.toString() : '');
        setGeofenceLng(sessionRes.data.geofenceLng != null ? sessionRes.data.geofenceLng.toString() : '');
        setGeofenceRadius(sessionRes.data.geofenceRadiusM || 150);
        setGpsEnforcement(!!sessionRes.data.gpsEnforcement);
      }
    } catch (err) {
      console.error('Error fetching session data', err);
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    fetchSessionData();
  }, [sessionId]);

  // Polling every 3.5s if session is live & autoRefresh is true
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

  // Geofence save
  const handleSaveGeofence = async () => {
    setSavingGeofence(true);
    setGeofenceMessage(null);
    try {
      const lat = geofenceLat.trim() ? parseFloat(geofenceLat.trim()) : null;
      const lng = geofenceLng.trim() ? parseFloat(geofenceLng.trim()) : null;

      const res = await api.put<AttendanceSession>(`/api/attendance-sessions/${sessionId}/geofence`, {
        geofenceLat: lat,
        geofenceLng: lng,
        geofenceRadiusM: geofenceRadius,
        gpsEnforcement: gpsEnforcement,
      });
      setSession(res.data);
      setGeofenceMessage('Đã lưu cấu hình GPS Geofence thành công!');
      setTimeout(() => setGeofenceMessage(null), 3000);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Không thể lưu cấu hình Geofence.');
    } finally {
      setSavingGeofence(false);
    }
  };

  // Use current browser location for Geofence center
  const handleUseCurrentLocation = () => {
    if (!navigator.geolocation) {
      alert('Trình duyệt không hỗ trợ Geolocation.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setGeofenceLat(pos.coords.latitude.toFixed(6));
        setGeofenceLng(pos.coords.longitude.toFixed(6));
      },
      (err) => {
        alert('Không thể lấy tọa độ hiện tại: ' + err.message);
      },
      { enableHighAccuracy: true }
    );
  };

  // Review queue actions: Approve
  const handleApproveAttempt = async (attempt: AttendanceReviewAttempt) => {
    if (!window.confirm(`Xác nhận DUYỆT điểm danh cho sinh viên ${attempt.studentName} (${attempt.studentCode})?`)) return;
    try {
      await api.post(`/api/attendance-review-attempts/${attempt.id}/approve`, {
        note: 'Đã duyệt bởi giảng viên/trợ giảng',
      });
      await fetchSessionData(true);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Không thể duyệt yêu cầu này.');
    }
  };

  // Review queue actions: Reject confirm
  const handleConfirmReject = async () => {
    if (!rejectModalAttempt) return;
    if (!rejectionReason.trim()) {
      alert('Vui lòng nhập lý do từ chối.');
      return;
    }
    try {
      await api.post(`/api/attendance-review-attempts/${rejectModalAttempt.id}/reject`, {
        reason: rejectionReason.trim(),
      });
      setRejectModalAttempt(null);
      setRejectionReason('');
      await fetchSessionData(true);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Không thể từ chối yêu cầu này.');
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

  const filteredReviewAttempts = reviewAttempts.filter((a) => {
    if (reviewStatusFilter !== 'ALL' && a.reviewStatus !== reviewStatusFilter) return false;
    if (reviewStageFilter !== 'ALL' && a.stage !== reviewStageFilter) return false;
    if (searchRoster.trim()) {
      const q = searchRoster.toLowerCase();
      if (!a.studentCode.toLowerCase().includes(q) && !a.studentName.toLowerCase().includes(q)) return false;
    }
    return true;
  });

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

  const pendingCount = reviewStats?.pendingReviews ?? 0;
  const rejectedCount = reviewStats?.rejectedAttempts ?? 0;
  const suspiciousCount = reviewStats?.suspiciousAttempts ?? 0;
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
              className="inline-flex items-center space-x-2 px-4 py-2.5 rounded-xl text-xs font-semibold text-emerald-300 bg-emerald-500/10 hover:bg-emerald-500/20 border border-emerald-500/30 transition-all shadow-sm"
            >
              <FileSpreadsheet className="w-4 h-4" />
              <span>Xuất Báo Cáo Excel</span>
            </button>
          </div>
        </div>

        {/* 6 KPI Summary Cards */}
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3.5 mb-8">
          {/* 1. Tổng sinh viên */}
          <div className="glass-card rounded-2xl p-3.5 border border-slate-800">
            <div className="flex items-center justify-between text-slate-400 mb-1">
              <span className="text-[11px] font-medium uppercase tracking-wider">Sĩ Số Lớp</span>
              <Users className="w-3.5 h-3.5 text-indigo-400" />
            </div>
            <div className="text-2xl font-black text-white">{session.totalStudents}</div>
            <div className="text-[10px] text-slate-500 mt-1">Tổng danh sách</div>
          </div>

          {/* 2. Đã Check-in */}
          <div className="glass-card rounded-2xl p-3.5 border border-slate-800">
            <div className="flex items-center justify-between text-slate-400 mb-1">
              <span className="text-[11px] font-medium uppercase tracking-wider">Đã Check-in</span>
              <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
            </div>
            <div className="text-2xl font-black text-emerald-400">
              {session.checkedInCount} <span className="text-[11px] font-normal text-slate-400">({checkInPercent}%)</span>
            </div>
            <div className="w-full bg-slate-800 h-1 rounded-full mt-2 overflow-hidden">
              <div className="bg-emerald-500 h-full rounded-full" style={{ width: `${checkInPercent}%` }} />
            </div>
          </div>

          {/* 3. Đã Check-out */}
          <div className="glass-card rounded-2xl p-3.5 border border-slate-800">
            <div className="flex items-center justify-between text-slate-400 mb-1">
              <span className="text-[11px] font-medium uppercase tracking-wider">Đã Check-out</span>
              <UserCheck className="w-3.5 h-3.5 text-blue-400" />
            </div>
            <div className="text-2xl font-black text-blue-400">
              {session.checkedOutCount} <span className="text-[11px] font-normal text-slate-400">({checkOutPercent}%)</span>
            </div>
            <div className="w-full bg-slate-800 h-1 rounded-full mt-2 overflow-hidden">
              <div className="bg-blue-500 h-full rounded-full" style={{ width: `${checkOutPercent}%` }} />
            </div>
          </div>

          {/* 4. Chờ Phê Duyệt (Warning pulse indicator) */}
          <div
            onClick={() => setActiveTab('review')}
            className={`glass-card rounded-2xl p-3.5 border cursor-pointer transition-all ${
              pendingCount > 0
                ? 'border-amber-500/50 bg-amber-500/10 shadow-lg shadow-amber-500/10 hover:border-amber-400'
                : 'border-slate-800'
            }`}
          >
            <div className="flex items-center justify-between text-slate-400 mb-1">
              <span className={`text-[11px] font-bold uppercase tracking-wider ${pendingCount > 0 ? 'text-amber-400' : ''}`}>
                Chờ Phê Duyệt
              </span>
              <Clock className={`w-3.5 h-3.5 ${pendingCount > 0 ? 'text-amber-400 animate-spin' : 'text-slate-500'}`} />
            </div>
            <div className={`text-2xl font-black ${pendingCount > 0 ? 'text-amber-400' : 'text-slate-300'}`}>
              {pendingCount}
            </div>
            <div className="text-[10px] text-amber-400/80 mt-1 font-medium">
              {pendingCount > 0 ? 'Cần giảng viên duyệt' : 'Không có yêu cầu'}
            </div>
          </div>

          {/* 5. Đã từ chối */}
          <div className="glass-card rounded-2xl p-3.5 border border-slate-800">
            <div className="flex items-center justify-between text-slate-400 mb-1">
              <span className="text-[11px] font-medium uppercase tracking-wider">Đã Từ Chối</span>
              <XCircle className="w-3.5 h-3.5 text-red-400" />
            </div>
            <div className="text-2xl font-black text-red-400">{rejectedCount}</div>
            <div className="text-[10px] text-slate-500 mt-1">Lượt không hợp lệ</div>
          </div>

          {/* 6. Nghi vấn ngoài vùng */}
          <div className="glass-card rounded-2xl p-3.5 border border-slate-800">
            <div className="flex items-center justify-between text-slate-400 mb-1">
              <span className="text-[11px] font-medium uppercase tracking-wider">Ngoài Vùng GPS</span>
              <ShieldAlert className="w-3.5 h-3.5 text-purple-400" />
            </div>
            <div className="text-2xl font-black text-purple-400">{suspiciousCount}</div>
            <div className="text-[10px] text-slate-500 mt-1">Ngoài bán kính lớp</div>
          </div>
        </div>

        {/* Main Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* LEFT: QR Code Presenter & Geofence Settings */}
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

            {/* Geofence Configuration Card */}
            <div className="glass-card rounded-2xl p-5 border border-slate-800">
              <div className="flex items-center justify-between mb-3">
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300 flex items-center gap-2">
                  <MapPin className="w-4 h-4 text-emerald-400" />
                  <span>Cấu hình GPS Geofence Lớp Học</span>
                </h4>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input
                    type="checkbox"
                    checked={gpsEnforcement}
                    onChange={(e) => setGpsEnforcement(e.target.checked)}
                    className="sr-only peer"
                  />
                  <div className="w-9 h-5 bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-emerald-600"></div>
                </label>
              </div>

              <p className="text-[11px] text-slate-400 mb-4">
                Bật định vị để kiểm tra sinh viên có mặt tại lớp. Các lượt định vị không chắc chắn sẽ được chuyển vào hàng đợi phê duyệt.
              </p>

              <div className="space-y-3">
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-[10px] text-slate-400 uppercase font-semibold mb-1">Vĩ độ (Lat)</label>
                    <input
                      type="text"
                      placeholder="vd: 10.776889"
                      value={geofenceLat}
                      onChange={(e) => setGeofenceLat(e.target.value)}
                      className="glass-input w-full px-2.5 py-1.5 rounded-lg text-xs font-mono text-white placeholder-slate-600"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] text-slate-400 uppercase font-semibold mb-1">Kinh độ (Lng)</label>
                    <input
                      type="text"
                      placeholder="vd: 106.700806"
                      value={geofenceLng}
                      onChange={(e) => setGeofenceLng(e.target.value)}
                      className="glass-input w-full px-2.5 py-1.5 rounded-lg text-xs font-mono text-white placeholder-slate-600"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-[10px] text-slate-400 uppercase font-semibold mb-1">
                    Bán kính cho phép (mét): <strong className="text-white">{geofenceRadius}m</strong>
                  </label>
                  <input
                    type="range"
                    min="30"
                    max="500"
                    step="10"
                    value={geofenceRadius}
                    onChange={(e) => setGeofenceRadius(parseInt(e.target.value))}
                    className="w-full h-1.5 bg-slate-700 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                  />
                </div>

                <div className="flex items-center gap-2 pt-1">
                  <button
                    type="button"
                    onClick={handleUseCurrentLocation}
                    className="inline-flex items-center space-x-1 px-2.5 py-1.5 rounded-lg text-[11px] font-medium text-indigo-300 bg-indigo-500/10 hover:bg-indigo-500/20 border border-indigo-500/20 transition-colors"
                  >
                    <Compass className="w-3.5 h-3.5" />
                    <span>Lấy vị trí của tôi</span>
                  </button>

                  <button
                    type="button"
                    onClick={handleSaveGeofence}
                    disabled={savingGeofence}
                    className="inline-flex items-center space-x-1 px-3 py-1.5 rounded-lg text-[11px] font-bold text-white bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 transition-colors ml-auto"
                  >
                    {savingGeofence ? <Loader2 className="w-3 h-3 animate-spin" /> : null}
                    <span>Lưu cấu hình GPS</span>
                  </button>
                </div>

                {geofenceMessage && (
                  <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400 text-[11px] font-medium text-center">
                    {geofenceMessage}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* RIGHT: Tabs: Bảng Điểm Danh (Roster) & Hàng Đợi Duyệt (Review Queue) */}
          <div className="lg:col-span-7">
            <div className="glass-panel rounded-3xl border border-slate-800 overflow-hidden flex flex-col h-full min-h-[580px]">
              {/* Tab Selector & Controls Header */}
              <div className="p-4 border-b border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-900/60">
                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => setActiveTab('roster')}
                    className={`px-3.5 py-2 rounded-xl text-xs font-bold transition-all flex items-center space-x-2 ${
                      activeTab === 'roster'
                        ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                        : 'text-slate-400 hover:text-white glass-card'
                    }`}
                  >
                    <Users className="w-4 h-4" />
                    <span>Danh Sách Lớp ({records.length})</span>
                  </button>

                  <button
                    onClick={() => setActiveTab('review')}
                    className={`px-3.5 py-2 rounded-xl text-xs font-bold transition-all flex items-center space-x-2 relative ${
                      activeTab === 'review'
                        ? 'bg-amber-600 text-white shadow-md shadow-amber-600/30'
                        : 'text-slate-400 hover:text-white glass-card'
                    }`}
                  >
                    <Clock className="w-4 h-4" />
                    <span>Hàng Đợi Duyệt GPS</span>
                    {pendingCount > 0 && (
                      <span className="px-1.5 py-0.2 rounded-full text-[10px] font-extrabold bg-red-500 text-white animate-pulse">
                        {pendingCount}
                      </span>
                    )}
                  </button>
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

              {/* Sub-bar: Search & Filter */}
              <div className="px-4 py-3 border-b border-slate-800/80 bg-slate-900/40 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <input
                  type="text"
                  placeholder="Lọc sinh viên theo tên hoặc MSSV..."
                  value={searchRoster}
                  onChange={(e) => setSearchRoster(e.target.value)}
                  className="glass-input flex-1 px-3 py-1.5 rounded-xl text-xs text-white placeholder-slate-500"
                />

                {activeTab === 'review' && (
                  <div className="flex items-center space-x-2">
                    <select
                      value={reviewStatusFilter}
                      onChange={(e) => setReviewStatusFilter(e.target.value)}
                      className="glass-input px-2.5 py-1.5 rounded-xl text-xs text-slate-200 bg-slate-900"
                    >
                      <option value="ALL">Tất cả trạng thái</option>
                      <option value="PENDING_REVIEW">Chờ duyệt (PENDING)</option>
                      <option value="APPROVED">Đã duyệt (APPROVED)</option>
                      <option value="REJECTED">Đã từ chối (REJECTED)</option>
                    </select>

                    <select
                      value={reviewStageFilter}
                      onChange={(e) => setReviewStageFilter(e.target.value)}
                      className="glass-input px-2.5 py-1.5 rounded-xl text-xs text-slate-200 bg-slate-900"
                    >
                      <option value="ALL">Tất cả giai đoạn</option>
                      <option value="CHECK_IN">Check-in</option>
                      <option value="CHECK_OUT">Check-out</option>
                    </select>
                  </div>
                )}
              </div>

              {/* TAB 1: Roster Table */}
              {activeTab === 'roster' && (
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
                        const status = r.attendanceStatus;

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
                              {status === 'PRESENT' ? (
                                <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                                  Đủ 2 lượt
                                </span>
                              ) : status === 'PENDING_REVIEW' ? (
                                <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30 animate-pulse">
                                  Chờ duyệt
                                </span>
                              ) : status === 'REJECTED' ? (
                                <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-red-500/20 text-red-400 border border-red-500/30">
                                  Bị từ chối
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
              )}

              {/* TAB 2: Review Queue Table */}
              {activeTab === 'review' && (
                <div className="flex-1 overflow-x-auto overflow-y-auto max-h-[600px]">
                  {filteredReviewAttempts.length === 0 ? (
                    <div className="flex flex-col items-center justify-center p-12 text-slate-400">
                      <CheckCircle2 className="w-10 h-10 text-emerald-400 mb-2" />
                      <p className="text-sm font-medium text-slate-300">Không có yêu cầu điểm danh nào cần duyệt</p>
                      <p className="text-xs text-slate-500 mt-1">Các lượt quét vị trí không chắc chắn sẽ xuất hiện tại đây.</p>
                    </div>
                  ) : (
                    <table className="w-full text-left text-xs">
                      <thead className="bg-slate-900/80 sticky top-0 z-10 text-slate-400 uppercase tracking-wider text-[11px] border-b border-slate-800">
                        <tr>
                          <th className="py-3 px-3 font-semibold">Sinh Viên</th>
                          <th className="py-3 px-3 font-semibold">Giai đoạn</th>
                          <th className="py-3 px-3 font-semibold">Khoảng Cách</th>
                          <th className="py-3 px-3 font-semibold">Kết Quả GPS</th>
                          <th className="py-3 px-3 font-semibold text-center">Trạng Thái</th>
                          <th className="py-3 px-3 font-semibold text-center">Thao Tác</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-800/40">
                        {filteredReviewAttempts.map((a) => {
                          const isPending = a.reviewStatus === 'PENDING_REVIEW';
                          const isApproved = a.reviewStatus === 'APPROVED';
                          const isRejected = a.reviewStatus === 'REJECTED';

                          return (
                            <tr
                              key={a.id}
                              className={`hover:bg-slate-800/40 transition-colors ${
                                isPending ? 'bg-amber-500/[0.04]' : ''
                              }`}
                            >
                              <td className="py-2.5 px-3">
                                <div className="font-bold text-white">{a.studentName}</div>
                                <div className="text-[11px] font-mono text-indigo-400">{a.studentCode}</div>
                                <div className="text-[10px] text-slate-500 mt-0.5">{formatTime(a.submittedAt)}</div>
                              </td>

                              <td className="py-2.5 px-3">
                                <span className={`inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold ${
                                  a.stage === 'CHECK_IN' ? 'bg-indigo-500/20 text-indigo-300' : 'bg-purple-500/20 text-purple-300'
                                }`}>
                                  {a.stage}
                                </span>
                              </td>

                              <td className="py-2.5 px-3 font-mono text-[11px]">
                                {a.distanceFromSchool != null ? (
                                  <div>
                                    <span className="font-semibold text-slate-200">
                                      {Math.round(a.distanceFromSchool)}m
                                    </span>
                                    {a.gpsAccuracyM != null && (
                                      <span className="text-[10px] text-slate-400 block">
                                        ±{Math.round(a.gpsAccuracyM)}m
                                      </span>
                                    )}
                                  </div>
                                ) : (
                                  <span className="text-slate-500">Không có vị trí</span>
                                )}
                              </td>

                              <td className="py-2.5 px-3">
                                {a.gpsResult === 'BOUNDARY' ? (
                                  <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30">
                                    Gần ranh giới
                                  </span>
                                ) : a.gpsResult === 'POOR_ACCURACY' ? (
                                  <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-orange-500/20 text-orange-300 border border-orange-500/30">
                                    Sai số lớn
                                  </span>
                                ) : a.gpsResult === 'OUTSIDE_GEOFENCE' ? (
                                  <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-red-500/20 text-red-300 border border-red-500/30">
                                    Ngoài vùng
                                  </span>
                                ) : a.gpsResult === 'NO_LOCATION' ? (
                                  <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-700 text-slate-300">
                                    Không vị trí
                                  </span>
                                ) : (
                                  <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-300">
                                    {a.gpsResult || 'Hợp lệ'}
                                  </span>
                                )}
                              </td>

                              <td className="py-2.5 px-3 text-center">
                                {isPending ? (
                                  <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-amber-500/20 text-amber-300 border border-amber-500/30 animate-pulse">
                                    CHỜ DUYỆT
                                  </span>
                                ) : isApproved ? (
                                  <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                                    ĐÃ DUYỆT
                                  </span>
                                ) : isRejected ? (
                                  <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-red-500/20 text-red-400 border border-red-500/30">
                                    TỪ CHỐI
                                  </span>
                                ) : (
                                  <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-medium bg-slate-800 text-slate-400">
                                    {a.reviewStatus}
                                  </span>
                                )}
                              </td>

                              <td className="py-2.5 px-3 text-center">
                                <div className="flex items-center justify-center space-x-1">
                                  {isPending && (
                                    <>
                                      <button
                                        onClick={() => handleApproveAttempt(a)}
                                        className="px-2.5 py-1 rounded-lg text-[10px] font-bold text-white bg-emerald-600 hover:bg-emerald-500 transition-colors shadow-sm"
                                        title="Chấp nhận điểm danh"
                                      >
                                        Duyệt
                                      </button>
                                      <button
                                        onClick={() => setRejectModalAttempt(a)}
                                        className="px-2.5 py-1 rounded-lg text-[10px] font-bold text-white bg-red-600 hover:bg-red-500 transition-colors shadow-sm"
                                        title="Từ chối điểm danh"
                                      >
                                        Từ chối
                                      </button>
                                    </>
                                  )}
                                  <button
                                    onClick={() => setDetailModalAttempt(a)}
                                    className="p-1 rounded-lg text-slate-400 hover:text-white glass-card transition-colors"
                                    title="Xem chi tiết tọa độ và lý do"
                                  >
                                    <Info className="w-3.5 h-3.5" />
                                  </button>
                                </div>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </main>

      {/* REJECTION MODAL */}
      {rejectModalAttempt && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="w-full max-w-md glass-panel rounded-3xl p-6 border border-red-500/30 shadow-2xl">
            <div className="flex items-center space-x-3 mb-4 text-red-400">
              <AlertTriangle className="w-6 h-6 flex-shrink-0" />
              <h3 className="text-base font-bold text-white">Từ Chối Yêu Cầu Điểm Danh</h3>
            </div>

            <p className="text-xs text-slate-300 mb-4 leading-relaxed">
              Bạn đang từ chối điểm danh cho sinh viên{' '}
              <strong className="text-white">{rejectModalAttempt.studentName}</strong> ({rejectModalAttempt.studentCode}).
              Lượt điểm danh này sẽ không được tính.
            </p>

            <div className="mb-4">
              <label className="block text-xs font-semibold text-slate-300 mb-1.5 uppercase">
                Lý do từ chối * (Bắt buộc)
              </label>
              <textarea
                rows={3}
                required
                placeholder="Ví dụ: Vị trí ở cách xa lớp học quá 500m, không chứng minh được có mặt tại giảng đường..."
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                className="glass-input w-full p-3 rounded-xl text-xs text-white placeholder-slate-500 focus:border-red-500"
              />
            </div>

            <div className="flex items-center justify-end space-x-3">
              <button
                type="button"
                onClick={() => {
                  setRejectModalAttempt(null);
                  setRejectionReason('');
                }}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-300 hover:text-white glass-card"
              >
                Hủy
              </button>

              <button
                type="button"
                onClick={handleConfirmReject}
                disabled={!rejectionReason.trim()}
                className="px-4 py-2 rounded-xl text-xs font-bold text-white bg-red-600 hover:bg-red-500 disabled:opacity-50 transition-all shadow-lg shadow-red-600/30"
              >
                Xác Nhận Từ Chối
              </button>
            </div>
          </div>
        </div>
      )}

      {/* DETAIL MODAL */}
      {detailModalAttempt && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="w-full max-w-lg glass-panel rounded-3xl p-6 border border-slate-700 shadow-2xl">
            <div className="flex items-center justify-between mb-4 border-b border-slate-800 pb-3">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <MapPin className="w-4 h-4 text-indigo-400" />
                <span>Chi Tiết Xác Thực Vị Trí Điểm Danh</span>
              </h3>
              <button
                onClick={() => setDetailModalAttempt(null)}
                className="text-slate-400 hover:text-white p-1"
              >
                ✕
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3 p-3 rounded-2xl bg-slate-900/80 border border-slate-800">
                <div>
                  <span className="text-slate-400 block text-[11px]">Sinh viên:</span>
                  <span className="font-bold text-white">{detailModalAttempt.studentName}</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[11px]">MSSV:</span>
                  <span className="font-mono text-indigo-400 font-bold">{detailModalAttempt.studentCode}</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[11px]">Giai đoạn:</span>
                  <span className="font-semibold text-slate-200">{detailModalAttempt.stage}</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[11px]">Thời gian gửi:</span>
                  <span className="font-mono text-slate-200">{formatTime(detailModalAttempt.submittedAt)}</span>
                </div>
              </div>

              <div className="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 space-y-2">
                <h4 className="font-bold text-slate-300 uppercase text-[11px] tracking-wider">Thông số GPS:</h4>
                <div className="flex justify-between text-slate-400">
                  <span>Tọa độ sinh viên:</span>
                  <span className="font-mono text-slate-200">
                    {detailModalAttempt.latitude != null && detailModalAttempt.longitude != null
                      ? `${detailModalAttempt.latitude.toFixed(6)}, ${detailModalAttempt.longitude.toFixed(6)}`
                      : 'Không có'}
                  </span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Sai số GPS (Accuracy):</span>
                  <span className="font-mono text-slate-200">
                    {detailModalAttempt.gpsAccuracyM != null ? `±${Math.round(detailModalAttempt.gpsAccuracyM)} mét` : 'N/A'}
                  </span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Khoảng cách tới lớp:</span>
                  <span className="font-mono font-bold text-amber-400">
                    {detailModalAttempt.distanceFromSchool != null ? `${Math.round(detailModalAttempt.distanceFromSchool)} mét` : 'N/A'}
                  </span>
                </div>
                <div className="flex justify-between text-slate-400">
                  <span>Kết luận GPS:</span>
                  <span className="font-bold text-indigo-300">{detailModalAttempt.gpsResult}</span>
                </div>
                <div className="pt-1 text-slate-300 text-[11px] leading-relaxed">
                  <strong>Lý do hệ thống:</strong> {detailModalAttempt.gpsReason || 'Không có mô tả'}
                </div>
              </div>

              {detailModalAttempt.adminDecision && (
                <div className="p-3 rounded-2xl bg-slate-900/80 border border-slate-800 text-[11px]">
                  <div className="font-bold text-slate-300 mb-1">
                    Quyết định giảng viên: <span className="text-white">{detailModalAttempt.adminDecision}</span>
                  </div>
                  {detailModalAttempt.reviewerUsername && (
                    <div className="text-slate-400">Duyệt bởi: {detailModalAttempt.reviewerUsername}</div>
                  )}
                  {detailModalAttempt.adminNote && (
                    <div className="text-slate-300 mt-1">Ghi chú: {detailModalAttempt.adminNote}</div>
                  )}
                </div>
              )}
            </div>

            <div className="mt-5 flex justify-end">
              <button
                type="button"
                onClick={() => setDetailModalAttempt(null)}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-300 hover:text-white glass-card"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SessionLivePage;
