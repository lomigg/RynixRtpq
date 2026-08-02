# RynixRtpq - Duo RTP Queue - 2 Players Same Location

> **Author: RinZz | Rynix Studio | Premium Optimized | Folia Supported | Java 21-25 | All MC 1.8-1.21.5+**

Premium Optimized RTP Queue with Inventory GUI - Duo Mode - 2 players teleport to same location - McPlugin format compatible.

![Java](https://img.shields.io/badge/Java-21--25-orange?style=for-the-badge&logo=openjdk)
![Minecraft](https://img.shields.io/badge/Minecraft-1.8--1.21.5%2B-green?style=for-the-badge&logo=minecraft)
![Folia](https://img.shields.io/badge/Folia-Supported-blue?style=for-the-badge)
![Author](https://img.shields.io/badge/Author-RinZz-purple?style=for-the-badge)

## ✨ Features

### 🎮 Commands - Both have GUI
- `/rtpq` - Open Duo Queue GUI (Main)
- `/rtpqueue` - Open Duo Queue GUI + Admin
- Click in GUI to Join/Leave/World/Invite - No need `/rtpq invite` command

### 👥 Duo Mode - 2 Players 1 Location
- `require: 2` - Need 2 players to start teleport
- `interval: 5` - Countdown 5s before teleport
- Both players teleport to SAME random location (spread 2 blocks)
- Invite system via GUI click (NETHER_STAR -> Player Head)

### 🖼️ 3 GUIs from McPlugin config
- **Main Menu (27 slots)**: JOIN (GREEN), LEAVE (RED), WORLD (GRASS), QUEUE (BOOK), INVITE (NETHER_STAR), DUO INFO (MAGENTA_BED)
- **World Menu (27 slots)**: Select world to queue
- **Invite Menu (54 slots)**: Paginated player list, NEXT slot 53, PREVIOUS slot 45

### 🚀 Optimized & Folia
- `SchedulerAdapter` - FoliaScheduler + PaperScheduler auto detect
- `ConcurrentLinkedQueue` + `ConcurrentHashMap` thread-safe
- `getChunkAtAsync` + `teleportAsync` - no lag
- BossBar, ActionBar, Title, Sound from config

### 🛡️ Bug Fixes v2.2
- Fixed `findSafeLocationSync` ignoring attempts config
- Fixed `getDuoSoloAfterSeconds` hardcoded 0
- Added full config validation (require:0, min-radius>radius, minY>maxY, delay:0)
- Fixed `processing` volatile for Folia race condition

### ☕ Java & MC Support
- **Java**: 21, 22, 23, 24, 25+ (and legacy 8-20 via reflection)
- **MC**: 1.8 - 1.21.5+ (api-version 1.16)
- **Server**: Spigot, Paper, Purpur, Pufferfish, Folia

## 📦 Installation

1. Download `RynixRtpq-2.1.0-JAVA21-25.jar` from Releases
2. Put into `plugins/`
3. Restart server
4. Type `/rtpq` or `/rtpqueue` to open GUI

## 🔧 Config

Full McPlugin format compatible - see `src/main/resources/config.yml`

```yaml
queue:
  hash: "/rtpqueue"
  invite-time: 60
  invite-cooldown: 60
  world-default: world
  damaged-cancel: true

list-world:
  world:
    require: 2
    interval: 5
    safe: true
    center: {x: 0, z: 0}
    radius: 500
    attempts: 20
    unsafe-block: [WATER, LAVA, VOID]

menu:
  size: 27
  join: {material: GREEN_STAINED_GLASS_PANE, slot: 16}
  leave: {material: RED_STAINED_GLASS_PANE, slot: 10}
  world: {material: GRASS_BLOCK, slot: 12}
  queue: {material: BOOK, slot: 13}
  invite: {material: NETHER_STAR, slot: 14}
```

## 👤 Author

**RinZz - Rynix Studio**

- GitHub: [RinZz](https://github.com/RinZz)
- Version: 2.2.0-BUGFIX
- Discord: Rynix Studio

## 📝 License

Premium - All rights reserved - Rynix Studio

---
**Built with ❤️ by RinZz | RynixRtpq Duo v2.2 | Java 21-25 | Folia Supported**
