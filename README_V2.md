# RynixRtpq v2.0 - McPlugin Format - Duo 2 Players 1 Location | Author RinZz

> Converted from config bạn gửi, giữ nguyên lệnh `/rtpq` và `/rtpqueue` đều có GUI

## ✅ Yêu cầu của bạn đã làm

### 1. Config format McPlugin bạn gửi
Đã convert toàn bộ sang RynixRtpq, giữ nguyên cấu trúc:
- `queue.hash: "/rtpqueue"` + aliases `/rtpq`, `/mchashqueue`
- `queue.invite-time: 60`, `invite-cooldown: 60`, `world-default`, `damaged-cancel`
- `list-world.world.require: 2`, `interval: 5`, `safe: true`, `center`, `radius`, `attempts`, `unsafe-block: [WATER, LAVA, VOID]`
- `menu.size: 27` với `leave` (RED), `join` (GREEN), `world` (GRASS), `queue` (BOOK), `invite` (NETHER_STAR)
- `world-menu.size: 9`, `invite-menu` với next slot 53, previous slot 45
- `boss-bar`, `title`, `action-bar`, `boardcast`, `message`, `sound` đầy đủ

### 2. Lệnh /rtpq và /rtpqueue đều có GUI
**plugin.yml:**
```yaml
commands:
  rtpq:
    aliases: [rtp, randomtp, wild, rtpduo]
    description: Has GUI
  rtpqueue:
    aliases: [rynixrtpq, rtpqadmin, rrtp, rtpqgui, mchashqueue]
    description: Has GUI
```

- `/rtpq` (không arg): Mở GUI chính (27 slots) như McPlugin
- `/rtpq gui`: Mở GUI
- `/rtpq [world]`: Join queue world đó
- `/rtpq invite <player>`: Mời duo cùng chỗ
- `/rtpq accept <player>`: Chấp nhận lời mời duo
- `/rtpqueue`: Mở GUI chính
- `/rtpqueue world`: Mở world selector
- `/rtpqueue invite`: Mở invite menu 54 slots phân trang

**GUI Main Menu (size 27) - từ config menu:**
- Slot 10: RED_STAINED_GLASS_PANE - LEAVE - Rời queue
- Slot 16: GREEN_STAINED_GLASS_PANE - JOIN - Tham gia duo
- Slot 12: GRASS_BLOCK - WORLD - Chọn world {world}
- Slot 13: BOOK - QUEUE - Waiting {count}, cần {need}
- Slot 14: NETHER_STAR - INVITE - Mời bạn bè duo
- Slot 4: MAGENTA_BED - DUO MODE info (thêm)

**World Menu (size 27):**
- Hiện tất cả world từ list-world, click chọn world để join queue

**Invite Menu (size 54):**
- Hiện online players dạng PLAYER_HEAD
- Slot 53: GREEN - NEXT page
- Slot 45: RED - PREVIOUS page
- Slot 49: BARRIER - Close
- Click đầu player = gửi lời mời duo

### 3. Tính năng 2 player lại 1 chỗ (Duo Mode)
Giữ nguyên tính năng bạn chọn trước: 2 người trong queue sẽ tele cùng 1 tọa độ.

**Logic mới theo McPlugin:**
- Mỗi world có `require: 2` và `interval: 5` riêng
- Queue nhóm theo world: ví dụ world có 2 người -> tìm match
- Khi đủ require, broadcast `Found the match in {world}` + title `&b&lFound the match`
- Đếm ngược interval 5s, action-bar `Teleport in ${interval}`
- Sau interval, tìm 1 safe location duy nhất, tele cả 2 cùng chỗ + spread 2 blocks
- Phát sound teleport, success, title `DUO!`

**Ví dụ:**
- A `/rtpq` world -> join queue world (1/2)
- B `/rtpq` world -> join queue world (2/2) -> Found match!
- Broadcast: `A, B -> Teleporting together to same location!`
- Countdown: `Teleport in 5,4,3,2,1`
- Cả 2 tele tới X=200 Z=-300 cùng nhau

**Invite System (mới từ McPlugin):**
- A đang trong queue `/rtpq invite B` -> gửi lời mời
- B nhận message clickable: `Bạn được A mời duo RTP [Click Here]` (click chạy `/rtpq accept A`)
- Invite tồn tại 60s, cooldown 60s
- Khi B accept, tự join queue cùng world với A
- Nếu B đã trong queue rồi thì ghép duo luôn

**Damaged Cancel:**
- Nếu `damaged-cancel: true` và player trong queue bị đánh -> tự rời queue + message `Bạn bị tấn công nên đã rời khỏi hàng đợi`

### 4. Tối ưu & Log Author RinZz Xịn + Folia

- **SchedulerAdapter**: FoliaScheduler (Region, Global, Async, Entity) + PaperScheduler auto detect
- **QueueManager**: ConcurrentLinkedQueue, per-world grouping, BossBar per world
- **RTPService**: getChunkAtAsync, teleportAsync, per-world unsafe-block check
- **SoundManager**: Đọc config sound.* (command, join, leave, teleport, success, time, click, break)
- **Logger xịn**: Banner RynixRtpq Duo v2.0 + Author RinZz + Rynix Studio
- **Folia Supported**: folia-supported true, test teleportAsync

## 📁 Files chính v2.0

```
com.rynix.rtpq
├── RynixRtpqPlugin.java (register /rtpq, /rtpqueue, /mchashqueue)
├── config/ConfigManager.java (parse McPlugin format + duo)
├── queue/QueueManager.java (per-world require, interval, broadcast)
├── rtp/RTPService.java (unsafe-block per world, duo same location)
├── gui/RtpqGUI.java (main menu from menu.* config)
├── gui/WorldMenuGUI.java (world-menu)
├── gui/InviteMenuGUI.java (invite-menu paginated)
├── gui/GUIListener.java (handle 3 GUIs)
├── invite/InviteManager.java (60s invite, clickable, cooldown)
├── listeners/PlayerListener.java (damage cancel, quit)
├── utils/LoggerUtil.java (banner v2.0 Duo), SoundManager, ChatUtil, CooldownManager
```

## 🔧 Build & Cài

```bash
cd RynixRtpq
mvn clean package
# jar: target/RynixRtpq-2.0.0-DUO.jar -> đổi thành RynixRtpq.jar bỏ vào plugins
```

Config mặc định là file bạn gửi nhưng đã thêm header RynixRtpq + duo settings, có GUI.

Cần thêm tính năng gì nữa: party 3-4 người, chọn bạn duo cố định, hay random ghép?
