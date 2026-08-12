# MagicCore Discord Bot

This process is deployed separately from Paper/Folia. It connects to Discord through JDA and to MagicCore's loopback bridge through authenticated, replay-protected HTTP envelopes.

Required environment variables:

- `DISCORD_BOT_TOKEN`
- `DISCORD_NOTIFICATION_CHANNEL_ID`
- `MAGICCORE_DISCORD_BRIDGE_SECRET` (same 32+ byte value used by the plugin)
- `MAGICCORE_BRIDGE_URL` (defaults to `http://127.0.0.1:8765/bridge`)

Build with `gradlew :discord-bot:shadowJar`, then run the all-in-one JAR from `discord-bot/build/libs/`.

The bot provides `/link code:<code>` and `/magiccore-health`, polls the durable outbox, acknowledges successful sends, and reports failed sends for bounded retry/dead-letter handling. It has no access to the Minecraft server process or Bukkit API.
