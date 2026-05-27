const { app, BrowserWindow, ipcMain, shell } = require('electron');
const path = require('path');
const os = require('os');
const fs = require('fs');
const { Client } = require('minecraft-launcher-core');
const msmc = require('msmc');
const axios = require('axios');
const { calculateHash, downloadGdriveZip, extractZip, syncModsFromGithub } = require('./github_sync');

let mainWindow;
const launcher = new Client();

async function getGithubRawBase() {
    try {
        const res = await axios.get("https://api.github.com/repos/mygus3377/NogeonEconomyLand/commits/main", {
            headers: {
                'User-Agent': 'NoGeon-Launcher',
                'Cache-Control': 'no-cache',
                'Pragma': 'no-cache'
            },
            timeout: 5000
        });
        if (res.data && res.data.sha) {
            const sha = res.data.sha;
            console.log(`[GitHub Sync] Successfully resolved latest commit SHA: ${sha}`);
            return `https://raw.githubusercontent.com/mygus3377/NogeonEconomyLand/${sha}`;
        }
    } catch (err) {
        console.warn(`[GitHub Sync] Failed to fetch latest commit SHA, falling back to main branch: ${err.message}`);
    }
    return "https://raw.githubusercontent.com/mygus3377/NogeonEconomyLand/main";
}

// 마인크래프트가 설치되고 동기화될 전용 루트 경로 (%APPDATA%/.nogeon_launcher)
const minecraftDir = path.join(os.homedir(), 'AppData', 'Roaming', '.nogeon_launcher');
const localModsPath = path.join(minecraftDir, 'mods');

function findJavaExecutable(rootDir) {
    if (!fs.existsSync(rootDir)) {
        return null;
    }
    const pending = [rootDir];
    while (pending.length > 0) {
        const current = pending.pop();
        let entries;
        try {
            entries = fs.readdirSync(current, { withFileTypes: true });
        } catch (err) {
            continue;
        }
        for (const entry of entries) {
            const fullPath = path.join(current, entry.name);
            if (entry.isDirectory()) {
                pending.push(fullPath);
                continue;
            }
            if (entry.isFile() && entry.name.toLowerCase() === 'java.exe' && path.basename(current).toLowerCase() === 'bin') {
                return fullPath;
            }
        }
    }
    return null;
}

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 800,
        height: 500,
        resizable: false,
        frame: false, // 헥스테크 프리미엄 연출을 위한 타이틀바 제거
        show: false,  // 초기 화면 깜빡임 방지용 숨김 처리
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: false
        }
    });

    mainWindow.loadFile('index.html');

    // 윈도우 렌더링 준비 완료 시 확실히 화면 전면으로 끌어올리고 포커스 부여
    mainWindow.once('ready-to-show', () => {
        mainWindow.show();
        mainWindow.focus();
        // Windows OS 창 레이어 맨 위로 강제 인양 트릭
        mainWindow.setAlwaysOnTop(true);
        mainWindow.setAlwaysOnTop(false);
    });

    // 🛠️ 로그인 에러 디버깅을 위한 개발자 도구 자동 활성화 (테스트 후 주석 처리 완료)
    // mainWindow.webContents.openDevTools();
}

app.whenReady().then(() => {
    createWindow();

    app.on('activate', () => {
        if (BrowserWindow.getAllWindows().length === 0) createWindow();
    });
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit();
});

// IPC 1: Microsoft 로그인 처리 (msmc v3 API 반영)
ipcMain.handle('auth:login', async () => {
    try {
        // fastLaunch returns a promise that resolves to a result object
        const result = await msmc.fastLaunch("electron", (update) => {
            console.log("[MSMC Login Update]", update);
        }, "select_account");
        
        if (msmc.errorCheck(result)) {
            throw new Error(result.reason || "Microsoft Login failed");
        }
        
        // Get MCLC-compatible profile format
        const profile = msmc.getMCLC().getAuth(result);
        
        // 세션 정보 캐싱 저장
        const sessionFilePath = path.join(minecraftDir, 'auth_session.json');
        if (!fs.existsSync(minecraftDir)) {
            fs.mkdirSync(minecraftDir, { recursive: true });
        }
        fs.writeFileSync(sessionFilePath, JSON.stringify(profile, null, 2), 'utf8');
        console.log(`[Auth Session] Cached profile successfully: ${profile.name}`);
        
        return {
            success: true,
            profile: profile
        };
    } catch (err) {
        return {
            success: false,
            error: err.message
        };
    }
});

// IPC 2: 하이브리드(구글 최초설치 + 깃허브 무결성) 동기화 처리
ipcMain.on('game:sync', async (event) => {
    try {
        const GITHUB_RAW_BASE = await getGithubRawBase();
        const defaultFileId = "1_kV4TFGyr9QcNelaUtLtXci3VMznxcNf"; // 기본 구글드라이브 폴백 ID
        
        event.sender.send('status:update', { 
            status: 'syncing', 
            message: '[설치 검증] 깃허브에서 서버 패치 설정 데이터를 조회 중...', 
            percent: 5 
        });

        // 1. 깃허브에서 실시간 manifest.json 수신하여 구글 드라이브 ID 동적 추출
        let dynamicFileId = defaultFileId;
        try {
            const res = await axios.get(`${GITHUB_RAW_BASE}/manifest.json?nocache=${Date.now()}`);
            if (res.data && res.data.gdrive_file_id) {
                dynamicFileId = res.data.gdrive_file_id;
                console.log(`[Dynamic Config] Loaded dynamic Google Drive file ID from GitHub: ${dynamicFileId}`);
            }
        } catch (manifestErr) {
            console.log(`[Dynamic Config Warning] Failed to load dynamic file ID, falling back to local default: ${manifestErr.message}`);
        }

        const tempZipPath = path.join(minecraftDir, 'modpack.zip');

        // 2단계: 로컬 mods 폴더가 없거나 모드 파일 개수가 비정상적으로 적은 경우 (예: 10개 미만) ➡️ 구글 드라이브 ZIP 최초 전체 설치
        const getLocalModCount = () => {
            if (!fs.existsSync(localModsPath)) return 0;
            try {
                return fs.readdirSync(localModsPath).filter(f => f.endsWith('.jar')).length;
            } catch (e) {
                return 0;
            }
        };
        const isFreshInstall = getLocalModCount() < 10;

        if (isFreshInstall) {
            event.sender.send('status:update', { 
                status: 'downloading', 
                message: '[최초 설치] 구글 드라이브에서 모드팩 전체 패키지 수신 중...', 
                percent: 10 
            });
            
            if (!fs.existsSync(minecraftDir)) {
                fs.mkdirSync(minecraftDir, { recursive: true });
            }

            // 구글 드라이브에서 ZIP 받기
            await downloadGdriveZip(dynamicFileId, tempZipPath, (percent) => {
                event.sender.send('status:update', { 
                    status: 'downloading', 
                    message: `[최초 설치] 모드팩 다운로드 중... (${percent}%)`, 
                    percent 
                });
            });

            // 압축 해제
            event.sender.send('status:update', { 
                status: 'extracting', 
                message: '[최초 설치] 모드팩 압축 해제 및 기반 환경 구축 중...', 
                percent: 90 
            });
            extractZip(tempZipPath, minecraftDir);

            // 임시 ZIP 제거
            if (fs.existsSync(tempZipPath)) {
                fs.unlinkSync(tempZipPath);
            }
            
            event.sender.send('status:update', { 
                status: 'extracting', 
                message: '최초 설치 성공! 이어서 깃허브 무결성 검증을 시작합니다.', 
                percent: 95 
            });
        }

        // 3단계: 깃허브 최신 manifest.json 기반 무결성 체크 및 핀셋 부분 패치 실행
        await syncModsFromGithub(localModsPath, (logMessage, logType = 'info') => {
            // 진행 로그를 렌더러(콘솔)로 실시간 송출
            event.sender.send('status:update', { 
                status: logType === 'error' ? 'error' : 'syncing', 
                message: logMessage,
                percent: logType === 'system' ? 100 : 95
            });
        }, GITHUB_RAW_BASE);

        event.sender.send('status:update', { 
            status: 'ready', 
            message: '서버 연동 동기화 완료! 즐겁게 게임을 켜주세요.', 
            percent: 100 
        });

    } catch (err) {
        event.sender.send('status:update', { 
            status: 'error', 
            message: `동기화 실패: ${err.message}`, 
            percent: 0 
        });
    }
});

// IPC 3: 마인크래프트 Forge 1.20.1 실행 처리
ipcMain.on('game:launch', async (event, options) => {
    const { profile, maxMemory } = options;
    
    event.sender.send('status:update', { status: 'launching', message: '마인크래프트 실행 환경 준비 중...', percent: 10 });

    const forgeVersion = "1.20.1-47.4.10";
    const forgeInstallerPath = path.join(minecraftDir, `forge-${forgeVersion}-installer.jar`);

    // ☕ 100% 무설정 자동 자바 17 환경(JRE) 구축 검사 (최초 1회)
    const jreDirectory = path.join(minecraftDir, 'jre17');
    const localJavaPath = path.join(jreDirectory, 'jdk-17.0.10+7-jre', 'bin', 'java.exe');
    const tempJreZipPath = path.join(minecraftDir, 'jre17.zip');
    let resolvedJavaPath = fs.existsSync(localJavaPath) ? localJavaPath : findJavaExecutable(jreDirectory);

    if (!resolvedJavaPath) {
        event.sender.send('status:update', { 
            status: 'launching', 
            message: '[자바 구성] 포터블 자바 JRE 17 환경이 없습니다. 자동 다운로드를 개시합니다... (최초 1회)', 
            percent: 12 
        });
        
        try {
            const jreUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.10%2B7/OpenJDK17U-jre_x64_windows_hotspot_17.0.10_7.zip";
            console.log(`[Java Config] Auto downloading JRE 17 from: ${jreUrl}`);
            
            // 40MB 용량 경량 JRE 다운로드
            const responseJre = await axios({
                method: 'get',
                url: jreUrl,
                responseType: 'stream'
            });
            
            const totalJreLength = responseJre.headers['content-length'];
            let downloadedJreLength = 0;
            const writerJre = fs.createWriteStream(tempJreZipPath);
            
            await new Promise((resolve, reject) => {
                responseJre.data.on('data', (chunk) => {
                    downloadedJreLength += chunk.length;
                    if (totalJreLength) {
                        const percent = Math.round((downloadedJreLength / totalJreLength) * 100);
                        event.sender.send('status:update', { 
                            status: 'launching', 
                            message: `[자바 구성] 포터블 자바 JRE 17 다운로드 중... (${percent}%)`, 
                            percent: 12 
                        });
                    }
                });
                responseJre.data.pipe(writerJre);
                writerJre.on('finish', () => resolve());
                writerJre.on('error', (err) => reject(err));
            });
            
            // 다운로드 완료 시 압축 해제
            event.sender.send('status:update', { 
                status: 'launching', 
                message: '[자바 구성] 포터블 자바 환경 압축 해제 중...', 
                percent: 14 
            });
            
            console.log(`[Java Config] Extracting JRE 17 to: ${jreDirectory}`);
            extractZip(tempJreZipPath, jreDirectory);
            
            // 임시 zip 삭제
            if (fs.existsSync(tempJreZipPath)) {
                fs.unlinkSync(tempJreZipPath);
            }
            resolvedJavaPath = fs.existsSync(localJavaPath) ? localJavaPath : findJavaExecutable(jreDirectory);
            if (!resolvedJavaPath) {
                throw new Error(`Portable Java was extracted, but java.exe was not found under ${jreDirectory}`);
            }
            console.log(`[Java Config] Portable JRE 17 configured successfully: ${resolvedJavaPath}`);
        } catch (jreErr) {
            event.sender.send('status:update', { 
                status: 'error', 
                message: `포터블 자바 환경 구성 실패: ${jreErr.message}. 수동 자바 설치가 필요할 수 있습니다.`, 
                percent: 0 
            });
            return;
        }
    }

    // Forge 인스톨러 JAR 자동 다운로드 로직 (최초 1회)
    resolvedJavaPath = resolvedJavaPath || (fs.existsSync(localJavaPath) ? localJavaPath : findJavaExecutable(jreDirectory));
    if (!resolvedJavaPath) {
        event.sender.send('status:update', {
            status: 'error',
            message: `Java executable was not found. Please run launcher reset/sync or remove this folder and try again: ${jreDirectory}`,
            percent: 0
        });
        return;
    }

    // 🖥️ 윈도우 환경인 경우, DirectX UserGpuPreferences 레지스트리에 JRE를 등록하여 외장 그래픽 카드(고성능 GPU) 사용을 강제합니다.
    if (process.platform === 'win32' && resolvedJavaPath) {
        const javawPath = resolvedJavaPath.replace(/java\.exe$/i, 'javaw.exe');
        if (fs.existsSync(javawPath)) {
            try {
                const { exec } = require('child_process');
                const regCmd = `reg add "HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences" /v "${javawPath}" /t REG_SZ /d "GpuPreference=2;" /f`;
                exec(regCmd, (err) => {
                    if (err) console.warn("[Registry Config] Failed to force high performance GPU:", err.message);
                });
            } catch (regErr) {
                console.warn("[Registry Config] Registry write failed:", regErr.message);
            }
        }
    }

    if (!fs.existsSync(forgeInstallerPath)) {
        event.sender.send('status:update', { 
            status: 'launching', 
            message: `[MCLC] 포지 엔진 설치 파일(${forgeVersion})을 다운로드하는 중... (최초 1회)`, 
            percent: 15 
        });
        
        try {
            const forgeUrl = `https://maven.minecraftforge.net/net/minecraftforge/forge/${forgeVersion}/forge-${forgeVersion}-installer.jar`;
            console.log(`[Forge Downloader] Downloading Forge installer from: ${forgeUrl}`);
            
            const response = await axios({
                method: 'get',
                url: forgeUrl,
                responseType: 'stream'
            });
            
            const writer = fs.createWriteStream(forgeInstallerPath);
            await new Promise((resolve, reject) => {
                response.data.pipe(writer);
                writer.on('finish', () => resolve());
                writer.on('error', (err) => reject(err));
            });
            
            console.log(`[Forge Downloader] Forge installer downloaded successfully: ${forgeInstallerPath}`);
        } catch (downloadErr) {
            event.sender.send('status:update', { 
                status: 'error', 
                message: `포지 설치 파일 다운로드 실패: ${downloadErr.message}`, 
                percent: 0 
            });
            return;
        }
    }

    // MCLC 이벤트 리스너: 로깅 처리
    launcher.on('debug', (e) => {
        console.log(`[MCLC DEBUG] ${e}`);
        // MCLC 내부 디버그 로그(다운로드 시도, 체크 등)를 UI로 실시간 송출하여 멈춘 것처럼 보이는 현상 해소
        event.sender.send('status:update', { 
            status: 'launching', 
            message: e, 
            percent: undefined 
        });
    });
    launcher.on('data', (e) => {
        // 인게임 실시간 로그를 UI에 한 줄씩 던져줌
        event.sender.send('status:update', { status: 'running', message: e.trim(), percent: 100 });
    });

    launcher.on('progress', (e) => {
        const percent = Math.round((e.task / e.total) * 100);
        event.sender.send('status:update', { 
            status: 'launching', 
            message: `${e.type} 리소스 준비 중...`, 
            percent 
        });
    });

    // MCLC 구동 옵션 빌드
    const launchOptions = {
        authorization: profile, // MS 로그인 프로필 주입
        root: minecraftDir,
        version: {
            number: "1.20.1",
            type: "release"
        },
        // ⭐ Forge 인스톨러 JAR의 절대 경로를 주입하여 adm-zip의 "Invalid filename" 오류 원천 차단!
        forge: forgeInstallerPath, 
        memory: {
            max: maxMemory || "8G", // 기본값 8G로 상향하여 렉 방지
            min: "2G"
        },
        // 🚀 대규모 모드팩 프레임 드랍 및 메모리 렉(GC 스터터링) 방지를 위한 G1GC 최적화 JVM 인수 강제 주입
        customArgs: [
            "-XX:+UseG1GC",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:MaxGCPauseMillis=50",
            "-XX:+DisableExplicitGC",
            "-XX:G1NewSizePercent=30",
            "-XX:G1MaxNewSizePercent=40",
            "-XX:G1ReservePercent=15",
            "-XX:G1HeapRegionSize=32m",
            "-XX:G1MixedGCCountTarget=8",
            "-XX:InitiatingHeapOccupancyPercent=15",
            "-XX:G1MixedGCLiveThresholdPercent=90",
            "-XX:G1RSetUpdatingPauseTimePercent=5",
            "-XX:SurvivorRatio=32",
            "-XX:+PerfDisableSharedMem",
            "-XX:MaxTenuringThreshold=1"
        ]
    };

    // ☕ 로컬 포터블 JRE 17이 완비되어 있다면 MCLC에 강제 주입하여 무설정 기동!
    launchOptions.javaPath = resolvedJavaPath;
    console.log(`[Java Config] Using portable JRE 17: ${resolvedJavaPath}`);

    // 🛠️ 대규모 모드팩 지형 로딩 렉(청크 로딩 랙) 방지를 위한 options.txt 내 Mipmap level 자동 최적화 (0 설정)
    const optionsPath = path.join(minecraftDir, 'options.txt');
    try {
        if (fs.existsSync(optionsPath)) {
            let optionsContent = fs.readFileSync(optionsPath, 'utf8');
            if (optionsContent.includes('mipmapLevels:')) {
                optionsContent = optionsContent.replace(/mipmapLevels:\d+/g, 'mipmapLevels:0');
            } else {
                optionsContent += '\nmipmapLevels:0\n';
            }
            fs.writeFileSync(optionsPath, optionsContent, 'utf8');
            console.log("[Optimize Options] Successfully optimized mipmapLevels to 0 in options.txt");
        } else {
            fs.writeFileSync(optionsPath, 'mipmapLevels:0\n', 'utf8');
            console.log("[Optimize Options] Created options.txt with mipmapLevels:0");
        }
    } catch (err) {
        console.error("[Optimize Options] Failed to modify options.txt:", err.message);
    }

    try {
        launcher.launch(launchOptions);
        
        launcher.once('spawn', () => {
            console.log("[MCLC] Minecraft spawned successfully.");
            event.sender.send('game:launched');
        });

        launcher.once('close', (code) => {
            console.log(`[MCLC] Minecraft process exited with code: ${code}`);
            event.sender.send('game:closed', code);
        });
    } catch (err) {
        event.sender.send('status:update', { status: 'error', message: `구동 에러: ${err.message}`, percent: 0 });
    }
});

// IPC 4: 윈도우 최소화 처리
ipcMain.on('window:minimize', () => {
    if (mainWindow) {
        mainWindow.minimize();
    }
});

// IPC 5: 폴더 열기 (게임 폴더, 크래시 폴더)
ipcMain.on('util:open-folder', (event, type) => {
    let targetPath = minecraftDir;
    if (type === 'crash_reports') {
        targetPath = path.join(minecraftDir, 'crash-reports');
    }
    
    if (!fs.existsSync(targetPath)) {
        fs.mkdirSync(targetPath, { recursive: true });
    }
    
    shell.openPath(targetPath)
        .then(err => {
            if (err) console.error("[Shell Open Path Error]", err);
        });
});

// IPC 6: 최신 로그 파일 읽기 (latest.log)
ipcMain.handle('util:read-log', async () => {
    const logPath = path.join(minecraftDir, 'logs', 'latest.log');
    try {
        if (!fs.existsSync(logPath)) {
            return { success: false, error: "최신 로그 파일이 아직 생성되지 않았습니다. 게임을 실행해 주세요." };
        }
        
        // 너무 큰 파일일 수 있으므로 마지막 100KB만 읽도록 안정장치 구현
        const stats = fs.statSync(logPath);
        const fileSize = stats.size;
        
        let logContent = "";
        if (fileSize > 100 * 1024) {
            const buffer = Buffer.alloc(100 * 1024);
            const fd = fs.openSync(logPath, 'r');
            fs.readSync(fd, buffer, 0, 100 * 1024, fileSize - 100 * 1024);
            fs.closeSync(fd);
            logContent = buffer.toString('utf8');
            logContent = "[일부 로그 생략됨 - 파일이 너무 큽니다]\n" + logContent;
        } else {
            logContent = fs.readFileSync(logPath, 'utf8');
        }
        
        return { success: true, content: logContent };
    } catch (err) {
        return { success: false, error: err.message };
    }
});

// IPC 7: 런처 안전 초기화 (데이터 및 세션 보존)
ipcMain.handle('util:reset', async () => {
    try {
        // 싱글 세이브 및 키설정 설정을 유지하고 꼬인 모드 파일만 리셋하기 위해 mods 폴더만 삭제
        const foldersToDelete = ['mods'];
        
        for (const folder of foldersToDelete) {
            const targetPath = path.join(minecraftDir, folder);
            if (fs.existsSync(targetPath)) {
                fs.rmSync(targetPath, { recursive: true, force: true });
            }
        }
        
        // zip 캐시도 있으면 지움
        const tempZipPath = path.join(minecraftDir, 'modpack.zip');
        if (fs.existsSync(tempZipPath)) {
            fs.unlinkSync(tempZipPath);
        }
        
        // 중요: auth_session.json(로그인 상태)은 절대 삭제하지 않고 유지함
        
        return { success: true };
    } catch (err) {
        return { success: false, error: err.message };
    }
});

// IPC 8: 실시간 모드 해시 자동 업데이트 감지 핸들러
ipcMain.handle('game:check-update', async () => {
    try {
        const GITHUB_RAW_BASE = await getGithubRawBase();
        
        // 1. 깃허브 최신 manifest.json 수신
        const res = await axios.get(`${GITHUB_RAW_BASE}/manifest.json?nocache=${Date.now()}`);
        const manifest = res.data;
        const serverMods = manifest.mods || [];

        // 2. 로컬에 mods 폴더가 아예 없거나 개수가 10개 미만이면 무조건 최초 설치/업데이트 필요
        if (!fs.existsSync(localModsPath)) {
            return { updateRequired: true, reason: "최초 전체 패키지 설치가 필요합니다." };
        }
        
        const getLocalModCount = () => {
            try {
                return fs.readdirSync(localModsPath).filter(f => f.endsWith('.jar')).length;
            } catch (e) {
                return 0;
            }
        };
        
        if (getLocalModCount() < 10) {
            return { updateRequired: true, reason: "기본 모드팩 설치가 온전하지 않습니다." };
        }

        // 3. 서버 manifest.json 모드들과 로컬 파일들의 해시값 비교
        for (const serverMod of serverMods) {
            const localPath = path.join(localModsPath, serverMod.name);
            
            // 파일이 아예 없거나
            if (!fs.existsSync(localPath)) {
                return { 
                    updateRequired: true, 
                    reason: `신규 추가된 모듈이 누락되었습니다: ${serverMod.name}` 
                };
            }
            
            // 파일 해시(SHA-256)가 다르면 업데이트 필요
            const localHash = calculateHash(localPath);
            if (localHash !== serverMod.sha256.toLowerCase()) {
                return { 
                    updateRequired: true, 
                    reason: `모드 업데이트가 필요합니다: ${serverMod.name}` 
                };
            }
        }

        // 모든 모드의 파일 및 해시 일치 확인 완료
        return { updateRequired: false };
    } catch (err) {
        console.error("[Check Update Error]", err);
        // 네트워크 연결 등의 에러 시에는 안전을 위해 업데이트 동기화가 가능하도록 true 처리
        return { updateRequired: true, reason: `업데이트 검증 실패: ${err.message}` };
    }
});

// IPC 9: 깃허브 최신 patch.txt 네이티브 수신 채널 (CORS/CSP 방지 완벽 라우팅)
ipcMain.handle('util:read-patch', async () => {
    try {
        const GITHUB_RAW_BASE = await getGithubRawBase();
        const patchUrl = `${GITHUB_RAW_BASE}/patch.txt`;
        const response = await axios.get(`${patchUrl}?nocache=${Date.now()}`);
        return { success: true, content: response.data };
    } catch (err) {
        console.error("[Read Patch Error]", err);
        return { success: false, error: err.message };
    }
});

// IPC 9.5: 로그아웃 처리 (세션 파일 삭제)
ipcMain.handle('auth:logout', async () => {
    const sessionFilePath = path.join(minecraftDir, 'auth_session.json');
    try {
        if (fs.existsSync(sessionFilePath)) {
            fs.unlinkSync(sessionFilePath);
        }
        console.log("[Auth Session] Logged out successfully (session file deleted)");
        return { success: true };
    } catch (err) {
        console.error("[Auth Session Logout Error]", err);
        return { success: false, error: err.message };
    }
});

// IPC 10: 로컬 세션 기반 자동 로그인 및 만료 토큰 백그라운드 자동 재로그인 처리
ipcMain.handle('auth:auto-login', async () => {
    const sessionFilePath = path.join(minecraftDir, 'auth_session.json');
    try {
        if (!fs.existsSync(sessionFilePath)) {
            return { success: false };
        }
        
        const sessionData = fs.readFileSync(sessionFilePath, 'utf8');
        const profile = JSON.parse(sessionData);
        
        // 세션 데이터 구조 정밀 체크 (비정상 세션 예방)
        if (!profile.name || !profile.uuid || !profile.access_token) {
            if (fs.existsSync(sessionFilePath)) fs.unlinkSync(sessionFilePath); // 손상된 세션은 파괴
            return { success: false };
        }

        // MSMC 백그라운드 토큰 자동 갱신 (세션 만료 자동 갱신 및 재로그인 실체화)
        try {
            console.log(`[Auth Session] Checking session validity and refreshing Microsoft token for player: ${profile.name}...`);
            const refreshedProfile = await msmc.getMCLC().refresh(profile, (update) => {
                console.log("[MSMC Auto Login Status]", update);
            });
            
            // 갱신된 최신 토큰 정보를 다시 캐싱 저장
            fs.writeFileSync(sessionFilePath, JSON.stringify(refreshedProfile, null, 2), 'utf8');
            console.log(`[Auth Session] Auto login and token refresh successful for player: ${refreshedProfile.name}`);
            
            return {
                success: true,
                profile: refreshedProfile
            };
        } catch (refreshErr) {
            console.warn(`[Auth Session Refresh Failed] Token refresh expired or failed: ${refreshErr.message}`);
            // 세션이 완전히 만료된 경우 세션 파일을 삭제하여 재로그인을 유도
            if (fs.existsSync(sessionFilePath)) fs.unlinkSync(sessionFilePath);
            return { success: false, error: "Session expired. Please log in again." };
        }
    } catch (err) {
        console.error("[Auth Session Error]", err);
        return { success: false };
    }
});

// IPC 11: 런처 자체 업데이트 검증 및 다운로드/재시작 핸들러
ipcMain.handle('launcher:check-self-update', async (event) => {
    try {
        const GITHUB_RAW_BASE = await getGithubRawBase();
        const currentVersion = app.getVersion(); // package.json의 version (예: "1.0.0")

        // 1. 깃허브 최신 manifest.json 수신
        const res = await axios.get(`${GITHUB_RAW_BASE}/manifest.json?nocache=${Date.now()}`);
        const manifest = res.data;
        const latestVersion = manifest.launcher_version;
        const downloadUrl = manifest.launcher_url;
        const gdriveId = manifest.launcher_gdrive_id;

        if (!latestVersion) {
            return { updateRequired: false };
        }

        // 버전 비교 헬퍼 함수
        const isNewerVersion = (current, latest) => {
            const curParts = current.split('.').map(Number);
            const latParts = latest.split('.').map(Number);
            for (let i = 0; i < Math.max(curParts.length, latParts.length); i++) {
                const cur = curParts[i] || 0;
                const lat = latParts[i] || 0;
                if (lat > cur) return true;
                if (cur > lat) return false;
            }
            return false;
        };

        if (isNewerVersion(currentVersion, latestVersion)) {
            console.log(`[Self Update] Newer launcher version detected: ${latestVersion} (Current: ${currentVersion})`);
            event.sender.send('status:update', {
                status: 'updating_launcher',
                message: `[런처 패치] 런처의 새로운 버전(${latestVersion})이 발견되었습니다. 자동 업데이트를 시작합니다...`,
                percent: 0
            });

            // 2. 새로운 실행 파일 다운로드 (구글 드라이브 ID 우선 연동, 없으면 일반 URL 연동)
            const tempUpdatePath = path.join(minecraftDir, 'launcher_update.exe');
            
            if (gdriveId) {
                console.log(`[Self Update] Downloading update from Google Drive ID: ${gdriveId}`);
                await downloadGdriveZip(gdriveId, tempUpdatePath, (percent) => {
                    event.sender.send('status:update', {
                        status: 'updating_launcher',
                        message: `[런처 패치] 최신 버전 다운로드 중... (${percent}%)`,
                        percent
                    });
                });
            } else if (downloadUrl) {
                console.log(`[Self Update] Downloading update from direct URL: ${downloadUrl}`);
                const response = await axios({
                    method: 'get',
                    url: downloadUrl,
                    responseType: 'stream'
                });

                const totalLength = response.headers['content-length'];
                let downloadedLength = 0;
                const writer = fs.createWriteStream(tempUpdatePath);

                await new Promise((resolve, reject) => {
                    response.data.on('data', (chunk) => {
                        downloadedLength += chunk.length;
                        if (totalLength) {
                            const percent = Math.round((downloadedLength / totalLength) * 100);
                            event.sender.send('status:update', {
                                status: 'updating_launcher',
                                message: `[런처 패치] 최신 버전 다운로드 중... (${percent}%)`,
                                percent
                            });
                        }
                    });
                    response.data.pipe(writer);
                    writer.on('finish', () => resolve());
                    writer.on('error', (err) => reject(err));
                });
            } else {
                console.warn("[Self Update Warning] Launcher update required but no download source specified.");
                return { updateRequired: false };
            }

            event.sender.send('status:update', {
                status: 'updating_launcher',
                message: '[런처 패치] 다운로드 완료. 프로그램 교체 및 재시작 작업을 실행합니다...',
                percent: 100
            });

            // 3. 배치 파일 작성하여 런처 교체 후 자동 재시작
            const batPath = path.join(minecraftDir, 'update.bat');
            const winTempUpdatePath = tempUpdatePath.replace(/\//g, '\\');
            const winExecPath = process.execPath.replace(/\//g, '\\');
            const batContent = `@echo off
timeout /t 3 /nobreak > nul
:retry
copy /y "${winTempUpdatePath}" "${winExecPath}"
if errorlevel 1 (
    echo [NoGeon Launcher] Waiting for old process to close...
    timeout /t 1 /nobreak > nul
    goto retry
)
start "" "${winExecPath}"
exit
`;
            fs.writeFileSync(batPath, batContent, 'utf8');

            // 배치 파일 실행 및 앱 즉시 종료
            const spawn = require('child_process').spawn;
            const child = spawn('cmd.exe', ['/c', batPath], {
                detached: true,
                stdio: 'ignore'
            });
            child.unref();

            setTimeout(() => {
                app.quit();
            }, 500);

            return { updateRequired: true };
        }

        return { updateRequired: false };
    } catch (err) {
        console.error("[Self Update Error]", err);
        return { updateRequired: false, error: err.message };
    }
});

// 순수 Node.js TCP 소켓 기반 마인크래프트 서버 상태 쿼리 및 Ping 헬퍼
const net = require('net');
function pingMinecraftServer(host, port = 25565, timeout = 3000) {
    return new Promise((resolve) => {
        const socket = net.createConnection(port, host);
        socket.setTimeout(timeout);
        
        let startTime = Date.now();
        
        socket.on('connect', () => {
            // 마인크래프트 Handshake & Status List Request 패킷 작성
            const packet = Buffer.concat([
                Buffer.from([0x00]), // Packet ID
                Buffer.from([0xf2, 0x05]), // Protocol Version (763 for 1.20.1)
                Buffer.from([host.length]), Buffer.from(host, 'utf-8'),
                Buffer.from([port >> 8, port & 0xFF]), // Port
                Buffer.from([0x01]), // Next State (1 for Status)
            ]);
            
            const handshakeFrame = Buffer.concat([Buffer.from([packet.length]), packet]);
            const requestFrame = Buffer.from([0x01, 0x00]); // Length 1, ID 0
            
            socket.write(handshakeFrame);
            socket.write(requestFrame);
        });
        
        let responseData = Buffer.alloc(0);
        socket.on('data', (data) => {
            responseData = Buffer.concat([responseData, data]);
            if (responseData.length > 5) {
                try {
                    const str = responseData.toString('utf-8');
                    const jsonStart = str.indexOf('{');
                    if (jsonStart !== -1) {
                        const jsonStr = str.substring(jsonStart);
                        const parsed = JSON.parse(jsonStr);
                        socket.end();
                        resolve({
                            online: true,
                            ping: Date.now() - startTime,
                            players: parsed.players ? parsed.players.online : 0,
                            maxPlayers: parsed.players ? parsed.players.max : 20
                        });
                        return;
                    }
                } catch (e) {
                    // JSON 파싱 에러 발생 시 단순 연결 성공 처리로 폴백
                }
            }
        });
        
        socket.on('timeout', () => {
            socket.destroy();
            resolve({ online: false, ping: 999, players: 0, maxPlayers: 0 });
        });
        
        socket.on('error', () => {
            socket.destroy();
            resolve({ online: false, ping: 999, players: 0, maxPlayers: 0 });
        });
    });
}

// IPC 12: 실시간 서버 상태 및 동접자 정보 가져오기 핸들러
ipcMain.handle('server:ping', async () => {
    try {
        const GITHUB_RAW_BASE = await getGithubRawBase();
        const res = await axios.get(`${GITHUB_RAW_BASE}/manifest.json?nocache=${Date.now()}`);
        const manifest = res.data;
        
        // manifest.json에 설정된 server_ip 및 server_port 사용 (없을 시 기본 로컬폴백)
        const host = manifest.server_ip || "127.0.0.1";
        const port = manifest.server_port || 25565;
        
        console.log(`[Server Status Query] Pinging Minecraft server at ${host}:${port}...`);
        const status = await pingMinecraftServer(host, port);
        return { success: true, ...status };
    } catch (err) {
        console.error("[Server Status Query Error]", err);
        return { success: false, online: false, ping: 999, players: 0, maxPlayers: 0 };
    }
});
