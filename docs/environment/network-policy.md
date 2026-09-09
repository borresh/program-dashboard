# Network Policy

Sandbox network access is controlled by sbx policy (Balanced mode).

## Allowed outbound

| Domain | Port | Purpose |
|--------|------|---------|
| `opencode.ai` | 443 | OpenCode Zen API |
| `repo.maven.apache.org` | 443 | Maven dependencies |
| `repo1.maven.org` | 443 | Maven dependencies |
| `registry.npmjs.org` | 443 | npm packages |
| `registry-1.docker.io` | 443 | Docker images |
| `auth.docker.io` | 443 | Docker auth |
| `production.cloudflare.docker.com` | 443 | Docker CDN |
| `github.com` | 443 | GitHub HTTPS |
| `ssh.github.com` | 443 | GitHub SSH |
| `api.github.com` | 443 | GitHub API |

## Default deny

All other outbound traffic is blocked by the Balanced policy.

## Policy management

```sh
sbx policy ls                           # list current rules
sbx policy allow network <host>:<port>  # add rule
sbx policy rm                           # remove all rules
```
