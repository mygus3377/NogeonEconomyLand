# NoGeon Economy Land 사용법

## 기본 GUI
- `G` 키: 경제 GUI 열기.
- 인벤토리 화면의 작은 `경제` 버튼: 경제 GUI 열기.
- 핫바 왼쪽: 현재 보유 크레딧 간략 표시.

## 관리자 소환 아이템
OP 권한으로 아래 아이템을 지급해서 월드에 우클릭 배치합니다.

- `nogeon_economy_land:general_trader_spawner`: 잡화 상인.
- `nogeon_economy_land:crop_trader_spawner`: 작물 상인.
- `nogeon_economy_land:fisher_trader_spawner`: 낚시꾼.
- `nogeon_economy_land:miner_trader_spawner`: 광부.
- `nogeon_economy_land:chef_trader_spawner`: 요리사.
- `nogeon_economy_land:lottery_trader_spawner`: 복권 상인.
- `nogeon_economy_land:gambler_trader_spawner`: 도박꾼.
- `nogeon_economy_land:gacha_trader_spawner`: 가챠 기계.
- `nogeon_economy_land:land_trader_spawner`: 토지 상인.
- `nogeon_economy_land:auction_trader_spawner`: 경매장 상인.

## 잡화 상인
- 일반 유저 우클릭: 잡화 상점 GUI 열기.
- 구매 시 크레딧 차감, 아이템 지급, 서버 공유 일일 재고 감소.
- OP Shift+우클릭: 관리자 편집 GUI 예정 안내.

## 관리자 품목 추가
- 손에 든 아이템을 잡화 상점 품목에 추가/교체:
`/economyadmin generalshop addhand <가격> <일일제한>`
- 예시:
`/economyadmin generalshop addhand 1500 32`

## 홈과 토지
- 경제 GUI → `토지`: 홈 저장/이동/삭제 가능.
- 토지 구매/보호/토지서 지정 모드는 다음 구현 단계.

## 직업과 스킬
- 직업 선택:
`/job select farmer`
`/job select fisher`
`/job select miner`
`/job select cook`
- 경제 GUI → `스킬`: 스킬포인트 투자 가능.
