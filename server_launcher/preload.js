const { contextBridge, ipcRenderer } = require('electron');

// 렌더러(프론트엔드)에서 안전하게 사용할 수 있는 브릿지 API 정의
contextBridge.exposeInMainWorld('launcherAPI', {
    // Microsoft 로그인 요청
    login: () => ipcRenderer.invoke('auth:login'),
    
    // 게임 시작 요청
    launch: (options) => ipcRenderer.send('game:launch', options),
    
    // 모드팩 동기화 요청
    sync: (fileId) => ipcRenderer.send('game:sync', fileId),
    
    // 상태 및 진행률 업데이트 구독 리스너
    onStatus: (callback) => ipcRenderer.on('status:update', (event, data) => callback(data)),
    
    // 실행 완료 리스너
    onLaunched: (callback) => ipcRenderer.on('game:launched', (event) => callback()),

    // 게임 종료 리스너
    onClosed: (callback) => ipcRenderer.on('game:closed', (event, code) => callback(code)),

    // 📂 폴더 열기 기능 (type: 'game_dir' | 'crash_reports')
    openFolder: (type) => ipcRenderer.send('util:open-folder', type),

    // 📝 최신 로그 읽기 기능
    readLog: () => ipcRenderer.invoke('util:read-log'),

    // 🔄 런처 공장 초기화
    resetLauncher: () => ipcRenderer.invoke('util:reset'),

    // ➖ 창 최소화 기능
    minimize: () => ipcRenderer.send('window:minimize'),

    // 🔍 실시간 업데이트 존재 여부 자동 스캔
    checkUpdate: () => ipcRenderer.invoke('game:check-update'),

    // 📢 깃허브 실시간 공지사항 네이티브 로더
    readPatch: () => ipcRenderer.invoke('util:read-patch'),

    // 🔑 로컬 세션 자동 로그인
    autoLogin: () => ipcRenderer.invoke('auth:auto-login'),

    // 🔄 런처 자체 자동 업데이트 검사
    checkSelfUpdate: () => ipcRenderer.invoke('launcher:check-self-update')
});
