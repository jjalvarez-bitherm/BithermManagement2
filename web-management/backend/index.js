const express = require('express');
const cors = require('cors');
const { google } = require('googleapis');
const path = require('path');
const fs = require('fs');

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

const app = express();
app.use(cors());
app.use(express.json());

const SPREADSHEET_ID = '1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4';
const CREDENTIALS_PATH = path.join(__dirname, '../../app/src/main/assets/credentials_default.json');
const VARIABLES_PATH = path.join(__dirname, '../../variables.json');

// Lista de libros disponibles (spreadsheets)
const AVAILABLE_BOOKS = [
    {
        id: '1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4',
        name: 'Bitherm Management 2.0',
        description: 'Libro principal de gestión'
    }
    // Aquí se pueden agregar más libros si es necesario
];

async function getSheetsService() {
    const auth = new google.auth.GoogleAuth({
        keyFile: CREDENTIALS_PATH,
        scopes: [
            'https://www.googleapis.com/auth/spreadsheets',
            'https://www.googleapis.com/auth/drive.readonly'
        ],
    });
    return google.sheets({ version: 'v4', auth });
}

async function getDriveService() {
    const auth = new google.auth.GoogleAuth({
        keyFile: CREDENTIALS_PATH,
        scopes: [
            'https://www.googleapis.com/auth/spreadsheets',
            'https://www.googleapis.com/auth/drive.readonly'
        ],
    });
    return google.drive({ version: 'v3', auth });
}

function safePad(n) {
    const num = parseInt(n);
    if (isNaN(num)) return "00";
    return num.toString().padStart(2, '0');
}

// 0. CONFIGURACIÓN
app.get('/api/config', (req, res) => {
    try {
        const vars = JSON.parse(fs.readFileSync(VARIABLES_PATH, 'utf8'));
        res.json({
            appName: vars.app_config?.app_name || 'Bitherm Admin',
            primaryColor: vars.login_module?.login_screen?.primary_color || '#3b82f6',
            logoText: 'B',
            version: vars.app_config?.version_name || '1.0'
        });
    } catch (e) {
        res.json({ appName: 'Bitherm Admin', primaryColor: '#3b82f6', logoText: 'B' });
    }
});

// 1. LOGIN
app.post('/api/login', async (req, res) => {
    try {
        const { username, password } = req.body;
        const sheets = await getSheetsService();
        const response = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID,
            range: 'TRABAJADORES!A1:T500',
        });
        const rows = response.data.values;
        if (!rows || rows.length < 1) return res.status(404).json({ message: 'No se encontraron trabajadores' });

        // Buscamos la fila de cabeceras
        let headIdx = rows.findIndex(r => r.includes('APP'));
        if (headIdx === -1) headIdx = 0;

        const headers = rows[headIdx];
        const dataRows = rows.slice(headIdx + 1);
        const appIndex = headers.indexOf('APP');
        const passIndex = headers.indexOf('PASS');

        const userRow = dataRows.find(row =>
            row[appIndex] && row[appIndex].trim() === username.trim() &&
            row[passIndex] && row[passIndex].toString().trim() === password.toString().trim()
        );

        if (userRow) {
            res.json({
                success: true,
                user: {
                    username: userRow[appIndex].trim(),
                    name: `${userRow[headers.indexOf('NOMBRE')] || ''} ${userRow[headers.indexOf('APELLIDOS')] || ''}`.trim(),
                    role: userRow[headers.indexOf('ROL')] || 'Operario'
                }
            });
        } else {
            res.status(401).json({ success: false, message: 'Credenciales inválidas' });
        }
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// 2. OTs
app.get('/api/workers', async (req, res) => {
    try {
        const sheets = await getSheetsService();
        const response = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID,
            range: 'TRABAJADORES!A1:T100',
        });
        const rows = response.data.values;
        if (!rows || rows.length < 1) return res.json([]);

        // Buscamos la fila de cabeceras (la que tiene 'APP')
        let headIdx = rows.findIndex(r => r.includes('APP'));
        if (headIdx === -1) headIdx = 0; // fallback

        const headers = rows[headIdx];
        const dataRows = rows.slice(headIdx + 1);

        const appIndex = headers.indexOf('APP');
        const nombreIndex = headers.indexOf('NOMBRE');
        const apeIndex = headers.indexOf('APELLIDOS');

        const workers = dataRows.map(row => ({
            username: row[appIndex]?.trim(),
            name: `${row[nombreIndex] || ''} ${row[apeIndex] || ''}`.trim()
        })).filter(w => w.username && w.username !== 'APP');

        res.json(workers);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.get('/api/ots/:username', async (req, res) => {
    try {
        const { username } = req.params;
        const sheets = await getSheetsService();
        const response = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID,
            range: 'FICHAJE-OT-MES!A1:G500',
        });
        const rows = response.data.values;
        if (!rows) return res.json([]);

        const ots = rows
            .filter(row => row[2] && row[2].toString().trim() === username.trim())
            .map(row => ({ id: row[4], title: row[4] }))
            .filter(item => item.id)
            .filter((v, i, a) => a.findIndex(t => t.id === v.id) === i);

        res.json(ots);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.get('/api/locations', async (req, res) => {
    try {
        const sheets = await getSheetsService();
        const response = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID,
            range: 'EQUIPOS!J2:L100',
        });
        const rows = response.data.values || [];
        const locations = rows
            .filter(row => row[0] && row[1])
            .map(row => ({
                name: row[0],
                coords: row[1],
                tolerance: parseFloat(row[2]) || 500
            }));
        res.json(locations);
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// 3. ESTADÍSTICAS Y HISTORIAL PAGINADO POR MES
app.get('/api/stats/:username', async (req, res) => {
    try {
        const { username } = req.params;
        const { month, year } = req.query;
        const sheets = await getSheetsService();
        const response = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID,
            range: 'FICHAJE-LOGS!A:G',
        });

        const allRows = response.data.values || [];
        const userLogs = allRows.filter(r => r[0] && r[0].toString().trim() === username.trim());

        const now = new Date();
        const targetMonth = month ? parseInt(month) : now.getMonth();
        const targetYear = year ? parseInt(year) : now.getFullYear();

        let monthlyNormalMinutes = 0;
        const history = [];
        let tempEntry = null;
        let activeSession = null;

        userLogs.forEach(row => {
            if (!row[1] || !row[2]) return;

            try {
                const dateParts = row[1].toString().split('/');
                const timeParts = row[2].toString().split(':');
                if (dateParts.length < 3) return;

                const [d, m, y] = dateParts;
                const rowDate = new Date(parseInt(y), parseInt(m) - 1, parseInt(d));
                const isTargetMonth = rowDate.getMonth() === targetMonth && rowDate.getFullYear() === targetYear;

                const [hh, mm, ss] = timeParts;
                const entryDate = new Date(parseInt(y), parseInt(m) - 1, parseInt(d), parseInt(hh), parseInt(mm), parseInt(ss || 0));
                if (isNaN(entryDate)) return;

                const timeFormatted = `${safePad(hh)}:${safePad(mm)}`;

                if (row[3] === 'ENTRADA') {
                    if (tempEntry && tempEntry.isTargetMonth) {
                        history.push({
                            date: tempEntry.date, in: tempEntry.time, out: '-',
                            duration: '0.0h', locIn: tempEntry.loc, locOut: '-',
                            sinSalida: true
                        });
                    }
                    tempEntry = { date: row[1], time: timeFormatted, loc: row[6] || 'Externo', rawDate: entryDate, isTargetMonth };
                } else if (row[3] === 'SALIDA') {
                    const durationMinutes = parseInt(row[4]) || 0;
                    if (isTargetMonth) monthlyNormalMinutes += durationMinutes;

                    if (tempEntry && tempEntry.date === row[1].toString()) {
                        if (isTargetMonth) {
                            history.push({
                                date: row[1], in: tempEntry.time, out: timeFormatted,
                                duration: `${(durationMinutes / 60).toFixed(1)}h`,
                                locIn: tempEntry.loc, locOut: row[6] || 'Externo'
                            });
                        }
                        tempEntry = null;
                    } else {
                        if (isTargetMonth) {
                            history.push({
                                date: row[1], in: '-', out: timeFormatted,
                                duration: `${(durationMinutes / 60).toFixed(1)}h`,
                                locIn: '-', locOut: row[6] || 'Externo'
                            });
                        }
                        tempEntry = null;
                    }
                }
            } catch (e) { }
        });

        if (tempEntry) {
            const hoursDiff = (now - tempEntry.rawDate) / (1000 * 60 * 60);
            if (hoursDiff < 24) {
                activeSession = { startTime: tempEntry.rawDate.getTime(), timeIn: tempEntry.time };
            }
            if (tempEntry.isTargetMonth) {
                history.push({
                    date: tempEntry.date, in: tempEntry.time, out: '-',
                    duration: '0.0h', locIn: tempEntry.loc, locOut: '-',
                    sinSalida: true
                });
            }
        }

        // Calcular Vacaciones y Bajas desde FICHAJE-OT-MES
        const otResponse = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID,
            range: 'FICHAJE-OT-MES!A1:ZZ500',
        });
        const otValues = otResponse.data.values || [];
        const otHeader = otValues[0] || [];

        let totalVacacionesHoras = 0;
        let totalBajaHoras = 0;

        // Encontrar columnas del mes actual (formato D/M o DD/MM)
        const monthCols = [];
        otHeader.forEach((h, idx) => {
            if (!h) return;
            const parts = h.toString().split('/');
            if (parts.length >= 2) {
                const m = parseInt(parts[1]);
                if (m === targetMonth + 1) monthCols.push(idx);
            }
        });

        otValues.forEach(row => {
            if (row[2] && row[2].toString().trim() === username.trim()) {
                const otName = (row[4] || "").toString().trim().toUpperCase();
                if (otName === 'VACACIONES' || otName === 'BAJAMED') {
                    monthCols.forEach(colIdx => {
                        const val = parseFloat(row[colIdx]) || 0;
                        if (otName === 'VACACIONES') totalVacacionesHoras += val;
                        else totalBajaHoras += val;
                    });
                }
            }
        });

        const totalHours = (monthlyNormalMinutes / 60).toFixed(1);
        const extraHours = Math.max(0, parseFloat(totalHours) - 160).toFixed(1);
        const trabajados = (parseFloat(totalHours) / 8).toFixed(1);
        const vacacionesDias = (totalVacacionesHoras / 8).toFixed(1);
        const bajaDias = (totalBajaHoras / 8).toFixed(1);

        res.json({
            totalHours: `${totalHours}h`,
            extraHours: `${extraHours}h`,
            trabajados: `${trabajados} días`,
            vacaciones: `${vacacionesDias} días`,
            baja: `${bajaDias} días`,
            activeSession,
            history: history.reverse()
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// 4. REGISTRAR FICHAJE (CON BACKFILL DE BAJAS/VACACIONES)
app.post('/api/fichaje', async (req, res) => {
    try {
        const { username, tipo, duracion, ubicacion, gps } = req.body;
        const sheets = await getSheetsService();

        // --- LOGICA DE BACKFILL DE BAJAS ---
        if (tipo === 'ENTRADA') {
            const trabajadoresRes = await sheets.spreadsheets.values.get({
                spreadsheetId: SPREADSHEET_ID, range: 'TRABAJADORES!A2:X',
            });
            const tRows = trabajadoresRes.data.values || [];
            const userRowIdx = tRows.findIndex(r => r[17] && r[17].trim() === username.trim());

            if (userRowIdx !== -1) {
                const row = tRows[userRowIdx];
                const leaveType = row[21]; // Col V
                const leaveStart = row[22]; // Col W

                if (leaveType && leaveStart) {
                    // Backfill desde el día siguiente al inicio hasta ayer
                    const startArr = leaveStart.split('/');
                    const start = new Date(parseInt(startArr[2]), parseInt(startArr[1]) - 1, parseInt(startArr[0]));
                    const today = new Date();
                    today.setHours(0, 0, 0, 0);

                    const iterate = new Date(start);
                    iterate.setDate(iterate.getDate() + 1);

                    while (iterate < today) {
                        const dateStr = `${iterate.getDate()}/${iterate.getMonth() + 1}/${iterate.getFullYear()}`;
                        const dayMonth = `${iterate.getDate()}/${iterate.getMonth() + 1}`;

                        // 1. Log en FICHAJE-LOGS (8h = 480 min)
                        await sheets.spreadsheets.values.append({
                            spreadsheetId: SPREADSHEET_ID,
                            range: 'FICHAJE-LOGS!A:G',
                            valueInputOption: 'USER_ENTERED',
                            requestBody: { values: [[username, dateStr, '08:00:00', 'ENTRADA', 0, '', 'AUTO-LEAVE']] },
                        });
                        await sheets.spreadsheets.values.append({
                            spreadsheetId: SPREADSHEET_ID,
                            range: 'FICHAJE-LOGS!A:G',
                            valueInputOption: 'USER_ENTERED',
                            requestBody: { values: [[username, dateStr, '16:00:00', 'SALIDA', 480, '', 'AUTO-LEAVE']] },
                        });

                        // 2. Log en FICHAJE-OT-MES
                        const otRes = await sheets.spreadsheets.values.get({
                            spreadsheetId: SPREADSHEET_ID, range: 'FICHAJE-OT-MES!A1:ZZ500',
                        });
                        const otVals = otRes.data.values;
                        const header = otVals[0];
                        let colIdx = header.findIndex(h => h && h.toString().trim().replace(/^0/, '') === dayMonth.replace(/^0/, ''));
                        const rowIndex = otVals.findIndex(r => r[2] === username && r[4] === leaveType);

                        if (colIdx !== -1 && rowIndex !== -1) {
                            await sheets.spreadsheets.values.update({
                                spreadsheetId: SPREADSHEET_ID,
                                range: `FICHAJE-OT-MES!${numberToColumn(colIdx)}${rowIndex + 1}`,
                                valueInputOption: 'USER_ENTERED',
                                requestBody: { values: [[8]] }
                            });
                        }
                        iterate.setDate(iterate.getDate() + 1);
                    }

                    // Limpiar estado
                    await sheets.spreadsheets.values.update({
                        spreadsheetId: SPREADSHEET_ID,
                        range: `TRABAJADORES!V${userRowIdx + 2}:W${userRowIdx + 2}`,
                        valueInputOption: 'USER_ENTERED',
                        requestBody: { values: [['', '']] }
                    });
                }
            }
        }
        // --- FIN LOGICA BACKFILL ---

        const hoy = new Date();
        const fechaStr = `${hoy.getDate()}/${hoy.getMonth() + 1}/${hoy.getFullYear()}`;
        const horaStr = `${safePad(hoy.getHours())}:${safePad(hoy.getMinutes())}:${safePad(hoy.getSeconds())}`;

        const values = [[
            username.trim(), fechaStr, horaStr, tipo, duracion || 0, gps || '', ubicacion || 'Externo'
        ]];

        await sheets.spreadsheets.values.append({
            spreadsheetId: SPREADSHEET_ID,
            range: 'FICHAJE-LOGS!A:G',
            valueInputOption: 'USER_ENTERED',
            requestBody: { values },
        });
        res.json({ success: true });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// 4.5 INICIAR BAJA/VACACIONES
app.post('/api/leave/start', async (req, res) => {
    try {
        const { username, type } = req.body; // type = 'VACACIONES' o 'BAJAMED'
        const sheets = await getSheetsService();
        const hoy = new Date();
        const dateStr = `${hoy.getDate()}/${hoy.getMonth() + 1}/${hoy.getFullYear()}`;
        const dayMonth = `${hoy.getDate()}/${hoy.getMonth() + 1}`;

        // 1. Marcar hoy como 8h en FICHAJE-OT-MES
        const otRes = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID, range: 'FICHAJE-OT-MES!A1:ZZ500',
        });
        const otVals = otRes.data.values;
        const header = otVals[0];
        let colIdx = header.findIndex(h => h && h.toString().trim().replace(/^0/, '') === dayMonth.replace(/^0/, ''));
        const rowIndex = otVals.findIndex(r => r[2] === username && r[4] === type);

        if (colIdx !== -1 && rowIndex !== -1) {
            await sheets.spreadsheets.values.update({
                spreadsheetId: SPREADSHEET_ID,
                range: `FICHAJE-OT-MES!${numberToColumn(colIdx)}${rowIndex + 1}`,
                valueInputOption: 'USER_ENTERED',
                requestBody: { values: [[8]] }
            });
        }

        // 2. Log en FICHAJE-LOGS (8h completa)
        await sheets.spreadsheets.values.append({
            spreadsheetId: SPREADSHEET_ID,
            range: 'FICHAJE-LOGS!A:G',
            valueInputOption: 'USER_ENTERED',
            requestBody: { values: [[username, dateStr, '08:00:00', 'ENTRADA', 0, '', 'LEAVE-START']] },
        });
        await sheets.spreadsheets.values.append({
            spreadsheetId: SPREADSHEET_ID,
            range: 'FICHAJE-LOGS!A:G',
            valueInputOption: 'USER_ENTERED',
            requestBody: { values: [[username, dateStr, '16:00:00', 'SALIDA', 480, '', 'LEAVE-START']] },
        });

        // 3. Guardar estado en TRABAJADORES (V y W)
        const trabajadoresRes = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID, range: 'TRABAJADORES!A2:T', // T es col 20 (idx 19), necesitamos V y W
        });
        const tRows = trabajadoresRes.data.values || [];
        const userRowIdx = tRows.findIndex(r => r[17] && r[17].trim() === username.trim());

        if (userRowIdx !== -1) {
            await sheets.spreadsheets.values.update({
                spreadsheetId: SPREADSHEET_ID,
                range: `TRABAJADORES!V${userRowIdx + 2}:W${userRowIdx + 2}`,
                valueInputOption: 'USER_ENTERED',
                requestBody: { values: [[type, dateStr]] }
            });
        }

        res.json({ success: true });
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// 5. GUARDAR DISTRIBUCIÓN
app.post('/api/distribution', async (req, res) => {
    try {
        const { username, distribution, type } = req.body;
        const sheets = await getSheetsService();
        const response = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID,
            range: 'FICHAJE-OT-MES!A1:ZZ500',
        });
        const values = response.data.values;
        const header = values[0];
        const now = new Date();
        const hoyStr = `${now.getDate()}/${now.getMonth() + 1}`;
        let colIndex = header.findIndex(h => h && h.toString().trim().replace(/^0/, '') === hoyStr.replace(/^0/, ''));
        if (colIndex === -1) {
            const hoyStrAlt = `${safePad(now.getDate())}/${safePad(now.getMonth() + 1)}`;
            colIndex = header.findIndex(h => h && h.toString().trim() === hoyStrAlt);
        }
        if (colIndex === -1) return res.status(404).json({ message: 'Día no encontrado' });

        const targetColIndex = type === 'extra' ? colIndex + 1 : colIndex;
        const updates = [];
        for (const [ot, horas] of Object.entries(distribution)) {
            if (horas <= 0) continue;
            const rowIndex = values.findIndex(row => row[2] === username && row[4] === ot);
            if (rowIndex !== -1) {
                updates.push({
                    range: `FICHAJE-OT-MES!${numberToColumn(targetColIndex)}${rowIndex + 1}`,
                    values: [[horas]]
                });
            }
        }
        for (const u of updates) {
            await sheets.spreadsheets.values.update({
                spreadsheetId: SPREADSHEET_ID,
                range: u.range,
                valueInputOption: 'USER_ENTERED',
                requestBody: { values: u.values },
            });
        }
        res.json({ success: true });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

function numberToColumn(num) {
    let name = "";
    while (num >= 0) {
        name = String.fromCharCode((num % 26) + 65) + name;
        num = Math.floor(num / 26) - 1;
    }
    return name;
}

// 6. GESTIÓN DE AUSENCIAS
app.get('/api/absences/logs', async (req, res) => {
    try {
        const sheets = await getSheetsService();
        const response = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID,
            range: 'AUSENCIAS-LOG!A:M',
        });
        const rows = response.data.values || [];
        if (rows.length === 0) return res.json([]);

        const headers = rows[0];
        const data = rows.slice(1).map(row => {
            const obj = {};
            headers.forEach((h, i) => obj[h] = row[i] || '');
            return obj;
        });
        res.json(data);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

app.get('/api/absences/cuadro', async (req, res) => {
    try {
        const sheets = await getSheetsService();
        const response = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID,
            range: 'AUSENCIAS-CUADRO!A1:NZ500',
        });
        res.json(response.data.values || []);
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// Endpoint para obtener equipos de trabajo
app.get('/api/teams', async (req, res) => {
    try {
        const sheets = await getSheetsService();
        const response = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID,
            range: 'EQUIPOS!A:H',
        });

        const rows = response.data.values || [];
        console.log('🟢 /api/teams - Total rows:', rows.length);
        if (rows.length === 0) {
            return res.json({});
        }

        // Leer headers de la primera fila
        const headers = rows[0].map(h => h.toString().toLowerCase().trim());
        console.log('🟢 /api/teams - Headers detectados:', headers);
        
        // Encontrar índices de columnas dinámicamente
        const usernameIdx = headers.findIndex(h => h === 'username');
        const workTeamIdx = headers.findIndex(h => h === 'workteam');
        const visibleNameIdx = headers.findIndex(h => h === 'visiblename');
        
        console.log('🟢 /api/teams - Índices:', { usernameIdx, workTeamIdx, visibleNameIdx });
        
        if (usernameIdx === -1 || workTeamIdx === -1) {
            console.error('❌ No se encontraron las columnas requeridas');
            return res.status(500).json({ error: 'Columnas username o workTeam no encontradas en EQUIPOS' });
        }

        // Procesar datos de equipos
        const teams = {};
        const roleMap = {
            'leader': 'Técnico',
            'iot': 'IoT', 
            'repair': 'Reparador',
            'inspection': 'Inspector'
        };

        // Procesar filas (saltando header)
        for (let i = 1; i < rows.length; i++) {
            const row = rows[i];
            const username = (row[usernameIdx] || '').toString().trim();
            const teamRaw = (row[workTeamIdx] || '').toString().trim();
            const visibleName = visibleNameIdx !== -1 ? (row[visibleNameIdx] || '').toString().trim() : '';
            
            if (!username) continue;

            // Limpiar el asterisco (*) del team si existe
            const team = teamRaw.replace('*', '').trim();

            teams[username] = {
                team: team, // 'leader', 'inspection', 'repair', 'iot'
                role: team, // Mismo valor que team para compatibilidad
                roleLabel: roleMap[team] || team,
                visibleName: visibleName
            };
        }

        console.log('🟢 /api/teams - Teams procesados:', Object.keys(teams).length);
        console.log('🟢 /api/teams - Muestra:', Object.entries(teams).slice(0, 3));
        res.json(teams);
    } catch (e) {
        console.error('Error obteniendo equipos:', e);
        res.status(500).json({ error: e.message });
    }
});

app.get('/api/festivos/:bookId/:sheetName', async (req, res) => {
    try {
        const { bookId, sheetName } = req.params;
        const sheets = await getSheetsService();
        
        const response = await sheets.spreadsheets.values.get({
            spreadsheetId: bookId,
            range: `${sheetName}!A:ZZ`,
        });

        const rows = response.data.values || [];
        if (rows.length === 0) {
            return res.json([]);
        }

        const festivos = [];
        
        // Buscar la fila de FESTIVOS (primeras 10 filas)
        let festivosRowIdx = -1;
        for (let i = 0; i < Math.min(10, rows.length); i++) {
            const firstCell = (rows[i][0] || '').toString().trim().toUpperCase();
            if (firstCell === 'FESTIVOS' || firstCell === 'F') {
                festivosRowIdx = i;
                break;
            }
        }

        if (festivosRowIdx === -1) {
            return res.json([]);
        }

        // Buscar la fila de fechas (row 0 o 1 típicamente)
        const headerRow = rows[0] || [];
        let firstDateCol = -1;
        
        for (let col = 0; col < headerRow.length; col++) {
            const cellValue = (headerRow[col] || '').toString().trim();
            if (cellValue.match(/^\d{1,2}\/\d{1,2}\/\d{2,4}$/)) {
                firstDateCol = col;
                break;
            }
        }

        if (firstDateCol === -1) {
            return res.json([]);
        }

        const festivosRow = rows[festivosRowIdx];
        
        // Recorrer las columnas desde firstDateCol
        for (let col = firstDateCol; col < headerRow.length; col++) {
            const dateValue = (headerRow[col] || '').toString().trim();
            const festivoMarker = (festivosRow[col] || '').toString().trim().toUpperCase();
            
            if (festivoMarker === 'F' && dateValue.match(/^\d{1,2}\/\d{1,2}\/\d{2,4}$/)) {
                festivos.push(dateValue);
            }
        }

        res.json(festivos);
    } catch (e) {
        console.error('Error obteniendo festivos:', e);
        res.status(500).json({ error: e.message });
    }
});

// Endpoint para guardar festivos como ausencias especiales en AUSENCIAS-LOG
app.post('/api/festivos/save', async (req, res) => {
    try {
        const { festivos } = req.body; // Array de fechas DD/MM/YYYY
        
        if (!Array.isArray(festivos) || festivos.length === 0) {
            return res.json({ success: true, saved: 0 });
        }

        const sheets = await getSheetsService();
        
        // Primero, limpiar festivos antiguos de AUSENCIAS-LOG
        const logRes = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID,
            range: 'AUSENCIAS-LOG!A:M',
        });
        
        const logRows = logRes.data.values || [];
        const festivoRowIndices = [];
        
        // Encontrar filas de festivos existentes
        for (let i = 1; i < logRows.length; i++) { // Empezar desde 1 para saltarse header
            if (logRows[i] && logRows[i][3] === 'FESTIVO') {
                festivoRowIndices.push(i);
            }
        }

        // Eliminar filas de festivos antiguos (de atrás para adelante para no cambiar índices)
        for (let i = festivoRowIndices.length - 1; i >= 0; i--) {
            const rowIdx = festivoRowIndices[i];
            await sheets.spreadsheets.batchUpdate({
                spreadsheetId: SPREADSHEET_ID,
                requestBody: {
                    requests: [{
                        deleteDimension: {
                            range: {
                                sheetId: 0, // Ajustar si es necesario
                                dimension: 'ROWS',
                                startIndex: rowIdx,
                                endIndex: rowIdx + 1
                            }
                        }
                    }]
                }
            });
        }

        // Ahora agregar festivos nuevos
        const hoy = new Date();
        const today = `${safePad(hoy.getDate())}/${safePad(hoy.getMonth() + 1)}/${hoy.getFullYear()} ${safePad(hoy.getHours())}:${safePad(hoy.getMinutes())}`;
        
        const rowsToAdd = festivos.map((dateStr, idx) => {
            const festivoId = `FES-${dateStr.replace(/\//g, '')}-${Date.now().toString().slice(-4)}`;
            return [
                festivoId,      // ID
                'SISTEMA',       // USUARIO
                'FESTIVO',       // NOMBRE COMPLETO (mostrar como FESTIVO)
                'FESTIVO',       // TIPO
                dateStr,         // FECHA INICIO
                dateStr,         // FECHA FIN
                '0',             // DIA LAB (no laborable)
                '1',             // DIA NAT
                '',              // DESCRIPCION
                today,           // FECHA SOLICITUD
                'Aprobada',      // ESTADO (automático)
                '',              // COMENTARIOS
                'Sistema'        // SOURCE
            ];
        });

        // Agregar todos los festivos
        if (rowsToAdd.length > 0) {
            await sheets.spreadsheets.values.append({
                spreadsheetId: SPREADSHEET_ID,
                range: 'AUSENCIAS-LOG!A:M',
                valueInputOption: 'USER_ENTERED',
                requestBody: { values: rowsToAdd },
            });
        }

        res.json({ success: true, saved: rowsToAdd.length });
    } catch (e) {
        console.error('Error guardando festivos:', e);
        res.status(500).json({ error: e.message });
    }
});

app.post('/api/absences/request', async (req, res) => {
    try {
        console.log('🔴 POST /api/absences/request body:', JSON.stringify(req.body, null, 2));
        const { username, name, type, startDate, endDate, description, comments } = req.body;
        const sheets = await getSheetsService();

        if (!username || !name || !type || !startDate || !endDate) {
            return res.status(400).json({ error: 'Faltan campos requeridos', received: { username, name, type, startDate, endDate } });
        }

        const start = new Date(startDate);
        const end = new Date(endDate);
        
        if (isNaN(start.getTime()) || isNaN(end.getTime())) {
            return res.status(400).json({ error: 'Fechas inválidas', startDate, endDate });
        }

        const fmt = (d) => d.toISOString().split('T')[0].replace(/-/g, '').slice(2);
        const typeCode = type === 'VACACIONES' ? 'V' : (type === 'BAJA' ? 'BJ' : 'O');
        const requestId = `${fmt(start)} ${username} ${fmt(end)} (${typeCode})`;
        const hoy = new Date();
        const requestDate = `${safePad(hoy.getDate())}/${safePad(hoy.getMonth() + 1)}/${hoy.getFullYear()} ${safePad(hoy.getHours())}:${safePad(hoy.getMinutes())}`;

        const diffTime = Math.abs(end - start);
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;

        console.log('✅ Validación OK:', { requestId, diffDays, hasComments: !!comments });

        // 1. Escribir en LOGS
        // Columnas: A=ID, B=Usuario, C=Nombre, D=Tipo, E=Inicio, F=Fin, G=Días, H=Días aprob, I=Descripción, J=Fecha solicitud, K=Estado, L=Comentarios, M=Origen
        const row = [
            requestId, username, name, type,
            `${safePad(start.getDate())}/${safePad(start.getMonth() + 1)}/${start.getFullYear()}`,
            `${safePad(end.getDate())}/${safePad(end.getMonth() + 1)}/${end.getFullYear()}`,
            diffDays, diffDays, description || '', requestDate, "Pendiente", comments || '', "Web Request"
        ];

        console.log('📝 Row a escribir:', row);

        await sheets.spreadsheets.values.append({
            spreadsheetId: SPREADSHEET_ID,
            range: 'AUSENCIAS-LOG!A:M',
            valueInputOption: 'USER_ENTERED',
            requestBody: { values: [row] },
        });
        console.log('✅ Escrito en AUSENCIAS-LOG');

        // 2. Marcar en CUADRO (opcional pero recomendado para consistencia)
        const cuadroRes = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID, range: 'AUSENCIAS-CUADRO!A1:NZ500',
        });
        const cuadroRows = cuadroRes.data.values;
        if (cuadroRows && cuadroRows.length > 2) {
            const headerDates = cuadroRows[0];
            const userRowIdx = cuadroRows.findIndex(r => r[5] === username || r[6] === name);

            if (userRowIdx !== -1) {
                const dayCode = type === 'VACACIONES' ? 'V' : (type === 'BAJA' ? 'BJ' : 'O');
                const updates = [];

                let cur = new Date(start);
                while (cur <= end) {
                    const dStr = `${safePad(cur.getDate())}/${safePad(cur.getMonth() + 1)}/${cur.getFullYear()}`;
                    const colIdx = headerDates.findIndex(h => h === dStr);
                    if (colIdx !== -1) {
                        updates.push({
                            range: `AUSENCIAS-CUADRO!${numberToColumn(colIdx)}${userRowIdx + 1}`,
                            values: [[`${dayCode}·`]] // Con el puntito de "sincronizado"
                        });
                    }
                    cur.setDate(cur.getDate() + 1);
                }

                for (const u of updates) {
                    await sheets.spreadsheets.values.update({
                        spreadsheetId: SPREADSHEET_ID, range: u.range,
                        valueInputOption: 'USER_ENTERED', requestBody: { values: u.values }
                    });
                }
            }
        }

        res.json({ success: true, id: requestId });
    } catch (e) { 
        console.error('❌ Error en POST /api/absences/request:', e.message, e.stack);
        res.status(500).json({ error: e.message, stack: e.stack }); 
    }
});

app.post('/api/absences/approve', async (req, res) => {
    try {
        const { requestId, status, comments } = req.body; // status: 'Aprobada' o 'Rechazada'
        const sheets = await getSheetsService();
        const response = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID,
            range: 'AUSENCIAS-LOG!A:M',
        });
        const rows = response.data.values;
        const rowIndex = rows.findIndex(r => r[0] === requestId);

        if (rowIndex !== -1) {
            await sheets.spreadsheets.values.update({
                spreadsheetId: SPREADSHEET_ID,
                range: `AUSENCIAS-LOG!K${rowIndex + 1}:L${rowIndex + 1}`,
                valueInputOption: 'USER_ENTERED',
                requestBody: { values: [[status, comments || ""]] }
            });
            res.json({ success: true });
        } else {
            res.status(404).json({ message: 'Solicitud no encontrada' });
        }
    } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/absences/update', async (req, res) => {
    try {
        const { requestId, type, startDate, endDate, description, note } = req.body;
        const sheets = await getSheetsService();
        const response = await sheets.spreadsheets.values.get({
            spreadsheetId: SPREADSHEET_ID,
            range: 'AUSENCIAS-LOG!A:M',
        });
        const rows = response.data.values;
        const rowIndex = rows.findIndex(r => r[0] === requestId);

        if (rowIndex !== -1) {
            const start = new Date(startDate);
            const end = new Date(endDate);
            const diffTime = Math.abs(end - start);
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;

            const currentDesc = rows[rowIndex][8] || "";
            const currentComments = rows[rowIndex][11] || "";
            const newDesc = `${description}\n[Edit: ${note}]`.trim();

            await sheets.spreadsheets.values.update({
                spreadsheetId: SPREADSHEET_ID,
                range: `AUSENCIAS-LOG!A${rowIndex + 1}:M${rowIndex + 1}`,
                valueInputOption: 'USER_ENTERED',
                requestBody: {
                    values: [[
                        requestId, // ID
                        rows[rowIndex][1], // Username
                        rows[rowIndex][2], // Name
                        type,
                        `${safePad(start.getDate())}/${safePad(start.getMonth() + 1)}/${start.getFullYear()}`,
                        `${safePad(end.getDate())}/${safePad(end.getMonth() + 1)}/${end.getFullYear()}`,
                        diffDays, // Dia Nat
                        diffDays, // Dia Lab
                        newDesc,
                        rows[rowIndex][9], // Request Date
                        "Pendiente", // STATUS REVERTS
                        currentComments,
                        "Web Edit"
                    ]]
                }
            });
            res.json({ success: true });
        } else {
            res.status(404).json({ message: 'Solicitud no encontrada' });
        }
    } catch (e) { res.status(500).json({ error: e.message }); }
});

// OBTENER LIBROS DISPONIBLES
app.get('/api/books', async (req, res) => {
    try {
        const drive = await getDriveService();
        
        // Buscar todos los spreadsheets compartidos con la cuenta de servicio
        const response = await drive.files.list({
            q: "mimeType='application/vnd.google-apps.spreadsheet' and trashed=false",
            spaces: 'drive',
            fields: 'files(id, name, description)',
            pageSize: 100,
        });
        
        const books = response.data.files.map(file => ({
            id: file.id,
            name: file.name,
            description: file.description || 'Spreadsheet'
        }));
        
        console.log(`✓ ${books.length} libros compartidos encontrados:`, books.map(b => b.name));
        res.json({ books });
    } catch (error) {
        console.error('Error al obtener libros:', error.message);
        // Fallback a libros conocidos si falla
        res.json({ books: AVAILABLE_BOOKS });
    }
});

// OBTENER HOJAS DE UN LIBRO ESPECÍFICO
app.get('/api/books/:bookId/sheets', async (req, res) => {
    try {
        const { bookId } = req.params;
        const sheets = await getSheetsService();
        
        const response = await sheets.spreadsheets.get({
            spreadsheetId: bookId,
        });
        
        if (response.data && response.data.sheets) {
            const sheetNames = response.data.sheets.map(s => s.properties.title);
            console.log(`✓ Sheets obtenidas para ${bookId}:`, sheetNames);
            return res.json({ sheets: sheetNames });
        } else {
            console.warn('No se encontraron sheets en la respuesta');
            return res.json({ sheets: [] });
        }
    } catch (e) { 
        console.error('❌ Error en /api/books/:bookId/sheets:', e.message);
        res.status(500).json({ error: e.message, sheets: [] });
    }
});

// OBTENER HOJAS DISPONIBLES (DEPRECATED - mantener por compatibilidad)
app.get('/api/sheets', async (req, res) => {
    try {
        const sheets = await getSheetsService();
        
        // Intentar obtener lista de hojas del spreadsheet principal
        const response = await sheets.spreadsheets.get({
            spreadsheetId: SPREADSHEET_ID,
        });
        
        if (response.data && response.data.sheets) {
            const sheetNames = response.data.sheets.map(s => s.properties.title);
            console.log('✓ Sheets obtenidas:', sheetNames);
            return res.json({ sheets: sheetNames });
        } else {
            console.warn('No se encontraron sheets en la respuesta');
            return res.json({ sheets: ['TRABAJADORES', 'AUSENCIAS-LOG', 'FICHAJE-OT-MES'] });
        }
    } catch (e) { 
        console.error('❌ Error en /api/sheets:', e.message);
        // Fallback con hojas conocidas
        res.json({ sheets: ['TRABAJADORES', 'AUSENCIAS-LOG', 'FICHAJE-OT-MES', 'CUADRANTE', 'CUADRANTE-PROCESADO'] });
    }
});

// OBTENER FECHAS DE UNA HOJA DE CUADRANTE
app.get('/api/sheet-dates/:bookId/:sheetName', async (req, res) => {
    try {
        const { bookId, sheetName } = req.params;
        const sheets = await getSheetsService();

        // Leer un rango más amplio para capturar las fechas que están en columnas más alejadas
        const response = await sheets.spreadsheets.values.get({
            spreadsheetId: bookId,
            range: `${sheetName}!A1:ZZ5`,
        });

        const rows = response.data.values || [];
        if (rows.length === 0) {
            return res.json({ firstDate: null, lastDate: null });
        }

        // Buscar en las primeras 5 filas la que tiene más fechas (es el header)
        let headerRow = null;
        let maxDates = 0;
        
        for (let i = 0; i < Math.min(5, rows.length); i++) {
            const row = rows[i] || [];
            const dateCount = row.filter(cell => cell && cell.toString().match(/^\d{1,2}\/\d{1,2}\/\d{4}$/)).length;
            if (dateCount > maxDates) {
                maxDates = dateCount;
                headerRow = row;
            }
        }

        if (!headerRow) {
            return res.json({ firstDate: null, lastDate: null });
        }

        // Extraer todas las fechas del header
        const dates = [];
        for (let col = 0; col < headerRow.length; col++) {
            const cellValue = (headerRow[col] || '').toString().trim();
            if (cellValue.match(/^\d{1,2}\/\d{1,2}\/\d{4}$/)) {
                dates.push(cellValue);
            }
        }

        const firstDate = dates.length > 0 ? dates[0] : null;
        const lastDate = dates.length > 0 ? dates[dates.length - 1] : null;

        console.log(`✓ Fechas de ${sheetName}: ${firstDate} - ${lastDate} (${dates.length} fechas)`);
        res.json({ firstDate, lastDate });
    } catch (error) {
        console.error('Error al obtener fechas:', error);
        res.json({ firstDate: null, lastDate: null });
    }
});

// OBTENER ÚLTIMA FECHA DE VACACIONES EN AUSENCIAS-LOG
app.get('/api/last-vacation-date/:bookId/:sheetName', async (req, res) => {
    try {
        const { bookId, sheetName } = req.params;
        const sheets = await getSheetsService();

        const response = await sheets.spreadsheets.values.get({
            spreadsheetId: bookId,
            range: `${sheetName}!A:F`,
        });

        const rows = response.data.values || [];
        let lastVacationDate = null;

        // Buscar en las columnas de fecha fin (columna F, o la que contenga fechas de fin)
        // Asumiendo que la estructura es: ID, USUARIO, NOMBRE, TIPO, FECHA_INICIO, FECHA_FIN
        for (let row = rows.length - 1; row >= 1; row--) {
            const cellValue = (rows[row][5] || '').toString().trim(); // Columna F (índice 5)
            if (cellValue.match(/^\d{1,2}\/\d{1,2}\/\d{4}$/)) {
                lastVacationDate = cellValue;
                break;
            }
        }

        console.log(`✓ Última fecha vacación en ${sheetName}: ${lastVacationDate}`);
        res.json({ lastVacationDate });
    } catch (error) {
        console.error('Error al obtener última fecha:', error);
        res.json({ lastVacationDate: null });
    }
});

// RECARGAR CUADRANTES - Procesar ausencias del cuadrante y guardar formateadas
app.post('/api/reload-quadrants', async (req, res) => {
    try {
        const { sourceBookId, sourceSheet, destBookId, destSheet, startDate, endDate } = req.body;
        if (!sourceSheet || !destSheet || !sourceBookId || !destBookId) {
            return res.status(400).json({ message: 'Falta sourceBookId, sourceSheet, destBookId o destSheet' });
        }

        const sheets = await getSheetsService();

        // 1. Leer cuadrante origen (expandido hasta columna ZZ para cubrir todo el año)
        const sourceResponse = await sheets.spreadsheets.values.get({
            spreadsheetId: sourceBookId,
            range: `${sourceSheet}!A:ZZ`,
        });
        const quadrant = sourceResponse.data.values || [];

        if (quadrant.length < 3) {
            return res.json({ success: false, message: 'Cuadrante vacío o con estructura incorrecta' });
        }

        // 1. Buscar fila de headers y columnas importantes
        let headerRowIdx = -1;
        let userColIdx = -1;
        let nameColIdx = -1;
        let firstDateCol = -1;
        
        // Buscar fila de headers (la que tiene "USUARIO")
        for (let i = 0; i < Math.min(5, quadrant.length); i++) {
            const row = quadrant[i];
            for (let col = 0; col < row.length; col++) {
                const cellValue = (row[col] || '').toString().toUpperCase().trim();
                if (cellValue === 'USUARIO') {
                    userColIdx = col;
                    headerRowIdx = i;
                }
            }
            if (headerRowIdx !== -1) break;
        }

        if (headerRowIdx === -1) {
            return res.json({ success: false, message: 'No se encontró columna USUARIO en el cuadrante' });
        }

        const headerRow = quadrant[headerRowIdx];
        
        // Ahora buscar todas las columnas importantes en la fila de headers
        for (let col = 0; col < headerRow.length; col++) {
            const cellValue = (headerRow[col] || '').toString().toUpperCase().trim();
            
            // Buscar nombre completo
            if (cellValue === 'NOMBRE Y APELLIDOS' || cellValue === 'NOMBRE COMPLETO') {
                nameColIdx = col;
            }
            
            // Buscar primera fecha
            if (cellValue.match(/^\d{1,2}\/\d{1,2}\/\d{4}$/) && firstDateCol === -1) {
                firstDateCol = col;
            }
        }

        const dataRows = quadrant.slice(headerRowIdx + 1);

        // 2. Extraer fechas de las columnas del header, filtradas por rango seleccionado
        const dates = [];
        
        // Convertir fechas de filtro a objetos Date si fueron provistas
        let filterStartDate = null;
        let filterEndDate = null;
        
        if (startDate) {
            const [d, m, y] = startDate.split('/').map(Number);
            filterStartDate = new Date(y, m - 1, d);
        }
        if (endDate) {
            const [d, m, y] = endDate.split('/').map(Number);
            filterEndDate = new Date(y, m - 1, d);
        }
        
        for (let col = firstDateCol; col < headerRow.length; col++) {
            const cellValue = (headerRow[col] || '').toString().trim();
            if (cellValue.match(/^\d{1,2}\/\d{1,2}\/\d{4}$/)) {
                // Verificar si la fecha está dentro del rango seleccionado
                const [day, month, year] = cellValue.split('/').map(Number);
                const colDate = new Date(year, month - 1, day);
                
                let includeDate = true;
                if (filterStartDate && colDate < filterStartDate) includeDate = false;
                if (filterEndDate && colDate > filterEndDate) includeDate = false;
                
                if (includeDate) {
                    // Formatear fecha con ceros
                    const formattedDate = formatDateWithZeros(cellValue);
                    dates.push({ col, date: formattedDate });
                }
            }
        }

        console.log(`✓ Rango de fechas: ${startDate || 'inicio'} - ${endDate || 'fin'}, ${dates.length} fechas a procesar`);
        console.log(`✓ Header en fila ${headerRowIdx}, usuario: col ${userColIdx}, nombre: col ${nameColIdx}, primeras fechas: col ${firstDateCol}`);

        // 2.5. Leer festivos de la fila 2 (o buscar fila que dice "FESTIVOS")
        const holidays = new Set();
        let holidayRowIdx = -1;
        
        for (let i = 0; i < Math.min(10, quadrant.length); i++) {
            const row = quadrant[i];
            const firstCell = (row[0] || '').toString().toUpperCase().trim();
            if (firstCell === 'FESTIVOS' || firstCell === 'F') {
                holidayRowIdx = i;
                break;
            }
        }
        
        if (holidayRowIdx !== -1) {
            const holidayRow = quadrant[holidayRowIdx];
            for (let col = firstDateCol; col < headerRow.length; col++) {
                const cellValue = (holidayRow[col] || '').toString().toUpperCase().trim();
                if (cellValue === 'F') {
                    const dateValue = (headerRow[col] || '').toString().trim();
                    if (dateValue.match(/^\d{1,2}\/\d{1,2}\/\d{4}$/)) {
                        // Formatear fecha de festivo con ceros
                        holidays.add(formatDateWithZeros(dateValue));
                    }
                }
            }
            console.log(`✓ Festivos detectados: ${holidays.size} días`);
        }

        // 3. Procesar ausencias por fila (cada fila = un usuario)
        const absences = [];
        for (let rowIdx = 0; rowIdx < dataRows.length; rowIdx++) {
            const row = dataRows[rowIdx];
            
            // Obtener usuario y nombre completo de las columnas identificadas
            const userName = (row[userColIdx] || '').toString().trim();
            const fullName = nameColIdx !== -1 ? (row[nameColIdx] || '').toString().trim() : userName;
            
            if (!userName || userName.toUpperCase() === 'FESTIVOS') continue; // Skip filas vacías o festivos

            let i = 0;
            while (i < dates.length) {
                const { col: dateCol, date } = dates[i];
                const cellValue = (row[dateCol] || '').toString().toUpperCase().trim();

                if (cellValue === 'V' || cellValue === 'PR' || cellValue === 'B') {
                    // Encontró una ausencia, agrupar días consecutivos (considerando fines de semana y festivos)
                    const type = cellValue === 'V' ? 'Vacaciones' : cellValue === 'PR' ? 'Permiso' : 'Baja';
                    const [day, month, year] = date.split('/').map(Number);
                    let startDate = new Date(year, month - 1, day);
                    let endDate = new Date(year, month - 1, day);
                    let workDays = 1; // Contador de días laborables
                    let j = i + 1;

                    // Buscar días consecutivos con el mismo tipo, CONSIDERANDO fines de semana y festivos
                    while (j < dates.length) {
                        const { col: nextDateCol, date: nextDate } = dates[j];
                        const nextValue = row[nextDateCol]?.toString().toUpperCase().trim() || '';
                        
                        const [nextDay, nextMonth, nextYear] = nextDate.split('/').map(Number);
                        const nextDateObj = new Date(nextYear, nextMonth - 1, nextDay);
                        
                        // Calcular diferencia en días
                        const daysDiff = (nextDateObj - endDate) / (1000 * 60 * 60 * 24);
                        
                        // Verificar si hay solo fines de semana o festivos entre medio
                        let onlyWeekendsOrHolidaysInBetween = true;
                        if (daysDiff > 1) {
                            for (let d = 1; d < daysDiff; d++) {
                                const checkDate = new Date(endDate);
                                checkDate.setDate(checkDate.getDate() + d);
                                const checkDateStr = `${safePad(checkDate.getDate())}/${safePad(checkDate.getMonth() + 1)}/${checkDate.getFullYear()}`;
                                const dayOfWeek = checkDate.getDay();
                                
                                // Si no es fin de semana (0=domingo, 6=sábado) ni festivo, rompe la cadena
                                if (dayOfWeek !== 0 && dayOfWeek !== 6 && !holidays.has(checkDateStr)) {
                                    onlyWeekendsOrHolidaysInBetween = false;
                                    break;
                                }
                            }
                        }
                        
                        // Si es el mismo tipo Y (es consecutivo O solo hay fines de semana/festivos entre medio)
                        if (nextValue === cellValue && (daysDiff === 1 || onlyWeekendsOrHolidaysInBetween)) {
                            endDate = nextDateObj;
                            workDays++;
                            j++;
                        } else if (nextValue === '' || nextValue === 'FS' || holidays.has(nextDate)) {
                            // Si es fin de semana o festivo, continuar sin romper la cadena
                            j++;
                        } else {
                            break;
                        }
                    }

                    // Calcular días naturales (rango completo)
                    const totalDays = Math.ceil((endDate - startDate) / (1000 * 60 * 60 * 24)) + 1;

                    // Determinar estado: Aprobada si comenzó antes de hoy, Pendiente si comienza hoy o después
                    const today = new Date();
                    today.setHours(0, 0, 0, 0);
                    const status = startDate < today ? 'Aprobada' : 'Pendiente';

                    // Generar ID único: FECHA1-USUARIO-FECHA2-(INICIAL) o FECHA1-USUARIO-(INICIAL) para un día
                    const typeInitial = cellValue === 'V' ? 'V' : cellValue === 'PR' ? 'P' : 'B';
                    const startDateStr = `${safePad(startDate.getFullYear() % 100)}${safePad(startDate.getMonth() + 1)}${safePad(startDate.getDate())}`;
                    const endDateStr = `${safePad(endDate.getFullYear() % 100)}${safePad(endDate.getMonth() + 1)}${safePad(endDate.getDate())}`;
                    const id = totalDays === 1 
                        ? `${startDateStr}-${userName}-(${typeInitial})`
                        : `${startDateStr}-${userName}-${endDateStr}-(${typeInitial})`;

                    absences.push({
                        username: userName,
                        name: fullName,
                        type,
                        startDate: `${safePad(startDate.getDate())}/${safePad(startDate.getMonth() + 1)}/${startDate.getFullYear()}`,
                        endDate: `${safePad(endDate.getDate())}/${safePad(endDate.getMonth() + 1)}/${endDate.getFullYear()}`,
                        daysNat: totalDays,
                        daysLab: workDays, // Días laborables (sin contar fines de semana/festivos)
                        reason: `Importado de cuadrante ${sourceSheet}`,
                        status: status,
                        id: id
                    });

                    i = j;
                } else {
                    i++;
                }
            }
        }

        console.log(`✓ ${absences.length} ausencias encontradas`);

        // 4. Guardar en hoja destino
        const rows = absences.map(a => [
            a.id, // ID único generado: AAMMDD-USER-TIPO-DURACION
            a.username,
            a.name,
            a.type,
            a.startDate,
            a.endDate,
            a.daysNat,
            a.daysLab,
            a.reason,
            new Date().toLocaleDateString('es-ES'),
            a.status,
            '',
            ''
        ]);

        if (rows.length > 0) {
            await sheets.spreadsheets.values.append({
                spreadsheetId: destBookId,
                range: `${destSheet}!A:M`,
                valueInputOption: 'USER_ENTERED',
                requestBody: {
                    values: rows
                }
            });
        }

        res.json({ 
            success: true, 
            message: `${absences.length} ausencias procesadas y guardadas en ${destSheet}` 
        });
    } catch (e) { 
        console.error(e);
        res.status(500).json({ error: e.message }); 
    }
});

app.listen(3001, () => console.log('Bitherm Proxy Running on 3001'));
