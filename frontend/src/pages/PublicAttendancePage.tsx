import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import api from '../services/api';
import { PublicTokenInfo, PublicAttendanceResponse } from '../types';
import {
  QrCode,
  CheckCircle2,
  AlertCircle,
  Clock,
  Loader2,
  User,
  ShieldCheck,
  Send,
  MapPin,
  Compass,
} from 'lucide-react';

export const PublicAttendancePage: React.FC = () => {
  const { token } = useParams<{ token: string }>();

  const [tokenInfo, setTokenInfo] = useState<PublicTokenInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [studentCode, setStudentCode] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<PublicAttendanceResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // GPS state
  const [coords, setCoords] = useState<{ latitude: number; longitude: number; accuracy: number } | null>(null);
  const [gpsStatus, setGpsStatus] = useState<'acquiring' | 'ready' | 'denied' | 'unavailable'>('acquiring');
  const [gpsReason, setGpsReason] = useState<string | null>(null);

  // Load saved studentCode from localStorage
  useEffect(() => {
    const saved = localStorage.getItem('qr_student_code');
    if (saved) setStudentCode(saved);
  }, []);

  // Request GPS position early
  const requestGps = () => {
    if (!navigator.geolocation) {
      setGpsStatus('unavailable');
      setGpsReason('LOCATION_UNAVAILABLE');
      return;
    }

    setGpsStatus('acquiring');
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setCoords({
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
          accuracy: pos.coords.accuracy,
        });
        setGpsStatus('ready');
        setGpsReason(null);
      },
      (err) => {
        if (err.code === err.PERMISSION_DENIED) {
          setGpsStatus('denied');
          setGpsReason('LOCATION_PERMISSION_DENIED');
        } else {
          setGpsStatus('unavailable');
          setGpsReason('LOCATION_UNAVAILABLE');
        }
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 15000,
      }
    );
  };

  useEffect(() => {
    requestGps();
  }, []);

  // Validate token
  useEffect(() => {
    const validateToken = async () => {
      try {
        setLoading(true);
        const res = await api.get<PublicTokenInfo>(`/api/public/attendance/${token}`);
        setTokenInfo(res.data);
      } catch (err: any) {
        setTokenInfo({
          valid: false,
          message: err.response?.data?.message || 'Mã QR không hợp lệ hoặc buổi điểm danh đã đóng.',
        });
      } finally {
        setLoading(false);
      }
    };

    if (token) validateToken();
  }, [token]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!studentCode.trim()) return;

    setSubmitting(true);
    setErrorMessage(null);
    setResult(null);

    const cleanCode = studentCode.trim().toUpperCase();

    // Prepare payload
    const payload = {
      studentCode: cleanCode,
      latitude: coords?.latitude || null,
      longitude: coords?.longitude || null,
      gpsAccuracyM: coords?.accuracy || null,
      locationUnavailableReason: coords ? null : (gpsReason || 'LOCATION_UNAVAILABLE'),
    };

    try {
      const res = await api.post<PublicAttendanceResponse>(`/api/public/attendance/${token}`, payload);
      setResult(res.data);
      localStorage.setItem('qr_student_code', cleanCode);
    } catch (err: any) {
      setErrorMessage(
        err.response?.data?.message || 'Không thể điểm danh. Vui lòng kiểm tra lại MSSV hoặc hỏi Trợ giảng.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#0a0e17] flex flex-col items-center justify-center p-4 text-slate-300">
        <Loader2 className="w-10 h-10 animate-spin text-indigo-500 mb-4" />
        <p className="text-sm font-medium">Đang xác thực mã điểm danh...</p>
      </div>
    );
  }

  // If token is invalid or session not open
  if (!tokenInfo?.valid) {
    return (
      <div className="min-h-screen bg-[#0a0e17] flex items-center justify-center p-4">
        <div className="w-full max-w-md glass-panel rounded-3xl p-8 border border-red-500/20 shadow-2xl text-center">
          <div className="w-16 h-16 rounded-2xl bg-red-500/10 text-red-400 flex items-center justify-center mx-auto mb-4 border border-red-500/20">
            <AlertCircle className="w-8 h-8" />
          </div>
          <h2 className="text-xl font-bold text-white mb-2">Mã Điểm Danh Không Hợp Lệ</h2>
          <p className="text-sm text-slate-400 mb-6">
            {tokenInfo?.message || 'Mã QR đã hết hạn, hoặc Trợ giảng chưa mở phiên điểm danh cho giai đoạn này.'}
          </p>
          <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-400">
            Vui lòng chú ý màn hình máy chiếu hoặc yêu cầu Trợ giảng mở lại mã điểm danh mới nhất.
          </div>
        </div>
      </div>
    );
  }

  const isCheckIn = tokenInfo.stage === 'CHECK_IN';

  return (
    <div className="min-h-screen bg-[#0a0e17] relative flex items-center justify-center p-4 overflow-hidden">
      <div className="absolute top-1/3 left-1/2 -translate-x-1/2 -translate-y-1/2 w-80 h-80 bg-indigo-600/15 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-md relative z-10">
        {/* Header Branding */}
        <div className="text-center mb-6">
          <div className="inline-flex p-3 rounded-2xl bg-gradient-to-tr from-indigo-600 to-purple-600 shadow-xl shadow-indigo-500/25 mb-3">
            <QrCode className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-xl font-extrabold text-white">Xác Nhận Điểm Danh</h1>
          <p className="text-xs text-slate-400 mt-0.5">Hệ thống điểm danh lớp học tự động</p>
        </div>

        {/* 1. Pending Review Screen */}
        {result?.reviewRequired ? (
          <div className="glass-panel rounded-3xl p-8 border border-amber-500/40 shadow-2xl text-center animate-fade-in bg-amber-950/10">
            <div className="w-16 h-16 rounded-full bg-amber-500/20 text-amber-400 flex items-center justify-center mx-auto mb-4 border border-amber-500/30">
              <Clock className="w-9 h-9" />
            </div>

            <span className="inline-block px-3 py-1 rounded-full text-xs font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30 mb-2 uppercase tracking-wider">
              Chờ Giảng Viên Xác Nhận
            </span>

            <h2 className="text-xl font-extrabold text-white mb-1">
              {result.studentName || 'Sinh viên'}
            </h2>
            <p className="text-xs font-mono text-amber-400 font-bold mb-4">
              MSSV: {studentCode.toUpperCase()}
            </p>

            <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-amber-200 text-xs leading-relaxed mb-6 font-medium">
              {result.message || 'Yêu cầu điểm danh của bạn đã được gửi và đang chờ giảng viên xác nhận. Lượt điểm danh chưa được xác nhận chính thức.'}
            </div>

            <div className="p-4 rounded-2xl bg-slate-900/80 border border-slate-800 text-left text-xs space-y-2 mb-6">
              <div className="flex justify-between text-slate-400">
                <span>Môn học:</span>
                <span className="text-slate-200 font-medium">{tokenInfo.className}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Buổi học:</span>
                <span className="text-slate-200 font-medium">{tokenInfo.sessionName}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Thời gian gửi:</span>
                <span className="text-amber-400 font-mono font-medium">
                  {new Date().toLocaleTimeString('vi-VN')}
                </span>
              </div>
            </div>

            <div className="p-3 rounded-xl bg-slate-900 border border-slate-800 text-[11px] text-slate-400">
              Lượt điểm danh chưa được xác nhận chính thức. Giảng viên / Trợ giảng sẽ kiểm tra và xác nhận thủ công trên bảng điều khiển.
            </div>
          </div>
        ) : result?.success ? (
          /* 2. Success Screen */
          <div className="glass-panel rounded-3xl p-8 border border-emerald-500/30 shadow-2xl text-center animate-fade-in">
            <div className="w-16 h-16 rounded-full bg-emerald-500/15 text-emerald-400 flex items-center justify-center mx-auto mb-4 border border-emerald-500/30 animate-pulse">
              <CheckCircle2 className="w-10 h-10" />
            </div>

            <span className="inline-block px-3 py-1 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-300 border border-emerald-500/20 mb-2 uppercase tracking-wider">
              {isCheckIn ? 'Check-in thành công' : 'Check-out thành công'}
            </span>

            <h2 className="text-xl font-extrabold text-white mb-1">
              {result.studentName || 'Sinh viên'}
            </h2>
            <p className="text-xs font-mono text-indigo-400 font-bold mb-6">
              MSSV: {studentCode.toUpperCase()}
            </p>

            <div className="p-4 rounded-2xl bg-slate-900/80 border border-slate-800 text-left text-xs space-y-2 mb-6">
              <div className="flex justify-between text-slate-400">
                <span>Môn học:</span>
                <span className="text-slate-200 font-medium">{tokenInfo.className}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Buổi học:</span>
                <span className="text-slate-200 font-medium">{tokenInfo.sessionName}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Thời gian ghi nhận:</span>
                <span className="text-emerald-400 font-mono font-medium">
                  {new Date().toLocaleTimeString('vi-VN')}
                </span>
              </div>
            </div>

            <div className="flex items-center justify-center space-x-1.5 text-xs text-slate-400">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>Dữ liệu đã được ghi nhận an toàn vào hệ thống</span>
            </div>
          </div>
        ) : (
          /* 3. Check-in Form Card */
          <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-slate-800 shadow-2xl">
            {/* Session Info Banner */}
            <div className="mb-6 p-4 rounded-2xl bg-indigo-500/10 border border-indigo-500/20">
              <div className="flex items-center justify-between mb-1">
                <span className="text-xs font-bold text-emerald-400 flex items-center gap-1">
                  <Clock className="w-3.5 h-3.5" />
                  {isCheckIn ? 'Giai đoạn: Check-in đầu giờ' : 'Giai đoạn: Check-out cuối giờ'}
                </span>
              </div>
              <h3 className="text-sm font-bold text-white mt-1">{tokenInfo.className}</h3>
              <p className="text-xs text-slate-400">{tokenInfo.sessionName}</p>
            </div>

            {/* GPS Location Status Indicator */}
            <div className="mb-5 p-3 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <MapPin className={`w-4 h-4 ${gpsStatus === 'ready' ? 'text-emerald-400' : gpsStatus === 'denied' ? 'text-red-400' : 'text-amber-400'}`} />
                <span className="text-slate-300 font-medium">Vị trí GPS:</span>
              </div>
              <div>
                {gpsStatus === 'ready' && coords && (
                  <span className="text-emerald-400 font-mono">
                    Đã nhận (±{Math.round(coords.accuracy)}m)
                  </span>
                )}
                {gpsStatus === 'acquiring' && (
                  <span className="text-amber-400 flex items-center gap-1">
                    <Loader2 className="w-3 h-3 animate-spin" />
                    Đang dò vị trí...
                  </span>
                )}
                {gpsStatus === 'denied' && (
                  <span className="text-red-400 font-medium">Chưa cấp quyền GPS</span>
                )}
                {gpsStatus === 'unavailable' && (
                  <span className="text-slate-400 font-medium">Không khả dụng</span>
                )}
              </div>
            </div>

            {/* Error Message */}
            {errorMessage && (
              <div className="mb-5 p-3.5 rounded-xl bg-red-500/10 border border-red-500/30 flex items-start space-x-3 text-red-400 text-xs">
                <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
                <span>{errorMessage}</span>
              </div>
            )}

            {/* Form */}
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                  Nhập Mã Số Sinh Viên (MSSV) *
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                    <User className="w-4 h-4" />
                  </div>
                  <input
                    type="text"
                    required
                    autoFocus
                    placeholder="Ví dụ: 20210001"
                    value={studentCode}
                    onChange={(e) => setStudentCode(e.target.value.toUpperCase())}
                    className="glass-input w-full pl-10 pr-4 py-3 rounded-xl text-base font-mono tracking-wider text-white placeholder-slate-500 uppercase focus:border-indigo-500"
                  />
                </div>
                <p className="text-[11px] text-slate-500 mt-1.5">
                  Vui lòng nhập chính xác MSSV đã đăng ký trong danh sách lớp.
                </p>
              </div>

              <button
                type="submit"
                disabled={submitting || !studentCode.trim()}
                className="w-full mt-2 py-3 px-4 rounded-xl font-bold text-sm text-white bg-gradient-to-r from-indigo-600 via-indigo-500 to-purple-600 hover:from-indigo-500 hover:to-purple-500 shadow-lg shadow-indigo-600/30 hover:shadow-indigo-600/40 active:scale-[0.99] transition-all flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Đang gửi điểm danh...</span>
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4" />
                    <span>Xác Nhận Điểm Danh {isCheckIn ? 'Check-in' : 'Check-out'}</span>
                  </>
                )}
              </button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
};

export default PublicAttendancePage;
