const express = require('express');
const cors = require('cors');
const { google } = require('googleapis');
const path = require('path');
const fs = require('fs');

const app = express();
app.use(cors());
app.use(express.json());

const SPREADSHEET_ID = '1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4';
const CREDENTIALS_PATH = path.join(__dirname, '../../app/src/main/assets/credentials_default.json');
const VARIABLES_PATH = path.join(__dirname, '../../variables.json');

async function getSheetsService() {
    const auth = new google.auth.GoogleAuth({
        keyFile: CREDENTIALS_PATH,
        scopes: ['https://www.googleapis.com/auth/spreadsheets'],
    });
    return google.sheets({ version: 'v4', auth });
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
            range: 'TRABAJADORES!A2:T',
        });
        const rows = response.data.values;
        if (!rows || rows.length < 2) return res.status(404).json({ message: 'No se encontraron trabajadores' });

        const headers = rows[0];
        const dataRows = rows.slice(1);
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

app.listen(3001, () => console.log('Bitherm Proxy Running on 3001'));
