# Ports

## Host (personal stack)

| Port | Service | Binding |
|------|---------|---------|
| 8080 | Backend API | `127.0.0.1:8080` |
| 4200 | Frontend dev server | `127.0.0.1:4200` |
| 5432 | PostgreSQL | `127.0.0.1:5432` |

All bound to localhost only — not accessible from LAN.

## Sandboxes

Each sandbox uses the VM's internal loopback. Ports don't collide between sandboxes or with the host.

To expose a sandbox port to Windows:
```sh
sbx ports pd-agent-a     # shows port mappings
# Example: 127.0.0.1:8081 → 8080 (backend inside sandbox A)
```

## Verification

```sh
# Check ports are free before starting
netstat -tlnp 2>/dev/null | grep -E ':(8080|4200|5432)' || echo "ports free"
```
