# Sabotage Kits Guide

This is a living reference for every kit in Sabotage. A **kit** is a loadout a player can
select to play as — a bundle of armor, weapons, and zero or more custom-item abilities. A
**custom item** is a standalone hardcoded class (extending `CustomItem`) that grants an
ability; it is not itself a kit, is not named after any particular kit, and may be reused
across multiple kits (or not used in any kit at all). This guide is filled in incrementally
as each kit's description and loadout are defined, and as the custom items it needs are
implemented.

## Conventions

- **Storage:** Each kit is its own human-editable YAML file under
  [src/main/resources/kits/](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/resources/kits)
  (bundled defaults, shipped inside the plugin jar). On first run, the plugin extracts any
  default kit file that doesn't already exist into `plugins/Sabotage/<game>/kits/<id>.yml` —
  from then on, that extracted copy is what's loaded and is safe to hand-edit without
  recompiling the plugin (editing it won't be overwritten unless you delete the file and let it
  re-extract). See [KitIO.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/fileIO/KitIO.java)
  for the loader/writer and the exact file format (hotbar/armor/offhand/custom-item
  references/enchants/potion effects).
- **Defuse Kit:** Every kit includes a Blaze Powder in hotbar slot `0` (the defuse kit),
  inserted automatically by the loader — never set it in a kit file.
- **Kit ID:** The lowercase `name` field in the kit's YAML file, used with `/kit <id>` (see
  [Kit.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/Kit.java)).
- **Icon:** A single `Material` (the `icon` field) shown in the kit-selection GUI. `Kit.getIcon()`
  builds the full tooltip from the kit's own fields: a bold kit name, cost in green, and the
  kit's description lines in gold — see
  [SabKits.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/groups/SabKits.java).
  It is purely cosmetic and does not have to match any item actually granted by the kit.
- **Armor:** Four pieces (`armor.boots`/`leggings`/`chestplate`/`helmet`), each an item spec
  (see below). Applied via `PlayerInventory.setArmorContents()` on kit load.
- **Offhand:** One optional item spec (`offhand`), applied via `setItemInOffHand()`.
- **Weapons:** Vanilla or lightly-modified (enchanted/unbreakable) items, placed in hotbar
  slots `1`-`8` via an item spec — either a plain Material name (`ARROW`) or a map with
  `material`, optional `amount`, optional `enchants` (map of enchantment name to level), and
  optional `unbreakable: true`.
- **Custom Items:** A kit may grant one or more custom-item abilities by referencing a
  registered custom item's key directly from a hotbar/armor/offhand slot (e.g.
  `{custom: rewind}`), which resolves to that
  [CustomItem](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/customitem/CustomItem.java)'s
  own `getItem()` at load time. Custom items are standalone classes in
  `src/main/java/bubbles/sabotage/plugin/items/`, registered in
  [Main.loadCustomItems()](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/Main.java),
  and are not named after any particular kit — the same custom item can be placed into more
  than one kit's loadout, or a kit can use none at all.
- **Potion Effects:** An optional `potionEffects` list (each with `type`, `amplifier`,
  `duration` in ticks), applied to the player on kit load — see
  [kits/test.yml](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/resources/kits/test.yml)
  for an example exercising every field this format supports.

### CustomItem hook reference

When implementing a new custom item, extend `CustomItem`, build the `ItemStack` in the
constructor and call `setItem(item)`, then override only the hooks the ability needs:

| Hook | Fires when the item... |
|---|---|
| `onRightClickAir(e, mainHand)` | right-clicks with nothing targeted |
| `onRightClickBlock(e, mainHand)` | right-clicks a block |
| `onRightClickPlayer(e, mainHand)` | right-clicks another player |
| `onLeftClickAir(e)` / `onLeftClickBlock(e)` | left-clicks air / a block |
| `onAttack(e, mainHand)` | deals damage to a player (melee or arrow) |
| `onShoot(e, mainHand)` | fires a bow |
| `onShotBlock(e, mainHand)` / `onShotPlayer(e, mainHand)` | a fired projectile hits a block / player |
| `onKill(e, attacker, victim)` | holder kills a player |
| `onDeath(e)` | holder dies |
| `stop()` | game/round resets — clear any per-player state here |

Helper methods available on `CustomItem`: `give(player, count)`, `consume(player, count)`,
`consumeAll(player, item)`, `contains(player, item, count)`, `getGame()`, `getPlugin()`.

### Existing custom item classes (available to assign into any kit's loadout)

These already exist in `src/main/java/bubbles/sabotage/plugin/items/` and are registered in
`Main.loadCustomItems()`. They are standalone abilities — assigning one to a kit below is a
deliberate decision made per-kit, not implied by naming similarity.

| Class | Item it grants | Ability summary |
|---|---|---|
| [HealingWand.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/HealingWand.java) | Blaze Rod | Right-click a teammate to heal them |
| [GrapplingHook.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/GrapplingHook.java) | Bow | Shoot to grapple toward a block or pull a hit player |
| [Sniper.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/Sniper.java) | Bow | Long-range arrow hits (15+ blocks) deal bonus damage |
| [Steak.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/Steak.java) | Cooked Beef | Right-click to consume and heal |
| [Grenade.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/Grenade.java) | Golden Hoe (+ Firework Star ammo) | Throw/lob explosive grenades, with reload mechanic |
| [RewindClock.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/RewindClock.java) | Clock | Right-click to teleport back to your position from ~12s ago |
| [Flare.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/Flare.java) | Redstone Torch | Throw to call down a lootable supply crate |
| [Landmine.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/Landmine.java) | Shears (+ pressure-plate mines) | Place hidden mines that explode when stepped on |
| [Bloodsucker.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/Bloodsucker.java) | Iron Sword | Kills grant redstone (max HP scales with redstone held) |
| [GlassShard.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/GlassShard.java) | Prismarine Shard | Toggle: strength + night vision at the cost of losing hunger-healing |
| [Bullet.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/Bullet.java) | Iron Nugget | Stubbed — ability not yet implemented |
| [KitSelect.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/KitSelect.java) / [TeamSelect.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/TeamSelect.java) / [Trash.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/Trash.java) | — | Menu/utility items, not kit abilities |

---

## Kit List

The table below is the full 29-kit roadmap from the project's naming brainstorm. It is
aspirational — the kits actually implemented today (as files under
[src/main/resources/kits/](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/resources/kits))
are `short`, `rewind`, `jugg`, `trooper`, `long`, `demo`, and the all-fields `test` kit.
Several of those existing ids correspond conceptually to entries below (`short`↔Shortbow,
`long`↔Longbow, `demo`↔Demolition) but were kept under their original ids rather than
renamed, since renaming would break any existing `/kit <id>` usage.

| # | Kit ID | Icon |
|---|---|---|
| 1 | trooper | Iron Sword |
| 2 | longbow | Bow |
| 3 | shortbow | Bow |
| 4 | sniper | Arrow |
| 5 | pyro | Blaze Powder |
| 6 | frost | Ice Block |
| 7 | demolition | Weighted Pressure Plate |
| 8 | explosive | Firework Star (dyed red) |
| 9 | ghost | Ghast Tear |
| 10 | bullet | Iron Nugget |
| 11 | bloodsucker | Redstone Dust |
| 12 | mechanic | Redstone Torch |
| 13 | bacca | Diamond Axe |
| 14 | glass | Prismarine Shard |
| 15 | ninja | Ender Pearl |
| 16 | medic | Golden Apple |
| 17 | teleporter | Monster Spawner |
| 18 | rewind | Clock |
| 19 | vegetarian | Carrot |
| 20 | venom | Potion of Poison |
| 21 | jugg | Diamond Chestplate |
| 22 | warper | Blaze Rod |
| 23 | speed | Sugar |
| 24 | bleeder | Ominous Bottle |
| 25 | dwarf | Bottle o' Enchanting |
| 26 | barracade | Oak Wood Plank |
| 27 | reaper | Iron Hoe |
| 28 | sacraficial | Red Dye |
| 29 | noahcraft | Enchanted Wood Stick |

---

## 1. Trooper ✅
- **Icon:** Iron Sword
- **Description:** ✅ A standard-issue loadout with an iron sword.
- **Armor:** ✅ Iron boots, leggings, chestplate, helmet
- **Weapons:** ✅ Iron sword, 5x golden apples
- **Custom Items:** None

## 2. Longbow ✅
- **Icon:** Bow
- **Description:** ✅ A long-range loadout with a punching, infinite-ammo bow.
- **Armor:** ✅ Chainmail boots, iron leggings, iron chestplate, chainmail helmet
- **Weapons:** ✅ Knockback II stone sword, Infinity I Power II bow, arrow
- **Custom Items:** None

## 3. Shortbow ✅
- **Icon:** Bow
- **Description:** ✅ A nimble melee-and-ranged loadout with a knockback sword.
- **Armor:** ✅ Chainmail boots, iron leggings, iron chestplate, chainmail helmet
- **Weapons:** ✅ Knockback II stone sword, Infinity I Power II bow, arrow
- **Custom Items:** None

## 4. Sniper ✅
- **Icon:** Arrow
- **Description:** ✅ A long-range specialist with a high-powered rifle.
- **Armor:** ✅ Leather boots, leggings, chestplate, helmet
- **Weapons:** ✅ Stone sword, rifle custom item, 3x arrows
- **Custom Items:** ✅ Rifle (distance-based damage: 1-10 blocks = 2 hearts, 20-40 blocks = 4 hearts, 50+ blocks = instant kill)

## 5. Pyro
- **Icon:** Blaze Powder
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 6. Frost
- **Icon:** Ice Block
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 7. Demolition ✅
- **Icon:** Weighted Pressure Plate
- **Description:** ✅ Places hidden landmines using the Landmine Arming Tool.
- **Armor:** ✅ Copper boots, iron leggings, iron chestplate, copper helmet
- **Weapons:** ✅ Knockback II stone sword, landmine custom item, 4x heavy weighted pressure plates
- **Custom Items:** ✅ Landmine

## 8. Explosive
- **Icon:** Firework Star (dyed red)
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 9. Ghost
- **Icon:** Ghast Tear
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 10. Bullet
- **Icon:** Iron Nugget
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 11. Bloodsucker
- **Icon:** Redstone Dust
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 12. Mechanic
- **Icon:** Redstone Torch
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 13. Bacca
- **Icon:** Diamond Axe
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 14. Glass
- **Icon:** Prismarine Shard
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 15. Ninja
- **Icon:** Ender Pearl
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 16. Medic
- **Icon:** Golden Apple
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 17. Teleporter
- **Icon:** Monster Spawner
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 18. Rewind ✅
- **Icon:** Clock
- **Description:** ✅ Carries the Rewind Clock to teleport back to an earlier position.
- **Armor:** ✅ Iron boots, chainmail leggings, iron chestplate, chainmail helmet
- **Weapons:** ✅ Iron sword, rewind clock custom item, spare iron helmet
- **Custom Items:** ✅ Already implemented — [RewindClock.java](/c:/Users/ZAKSGAMINGRIG/Desktop/Sabotage/src/main/java/bubbles/sabotage/plugin/items/RewindClock.java)

## 19. Vegetarian
- **Icon:** Carrot
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 20. Venom
- **Icon:** Potion of Poison
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 21. Jugg ✅
- **Icon:** Diamond Chestplate
- **Description:** ✅ A tanky loadout built around heavy armor.
- **Armor:** ✅ Iron boots, iron leggings, diamond chestplate, iron helmet
- **Weapons:** ✅ Stone sword
- **Potion Effects:** ✅ Slowness II (indefinite duration, no particles)
- **Custom Items:** None

## 22. Warper
- **Icon:** Blaze Rod
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 23. Speed
- **Icon:** Sugar
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 24. Bleeder
- **Icon:** Ominous Bottle
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 25. Dwarf
- **Icon:** Bottle o' Enchanting
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 26. Barracade
- **Icon:** Oak Wood Plank
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 27. Reaper
- **Icon:** Iron Hoe
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 28. Sacraficial
- **Icon:** Red Dye
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_

## 29. Noahcraft
- **Icon:** Enchanted Wood Stick
- **Description:** _TBD_
- **Armor:** _TBD_
- **Weapons:** _TBD_
- **Custom Items:** _TBD_
