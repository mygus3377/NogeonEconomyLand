# NoGeon Economy Land Design

## Core Model
Every feature uses one persistent server state.

- Player profile: credits, job, job stats, homes, social class.
- Server economy: shop stock, shop value table, daily demand modifiers, lottery round, auction listings.
- Land registry: regions, owners, type, memo, price history, home markers.

## Credits
Credits are integer values. Prices are tuned around Korean won-like intuition:

- Daily basics: 100 to 5,000 credits.
- Tools and useful supplies: 1,000 to 30,000 credits.
- Land: normal 100 credits per block, industrial 300 credits per block.
- Lottery jackpot: 100,000,000 credits.
- **Mob Drops (New)**: 몹 처치 시 몬스터의 체력 수준과 레어도에 비례하여 크레딧이 드롭됩니다.

## Jobs
Jobs are farmer, fisher, miner, cook, and **hunter (사냥꾼 - 신규)**. 
Each job has exp, level, and skill points. Skill trees must affect delivery price first, then efficiency and convenience.

## GUI Policy
Commands are fallback only. Normal users should manage economy, jobs, homes, land, shops, trade, auction, gambling, gacha, and NPCs through GUI screens.

## First Playable Slice
The first playable slice is:

1. Credits are saved.
2. Players can choose a job.
3. Players can save and use homes.
4. Admins can grant credits for testing.
5. GUI screens are added on top of these same state APIs.

---

# 시스템 기획 세부사항

이 모드는 경제 및 토지, 상점 모드임.
자체 상인 및 엔티티 설정 가능 해야하며 모든 것은 GUI를 통해 설정하고 조작하고 생성, 관리가 가능해야한다.

## 경제
단위는 크레딧. 현실의 한국돈과 비슷한 가치를 가져야함.
이 돈은 이 모드로 구현되는 모든 요소와 상호작용 되어야함.

## 직업
- 농사꾼
- 낚시꾼
- 광부
- 요리사
- **사냥꾼 (신규)**

총 다섯 가지가 있으며 스킬 GUI를 추가하여 각 직업군에 맞는 역할을 했을 때 경험치를 얻어서 레벨업을 하면 스킬포인트를 얻음.
이 스킬포인트를 찍어서 납품할때 돈을 더 받을 수 있거나 관련 행위의 효율, 편의성이 올라가는 스킬 구성 추가.

## 상점 및 데이터베이스 개편 (진행 예정)
모든 상인에게 **데이터베이스를 내장**하여 사고파는 품목, 수량, 가격을 GUI를 통해 자유롭게 수정할 수 있어야 하며, 저장된 설정은 나중에 재소환해도 유지되도록 구성함.
- **관리자 설정 GUI 친화성**: 판매/매입 아이템 추가 시 인벤토리 창 옆에 별도 상자 GUI가 열리고, 인벤토리에서 우클릭하여 등록할 수 있도록 직관적으로 구현. 일일 제한 갯수와 가격을 난잡하지 않게 설정 가능해야 함.
- **툴팁 공통**: 모든 상점 GUI에서 아이템 아이콘에 마우스 커서를 올렸을 때 툴팁(효과, 데미지 등)이 정상적으로 표시되어야 함.

1. 잡화 상인
빵, 구운 감자, 스테이크, 횃불, 침대, 물/빈 양동이, 화살, 방패, 사다리, 보트, 가죽끈, 이름표 등.
**변경점**: 빵, 횃불, 구운 감자, 스테이크, 화살, 사다리 등은 묶음 단위가 아닌 **개별 단위 판매**로 변경하고, 크레딧 가격을 개별 수준으로 낮춤. 가치있는 것이 아니라면 매입해서는 안됨.

2. 작물 상인
씨앗, 묘목, 기초 식재료, 요리 재료, 소량의 특수 작물, 나무/돌/철 괭이.
플레이어가 작물을 팔 수 있음.

3. 낚시꾼
낚싯대, 실, 물 양동이, 보트, 횃불/랜턴.
희귀도에 따라 생선류 납품 및 가격 차이.

4. 광부
나무/돌/철 곡괭이 삽 도끼. (다이아몬드, 네더라이트, 고급 모드 광물 기본 판매 X)
광물 납품 처리.

5. 요리사
온갖 음식 판매 (비쌈). 요리 납품 처리.

6. 복권상인
매일 복권 판매.

7. 도박꾼
크레딧을 걸고 미니게임(하이로우 카드, 주사위 대결) 진행.

8. 가챠 뽑기 기계
초급/중급/고급/전설 뽑기.
**변경점**: 보상 풀에서 잘못된 총기류(블래스터 등) 제거.

9. 총포상 (수정)
**변경점**: 현재 판매 중인 잘못된 총기(블래스터 등)를 모두 제외. 당분간은 총기 판매를 중단하고 기존에 있는 **탄환만 판매**하도록 축소 운영.

10. 물약 상인 (개선)
**변경점**: 판매하는 물약 종류 다양화 (투척용 포션 등 누락된 물약 추가). 아이템 툴팁 제대로 나오도록 렌더링 수정.

11. 대장간 및 강화 (대규모 개편)
- **강화 GUI**: 현재 들고 있는 아이템 기준에서, 대장간 강화 탭 진입 시 **인벤토리를 열어 강화할 아이템을 직접 선택**하는 로직으로 변경.
- **수리 및 무기 상점**: 대장간 NPC에 무기/방어구 내구도 수리 기능 추가. 수리 비용은 깎인 내구도에 비례하며, 희귀도/재료/모드 장비 여부에 따라 요구 크레딧이 크게 증가. 간단한 무기 판매 기능도 추가.
- **강화 스케일링 & 호환성**: 강화에 따른 데미지 증가폭을 대폭 상향. (네더라이트 10강 기준 +4는 너무 낮음). 바닐라 장비 외 타 모드 무기(TacZ 등 1.20.1 Forge 기준)도 강화될 수 있도록 호환성 작업.
- **시각 효과(VFX) & 이름**: 강화 성공 시 무기 이름 앞에 수치 부여 (예: "+10 네더라이트 검"), 툴팁 내 표시 강화 및 무기에 VFX 이펙트 추가.

12. 사냥꾼 NPC (신규)
사냥꾼 직업과 연계된 상인. 여러 종류의 활과 화살, 사냥에 도움이 되는 물품을 판매하며, 몬스터 전리품을 납품받음.

13. 토지 상인
토지와 신분을 사고파는 상인. 
**이슈**: 현재 남아있는 "토지서 문제" 버그 최우선 확인 및 픽스 예정.

14. 경매장 상인
수수료(10%)를 내고 아이템을 유저간 매매할 수 있도록 돕는 상인.

*참고: 납품받는 상인들은 매일 기상 상태나 사정에 따라 일정 확률로 납품 가격이 달라지는 수요 시스템이 적용되어야 함.*

---
(이하 토지, 거래, 경매 시스템 기존 세부 기획은 생략 없이 원래 룰을 따릅니다)
- 토지 (서민, 중산층, 부자, 거부, 억만장자 단계별 권한)
- 거래 시스템 (/거래 명령어, 전용 GUI)
- 경매장 (익명 검색, 수수료, 매물 확인)
