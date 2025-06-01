# SweetAfdian

基于赞助和电铺的爱发电充值/购买系统。

<details>
    <summary>免责声明</summary>
在使用本项目的任何部分（包括但不限于源代码、二进制文件等等）时，应当遵守<a href="https://afdian.com/term" target="_blank">爱发电使用条款</a>。

正如生产菜刀不是为了伤害他人一样，你（使用者）应当独自承担使用本项目产生的一切责任。
</details>

## 简介

使用两种工作模式，实现通过爱发电购买虚拟商品的功能。

推荐使用的是使用 Webhook 接收爱发电订单信息，在[开发者](https://afdian.com/dashboard/dev)页面配置好 Webhook 地址后，爱发电将会把订单发送到那个地址，插件接收后作出反应。缺点是需要额外开放一个端口，实在不行可以上 Cloudflare Tunnel 之类的进行内网穿透。

除此之外，本插件还有轮询API模式。插件会把自己处理过的每一笔订单的订单号及其备份信息存到数据库，定时器会定期向爱发电接口查询订单列表，并寻找没有处理过的订单，作出相应的操作。

## 用法

爱发电上的用户进行发电时，在“留言”那一栏里输入玩家名，只要提交时服务器有这个离线玩家，就可以触发相关操作。

特殊地，你也可以创建电铺商品，在插件配置文件中设定收到某个商品的某个型号的订单时，执行相关操作。

个人觉得电铺商品比普通发电要更好用，你可以设置“留言”栏显示自定义文字，以更好地引导玩家输入玩家名。

简单来说，你只要在爱发电上面配置好商品，在插件配置文件配置好执行的操作就可以了。玩家需要做的，只有下单时在“留言”那一栏填写自己的玩家名。

至于配置的方法以及注意事项，已经全部写到配置文件注释了，尽情享用吧！

## 开发者

[![jitpack](https://jitpack.io/v/MrXiaoM/SweetAfdian.svg)](https://jitpack.io/#MrXiaoM/SweetAfdian)

```kotlin
repositories {
    maven("https://jitpack.io/")
}
dependencies {
    compileOnly("com.github.MrXiaoM:SweetAfdian:$VERSION")
}
```

在收到订单时，插件会广播事件 `top.mrxiaom.sweet.afdian.events.ReceiveOrderEvent`。  
你可以通过这个事件获取订单信息，也可以取消这个事件避免执行 `config.yml` 中设定的命令。
