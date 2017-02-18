# DeathSpectating
When a player dies, they will "watch" ("spectate") their killer the moment they die for a brief period of time before respawning. This is similar to most FPS games.

[Permissions](https://github.com/MLG-Fortress/DeathSpectating/blob/master/src/main/resources/plugin.yml) | [Configuration](https://github.com/MLG-Fortress/DeathSpectating/wiki/Configuration)

Unlike some other plugins, this one attempts to **maintain compatiblity with existing plugins,** particularly those that handle death and respawning (PlayerDeathEvent and PlayerRespawnEvent).

There are some minor [caveats](https://github.com/MLG-Fortress/DeathSpectating/wiki/Caveats) due to performing this sort of compatibility, primarily because the player does not actually die or respawn in the vanilla/Craftbukkit code. But the difference in experience is far better when a player does not "actually" die. More info here on that if you're interested: //TODO: add sway.com presentation here
