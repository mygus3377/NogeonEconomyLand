const fs = require('fs');
const path = require('path');
const axios = require('axios');
const AdmZip = require('adm-zip');

/**
 * 구글 드라이브 Direct Download URL을 사용해 모드팩 zip 파일을 다운로드합니다.
 * @param {string} fileId 구글 드라이브 파일 ID
 * @param {string} downloadPath 저장할 로컬 파일 전체 경로
 * @param {function} onProgress 진행 상황 리포트 콜백 (0 ~ 100)
 */
async function downloadModpack(fileId, downloadPath, onProgress) {
    const url = `https://docs.google.com/uc?export=download&id=${fileId}&confirm=t`;
    
    const response = await axios({
        method: 'get',
        url: url,
        responseType: 'stream'
    });

    const totalLength = response.headers['content-length'];
    let downloadedLength = 0;
    
    const writer = fs.createWriteStream(downloadPath);

    return new Promise((resolve, reject) => {
        response.data.on('data', (chunk) => {
            downloadedLength += chunk.length;
            if (totalLength && onProgress) {
                const percent = Math.round((downloadedLength / totalLength) * 100);
                onProgress(percent);
            } else if (onProgress) {
                // Content-Length가 없을 때 유저에게 진행 느낌을 주기 위한 가상 처리
                onProgress(-1); 
            }
        });

        response.data.pipe(writer);

        writer.on('finish', () => resolve());
        writer.on('error', (err) => reject(err));
    });
}

/**
 * 모드팩 압축 파일을 타겟 마인크래프트 디렉토리에 풉니다.
 * @param {string} zipPath zip 파일 경로
 * @param {string} targetDir 마인크래프트 게임 인스턴스 루트 경로 (.minecraft)
 */
function extractModpack(zipPath, targetDir) {
    if (!fs.existsSync(targetDir)) {
        fs.mkdirSync(targetDir, { recursive: true });
    }

    const zip = new AdmZip(zipPath);
    
    // 타겟 폴더에 그대로 압축 해제 (mods 및 config 폴더가 덮어씌워짐)
    zip.extractAllTo(targetDir, true);
}

module.exports = {
    downloadModpack,
    extractModpack
};
