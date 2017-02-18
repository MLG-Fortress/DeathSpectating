# DeathSpectating
When a player dies, they will "watch" ("spectate") their killer the moment they die for a brief period of time before respawning. This is similar to most FPS games.

Video: https://www.youtube.com/watch?v=NLXQV4Ff1jY

[Documentation](https://github.com/MLG-Fortress/DeathSpectating/wiki)

Players in death spectating mode will not be able to move or do anything other than spectate at their death location (duh), chat, and use a set of commands that you allow (such as /me, /msg, etc.)

Unlike some other plugins, this one attempts to **maintain compatiblity with existing plugins,** particularly those that handle death and respawning (PlayerDeathEvent and PlayerRespawnEvent). There are some minor [caveats](https://github.com/MLG-Fortress/DeathSpectating/wiki/Caveats) due to manually performing this sort of compatibility, primarily because the player does not "actually" die or respawn. But the difference in experience is far better when a player does not "actually" die. More info here on that (and a small story of how this plugin came to be) if you're interested: //TODO: add sway.com presentation here

### Addons
DeathSpectating supports addons, such as sending a Title message to the death spectating player along with the time remaining until respawn. Here's a [list of publicly available DeathSpectating addons!](https://github.com/MLG-Fortress/DeathSpectating/wiki/Addons)