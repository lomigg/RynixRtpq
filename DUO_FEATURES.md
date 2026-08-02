# RynixRtpq v1.1.0 DUO - 2 Players 1 Location | Author RinZz

## Changelog từ yêu cầu mới của bạn

### 1. Đổi lệnh về /rtpq và /rtpqueue (như yêu cầu)
**plugin.yml mới:**
```yaml
commands:
  rtpq:
    description: Join RTP queue - Duo Mode - 2 players same location
    usage: /rtpq [world]
    aliases: [rtp, randomtp, wild, rtpduo]
  rtpqueue:
    description: Main GUI & Admin command for RynixRtpq Duo
    usage: /rtpqueue <gui|reload|clear|stats|duo>
    aliases: [rynixrtpq, rtpqadmin, rrtp, rtpqgui]
```

- `/rtpq [world]` : Tham gia hàng đợi. Không có arg thì vào world default. Có `gui` thì mở GUI.
- `/rtpqueue gui` : Mở GUI
- `/rtpqueue stats/duo` : Xem thống kê duo
- `/rtpqueue list` : Xem list duo được nhóm theo cặp [DUO 1] A + B
- Tương thích ngược: vẫn hỗ trợ `/rtp`

### 2. Tính năng DUO - 2 Player lại 1 chỗ

**Config mới:**
```yaml
duo:
  enabled: true
  size: 2  # 2 người 1 chỗ, có thể đổi thành 3,4 nếu muốn party
  same-location: true
  spread-distance: 2 # 2 người sẽ cách nhau 2 blocks để không kẹt
  solo-after-seconds: 60 # Nếu đợi 60s không đủ 2 người, tự tele solo
  notify-partner: true
  require-same-world: false
```

**Logic QueueManager mới:**
- Khi `duo.enabled = true`, queue phải đủ `duo.size` (default 2) người mới bắt đầu tìm location
- `getNextPlayers(2)` lấy 2 người từ priorityQueue -> normalQueue
- `teleportDuoWithRetry(List<Player>, world)`:
  - Tìm 1 vị trí an toàn DUY NHẤT bằng async
  - Player 1 tele đúng vị trí đó
  - Player 2+ tele vị trí đó + random offset trong `spread-distance` để không chồng lên nhau
  - Kiểm tra safe Y cho từng vị trí spread
  - Gửi message `teleported-duo: Đã tele cùng {partner}` cho từng người
  - Cooldown + invulnerability cho cả 2

**Ví dụ flow:**
1. Player A `/rtpq` -> Queue: 1/2 - đang chờ thêm 1 người
2. Player B `/rtpq` -> Queue: 2/2 - Đủ người!
3. Console log: `[RynixRtpq] DUO TELEPORT: A, B -> same location in world world`
4. Cả A và B nhận: `DUO TELEPORT! Bạn và B đã được tele cùng nhau!`
5. Cả 2 được tele tới X=1230 Z=-500 cùng nhau, cách nhau 2 blocks

**Xử lý edge cases:**
- Nếu 1 người offline khi đang chờ duo -> tự động bỏ người đó, tìm người mới
- Nếu `require-same-world=true` mà 2 người chọn world khác nhau -> fallback tele solo
- Nếu chỉ có 1 người đợi quá 60s -> tele solo luôn (tránh đợi vô hạn)
- BossBar/ActionBar hiển thị `Cần {need} người` để biết còn thiếu bao nhiêu

### 3. GUI update cho Duo

- Title đổi thành `● RynixRtpq | DUO Queue ●`
- Thêm item `MAGENTA_BED` ở slot 15 hiển thị Duo Mode info:
  - Khi đủ X người
  - Tất cả tele cùng 1 chỗ
  - Spread bao nhiêu blocks
  - Hiện tại bao nhiêu người chờ
- Player heads hiển thị `Duo: {duoIndex}/2` để biết mình thuộc cặp nào
- Slot world selector vẫn giữ
- Refresh + Close như cũ

### 4. Messages mới

```yaml
joined-queue: "Đã tham gia hàng đợi RynixRtpq Duo! Vị trí: #{position} | Tổng: {total} | Chờ đủ {need} người để tele cùng!"
teleported-duo: "DUO TELEPORT! Bạn và {partner} đã được tele cùng nhau!"
duo-waiting: "Đang chờ đủ {need} người để duo cùng! Hiện tại: {total}/2"
duo-found: "Đã tìm thấy duo! {player1} + {player2} -> Đang tele cùng nhau!"
```

### 5. Tối ưu & Folia vẫn giữ nguyên

- Vẫn dùng `SchedulerAdapter` -> Folia + Paper
- `ConcurrentLinkedQueue` thread-safe
- `getChunkAtAsync` + `teleportAsync`
- Cache ItemStack filler
- Log author RinZz xịn v1.1.0-DUO

## Test thử

```
/rtpq -> A join queue -> thông báo chờ 1 người nữa
/rtpq -> B join queue -> cả 2 tele cùng chỗ
/rtpqueue gui -> mở GUI thấy 2 đầu player ở 2 slot đầu, cùng duo group
/rtpqueue list -> thấy [DUO 1] A + B
```

Cần mình thêm tính năng invite chỉ định người duo chung không? Hiện tại là random ghép cặp trong queue, nếu muốn `/rtpq invite <player>` để chọn bạn duo cụ thể thì bảo mình code thêm!
