import React, { useState, useEffect, useRef } from 'react';
import {
  Clock, Play, Square, LayoutDashboard, ClipboardCheck, Settings,
  History, User, ChevronRight, TrendingUp, CheckCircle2,
  LogOut, Lock, User as UserIcon, Loader2, MapPin, AlertCircle,
  Check, ArrowRight, ArrowLeft, ChevronLeft, MapPinned,
  Crosshair, X, Signal, SignalHigh, HelpCircle, Calendar, Plus,
  Minus, Save, Info, Users, Edit2, Trash2, CheckCircle, XCircle,
  Search, Filter, ChevronUp, ChevronDown, ListFilter, Calendar as CalendarIcon, RefreshCw, Archive, CheckCheck, AlertTriangle
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

const API_BASE = 'http://localhost:3001/api';

// Helper para formatear fechas DD/MM/YYYY con ceros
function formatDateWithZeros(dateStr) {
  if (!dateStr) return dateStr;
  const parts = dateStr.split('/');
  if (parts.length !== 3) return dateStr;
  const day = parts[0].padStart(2, '0');
  const month = parts[1].padStart(2, '0');
  const year = parts[2];
  return `${day}/${month}/${year}`;
}

// Helpers para caducidades
const parseDDMMYYYY = (v) => {
  if (!v) return null;
  const str = v.toString().trim();
  const m = str.match(/^(\d{1,2})\/(\d{1,2})\/(\d{2,4})$/);
  if (!m) return null;
  const d = parseInt(m[1], 10);
  const mo = parseInt(m[2], 10) - 1;
  const y = parseInt(m[3].length === 2 ? (m[3] < 50 ? '20' + m[3] : '19' + m[3]) : m[3], 10);
  const dt = new Date(y, mo, d);
  return isNaN(dt.getTime()) ? null : dt;
};

const caducityStatus = (value) => {
  const date = parseDDMMYYYY(value);
  if (!date) return null; // no es fecha
  const today = new Date();
  today.setHours(0,0,0,0);
  const diffDays = Math.floor((date.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  if (diffDays < 0) return 'expired';
  if (diffDays <= 30) return 'warning';
  return 'valid';
};

const App = () => {
  const [user, setUser] = useState(null);
  const [config, setConfig] = useState({ appName: 'Bitherm Admin', primaryColor: '#3b82f6', secondaryColor: '#ef4444', sidebarBg: '#0f172a', sidebarText: '#ffffff', sidebarItemText: '#94a3b8', logoText: 'B', logoImageUrl: '', faviconUrl: '' });
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
  const [activeTab, setActiveTab] = useState('dashboard');

  const [currentDate, setCurrentDate] = useState(new Date());
  const timerRef = useRef(null);
  const watchIdRef = useRef(null);

  // Estado para recarga de cuadrantes
  const [showReloadModal, setShowReloadModal] = useState(false);
  const [reloadConfig, setReloadConfig] = useState({ 
    sourceBook: '', 
    sourceSheet: '',
    destBook: '',
    destSheet: '',
    startDate: '',
    endDate: ''
  });
  const [availableBooks, setAvailableBooks] = useState([]);
  const [sourceSheets, setSourceSheets] = useState([]);
  const [destSheets, setDestSheets] = useState([]);
  const [sourceDates, setSourceDates] = useState({ firstDate: null, lastDate: null });
  const [destDates, setDestDates] = useState({ lastVacationDate: null });
  const [reloadLoading, setReloadLoading] = useState(false);
  const [absenceLogs, setAbsenceLogs] = useState([]);
  const [teams, setTeams] = useState({});
  const [festivos, setFestivos] = useState([]);

  const fetchAbsenceLogs = async () => {
    try {
      const res = await fetch(`${API_BASE}/absences/logs`);
      const data = await res.json();
      setAbsenceLogs(data);
    } catch (error) {
      console.error('Error fetching absence logs:', error);
    }
  };

  const fetchTeams = async () => {
    try {
      const res = await fetch(`${API_BASE}/teams`);
      const data = await res.json();
      console.log('🔴 FETCHTEAMS - URL:', `${API_BASE}/teams`);
      console.log('🔴 FETCHTEAMS - Response:', res.status, res.ok);
      console.log('🔴 FETCHTEAMS - Data recibida:', data);
      console.log('🔴 FETCHTEAMS - Tipo de data:', typeof data, Array.isArray(data));
      setTeams(data);
    } catch (error) {
      console.error('❌ Error fetching teams:', error);
    }
  };

  useEffect(() => {
    fetch(`${API_BASE}/config`).then(res => res.json()).then(setConfig).catch(console.error);
    fetch(`${API_BASE}/locations`).then(res => res.json()).then(setLocations).catch(console.error);
    fetchTeams();
  }, []);

  // Aplicar favicon cuando cambie en config
  useEffect(() => {
    if (config && config.faviconUrl) {
      let link = document.querySelector("link[rel='icon']");
      if (!link) {
        link = document.createElement('link');
        link.rel = 'icon';
        document.head.appendChild(link);
      }
      link.type = 'image/png';
      link.href = config.faviconUrl;
    }
  }, [config?.faviconUrl]);

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

  useEffect(() => {
    if (showReloadModal) {
      // Cargar libros disponibles
      fetch(`${API_BASE}/books`)
        .then(res => res.json())
        .then(data => setAvailableBooks(data.books || []))
        .catch(console.error);
    }
  }, [showReloadModal]);

  // Efecto para cargar hojas del libro de origen
  useEffect(() => {
    if (reloadConfig.sourceBook) {
      fetch(`${API_BASE}/books/${reloadConfig.sourceBook}/sheets`)
        .then(res => res.json())
        .then(data => setSourceSheets(data.sheets || []))
        .catch(e => {
          console.error('Error cargando hojas origen:', e);
          setSourceSheets([]);
        });
    }
  }, [reloadConfig.sourceBook]);

  // Efecto para cargar fechas del libro y hoja de origen
  useEffect(() => {
    if (reloadConfig.sourceBook && reloadConfig.sourceSheet) {
      fetch(`${API_BASE}/sheet-dates/${reloadConfig.sourceBook}/${reloadConfig.sourceSheet}`)
        .then(res => res.json())
        .then(data => setSourceDates(data))
        .catch(e => {
          console.error('Error cargando fechas origen:', e);
          setSourceDates({ firstDate: null, lastDate: null });
        });
    }
  }, [reloadConfig.sourceBook, reloadConfig.sourceSheet]);

  // Efecto para cargar hojas del libro de destino
  useEffect(() => {
    if (reloadConfig.destBook) {
      fetch(`${API_BASE}/books/${reloadConfig.destBook}/sheets`)
        .then(res => res.json())
        .then(data => setDestSheets(data.sheets || []))
        .catch(e => {
          console.error('Error cargando hojas destino:', e);
          setDestSheets([]);
        });
    }
  }, [reloadConfig.destBook]);

  // Efecto para cargar última fecha de vacaciones del destino
  useEffect(() => {
    if (reloadConfig.destBook && reloadConfig.destSheet) {
      fetch(`${API_BASE}/last-vacation-date/${reloadConfig.destBook}/${reloadConfig.destSheet}`)
        .then(res => res.json())
        .then(data => setDestDates(data))
        .catch(e => {
          console.error('Error cargando última fecha destino:', e);
          setDestDates({ lastVacationDate: null });
        });
    }
  }, [reloadConfig.destBook, reloadConfig.destSheet]);

  // Efecto para cargar festivos cuando se selecciona hoja de origen
  useEffect(() => {
    if (reloadConfig.sourceBook && reloadConfig.sourceSheet) {
      fetch(`${API_BASE}/festivos/${reloadConfig.sourceBook}/${reloadConfig.sourceSheet}`)
        .then(res => res.json())
        .then(data => setFestivos(data || []))
        .catch(e => {
          console.error('Error cargando festivos:', e);
          setFestivos([]);
        });
    }
  }, [reloadConfig.sourceBook, reloadConfig.sourceSheet]);

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

  const saveFestivosToDatabase = async (festivosToSave) => {
    try {
      const res = await fetch(`${API_BASE}/festivos/save`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ festivos: festivosToSave })
      });
      const data = await res.json();
      return data;
    } catch (err) {
      console.error('Error guardando festivos:', err);
      throw err;
    }
  };

  const handleReloadQuadrants = async () => {
    if (!reloadConfig.sourceBook || !reloadConfig.sourceSheet || !reloadConfig.destBook || !reloadConfig.destSheet) {
      alert('Por favor selecciona libro y hoja para origen y destino');
      return;
    }
    setReloadLoading(true);
    try {
      const res = await fetch(`${API_BASE}/reload-quadrants`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          sourceBookId: reloadConfig.sourceBook,
          sourceSheet: reloadConfig.sourceSheet,
          destBookId: reloadConfig.destBook,
          destSheet: reloadConfig.destSheet,
          startDate: reloadConfig.startDate || sourceDates.firstDate,
          endDate: reloadConfig.endDate || sourceDates.lastDate
        })
      });
      const data = await res.json();
      if (data.success) {
        // Guardar festivos en AUSENCIAS-LOG
        if (Array.isArray(festivos) && festivos.length > 0) {
          try {
            const festivoData = await saveFestivosToDatabase(festivos);
            alert(`✅ Cuadrante recargado: ${data.message}\n✓ ${festivoData.saved} festivo(s) guardado(s)`);
          } catch (err) {
            alert(`✅ Cuadrante recargado: ${data.message}\n⚠️ Error al guardar festivos`);
          }
        } else {
          alert(`✅ Cuadrante recargado: ${data.message}`);
        }
        
        setShowReloadModal(false);
        setReloadConfig({ sourceBook: '', sourceSheet: '', destBook: '', destSheet: '', startDate: '', endDate: '' });
        fetchStats();
        // Recargar datos del calendario inmediatamente después de la importación
        await fetchAbsenceLogs();
      } else alert("Error: " + data.message);
    } catch (err) {
      console.error(err);
      alert("Error al recargar cuadrante");
    } finally {
      setReloadLoading(false);
    }
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
          <div className="logo-badge" style={{ backgroundColor: config.primaryColor }}>
            {config.logoImageUrl ? (
              <img src={config.logoImageUrl} alt="logo" className="w-10 h-10 object-cover rounded-lg" />
            ) : (
              <span className="text-white font-black text-2xl">{config.logoText}</span>
            )}
          </div>
          <div className="text-center mb-12">
            <h1 className="text-4xl font-black text-slate-900 tracking-tight whitespace-pre-line">{config.appName}</h1>
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
      <aside className="w-72 text-white flex flex-col p-8 space-y-10 z-20" style={{ backgroundColor: config.sidebarBg || '#0f172a', color: config.sidebarText || '#ffffff' }}>
        <div className="flex items-center space-x-4">
          <div className="w-12 h-12 rounded-lg flex items-center justify-center font-black text-2xl shadow-lg overflow-hidden" style={{ backgroundColor: config.primaryColor }}>
            {config.logoImageUrl ? (
              <img src={config.logoImageUrl} alt="logo" className="w-full h-full object-cover" />
            ) : (
              <span>{config.logoText}</span>
            )}
          </div>
          <span className="font-bold text-xl tracking-tight whitespace-pre-line">{config.appName}</span>
        </div>
        <nav className="flex-1 space-y-3">
          {(config.menuSections || []).map(section => {
            if (section.superadminOnly && user.role !== 'SUPERADMIN') return null;
            const IconComponent = section.icon === 'LayoutDashboard' ? LayoutDashboard :
              section.icon === 'Calendar' ? Calendar :
              section.icon === 'ClipboardCheck' ? ClipboardCheck :
              section.icon === 'History' ? History :
              section.icon === 'User' ? User :
              section.icon === 'Settings' ? Settings : LayoutDashboard;
            return (
              <NavItem key={section.id} icon={<IconComponent size={22} />} label={section.label} active={activeTab === section.id} color={config.secondaryColor || config.primaryColor} itemColor={config.sidebarItemText || '#94a3b8'} onClick={() => setActiveTab(section.id)} />
            );
          })}
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

      <main className="flex-1 overflow-y-auto p-12 custom-scrollbar relative">
        {activeTab === 'dashboard' ? (
          <>
            <header className="flex justify-between items-center mb-16">
              <div>
                <h2 className="text-3xl font-black text-slate-800 tracking-tight">Hola, {user.name} 👋</h2>
                <div className="flex items-center space-x-2 text-slate-400 mt-2 font-bold text-sm uppercase tracking-widest">
                  <MapPin size={14} className="text-blue-500" />
                  <span>{isClockedIn ? 'Trabajando ahora' : 'Fuera de jornada'}</span>
                </div>
              </div>
              <div className="flex items-center space-x-6">
                <button onClick={() => setUser(null)} className="w-12 h-12 rounded-xl bg-white border border-slate-200 flex items-center justify-center text-slate-400 hover:text-red-500 hover:bg-red-50 transition-all shadow-sm">
                  <LogOut size={20} />
                </button>
              </div>
            </header>

            <section className="grid grid-cols-1 lg:grid-cols-3 gap-10 mb-16">
              <div className="lg:col-span-2 bg-white rounded-2xl p-10 border border-slate-200 shadow-xl relative overflow-hidden group">
                <div className="absolute top-0 right-0 w-64 h-64 bg-slate-50 rounded-full -mr-32 -mt-32 transition-transform group-hover:scale-110" />
                <div className="relative z-10">
                  <h3 className="font-black text-xs uppercase tracking-widest mb-10 text-slate-400 flex items-center space-x-2">
                    <Clock size={18} className="text-blue-500" /><span>Acción de Personal</span>
                  </h3>

                  {isClockedIn && (
                    <div className="mb-10">
                      <p className="text-slate-400 text-[10px] font-black uppercase tracking-widest mb-2">Tiempo Transcurrido</p>
                      <h2 className="text-8xl font-black text-slate-900 tracking-tighter tabular-nums">
                        {formatTime(elapsedTime)}
                      </h2>
                    </div>
                  )}

                  <div className="flex flex-wrap items-center gap-6">
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
                    {!stats.history || stats.history.length === 0 ? (
                      <tr>
                        <td colSpan="4" className="px-10 py-20 text-center text-slate-400 font-bold uppercase tracking-widest text-xs">
                          No hay registros este mes
                        </td>
                      </tr>
                    ) : (
                      stats.history.map((row, i) => (
                        <tr key={i} className="hover:bg-slate-50 transition-colors group">
                          <td className="px-10 py-6 font-bold text-slate-700">{row.date}</td>
                          <td className="px-10 py-6">
                            <div className="flex items-center space-x-4 font-bold">
                              <span className="text-emerald-500 bg-emerald-50 px-2.5 py-1 rounded-lg border border-emerald-100 min-w-[65px] text-center">{row.in}</span>
                              <span className="text-slate-300">→</span>
                              {row.sinSalida ? (
                                <span className="flex items-center space-x-1.5 text-orange-500 bg-orange-50 px-2.5 py-1 rounded-lg border border-orange-100 font-black text-[10px]">
                                  <AlertCircle size={12} />
                                  <span>SIN SALIDA</span>
                                </span>
                              ) : (
                                <span className="text-red-500 bg-red-50 px-2.5 py-1 rounded-lg border border-red-100 min-w-[65px] text-center">{row.out}</span>
                              )}
                            </div>
                          </td>
                          <td className="px-10 py-6">
                            <div className="flex items-center">
                              <div className="w-32 flex items-center space-x-1.5 overflow-hidden">
                                <MapPin size={12} className="text-slate-300 flex-shrink-0" />
                                <span className="text-[10px] font-black text-slate-500 uppercase truncate pr-2">{row.locIn}</span>
                              </div>
                              {row.locOut !== '-' && (
                                <div className="flex items-center space-x-1.5 overflow-hidden border-l border-slate-100 pl-3">
                                  <MapPinned size={12} className="text-emerald-400 flex-shrink-0" />
                                  <span className="text-[10px] font-black text-emerald-600 uppercase truncate">{row.locOut}</span>
                                </div>
                              )}
                            </div>
                          </td>
                          <td className="px-10 py-6 text-right font-black text-slate-900">{row.duration}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </section>
          </>
        ) : activeTab === 'vacations' ? (
          <AbsenceManagement 
            user={user} 
            config={config}
            showReloadModal={showReloadModal}
            setShowReloadModal={setShowReloadModal}
            reloadConfig={reloadConfig}
            setReloadConfig={setReloadConfig}
            availableBooks={availableBooks}
            sourceSheets={sourceSheets}
            destSheets={destSheets}
            sourceDates={sourceDates}
            destDates={destDates}
            reloadLoading={reloadLoading}
            handleReloadQuadrants={handleReloadQuadrants}
            fetchLogs={fetchAbsenceLogs}
            logs={absenceLogs}
            teams={teams}
            festivos={festivos}
          />
        ) : activeTab === 'profile' ? (
          (user.role === 'ADMIN' || user.role === 'SUPERADMIN')
            ? <AdminProfileTabs user={user} config={config} onLogout={() => setUser(null)} />
            : <ProfileView user={user} config={config} onLogout={() => setUser(null)} />
        ) : activeTab === 'settings' ? (
          <ConfigurationView config={config} setConfig={setConfig} />
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-slate-400 space-y-4">
            <Settings size={48} className="animate-spin-slow" />
            <p className="font-black uppercase tracking-widest text-xs">Módulo en desarrollo</p>
          </div>
        )}
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

const NavItem = ({ icon, label, active = false, color, itemColor, onClick }) => (
  <div onClick={onClick} className={`flex items-center space-x-4 p-4 rounded-xl cursor-pointer transition-all ${active ? 'text-white shadow-xl translate-x-1' : 'hover:text-white hover:bg-slate-800'
    }`} style={active ? { backgroundColor: color, color: '#ffffff' } : { color: itemColor }}>
    {icon} <span className="font-bold whitespace-pre-line">{label}</span>
  </div>
);

const CalendarView = ({ logs, teams, festivos }) => {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [popup, setPopup] = useState({ show: false, absences: [], day: null, pos: { x: 0, y: 0 } });

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  const firstDayRaw = new Date(year, month, 1).getDay();
  const firstDay = (firstDayRaw + 6) % 7;
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const monthName = currentDate.toLocaleString('es-ES', { month: 'long' });

  const prevMonth = () => { setCurrentDate(new Date(year, month - 1, 1)); setPopup({ ...popup, show: false }); };
  const nextMonth = () => { setCurrentDate(new Date(year, month + 1, 1)); setPopup({ ...popup, show: false }); };

  const acceptedLogs = logs.filter(l => l.ESTADO?.toUpperCase() === 'APROBADA');

  const normalizeDay = (d) => {
    const date = new Date(d);
    date.setHours(0, 0, 0, 0);
    return date.getTime();
  };

  const parseDate = (d) => {
    if (!d) return null;
    const parts = d.split('/');
    if (parts.length !== 3) return null;
    const date = new Date(parts[2], parts[1] - 1, parts[0]);
    date.setHours(0, 0, 0, 0);
    return date;
  };

  const getAbsencesForDay = (day) => {
    const currentMs = new Date(year, month, day).setHours(0, 0, 0, 0);
    return acceptedLogs.filter(l => {
      const start = parseDate(l["FECHA INICIO"]);
      const end = parseDate(l["FECHA FIN"]);
      return start && end && currentMs >= start.getTime() && currentMs <= end.getTime();
    });
  };

  const currentMonthAbsences = acceptedLogs.filter(l => {
    const start = parseDate(l["FECHA INICIO"]);
    const end = parseDate(l["FECHA FIN"]);
    const monthStart = new Date(year, month, 1).setHours(0, 0, 0, 0);
    const monthEnd = new Date(year, month + 1, 0).setHours(23, 59, 59, 999);
    return start && end && (start.getTime() <= monthEnd && end.getTime() >= monthStart);
  });

  const handleDayClick = (e, day, absences) => {
    if (absences.length === 0) return;
    setPopup({
      show: true, absences, day,
      pos: { x: e.clientX, y: e.clientY }
    });
  };

  const dayLabels = ['Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab', 'Dom'];

  return (
    <div className="flex flex-col space-y-4 animate-in fade-in duration-500 rounded-none h-full">
      <div className="bg-white rounded-none border border-slate-200 shadow-2xl overflow-hidden">
        <div className="p-6 border-b flex justify-center items-center bg-slate-50/50 rounded-none relative">
          <div className="flex flex-col items-center space-y-2 w-full">
            <div className="flex items-center space-x-12">
              <button onClick={prevMonth} className="p-2 bg-white border border-slate-200 hover:bg-slate-50 rounded-none transition-all"><ChevronLeft size={18} /></button>
              <h3 className="text-2xl font-black text-slate-800 uppercase tracking-tighter min-w-[240px] text-center">
                {monthName} <span className="text-blue-600 font-light">{year}</span>
              </h3>
              <button onClick={nextMonth} className="p-2 bg-white border border-slate-200 hover:bg-slate-50 rounded-none transition-all"><ChevronRight size={18} /></button>
            </div>
            {Array.isArray(festivos) && festivos.length > 0 && (
              <div className="text-[10px] font-black uppercase tracking-widest text-red-500 bg-red-50 px-3 py-1 border border-red-200 rounded-none">
                ✓ {festivos.length} festivo{festivos.length === 1 ? '' : 's'} cargado{festivos.length === 1 ? '' : 's'}
              </div>
            )}
          </div>
          
          <button onClick={() => setCurrentDate(new Date())} className="absolute right-6 px-4 py-2 bg-white border border-slate-200 text-[9px] font-black uppercase tracking-widest hover:bg-slate-50 rounded-none">Hoy</button>
        </div>

        <div className="grid grid-cols-7 gap-px bg-slate-200">
          {dayLabels.map((day, idx) => (
            <div key={day} className={`p-4 text-center text-[9px] font-black uppercase tracking-widest border-b border-slate-200 ${idx >= 5 ? 'bg-slate-100/80 text-slate-500' : 'bg-slate-50 text-slate-400'}`}>
              {day}
            </div>
          ))}
          {Array.from({ length: 42 }).map((_, i) => {
            const dayNumber = i - firstDay + 1;
            const isCurrentMonth = dayNumber > 0 && dayNumber <= daysInMonth;
            const absences = isCurrentMonth ? getAbsencesForDay(dayNumber) : [];
            const isWeekend = i % 7 >= 5;

            // Verificar si este día es festivo
            const currentDayDate = isCurrentMonth ? `${String(dayNumber).padStart(2, '0')}/${String(month + 1).padStart(2, '0')}/${year}` : '';
            const isFestivo = Array.isArray(festivos) && festivos.some(f => {
              const [d, m, y] = f.split('/');
              const festivoDate = `${String(parseInt(d)).padStart(2, '0')}/${String(parseInt(m)).padStart(2, '0')}/${y.length === 2 ? '20' + y : y}`;
              return festivoDate === currentDayDate;
            });

            const getT = (type) => (type || '').toString().toUpperCase().trim();
            const vacs = absences.filter(a => getT(a.TIPO) === 'VACACIONES').length;
            const perms = absences.filter(a => getT(a.TIPO) === 'PERMISO').length;
            const bajas = absences.filter(a => ['BAJA', 'BAJAMED', 'BAJA MÉDICA', 'ENFERMEDAD'].includes(getT(a.TIPO))).length;

            // Detectar conflictos de equipo (2+ personas del mismo equipo de vacaciones)
            const teamConflict = absences.filter(a => getT(a.TIPO) === 'VACACIONES').reduce((acc, absence) => {
              const userTeam = teams[absence.USUARIO];
              if (!userTeam || !userTeam.team) return acc;
              acc[userTeam.team] = (acc[userTeam.team] || 0) + 1;
              return acc;
            }, {});
            const hasTeamConflict = Object.values(teamConflict).some(count => count >= 2);

            return (
              <div key={i} onClick={(e) => isCurrentMonth && handleDayClick(e, dayNumber, absences)}
                className={`min-h-[64px] p-2 relative group transition-all cursor-pointer ${!isCurrentMonth ? 'bg-slate-50/20' : isFestivo ? 'bg-red-50 border border-red-200' : isWeekend ? 'bg-slate-50/40' : 'bg-white hover:bg-slate-50/30'}`}>
                {isCurrentMonth && (
                  <>
                    <div className="flex items-center justify-between">
                      <span className={`text-[11px] font-black tracking-tighter ${isWeekend ? 'text-slate-400' : isFestivo ? 'text-red-500' : 'text-slate-400 group-hover:text-blue-600'}`}>{dayNumber}</span>
                      {hasTeamConflict && <span className="text-orange-500 font-black text-sm" title="⚠️ Múltiples personas del mismo equipo de vacaciones">⚠️</span>}
                    </div>
                    {isFestivo && <p className="text-[8px] font-black uppercase text-red-400 tracking-wider mt-0.5">FESTIVO</p>}
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {Array.from({ length: vacs }).map((_, j) => <div key={`v-${j}`} className="w-2 h-2 bg-blue-500 rounded-none shadow-sm" title="Vacaciones" />)}
                      {Array.from({ length: perms }).map((_, j) => <div key={`p-${j}`} className="w-2 h-2 bg-amber-500 rounded-none shadow-sm" title="Permiso" />)}
                      {Array.from({ length: bajas }).map((_, j) => <div key={`b-${j}`} className="w-2 h-2 bg-red-500 rounded-none shadow-sm" title="Baja" />)}
                    </div>
                  </>
                )}
              </div>
            );
          })}
        </div>
      </div>

      <div className="bg-white border border-slate-200 shadow-xl overflow-hidden rounded-none flex-1 min-h-[400px]">
        <div className="p-8 border-b bg-slate-50/30 flex justify-between items-center">
          <h4 className="font-black text-sm uppercase tracking-[0.3em] text-slate-800">
            Resumen de Ausencias - {monthName}
          </h4>
          <span className="px-4 py-1.5 bg-blue-100 text-blue-700 font-black text-xs uppercase tracking-widest rounded-none border border-blue-200">
            {currentMonthAbsences.length} Aceptadas
          </span>
        </div>
        <div className="overflow-x-auto p-4">
          <table className="w-full text-left">
            <thead className="text-xs font-black uppercase text-slate-400 tracking-widest border-b">
              <tr>
                <th className="px-6 py-4 w-1/2">Empleado</th>
                <th className="px-6 py-4 w-3/20">Tipo</th>
                <th className="px-6 py-4 w-1/5">Periodo</th>
                <th className="px-6 py-4 w-3/20 text-center">Duración</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {currentMonthAbsences.length === 0 ? (
                <tr>
                  <td colSpan="4" className="p-20 text-center text-slate-400 text-sm font-bold uppercase tracking-widest italic">
                    Sin ausencias aceptadas este mes
                  </td>
                </tr>
              ) : (
                currentMonthAbsences.sort((a, b) => parseDate(a["FECHA INICIO"]) - parseDate(b["FECHA INICIO"])).map((log, idx) => {
                  const t = (log.TIPO || '').toString().toUpperCase().trim();
                  const isSingleDay = log["DIA NAT"] === 1 || log["DIA NAT"] === '1';
                  return (
                    <tr key={idx} className="hover:bg-slate-50 transition-colors">
                      <td className="px-6 py-4 font-bold text-slate-700 text-base w-1/2">{log["NOMBRE COMPLETO"]}</td>
                      <td className="px-6 py-4 w-3/20">
                        <span className={`px-2.5 py-1 text-xs font-black uppercase tracking-widest rounded-none ${t === 'VACACIONES' ? 'bg-blue-50 text-blue-600' : 'bg-amber-50 text-amber-600'}`}>
                          {log.TIPO}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm font-bold text-slate-500 tracking-tighter w-1/5">
                        {isSingleDay ? formatDateWithZeros(log["FECHA INICIO"]) : `${formatDateWithZeros(log["FECHA INICIO"])} → ${formatDateWithZeros(log["FECHA FIN"])}`}
                      </td>
                      <td className="px-6 py-4 text-center text-sm font-black text-slate-700 w-3/20">
                        {parseInt(log["DIA LAB"]) === parseInt(log["DIA NAT"]) ? `${log["DIA NAT"]} día${parseInt(log["DIA NAT"]) > 1 ? 's' : ''}` : `${log["DIA LAB"]} / ${log["DIA NAT"]}`}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      <AnimatePresence>
        {popup.show && (
          <>
            <div className="fixed inset-0 z-[400]" onClick={() => setPopup({ ...popup, show: false })} />
            <motion.div initial={{ opacity: 0, y: 5 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 5 }}
              style={{ position: 'fixed', top: popup.pos.y, left: popup.pos.x }}
              className="z-[401] min-w-[240px] bg-slate-900 text-white p-6 shadow-2xl border-l-4 border-blue-500 rounded-none pointer-events-none"
            >
              <h5 className="font-black text-[10px] uppercase tracking-[0.2em] mb-4 text-blue-400 border-b border-slate-700 pb-2">Aceptadas el día {popup.day}</h5>
              <div className="space-y-4">
                {popup.absences.map((abs, i) => {
                  const userTeam = teams[abs.USUARIO];
                  const roleText = userTeam?.roleLabel ? ` (${userTeam.roleLabel})` : '';
                  return (
                    <div key={i} className="flex flex-col border-b border-white/5 pb-2 last:border-0 last:pb-0">
                      <p className="font-black text-xs uppercase tracking-tight">
                        {abs["NOMBRE COMPLETO"]}{roleText}
                      </p>
                      <p className={`text-[9px] font-black uppercase tracking-widest mt-1 ${abs.TIPO?.toString().toUpperCase().trim() === 'VACACIONES' ? 'text-blue-300' : 'text-amber-300'}`}>{abs.TIPO}</p>
                    </div>
                  );
                })}
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
};

const ProfileView = ({ user, config, onLogout }) => {
  const [profile, setProfile] = useState({ groups: {} });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const initial = (user?.name || '').trim().charAt(0).toUpperCase() || 'U';

  useEffect(() => {
    if (!user) {
      setError('No hay usuario autenticado');
      setLoading(false);
      return;
    }
    
    const load = async () => {
      setLoading(true);
      setError('');
      
      try {
        if (!user.username) {
          setError('❌ Usuario sin identificador (username ausente)');
          setLoading(false);
          return;
        }
        
        const url = `${API_BASE}/profile/${user.username}`;
        console.log('📡 Fetching profile from:', url);
        console.log('📝 User object:', user);
        
        const res = await fetch(url);
        console.log('✓ Response status:', res.status);
        
        const data = await res.json();
        console.log('✓ Response data:', data);
        
        if (res.ok) {
          setProfile(data);
        } else {
          setError(`❌ ${data.message || 'Error cargando perfil'}`);
        }
      } catch (e) { 
        console.error('❌ ProfileView fetch error:', e);
        console.error('Error details:', { 
          message: e.message, 
          stack: e.stack,
          name: e.name 
        });
        setError(`❌ ${e.message || 'Conexión rechazada'}`); 
      }
      finally { 
        setLoading(false); 
      }
    };
    
    // Pequeño retardo para asegurar que el DOM está listo
    const timer = setTimeout(load, 100);
    return () => clearTimeout(timer);
  }, [user]);

  const isEditable = (groupName, item) => {
    const g = (groupName || '').toLowerCase();
    if (g.includes('personal') || g.includes('contacto')) return true;
    return false;
  };

  const [editModal, setEditModal] = useState({ show: false, item: null });

  const handleEditClick = (item) => {
    setEditModal({ show: true, item });
  };

  const handleModalSave = (updatedValue) => {
    if (!editModal.item) return;
    setProfile(prev => ({
      ...prev,
      groups: Object.fromEntries(Object.entries(prev.groups).map(([g, items]) => [g, items.map(i => i.field === editModal.item.field ? { ...i, value: updatedValue } : i)]))
    }));
    setEditModal({ show: false, item: null });
  };

  if (!user) return null;
  return (
    <div className="mx-auto space-y-10 animate-in fade-in duration-300 px-8 ml-0 md:ml-80">
      <header className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <div className="w-14 h-14 rounded-xl flex items-center justify-center text-white font-black text-2xl shadow-lg" style={{ backgroundColor: config.primaryColor }}>
            {initial}
          </div>
          <div>
            <h2 className="text-3xl font-black text-slate-800 tracking-tight">Mi perfil</h2>
            <p className="text-slate-400 text-sm font-bold uppercase tracking-widest">Gestión de cuenta</p>
          </div>
        </div>
        <button onClick={onLogout} className="px-4 py-2 bg-white border border-slate-200 rounded-xl text-slate-500 hover:text-red-600 hover:bg-red-50 transition-all flex items-center space-x-2">
          <LogOut size={18} />
          <span className="text-xs font-black uppercase tracking-widest">Cerrar sesión</span>
        </button>
      </header>

      {loading ? (
        <div className="flex items-center space-x-3 text-slate-400"><Loader2 className="animate-spin" size={18} /><span className="font-bold text-sm">Cargando perfil...</span></div>
      ) : error ? (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 font-bold text-sm">{error}</div>
      ) : (
        <section className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {Object.entries(profile.groups || {}).map(([groupName, items]) => (
            <div key={groupName} className="rounded-2xl p-8 border-8 border-slate-100 shadow-lg backdrop-blur-sm bg-white/50 hover:shadow-xl hover:border-slate-200 transition-all">
              <h3 className="font-black text-sm uppercase tracking-widest mb-6 text-slate-600 flex items-center space-x-2">
                {groupName.toLowerCase().includes('personal') ? <User size={18} className="text-blue-600" /> : <Info size={18} className="text-blue-600" />}
                <span>{groupName}</span>
              </h3>
              <div className="space-y-4">
                {items.map((item) => {
                  const isCadGroup = (groupName || '').toLowerCase().includes('caducidades');
                  const status = isCadGroup ? caducityStatus(item.value) : null;
                  const isEmpty = !item.value || item.value.toString().trim() === '';
                  const dotColor = status === 'valid' ? 'bg-emerald-500' : status === 'warning' ? 'bg-amber-500' : status === 'expired' ? 'bg-red-500' : 'bg-slate-300';
                  return (
                    <div key={`${groupName}-${item.field}`} className="flex items-center justify-between">
                      <span className="text-slate-400 text-xs font-black uppercase tracking-widest">{item.label}</span>
                      <div className="flex items-center space-x-3">
                        {isCadGroup && (
                          <span className={`inline-block w-2.5 h-2.5 rounded-full ${dotColor}`} title={status === 'valid' ? 'Vigente' : status === 'warning' ? 'Caduca en ≤30 días' : status === 'expired' ? 'Caducada' : 'Sin fecha'}></span>
                        )}
                        <span className="font-bold text-slate-800">{item.value || '-'}</span>
                        {isEmpty && (
                          <AlertTriangle size={16} className="text-amber-500" title="Campo vacío" />
                        )}
                        {isEditable(groupName, item) && (
                          <button onClick={() => handleEditClick(item)} className="p-2 bg-white border border-slate-200 rounded-xl text-slate-500 hover:text-blue-600 hover:bg-blue-50 transition-all" title="Editar">
                            <Edit2 size={16} />
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </section>
      )}

      <AnimatePresence>
        {editModal.show && editModal.item && (
          <EditProfileModal item={editModal.item} user={user} onClose={() => setEditModal({ show: false, item: null })} onSave={handleModalSave} />
        )}
      </AnimatePresence>
    </div>
  );
};

// Tabs de admins: Mi perfil, Cambios pendientes, Editar trabajador
const AdminProfileTabs = ({ user, config, onLogout }) => {
  const [tab, setTab] = useState('mi-perfil');
  return (
    <div className="space-y-6">
      <div className="flex items-center space-x-3">
        {[
          { id: 'mi-perfil', label: 'Mi perfil' },
          { id: 'cambios', label: 'Cambios pendientes' },
          { id: 'editar', label: 'Editar trabajador' }
        ].map(t => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`px-4 py-2 rounded-xl font-black text-xs uppercase tracking-widest border transition-all ${tab === t.id ? 'bg-blue-600 text-white border-blue-600 shadow' : 'bg-white text-slate-600 border-slate-200 hover:border-blue-300 hover:text-blue-700'}`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'mi-perfil' && <ProfileView user={user} config={config} onLogout={onLogout} />}
      {tab === 'cambios' && <PendingChangesTab />}
      {tab === 'editar' && <EditWorkerTab config={config} currentUser={user} />}
    </div>
  );
};

const PendingChangesTab = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [pendientes, setPendientes] = useState([]);
  const [archivados, setArchivados] = useState([]);
  const [showArchivados, setShowArchivados] = useState(false);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await fetch(`${API_BASE}/cambios-pendientes`);
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Error al cargar');
      setPendientes(data.pendientes || []);
      setArchivados(data.archivados || []);
    } catch (e) {
      setError(e.message || 'Servidor fuera de línea');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const updateEstado = async (changeId, nuevoEstado) => {
    const endpoint = nuevoEstado === 'archivado' ? 'archivar' : 'aceptar';
    await fetch(`${API_BASE}/cambios-pendientes/${endpoint}`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ changeId })
    });
    setPendientes(prev => prev.filter(c => c.id !== changeId));
    if (nuevoEstado === 'archivado') {
      const archivedItem = pendientes.find(c => c.id === changeId);
      if (archivedItem) setArchivados(prev => [...prev, { ...archivedItem, estado: 'archivado' }]);
    }
  };

  return (
    <div className="mx-auto space-y-6 animate-in fade-in duration-300">
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-black text-slate-800 tracking-tight">Cambios pendientes</h2>
          <p className="text-slate-400 text-sm font-bold uppercase tracking-widest">Visor de cambios realizados por usuarios</p>
        </div>
        <button onClick={load} className="px-4 py-2 bg-white border border-slate-200 rounded-xl text-slate-500 hover:text-blue-600 hover:bg-blue-50 transition-all flex items-center space-x-2 text-xs font-black uppercase tracking-widest">
          <RefreshCw size={16} />
          <span>Refrescar</span>
        </button>
      </header>

      {loading ? (
        <div className="flex items-center space-x-3 text-slate-400"><Loader2 className="animate-spin" size={18} /><span className="font-bold text-sm">Cargando cambios...</span></div>
      ) : error ? (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 font-bold text-sm">{error}</div>
      ) : (
        <div className="space-y-6">
          <section className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-slate-600 font-black text-sm uppercase tracking-widest">Pendientes ({pendientes.length})</h3>
            </div>
            {pendientes.length === 0 ? (
              <div className="p-6 bg-white border-2 border-dashed border-slate-200 rounded-2xl text-slate-400 font-bold text-xs uppercase tracking-widest">Sin cambios pendientes</div>
            ) : (
              <div className="space-y-2">
                {pendientes.map(c => (
                  <div key={c.id} className="rounded-xl border border-slate-200 bg-white hover:bg-slate-50 transition-colors p-3 flex items-center gap-4">
                    <div className="flex-shrink-0 w-40">
                      <div className="text-[9px] font-black uppercase tracking-widest text-slate-400">Usuario</div>
                      <div className="text-xs font-bold text-slate-700 truncate">{c.usuarioCambio}</div>
                    </div>
                    <div className="flex-shrink-0 w-20">
                      <div className="text-[9px] font-black uppercase tracking-widest text-slate-400">Campo</div>
                      <div className="text-[10px] font-bold text-slate-600 truncate">{c.campo}</div>
                    </div>
                    <div className="flex-shrink-0 w-32">
                      <div className="text-[9px] font-black uppercase tracking-widest text-slate-400">Fecha</div>
                      <div className="text-[10px] font-bold text-slate-500">{c.fechaCambio}</div>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="text-[9px] font-black uppercase tracking-widest text-slate-400">Valor anterior</div>
                      <div className="text-xs font-bold text-slate-600 line-through bg-slate-100 px-2 py-1 rounded inline-block max-w-full truncate">{c.valorAnterior || 'vacío'}</div>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="text-[9px] font-black uppercase tracking-widest text-slate-400">Valor nuevo</div>
                      <div className="text-xs font-bold text-blue-700 bg-blue-50 px-2 py-1 rounded inline-block max-w-full truncate">{c.valorNuevo || 'vacío'}</div>
                    </div>
                    <div className="flex-shrink-0 flex gap-2">
                      <button onClick={() => updateEstado(c.id, 'aceptado')} className="p-2 rounded-lg bg-emerald-50 text-emerald-600 border border-emerald-100 hover:bg-emerald-100 transition-colors" title="Aceptar">
                        <Check size={16} />
                      </button>
                      <button onClick={() => updateEstado(c.id, 'archivado')} className="p-2 rounded-lg bg-slate-50 text-slate-600 border border-slate-200 hover:bg-slate-100 transition-colors" title="Archivar">
                        <CheckCheck size={16} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className="space-y-3">
            <button onClick={() => setShowArchivados(!showArchivados)} className="flex items-center space-x-2 text-slate-500 font-black text-xs uppercase tracking-widest hover:text-slate-700 transition-colors">
              <ChevronDown className={`${showArchivados ? 'rotate-180' : ''} transition-transform`} size={14} />
              <span>Cambios archivados ({archivados.length})</span>
            </button>
            {showArchivados && (
              archivados.length === 0 ? (
                <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl text-slate-400 text-xs font-bold uppercase tracking-widest">Sin cambios archivados</div>
              ) : (
                <div className="space-y-2">
                  {archivados.map(c => (
                    <div key={c.id} className="rounded-xl border border-slate-200 bg-slate-50 p-3 flex items-center gap-4 opacity-75">
                      <div className="flex-shrink-0 w-40">
                        <div className="text-[9px] font-black uppercase tracking-widest text-slate-400">Usuario</div>
                        <div className="text-xs font-bold text-slate-700 truncate">{c.usuarioCambio}</div>
                      </div>
                      <div className="flex-shrink-0 w-20">
                        <div className="text-[9px] font-black uppercase tracking-widest text-slate-400">Campo</div>
                        <div className="text-[10px] font-bold text-slate-600 truncate">{c.campo}</div>
                      </div>
                      <div className="flex-shrink-0 w-32">
                        <div className="text-[9px] font-black uppercase tracking-widest text-slate-400">Fecha</div>
                        <div className="text-[10px] font-bold text-slate-500">{c.fechaCambio}</div>
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="text-[9px] font-black uppercase tracking-widest text-slate-400">Valor anterior</div>
                        <div className="text-xs font-bold text-slate-600 line-through bg-slate-100 px-2 py-1 rounded inline-block max-w-full truncate">{c.valorAnterior || 'vacío'}</div>
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="text-[9px] font-black uppercase tracking-widest text-slate-400">Valor nuevo</div>
                        <div className="text-xs font-bold text-slate-600 bg-slate-200 px-2 py-1 rounded inline-block max-w-full truncate">{c.valorNuevo || 'vacío'}</div>
                      </div>
                      <div className="flex-shrink-0 w-16 text-center">
                        <Archive size={16} className="text-slate-400 mx-auto" />
                      </div>
                    </div>
                  ))}
                </div>
              )
            )}
          </section>
        </div>
      )}
    </div>
  );
};

const EditWorkerTab = ({ config, currentUser }) => {
  const [workers, setWorkers] = useState([]);
  const [selectedUser, setSelectedUser] = useState('');
  const [profile, setProfile] = useState(null);
  const [loadingProfile, setLoadingProfile] = useState(false);
  const [error, setError] = useState('');
  const [editModal, setEditModal] = useState({ show: false, item: null });

  useEffect(() => {
    const loadWorkers = async () => {
      try {
        const res = await fetch(`${API_BASE}/workers`);
        const data = await res.json();
        setWorkers(data || []);
      } catch (e) {
        setError('No se pudieron cargar trabajadores');
      }
    };
    loadWorkers();
  }, []);

  const fetchProfile = async (username) => {
    if (!username) return;
    setLoadingProfile(true);
    setError('');
    try {
      const res = await fetch(`${API_BASE}/profile/${username}`);
      const data = await res.json();
      if (!res.ok) throw new Error(data.message || 'Error al cargar perfil');
      setProfile(data);
    } catch (e) {
      setError(e.message);
      setProfile(null);
    } finally {
      setLoadingProfile(false);
    }
  };

  const isEditable = () => true;

  const handleEditClick = (item) => setEditModal({ show: true, item });
  const handleModalSave = (updatedValue) => {
    if (!editModal.item) return;
    setProfile(prev => ({
      ...prev,
      groups: Object.fromEntries(Object.entries(prev.groups).map(([g, items]) => [g, items.map(i => i.field === editModal.item.field ? { ...i, value: updatedValue } : i)]))
    }));
    setEditModal({ show: false, item: null });
  };

  return (
    <div className="mx-auto space-y-8 animate-in fade-in duration-300">
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-black text-slate-800 tracking-tight">Editar trabajador</h2>
          <p className="text-slate-400 text-sm font-bold uppercase tracking-widest">Selecciona un trabajador y edita cualquier campo</p>
        </div>
        <button onClick={() => fetchProfile(selectedUser)} disabled={!selectedUser} className="px-4 py-2 bg-white border border-slate-200 rounded-xl text-slate-500 hover:text-blue-600 hover:bg-blue-50 transition-all text-xs font-black uppercase tracking-widest">
          Recargar perfil
        </button>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="md:col-span-1">
          <label className="text-xs font-black text-slate-400 uppercase tracking-widest mb-2 block">Trabajador</label>
          <select value={selectedUser} onChange={(e) => { setSelectedUser(e.target.value); fetchProfile(e.target.value); }} className="w-full px-4 py-3 border border-slate-200 rounded-xl text-slate-700 font-bold">
            <option value="">Selecciona...</option>
            {workers.map(w => (
              <option key={w.username} value={w.username}>{w.name} ({w.username})</option>
            ))}
          </select>
        </div>
      </div>

      {loadingProfile ? (
        <div className="flex items-center space-x-3 text-slate-400"><Loader2 className="animate-spin" size={18} /><span className="font-bold text-sm">Cargando perfil...</span></div>
      ) : error ? (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 font-bold text-sm">{error}</div>
      ) : !profile ? (
        <div className="p-6 bg-white border-2 border-dashed border-slate-200 rounded-2xl text-slate-400 font-bold text-xs uppercase tracking-widest">Selecciona un trabajador para ver su perfil</div>
      ) : (
        <section className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {Object.entries(profile.groups || {}).map(([groupName, items]) => (
            <div key={groupName} className="rounded-2xl p-8 border-8 border-slate-100 shadow-lg backdrop-blur-sm bg-white/50 hover:shadow-xl hover:border-slate-200 transition-all">
              <h3 className="font-black text-sm uppercase tracking-widest mb-6 text-slate-600 flex items-center space-x-2">
                <Info size={18} className="text-blue-600" />
                <span>{groupName}</span>
              </h3>
              <div className="space-y-4">
                {items.map((item) => {
                  const isCadGroup = (groupName || '').toLowerCase().includes('caducidades');
                  const status = isCadGroup ? caducityStatus(item.value) : null;
                  const isEmpty = !item.value || item.value.toString().trim() === '';
                  const dotColor = status === 'valid' ? 'bg-emerald-500' : status === 'warning' ? 'bg-amber-500' : status === 'expired' ? 'bg-red-500' : 'bg-slate-300';
                  return (
                    <div key={`${groupName}-${item.field}`} className="flex items-center justify-between">
                      <span className="text-slate-400 text-xs font-black uppercase tracking-widest">{item.label}</span>
                      <div className="flex items-center space-x-3">
                        {isCadGroup && (
                          <span className={`inline-block w-2.5 h-2.5 rounded-full ${dotColor}`} title={status === 'valid' ? 'Vigente' : status === 'warning' ? 'Caduca en ≤30 días' : status === 'expired' ? 'Caducada' : 'Sin fecha'}></span>
                        )}
                        <span className="font-bold text-slate-800">{item.value || '-'}
                        </span>
                        {isEmpty && (
                          <AlertTriangle size={16} className="text-amber-500" title="Campo vacío" />
                        )}
                        <button onClick={() => handleEditClick(item)} className="p-2 bg-white border border-slate-200 rounded-xl text-slate-500 hover:text-blue-600 hover:bg-blue-50 transition-all" title="Editar">
                          <Edit2 size={16} />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </section>
      )}

      <AnimatePresence>
        {editModal.show && editModal.item && (
          <EditProfileModal item={editModal.item} user={currentUser} targetUsername={selectedUser} onClose={() => setEditModal({ show: false, item: null })} onSave={handleModalSave} />
        )}
      </AnimatePresence>
    </div>
  );
};

const AbsenceManagement = ({ user, config, showReloadModal, setShowReloadModal, reloadConfig, setReloadConfig, availableBooks, sourceSheets, destSheets, sourceDates, destDates, reloadLoading, handleReloadQuadrants, fetchLogs, logs, teams, festivos }) => {
  const [workers, setWorkers] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [activeTab, setActiveTab] = useState('Calendario');
  const [activeSubTab, setActiveSubTab] = useState('Pendiente');
  const [reason, setReason] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [editRequest, setEditRequest] = useState(null);
  const [selectedRequests, setSelectedRequests] = useState(new Set());
  const [newRequest, setNewRequest] = useState({
    type: 'VACACIONES',
    startDate: '',
    endDate: '',
    description: '',
    username: user.username,
    name: user.name
  });

  const isAdmin = user.role === 'ADMIN' || user.role === 'SUPERADMIN';

  useEffect(() => {
    fetchLogs();
    if (isAdmin) fetchWorkers();
  }, [user]);

  // Recargar logs al cambiar a pestaña Calendario
  useEffect(() => {
    if (activeTab === 'Calendario') {
      fetchLogs();
    }
  }, [activeTab]);

  const fetchWorkers = async () => {
    try {
      const res = await fetch(`${API_BASE}/workers`);
      const data = await res.json();
      setWorkers(data);
    } catch (e) { console.error(e); }
  };

  // Función para detectar conflictos de equipo en un rango de fechas
  const checkTeamConflicts = (username, startDate, endDate, excludeId = null) => {
    const userTeam = teams[username];
    console.log('🔵 DEBUG checkTeamConflicts:', { username, userTeam, startDate, endDate, totalTeams: Object.keys(teams).length });
    if (!userTeam || !userTeam.team) {
      console.log('⚠️ DEBUG: Usuario sin equipo o teams vacío');
      return [];
    }

    // Convertir fechas DD/MM/YYYY a Date
    const parseDate = (dateStr) => {
      const [day, month, year] = dateStr.split('/');
      return new Date(year, month - 1, day);
    };

    const start = parseDate(startDate);
    const end = parseDate(endDate);
    
    const conflicts = logs.filter(log => {
      if (excludeId && log.ID === excludeId) return false; // Excluir la solicitud actual al editar
      if (log.USUARIO === username) return false; // No comparar consigo mismo
      const estado = log.ESTADO?.trim().toUpperCase() || '';
      if (estado !== 'APROBADA') return false; // Solo ausencias aprobadas
      // Incluir VACACIONES y FESTIVOS
      const logType = log.TIPO?.trim().toUpperCase() || '';
      if (logType !== 'VACACIONES' && logType !== 'FESTIVO') return false;
      
      const otherUserTeam = teams[log.USUARIO];
      if (!otherUserTeam || !otherUserTeam.team) return false; // Mismo equipo
      if (otherUserTeam.team !== userTeam.team) return false; // Mismo equipo

      const logStart = parseDate(log["FECHA INICIO"]);
      const logEnd = parseDate(log["FECHA FIN"]);

      // Verificar si hay solapamiento de fechas
      return (start <= logEnd && end >= logStart);
    });

    console.log('🔍 DEBUG: Conflictos encontrados:', conflicts.length, conflicts);
    return conflicts;
  };

  // Contar días consumidos por tipo y año
  const getConsumedByYearAndType = (username, type) => {
    const parseDate = (dateStr) => {
      const [day, month, year] = dateStr.split('/');
      return new Date(year, month - 1, day);
    };

    const consumed = {};
    
    logs.filter(log => {
      if (log.USUARIO !== username) return false;
      const estado = log.ESTADO?.trim().toUpperCase() || '';
      if (estado !== 'APROBADA') return false;
      const logType = log.TIPO?.trim().toUpperCase() || '';
      return logType === type.toUpperCase();
    }).forEach(log => {
      const start = parseDate(log["FECHA INICIO"]);
      const end = parseDate(log["FECHA FIN"]);
      const diffTime = Math.abs(end - start);
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
      
      const year = start.getFullYear().toString();
      consumed[year] = (consumed[year] || 0) + diffDays;
    });
    
    return consumed;
  };

  const handleApprove = async (id, status) => {
    if ((status === 'Rechazada' || status === 'Pendiente') && !reason.trim()) {
      alert("Es obligatorio indicar un motivo para rechazar o mantener en pendiente.");
      return;
    }

    // Verificar conflictos de equipo al aprobar
    if (status === 'Aprobada') {
      const request = logs.find(log => log.ID === id);
      if (request && request.TIPO?.toUpperCase() === 'VACACIONES') {
        const conflicts = checkTeamConflicts(request.USUARIO, request["FECHA INICIO"], request["FECHA FIN"], id);
        if (conflicts.length > 0) {
          const conflictNames = conflicts.map(c => `${c["NOMBRE COMPLETO"]} (${c["FECHA INICIO"]} - ${c["FECHA FIN"]})`).join('\n');
          const userTeam = teams[request.USUARIO];
          const proceed = window.confirm(`⚠️ ADVERTENCIA: Ya hay ${conflicts.length} persona(s) del equipo "${userTeam?.team}" de vacaciones en esas fechas:\n\n${conflictNames}\n\n¿Deseas aprobar igualmente?`);
          if (!proceed) return;
        }
      }
    }

    if (!window.confirm(`¿Seguro que quieres poner esta solicitud como ${status.toLowerCase()}?`)) return;
    try {
      const res = await fetch(`${API_BASE}/absences/approve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ requestId: id, status, comments: reason || "Procesado desde Web" })
      });
      if (res.ok) {
        setReason('');
        fetchLogs();
        setSelectedRequests(new Set());
      }
    } catch (e) { console.error(e); }
  };


  const toggleSelectRequest = (id) => {
    const newSelected = new Set(selectedRequests);
    if (newSelected.has(id)) {
      newSelected.delete(id);
    } else {
      newSelected.add(id);
    }
    setSelectedRequests(newSelected);
  };

  const selectAllRequests = (ids) => {
    setSelectedRequests(new Set(ids));
  };

  const clearSelection = () => {
    setSelectedRequests(new Set());
  };

  const handleMassApprove = async () => {
    if (selectedRequests.size === 0) return;
    if (!window.confirm(`¿Seguro que quieres aprobar ${selectedRequests.size} solicitud(es)?`)) return;

    try {
      for (const id of selectedRequests) {
        await fetch(`${API_BASE}/absences/approve`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ requestId: id, status: 'Aprobada', comments: "Aprobado masivamente desde Web" })
        });
      }
      setReason('');
      fetchLogs();
      clearSelection();
    } catch (e) { console.error(e); }
  };

  const handleMassReject = async () => {
    if (selectedRequests.size === 0) return;
    const motivo = prompt(`Motivo para rechazar ${selectedRequests.size} solicitud(es):`);
    if (!motivo || !motivo.trim()) return;
    if (!window.confirm(`¿Seguro que quieres rechazar ${selectedRequests.size} solicitud(es)?`)) return;

    try {
      for (const id of selectedRequests) {
        await fetch(`${API_BASE}/absences/approve`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ requestId: id, status: 'Rechazada', comments: motivo })
        });
      }
      setReason('');
      fetchLogs();
      clearSelection();
    } catch (e) { console.error(e); }
  };

  // Auto-actualizar comentario cuando hay conflictos
  const [conflictComment, setConflictComment] = React.useState('');
  
  React.useEffect(() => {
    if (newRequest.type === 'VACACIONES' && newRequest.startDate && newRequest.endDate) {
      const formatDate = (dateStr) => {
        const [year, month, day] = dateStr.split('-');
        return `${day}/${month}/${year}`;
      };
      
      const conflicts = checkTeamConflicts(
        newRequest.username,
        formatDate(newRequest.startDate),
        formatDate(newRequest.endDate),
        isEditing ? editRequest?.ID : null
      );
      
      if (conflicts.length > 0) {
        const userTeam = teams[newRequest.username];
        const teamLabel = userTeam?.roleLabel || userTeam?.team || '';
        
        // Calcular días de la solicitud
        const start = new Date(newRequest.startDate);
        const end = new Date(newRequest.endDate);
        const diffTime = Math.abs(end - start);
        const totalDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
        
        // Para cada conflicto, contar cuántos días coinciden
        let overlapCount = 0;
        for (const conflict of conflicts) {
          const conflictStart = conflict["FECHA INICIO"];
          const conflictEnd = conflict["FECHA FIN"];
          
          // Parsear fechas DD/MM/YYYY a Date objects
          const parseDate = (dateStr) => {
            const [day, month, year] = dateStr.split('/');
            return new Date(year, month - 1, day);
          };
          
          const cStart = parseDate(conflictStart);
          const cEnd = parseDate(conflictEnd);
          
          // Encontrar overlaps entre [start, end] y [cStart, cEnd]
          const overlapStart = new Date(Math.max(start.getTime(), cStart.getTime()));
          const overlapEnd = new Date(Math.min(end.getTime(), cEnd.getTime()));
          
          if (overlapStart <= overlapEnd) {
            const overlapDiff = Math.abs(overlapEnd - overlapStart);
            const overlapDays = Math.ceil(overlapDiff / (1000 * 60 * 60 * 24)) + 1;
            overlapCount = Math.max(overlapCount, overlapDays);
          }
        }
        
        // Determinar si es TOTAL o PARCIAL
        const isTotal = overlapCount >= totalDays;
        const conflictNames = conflicts.map(c => c["NOMBRE COMPLETO"]).join(', ');
        
        let shortComment = '';
        if (isTotal) {
          shortComment = `Coincidencia TOTAL del equipo ${teamLabel} con ${conflictNames}`;
        } else {
          shortComment = `Coincidencia PARCIAL (${overlapCount} coincidente${overlapCount !== 1 ? 's' : ''} / ${totalDays} días) del equipo ${teamLabel} con ${conflictNames}`;
        }
        
        setConflictComment(shortComment);
      } else {
        setConflictComment('');
      }
    } else {
      setConflictComment('');
    }
  }, [newRequest.startDate, newRequest.endDate, newRequest.type, newRequest.username, teams, logs, isEditing, editRequest]);

  const handleSubmit = async (e) => {
    if (e) e.preventDefault();

    setIsLoading(true);
    // Cerrar modal inmediatamente
    setShowModal(false);
    
    try {
      const url = isEditing ? `${API_BASE}/absences/update` : `${API_BASE}/absences/request`;
      const body = isEditing
        ? { ...newRequest, requestId: editRequest.ID, note: newRequest.editNote || `Modificado el ${new Date().toLocaleDateString()}` }
        : { ...newRequest, status: isAdmin ? 'Aprobada' : 'Pendiente', comments: conflictComment || '' };

      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      if (res.ok) {
        setIsEditing(false);
        setEditRequest(null);
        fetchLogs();
        alert(isEditing ? "✅ Solicitud actualizada (vuelve a estado Pendiente)" : "✅ Solicitud enviada correctamente");
      }
    } catch (e) { console.error(e); }
    finally { setIsLoading(false); }
  };

  const startEdit = (log) => {
    setIsEditing(true);
    setEditRequest(log);
    // Parse dates if possible (assuming format "DD/MM/YYYY")
    const parseDate = (d) => {
      const parts = d.split('/');
      return `${parts[2]}-${parts[1]}-${parts[0]}`;
    };
    setNewRequest({
      type: log.TIPO,
      startDate: parseDate(log["FECHA INICIO"]),
      endDate: parseDate(log["FECHA FIN"]),
      description: log["DESCRIPCIÓN"] || ""
    });
    setShowModal(true);
  };

  const myLogs = logs.filter(l => l.USUARIO === user.username);
  const allLogs = logs;

  const currentLogs = activeTab === 'Mis solicitudes' ? myLogs : allLogs;
  const filteredLogs = currentLogs.filter(l => l.ESTADO === activeSubTab);

  // Ordenar por fecha de inicio y luego por timestamp de solicitud
  const sortedLogs = [...filteredLogs].sort((a, b) => {
    // Convertir fechas DD/MM/YYYY a Date objects
    const parseDate = (dateStr) => {
      const [day, month, year] = dateStr.split('/');
      return new Date(year, month - 1, day);
    };
    
    const dateA = parseDate(a["FECHA INICIO"]);
    const dateB = parseDate(b["FECHA INICIO"]);
    
    if (dateA.getTime() !== dateB.getTime()) {
      return dateA - dateB;
    }
    
    // Si las fechas son iguales, ordenar por timestamp de solicitud
    const parseTimestamp = (dateStr) => {
      const [datePart, timePart] = dateStr.split(' ');
      const [day, month, year] = datePart.split('/');
      const [hour, minute] = (timePart || '00:00').split(':');
      return new Date(year, month - 1, day, hour, minute);
    };
    
    const timestampA = parseTimestamp(a["FECHA SOLICITUD"] || '01/01/2000 00:00');
    const timestampB = parseTimestamp(b["FECHA SOLICITUD"] || '01/01/2000 00:00');
    
    return timestampA - timestampB;
  });

  // Separar en ausencias de un día y de múltiples días
  const oneDayLogs = sortedLogs.filter(log => log["DIA NAT"] === 1 || log["DIA NAT"] === '1');
  const multiDayLogs = sortedLogs.filter(log => log["DIA NAT"] !== 1 && log["DIA NAT"] !== '1');

  const tabs = isAdmin ? ['Calendario', 'Mis solicitudes', 'Solicitudes'] : ['Calendario', 'Mis solicitudes'];
  const subTabs = [
    { name: 'Pendiente', color: 'bg-orange-100 text-orange-700', active: 'bg-orange-500 text-white' },
    { name: 'Aprobada', color: 'bg-emerald-100 text-emerald-700', active: 'bg-emerald-500 text-white' },
    { name: 'Rechazada', color: 'bg-red-100 text-red-700', active: 'bg-red-500 text-white' }
  ];

  return (
    <div className="animate-in fade-in duration-700 rounded-none">
      <header className="flex flex-col md:flex-row justify-between items-start md:items-center p-12 border-b border-blue-900 gap-10 rounded-none" style={{ backgroundColor: '#1e40af' }}>
        <div className="space-y-8 rounded-none">
          <h2 className="text-4xl font-black text-white tracking-tighter uppercase rounded-none">Gestión de Ausencias</h2>
          <div className="flex items-center gap-14 p-0 bg-transparent w-fit rounded-none h-12">
            {tabs.map(t => (
              <button key={t} onClick={() => setActiveTab(t)}
                className={`h-full px-0 pt-2 pb-4 text-[11px] font-black uppercase tracking-[0.3em] transition-all relative rounded-none ${activeTab === t ? 'text-white' : 'text-blue-200/40 hover:text-white'}`}>
                {t}
                {activeTab === t && <motion.div layoutId="activeTabUnderline" className="absolute bottom-0 left-0 right-0 h-1.5 bg-white rounded-none" />}
              </button>
            ))}
          </div>
        </div>
        <div className="flex items-center gap-4">
          {isAdmin && (
            <button onClick={() => setShowReloadModal(true)}
              className="bg-purple-600 text-white px-8 py-4 rounded-lg font-bold flex items-center space-x-3 shadow-lg hover:bg-purple-700 active:scale-95 transition-all text-sm uppercase tracking-wider">
              <RefreshCw size={20} /><span>Recargar Cuadrantes</span>
            </button>
          )}
          <button onClick={() => { 
            setIsEditing(false); 
            const initialRequest = { type: 'VACACIONES', startDate: '', endDate: '', description: '', editNote: '', username: user.username, name: user.name };
            console.log('🟣 DEBUG: Abriendo modal nueva solicitud:', initialRequest);
            setNewRequest(initialRequest); 
            setShowModal(true); 
          }}
            className="bg-white text-blue-900 px-12 py-6 rounded-none font-black flex items-center space-x-5 shadow-2xl hover:bg-slate-50 active:scale-95 transition-all text-xs tracking-[0.2em] uppercase">
            <Plus size={24} /><span className="border-l border-blue-100 pl-5">NUEVA SOLICITUD</span>
          </button>
        </div>
      </header>

      <div className="p-0 sm:p-2 lg:p-4 rounded-none">
        {activeTab === 'Calendario' ? (
          <CalendarView logs={logs} teams={teams} festivos={festivos} />
        ) : (
          <div className="bg-white border border-slate-200 shadow-2xl overflow-hidden min-h-[600px] rounded-none mt-2">
            <div className="pt-8 pb-5 px-12 border-b flex flex-col md:flex-row justify-between items-start md:items-center bg-white gap-10 rounded-none">
              <div className="flex items-center gap-16 p-0 bg-transparent rounded-none h-8">
                {subTabs.map(st => (
                  <button key={st.name} onClick={() => setActiveSubTab(st.name)}
                    className={`h-full px-0 pt-1 pb-4 text-[11px] font-black uppercase tracking-[0.3em] transition-all relative rounded-none ${activeSubTab === st.name ? st.name === 'Pendiente' ? 'text-orange-500' : st.name === 'Aprobada' ? 'text-emerald-500' : 'text-red-500' : 'text-slate-300 hover:text-slate-600'}`}>
                    {st.name}
                    {activeSubTab === st.name && <motion.div layoutId="activeSubTabUnderline" className={`absolute bottom-0 left-0 right-0 h-1.5 rounded-none ${st.name === 'Pendiente' ? 'bg-orange-500' : st.name === 'Aprobada' ? 'bg-emerald-500' : 'bg-red-500'}`} />}
                  </button>
                ))}
              </div>
              <div className="flex items-center space-x-4 px-8 py-3 bg-slate-50 border border-slate-100 rounded-none">
                <span className="w-3 h-3 rounded-none bg-blue-500 animate-pulse"></span>
                <p className="text-[11px] font-black text-slate-500 uppercase tracking-widest">{sortedLogs.length} Registros encontrados</p>
              </div>

              {activeTab === 'Solicitudes' && selectedRequests.size > 0 && (
                <div className="px-8 py-4 bg-blue-50 border border-blue-200 flex items-center justify-between">
                  <p className="text-[11px] font-black text-blue-700 uppercase tracking-widest">{selectedRequests.size} solicitud(es) seleccionada(s)</p>
                  <div className="flex items-center space-x-3">
                    <button onClick={handleMassApprove} className="px-6 py-2 bg-emerald-500 text-white text-xs font-black uppercase tracking-wider rounded-lg hover:bg-emerald-600 transition-all flex items-center space-x-2">
                      <Check size={16} />
                      <span>Aprobar ({selectedRequests.size})</span>
                    </button>
                    <button onClick={handleMassReject} className="px-6 py-2 bg-red-500 text-white text-xs font-black uppercase tracking-wider rounded-lg hover:bg-red-600 transition-all flex items-center space-x-2">
                      <X size={16} />
                      <span>Rechazar ({selectedRequests.size})</span>
                    </button>
                    <button onClick={clearSelection} className="px-6 py-2 bg-slate-300 text-slate-700 text-xs font-black uppercase tracking-wider rounded-lg hover:bg-slate-400 transition-all">
                      Limpiar
                    </button>
                  </div>
                </div>
              )}
            </div>

            <div className="overflow-x-auto p-4">
              <table className="w-full text-left">
                <thead className="bg-slate-50/50 text-slate-400 uppercase text-[10px] font-black tracking-widest">
                  <tr>
                    {activeTab === 'Solicitudes' && <th className="px-4 py-4 w-12 text-center"></th>}
                    <th className="px-4 py-4 w-1/2">Empleado</th>
                    <th className="px-6 py-4 w-3/20">Tipo / Razón</th>
                    <th className="px-6 py-4 w-1/5 text-center">Periodo</th>
                    <th className="px-6 py-4 w-3/20 text-center">Duración</th>
                    <th className="px-6 py-4 text-right">Acciones</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {isLoading ? (
                    <tr><td colSpan={activeTab === 'Solicitudes' ? 6 : 5} className="p-20 text-center"><Loader2 className="animate-spin mx-auto text-slate-300" size={40} /></td></tr>
                  ) : sortedLogs.length === 0 ? (
                    <tr><td colSpan={activeTab === 'Solicitudes' ? 6 : 5} className="p-20 text-center text-slate-400 font-bold uppercase tracking-widest text-xs italic">No hay registros en esta sección</td></tr>
                  ) : (
                    <>
                      {/* SECCIÓN: Ausencias de un solo día */}
                      {oneDayLogs.length > 0 && (
                        <>
                          <tr className="bg-blue-50/30">
                            <td colSpan={activeTab === 'Solicitudes' ? 6 : 5} className="px-6 py-3">
                              <p className="text-[10px] font-black text-blue-600 uppercase tracking-[0.2em]">🔹 Ausencias de un día ({oneDayLogs.length})</p>
                            </td>
                          </tr>
                          {oneDayLogs.map((log) => (
                            <tr key={log.ID} className={`group transition-colors ${selectedRequests.has(log.ID) ? 'bg-blue-100' : 'hover:bg-slate-50/50'}`}>
                              {activeTab === 'Solicitudes' && (
                                <td className="px-4 py-5 text-center w-12">
                                  <input type="checkbox" checked={selectedRequests.has(log.ID)} onChange={() => toggleSelectRequest(log.ID)} className="w-4 h-4 cursor-pointer" />
                                </td>
                              )}
                              <td className="px-4 py-5 w-1/2">
                                {(() => {
                                  const userTeamData = teams[log.USUARIO];
                                  console.log('🟡 DEBUG Badge RENDER:', { 
                                    usuario: log.USUARIO, 
                                    team: userTeamData?.team, 
                                    totalTeams: Object.keys(teams).length,
                                    userTeamData: userTeamData,
                                    teamsKeys: Object.keys(teams).slice(0, 5)
                                  });
                                  const teamColors = {
                                    'leader': 'bg-yellow-400 text-yellow-900 border border-yellow-600',
                                    'inspection': 'bg-green-400 text-green-900 border border-green-600',
                                    'repair': 'bg-red-400 text-red-900 border border-red-600',
                                    'iot': 'bg-blue-400 text-blue-900 border border-blue-600'
                                  };
                                  const teamColor = teamColors[userTeamData?.team] || 'bg-slate-200 text-slate-900 border border-slate-400';
                                  
                                  const teamLabel = userTeamData?.team === 'leader' ? 'Técnico' :
                                                   userTeamData?.team === 'inspection' ? 'Inspector' :
                                                   userTeamData?.team === 'repair' ? 'Reparadores' :
                                                   userTeamData?.team === 'iot' ? 'IoT' : 
                                                   userTeamData?.team || 'Sin equipo';
                                  
                                  return (
                                    <div className="flex items-center space-x-3">
                                      <div className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center font-bold text-slate-500 text-xs">
                                        {log.USUARIO?.slice(0, 1).toUpperCase()}
                                      </div>
                                      <div>
                                        <p className="font-bold text-slate-800 text-sm">{log["NOMBRE COMPLETO"]}</p>
                                        <div className="flex items-center space-x-2 mt-1">
                                          <p className="text-[10px] text-slate-400 font-medium uppercase tracking-widest">{log.USUARIO}</p>
                                          <span className={`text-[8px] font-black uppercase tracking-widest px-2 py-1 rounded-md ${teamColor} inline-block`}>
                                            {teamLabel}
                                          </span>
                                        </div>
                                      </div>
                                    </div>
                                  );
                                })()}
                              </td>
                              <td className="px-6 py-5 w-3/20">
                                <span className={`inline-block px-2.5 py-1 rounded-lg text-[10px] font-black uppercase tracking-widest mb-1 ${log.TIPO?.toString().toUpperCase().trim() === 'VACACIONES' ? 'bg-blue-50 text-blue-600' : 'bg-purple-50 text-purple-600'}`}>
                                  {log.TIPO}
                                </span>
                                <p className="text-xs text-slate-500 line-clamp-1 italic">{log["DESCRIPCIÓN"] || "Sin motivo detallado"}</p>
                              </td>
                              <td className="px-6 py-5 w-1/5">
                                <div className="flex flex-col items-center">
                                  <span className="text-sm font-bold text-slate-700">{formatDateWithZeros(log["FECHA INICIO"])}</span>
                                </div>
                              </td>
                              <td className="px-6 py-5 w-3/20 text-center">
                                <p className="text-sm font-black text-slate-700">{log["DIA NAT"]} día</p>
                              </td>
                              <td className="px-6 py-5 text-right">
                                {activeTab === 'Solicitudes' && isAdmin && log.ESTADO === 'Pendiente' && selectedRequests.size === 0 ? (
                                  <div className="flex flex-col space-y-2">
                                    <input type="text" placeholder="Motivo (Rechazo/Pend)" value={reason} onChange={e => setReason(e.target.value)}
                                      className="text-[10px] p-2 border border-slate-100 rounded-lg focus:outline-none focus:border-blue-300" />
                                    <div className="flex justify-end space-x-2">
                                      <button onClick={() => handleApprove(log.ID, 'Aprobada')} className="p-2 bg-emerald-50 text-emerald-600 rounded-xl hover:bg-emerald-500 hover:text-white transition-all shadow-sm">
                                        <Check size={16} />
                                      </button>
                                      <button onClick={() => handleApprove(log.ID, 'Rechazada')} className="p-2 bg-red-50 text-red-600 rounded-xl hover:bg-red-500 hover:text-white transition-all shadow-sm">
                                        <X size={16} />
                                      </button>
                                    </div>
                                  </div>
                                ) : activeTab === 'Mis solicitudes' ? (
                                  <div className="flex justify-end space-x-2">
                                    <button onClick={() => startEdit(log)} className="p-2 text-slate-400 hover:text-blue-500 hover:bg-blue-50 rounded-lg transition-all">
                                      <Edit2 size={16} />
                                    </button>
                                    <button className="p-2 text-slate-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all opacity-0 group-hover:opacity-100">
                                      <Trash2 size={16} />
                                    </button>
                                  </div>
                                ) : (
                                  <StatusBadge status={log.ESTADO} />
                                )}
                              </td>
                            </tr>
                          ))}
                        </>
                      )}

                      {/* SECCIÓN: Ausencias de múltiples días */}
                      {multiDayLogs.length > 0 && (
                        <>
                          {oneDayLogs.length > 0 && (
                            <tr className="bg-slate-100">
                              <td colSpan={activeTab === 'Solicitudes' ? 6 : 5} className="py-1"></td>
                            </tr>
                          )}
                          <tr className="bg-purple-50/30">
                            <td colSpan={activeTab === 'Solicitudes' ? 6 : 5} className="px-6 py-3">
                              <p className="text-[10px] font-black text-purple-600 uppercase tracking-[0.2em]">📅 Periodos de vacaciones ({multiDayLogs.length})</p>
                            </td>
                          </tr>
                          {multiDayLogs.map((log) => (
                            <tr key={log.ID} className={`group transition-colors ${selectedRequests.has(log.ID) ? 'bg-blue-100' : 'hover:bg-slate-50/50'}`}>
                              {activeTab === 'Solicitudes' && (
                                <td className="px-4 py-5 text-center w-12">
                                  <input type="checkbox" checked={selectedRequests.has(log.ID)} onChange={() => toggleSelectRequest(log.ID)} className="w-4 h-4 cursor-pointer" />
                                </td>
                              )}
                        <td className="px-4 py-5 w-2/5">
                          {(() => {
                            const userTeamData = teams[log.USUARIO];
                            const teamColors = {
                              'leader': 'bg-yellow-100 text-yellow-900',
                              'inspection': 'bg-green-100 text-green-900',
                              'repair': 'bg-red-100 text-red-900',
                              'iot': 'bg-blue-100 text-blue-900'
                            };
                            const teamColor = teamColors[userTeamData?.team] || 'bg-slate-100 text-slate-900';
                            return (
                              <div className="flex items-center space-x-3">
                                <div className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center font-bold text-slate-500 text-xs">
                                  {log.USUARIO?.slice(0, 1).toUpperCase()}
                                </div>
                                <div>
                                  <p className="font-bold text-slate-800 text-sm">{log["NOMBRE COMPLETO"]}</p>
                                  <div className="flex items-center space-x-2 mt-1">
                                    <p className="text-[10px] text-slate-400 font-medium uppercase tracking-widest">{log.USUARIO}</p>
                                    {userTeamData && (
                                      <span className={`text-[8px] font-black uppercase tracking-widest px-2 py-1 rounded-md ${teamColor}`}>
                                        {userTeamData.team === 'leader' ? 'Técnico' :
                                         userTeamData.team === 'inspection' ? 'Inspector' :
                                         userTeamData.team === 'repair' ? 'Reparadores' :
                                         userTeamData.team === 'iot' ? 'IoT' : userTeamData.team}
                                      </span>
                                    )}
                                  </div>
                                </div>
                              </div>
                            );
                          })()}
                        </td>
                        <td className="px-6 py-5 w-3/20">
                          <span className={`inline-block px-2.5 py-1 rounded-lg text-[10px] font-black uppercase tracking-widest mb-1 ${log.TIPO?.toString().toUpperCase().trim() === 'VACACIONES' ? 'bg-blue-50 text-blue-600' : 'bg-purple-50 text-purple-600'}`}>
                            {log.TIPO}
                          </span>
                          <p className="text-xs text-slate-500 line-clamp-1 italic">{log["DESCRIPCIÓN"] || "Sin motivo detallado"}</p>
                        </td>
                        <td className="px-6 py-5 w-3/10">
                          <div className="flex flex-col items-center">
                            <span className="text-xs font-bold text-slate-700">{formatDateWithZeros(log["FECHA INICIO"])}</span>
                            <span className="text-[10px] text-slate-400">— al —</span>
                            <span className="text-xs font-bold text-slate-700">{formatDateWithZeros(log["FECHA FIN"])}</span>
                          </div>
                        </td>
                        <td className="px-6 py-5 w-3/20 text-center">
                          <p className="text-sm font-black text-slate-700">{parseInt(log["DIA LAB"]) === parseInt(log["DIA NAT"]) ? `${log["DIA NAT"]} días` : `${log["DIA LAB"]} / ${log["DIA NAT"]}`}</p>
                        </td>
                        <td className="px-6 py-5 text-right">
                          {activeTab === 'Solicitudes' && isAdmin && log.ESTADO === 'Pendiente' && selectedRequests.size === 0 ? (
                            <div className="flex flex-col space-y-2">
                              <input type="text" placeholder="Motivo (Rechazo/Pend)" value={reason} onChange={e => setReason(e.target.value)}
                                className="text-[10px] p-2 border border-slate-100 rounded-lg focus:outline-none focus:border-blue-300" />
                              <div className="flex justify-end space-x-2">
                                <button onClick={() => handleApprove(log.ID, 'Aprobada')} className="p-2 bg-emerald-50 text-emerald-600 rounded-xl hover:bg-emerald-500 hover:text-white transition-all shadow-sm">
                                  <Check size={16} />
                                </button>
                                <button onClick={() => handleApprove(log.ID, 'Rechazada')} className="p-2 bg-red-50 text-red-600 rounded-xl hover:bg-red-500 hover:text-white transition-all shadow-sm">
                                  <X size={16} />
                                </button>
                              </div>
                            </div>
                          ) : activeTab === 'Mis solicitudes' ? (
                            <div className="flex justify-end space-x-2">
                              <button onClick={() => startEdit(log)} className="p-2 text-slate-400 hover:text-blue-500 hover:bg-blue-50 rounded-lg transition-all">
                                <Edit2 size={16} />
                              </button>
                              <button className="p-2 text-slate-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all opacity-0 group-hover:opacity-100">
                                <Trash2 size={16} />
                              </button>
                            </div>
                          ) : (
                            <StatusBadge status={log.ESTADO} />
                          )}
                        </td>
                      </tr>
                          ))}
                        </>
                      )}
                    </>
                  )
                  }
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
      <AnimatePresence>
        {showReloadModal && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-[400] flex items-center justify-center bg-black/40 backdrop-blur-[2px] p-4">
            
            <motion.div
              initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 10 }}
              className="bg-white shadow-[0_32px_64px_-12px_rgba(0,0,0,0.14)] w-full max-w-6xl rounded-2xl overflow-hidden flex flex-col border border-slate-200"
            >
              {/* HEADER */}
              <div className="px-10 pt-10 pb-8 flex justify-between items-center text-white bg-gradient-to-r from-purple-600 to-purple-700">
                <div className="flex items-center space-x-4">
                  <div className="w-16 h-16 bg-white/20 rounded-xl flex items-center justify-center">
                    <RefreshCw size={32} />
                  </div>
                  <h2 className="text-3xl font-bold tracking-tight">Recargar Cuadrantes</h2>
                </div>
                <button onClick={() => setShowReloadModal(false)} className="w-12 h-12 rounded-xl bg-white/10 flex items-center justify-center text-white hover:bg-white/20 transition-all">
                  <X size={24} />
                </button>
              </div>
              
              <div className="px-10 py-10 space-y-10">
                {/* ORIGEN */}
                <div className="space-y-5">
                  <div className="flex items-center space-x-3 mb-6">
                    <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                      <span className="text-2xl">📥</span>
                    </div>
                    <h3 className="text-2xl font-bold text-slate-800">Origen</h3>
                  </div>
                  
                  {/* Fila 1: Libro y Hoja */}
                  <div className="grid grid-cols-2 gap-6">
                    <div className="space-y-2">
                      <label className="text-sm font-semibold text-slate-600 uppercase tracking-wide">📚 Libro</label>
                      <select 
                        value={reloadConfig.sourceBook}
                        onChange={e => setReloadConfig({ ...reloadConfig, sourceBook: e.target.value, sourceSheet: '', startDate: '', endDate: '' })}
                        className="w-full px-5 py-3.5 border-2 border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white text-slate-900 font-medium text-base"
                      >
                        <option value="">Selecciona libro...</option>
                        {availableBooks.map(book => (
                          <option key={book.id} value={book.id}>{book.name}</option>
                        ))}
                      </select>
                    </div>
                    
                    <div className="space-y-2">
                      <label className="text-sm font-semibold text-slate-600 uppercase tracking-wide">📊 Hoja</label>
                      <select 
                        value={reloadConfig.sourceSheet}
                        onChange={e => setReloadConfig({ ...reloadConfig, sourceSheet: e.target.value })}
                        disabled={!reloadConfig.sourceBook}
                        className="w-full px-5 py-3.5 border-2 border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white text-slate-900 font-medium text-base disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <option value="">{reloadConfig.sourceBook ? 'Selecciona hoja...' : 'Selecciona libro primero...'}</option>
                        {sourceSheets.map(sheet => (
                          <option key={sheet} value={sheet}>{sheet}</option>
                        ))}
                      </select>
                    </div>
                  </div>
                  
                  {/* Fila 2: Fechas */}
                  {sourceDates.firstDate && (
                    <div className="grid grid-cols-2 gap-6">
                      <div className="space-y-2">
                        <label className="text-sm font-semibold text-slate-600 uppercase tracking-wide">📅 Fecha Inicio</label>
                        <input
                          type="text"
                          placeholder={sourceDates.firstDate}
                          value={reloadConfig.startDate}
                          onChange={e => setReloadConfig({ ...reloadConfig, startDate: e.target.value })}
                          className="w-full px-5 py-3.5 border-2 border-slate-200 rounded-xl text-base focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        />
                        <p className="text-xs text-slate-500">Detectado: <span className="font-semibold text-blue-600">{sourceDates.firstDate}</span></p>
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-semibold text-slate-600 uppercase tracking-wide">📅 Fecha Fin</label>
                        <input
                          type="text"
                          placeholder={sourceDates.lastDate}
                          value={reloadConfig.endDate}
                          onChange={e => setReloadConfig({ ...reloadConfig, endDate: e.target.value })}
                          className="w-full px-5 py-3.5 border-2 border-slate-200 rounded-xl text-base focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        />
                        <p className="text-xs text-slate-500">Detectado: <span className="font-semibold text-blue-600">{sourceDates.lastDate}</span></p>
                      </div>
                    </div>
                  )}
                </div>

                {/* SEPARADOR */}
                <div className="border-t-2 border-slate-200"></div>

                {/* DESTINO */}
                <div className="space-y-5">
                  <div className="flex items-center space-x-3 mb-6">
                    <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                      <span className="text-2xl">📤</span>
                    </div>
                    <h3 className="text-2xl font-bold text-slate-800">Destino</h3>
                  </div>
                  
                  {/* Fila 1: Libro y Hoja */}
                  <div className="grid grid-cols-2 gap-6">
                    <div className="space-y-2">
                      <label className="text-sm font-semibold text-slate-600 uppercase tracking-wide">📚 Libro</label>
                      <select 
                        value={reloadConfig.destBook}
                        onChange={e => setReloadConfig({ ...reloadConfig, destBook: e.target.value, destSheet: '' })}
                        className="w-full px-5 py-3.5 border-2 border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent bg-white text-slate-900 font-medium text-base"
                      >
                        <option value="">Selecciona libro...</option>
                        {availableBooks.map(book => (
                          <option key={book.id} value={book.id}>{book.name}</option>
                        ))}
                      </select>
                    </div>
                    
                    <div className="space-y-2">
                      <label className="text-sm font-semibold text-slate-600 uppercase tracking-wide">✓ Hoja</label>
                      <select 
                        value={reloadConfig.destSheet}
                        onChange={e => setReloadConfig({ ...reloadConfig, destSheet: e.target.value })}
                        disabled={!reloadConfig.destBook}
                        className="w-full px-5 py-3.5 border-2 border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent bg-white text-slate-900 font-medium text-base disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <option value="">{reloadConfig.destBook ? 'Selecciona hoja...' : 'Selecciona libro primero...'}</option>
                        {destSheets.map(sheet => (
                          <option key={sheet} value={sheet}>{sheet}</option>
                        ))}
                      </select>
                    </div>
                  </div>
                  
                  {/* Info última vacación */}
                  {destDates.lastVacationDate && (
                    <div className="bg-green-50 border-2 border-green-200 rounded-xl p-5">
                      <p className="text-sm font-semibold text-green-900">📅 Última vacación registrada:</p>
                      <p className="text-2xl font-bold text-green-700 mt-1">{destDates.lastVacationDate}</p>
                    </div>
                  )}
                </div>

                {/* Botones */}
                <div className="flex gap-4 pt-6">
                  <button 
                    onClick={() => setShowReloadModal(false)}
                    className="flex-1 py-4 rounded-xl bg-slate-100 text-slate-700 font-bold text-lg hover:bg-slate-200 transition-all"
                  >
                    Cancelar
                  </button>
                  <button 
                    onClick={handleReloadQuadrants}
                    disabled={reloadLoading || !reloadConfig.sourceSheet || !reloadConfig.destSheet}
                    className="flex-1 py-4 rounded-xl bg-gradient-to-r from-purple-600 to-purple-700 text-white font-bold text-lg hover:from-purple-700 hover:to-purple-800 disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center justify-center space-x-3 shadow-lg shadow-purple-500/30"
                  >
                    {reloadLoading ? (
                      <>
                        <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                        <span>Procesando...</span>
                      </>
                    ) : (
                      <>
                        <RefreshCw size={20} />
                        <span>Recargar Cuadrantes</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showModal && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-[300] flex items-center justify-center bg-black/40 backdrop-blur-[2px] p-[8px]"
            onAnimationComplete={() => console.log('🟣 MODAL RENDERIZADO COMPLETO')}
          >

            <motion.div
              initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 10 }}
              className="bg-white shadow-[0_32px_64px_-12px_rgba(0,0,0,0.14)] max-w-5xl overflow-hidden flex flex-col border border-slate-200 h-auto max-h-[95vh]"
            >
              {/* HEADER CON GRADIENTE */}
              <div className="px-12 pt-10 pb-8 flex justify-between items-center text-white bg-gradient-to-r from-blue-600 to-blue-700">
                <div className="flex items-center space-x-4">
                  <div className="w-14 h-14 bg-white/20 flex items-center justify-center">
                    <Calendar size={28} />
                  </div>
                  <h2 className="text-3xl font-bold">{isEditing ? 'Editar solicitud' : 'Solicitar ausencia'}</h2>
                </div>
                <button onClick={() => setShowModal(false)} className="w-12 h-12 bg-white/10 flex items-center justify-center text-white hover:bg-white/20 transition-all">
                  <X size={24} />
                </button>
              </div>
              <div className="flex-1 overflow-hidden flex flex-col">
                <div className="h-full overflow-y-auto overflow-x-hidden flex flex-col">
                  {/* CONTENEDOR CON PADDING PARA TODAS LAS SECCIONES */}
                  <div className="p-[8px] flex flex-col gap-[8px] border-[12px] border-transparent">
                {/* CARD DE INFORMACIÓN PRINCIPAL */}
                {/* SECCIÓN 1: SOLICITANTE */}
                {!isEditing && (
                  <div className="flex items-center justify-start border-[12px] border-transparent p-6 h-auto" style={{ backgroundColor: '#fee2e2' }}>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-5">
                        <div className="w-16 h-16 bg-blue-100 flex items_center justify-center text-blue-600">
                          <Users size={28} />
                        </div>
                        <div>
                          <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Solicitante</span>
                          {isAdmin ? (
                            <select
                              value={newRequest.username}
                              onChange={e => {
                                const w = workers.find(work => work.username === e.target.value);
                                setNewRequest({ ...newRequest, username: e.target.value, name: w ? w.name : user.name });
                              }}
                              className="bg-transparent border-none px-0 py-2 font-black text-blue-600 focus:outline-none cursor-pointer text-3xl"
                            >
                              {workers.map(w => (
                                <option key={w.username} value={w.username}>{w.name}</option>
                              ))}
                            </select>
                          ) : (
                            <p className="font-black text-slate-900 text-3xl">{user.name}</p>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                )}
                {/* SECCIÓN 2: CARDS (TIPO + CONSUMO + PENDIENTES) */}
                <div className="flex items-center justify_center border-[12px] border-transparent p-6 h-auto" style={{ backgroundColor: '#dbeafe' }}>
                  <div className="p-[5px] border-2 w-full" style={{ borderColor: '#dbeafe' }}>
                  <div className="grid grid-cols-3 gap-[8px]">
                    {/* Category */}
                    <div className="p-4 bg-white border border-gray-200">
                      <p className="text-xs font-bold text-blue-600 uppercase tracking-wider mb-2">Tipo de ausencia</p>
                      <select value={newRequest.type} onChange={e => setNewRequest({ ...newRequest, type: e.target.value })}
                        className="bg-transparent font-bold text-slate-900 focus:outline-none cursor-pointer border-none p-0 w-full"
                        style={{ fontSize: '2em' }}>
                        <option value="VACACIONES">Vacaciones</option>
                        <option value="PERMISO">Permiso Retribuido</option>
                      </select>
                    </div>

                    {/* Consumo - Tabla de tipos vs años */}
                    <div className="p-4 bg-white border border-gray-200">
                      <p className="text-xs font-bold text-blue-600 uppercase tracking-wider mb-3">Consumo</p>
                      <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                          <thead className="bg-blue-100 text-blue-800">
                            <tr>
                              <th className="text-left font-bold pb-2 pr-2">Tipo</th>
                              {(() => {
                                // Obtener todos los años disponibles de todos los tipos
                                const allYears = new Set();
                                ['VACACIONES', 'PERMISO', 'BAJA'].forEach(type => {
                                  const consumed = getConsumedByYearAndType(newRequest.username, type);
                                  Object.keys(consumed).forEach(year => allYears.add(year));
                                });
                                return Array.from(allYears).sort().map(year => (
                                  <th key={year} className="text-center font-bold pb-2 px-2 bg-blue-100">{year}</th>
                                ));
                              })()}
                            </tr>
                          </thead>
                          <tbody>
                            {['VACACIONES', 'PERMISO', 'BAJA'].map(type => {
                              const consumed = getConsumedByYearAndType(newRequest.username, type);
                              const typeLabel = type === 'VACACIONES' ? 'Vacaciones' : type === 'PERMISO' ? 'Permiso R.' : 'Baja M.';
                              return (
                                <tr key={type}>
                                  <td className="font-bold text-blue-800 pr-2 py-1 bg-blue-50">{typeLabel}</td>
                                  {(() => {
                                    const allYears = new Set();
                                    ['VACACIONES', 'PERMISO', 'BAJA'].forEach(t => {
                                      const c = getConsumedByYearAndType(newRequest.username, t);
                                      Object.keys(c).forEach(year => allYears.add(year));
                                    });
                                    return Array.from(allYears).sort().map(year => (
                                      <td key={year} className="text-center py-1 px-2">
                                        {consumed[year] ? `${consumed[year]}d` : '-'}
                                      </td>
                                    ));
                                  })()}
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      </div>
                    </div>

                    {/* Vacaciones Pendientes - Tabla de años vs estadísticas */}
                    <div className="p-4 bg-white border border-gray-200">
                      <p className="text-xs font-bold text-purple-600 uppercase tracking-wider mb-3">Vacaciones Pendientes</p>
                      <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                          <thead className="bg-purple-100 text-purple-800">
                            <tr>
                              <th className="text-left font-bold pb-2 pr-2">Año</th>
                              <th className="text-center font-bold pb-2 px-2">Consumidas</th>
                              <th className="text-center font-bold pb-2 px-2">Solicitadas</th>
                              <th className="text-center font-bold pb-2 px-2">Restantes</th>
                            </tr>
                          </thead>
                          <tbody>
                            <tr>
                              <td className="font-bold text-purple-800 pr-2 py-1 bg-purple-50">2026</td>
                              <td className="text-center py-1 px-2 text-purple-800">5</td>
                              <td className="text-center py-1 px-2 text-purple-800">0</td>
                              <td className="text-center py-1 px-2 font-bold text-purple-800">17</td>
                            </tr>
                            <tr>
                              <td className="font-bold text-purple-800 pr-2 py-1 bg-purple-50">2025</td>
                              <td className="text-center py-1 px-2 text-purple-800">10</td>
                              <td className="text-center py-1 px-2 text-purple-800">3</td>
                              <td className="text-center py-1 px-2 font-bold text-purple-800">9</td>
                            </tr>
                          </tbody>
                        </table>
                      </div>
                    </div>
                  </div>
                  </div>
                </div>

                {/* SECCIÓN 3: PERÍODO DE AUSENCIA */}
                <div className="flex items-center justify-start border-[12px] border-transparent p-6 h-auto" style={{ backgroundColor: '#fed7aa' }}>
                  <div className="p-[5px] border-2 w-full flex flex-col gap-4" style={{ borderColor: '#fed7aa' }}>
                  <label className="text-xs font-bold text-slate-600 uppercase tracking-wider block mb-4">Período de ausencia</label>
                  <div className="flex items-center space-x-4 bg-orange-50/50 border border-orange-100 p-4">
                      <div className="w-12 h-12 bg-orange-100 flex items-center justify-center text-orange-600 flex-shrink-0">
                        <Clock size={24} />
                      </div>
                      <div className="flex-1 flex items-center space-x-3">
                        <input type="date" value={newRequest.startDate} onChange={e => {
                          console.log('📅 DEBUG: Cambiando FECHA INICIO:', e.target.value);
                          setNewRequest({ ...newRequest, startDate: e.target.value });
                        }}
                          className="flex-1 bg-white border border-orange-200 px-4 py-2 font-bold text-slate-900 focus:outline-none focus:border-orange-400 focus:ring-2 focus:ring-orange-200 text-sm" />
                        <span className="text-slate-400 font-bold">—</span>
                        <input type="date" value={newRequest.endDate} onChange={e => {
                          console.log('📅 DEBUG: Cambiando FECHA FIN:', e.target.value);
                          setNewRequest({ ...newRequest, endDate: e.target.value });
                        }}
                          className="flex-1 bg-white border border-orange-200 px-4 py-2 font-bold text-slate-900 focus:outline-none focus:border-orange-400 focus:ring-2 focus:ring-orange-200 text-sm" />
                      </div>
                    </div>
                  </div>
                  </div>

                {/* SECCIÓN 4: MOTIVO/JUSTIFICACIÓN */}
                <div className="flex items-center justify-center border-[12px] border-transparent p-6 h-auto" style={{ backgroundColor: '#dcfce7' }}>
                  <div className="p-[5px] border-2 w-full flex flex-col gap-3" style={{ borderColor: '#dcfce7' }}>
                  <label className="text-xs font-bold text-slate-600 uppercase tracking-wider block">Motivo o justificación</label>
                  
                  <textarea value={newRequest.description} onChange={e => setNewRequest({ ...newRequest, description: e.target.value })}
                    className="w-full p-6 bg-white border border-slate-200 text-slate-900 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition-all min-h-[140px] resize-none text-base placeholder-slate-400"
                    placeholder="Describe los detalles de tu solicitud..." />
                  <p className="text-xs text-slate-500 text-right">
                    {newRequest.description.length} / 500 caracteres
                  </p>
                  </div>
                </div>

                {/* SECCIÓN 5: NOTA DE CAMBIO (solo en edición) */}
                {isEditing && (
                  <div className="flex items-center justify-center border-[12px] border-transparent p-6 h-auto" style={{ backgroundColor: '#cffafe' }}>
                    <div className="p-[5px] border-2 w-full flex flex-col gap-3" style={{ borderColor: '#cffafe' }}>
                    <label className="text-xs font-bold text-blue-600 uppercase tracking-wider block">Nota del cambio</label>
                    <input type="text" placeholder="Describe qué has modificado (ej: Cambio de fechas)"
                      value={newRequest.editNote || ''} onChange={e => setNewRequest({ ...newRequest, editNote: e.target.value })}
                      className="w-full px-5 py-3 bg-white border border-blue-200 text-slate-900 focus:outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100 text-base transition-all" />
                    </div>
                  </div>
                )}

                {/* SECCIÓN 6: SUPERPOSICIONES/CONFLICTOS */}
                {(() => {
                  // Calcular conflictos si hay fechas válidas
                  if (!newRequest.startDate || !newRequest.endDate) return null;
                  
                  // Convertir fechas YYYY-MM-DD a DD/MM/YYYY para checkTeamConflicts
                  const formatDate = (dateStr) => {
                    const [year, month, day] = dateStr.split('-');
                    return `${day}/${month}/${year}`;
                  };
                  
                  const conflicts = checkTeamConflicts(
                    newRequest.username,
                    formatDate(newRequest.startDate),
                    formatDate(newRequest.endDate),
                    isEditing ? editRequest.ID : null
                  );
                  
                  if (conflicts.length === 0) {
                    return (
                      <div className="flex items-center justify-center border-[12px] border-transparent p-6 h-auto" style={{ backgroundColor: '#dcfce7' }}>
                        <div className="p-[5px] border-2 w-full flex flex-col gap-3" style={{ borderColor: '#dcfce7' }}>
                        <div className="flex items-center space-x-4 mb-4">
                          <div className="w-12 h-12 bg-emerald-100 flex items-center justify-center text-emerald-600 flex-shrink-0">
                            <CheckCircle size={24} />
                          </div>
                          <h3 className="text-base font-bold text-emerald-900">Superposiciones del equipo</h3>
                        </div>
                        <p className="text-base text-emerald-700 ml-16">✓ No hay compañeros de vacaciones en estas fechas</p>
                        </div>
                      </div>
                    );
                  } else {
                    const userTeam = teams[newRequest.username];
                    const teamLabel = userTeam?.roleLabel || 
                                     (userTeam?.team === 'leader' ? 'Técnico' :
                                      userTeam?.team === 'inspection' ? 'Inspector' :
                                      userTeam?.team === 'repair' ? 'Reparadores' :
                                      userTeam?.team === 'iot' ? 'IoT' : 
                                      userTeam?.team || 'Sin equipo');
                    return (
                      <div className="flex items-center justify-center border-[12px] border-transparent p-6 h-auto" style={{ backgroundColor: '#fef3c7' }}>
                        <div className="p-[5px] border-2 w-full flex flex-col gap-3" style={{ borderColor: '#fef3c7' }}>
                        <div className="flex items-center space-x-4 mb-4">
                          <div className="w-12 h-12 bg-amber-100 flex items-center justify-center text-amber-600 flex-shrink-0">
                            <span className="text-2xl">⚠️</span>
                          </div>
                          <h3 className="text-base font-bold text-amber-900">Conflicto de equipo detectado</h3>
                        </div>
                        <p className="text-base text-amber-800 ml-16 mb-3">
                          ⚠️ Ya hay <strong>{conflicts.length} persona(s)</strong> del equipo "<strong>{teamLabel}</strong>" de vacaciones en esas fechas:
                        </p>
                        <ul className="ml-16 space-y-1">
                          {conflicts.map((c, idx) => (
                            <li key={idx} className="text-sm text-amber-700">
                              • <strong>{c["NOMBRE COMPLETO"]}</strong> ({c["FECHA INICIO"]} - {c["FECHA FIN"]})
                            </li>
                          ))}
                        </ul>
                        </div>
                      </div>
                    );
                  }
                })()}

                {/* BOTONES DE ACCIÓN */}
                {console.log('🔴🔴🔴 RENDERIZANDO BOTONES - newRequest:', newRequest, 'disabled:', isLoading || !newRequest.startDate || !newRequest.endDate || !newRequest.type)}
                <div className="flex items-center justify-end gap-[8px] pt-8 border-[12px] border-transparent p-6 h-auto" style={{ backgroundColor: '#f3e8ff' }}>
                  <div className="p-[5px] border-2 w-full flex items-center justify-end gap-[8px]" style={{ borderColor: '#f3e8ff' }}>
                  <button onClick={() => setShowModal(false)}
                    className="w-[21%] min-w-[150px] py-5 text-slate-700 font-bold text-base uppercase tracking-wider bg-slate-100 hover:bg-slate-200 transition-all border border-slate-300">
                    Cancelar
                  </button>
                  <button 
                    onClick={(e) => { 
                      console.log('🟢 DEBUG Botón Click:', { 
                        disabled: isLoading || !newRequest.startDate || !newRequest.endDate || !newRequest.type, 
                        newRequest, 
                        isLoading 
                      }); 
                      handleSubmit(e); 
                    }} 
                    disabled={isLoading || !newRequest.startDate || !newRequest.endDate || !newRequest.type}
                    style={{ 
                      backgroundColor: (isLoading || !newRequest.startDate || !newRequest.endDate || !newRequest.type) ? '#cbd5e1' : '#22c55e'
                    }}
                    className="w-[21%] min-w-[150px] py-5 text-white font-bold text-base uppercase tracking-wider shadow-lg hover:brightness-110 active:scale-[0.98] transition-all flex items-center justify-center space-x-2 disabled:cursor-not-allowed"
                  >
                    {isLoading ?
                      <div className="w-5 h-5 border-2 border-white/30 border-t-white animate-spin" /> :
                      <><span>{isEditing ? 'Actualizar' : 'Confirmar'}</span><ChevronRight size={20} /></>
                    }
                  </button>
                  </div>
                </div>
                {/* FIN CONTENEDOR CON PADDING */}
              </div>
            </div>
          </div>
        </motion.div>
      </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

const AbsenceStat = ({ label, count, color }) => (
  <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex items-center space-x-5">
    <div className={`w-12 h-12 rounded-xl ${color} text-white flex items-center justify-center font-black text-xl shadow-lg shadow-blue-100`}>
      {count}
    </div>
    <div>
      <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">{label}</p>
      <p className="text-xl font-black text-slate-800 tabular-nums">Total registros</p>
    </div>
  </div>
);

const StatusBadge = ({ status }) => {
  const styles = {
    'Pendiente': 'bg-amber-50 text-amber-600 border-amber-200',
    'Aprobada': 'bg-emerald-50 text-emerald-600 border-emerald-200',
    'Rechazada': 'bg-red-50 text-red-600 border-red-200',
    'default': 'bg-slate-50 text-slate-600 border-slate-200'
  };
  return (
    <span className={`px-4 py-1.5 rounded-none border text-[10px] font-black uppercase tracking-widest ${styles[status] || styles.default}`}>
      {status}
    </span>
  );
};

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
      className="fixed inset-0 z-[300] flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
      <motion.div initial={{ scale: 0.95, opacity: 0, y: 20 }} animate={{ scale: 1, opacity: 1, y: 0 }} exit={{ scale: 0.95, opacity: 0, y: 20 }}
        className="bg-white shadow-[0_32px_64px_-12px_rgba(0,0,0,0.14)] w-full max-w-6xl rounded-[32px] overflow-hidden flex flex-col border border-slate-100 h-[85vh]">

        {/* HEADER MODERNO */}
        <div className="px-12 pt-10 pb-6 flex justify-between items-start bg-white border-b border-slate-50">
          <div className="space-y-4">
            <h2 className="text-3xl font-bold text-slate-900">Registro de Jornada</h2>
            <div className="flex items-center space-x-8">
              {[1, 2, 3].map(s => (
                <div key={s} className={`flex items-center space-x-2 transition-all ${step === s ? 'text-blue-600' : 'text-slate-400'}`}>
                  <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold border-2 ${step === s ? 'border-blue-600 bg-blue-50' : 'border-slate-200'}`}>
                    {s}
                  </div>
                  <span className={`text-[11px] font-bold uppercase tracking-widest ${step === s ? 'opacity-100' : 'opacity-60'}`}>
                    {s === 1 ? 'Selección' : s === 2 ? 'Normal' : 'Extra'}
                  </span>
                </div>
              ))}
            </div>
          </div>
          <div className="flex items-center space-x-8">
            <div className="text-right">
              <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest leading-none mb-2">Total Jornada</p>
              <p className="text-4xl font-black text-slate-900 tabular-nums">{totalHoursDecimal.toFixed(1)}h</p>
            </div>
            <button onClick={onClose} className="w-12 h-12 rounded-full bg-slate-50 flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-all">
              <X size={24} />
            </button>
          </div>
        </div>

        <div className="px-12 py-10 flex flex-col flex-1 min-h-0 bg-white">
          <div className="flex-1 overflow-y-auto custom-scrollbar-minimal pr-4">
            {step === 1 && (
              <div className="space-y-8 animate-in fade-in slide-in-from-bottom-2 duration-300">
                <div className="flex items-center space-x-3 text-slate-400">
                  <LayoutDashboard size={18} />
                  <p className="text-sm font-medium">Seleccione los proyectos correspondientes</p>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                  {availableOTs.map(ot => (
                    <div key={ot.id} onClick={() => toggleOT(ot.id)}
                      className={`p-6 rounded-[24px] border-2 transition-all cursor-pointer group flex flex-col justify-between h-32 ${selectedOTs.includes(ot.id) ? 'border-blue-600 bg-blue-50/20' : 'border-slate-50 bg-slate-50/30 hover:border-slate-200'}`}>
                      <div className="flex justify-between items-start">
                        <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">{ot.id}</span>
                        <div className={`w-6 h-6 rounded-full flex items-center justify-center transition-all ${selectedOTs.includes(ot.id) ? 'bg-blue-600 text-white' : 'bg-white border-2 border-slate-200 text-transparent'}`}>
                          <Check size={14} strokeWidth={4} />
                        </div>
                      </div>
                      <p className={`font-bold transition-colors ${selectedOTs.includes(ot.id) ? 'text-slate-900' : 'text-slate-600 group-hover:text-slate-900'}`}>{ot.title}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {step === 2 && (
              <div className="space-y-12 animate-in fade-in slide-in-from-bottom-2 duration-300">
                <div className="p-6 rounded-3xl bg-blue-50 border border-blue-100 flex justify-between items-center">
                  <div className="flex items-center space-x-4">
                    <div className="w-12 h-12 rounded-2xl bg-blue-600 text-white flex items-center justify-center">
                      <Clock size={24} />
                    </div>
                    <div>
                      <p className="text-[10px] font-bold text-blue-600 uppercase tracking-widest leading-none mb-1">Horas Normales</p>
                      <p className="text-sm text-blue-900 font-medium tracking-tight">Distribución obligatoria de jornada</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className={`text-3xl font-black tabular-nums ${isStep2Valid ? 'text-emerald-500' : 'text-blue-600'}`}>
                      {currentTotal('normal').toFixed(1)} / {normalHoursMax.toFixed(1)}h
                    </p>
                  </div>
                </div>

                <div className="grid grid-cols-1 gap-8">
                  {selectedOTs.map(otId => (
                    <div key={otId} className="p-8 rounded-[32px] bg-white border border-slate-100 shadow-sm space-y-6">
                      <div className="flex justify-between items-center">
                        <span className="font-bold text-slate-900 text-lg">{availableOTs.find(o => o.id === otId)?.title}</span>
                        <div className="px-4 py-2 rounded-xl bg-slate-50 text-blue-600 font-black tabular-nums">
                          {(distribution.normal[otId] || 0).toFixed(1)}h
                        </div>
                      </div>
                      <input type="range" min="0" max={normalHoursMax} step="0.5" value={distribution.normal[otId] || 0}
                        className="w-full accent-blue-600 h-2 bg-slate-100 rounded-lg appearance-none cursor-pointer"
                        onChange={(e) => handleSliderChange(otId, e.target.value, 'normal')} />
                    </div>
                  ))}
                </div>
              </div>
            )}

            {step === 3 && (
              <div className="space-y-12 animate-in fade-in slide-in-from-bottom-2 duration-300">
                <div className="p-6 rounded-3xl bg-orange-50 border border-orange-100 flex justify-between items-center">
                  <div className="flex items-center space-x-4">
                    <div className="w-12 h-12 rounded-2xl bg-orange-500 text-white flex items-center justify-center">
                      <Clock size={24} />
                    </div>
                    <div>
                      <p className="text-[10px] font-bold text-orange-600 uppercase tracking-widest leading-none mb-1">Excedente / Extra</p>
                      <p className="text-sm text-orange-900 font-medium tracking-tight">Distribución de horas fuera de jornada</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className={`text-3xl font-black tabular-nums ${isStep3Valid ? 'text-emerald-500' : 'text-orange-500'}`}>
                      {currentTotal('extra').toFixed(1)} / {extraHoursMax.toFixed(1)}h
                    </p>
                  </div>
                </div>

                {extraHoursMax > 0 ? (
                  <div className="grid grid-cols-1 gap-8">
                    {selectedOTs.map(otId => (
                      <div key={otId} className="p-8 rounded-[32px] bg-white border border-slate-100 shadow-sm space-y-6">
                        <div className="flex justify-between items-center">
                          <span className="font-bold text-slate-900 text-lg">{availableOTs.find(o => o.id === otId)?.title}</span>
                          <div className="px-4 py-2 rounded-xl bg-slate-50 text-orange-600 font-black tabular-nums">
                            {(distribution.extra[otId] || 0).toFixed(1)}h
                          </div>
                        </div>
                        <input type="range" min="0" max={extraHoursMax} step="0.5" value={distribution.extra[otId] || 0}
                          className="w-full accent-orange-500 h-2 bg-slate-100 rounded-lg appearance-none cursor-pointer"
                          onChange={(e) => handleSliderChange(otId, e.target.value, 'extra')} />
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center py-24 bg-slate-50 rounded-[48px] border-2 border-dashed border-slate-200">
                    <div className="w-20 h-20 rounded-full bg-emerald-50 text-emerald-500 flex items-center justify-center mb-6">
                      <CheckCircle2 size={40} />
                    </div>
                    <p className="text-2xl font-bold text-slate-900 mb-2">Todo en orden</p>
                    <p className="text-slate-500">No hay horas de excedente para asignar hoy</p>
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="flex justify-between items-center pt-8 border-t border-slate-50 mt-auto">
            <button onClick={step === 1 ? onClose : () => setStep(step - 1)}
              className="px-8 py-4 text-slate-400 font-bold hover:text-slate-600 transition-all uppercase text-[10px] tracking-[0.2em] flex items-center space-x-3">
              <ChevronLeft size={16} />
              <span>{step === 1 ? 'Cerrar' : 'Atrás'}</span>
            </button>
            <button
              disabled={isLoading || (step === 1 && selectedOTs.length === 0) || (step === 2 && !isStep2Valid) || (step === 3 && !isStep3Valid)}
              onClick={() => {
                if (step === 1) setStep(2);
                else if (step === 2) extraHoursMax > 0 ? setStep(3) : onSave(distribution);
                else onSave(distribution);
              }}
              style={{ backgroundColor: (isLoading || (step === 1 && selectedOTs.length === 0) || (step === 2 && !isStep2Valid) || (step === 3 && !isStep3Valid)) ? '#f1f5f9' : (config.primaryColor || '#3b82f6') }}
              className={`px-12 py-4 rounded-[20px] font-bold text-[10px] tracking-[0.2em] uppercase transition-all flex items-center space-x-3 shadow-xl ${(isLoading || (step === 1 && selectedOTs.length === 0) || (step === 2 && !isStep2Valid) || (step === 3 && !isStep3Valid)) ? 'text-slate-300 shadow-none' : 'text-white shadow-blue-500/20 hover:brightness-110 active:scale-[0.98]'}`}
            >
              {isLoading ? (
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <>
                  <span>{(step === 3 || (step === 2 && extraHoursMax === 0)) ? 'Finalizar' : 'Siguiente'}</span>
                  <ChevronRight size={16} />
                </>
              )}
            </button>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
};

const ConfigurationView = ({ config, setConfig }) => {
  const [localConfig, setLocalConfig] = useState({ appName: '', primaryColor: '', secondaryColor: '', sidebarBg: '', sidebarText: '', sidebarItemText: '', logoText: '', logoImageUrl: '', faviconUrl: '', menuSections: [] });
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    const loadConfig = async () => {
      try {
        const res = await fetch(`${API_BASE}/config`);
        const data = await res.json();
        setLocalConfig({
          appName: data.appName || '',
          primaryColor: data.primaryColor || '#3b82f6',
          secondaryColor: data.secondaryColor || '#ef4444',
          sidebarBg: data.sidebarBg || '#0f172a',
          sidebarText: data.sidebarText || '#ffffff',
          sidebarItemText: data.sidebarItemText || '#94a3b8',
          logoText: data.logoText || 'B',
          logoImageUrl: data.logoImageUrl || '',
          faviconUrl: data.faviconUrl || '',
          menuSections: data.menuSections || []
        });
      } catch (e) {
        console.error('Error cargando configuración:', e);
      }
    };
    loadConfig();
  }, []);

  const handleSave = async () => {
    setSaving(true);
    setMessage('');
    try {
      const res = await fetch(`${API_BASE}/config`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(localConfig)
      });
      const data = await res.json();
      if (data.success) {
        // Actualizar config global inmediatamente
        setConfig({ 
          ...config, 
          appName: localConfig.appName, 
          primaryColor: localConfig.primaryColor,
          secondaryColor: localConfig.secondaryColor,
          sidebarBg: localConfig.sidebarBg,
          sidebarText: localConfig.sidebarText,
          sidebarItemText: localConfig.sidebarItemText,
          logoText: localConfig.logoText,
          logoImageUrl: localConfig.logoImageUrl,
          faviconUrl: localConfig.faviconUrl,
          menuSections: localConfig.menuSections
        });
        setMessage('✅ Configuración guardada y aplicada correctamente.');
      } else {
        setMessage('❌ Error al guardar: ' + (data.error || 'desconocido'));
      }
    } catch (e) {
      setMessage('❌ Servidor fuera de línea');
    } finally {
      setSaving(false);
    }
  };

  const updateSection = (index, field, value) => {
    const updated = [...localConfig.menuSections];
    updated[index] = { ...updated[index], [field]: value };
    setLocalConfig({ ...localConfig, menuSections: updated });
  };

  const availableIcons = ['LayoutDashboard', 'Calendar', 'ClipboardCheck', 'History', 'User', 'Settings', 'Clock', 'TrendingUp'];

  return (
    <div className="mx-auto space-y-8 animate-in fade-in duration-300 px-8 ml-0 md:ml-80">
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-black text-slate-800 tracking-tight">Configuración de la aplicación</h2>
          <p className="text-slate-400 text-sm font-bold uppercase tracking-widest">Panel exclusivo para SUPERADMIN</p>
        </div>
        <button onClick={handleSave} disabled={saving} className="px-6 py-3 bg-blue-600 text-white rounded-xl font-black text-xs uppercase tracking-widest hover:bg-blue-700 transition-all disabled:opacity-50 flex items-center space-x-2">
          {saving ? <Loader2 className="animate-spin" size={16} /> : <Save size={16} />}
          <span>{saving ? 'Guardando...' : 'Guardar cambios'}</span>
        </button>
      </header>

      {message && (
        <div className={`p-4 rounded-xl font-bold text-sm border ${message.includes('✅') ? 'bg-emerald-50 border-emerald-200 text-emerald-700' : 'bg-red-50 border-red-200 text-red-700'}`}>
          {message}
        </div>
      )}

      <section className="rounded-2xl p-8 border-8 border-slate-100 shadow-lg bg-white">
        <h3 className="font-black text-sm uppercase tracking-widest mb-6 text-slate-600">General</h3>
        <div className="space-y-6">
          <div>
            <label className="text-xs font-black text-slate-400 uppercase tracking-widest mb-2 block">Nombre de la aplicación</label>
            <textarea value={localConfig.appName} onChange={(e) => setLocalConfig({ ...localConfig, appName: e.target.value })} rows="2" className="w-full px-4 py-3 border border-slate-200 rounded-xl font-bold text-slate-700 resize-none" placeholder="Presiona Enter para salto de línea" />
          </div>
          <div>
            <label className="text-xs font-black text-slate-400 uppercase tracking-widest mb-2 block">Icono del logo (1 letra)</label>
            <input type="text" maxLength="1" value={localConfig.logoText} onChange={(e) => setLocalConfig({ ...localConfig, logoText: e.target.value.toUpperCase() })} className="w-full px-4 py-3 border border-slate-200 rounded-xl font-bold text-slate-700 text-center text-2xl" placeholder="B" />
          </div>
          <div>
            <label className="text-xs font-black text-slate-400 uppercase tracking-widest mb-2 block">Logo PNG (opcional)</label>
            <div className="flex items-center space-x-4">
              <input type="file" accept="image/png" onChange={async (e) => {
                const file = e.target.files?.[0];
                if (!file) return;
                const reader = new FileReader();
                reader.onload = async () => {
                  try {
                    const res = await fetch(`${API_BASE}/upload`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ type: 'logo', data: reader.result }) });
                    const out = await res.json();
                    if (out.success) setLocalConfig({ ...localConfig, logoImageUrl: out.url });
                  } catch (err) { console.error('Upload logo error', err); }
                };
                reader.readAsDataURL(file);
              }} />
              {localConfig.logoImageUrl && <img src={localConfig.logoImageUrl} alt="logo" className="w-10 h-10 rounded-lg object-cover" />}
            </div>
          </div>
          <div>
            <label className="text-xs font-black text-slate-400 uppercase tracking-widest mb-2 block">Favicon PNG (16x16 o 32x32)</label>
            <div className="flex items-center space-x-4">
              <input type="file" accept="image/png" onChange={async (e) => {
                const file = e.target.files?.[0];
                if (!file) return;
                const reader = new FileReader();
                reader.onload = async () => {
                  try {
                    const res = await fetch(`${API_BASE}/upload`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ type: 'favicon', data: reader.result }) });
                    const out = await res.json();
                    if (out.success) setLocalConfig({ ...localConfig, faviconUrl: out.url });
                  } catch (err) { console.error('Upload favicon error', err); }
                };
                reader.readAsDataURL(file);
              }} />
              {localConfig.faviconUrl && <img src={localConfig.faviconUrl} alt="favicon" className="w-8 h-8 rounded object-cover" />}
            </div>
          </div>
        </div>
      </section>

      <section className="rounded-2xl p-8 border-8 border-slate-100 shadow-lg bg-white">
        <h3 className="font-black text-sm uppercase tracking-widest mb-6 text-slate-600">Colores</h3>
        <div className="space-y-6">
          <div>
            <label className="text-xs font-black text-slate-400 uppercase tracking-widest mb-2 block">Color primario (botones, login)</label>
            <div className="flex items-center space-x-4">
              <input type="color" value={localConfig.primaryColor} onChange={(e) => setLocalConfig({ ...localConfig, primaryColor: e.target.value })} className="w-16 h-12 border border-slate-200 rounded-xl cursor-pointer" />
              <input type="text" value={localConfig.primaryColor} onChange={(e) => setLocalConfig({ ...localConfig, primaryColor: e.target.value })} className="flex-1 px-4 py-3 border border-slate-200 rounded-xl font-mono text-slate-700" />
            </div>
          </div>
          <div>
            <label className="text-xs font-black text-slate-400 uppercase tracking-widest mb-2 block">Color secundario (item seleccionado)</label>
            <div className="flex items-center space-x-4">
              <input type="color" value={localConfig.secondaryColor} onChange={(e) => setLocalConfig({ ...localConfig, secondaryColor: e.target.value })} className="w-16 h-12 border border-slate-200 rounded-xl cursor-pointer" />
              <input type="text" value={localConfig.secondaryColor} onChange={(e) => setLocalConfig({ ...localConfig, secondaryColor: e.target.value })} className="flex-1 px-4 py-3 border border-slate-200 rounded-xl font-mono text-slate-700" />
            </div>
          </div>
          <div>
            <label className="text-xs font-black text-slate-400 uppercase tracking-widest mb-2 block">Fondo barra lateral</label>
            <div className="flex items-center space-x-4">
              <input type="color" value={localConfig.sidebarBg} onChange={(e) => setLocalConfig({ ...localConfig, sidebarBg: e.target.value })} className="w-16 h-12 border border-slate-200 rounded-xl cursor-pointer" />
              <input type="text" value={localConfig.sidebarBg} onChange={(e) => setLocalConfig({ ...localConfig, sidebarBg: e.target.value })} className="flex-1 px-4 py-3 border border-slate-200 rounded-xl font-mono text-slate-700" />
            </div>
          </div>
          <div>
            <label className="text-xs font-black text-slate-400 uppercase tracking-widest mb-2 block">Texto barra lateral (título)</label>
            <div className="flex items-center space-x-4">
              <input type="color" value={localConfig.sidebarText} onChange={(e) => setLocalConfig({ ...localConfig, sidebarText: e.target.value })} className="w-16 h-12 border border-slate-200 rounded-xl cursor-pointer" />
              <input type="text" value={localConfig.sidebarText} onChange={(e) => setLocalConfig({ ...localConfig, sidebarText: e.target.value })} className="flex-1 px-4 py-3 border border-slate-200 rounded-xl font-mono text-slate-700" />
            </div>
          </div>
          <div>
            <label className="text-xs font-black text-slate-400 uppercase tracking-widest mb-2 block">Texto items menú</label>
            <div className="flex items-center space-x-4">
              <input type="color" value={localConfig.sidebarItemText} onChange={(e) => setLocalConfig({ ...localConfig, sidebarItemText: e.target.value })} className="w-16 h-12 border border-slate-200 rounded-xl cursor-pointer" />
              <input type="text" value={localConfig.sidebarItemText} onChange={(e) => setLocalConfig({ ...localConfig, sidebarItemText: e.target.value })} className="flex-1 px-4 py-3 border border-slate-200 rounded-xl font-mono text-slate-700" />
            </div>
          </div>
        </div>
      </section>

      <section className="rounded-2xl p-8 border-8 border-slate-100 shadow-lg bg-white">
        <h3 className="font-black text-sm uppercase tracking-widest mb-6 text-slate-600">Menú lateral</h3>
        <div className="space-y-4">
          {localConfig.menuSections.map((section, idx) => (
            <div key={idx} className="flex items-start space-x-4 p-4 border border-slate-200 rounded-xl bg-slate-50">
              <div className="flex-1">
                <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1 block">Etiqueta (presiona Enter para salto de línea)</label>
                <textarea value={section.label} onChange={(e) => updateSection(idx, 'label', e.target.value)} rows="2" className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm font-bold text-slate-700 resize-none" />
              </div>
              <div className="w-48">
                <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1 block">Icono</label>
                <select value={section.icon} onChange={(e) => updateSection(idx, 'icon', e.target.value)} className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm font-bold text-slate-700">
                  {availableIcons.map(icon => <option key={icon} value={icon}>{icon}</option>)}
                </select>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};

const EditProfileModal = ({ item, user, targetUsername, onClose, onSave }) => {
  const [value, setValue] = useState(item.value || '');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const validateInput = () => {
    const lowerLabel = (item.label || '').toLowerCase();
    if (lowerLabel.includes('email')) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (value && !emailRegex.test(value)) {
        setError('Email inválido. Ej: usuario@empresa.com');
        return false;
      }
    } else if (lowerLabel.includes('teléfono')) {
      const phoneRegex = /^[\d\s\-\+\(\)]{6,}$/;
      if (value && !phoneRegex.test(value)) {
        setError('Teléfono inválido. Debe tener al menos 6 dígitos.');
        return false;
      }
    }
    setError('');
    return true;
  };

  const handleSave = async () => {
    if (!validateInput()) return;
    setSaving(true);
    try {
      const res = await fetch(`${API_BASE}/profile/update`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: targetUsername || user.username, field: item.field, value, actor: user.username })
      });
      const data = await res.json();
      if (res.ok && data.success) {
        onSave(value);
      } else {
        setError(data.message || 'Error al guardar');
      }
    } catch (e) {
      setError('Servidor fuera de línea');
    } finally {
      setSaving(false);
    }
  };

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 z-[500] flex items-center justify-center bg-black/40 backdrop-blur-[2px] p-4">
      <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.95, opacity: 0 }} className="bg-white rounded-2xl p-8 max-w-sm w-full shadow-2xl border border-slate-200">
        <h3 className="text-2xl font-black text-slate-900 mb-2 tracking-tight">Editar {item.label}</h3>
        <p className="text-slate-400 text-sm font-bold mb-6 uppercase tracking-widest">Valor actual: {item.value || 'vacío'}</p>

        <div className="space-y-4">
          <input
            type="text"
            value={value}
            onChange={(e) => { setValue(e.target.value); setError(''); }}
            placeholder={`Nuevo ${item.label.toLowerCase()}`}
            className="w-full px-4 py-3 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
          />

          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-red-600 text-xs font-bold">
              {error}
            </div>
          )}

          <div className="flex space-x-3 pt-4">
            <button onClick={onClose} disabled={saving} className="flex-1 px-4 py-3 border border-slate-200 text-slate-600 rounded-xl font-black text-xs uppercase tracking-widest hover:bg-slate-50 transition-all disabled:opacity-50">
              Cancelar
            </button>
            <button onClick={handleSave} disabled={saving} className="flex-1 px-4 py-3 bg-blue-600 text-white rounded-xl font-black text-xs uppercase tracking-widest hover:bg-blue-700 transition-all disabled:opacity-50 flex items-center justify-center space-x-2">
              {saving ? <Loader2 className="animate-spin" size={16} /> : <Check size={16} />}
              <span>{saving ? 'Guardando...' : 'Guardar'}</span>
            </button>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
};

export default App;
