# TPManager

<!-- README-I18N:START -->
**English** | [汉语](./README.zh.md)
<!-- README-I18N:END -->

# [Downlolad Here (Modrinth)](https://modrinth.com/mod/tp-manager)
---
## HOW TO USE
- TPA System (Teleport Request)
  - `/tpa <target>`: Sends a teleport request to the target player.
    - `/tpaccept`: Accepts the incoming teleport request.
    - `/tpdeny`: Denies the incoming teleport request.
  - `/tpa config <autoAccept|timeCancel>`: Configures TPA settings.
    - `autoAccept`: Toggles whether to automatically accept incoming teleport requests.
    - `timeCancel`: Sets the expiration time (in seconds) for teleport requests.
- Home & Visit System
  - `/sethome`: Sets your current location as your home.
  - `/tphome`: Teleports you to your own home.
    - `/tphome <target>`: Teleports you to the target player's home (requires visit permission).
  - `/home visit <target> [message]`: Requests permission to visit the target player's home with an optional message.
  - `/home visit view`: Displays all pending visit requests you have received.
  - `/home accept <target>`: Grants the target player permission to visit your home.
  - `/home deny <target>`: Rejects the target player's visit request.
  - `/home remove <target>`: Revokes the target player's visit permission.
  - `/home clear`: Removes all players from your visit permission list.
  - `/home`: Shows information about your home, including your current permission list.
  - `/home config <permissionLevel> <value>`: Sets the global access level for your home.
    - `DEFAULT`: Only players in your permission list can visit.
    - `PUBLIC`: Anyone can visit your home without a request.
    - `PRIVATE`: No one can visit your home (ignores the permission list).