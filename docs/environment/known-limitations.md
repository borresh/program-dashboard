# Known Limitations

## sbx version

- Current: v0.39.0. Upgrade to v0.42.1 recommended (fixes Windows mount-path issue).
- Upgrade via: `winget upgrade Docker.sbx` (PowerShell on Windows).

## Big Pickle model

- `opencode/big-pickle` is free for a limited time.
- Free-period traffic may be used to improve the model (per Zen docs).
- No SLA guarantees during free period.

## JDK mismatch

- Project needs JDK 25. Host has JDK 26 (`~/.jdks/openjdk-26.0.2.1`).
- JDK 26 is backward-compatible — works fine. Sandboxes may have different JDK versions.

## Port collisions

- Personal stack and sandboxes use separate Docker daemons — no port collisions.
- If running personal stack simultaneously with both sandboxes during heavy builds, expect high resource usage (32 CPUs, 31 GiB RAM available).

## `gh` CLI

- Installed to `~/.local/bin/gh` (not on default PATH).
- Requires `export PATH="$HOME/.local/bin:$PATH"` or full path usage.
- Requires `gh auth login` before first use.
