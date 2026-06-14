# docsscanner

Monorepo for the PocketScan Android app and the Android Dev Fleet of agents/skills/commands used to build it.

## Layout

```
docsscanner/
├── pocketscan/        # PocketScan Android app — Kotlin + Jetpack Compose
├── fleet/             # Android Dev Fleet — Claude Code plugin (skills, agents, commands)
└── docs/              # Public landing page (one-pager with QR download)
```

### pocketscan/
Production-quality, offline-first Android document scanner. Scans paper documents via Google ML Kit and saves PDFs to **Downloads/PocketScan/** on the device. No backend, no analytics, no Internet permission.

- Build: `cd pocketscan && ./gradlew :app:assembleDebug`
- Signed release APK: [`pocketscan/distribution/PocketScan-1.0-release.apk`](pocketscan/distribution/PocketScan-1.0-release.apk)
- App details: [`pocketscan/README.md`](pocketscan/README.md)
- Spec: [`pocketscan/SPEC.md`](pocketscan/SPEC.md)

### fleet/
A Claude Code plugin packaging the orchestration that built PocketScan: a manager agent, a tech-spec writer (spec-first gate), a CI agent, an emulator-verify agent, plus skills for scaffolding native Kotlin/Compose and Expo apps and generating Compose screens. Designed for parallel execution.

- Plugin manifest: [`fleet/.claude-plugin/plugin.json`](fleet/.claude-plugin/plugin.json)
- Conventions: [`fleet/CLAUDE.md`](fleet/CLAUDE.md)
- Overview: [`fleet/README.md`](fleet/README.md)

### docs/
Static, self-contained HTML landing page with a QR code that points to the Drive folder hosting the signed APK.

- Open: `open docs/index.html`

## License

MIT — see [LICENSE](LICENSE).
