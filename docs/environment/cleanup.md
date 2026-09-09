# Cleanup

## Remove a sandbox

```sh
sbx rm pd-agent-a     # destroys VM, private daemon, all state
sbx rm pd-agent-b
```

## Remove all sandboxes

```sh
sbx ls                # list all
sbx rm <name>         # remove each
```

## Remove network policy

```sh
sbx policy rm         # removes all allow rules
```

## Remove agent branches (optional)

```sh
git branch -d agent/a agent/b
git push origin --delete agent/a agent/b
```

## Remove GitHub repo (nuclear option)

```sh
gh repo delete borresh/program-dashboard --yes
```

## Full teardown order

1. `sbx rm pd-agent-a` + `sbx rm pd-agent-b`
2. `sbx policy rm` (optional)
3. `git push origin --delete agent/a agent/b` (optional)
4. `gh repo delete` (optional)
5. Host worktree remains (just a git clone)
