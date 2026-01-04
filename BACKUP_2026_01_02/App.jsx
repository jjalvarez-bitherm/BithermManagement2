import React, { useState, useEffect, useRef } from 'react';
import {
  Clock, Play, Square, LayoutDashboard, ClipboardCheck, Settings,
  History, User, ChevronRight, TrendingUp, CheckCircle2,
  LogOut, Lock, User as UserIcon, Loader2, MapPin, AlertCircle,
  Check, ArrowRight, ArrowLeft, ChevronLeft, MapPinned,
  Crosshair, X, Signal, SignalHigh, HelpCircle
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

const API_BASE = 'http://localhost:3001/api';

const App = () => {
  const [user, setUser] = useState(null);
  const [config, setConfig] = useState({ appName: 'Bitherm Admin', primaryColor: '#3b82f6', logoText: 'B' });
  const [loginData, setLoginData] = useState({ username: '', password: '' });
  const [loginError, setLoginError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const [geoModal, setGeoModal] = useState({ show: false, accuracy: null, target: 40 });
  const [geoError, setGeoError] = useState(null);
  const [showFallback, setShowFallback] = useState(false);
  const latestPosRef = useRef(null);

  const [availableOTs, setAvailableOTs] = useState([]);
  const [locations, setLocations] = useState([]);
  const [isClockedIn, setIsClockedIn] = useState(false);
  const [startTime, setStartTime] = useState(null);
  const [elapsedTime, setElapsedTime] = useState(0);
  const [showDistribution, setShowDistribution] = useState(false);
  const [stats, setStats] = useState({ totalHours: '0.0h', extraHours: '0.0h', trabajados: '0.0 días', vacaciones: '0.0 días', baja: '0.0 días', history: [] });

  const [currentDate, setCurrentDate] = useState(new Date());
  const timerRef = useRef(null);
  const watchIdRef = useRef(null);

  useEffect(() => {
    fetch(`${API_BASE}/config`).then(res => res.json()).then(setConfig).catch(console.error);
    fetch(`${API_BASE}/locations`).then(res => res.json()).then(setLocations).catch(console.error);
  }, []);

  useEffect(() => {
    let interval = null;
    if (isClockedIn && startTime) {
      const updateTimer = () => setElapsedTime(Date.now() - startTime);
      updateTimer();
      interval = setInterval(updateTimer, 1000);
    } else {
      setElapsedTime(0);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [isClockedIn, startTime]);

  useEffect(() => {
    if (user) {
      fetch(`${API_BASE}/ots/${user.username}`).then(res => res.json()).then(setAvailableOTs).catch(console.error);
      fetchStats();
    }
  }, [user, currentDate]);

  const fetchStats = async () => {
    if (!user) return;
    try {
      const month = currentDate.getMonth();
      const year = currentDate.getFullYear();
      const res = await fetch(`${API_BASE}/stats/${user.username}?month=${month}&year=${year}`);
      const data = await res.json();
      if (data) {
        setStats(data);
        if (data.activeSession) {
          setStartTime(data.activeSession.startTime);
          setElapsedTime(Date.now() - data.activeSession.startTime);
          setIsClockedIn(true);
        } else {
          setIsClockedIn(false);
          setStartTime(null);
          setElapsedTime(0);
        }
      }
    } catch (e) { console.error("Error fetching stats:", e); }
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setLoginError('');
    try {
      const res = await fetch(`${API_BASE}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(loginData)
      });
      const data = await res.json();
      if (data.success) setUser(data.user);
      else setLoginError(data.message);
    } catch (err) { setLoginError('Servidor fuera de línea'); }
    finally { setIsLoading(false); }
  };

  const getHighPrecisionLocation = () => {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject("Tu navegador no soporta geolocalización");
        return;
      }

      setGeoError(null);
      setShowFallback(false);
      latestPosRef.current = null;
      setGeoModal({ show: true, accuracy: null, target: 40 });

      const finish = (position) => {
        if (watchIdRef.current !== null) {
          navigator.geolocation.clearWatch(watchIdRef.current);
          watchIdRef.current = null;
        }
        const lat = position.coords.latitude;
        const lon = position.coords.longitude;
        const gps = `${lat}, ${lon}`;

        let detectedLocation = 'Externo';
        for (const loc of locations) {
          const [locLat, locLon] = loc.coords.split(',').map(c => parseFloat(c.trim()));
          const distance = calculateDistance(lat, lon, locLat, locLon);
          // Si la señal es buena (<=40), comprobamos tolerancia exacta.
          // Si la señal es de PC/WiFi (>40), somos más flexibles si la distancia es coherente.
          const isCloseEnough = (position.coords.accuracy <= 40 && distance <= loc.tolerance) ||
            (position.coords.accuracy > 40 && distance <= Math.max(loc.tolerance, position.coords.accuracy));

          if (isCloseEnough) { detectedLocation = loc.name; break; }
        }

        setGeoModal(prev => ({ ...prev, show: false }));
        resolve({ gps, location: detectedLocation });
      };

      const startWatching = (highAccuracy) => {
        if (watchIdRef.current) navigator.geolocation.clearWatch(watchIdRef.current);

        watchIdRef.current = navigator.geolocation.watchPosition(
          (position) => {
            const acc = Math.round(position.coords.accuracy);
            latestPosRef.current = position;
            setGeoModal(prev => ({ ...prev, accuracy: acc }));
            setShowFallback(true);

            if (acc <= 40) {
              finish(position);
            }
          },
          (error) => {
            console.warn("Geo error:", error);
            if (highAccuracy && (error.code === 3 || error.code === 2)) {
              startWatching(false);
            } else {
              let msg = "Error de ubicación.";
              if (error.code === 1) msg = "Permiso denegado.";
              else if (error.code === 2) msg = "Señal no disponible.";
              else if (error.code === 3) msg = "Tiempo agotado.";
              setGeoError(msg);
              setShowFallback(true);
            }
          },
          {
            enableHighAccuracy: highAccuracy,
            maximumAge: 0,
            timeout: highAccuracy ? 10000 : 30000
          }
        );
      };

      startWatching(true);

      window.__forceGeo = () => {
        if (latestPosRef.current) {
          finish(latestPosRef.current);
        } else {
          setGeoModal(prev => ({ ...prev, show: false }));
          if (watchIdRef.current) navigator.geolocation.clearWatch(watchIdRef.current);
          resolve({ gps: "0,0", location: "Externo (Sin Señal)" });
        }
      };
    });
  };

  const calculateDistance = (lat1, lon1, lat2, lon2) => {
    const R = 6371e3;
    const φ1 = lat1 * Math.PI / 180;
    const φ2 = lat2 * Math.PI / 180;
    const Δφ = (lat2 - lat1) * Math.PI / 180;
    const Δλ = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) + Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  };

  const handleClockIn = async () => {
    setIsLoading(true);
    try {
      const geo = await getHighPrecisionLocation();
      await fetch(`${API_BASE}/fichaje`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: user.username, tipo: 'ENTRADA', gps: geo.gps, ubicacion: geo.location
        })
      });
      setStartTime(Date.now());
      setIsClockedIn(true);
      fetchStats();
    } catch (err) { alert(err); }
    finally { setIsLoading(false); }
  };

  const handleSpecialRegistry = async (type) => {
    if (!confirm(`¿Confirmar registro de ${type} para hoy? (Se asignarán 8h automáticamente)`)) return;
    setIsLoading(true);
    try {
      const res = await fetch(`${API_BASE}/leave/start`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user.username, type })
      });
      const data = await res.json();
      if (data.success) {
        alert(`✅ ${type} registrado correctamente.`);
        fetchStats();
      } else alert("Error: " + data.message);
    } catch (err) { alert("Error de conexión"); }
    finally { setIsLoading(false); }
  };

  const saveFinalRegistry = async (distribution) => {
    const duracionFinal = elapsedTime;
    const duracionMinutos = Math.floor(duracionFinal / (1000 * 60));
    setIsLoading(true);
    try {
      const geo = await getHighPrecisionLocation();
      await fetch(`${API_BASE}/fichaje`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: user.username, tipo: 'SALIDA', duracion: duracionMinutos, gps: geo.gps, ubicacion: geo.location
        })
      });
      await fetch(`${API_BASE}/distribution`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user.username, distribution: distribution.normal, type: 'normal' })
      });
      if (Object.keys(distribution.extra).length > 0) {
        await fetch(`${API_BASE}/distribution`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: user.username, distribution: distribution.extra, type: 'extra' })
        });
      }
      setIsClockedIn(false);
      setStartTime(null);
      setElapsedTime(0);
      setShowDistribution(false); // Cerramos el modal ANTES del alert
      fetchStats();
      setTimeout(() => alert("✅ Registro guardado correctamente"), 100);
    } catch (err) { alert(err); }
    finally { setIsLoading(false); }
  };

  const formatTime = (ms) => {
    const s = Math.floor(ms / 1000);
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`;
  };

  const changeMonth = (delta) => {
    const newDate = new Date(currentDate);
    newDate.setMonth(newDate.getMonth() + delta);
    setCurrentDate(newDate);
  };

  const cancelGeo = () => {
    if (watchIdRef.current !== null) {
      navigator.geolocation.clearWatch(watchIdRef.current);
      watchIdRef.current = null;
    }
    setGeoModal(prev => ({ ...prev, show: false }));
    setIsLoading(false);
  };

  const monthLabels = ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"];

  if (!user) {
    return (
      <div className="auth-container">
        <motion.div initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} className="login-card">
          <div className="logo-badge" style={{ backgroundColor: config.primaryColor }}>{config.logoText}</div>
          <div className="text-center mb-12">
            <h1 className="text-4xl font-black text-slate-900 tracking-tight">{config.appName}</h1>
            <p className="text-slate-500 font-medium mt-2">Portal de Producción</p>
          </div>
          <form onSubmit={handleLogin} className="space-y-6">
            <div className="input-group">
              <label className="text-xs font-black text-slate-400 uppercase tracking-widest ml-1 mb-2 block">Usuario APP</label>
              <div className="relative">
                <UserIcon className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
                <input type="text" value={loginData.username} onChange={e => setLoginData({ ...loginData, username: e.target.value })}
                  className="input-field" placeholder="ej: jjalvarez" required />
              </div>
            </div>
            <div className="input-group">
              <label className="text-xs font-black text-slate-400 uppercase tracking-widest ml-1 mb-2 block">Contraseña</label>
              <div className="relative">
                <Lock className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
                <input type="password" value={loginData.password} onChange={e => setLoginData({ ...loginData, password: e.target.value })}
                  className="input-field" placeholder="••••••••" required />
              </div>
            </div>
            {loginError && <div className="text-red-500 text-sm font-bold text-center bg-red-50 py-3 rounded-xl border border-red-100">{loginError}</div>}
            <button type="submit" disabled={isLoading} style={{ backgroundColor: config.primaryColor }}
              className="w-full text-white font-black py-5 rounded-xl shadow-xl hover:opacity-90 transition-all flex items-center justify-center">
              {isLoading ? <Loader2 className="animate-spin" /> : 'INICIAR SESIÓN'}
            </button>
          </form>
          <p className="text-center text-slate-300 text-[10px] mt-12 font-bold tracking-widest uppercase">Version {config.version}</p>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="flex h-screen overflow-hidden bg-[#f3f4f6]">
      <aside className="w-72 bg-[#0f172a] text-white flex flex-col p-8 space-y-10 z-20">
        <div className="flex items-center space-x-4">
          <div className="w-12 h-12 rounded-lg flex items-center justify-center font-black text-2xl shadow-lg" style={{ backgroundColor: config.primaryColor }}>{config.logoText}</div>
          <span className="font-bold text-xl tracking-tight">{config.appName}</span>
        </div>
        <nav className="flex-1 space-y-3">
          <NavItem icon={<LayoutDashboard size={22} />} label="Control Horario" active color={config.primaryColor} />
          <NavItem icon={<ClipboardCheck size={22} />} label="Inspecciones" />
          <NavItem icon={<History size={22} />} label="Historial" />
          <NavItem icon={<Settings size={22} />} label="Configuración" />
        </nav>
        <div className="pt-8 border-t border-slate-700">
          <div className="flex items-center space-x-4 bg-slate-800/40 p-4 rounded-xl">
            <div className="flex-1 overflow-hidden">
              <p className="text-sm font-bold truncate">{user.name}</p>
              <p className="text-[10px] text-slate-500 uppercase font-black tracking-widest">{user.role}</p>
            </div>
            <LogOut size={22} className="text-slate-500 cursor-pointer hover:text-white transition-colors ml-2" onClick={() => setUser(null)} />
          </div>
        </div>
      </aside>

      <main className="flex-1 overflow-y-auto">
        <header className="h-24 bg-white/80 backdrop-blur-md border-b flex items-center justify-between px-10 sticky top-0 z-10">
          <h1 className="text-2xl font-black text-slate-800">Panel de Control</h1>
          <div className="flex items-center space-x-4">
            {isLoading && <Loader2 className="animate-spin text-slate-400" size={20} />}
            <div className="flex items-center space-x-4 bg-slate-100 px-5 py-2.5 rounded-full text-sm font-black text-slate-600">
              <div className={`w-2.5 h-2.5 rounded-full ${isClockedIn ? 'bg-emerald-500 animate-pulse' : 'bg-slate-300'}`}></div>
              <span>{isClockedIn ? 'FICHAJE ACTIVO' : 'SIN ACTIVIDAD'}</span>
            </div>
          </div>
        </header>

        <div className="p-10 max-w-7xl mx-auto space-y-10">
          <section className="grid grid-cols-1 lg:grid-cols-3 gap-10">
            <div className="lg:col-span-2 bg-white rounded-2xl p-10 border border-slate-200 shadow-xl relative overflow-hidden group">
              <div className="absolute -top-10 -right-10 opacity-5 transition-transform duration-700"><Clock size={300} /></div>
              <div className="relative">
                <p className="text-slate-400 font-black text-xs uppercase tracking-widest mb-4">Tiempo Hoy</p>
                <h2 className="text-8xl font-black text-slate-900 tracking-tighter mb-10 tabular-nums">{formatTime(isClockedIn ? elapsedTime : 0)}</h2>
                <div className="flex flex-wrap gap-4">
                  {!isClockedIn ? (
                    <>
                      <motion.button whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }} onClick={handleClockIn} disabled={isLoading}
                        className="bg-blue-600 text-white px-10 py-5 rounded-xl font-black flex items-center space-x-3 shadow-2xl shadow-blue-200"
                        style={{ backgroundColor: config.primaryColor }}>
                        <Play fill="currentColor" size={20} /><span>REGISTRAR ENTRADA</span>
                      </motion.button>

                      {availableOTs.some(ot => ['VACACIONES', 'BAJAMED'].includes(ot.id)) && (
                        <div className="flex space-x-2">
                          {availableOTs.filter(ot => ['VACACIONES', 'BAJAMED'].includes(ot.id)).map(ot => (
                            <motion.button key={ot.id} whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}
                              onClick={() => handleSpecialRegistry(ot.id)} disabled={isLoading}
                              className={`px-6 py-5 rounded-xl font-black flex items-center space-x-2 border-2 transition-all ${ot.id === 'VACACIONES' ? 'border-emerald-100 text-emerald-600 hover:bg-emerald-50' : 'border-red-100 text-red-600 hover:bg-red-50'
                                }`}>
                              <HelpCircle size={18} />
                              <span className="text-xs uppercase tracking-widest">{ot.id}</span>
                            </motion.button>
                          ))}
                        </div>
                      )}
                    </>
                  ) : (
                    <motion.button whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }} onClick={() => setShowDistribution(true)}
                      className="bg-red-500 text-white px-10 py-5 rounded-xl font-black flex items-center space-x-3 shadow-2xl shadow-red-200">
                      <Square fill="currentColor" size={20} /><span>REGISTRAR SALIDA</span>
                    </motion.button>
                  )}
                </div>
              </div>
            </div>
            <div className="stat-card shadow-2xl border border-slate-800">
              <h3 className="font-black text-xs uppercase tracking-widest mb-8 text-blue-400 flex items-center space-x-2">
                <TrendingUp size={18} /><span>RESUMEN MENSUAL</span>
              </h3>
              <div className="space-y-8 relative">
                <StatRow label="Horas Normales" value={stats.totalHours} subValue={stats.trabajados} />
                <StatRow label="Horas Extra" value={stats.extraHours} color="text-orange-400" />
                <StatRow label="Vacaciones" value={stats.vacaciones} color="text-emerald-400" />
                {parseFloat(stats.baja) > 0 && <StatRow label="Baja Médica" value={stats.baja} color="text-red-400" />}
              </div>
            </div>
          </section>

          <section className="bg-white rounded-2xl border border-slate-200 shadow-xl overflow-hidden">
            <div className="p-8 border-b flex justify-between items-center bg-slate-50/50">
              <h3 className="font-black text-slate-800 text-xl tracking-tight">Actividad Reciente</h3>
              <div className="flex items-center bg-slate-100 rounded-xl p-1 shadow-inner">
                <button onClick={() => changeMonth(-1)} className="p-2 hover:bg-white rounded-lg transition-all text-slate-400 hover:text-slate-900">
                  <ChevronLeft size={18} />
                </button>
                <span className="px-6 font-black text-slate-800 text-xs uppercase tracking-widest min-w-[140px] text-center">
                  {monthLabels[currentDate.getMonth()]} - {currentDate.getFullYear().toString().slice(-2)}
                </span>
                <button onClick={() => changeMonth(1)} className="p-2 hover:bg-white rounded-lg transition-all text-slate-400 hover:text-slate-900">
                  <ChevronRight size={18} />
                </button>
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead className="bg-slate-100/50 text-slate-400 uppercase text-[10px] font-black tracking-widest">
                  <tr>
                    <th className="px-10 py-5">Fecha</th>
                    <th className="px-10 py-5">Entrada / Salida</th>
                    <th className="px-10 py-5">
                      Ubicación
                      <div className="flex mt-1 text-[8px] opacity-70">
                        <span className="w-32">ENTRADA</span>
                        <span>SALIDA</span>
                      </div>
                    </th>
                    <th className="px-10 py-5 text-right">Duración</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {stats && stats.history && stats.history.length > 0 ? stats.history.map((h, i) => (
                    <tr key={i} className="hover:bg-slate-50 transition-colors">
                      <td className="px-10 py-6 font-bold text-slate-700">{h.date}</td>
                      <td className="px-10 py-6 font-medium">
                        <div className="flex items-center space-x-2">
                          <span className="text-emerald-500 bg-emerald-50 px-2.5 py-1 rounded-lg border border-emerald-100 min-w-[65px] text-center">{h.in}</span>
                          <span className="text-slate-300">→</span>
                          {h.sinSalida ? (
                            <span className="flex items-center space-x-1.5 text-orange-500 bg-orange-50 px-2.5 py-1 rounded-lg border border-orange-100 font-black text-[10px]">
                              <AlertCircle size={12} />
                              <span>SIN SALIDA</span>
                            </span>
                          ) : (
                            <span className="text-red-500 bg-red-50 px-2.5 py-1 rounded-lg border border-red-100 min-w-[65px] text-center">{h.out}</span>
                          )}
                        </div>
                      </td>
                      <td className="px-10 py-6">
                        <div className="flex items-center">
                          <div className="w-32 flex items-center space-x-1.5 overflow-hidden">
                            <MapPin size={12} className="text-slate-300 flex-shrink-0" />
                            <span className="text-[10px] font-black text-slate-500 uppercase truncate pr-2">{h.locIn}</span>
                          </div>
                          {h.locOut !== '-' && (
                            <div className="flex items-center space-x-1.5 overflow-hidden border-l border-slate-100 pl-3">
                              <MapPinned size={12} className="text-emerald-400 flex-shrink-0" />
                              <span className="text-[10px] font-black text-emerald-600 uppercase truncate">{h.locOut}</span>
                            </div>
                          )}
                        </div>
                      </td>
                      <td className="px-10 py-6 text-right font-black text-slate-900">{h.duration}</td>
                    </tr>
                  )) : (
                    <tr><td colSpan="4" className="p-20 text-center text-slate-300 font-bold italic">No hay registros en este mes</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </main>

      <AnimatePresence>
        {geoModal.show && (
          <div className="fixed inset-0 z-[100] flex items-center justify-center p-6 bg-slate-900/60 backdrop-blur-md">
            <motion.div initial={{ scale: 0.9, opacity: 0 }} animate={{ scale: 1, opacity: 1 }}
              className="bg-white rounded-2xl p-10 max-w-sm w-full shadow-2xl text-center border border-slate-100 relative">
              <button onClick={cancelGeo} className="absolute top-4 right-4 text-slate-300 hover:text-slate-600 transition-colors">
                <X size={24} />
              </button>

              <div className="w-20 h-20 bg-blue-50 rounded-full flex items-center justify-center mx-auto mb-8 relative">
                <Crosshair className="text-blue-600 animate-pulse" size={32} />
                <div className="absolute inset-0 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
              </div>

              <h3 className="text-2xl font-black text-slate-900 mb-2 tracking-tight">Detectando posición...</h3>
              <p className="text-slate-500 font-bold mb-8 leading-snug">
                {geoModal.accuracy > 100
                  ? 'Señal de red detectada (baja precisión). Puedes registrar ahora o esperar.'
                  : 'El sistema está buscando tu coordenada exacta...'}
              </p>

              {geoError && (
                <div className="mb-6 p-4 bg-orange-50 text-orange-700 rounded-xl text-xs font-bold border border-orange-100 flex items-center space-x-2">
                  <AlertCircle size={14} className="flex-shrink-0" />
                  <span className="text-left">{geoError}</span>
                </div>
              )}

              {!geoError && (
                <div className="bg-slate-50 rounded-2xl p-6 border-2 border-slate-100 mb-6">
                  <div className="flex justify-between items-end mb-4">
                    <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Precisión actual</span>
                    <span className="text-[10px] font-black text-emerald-500 uppercase tracking-widest">Objetivo: {geoModal.target}m</span>
                  </div>
                  <div className="flex items-baseline justify-center space-x-2">
                    <span className={`text-5xl font-black tabular-nums ${geoModal.accuracy ? 'text-slate-900' : 'text-slate-300'}`}>
                      {geoModal.accuracy || '--'}
                    </span>
                    <span className="text-xl font-black text-slate-400">m</span>
                  </div>
                </div>
              )}

              {showFallback && (
                <motion.button initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }}
                  onClick={() => window.__forceGeo()}
                  className="w-full bg-slate-900 text-white font-black py-4 px-6 rounded-xl shadow-xl hover:bg-black transition-all flex items-center justify-center space-x-3 overflow-hidden">
                  <SignalHigh size={20} className="text-emerald-400" />
                  <span className="truncate">REGISTRAR CON ESTA SEÑAL</span>
                </motion.button>
              )}
            </motion.div>
          </div>
        )}

        {showDistribution && (
          <HourDistributionModal elapsedTime={elapsedTime} availableOTs={availableOTs} config={config}
            onClose={() => setShowDistribution(false)} onSave={saveFinalRegistry} isLoading={isLoading} />
        )}
      </AnimatePresence>
    </div>
  );
};

const NavItem = ({ icon, label, active = false, color }) => (
  <div className={`flex items-center space-x-4 p-4 rounded-xl cursor-pointer transition-all ${active ? 'text-white shadow-xl translate-x-1' : 'text-slate-500 hover:text-white hover:bg-slate-800'
    }`} style={active ? { backgroundColor: color } : {}}>
    {icon} <span className="font-bold">{label}</span>
  </div>
);

const StatRow = ({ label, value, subValue, color = "text-white" }) => (
  <div className="flex justify-between items-center border-b border-slate-700 pb-4">
    <span className="text-slate-400 font-bold text-sm">{label}</span>
    <div className="flex items-baseline space-x-3">
      <span className={`text-3xl font-black ${color}`}>{value}</span>
      {subValue && <span className="text-[11px] text-slate-400 font-black uppercase tracking-widest">{subValue}</span>}
    </div>
  </div>
);

const HourDistributionModal = ({ elapsedTime, availableOTs, config, onClose, onSave, isLoading }) => {
  const totalHoursDecimal = elapsedTime / (1000 * 60 * 60);

  // Lógica de 8.1h: Si es <= 8.1, todo es normal. Si es > 8.1, se separa en 8.0 + extra.
  const hasExtra = totalHoursDecimal > 8.1;
  const normalHoursMax = hasExtra ? 8 : totalHoursDecimal;
  const extraHoursMax = hasExtra ? totalHoursDecimal - 8 : 0;

  const [selectedOTs, setSelectedOTs] = useState([]);
  const [step, setStep] = useState(1);
  const [distribution, setDistribution] = useState({ normal: {}, extra: {} });

  const toggleOT = (id) => {
    if (id === 'VACACIONES' || id === 'BAJAMED') {
      // Si seleccionas una especial, se queda ella sola y saltamos
      setSelectedOTs([id]);
      setDistribution({
        normal: { [id]: 8 },
        extra: {}
      });
      setStep(2);
      return;
    }

    // Si seleccionas cualquier otra, quitamos las especiales si estaban
    setSelectedOTs(s => {
      const filtered = s.filter(i => i !== 'VACACIONES' && i !== 'BAJAMED');
      return filtered.includes(id) ? filtered.filter(i => i !== id) : [...filtered, id];
    });
  };

  const handleSliderChange = (otId, value, type) => {
    setDistribution(prev => ({
      ...prev,
      [type]: { ...prev[type], [otId]: parseFloat(value) }
    }));
  };

  const currentTotal = (type) => Object.values(distribution[type] || {}).reduce((a, b) => a + b, 0);

  useEffect(() => {
    if (selectedOTs.length === 1) {
      setDistribution({
        normal: { [selectedOTs[0]]: normalHoursMax },
        extra: extraHoursMax > 0 ? { [selectedOTs[0]]: extraHoursMax } : {}
      });
    }
  }, [selectedOTs, normalHoursMax, extraHoursMax]);

  const isStep2Valid = Math.abs(currentTotal('normal') - normalHoursMax) < 0.05;
  const isStep3Valid = extraHoursMax <= 0 || Math.abs(currentTotal('extra') - extraHoursMax) < 0.05;

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/90 backdrop-blur-sm">
      <motion.div initial={{ scale: 0.9, opacity: 0, y: 20 }} animate={{ scale: 1, opacity: 1, y: 0 }} exit={{ scale: 0.9, opacity: 0, y: 20 }}
        className="bg-white rounded-2xl w-full max-w-2xl overflow-hidden flex flex-col shadow-2xl border border-white/20">

        <div className="px-8 pt-10 pb-8 relative bg-slate-50/50">
          <div className="absolute top-0 left-0 w-full h-1.5 bg-slate-100">
            <motion.div className="h-full" initial={{ width: 0 }} animate={{ width: `${(step / 3) * 100}%` }} style={{ backgroundColor: config.primaryColor }} />
          </div>
          <div className="flex justify-between items-start">
            <div>
              <h2 className="text-3xl font-black text-slate-900 tracking-tight">Registro de Jornada</h2>
              <p className="text-slate-500 font-bold mt-1.5 flex items-center">
                <Clock size={16} className="mr-2" />
                Total: <span className="text-slate-900 ml-1.5 font-black">{totalHoursDecimal.toFixed(1)}h</span>
              </p>
            </div>
            <div className="bg-white px-4 py-2 rounded-xl border-2 border-slate-100 shadow-sm">
              <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest leading-none mb-1">Paso</p>
              <p className="text-xl font-black text-slate-800 leading-none">{step}/3</p>
            </div>
          </div>
        </div>

        <div className="flex-1 px-8 py-2 overflow-y-auto overflow-x-hidden max-h-[60vh] min-h-[350px] scrollbar-hide">
          {step === 1 && (
            <div className="space-y-4 pb-10">
              <div className="flex items-center justify-between mb-4">
                <p className="font-black text-slate-400 uppercase text-xs tracking-widest">1. Selección de OTs</p>
              </div>
              <div className="grid gap-3">
                {availableOTs.map(ot => (
                  <motion.div key={ot.id} whileHover={{ scale: 1.01 }} whileTap={{ scale: 0.99 }} onClick={() => toggleOT(ot.id)}
                    className={`p-4 rounded-xl border-2 transition-all cursor-pointer flex items-center space-x-4 ${selectedOTs.includes(ot.id) ? 'border-blue-500 bg-blue-50/50' : 'border-slate-100 hover:bg-slate-50'
                      }`}>
                    <div className={`w-6 h-6 rounded-lg border-2 flex items-center justify-center flex-shrink-0 ${selectedOTs.includes(ot.id) ? 'bg-blue-600 border-blue-600' : 'border-slate-200 bg-white'}`}>
                      {selectedOTs.includes(ot.id) && <Check size={14} className="text-white" strokeWidth={4} />}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-[9px] font-black text-slate-400 uppercase tracking-widest leading-none mb-1 truncate">{ot.id}</p>
                      <p className="font-black text-slate-800 text-base leading-tight truncate">{ot.title}</p>
                    </div>
                  </motion.div>
                ))}
              </div>
            </div>
          )}

          {step === 2 && (
            <div className="space-y-8 pb-10">
              <p className="font-black text-slate-400 uppercase text-xs tracking-widest">2. Horas Normales (Máx. 8h)</p>
              <div className="bg-slate-900 rounded-xl p-8 text-white flex justify-between items-center shadow-xl">
                <div><p className="text-slate-400 text-[10px] font-black uppercase tracking-widest mb-1">Total</p><p className="text-4xl font-black">{normalHoursMax.toFixed(1)}h</p></div>
                <div className="text-right"><p className="text-slate-400 text-[10px] font-black uppercase tracking-widest mb-1">Asignadas</p>
                  <p className={`text-4xl font-black ${isStep2Valid ? 'text-emerald-400' : 'text-orange-400'}`}>{currentTotal('normal').toFixed(1)}h</p></div>
              </div>
              <div className="space-y-12">
                {selectedOTs.map(otId => (
                  <div key={otId} className="space-y-5">
                    <div className="flex justify-between items-center">
                      <span className="font-black text-slate-800 text-lg truncate pr-4">{availableOTs.find(o => o.id === otId)?.title}</span>
                      <div className="bg-slate-100 px-4 py-2 rounded-xl font-black text-slate-900 flex-shrink-0">{(distribution.normal[otId] || 0).toFixed(1)}h</div>
                    </div>
                    <input type="range" min="0" max={normalHoursMax} step="0.5" value={distribution.normal[otId] || 0}
                      onChange={(e) => handleSliderChange(otId, e.target.value, 'normal')} />
                  </div>
                ))}
              </div>
            </div>
          )}

          {step === 3 && (
            <div className="space-y-8 pb-10">
              <p className="font-black text-slate-400 uppercase text-xs tracking-widest">3. Horas Extra</p>
              {extraHoursMax > 0 ? (
                <>
                  <div className="bg-orange-600 rounded-xl p-8 text-white flex justify-between items-center shadow-xl">
                    <div><p className="text-orange-200 text-[10px] font-black uppercase tracking-widest mb-1 text-center">Extra</p><p className="text-4xl font-black">{extraHoursMax.toFixed(1)}h</p></div>
                    <div className="text-right"><p className="text-orange-200 text-[10px] font-black uppercase tracking-widest mb-1">Asignadas</p>
                      <p className={`text-4xl font-black ${isStep3Valid ? 'text-white' : 'text-orange-200'}`}>{currentTotal('extra').toFixed(1)}h</p></div>
                  </div>
                  <div className="space-y-12">
                    {selectedOTs.map(otId => (
                      <div key={otId} className="space-y-5">
                        <div className="flex justify-between items-center">
                          <span className="font-black text-orange-950 text-lg truncate pr-4">{availableOTs.find(o => o.id === otId)?.title}</span>
                          <div className="bg-orange-50 px-4 py-2 rounded-xl font-black text-orange-600 flex-shrink-0">{(distribution.extra[otId] || 0).toFixed(1)}h</div>
                        </div>
                        <input type="range" min="0" max={extraHoursMax} step="0.5" value={distribution.extra[otId] || 0}
                          onChange={(e) => handleSliderChange(otId, e.target.value, 'extra')} />
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <div className="text-center py-16 bg-emerald-50 rounded-xl">
                  <CheckCircle2 size={50} className="text-emerald-500 mx-auto mb-4" />
                  <p className="text-2xl font-black text-emerald-900">¡Sin horas extra!</p>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="px-10 py-8 border-t bg-slate-50 flex items-center justify-between mt-auto">
          <button onClick={step === 1 ? onClose : () => setStep(step - 1)} className="font-black text-slate-400 uppercase tracking-widest text-[10px] hover:text-slate-900 transition-colors">
            {step === 1 ? 'CANCELAR' : 'ATRÁS'}
          </button>
          <motion.button whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}
            disabled={isLoading || (step === 1 && selectedOTs.length === 0) || (step === 2 && !isStep2Valid) || (step === 3 && !isStep3Valid)}
            onClick={() => {
              if (step === 1) setStep(2);
              else if (step === 2) hasExtra ? setStep(3) : onSave(distribution);
              else onSave(distribution);
            }}
            className="flex items-center space-x-3 text-white px-8 py-3.5 rounded-xl font-black shadow-2xl disabled:bg-slate-200 transition-all text-xs tracking-widest"
            style={{ backgroundColor: (isLoading || (step === 1 && selectedOTs.length === 0) || (step === 2 && !isStep2Valid)) ? '#cbd5e1' : config.primaryColor }}>
            {isLoading ? <Loader2 className="animate-spin" /> : <span>{(step === 3 || (step === 2 && extraHoursMax === 0)) ? 'FINALIZAR' : 'CONTINUAR'}</span>}
          </motion.button>
        </div>
      </motion.div>
    </motion.div>
  );
};

export default App;
