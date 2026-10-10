# QuickStack & ChestDrop

Clientside Fabric mod for Minecraft Java Edition 1.21.11. It registers two configurable inventory keybinds:

- **G** — throw every matching stack from the open container onto the ground.
- **H** — move matching stacks from the open container into the player inventory.

The hovered container stack selects the item. Matching compares the item and all data components, including enchantments and custom component values. An empty cursor target or a hovered player-inventory slot does nothing. The source scan covers every non-player slot in the open screen handler, including both halves of a double chest.

## Multiplayer behavior and limits

This mod does not edit inventory contents locally. Throw uses vanilla `THROW` slot clicks; collection uses vanilla `QUICK_MOVE` clicks, so the server remains authoritative and decides what is accepted. The mod sends one operation at a time, with a tick between clicks. Server plugins may reject or alter these actions. Collection follows vanilla shift-click transfer rules and leaves items that the server cannot move in the container. Feedback is computed from the client-visible slot state after the click sequence, so it can lag or be affected by server/plugin behavior.

The client can only operate on slots exposed by the current screen handler. Compatibility with standard chest, double chest, barrel, hopper, and other conventional container screens follows their vanilla slot layout and shift-click rules. Custom modded screens that implement unusual slot behavior may not behave the same way. No server-side mod is required, but the server must accept normal vanilla slot actions.

## Install

1. Install Fabric Loader for Minecraft 1.21.11 and Fabric API 0.141.6+1.21.11.
2. Put the built `quickstack-chestdrop-1.0.0.jar` into the instance's `mods` folder.
3. Open **Options → Controls → Key Binds** and change either QuickStack binding as needed.

The mod is client-only. Java 21 is required for Minecraft 1.21.11.

## Build

With JDK 21 and Gradle 9.2.1 or newer installed, run `gradle build` in this directory. The distributable jar will be in `build/libs/`.

## Verification scenarios

The project intentionally uses standard handled-screen slots and server-side slot actions, so the functional checks should be performed in a 1.21.11 client with a local world and a multiplayer server:

1. Put several stacks of the selected item in a chest; press G and confirm only matching stacks are dropped.
2. Put another wood type alongside it; confirm it remains untouched.
3. Repeat with a double chest and verify both halves are scanned.
4. Collect with partial inventory space and confirm existing stacks fill before open slots.
5. Collect with full inventory and confirm the source items remain and feedback reports the remainder.
6. Use enchanted or component-customized stacks and confirm only identical item+component stacks match.
7. Press either key with no hovered stack; confirm no changes.
8. Repeat in singleplayer and on a multiplayer server that accepts normal clicks.
9. Rebind both keys in Controls and confirm the new keys trigger the actions.

A live Minecraft session/server was not available in this workspace, so those gameplay checks cannot be claimed as run here.

