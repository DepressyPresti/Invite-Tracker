
# DomainInviteTracker

Track **unique player joins per domain** and post live updates to **Discord** — perfect for affiliate/creator invite tracking across multiple subdomains behind Velocity/Waterfall.

Built for **Purpur/Paper 1.21**. Uses **AsyncPlayerPreLoginEvent** (no LuckPerms nag) and an embedded **JDA** bot (shaded) with milestone logic and message replacement.

---

## ✨ Features

- 🧭 Detects the **exact hostname** players typed (e.g., `danasty.ashesofheaven.co.uk`)  
- 🧮 Counts **unique** joins per *domain + promoter* (re-joins don't increment)  
- 🪄 **Milestones** with dynamic titles: `3, 5, 7, 10, 12, 15, 17, 20, 22, 25, 27, 30, 32, 35, 37, 40, 42, 45, 47, 50` and **every +2** after `50`  
- 🔁 **Replaces** the previous embed for that promoter on each new unique join (clean tracking thread)  
- 🏷️ Embed includes: promoter ping, Minecraft username, domain, total invites, footer with `Invites: X • Milestone: Y`  
- ⚙️ `/tracker reload` hot‑reloads config and restarts the bot if the token changed  
- 🧪 Robust hostname loading: supports **FQDN keys** and YAML’s nested sections caused by dots in keys

---

## 📦 Requirements

- Java 17+
- Purpur or Paper **1.21.x**
- A Discord bot (token) that can post in your target channel

---

## 🚀 Installation

1. Download the latest shaded JAR from Releases (or build it yourself, see below).
2. Drop it into your server’s `plugins/` folder.
3. Start the server once to create `plugins/DomainInviteTracker/config.yml`.
4. Put your **Discord bot token** and domain mappings in `config.yml`.
5. Run `/tracker reload` or restart the server.

---

## ⚙️ Configuration

**`plugins/DomainInviteTracker/config.yml`**

> **Tip about dots in YAML keys:** Bukkit treats dots as path separators. This plugin handles both **full keys** and **nested sections** transparently.

### Option A — Full FQDN keys (simple)
```yaml
discord:
  botToken: "PUT_YOUR_TOKEN_HERE"

domains:
  danasty.ashesofheaven.co.uk:
    channelId: "1403060253364588604"
    userId: "184058325246148609"

  katvenly.ashesofheaven.co.uk:
    channelId: "1403060253364588604"
    userId: "573732145349132288"
```

### Option B — Nested sections (what Bukkit/YAML does under the hood)
```yaml
discord:
  botToken: "PUT_YOUR_TOKEN_HERE"

domains:
  danasty:
    ashesofheaven:
      co:
        uk:
          channelId: 1403060253364588604  # quotes optional
          userId: 184058325246148609
```

> **IDs can be quoted or numeric;** the plugin reads them safely either way.

### What gets tracked
- The plugin normalizes the hostname from the login event (lowercase, strips `:port`) and matches it to your `domains` keys.
- Only **listed** domains trigger Discord messages; everything else is ignored.

---

## 🛡️ Discord Bot Setup (Private & Minimal)

- **Public Bot:** OFF (keeps the bot private)
- **Requires OAuth2 Code Grant:** OFF
- **Scopes:** `bot`
- **Permissions:** at minimum `View Channels`, `Send Messages`, `Embed Links` (optionally `Read Message History`)
- **Gateway Intents:** none required (no privileged intents needed)

> The plugin shades JDA and includes an SLF4J binding to avoid console spam.

---

## 🧩 Milestones

Milestone **numbers** are assigned in order when the **total unique invites** hits the values below:

```
3, 5, 7, 10, 12, 15, 17, 20, 22, 25, 27, 30, 32, 35, 37, 40, 42, 45, 47, 50, then every +2 after 50
```

When a milestone is reached, the embed title becomes:  
`<@PROMOTER_ID> has reached milestone N`

The embed **footer** always shows:  
`Invites: X • Milestone: Y`  
where **Y** is the highest milestone reached so far for that promoter+domain.

---

## 🧰 Commands & Permissions

| Command            | Permission              | Default |
|--------------------|-------------------------|---------|
| `/tracker reload`  | `invitetracker.reload`  | OP      |

- Reloads `config.yml`, domain mappings, and restarts the Discord bot token if it changed.

---

## 💾 Storage

- File: `plugins/DomainInviteTracker/data.yml`
- Saves:
  - Per‑promoter, per‑domain **UUIDs** that have already counted (ensures uniqueness)
  - Last Discord **message ID** so the next unique join can **replace** it

> To reset counts, stop the server and remove the relevant sections (or the file).

---

## 🔧 Build From Source

This is a standard Maven project that shades JDA:

```bash
mvn -q package
```

Output: `target/domain-invite-tracker-<version>-shaded.jar`

Key deps (in `pom.xml`):
- `io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT` (provided)
- `net.dv8tion:JDA:5.x`
- `org.slf4j:slf4j-simple:2.0.x`

---

## 🐛 Troubleshooting

- **“Skipping domain 'XYZ' due to missing channelId or userId.”**  
  Your domain key doesn’t resolve to a section with both fields. Ensure you used **Option A** *or* **Option B** formats above.

- **“[HorriblePlayerLoginEventHack] … [LuckPerms]”**  
  That warning is from LuckPerms still listening to `PlayerLoginEvent`. This plugin uses `AsyncPlayerPreLoginEvent` and doesn’t cause the nag. Safe to ignore.

- **No Discord messages**  
  - Verify the bot token and that the bot is in the server.
  - Ensure the bot has **View**, **Send**, and **Embed** in the target channel.
  - Confirm players are joining with a **listed** domain.
  - Check `data.yml` to see if the UUID already counted (uniqueness).

---

## 🔐 Security

Treat your **Discord bot token** like a password. If it was ever exposed, **reset it** in the Developer Portal and update `config.yml`.

---

## 📜 License

MIT (or your preferred license).

---

## 🤝 Credits

Created for the LostInBedrock ecosystem and friends. PRs welcome for enhancements like per-domain leaderboards or `/tracker test <domain>`.
