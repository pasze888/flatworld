# Flat World Dimension (flatworld)

[English](README.md) | [简体中文](README.zh-CN.md)

A NeoForge 1.21.1 mod that adds a superflat dimension to your world, with a `/flatworld` command to travel
between the overworld and it. Time, weather and mob spawning in the dimension are all configurable.

## Features

- Adds the superflat dimension `flatworld:flat_world` — a flat plain of bedrock, stone, dirt and grass
  (surface at y≈-1).
- **Configurable time**: pinned to noon (`DAY`), pinned to midnight (`NIGHT`), or advancing on its own (`CYCLE`).
- **Configurable weather**: `CLEAR`, `RAIN` or `THUNDER`.
- **No natural mob spawning**: the dimension uses its own biome `flatworld:flat_plains` with empty `spawners`,
  so the natural spawning cycle produces nothing. Mob spawners and structure spawning are unaffected.
- `/flatworld` travels both ways and picks a safe landing spot near the surface.

## Installation

1. Install Minecraft 1.21.1 and NeoForge 21.1.x.
2. Drop `flatworld-<version>.jar` into your `mods/` folder.

## Getting started

Run `/flatworld` to travel both ways depending on the dimension you are in:

- In **any other dimension** (e.g. the overworld) → travel to the flat world dimension (world spawn);
- Already in the **flat world dimension** → back to the overworld (respawn point/bed if set, otherwise world spawn).

The required permission level is set by the `commandPermissionLevel` config entry, default `0` (every player).

## Configuration

The config file is `config/flatworld-common.toml`, created on first launch:

| Entry | Default | Description |
|---|---|---|
| `commandPermissionLevel` | `0` | Minimum permission level required to run `/flatworld` (0 = all players, 1-4 = operator levels). |
| `timeMode` | `DAY` | `DAY` = pinned to noon (6000), `NIGHT` = pinned to midnight (18000), `CYCLE` = advances on its own and still obeys the `doDaylightCycle` game rule. |
| `weatherMode` | `CLEAR` | `CLEAR`, `RAIN` or `THUNDER`. |

> While `timeMode` is `DAY` or `NIGHT`, `/time set` has no effect inside the dimension; while `weatherMode`
> is pinned, neither does `/weather` — the config is the single source of truth. Mode changes apply
> immediately, with no restart needed.

## Commands

| Command | Description |
|---|---|
| `/flatworld` | Travel between the overworld and the flat world dimension. |

## Documentation

- [Design: time and weather modes](docs/design/time-weather-modes.md)
- [Reference: dimension registration and verified API facts](docs/reference/dimension-registration.md)
- [Troubleshooting: environment and build](docs/troubleshooting.md)

## Building

```bash
./gradlew build     # compile and package to build/libs/flatworld-<version>.jar
./gradlew runData   # regenerate the dimension type JSON (after editing ModDimensions#bootstrap)
./gradlew runClient # launch the development client
```

## License

MIT — see [LICENSE](LICENSE).
