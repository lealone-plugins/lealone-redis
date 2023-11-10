# lealone-redis

兼容 redis 协议和 api 的插件，redis 最高版本支持 7.2.3


## 编译需要

* jdk 1.8+
* maven 3.8+


## 打包插件

运行 `mvn clean package -Dmaven.test.skip=true`

生成 jar 包 `target\lealone-redis-plugin-6.0.0.jar`

假设 jar 包的绝对路径是 `E:\lealone\lealone-plugins\redis\target\lealone-redis-plugin-6.0.0.jar`


## 运行插件

先参考[ lealone 快速入门](https://github.com/lealone/Lealone-Docs/blob/master/应用文档/Lealone数据库快速入门.md) 启动 lealone 数据库并打开一个命令行客户端

然后执行以下命令创建并启动插件：

```sql
create plugin redis
  implement by 'org.lealone.plugins.redis.server.RedisServerEngine' 
  class path 'E:\lealone\lealone-plugins\redis\target\lealone-redis-plugin-6.0.0.jar' --最好使用绝对路径
  parameters (port=6379); --端口号默认就是6379，如果被其他进程占用了可以改成别的
 
start plugin redis;
```

要 stop 和 drop 插件可以执行以下命令：

```sql
stop plugin redis;

drop plugin redis;
```

执行 stop plugin 只是把插件对应的服务停掉，可以再次通过执行 start plugin 启动插件

执行 drop plugin 会把插件占用的内存资源都释放掉，需要再次执行 create plugin 才能重新启动插件


## 使用 redis 的客户端访问 lealone

可以使用 `redis-cli -p 6379` 通过命令行的方式访问 lealone

也可以使用 jedis 通过编写客户端代码的方式访问 lealone

用 redis 各种客户端访问 lealone 的用法都跟正常访问 redis 一样

