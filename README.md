# [Development Stopped] Water Physics Overhaul (1.16, 1.18 coming soon)

I am stopping development on this mod in favor of https://www.curseforge.com/minecraft/mc-mods/flowing-fluids by Traben.


https://www.curseforge.com/minecraft/mc-mods/realistic-fluid-flows

-- video/images coming soon --

## Concept
This mod completely overhauls the vanilla fluid dynamics in minecraft to add more realistic fluid dynamics, which include:
- fluids in world now exist as packets (partial blocks): 1 block/bucket = 8 fluid packets (125 mb each)
  - rendering/picking up/placing/waterlogging/... => works with single packets
  - vanilla bucket was tweaked to be able to pickup/hold/place 0-8 packets (bucket slurp distance configurable)
- mass conservation: fluid packets (e.g. water) cannot be duplicated or (easily) destroyed
- fluid packets seek lower ground (horizontal travel distance configurable)
- fluid equalization: bodies of fluid equalize (distance configurable) and try to have same fluid level (in packets)
- full blocks displace fluids: falling sand/placing block/pushing piston "pushes" fluid packets out of sides or top without destroying them
- fluidlogging: similar to waterlogging, but with any fluid and any fluid level; also adding fluidlogging to many more non-full blocks
- _(future: add rain mechanics: replenish water bodies, collect water for use)_

## Bugs
TL;DR: There are still many serious bugs in the latest version, which can destroy/create fluids, destroy blocks, behave erroneously and create lots of lag.

Note that many things are still not working (in all versions), including, but not limited to:
- fluid-fluid interactions: lava and water interaction is not correct (not fixed for packets)
- worlgen & saving/loading: bodies of water settle (e.g. water plants do not spawn as fluidlogged) or even drain after worldgen/save-load
- lag: equalization for large bodies of water can become quite laggy
- mod compat (other fluids, pumps etc.): not tackled yet


<a href="https://www.buymeacoffee.com/felicis" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;" ></a>
