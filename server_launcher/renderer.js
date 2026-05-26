// NoGeon Launcher Frontend Controller

// UI 요소 캐싱
const btnLogin = document.getElementById('btn-login');
const btnSync = document.getElementById('btn-sync');
const btnLaunch = document.getElementById('btn-launch');
const usernameText = document.getElementById('username');
const statusBadge = document.getElementById('status-badge');
const avatar = document.getElementById('avatar');
const ramSelect = document.getElementById('ram-select');
const consoleBox = document.getElementById('console');
const progressBar = document.getElementById('progress-bar');
const progressText = document.getElementById('progress-text');

// 신규 추가 유틸리티 UI 요소 캐싱
const btnMinimize = document.getElementById('btn-minimize');
const btnUtilFolder = document.getElementById('btn-util-folder');
const btnUtilCrash = document.getElementById('btn-util-crash');
const btnUtilLog = document.getElementById('btn-util-log');
const btnUtilReset = document.getElementById('btn-util-reset');

const logModal = document.getElementById('log-modal');
const btnModalCopy = document.getElementById('btn-modal-copy');
const btnModalClose = document.getElementById('btn-modal-close');
const modalLogContent = document.getElementById('modal-log-content');

// 패치노트 모달 UI 요소 캐싱
const btnUtilPatch = document.getElementById('btn-util-patch');
const patchModal = document.getElementById('patch-modal');
const btnPatchClose = document.getElementById('btn-patch-close');
const modalPatchContent = document.getElementById('modal-patch-content');

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

        // 실시간 업데이트 존재 여부 자동 스캔
        await checkUpdatesAndSetUI();
    } else {
        btnLogin.disabled = false;
        appendLog(`[에러] 로그인 실패: ${result.error}`, 'error');
    }
});

// 2. 모드 동기화 버튼 이벤트
btnSync.addEventListener('click', () => {
    appendLog('[동기화] 구글 드라이브(최초설치) 및 깃허브(실시간패치) 하이브리드 연동 동기화를 시작합니다...', 'system');
    btnSync.disabled = true;
    window.launcherAPI.sync();
});

// 3. 게임 시작 버튼 이벤트
btnLaunch.addEventListener('click', () => {
    if (!authProfile) {
        appendLog('[에러] 로그인이 만료되었거나 비활성 상태입니다.', 'error');
        return;
    }

    appendLog('[실행] 마인크래프트 포지 1.20.1 서버 접속 환경을 구동합니다...', 'system');
    btnLaunch.disabled = true;
    btnSync.disabled = true;

    // 게임 시작 시그널 전송 (로그인 토큰 및 RAM 용량 주입)
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
        const confirmReset = confirm("⚠️ 경고: 정말로 노건 런처를 공장 초기화하시겠습니까?\n\n이 작업은 로컬 mods, config 등의 모든 마인크래프트 모드팩 파일을 삭제합니다. 런처 실행 시 최초 전체 다운로드 패키지(2.2GB)를 다시 완전히 처음부터 설치하게 됩니다.");
        if (confirmReset) {
            appendLog('[초기화] 런처 공장 초기화를 진행 중...', 'system');
            const result = await window.launcherAPI.resetLauncher();
            if (result.success) {
                alert("런처 초기화가 완료되었습니다. 프로그램이 재부팅됩니다.");
                location.reload();
            } else {
                appendLog(`[에러] 초기화 실패: ${result.error}`, 'error');
                alert(`초기화 실패: ${result.error}`);
            }
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
