// NoGeon Launcher Frontend Controller

// UI 요소 캐싱
const btnLogin = document.getElementById('btn-login');
const btnLogout = document.getElementById('btn-logout');
const btnSync = document.getElementById('btn-sync');
const btnLaunch = document.getElementById('btn-launch');
const usernameText = document.getElementById('username');
const statusBadge = document.getElementById('status-badge');
const avatar = document.getElementById('avatar');
const ramSelect = document.getElementById('ram-select');
// 로컬 RAM 설정 복원 및 변경 자동 저장 연동
const savedRam = localStorage.getItem('nogeon_launcher_ram');
if (savedRam && Array.from(ramSelect.options).some(opt => opt.value === savedRam)) {
    ramSelect.value = savedRam;
}
ramSelect.addEventListener('change', () => {
    localStorage.setItem('nogeon_launcher_ram', ramSelect.value);
    appendLog(`[설정] RAM 할당량이 ${ramSelect.value}로 자동 저장되었습니다.`, 'info');
});
const consoleBox = document.getElementById('console');
const progressBar = document.getElementById('progress-bar');
const progressText = document.getElementById('progress-text');

// 신규 추가 유틸리티 UI 요소 캐싱
const btnMinimize = document.getElementById('btn-minimize');
const btnUtilFolder = document.getElementById('btn-util-folder');
const btnUtilCrash = document.getElementById('btn-util-crash');
const btnUtilLog = document.getElementById('btn-util-log');
const btnUtilReset = document.getElementById('btn-util-reset');
const btnUtilRefresh = document.getElementById('btn-util-refresh');

const serverStatusContainer = document.getElementById('server-status-container');
const serverStatusDot = document.getElementById('server-status-dot');
const serverStatusText = document.getElementById('server-status-text');

const logModal = document.getElementById('log-modal');
const btnModalCopy = document.getElementById('btn-modal-copy');
const btnModalClose = document.getElementById('btn-modal-close');
const modalLogContent = document.getElementById('modal-log-content');

// 패치노트 모달 UI 요소 캐싱
const btnUtilPatch = document.getElementById('btn-util-patch');
const patchModal = document.getElementById('patch-modal');
const btnPatchClose = document.getElementById('btn-patch-close');
const modalPatchContent = document.getElementById('modal-patch-content');

// 백업 복구 모달 UI 요소 캐싱
const btnUtilBackup = document.getElementById('btn-util-backup');
const backupModal = document.getElementById('backup-modal');
const btnBackupClose = document.getElementById('btn-backup-close');
const backupListContainer = document.getElementById('backup-list-container');

let authProfile = null;

// 콘솔에 실시간 텍스트 출력 함수
function appendLog(message, type = 'info') {
    const p = document.createElement('p');
    p.className = `log-${type}`;
    p.textContent = message;
    consoleBox.appendChild(p);
    
    // 자동 스크롤 하단 고정
    consoleBox.scrollTop = consoleBox.scrollHeight;
}

// 깃허브 실시간 패치 노트 핫로딩 함수 (무중단 갱신)
async function loadRealtimePatchNote() {
    try {
        const result = await window.launcherAPI.readPatch();
        if (result.success) {
            const text = result.content;
            
            // 기존 도움말 비우기
            consoleBox.innerHTML = '';
            
            // 모달 패치 내용 초기화
            if (modalPatchContent) modalPatchContent.innerHTML = '';
            
            // 줄 단위 파싱 및 마크업 출력
            const lines = text.split('\n');
            lines.forEach(line => {
                const trimmed = line.trim();
                if (trimmed === '') return;
                
                // 모달 렌더링용 엘리먼트 생성
                const mp = document.createElement('div');
                
                if (trimmed.startsWith('📢') || trimmed.startsWith('[안내]') || trimmed.startsWith('[공지]')) {
                    appendLog(trimmed, 'system');
                    if (modalPatchContent) {
                        mp.className = 'patch-line-system';
                        mp.textContent = trimmed;
                        modalPatchContent.appendChild(mp);
                    }
                } else if (trimmed.startsWith('-') || trimmed.startsWith('*')) {
                    appendLog(trimmed, 'detail');
                    if (modalPatchContent) {
                        mp.className = 'patch-line-detail';
                        mp.textContent = trimmed;
                        modalPatchContent.appendChild(mp);
                    }
                } else if (trimmed.startsWith('[에러]') || trimmed.startsWith('[경고]')) {
                    appendLog(trimmed, 'error');
                    if (modalPatchContent) {
                        mp.className = 'patch-line-error';
                        mp.textContent = trimmed;
                        modalPatchContent.appendChild(mp);
                    }
                } else {
                    appendLog(trimmed, 'info');
                    if (modalPatchContent) {
                        mp.className = 'patch-line-info';
                        mp.textContent = trimmed;
                        modalPatchContent.appendChild(mp);
                    }
                }
            });
        } else {
            appendLog(`[경고] 실시간 패치 정보 로드 실패: ${result.error}`, 'error');
        }
    } catch (err) {
        appendLog(`[경고] 실시간 공지사항 로딩 실패: ${err.message}`, 'error');
    }
}

// 🟢 실시간 서버 상태 갱신 함수
async function updateServerStatus() {
    try {
        const result = await window.launcherAPI.pingServer();
        if (result && result.success) {
            if (result.online) {
                serverStatusDot.className = 'status-dot online';
                serverStatusText.className = 'status-text online';
                serverStatusText.textContent = `온라인 (${result.players}/${result.maxPlayers}명) - ${result.ping}ms`;
            } else {
                serverStatusDot.className = 'status-dot offline';
                serverStatusText.className = 'status-text offline';
                serverStatusText.textContent = '오프라인';
            }
        } else {
            serverStatusDot.className = 'status-dot offline';
            serverStatusText.className = 'status-text offline';
            serverStatusText.textContent = '오프라인';
        }
    } catch (err) {
        console.error("[Server Status Update Failed]", err);
        serverStatusDot.className = 'status-dot offline';
        serverStatusText.className = 'status-text offline';
        serverStatusText.textContent = '오프라인';
    }
}

// 🔍 로컬 세션 파일 기반 자동 로그인 수행 함수
async function tryAutoLogin() {
    progressText.textContent = "로컬 세션 자동 로그인 확인 중...";
    const result = await window.launcherAPI.autoLogin();
    
    if (result.success) {
        authProfile = result.profile;
        
        // 로그인 성공 UI 피드백 적용
        usernameText.textContent = authProfile.name;
        statusBadge.textContent = "정품 계정 자동 로그인 완료";
        statusBadge.style.color = "#10faaa";
        avatar.textContent = authProfile.name.substring(0, 2).toUpperCase();
        avatar.style.background = "#10faaa";
        avatar.style.color = "#040807";

        appendLog(`[자동 로그인] ${authProfile.name} 님 세션 로드에 성공했습니다.`, 'system');
        btnLogin.style.display = 'none'; // 로그인 버튼 숨김
        btnLogout.style.display = 'block'; // 로그아웃 버튼 표시

        // 실시간 업데이트 존재 여부 자동 스캔 즉시 구동
        await checkUpdatesAndSetUI();
    } else {
        progressText.textContent = "대기 중... 마이크로소프트 로그인을 완료해 주세요.";
        appendLog('[안내] 저장된 로그인 세션이 없거나 만료되었습니다. 로그인을 진행해 주세요.', 'info');
    }
}

// 앱 구동 시 실시간 공지 로드 수행 및 자동 로그인 시도
window.addEventListener('DOMContentLoaded', async () => {
    loadRealtimePatchNote();
    
    // 서버 상태 핑 최초 1회 즉시 실행 및 15초 주기 폴링 예약
    updateServerStatus();
    setInterval(updateServerStatus, 15000);
    
    // 런처 자체 업데이트 확인
    try {
        const updateCheck = await window.launcherAPI.checkSelfUpdate();
        if (updateCheck && updateCheck.updateRequired) {
            // 업데이트 다운로드 및 교체 프로세스가 진행 중이므로 일반 실행 흐름을 중단하고 대기합니다.
            btnLaunch.disabled = true;
            btnSync.disabled = true;
            btnLogin.disabled = true;
            return;
        }
    } catch (err) {
        console.error("[Launcher Update Check Failed]", err);
    }
    
    await tryAutoLogin();
});

// 🔍 업데이트 검사 및 UI 지능형 제어 함수
async function checkUpdatesAndSetUI() {
    if (!authProfile) return;
    
    progressText.textContent = "최신 모드 패치 내역 대조 스캔 중...";
    progressText.style.color = "var(--text-muted)";
    appendLog('[검사] 백그라운드에서 실시간 모드 해시 무결성 검증을 시작합니다...', 'info');
    
    const result = await window.launcherAPI.checkUpdate();
    
    if (result.updateRequired) {
        appendLog(`[경고] 업데이트 필요 대상 감지: ${result.reason}`, 'error');
        
        // 동기화 필수 유도 안내
        progressText.textContent = "⚠️ 새로운 서버 패치가 감지되었습니다! '모드 동기화' 버튼을 눌러주세요.";
        progressText.style.color = "#ff6b6b";
        
        // 버튼 제어: 동기화만 엶, 시작은 닫음
        btnSync.disabled = false;
        btnLaunch.disabled = true;
    } else {
        appendLog('[완료] 모든 로컬 모드의 무결성이 최신 깃허브 버전과 100% 일치합니다!', 'system');
        
        // 통과 안내
        progressText.textContent = "✅ 최신 버전 패치가 완벽히 적용되었습니다! 즉시 게임을 시작하세요.";
        progressText.style.color = "#10faaa";
        
        // 버튼 제어: 게임 시작 즉각 완전 개방!
        btnLaunch.disabled = false;
        btnSync.disabled = false; // 추가 패치가 있을 수 있으므로 활성 유지
    }
}

// 1. Microsoft 로그인 버튼 이벤트
btnLogin.addEventListener('click', async () => {
    appendLog('[로그인] Microsoft 계정 인증을 시도합니다. 웹 팝업창을 확인해 주세요...', 'system');
    btnLogin.disabled = true;

    const result = await window.launcherAPI.login();

    if (result.success) {
        authProfile = result.profile;
        
        // 로그인 성공 UI 피드백
        usernameText.textContent = authProfile.name;
        statusBadge.textContent = "정품 계정 로그인 완료";
        statusBadge.style.color = "#10faaa";
        avatar.textContent = authProfile.name.substring(0, 2).toUpperCase();
        avatar.style.background = "#10faaa";
        avatar.style.color = "#040807";

        appendLog(`[로그인] ${authProfile.name} 님 환영합니다! 로그인이 성공적으로 완료되었습니다.`, 'system');
        btnLogin.style.display = 'none'; // 로그인 버튼 숨김
        btnLogout.style.display = 'block'; // 로그아웃 버튼 표시

        // 실시간 업데이트 존재 여부 자동 스캔
        await checkUpdatesAndSetUI();
    } else {
        btnLogin.disabled = false;
        appendLog(`[에러] 로그인 실패: ${result.error}`, 'error');
    }
});

// 1.5. Microsoft 로그아웃 버튼 이벤트
btnLogout.addEventListener('click', async () => {
    const confirmLogout = confirm("정말로 로그아웃 하시겠습니까?");
    if (!confirmLogout) return;
    
    appendLog('[로그아웃] 로그아웃을 시도합니다...', 'system');
    const result = await window.launcherAPI.logout();
    
    if (result.success) {
        authProfile = null;
        
        // 로그인 권장 UI 복구
        usernameText.textContent = "Guest Account";
        statusBadge.textContent = "로그인 필요";
        statusBadge.style.color = "";
        avatar.textContent = "👤";
        avatar.style.background = "";
        avatar.style.color = "";
        
        // 버튼 상태 환원
        btnLaunch.disabled = true;
        btnSync.disabled = true;
        
        progressText.textContent = "대기 중... 마이크로소프트 로그인을 완료해 주세요.";
        progressText.style.color = "";
        
        btnLogout.style.display = 'none'; // 로그아웃 버튼 숨김
        btnLogin.style.display = 'block'; // 로그인 버튼 표시
        btnLogin.disabled = false;
        
        progressBar.style.width = "0%";
        appendLog('[로그아웃] 로그아웃이 완료되었습니다. 다시 로그인해 주세요.', 'system');
    } else {
        appendLog(`[에러] 로그아웃 실패: ${result.error}`, 'error');
    }
});

// 2. 모드 동기화 버튼 이벤트
btnSync.addEventListener('click', () => {
    appendLog('[동기화] 구글 드라이브(최초설치) 및 깃허브(실시간패치) 하이브리드 연동 동기화를 시작합니다...', 'system');
    btnSync.disabled = true;
    window.launcherAPI.sync();
});

// 3. 게임 시작 버튼 이벤트 (비동기 로그인 검증/재로그인 자동 연동)
btnLaunch.addEventListener('click', async () => {
    if (!authProfile) {
        appendLog('[에러] 로그인이 만료되었거나 비활성 상태입니다. 로그인을 진행해 주세요.', 'error');
        return;
    }

    btnLaunch.disabled = true;
    btnSync.disabled = true;
    btnUtilReset.disabled = true;

    appendLog('[실행 준비] 마이크로소프트 로그인 세션을 자동 검증하고 있습니다...', 'info');
    progressText.textContent = "로그인 세션 만료 여부 확인 중...";

    // 1단계: 백그라운드 자동 세션 갱신 시도 (msmc.refresh)
    const authCheck = await window.launcherAPI.autoLogin();
    
    if (authCheck.success) {
        authProfile = authCheck.profile;
        appendLog(`[세션 확인] 로그인 세션이 유효합니다. (플레이어: ${authProfile.name})`, 'system');
    } else {
        // 2단계: 자동 로그인 실패 시, 자동으로 Microsoft 재인증 팝업 호출 (알아서 자동 재로그인)
        appendLog('[세션 만료] 로그인 세션이 만료되었습니다. 안전한 기동을 위해 자동 재로그인을 진행합니다...', 'error');
        progressText.textContent = "로그인 세션 만료! 재로그인 인증이 필요합니다...";
        
        const loginResult = await window.launcherAPI.login();
        if (loginResult.success) {
            authProfile = loginResult.profile;
            
            // 로그인 UI 업데이트
            usernameText.textContent = authProfile.name;
            statusBadge.textContent = "정품 계정 로그인 완료";
            statusBadge.style.color = "#10faaa";
            avatar.textContent = authProfile.name.substring(0, 2).toUpperCase();
            avatar.style.background = "#10faaa";
            avatar.style.color = "#040807";
            btnLogin.style.display = 'none';
            btnLogout.style.display = 'block';
            
            appendLog(`[재로그인 성공] ${authProfile.name} 님 환영합니다! 인증이 즉각 완료되었습니다.`, 'system');
        } else {
            // 재로그인 실패 시 중단하고 버튼 활성화 복구
            appendLog(`[에러] 재로그인 인증 실패: ${loginResult.error}. 수동으로 로그인하신 후 다시 시도해 주세요.`, 'error');
            progressText.textContent = "재로그인 실패. 수동 로그인을 시도해 주세요.";
            progressText.style.color = "#ff6b6b";
            
            btnLaunch.disabled = false;
            btnSync.disabled = false;
            btnUtilReset.disabled = false;
            return;
        }
    }

    appendLog('[실행] 마인크래프트 포지 1.20.1 서버 접속 환경을 구동합니다...', 'system');
    // 게임 시작 시그널 전송 (갱신 및 확인된 안전한 로그인 토큰 주입)
    window.launcherAPI.launch({
        profile: authProfile,
        maxMemory: ramSelect.value
    });
});

// 메인 프로세스로부터 들어오는 동적 상태 및 로그 리포팅 수신
window.launcherAPI.onStatus(async (data) => {
    const { status, message, percent } = data;

    // 프로그레스 바 갱신
    if (percent !== undefined) {
        progressBar.style.width = `${percent}%`;
    }

    // 상태 메세지 갱신
    progressText.textContent = message;

    if (status === 'downloading') {
        // 다운로드 중에는 진행 상황만 가볍게 한 번씩 노출
        if (percent % 10 === 0) {
            appendLog(`[다운로드] 모드팩 패키지 수신 중... (${percent}%)`, 'info');
        }
    } else if (status === 'extracting') {
        appendLog('[압축 해제] 로컬 mods 및 configs 폴더 덮어쓰기 적용 중...', 'info');
    } else if (status === 'ready') {
        appendLog('[완료] 모드팩 동기화 패치가 완료되었습니다! 최종 검증 중...', 'system');
        await checkUpdatesAndSetUI();
    } else if (status === 'launching') {
        appendLog(`[MCLC] ${message}`, 'info');
    } else if (status === 'running') {
        // 인게임 실시간 콘솔 로그 출력 (한글 깨짐 없는 깔끔한 마크 로그)
        appendLog(message, 'detail');
    } else if (status === 'error') {
        appendLog(`[에러] 동기화 또는 구동 실패: ${message}`, 'error');
        btnSync.disabled = false;
    } else if (status === 'syncing') {
        // 깃허브 무결성 실시간 패치 로그 직송
        appendLog(message, 'info');
    }
});

// 게임 실행 성공 시 리스너
window.launcherAPI.onLaunched(() => {
    appendLog('[성공] 마인크래프트 Forge 1.20.1 클라이언트가 정상적으로 실행되었습니다!', 'system');
    progressText.textContent = "🎮 게임 플레이 중... 마인크래프트를 종료하면 런처가 대기 상태로 복귀합니다.";
    progressText.style.color = "#10faaa";
    
    // 게임 구동 시 모든 핵심 작동 버튼 차단
    btnLaunch.disabled = true;
    btnSync.disabled = true;
    btnUtilReset.disabled = true;
});

// 게임 종료 시 리스너
window.launcherAPI.onClosed((code) => {
    appendLog(`[알림] 마인크래프트 프로세스가 완전히 종료되었습니다. (종료 코드: ${code})`, 'system');
    
    // 리셋 버튼 개방 및 해시 실시간 자동 감지 스캔을 돌려 UI 상태 자동 환원
    btnUtilReset.disabled = false;
    checkUpdatesAndSetUI();
});

// 4. 최소화 버튼 클릭 이벤트
if (btnMinimize) {
    btnMinimize.addEventListener('click', () => {
        window.launcherAPI.minimize();
    });
}

// 5. 퀵 유틸리티 버튼 클릭 이벤트
if (btnUtilFolder) {
    btnUtilFolder.addEventListener('click', () => {
        window.launcherAPI.openFolder('game_dir');
        appendLog('[안내] 마인크래프트 게임 데이터 폴더를 탐색기로 열었습니다.', 'info');
    });
}

if (btnUtilCrash) {
    btnUtilCrash.addEventListener('click', () => {
        window.launcherAPI.openFolder('crash_reports');
        appendLog('[안내] 마인크래프트 크래시 보고서 폴더를 탐색기로 열었습니다.', 'info');
    });
}

if (btnUtilLog) {
    btnUtilLog.addEventListener('click', async () => {
        logModal.classList.add('active');
        modalLogContent.textContent = "로그 파일을 불러오는 중입니다...";
        
        const result = await window.launcherAPI.readLog();
        if (result.success) {
            modalLogContent.textContent = result.content;
            // 불러오면 스크롤을 맨 아래로 내려 최신 오류를 보기 쉽게 함
            const modalBody = logModal.querySelector('.modal-body');
            if (modalBody) {
                modalBody.scrollTop = modalBody.scrollHeight;
            }
        } else {
            modalLogContent.textContent = `[에러] 로그 파일을 읽을 수 없습니다:\n${result.error}`;
        }
    });
}

if (btnUtilReset) {
    btnUtilReset.addEventListener('click', async () => {
        const confirmReset = confirm("⚠️ 모드 재설치: 정말로 모드 폴더를 재설치하시겠습니까?\n\n이 작업은 로컬 mods 폴더만 안전하게 비운 후 최신 모드 파일들을 서버에서 다시 깨끗하게 자동으로 다운로드합니다. 싱글 세이브 데이터(saves)와 개인 설정, 로그인 세션 등은 안전하게 보존됩니다.");
        if (confirmReset) {
            appendLog('[초기화] 기존 모드 폴더를 안전하게 삭제하고 있습니다...', 'system');
            btnUtilReset.disabled = true;
            btnLaunch.disabled = true;
            btnSync.disabled = true;
            
            const result = await window.launcherAPI.resetLauncher();
            if (result.success) {
                appendLog('[초기화 완료] 모드 폴더 삭제 완료! 최신 모드팩 다운로드를 즉시 자동으로 개시합니다.', 'system');
                
                // 리로드하지 않고, 백그라운드 동기화를 즉시 자동 트리거 (원터치 자동 동기화 연동)
                window.launcherAPI.sync();
            } else {
                appendLog(`[에러] 초기화 실패: ${result.error}`, 'error');
                alert(`초기화 실패: ${result.error}`);
                btnUtilReset.disabled = false;
                btnLaunch.disabled = false;
                btnSync.disabled = false;
            }
        }
    });
}

if (btnUtilBackup) {
    btnUtilBackup.addEventListener('click', () => {
        loadBackupList();
        backupModal.classList.add('active');
    });
}

if (btnBackupClose) {
    btnBackupClose.addEventListener('click', () => {
        backupModal.classList.remove('active');
    });
}

if (backupModal) {
    backupModal.addEventListener('click', (e) => {
        if (e.target === backupModal) {
            backupModal.classList.remove('active');
        }
    });
}

// 6. 로그 모달 닫기 및 복사 이벤트
if (btnModalClose) {
    btnModalClose.addEventListener('click', () => {
        logModal.classList.remove('active');
    });
}

// 오버레이 클릭 시 닫기
if (logModal) {
    logModal.addEventListener('click', (e) => {
        if (e.target === logModal) {
            logModal.classList.remove('active');
        }
    });
}

if (btnModalCopy) {
    btnModalCopy.addEventListener('click', async () => {
        try {
            await navigator.clipboard.writeText(modalLogContent.textContent);
            const originalText = btnModalCopy.textContent;
            btnModalCopy.textContent = "✅ 복사 완료!";
            btnModalCopy.style.background = "#10faaa";
            btnModalCopy.style.color = "#040807";
            
            setTimeout(() => {
                btnModalCopy.textContent = originalText;
                btnModalCopy.style.background = "";
                btnModalCopy.style.color = "";
            }, 1500);
        } catch (err) {
            alert("로그 복사 실패: " + err.message);
        }
    });
}

// 7. 패치노트 모달 제어 이벤트
if (btnUtilPatch) {
    btnUtilPatch.addEventListener('click', () => {
        loadRealtimePatchNote();
        patchModal.classList.add('active');
    });
}

if (btnPatchClose) {
    btnPatchClose.addEventListener('click', () => {
        patchModal.classList.remove('active');
    });
}

if (patchModal) {
    patchModal.addEventListener('click', (e) => {
        if (e.target === patchModal) {
            patchModal.classList.remove('active');
        }
    });
}

// 8. 패치 및 업데이트 상태 실시간 새로고침 버튼 이벤트
if (btnUtilRefresh) {
    btnUtilRefresh.addEventListener('click', async () => {
        appendLog('[새로고침] 깃허브에서 실시간 패치 정보 및 업데이트 상태를 새로고침합니다...', 'info');
        btnUtilRefresh.disabled = true;
        const originalText = btnUtilRefresh.textContent;
        btnUtilRefresh.textContent = "⟳ 로딩 중...";
        let shouldRestoreButton = true;
        
        try {
            // 1. 런처 자체 업데이트 검증
            const updateCheck = await window.launcherAPI.checkSelfUpdate();
            if (updateCheck && updateCheck.updateRequired) {
                // 새로운 버전 다운로드 및 교체/재시작 루틴이 실행 중이므로 일반 실행 흐름을 완전히 중단합니다.
                btnLaunch.disabled = true;
                btnSync.disabled = true;
                btnUtilRefresh.disabled = true;
                shouldRestoreButton = false;
                return;
            }

            // 2. 실시간 패치노트 및 모드 대조
            await loadRealtimePatchNote();
            if (authProfile) {
                await checkUpdatesAndSetUI();
                appendLog('[새로고침 완료] 실시간 패치 정보 및 업데이트 대조 완료.', 'system');
            } else {
                appendLog('[새로고침 완료] 실시간 패치 정보가 새로고침되었습니다. (업데이트 대조는 로그인 완료 시 자동 수행됩니다.)', 'system');
            }
        } catch (err) {
            appendLog(`[에러] 새로고침 실패: ${err.message}`, 'error');
        } finally {
            if (shouldRestoreButton) {
                btnUtilRefresh.textContent = originalText;
                btnUtilRefresh.disabled = false;
            }
        }
    });
}

// 💾 백업 리스트 로드 및 렌더링 함수
async function loadBackupList() {
    try {
        if (backupListContainer) backupListContainer.innerHTML = '';
        const list = await window.launcherAPI.listBackups();
        
        if (!list || list.length === 0) {
            const noData = document.createElement('div');
            noData.className = 'backup-no-data';
            noData.textContent = "저장된 백업 파일이 없습니다. 모드팩 동기화 시 자동으로 생성됩니다.";
            backupListContainer.appendChild(noData);
            return;
        }

        list.forEach(item => {
            const itemDiv = document.createElement('div');
            itemDiv.className = 'backup-item';

            const infoDiv = document.createElement('div');
            infoDiv.className = 'backup-info';

            const timeSpan = document.createElement('span');
            timeSpan.className = 'backup-time';
            timeSpan.textContent = item.absoluteTime;

            const relSpan = document.createElement('span');
            relSpan.className = 'backup-relative';
            relSpan.textContent = `(${item.relativeTime})`;

            infoDiv.appendChild(timeSpan);
            infoDiv.appendChild(relSpan);

            const restoreBtn = document.createElement('button');
            restoreBtn.className = 'backup-restore-btn';
            restoreBtn.textContent = '복구하기';
            restoreBtn.addEventListener('click', async () => {
                const confirmRestore = confirm(`⚠️ 맵 & 세이브 복구 경고\n\n정말로 선택한 시점(${item.absoluteTime})으로 복구하시겠습니까?\n\n[주의]\n1. 현재의 세이브 및 미니맵 데이터가 해당 백업 데이터로 교체(덮어쓰기)됩니다.\n2. 혹시 모를 실수를 방지하기 위해, 현재 상태도 'backup_before_restore_' 폴더에 자동 백업되므로 원하실 경우 수동 롤백이 가능합니다.`);
                if (confirmRestore) {
                    appendLog(`[복구 시작] ${item.absoluteTime} 시점 백업 복구를 실행합니다...`, 'system');
                    backupModal.classList.remove('active');
                    
                    // 화면 컨트롤 잠금
                    btnSync.disabled = true;
                    btnLaunch.disabled = true;
                    btnUtilReset.disabled = true;
                    if (btnUtilBackup) btnUtilBackup.disabled = true;

                    progressText.textContent = "세이브 & 미니맵 데이터 복구 작업 진행 중...";
                    progressBar.style.width = "50%";

                    try {
                        const res = await window.launcherAPI.restoreBackup(item.folderName);
                        if (res && res.success) {
                            progressBar.style.width = "100%";
                            progressText.textContent = "✅ 백업 데이터 복구 성공!";
                            appendLog(`[복구 성공] 성공적으로 복원되었습니다. (안전 롤백 백업 생성됨: ${res.rollbackFolder})`, 'system');
                            alert(`✅ 복구가 완료되었습니다!\n\n만약 실수로 복원하신 경우, 게임 폴더의 backups/${res.rollbackFolder}에서 원래 데이터를 수동으로 찾을 수 있습니다.`);
                        } else {
                            throw new Error(res ? res.error : "알 수 없는 에러");
                        }
                    } catch (restoreErr) {
                        progressBar.style.width = "0%";
                        progressText.textContent = `❌ 복구 실패: ${restoreErr.message}`;
                        appendLog(`[복구 오류] 복구 진행 중 문제가 발생했습니다: ${restoreErr.message}`, 'error');
                        alert(`❌ 복구 실패: ${restoreErr.message}`);
                    } finally {
                        // 잠금 해제
                        btnSync.disabled = false;
                        btnLaunch.disabled = false;
                        btnUtilReset.disabled = false;
                        if (btnUtilBackup) btnUtilBackup.disabled = false;
                        checkUpdatesAndSetUI();
                    }
                }
            });

            itemDiv.appendChild(infoDiv);
            itemDiv.appendChild(restoreBtn);
            backupListContainer.appendChild(itemDiv);
        });
    } catch (err) {
        appendLog(`[에러] 백업 목록을 불러올 수 없습니다: ${err.message}`, 'error');
    }
}
