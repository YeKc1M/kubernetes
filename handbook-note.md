[handbook](https://github.com/feiskyer/kubernetes-handbook/blob/master/SUMMARY.md)

# 核心原理

## 架构原理

参考[learning.md](./learning.md)

## 设计理念

### API设计理念

1. 所有API应该是声明式的。
   1. 命令式关注计算机完成的每一步，声明式关注计算机要完成什么而非怎么做
2. API对象彼此互补且可组合
   1. 尽量实现面向对象设计的哟啊求
3. 高层API以操作意图为设计基础
   1. 从业务出发设计API
4. 底层API根据高层API的控制需要设计
   1. 3和4主要说要尽量抵抗受技术实现影响的诱惑
5. 尽量避免简单封装，不要有在外部API无法显式知道的内部隐藏机制
6. API操作复杂性与对象数量成正比
7. API对象状态不能依赖于网络连接状态
   1. API对象能应对网络不稳定性
8. 尽量避免让操作机制依赖于全局状态
   1. 分布式系统保证全局状态的同步非常困难

### 控制机制设计原则

略

### 架构设计原则

* 只有apiserver可以直接访问etcd存储，其他服务必须通过Kubernetes API来访问集群状态
* 单节点故障不应该影响集群的状态
* 在没有新请求的情况下，所有组件应该在故障恢复后继续执行上次最后收到的请求（比如网络分区或服务重启等）
* 所有组件都应该在内存中保持所需要的状态，apiserver将状态写入etcd存储，而其他组件则通过apiserver更新并监听所有的变化
* 优先使用事件监听而不是轮询

### 核心技术概念和API对象

所有API对象有3大类属性：

* 元属性metadata
  * 标识API对象，每个对象至少有3个元数据：namespace、name和uid
  * 还可以用各种labels标识和匹配不同对象
* 规范spec
  * 描述了理想状态
  * 所有配置都是通过spec去设置（声明式）
* 状态status
  * 描述实际状态

#### Pod

集群中运行部署应用或服务的最小单元

支持多个容器，多个容器在一个pod中共享网络地址和文件系统，可以通过进程间通信和文件共享完成服务

K8S业务分类：

* 长期伺服型long-running——Deployment
* 批处理batch——Job
* 节点后台支撑node-daemon——DaemonSet
* 有状态应用stateful application——StatefulSet

#### RC

RC早期技术概念，只适用于长期伺服型的业务类型，如今几乎被RS替代

#### RS

支持更多种类的匹配模式，作为Deployment的理想状态参数使用

参考[learning.md](./learning.md##Pod)

#### Deployment

标识用户对K8S集群的一次更新操作

滚动升级实际是创建一个新的RS，逐渐增加新RS中副本数到理想状态，将旧RS中副本数减小到0。

未来所有长期伺服型的业务管理都会通过Deployment

#### Service

k8s集群中微服务的负载均衡由kube-proxy实现

kube-proxy是一个分布式代理服务器，在k8s每个节点上都有一个

#### Job

批处理业务有始有终，长期伺服型业务在用户不停止的情况下永远运行

成功完成的标志根据`spec.completions`策略

#### DaemonSet

长期伺服型和批处理型服务核心在业务应用，后台支撑型关注点在K8S中的节点，要保证每个而节点上都有一个某类的Pod运行

节点可能是所有集群节点，也可能是通过nodeSelector选定的节点

主要包括存储、日志和监控等服务

#### StatefulSet

有状态服务

SS中每个Pod名字事先确定，不能更改

每个Pod挂载自己独立的存储，如果一个pod出现故障，从其他节点启动一个同样名字的pod，要挂载原来Pod的存储以继续它的状态服务

还处于alpha阶段

#### 集群联邦federation

[federation](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/concepts.md#%E9%9B%86%E7%BE%A4%E8%81%94%E9%82%A6federation)

#### 存储卷Volume

作用范围是一个pod

##### PV和PVC

Persistent Volume, Persistent Volume Claim

PV是资源提供者，pvc是资源使用者，根据业务服务的需求变化而变化

PV是集群中的资源，生命周期与pod相互独立

[reference](https://www.kubernetes.org.cn/4069.html)

声明周期：

* 供应provisioning：PV的创建，分为静态创建（直接创建）和动态创建（StorageClass）
* 绑定binding：将PV分配给PVC
* 使用using：pod通过pvc使用该volume
* 释放releasing：pod释放volume并删除pvc
* 回收reclaiming：回收pv，保留pv以便下次使用，也可以直接从云存储删除

状态：available、bound、released、failed

#### Node

工作主机，物理机或虚拟机

#### Secret

保存和传递密码、密钥、认证凭证等

#### user account & service account

用户账户跨namespace，服务账户对应一个运行中程序的身份呢

#### namespace

提供虚拟的隔离作用

#### rbac

role、rolebinding

## 核心组件
