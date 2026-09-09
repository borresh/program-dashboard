# Troubleshooting

## sbx commands fail from WSL

All sbx commands run via `powershell.exe`:
```sh
powershell.exe -Command "& 'C:\Users\borre\AppData\Local\DockerSandboxes\bin\sbx.exe' <command>"
```

## `./up.sh` fails with "no JDK found"

Install JDK 25 via SDKMAN:
```sh
curl -s "https://get.sdkman.io" | bash
sdk install java 25-tem
```

Or set `JAVA_HOME` manually:
```sh
export JAVA_HOME=~/.jdks/openjdk-26.0.2.1
```

## Port already in use

Check what's using the port:
```sh
netstat -tlnp 2>/dev/null | grep :8080
# Stop the conflicting service or change ports in .env
```

## Docker Desktop not running

```sh
docker info | grep "Name"   # should show "docker-desktop"
# Start Docker Desktop on Windows if not running
```

## SSH auth fails for GitHub push

```sh
eval "$(ssh-agent -s)" && ssh-add ~/.ssh/id_ed25519
ssh -T git@github.com     # should show "Hi borresh!"
```

## Sandbox can't reach GitHub

Check sbx network policy:
```sh
sbx policy ls   # verify github.com:443 is allowed
```

## `gh` CLI not found

```sh
export PATH="$HOME/.local/bin:$PATH"
gh --version
```
