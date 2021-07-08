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

[reference](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/components/README.md)

![核心组件](https://github.com/feiskyer/kubernetes-handbook/blob/master/.gitbook/assets/components%20%2811%29.png)

* etcd键值对数据库，保存整个集群的状态
* API Server提供资源操作的唯一入口，提供认证、授权、访问控制、API注册和发现等机制
* controller manager负责维护集群状态，如故障检测、自动扩展、滚动更新
* scheduler负责资源调度，按预定调度策略将pod调度到对应机器
* kublet维护容器声明周期，负责Volume和网络的管理
* container runtime负责镜像管理以及pod和容器的运行
* kube-proxy负责为service和cluster内部的服务发现和负载均衡

### 组件通讯

* 只有api server能直接操作etcd

略

## 资源对象

### autoscaling

HPA根据CPU使用或自定义metrics自动扩展pod数量（rc、deployment和rs）

* 控制管理器每30s查询metrics的资源使用情况（`--horizontal-pod-autoscaler-sync-period`）
* 三种metrics
  * 预定义的metrics以利用率方式计算
  * 自定义的pod metrics以原始值方式计算
  * 自定义的object metrics
* 两种metrics查询方式：heapster和自定义REST API
* 支持多metrics

[heapster](https://www.kubernetes.org.cn/932.html)

### cluster autoscaler

当集群容量不足时，去cloud provider常见新的node

在node长时间（超过10min）资源利用率很低（50%）自动将其删除，删除时会有1min的graceful termination time

删除后，原来的pod会自动调度到其他node上（通过Deployment、SS等控制器）

每10s定期检测是否由重组资源来调度新创建的pod，不足将创建新node

### namespace

虚拟隔离。node，pv，namespace等资源不属于任何namespace

```
kubectl get namespaces
kubectl create namespace <name>
kubectl delete namespace <name>
//删除一个namespace会删除所有该namespace的资源
//event是否属于该namespace取决于产生event的对象
```

### deployment

为pod和rs提供了一个声明式定义方法

* 定义deployment创建pod和rs
* 滚动升级和回滚应用
* 扩容和缩容
* 暂停和继续deployment

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/deployment.md#api-%E7%89%88%E6%9C%AC%E5%AF%B9%E7%85%A7%E8%A1%A8)

deployment更新只会发生在`.spec.template`中label或者镜像更改时触发

[回退](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/deployment.md#%E5%9B%9E%E9%80%80-deployment)

[spec](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/deployment.md#%E7%BC%96%E5%86%99-deployment-spec)

### configmap

实现应用和配置分离，避免因为修改配置而重新构建镜像

可以保存单个属性或配置文件

[创建configmap](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/configmap.md#configmap-%E5%88%9B%E5%BB%BA)

[yaml使用configmap](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/configmap.md#configmap-%E4%BD%BF%E7%94%A8)

大量的ConfigMap和Secret会使大量的watch事件急剧增加kube-apiserver的负载，并会导致错误配置过快传播到整个集群

yaml中设置`immutable: true`

* 保护应用，使之免受意外更新带来的负面影响
* kubenetes会关闭不可变ConfigMap的监视操作

### LocalVolume

[local volume](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/local-volume.md)

### CRD

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/customresourcedefinition.md#crd-%E7%A4%BA%E4%BE%8B)

finalizer用于实现控制器的异步预删除。finalizer只当后，客户端删除对象只会设置`metadata.deletionTimestamp`而不是直接操作

会触发正在监听CRD的控制器，控制器执行一些删除前的清理操作，从列表中删除自己的finalizer，然后再重新发起一个删除操作

yaml`validatoin`可以提前验证用户提交的资源是否符合规范

[validation](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/customresourcedefinition.md#validation)

[subresources](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/customresourcedefinition.md#subresources)支持/status和/scale两个子资源

[categories](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/customresourcedefinition.md#categories)给CRD分组

### DaemonSet

在每个容器上运行一个容器副本，用来部署一些集群日志、监控或者其他系统管理应用

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/daemonset.md#api-%E7%89%88%E6%9C%AC%E5%AF%B9%E7%85%A7%E8%A1%A8)

通过`.spec.updateStrategy.type`设置更新策略

* OnDelete：默认，更新模板后，只有手动删除旧的pod才会创建新pod
* RollingUpdate：自动删除旧的并创建新的
  * `.spec.updateStrategy.rollingUpdate.maxUnavailable`默认1
  * `spec.minReadySeconds`默认0

[回滚](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/daemonset.md#%E5%9B%9E%E6%BB%9A)

### Job

批量处理短暂的一次性任务

* 非并行job：创建一个pod直到其成功结束
* 固定结束次数的job：设置`.spec.completions`，创建多个pod，直到`.spec.completions`个pod成功结束
* 带有工作队列的并行job：设置`.spec.Parallelism`，当所有pod结束并至少一个成功，job被认为成功

job controller负责根据job spec创建pod，持续监控pod状态，直到结束。如果失败，根据`restartPolicy`决定是否创建新的pod再次重试任务（只支持`OnFailure`和`Never`）

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/job.md#job-spec-%E6%A0%BC%E5%BC%8F)

* `spec.template`同pod
* `RestartPolicy`只支持`Never`和`OnFailure`
* `spec.activeDeadlineSeconds`标志失败pod的重试最大时间，超过这个时间不会继续重试

[Indexed Job](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/job.md#indexed-job)

TTL控制器用来自动清理已经结束的pod（Complete和Failed）`.spec.ttlSecondsAfterFinished`

`.sepc.suspend`暂停和重启job

### CronJob

定时任务，在指定时间周期运行指定的任务

[spec](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/cronjob.md#cronjob-spec)

### Ingress

通常，service和pod的IP仅可在集群内部访问。集群外部请求需要通过负载均衡转发到service和Node上暴露的NodePort，然后再由kube-proxy通过edge router将其转发给相关的pod或者丢弃

ingress是为进入集群的请求提供路由规则的集合

#### ingress controller

为配置Ingress规则，需要部署一个[ingress controller](https://github.com/feiskyer/kubernetes-handbook/tree/master/extension/ingress)

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/ingress.md#ingress-%E6%A0%BC%E5%BC%8F)

更新ingress`kubectl edit ing <ing_name>`

# todo

[sample controller](https://github.com/kubernetes/sample-controller)管理CRD

组件通讯

heapster

[HPA最佳实践](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/autoscaling.md)

[cluster autoscaler部署](https://github.com/feiskyer/kubernetes-handbook/blob/master/setup/addon-list/cluster-autoscaler.md#%E9%83%A8%E7%BD%B2)

ingress controller
