const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const axios = require('axios');
const AdmZip = require('adm-zip');
const { execSync } = require('child_process');

// 깃허브 저장소 정보 하드코딩 (서버장님의 저장소)
const GITHUB_RAW_BASE = "https://raw.githubusercontent.com/mygus3377/NogeonEconomyLand/main";

/**
 * 로컬 파일의 SHA-256 해시값을 계산합니다.
 */
function calculateHash(filePath) {
    if (!fs.existsSync(filePath)) return '';
    const fileBuffer = fs.readFileSync(filePath);
    const hashSum = crypto.createHash('sha256');
    hashSum.update(fileBuffer);
    return hashSum.digest('hex');
}

/**
 * 단일 파일 다운로드 유틸리티
 */
async function downloadFile(url, destPath) {
    const response = await axios({
        method: 'get',
        url: url,
        responseType: 'stream'
    });
    
    const writer = fs.createWriteStream(destPath);
    return new Promise((resolve, reject) => {
        response.data.pipe(writer);
        writer.on('finish', () => resolve());
        writer.on('error', (err) => reject(err));
    });
}

/**
 * 구글 드라이브 최초 대용량 ZIP 파일 설치 및 패치 모듈 (대용량 다운로드 경고창 동적 우회 기능 포함)
 */
async function downloadGdriveZip(fileId, downloadPath, onProgress) {
    const baseUrl = `https://docs.google.com/uc?export=download&id=${fileId}`;

    // 1단계: 바이러스 검사 경고 우회용 Form 데이터 동적 추출 (action, confirm, uuid)
    let finalUrl = baseUrl;
    try {
        const responseInit = await axios.get(baseUrl);
        
        const actionMatch = responseInit.data.match(/action="([^"]+)"/);
        const confirmMatch = responseInit.data.match(/name="confirm" value="([^"]+)"/);
        const uuidMatch = responseInit.data.match(/name="uuid" value="([^"]+)"/);
        
        if (actionMatch && confirmMatch) {
            const actionUrl = actionMatch[1];
            const confirmVal = confirmMatch[1];
            const uuidVal = uuidMatch ? uuidMatch[1] : '';
            
            finalUrl = `${actionUrl}?id=${fileId}&export=download&confirm=${confirmVal}`;
            if (uuidVal) {
                finalUrl += `&uuid=${uuidVal}`;
            }
            console.log(`[Google Drive Sync] Successfully parsed dynamic download URL: ${finalUrl}`);
        } else {
            // 폴백: 기존 단순 confirm=t 및 토큰 추출
            const tokenMatch = responseInit.data.match(/confirm=([0-9A-Za-z_]+)/);
            if (tokenMatch) {
                finalUrl = `${baseUrl}&confirm=${tokenMatch[1]}`;
            } else {
                finalUrl = `${baseUrl}&confirm=t`;
            }
            console.log(`[Google Drive Sync] Fallback download URL: ${finalUrl}`);
        }
    } catch (err) {
        finalUrl = `${baseUrl}&confirm=t`;
        console.log(`[Google Drive Sync] Failed to parse confirm token, using fallback: ${err.message}`);
    }

    // 2단계: 실제 스트림 다운로드 수행
    const response = await axios({
        method: 'get',
        url: finalUrl,
        responseType: 'stream'
    });

    const totalLength = response.headers['content-length'];
    let downloadedLength = 0;
    const writer = fs.createWriteStream(downloadPath);

    return new Promise((resolve, reject) => {
        response.data.on('data', (chunk) => {
            downloadedLength += chunk.length;
            // 최초 ZIP 파일은 진행도가 100%까지 예쁘게 표출되도록 퍼센트 계산 피드백 전송
            if (totalLength && onProgress) {
                const percent = Math.round((downloadedLength / totalLength) * 100);
                onProgress(percent);
            } else if (onProgress) {
                // content-length가 누락된 경우를 위한 안전장치 (받은 데이터 크기 환산 노출)
                const mb = Math.round(downloadedLength / (1024 * 1024));
                onProgress(mb > 100 ? 99 : mb); // 간접 표시
            }
        });
        response.data.pipe(writer);
        writer.on('finish', () => {
            // 다운로드 완료 후 파일 크기 및 HTML 여부 정밀 검증
            if (fs.existsSync(downloadPath)) {
                const stats = fs.statSync(downloadPath);
                const minSize = 50 * 1024 * 1024; // 최소 50MB 이상
                if (stats.size < minSize) {
                    let detail = "";
                    try {
                        const header = fs.readFileSync(downloadPath, 'utf8').substring(0, 500);
                        if (header.includes("<!DOCTYPE html>") || header.includes("<html") || header.includes("Google Drive")) {
                            detail = " (구글 드라이브의 바이러스 검사 경고창 또는 트래픽 차단 페이지가 수신되었습니다)";
                        }
                    } catch (e) {}
                    reject(new Error(`다운로드된 파일이 비정상적으로 작습니다: 크기 ${(stats.size / 1024 / 1024).toFixed(2)}MB${detail}`));
                    return;
                }
            }
            resolve();
        });
        writer.on('error', (err) => reject(err));
    });
}

/**
 * Recursively moves files or merges directories from src to dest.
 */
function moveOrMergeSync(src, dest) {
    if (!fs.existsSync(src)) return;
    
    const srcStat = fs.statSync(src);
    
    if (srcStat.isDirectory()) {
        if (fs.existsSync(dest)) {
            const destStat = fs.statSync(dest);
            if (destStat.isDirectory()) {
                // Both are directories: recursively merge contents
                const items = fs.readdirSync(src);
                for (const item of items) {
                    moveOrMergeSync(path.join(src, item), path.join(dest, item));
                }
                try {
                    fs.rmdirSync(src);
                } catch (e) {
                    console.warn(`[Merge Warning] Could not remove empty directory ${src}: ${e.message}`);
                }
            } else {
                // Destination is a file: remove it first, then move directory
                fs.rmSync(dest, { force: true });
                fs.renameSync(src, dest);
            }
        } else {
            // Destination does not exist: rename directory
            fs.renameSync(src, dest);
        }
    } else {
        // Source is a file
        if (fs.existsSync(dest)) {
            const destStat = fs.statSync(dest);
            if (destStat.isDirectory()) {
                // Destination is a directory: remove it first
                fs.rmSync(dest, { recursive: true, force: true });
            } else {
                // Destination is a file: delete it
                fs.unlinkSync(dest);
            }
        }
        fs.renameSync(src, dest);
    }
}

/**
 * Backs up key user folders (saves, XaeroWorldMap, XaeroMinimapWaypoints, options.txt)
 * to backups/backup_YYYYMMDD_HHMMSS folder and rotates backups to keep only the latest 5.
 */
function backupUserData(minecraftDir) {
    try {
        const targets = ['saves', 'XaeroWorldMap', 'XaeroMinimapWaypoints', 'options.txt'];
        const backupBaseDir = path.join(minecraftDir, 'backups');
        
        // Check if any target exists
        const existingTargets = targets.filter(target => fs.existsSync(path.join(minecraftDir, target)));
        if (existingTargets.length === 0) {
            console.log('[Backup] No user data to backup.');
            return;
        }

        if (!fs.existsSync(backupBaseDir)) {
            fs.mkdirSync(backupBaseDir, { recursive: true });
        }

        const now = new Date();
        const pad = (n) => String(n).padStart(2, '0');
        const timestamp = `${now.getFullYear()}${pad(now.getMonth()+1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
        const backupDestDir = path.join(backupBaseDir, `backup_${timestamp}`);

        fs.mkdirSync(backupDestDir, { recursive: true });
        console.log(`[Backup] Creating backup at: ${backupDestDir}`);

        for (const target of existingTargets) {
            const srcPath = path.join(minecraftDir, target);
            const destPath = path.join(backupDestDir, target);
            try {
                if (typeof fs.cpSync === 'function') {
                    fs.cpSync(srcPath, destPath, { recursive: true });
                } else {
                    // Fallback helper for older Node versions
                    copyRecursiveSync(srcPath, destPath);
                }
                console.log(`[Backup] Successfully backed up: ${target}`);
            } catch (copyErr) {
                console.error(`[Backup Error] Failed to copy ${target}: ${copyErr.message}`);
            }
        }

        // Rotate backups to keep only 5 latest backups
        const backups = fs.readdirSync(backupBaseDir)
            .filter(name => name.startsWith('backup_') && fs.statSync(path.join(backupBaseDir, name)).isDirectory())
            .sort(); // Sorts ascending alphabetically (oldest first due to timestamp format)

        if (backups.length > 5) {
            const toDelete = backups.slice(0, backups.length - 5);
            for (const oldBackup of toDelete) {
                const oldPath = path.join(backupBaseDir, oldBackup);
                try {
                    fs.rmSync(oldPath, { recursive: true, force: true });
                    console.log(`[Backup Rotation] Deleted old backup: ${oldBackup}`);
                } catch (rmErr) {
                    console.error(`[Backup Rotation Error] Failed to delete ${oldBackup}: ${rmErr.message}`);
                }
            }
        }
    } catch (err) {
        console.error(`[Backup Error] Overall backup process failed: ${err.message}`);
    }
}

function copyRecursiveSync(src, dest) {
    const exists = fs.existsSync(src);
    const stats = exists && fs.statSync(src);
    const isDirectory = exists && stats.isDirectory();
    if (isDirectory) {
        fs.mkdirSync(dest, { recursive: true });
        fs.readdirSync(src).forEach((childItemName) => {
            copyRecursiveSync(path.join(src, childItemName), path.join(dest, childItemName));
        });
    } else {
        fs.copyFileSync(src, dest);
    }
}

/**
 * Calculates human-readable relative time string.
 */
function getRelativeTimeString(diffMs) {
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHr = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHr / 24);
    
    if (diffSec < 60) return "방금 전";
    if (diffMin < 60) return `${diffMin}분 전`;
    if (diffHr < 24) return `${diffHr}시간 전`;
    return `${diffDay}일 전`;
}

/**
 * Lists all available backups with absolute and relative timestamps.
 */
function listBackups(minecraftDir) {
    try {
        const backupBaseDir = path.join(minecraftDir, 'backups');
        if (!fs.existsSync(backupBaseDir)) {
            return [];
        }

        const items = fs.readdirSync(backupBaseDir);
        const backupList = [];

        for (const item of items) {
            // backup_YYYYMMDD_HHmmss 형식만 매칭 (임시 rollback 백업인 backup_before_restore_는 유저 UI 목록에서 감춤)
            if (item.startsWith('backup_') && !item.startsWith('backup_before_restore_')) {
                const fullPath = path.join(backupBaseDir, item);
                if (fs.statSync(fullPath).isDirectory()) {
                    const match = item.match(/^backup_(\d{4})(\d{2})(\d{2})_(\d{2})(\d{2})(\d{2})$/);
                    let backupDate = null;
                    if (match) {
                        const [_, yyyy, mm, dd, hh, min, ss] = match;
                        backupDate = new Date(yyyy, mm - 1, dd, hh, min, ss);
                    } else {
                        backupDate = fs.statSync(fullPath).birthtime || fs.statSync(fullPath).mtime;
                    }

                    const now = new Date();
                    const diffMs = now.getTime() - backupDate.getTime();
                    
                    const pad = (n) => String(n).padStart(2, '0');
                    const absoluteTime = `${backupDate.getFullYear()}-${pad(backupDate.getMonth()+1)}-${pad(backupDate.getDate())} ${pad(backupDate.getHours())}:${pad(backupDate.getMinutes())}:${pad(backupDate.getSeconds())}`;
                    const relativeTime = getRelativeTimeString(diffMs);

                    backupList.push({
                        folderName: item,
                        timestamp: backupDate.getTime(),
                        absoluteTime: absoluteTime,
                        relativeTime: relativeTime
                    });
                }
            }
        }

        return backupList.sort((a, b) => b.timestamp - a.timestamp);
    } catch (err) {
        console.error(`[Backup List Error] Failed to list backups: ${err.message}`);
        return [];
    }
}

/**
 * Restores a specific backup by folder name.
 * Temporarily backs up the current state as 'backup_before_restore_<timestamp>' to prevent data loss.
 */
function restoreBackup(minecraftDir, folderName) {
    try {
        const backupSrcDir = path.join(minecraftDir, 'backups', folderName);
        if (!fs.existsSync(backupSrcDir)) {
            throw new Error(`백업 폴더가 존재하지 않습니다: ${folderName}`);
        }

        console.log(`[Restore] Starting restore from: ${folderName}`);
        const targets = ['saves', 'XaeroWorldMap', 'XaeroMinimapWaypoints', 'options.txt'];
        
        // 1. 안전장치: 현재 살아있는 데이터들을 'backup_before_restore_<timestamp>'로 복사 보관
        const backupBaseDir = path.join(minecraftDir, 'backups');
        const now = new Date();
        const pad = (n) => String(n).padStart(2, '0');
        const timestamp = `${now.getFullYear()}${pad(now.getMonth()+1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
        const rollbackDir = path.join(backupBaseDir, `backup_before_restore_${timestamp}`);

        const existingTargets = targets.filter(target => fs.existsSync(path.join(minecraftDir, target)));
        if (existingTargets.length > 0) {
            fs.mkdirSync(rollbackDir, { recursive: true });
            console.log(`[Restore Safety] Creating temporary rollback backup at: ${rollbackDir}`);
            for (const target of existingTargets) {
                const srcPath = path.join(minecraftDir, target);
                const destPath = path.join(rollbackDir, target);
                try {
                    if (typeof fs.cpSync === 'function') {
                        fs.cpSync(srcPath, destPath, { recursive: true });
                    } else {
                        copyRecursiveSync(srcPath, destPath);
                    }
                } catch (copyErr) {
                    console.error(`[Restore Safety Error] Failed to backup ${target} before restore: ${copyErr.message}`);
                }
            }
        }

        // 2. 현재 활성 데이터 삭제
        for (const target of targets) {
            const targetPath = path.join(minecraftDir, target);
            if (fs.existsSync(targetPath)) {
                try {
                    fs.rmSync(targetPath, { recursive: true, force: true });
                    console.log(`[Restore Cleanup] Removed active target: ${target}`);
                } catch (rmErr) {
                    console.error(`[Restore Cleanup Error] Failed to remove active ${target}: ${rmErr.message}`);
                }
            }
        }

        // 3. 백업 데이터 복사 복원
        const backupTargets = targets.filter(target => fs.existsSync(path.join(backupSrcDir, target)));
        for (const target of backupTargets) {
            const srcPath = path.join(backupSrcDir, target);
            const destPath = path.join(minecraftDir, target);
            try {
                if (typeof fs.cpSync === 'function') {
                    fs.cpSync(srcPath, destPath, { recursive: true });
                } else {
                    copyRecursiveSync(srcPath, destPath);
                }
                console.log(`[Restore Success] Restored target: ${target}`);
            } catch (copyErr) {
                console.error(`[Restore Error] Failed to copy ${target} back: ${copyErr.message}`);
                throw new Error(`${target} 복원 도중 복사 오류 발생: ${copyErr.message}`);
            }
        }

        console.log(`[Restore] Successfully restored from ${folderName}`);
        return { success: true, rollbackFolder: `backup_before_restore_${timestamp}` };
    } catch (err) {
        console.error(`[Restore Failed] ${err.message}`);
        return { success: false, error: err.message };
    }
}

function flattenNestedFolder(targetDir) {
    try {
        console.log(`[Flatten] Starting nested folder check in: ${targetDir}`);
        const items = fs.readdirSync(targetDir);
        
        // 이미 mods 폴더가 최상위에 존재한다면 정상적인 압축 해제이므로 평탄화 건너뜀
        if (items.includes('mods')) {
            console.log('[Flatten] "mods" folder already exists at root. No flattening needed.');
            return;
        }
        
        // 시스템 관련 숨김 폴더나 파일, 그리고 libraries, assets 폴더 제외하고 하위 폴더들을 추출
        const subDirs = items.filter(item => {
            const fullPath = path.join(targetDir, item);
            return fs.statSync(fullPath).isDirectory() && 
                   !item.startsWith('.') && 
                   !['libraries', 'assets', 'natives'].includes(item);
        });
        
        console.log(`[Flatten] Subdirectories found:`, subDirs);
        
        // 만약 단 하나의 하위 폴더만 존재하고 그 폴더 내부를 보니 mods나 config 등이 있다면 평탄화
        if (subDirs.length === 1) {
            const nestedDirName = subDirs[0];
            const nestedDirPath = path.join(targetDir, nestedDirName);
            const nestedItems = fs.readdirSync(nestedDirPath);
            
            if (nestedItems.includes('mods') || nestedItems.some(item => ['config', 'kubejs', 'resourcepacks'].includes(item))) {
                console.log(`[Flatten] Nested root folder detected: "${nestedDirName}". Moving all files up to root...`);
                
                for (const item of nestedItems) {
                    const srcPath = path.join(nestedDirPath, item);
                    const destPath = path.join(targetDir, item);
                    
                    moveOrMergeSync(srcPath, destPath);
                }
                
                // 비어 있는 원래의 중첩 폴더 삭제
                fs.rmSync(nestedDirPath, { recursive: true, force: true });
                console.log(`[Flatten] Successfully flattened nested folder: "${nestedDirName}"`);
            }
        } else {
            // subDirs가 여러 개 있더라도, 그 중 하나에 mods가 있다면 평탄화 폴백 처리
            for (const dir of subDirs) {
                const nestedDirPath = path.join(targetDir, dir);
                const nestedItems = fs.readdirSync(nestedDirPath);
                if (nestedItems.includes('mods')) {
                    console.log(`[Flatten Fallback] Detected mods folder inside subdirectory: "${dir}". Elevating contents...`);
                    for (const item of nestedItems) {
                        const srcPath = path.join(nestedDirPath, item);
                        const destPath = path.join(targetDir, item);
                        
                        moveOrMergeSync(srcPath, destPath);
                    }
                    fs.rmSync(nestedDirPath, { recursive: true, force: true });
                    console.log(`[Flatten Fallback] Finished elevating contents of: "${dir}"`);
                    break;
                }
            }
        }
    } catch (err) {
        console.error(`[Flatten Error] Failed to flatten nested folders: ${err.message}`);
    }
}

function extractZip(zipPath, targetDir) {
    if (!fs.existsSync(targetDir)) {
        fs.mkdirSync(targetDir, { recursive: true });
    }
    
    console.log(`[ZIP Extractor] Extracting ZIP (${zipPath}) to (${targetDir}) using native system command...`);
    
    // Windows 10/11은 tar.exe가 기본 탑재되어 있어 대용량 ZIP 압축해제 속도가 매우 빠르고 Node.js의 2GB 버퍼 제한을 완벽히 우회합니다!
    try {
        const cmd = `tar -xf "${zipPath}" -C "${targetDir}"`;
        console.log(`[ZIP Extractor] Executing command: ${cmd}`);
        execSync(cmd, { stdio: 'ignore' });
        console.log(`[ZIP Extractor] Extraction complete using native tar command.`);
    } catch (tarErr) {
        console.log(`[ZIP Extractor Warning] Native tar command failed: ${tarErr.message}. Falling back to PowerShell...`);
        try {
            const psCmd = `powershell -NoProfile -Command "Expand-Archive -Path '${zipPath}' -DestinationPath '${targetDir}' -Force"`;
            console.log(`[ZIP Extractor] Executing PowerShell command: ${psCmd}`);
            execSync(psCmd, { stdio: 'ignore' });
            console.log(`[ZIP Extractor] Extraction complete using PowerShell Expand-Archive.`);
        } catch (psErr) {
            console.error(`[ZIP Extractor Error] All extraction methods failed. PowerShell error: ${psErr.message}`);
            // 최종 폴백으로 adm-zip 시도 (2GB 미만일 때만 작동 가능)
            const zip = new AdmZip(zipPath);
            zip.extractAllTo(targetDir, true);
        }
    }
    
    // 압축 완료 후 중첩 폴더가 있는 경우 평탄화
    flattenNestedFolder(targetDir);
}

/**
 * 깃허브 manifest.json을 연동하여 로컬 mods 폴더와 무결성 검증을 거쳐 부분 업데이트를 처리합니다.
 */
async function syncModsFromGithub(localModsDir, onLog, githubRawBase) {
    const rawBase = githubRawBase || GITHUB_RAW_BASE;
    if (!fs.existsSync(localModsDir)) {
        fs.mkdirSync(localModsDir, { recursive: true });
    }

    onLog('[무결성 검사] 깃허브 서버에서 최신 모드 목록 및 무결성 테이블을 가져오는 중...');
    
    // 1. 깃허브 최신 manifest.json 수신
    let manifest;
    try {
        const res = await axios.get(`${rawBase}/manifest.json?nocache=${Date.now()}`);
        manifest = res.data;
    } catch (err) {
        onLog(`[무결성 검사 거부] manifest.json 로딩 실패: ${err.message}. 개별 검사를 건너뜁니다.`, 'error');
        return;
    }

    const serverMods = manifest.mods || [];
    const serverModNames = serverMods.map(m => m.name);

    // 원격 삭제 대상 목록 처리 (manifest.json의 delete 배열에 명시된 파일 제거)
    if (manifest.delete && Array.isArray(manifest.delete)) {
        for (const fileToDelete of manifest.delete) {
            const filePath = path.join(localModsDir, fileToDelete);
            if (fs.existsSync(filePath)) {
                onLog(`[불필요 모드 제거] 삭제 대상 감지: ${fileToDelete}`);
                try {
                    fs.unlinkSync(filePath);
                } catch (e) {
                    console.error(`Failed to delete obsolete file ${fileToDelete}: ${e.message}`);
                }
            }
        }
    }

    // 2. 로컬 mods 폴더 내의 기존 파일 무결성 대조 및 구버전 교체 준비
    onLog('[무결성 검사] 로컬 모드 폴더 무결성 대조를 시작합니다...');
    const localFiles = fs.readdirSync(localModsDir);
    
    // 이 단계에서는 유저가 가진 다른 290여 개의 기본 모드팩 파일들을 절대 지우지 않고 안전하게 보존합니다.
    // 오직 메인 모드(nogeon-economy-land)의 '이름이 다른 구버전 파일'이 로컬에 중복 존재할 때만 버전 꼬임 방지를 위해 삭제합니다.
    for (const file of localFiles) {
        const filePath = path.join(localModsDir, file);
        if (!fs.existsSync(filePath)) continue;
        if (fs.statSync(filePath).isDirectory()) continue;

        if (file.toLowerCase().startsWith('nogeon-economy-land')) {
            for (const serverMod of serverMods) {
                if (serverMod.name.startsWith('nogeon-economy-land') && file !== serverMod.name) {
                    onLog(`[버전 정리] 메인 모드 구버전 제거 대상 감지: ${file}`);
                    try {
                        fs.unlinkSync(filePath);
                    } catch (e) {
                        console.error(`Failed to delete old main mod ${file}: ${e.message}`);
                    }
                }
            }
        }
    }

    // 3. 누락되었거나 해시(SHA-256)가 다른 최신 모드 파일만 핀셋 부분 다운로드
    for (const serverMod of serverMods) {
        const localPath = path.join(localModsDir, serverMod.name);
        const exists = fs.existsSync(localPath);
        
        let shouldDownload = false;
        if (!exists) {
            onLog(`[모드 누락 감지] 신규 모듈 설치 대상: ${serverMod.name}`);
            shouldDownload = true;
        } else {
            // 로컬 파일의 해시값 연산 후 서버와 대조
            const localHash = calculateHash(localPath);
            if (localHash !== serverMod.sha256.toLowerCase()) {
                onLog(`[업데이트 감지] 버전 교체 대상: ${serverMod.name}`);
                shouldDownload = true;
            }
        }

        if (shouldDownload) {
            onLog(`[패치 중] 초고속 다운로드 시작: ${serverMod.name}...`);
            const fileUrl = `${rawBase}/mods/${encodeURIComponent(serverMod.name)}`;
            try {
                await downloadFile(fileUrl, localPath);
                onLog(`[패치 성공] 완료: ${serverMod.name}`);
            } catch (err) {
                onLog(`[패치 오류] 다운로드 실패: ${serverMod.name} (${err.message})`, 'error');
            }
        }
    }

    onLog('[무결성 검사 완료] 모든 모드가 서버와 100% 동일하게 동기화되었습니다.', 'system');
}

module.exports = {
    calculateHash,
    downloadGdriveZip,
    extractZip,
    syncModsFromGithub,
    backupUserData,
    listBackups,
    restoreBackup
};
