# Tom's Simple Storage 1.7.10 Port

[简体中文](README_zh.md)

Tom's Simple Storage 1.7.10 Port is a lightweight backport of [Tom's Simple Storage](https://github.com/tom5454/Toms-Storage) for Minecraft 1.7.10 and the GT New Horizons ecosystem.

The mod is intended for early-game inventory management and simple automation. It provides convenient access to ordinary containers without replacing the larger storage and logistics systems available later in a GTNH playthrough. This is a focused port rather than a complete backport of every upstream feature.

## Download

- [Latest GitHub release](https://github.com/liansishen/TomsStorageNH/releases/latest)
- [All releases](https://github.com/liansishen/TomsStorageNH/releases)

## Required Dependencies

- NotEnoughItems, using the GTNH line or a compatible version
- UniMixins
- GTNHLib
- ModularUI2

The NEI integration is provided by ModularUI2's NEI support plus one client-side mixin. A compatible NEI build must provide `AutoCraftingManager`.

NotEnoughCharacters is optional and improves Chinese and pinyin searching.

## Features

### Storage Network

- Inventory Connectors combine touching inventories into one storage network.
- Inventory Trim extends the connected path between inventories.
- Storage Terminals provide centralized insertion, extraction, sorting and searching.
- Crafting Terminals add a crafting grid with NEI recipe transfer and auto-crafting integration. A toggle beside the crafting output chooses whether results go to your inventory or into the storage network.
- Crafting is paid for out of the network, like AE2: the 3x3 is the pattern you arrange, not the stock being eaten, so it stays as you left it and you can keep crafting well past the one stack that fits in it. If the network runs out of an ingredient the grid itself is used instead, and depletes normally. Shift-clicking the result stops at a full stack by default; see `shiftCraftLimit`. Whatever is left in the grid is returned to the network when the terminal is closed.
- Every terminal shows two bars in its header: slots used out of the network total, and items stored out of the network's capacity. Capacity is summed from each inventory's own slot limit, so a drawer or barrel counts for what it can really hold. Hover either bar for the exact numbers.
- Wireless Terminals can be bound to a terminal and used within the configured range and dimension.
- Placing or breaking this mod's own blocks updates the network immediately. A container placed or broken deeper in a chain of containers is picked up by a periodic rescan instead, so it can take a few seconds to appear (see `networkTopologyRescanInterval`). Items moved in or out by other mods are noticed by a continuous background sweep (see `networkSweepSlotsPerTick`).

### Search

- Searches item names and tooltips; prefix a query with `@` to search by mod.
- Optional NotEnoughCharacters support for Chinese and pinyin matching.
- One search button cycles through four presets:
  1. Standard search
  2. Auto search
  3. Auto search with retained text
  4. Auto search with retained text and NEI synchronization
- Search state is stored locally, so multiple players using the same terminal do not overwrite each other.

### Basic Automation

- Inventory Cables connect only to other cables, an Inventory Connector, or the network side of a Basic Inventory Hopper.
- Cables do not directly connect ordinary inventories, machines, terminals or Inventory Trim.
- Multiple hoppers may share one cable network. A cable network containing multiple Inventory Connectors is disabled as a conflict.
- The Basic Inventory Hopper transfers one item every 10 ticks and respects sided inventories and redstone disabling.
- Its wide end is the input and its narrow nozzle is the output:
  - Connect the cable to the wide end to export from the storage network into the nozzle-side inventory.
  - Connect the cable to the narrow end to import from the wide-side inventory into the storage network.
- Exporting from the storage network requires an item filter. Import filters are optional.
- Right-click the hopper with an item to set its filter, or sneak-right-click it with an empty hand to clear the filter.

### Inventory Connector Range Preview

- Sneak-right-click an Inventory Connector with an empty hand to toggle a colored spherical range preview.
- The sphere represents the local inventory/Trim scan radius. Inventories still require a continuous connected path.
- Cable networks use their own node limit and are not restricted by this sphere.

## Configuration

Storage network tuning, in `config/tomsstorage.cfg`:

- `networkTopologyRescanInterval` (default 100 ticks) — how often an Inventory Connector re-walks its network to find blocks placed or removed by other mods. Blocks from this mod are picked up immediately, so this only bounds how long a foreign chest takes to appear.
- `networkSweepSlotsPerTick` (default 512) — how many network slots each world re-reads per tick to notice items moved by other mods. Higher reacts faster to outside changes, lower costs less.
- `debugNetworkStats` (default false) — adds a statistics line (members, slots, distinct items, last rebuild time) when right-clicking an Inventory Connector.
- `neiAutocrafting` (default true) — lets NEI bookmark autocrafting draw ingredients from the storage network. Synced from the server, so a server that turns it off disables it for every client.
- `shiftCraftLimit` (default `ONE_STACK`) — how much one shift-click on the crafting result produces. `ONE_STACK` stops at a full stack of the result, the same boundary AE2 uses, so a recipe yielding 3 at a time stops at 63 rather than overshooting. `ALL_AVAILABLE` keeps crafting until the ingredients run out. Applies to both craft directions; a plain click is always a single craft and is never limited.

### Content Toggles

The `content` category of the same file has one switch per block and item, all on by default: `inventoryConnector`, `storageTerminal`, `craftingTerminal`, `trim`, `inventoryCable`, `inventoryHopper`, `wirelessTerminal`.

Turning one off skips its block or item, its tile entity, and its own recipe. Recipes that merely *use* it stay craftable: the slot either falls back to a substitute — the Crafting Terminal takes a chest in place of a disabled Storage Terminal — or is stripped out, as the Basic Inventory Hopper's cable slot is. A recipe is only dropped when its result is disabled, or when stripping would leave it with no ingredients at all. A pack can trim the mod down this way without a recipe tweaker.

`registerRecipes` (default true) is the blunt version: turn it off and every block and item stays in the game with no recipe at all, for packs that supply their own through a tweaker.

These are read while registering, so a change needs a restart, and blocks already placed in a world will vanish when their entry is switched off.

## Quick Start

1. Place an Inventory Connector against the inventories you want to combine. Use Inventory Trim to extend the local connected path.
2. Place a Storage Terminal or Crafting Terminal against the Inventory Connector to access the complete network.
3. For automation, run Inventory Cable from the connector to one or more Basic Inventory Hoppers.
4. Orient each hopper so its narrow nozzle points toward the intended output. Attach the cable to the opposite end for network export, or to the nozzle for network import.
5. Set an item filter before exporting from the storage network.

## Building

Run the repository Gradle wrapper:

```shell
./gradlew build
```

The reobfuscated release jar is generated under `build/libs`.

## Credits and License

This project is based on the concepts and assets of [Tom's Simple Storage](https://github.com/tom5454/Toms-Storage).

This project is licensed under the [GPL-3.0 License](LICENSE).
