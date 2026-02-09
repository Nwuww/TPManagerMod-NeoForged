# 进行一个NeoForge开发的学🤤
> 一个适用于**Minecraft1.21+**的**NeoForge**服务器传送mod
>  ~~***终于写完辣哈哈哈***~~
# 状态：Modrinth审核中..
---
## 功能列表
- tpa
  - `/tpa <target>` 向target发送传送请求
    - `/tpaccept` 接受传送请求
    - `/tpdeny` 拒绝传送请求
  - `/tpa config <autoAccept|timeCancel>` 配置传送请求
    - autoAccept: 是否自动接受传送请求
    - timeCancel: 传送请求的过期时间(秒)
- home
  - `/sethome` 设置当前位置为家
  - `/tphome` 传送到家
    - `/tphome <target>` 传送到target的家(需要获取target的访问权限)
  - `/home visit <target> [message]` 请求target的访问权限
  - `/home visit view` 查看访问权限请求
  - `/home accept <target>` 接受target的访问权限请求
  - `/home deny <target>` 拒绝target的访问权限请求
  - `/home remove <target>` 移除target的访问权限
  - `/home clear` 清除所有访问权限
  - `/home` 查看家相关信息(以及访问权限列表)
  - `/home config <permissionLevel>` 配置访问权限等级
    - permissionLevel: 访问权限等级
      - DEFAULT: 仅权限列表中的玩家可以访问
      - PUBLIC: 所有玩家都可以访问
      - PRIVATE: 仅自己可以访问
