# Directory Layout

```
~/dev/projects/program-dashboard/          main @ main (IntelliJ + sbx workspace)
├── .sbx/                                  task briefs for sandbox agents
│   ├── agent-a.md
│   ├── agent-b.md
│   └── README.md
├── docs/environment/                      this documentation
├── backend/                               Spring Boot application
├── frontend/                              Angular application
├── e2e/                                   Playwright browser checks
├── docker-compose.yml                     production-like stack
├── docker-compose.e2e.yml                 e2e test overrides
├── .env.example                           environment template (no secrets)
├── up.sh                                  build + bring up stack
├── e2e.sh                                 run browser checks
└── prompts/                               original program prompts

~/dev/sbx_opencode_configuration/          shared opencode agent config (unversioned)
├── AGENTS.md                              baseline agent instructions
├── opencode.json                          model + permissions config
└── .opencode/                             agents, commands, skills, references
```

In-sandbox layout (ephemeral, inside each microVM):
```
/home/agent/workspace/                     in-sandbox clone (private, writable)
/run/sandbox/source/                       host repo (read-only mount)
~/.config/opencode/                        copied shared agent config
```
