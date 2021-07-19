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

成功完成的标志根据 `spec.completions`策略

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

### node

node controller负责

* 维护node状态
* 与cloud provider同步node
* 给node分配容器cidr
* 删除带有 `NoExecute`taint的node上的pods

默认情况下，kubelet在启动时会向master注册自己，并创建node资源

node包括以下状态信息

* 地址：hostname、外网ip和内网ip
* 条件condition：OutOfDisk、Ready、MemoryPressure和DiskPressure
* 容量capacity：node上可用资源，包括CPU、内容和pod总数
* 基本信息：内核版本、容器引擎版本等

#### kube-scheduler

监听kube-apiserver，查询为分配node的pod，根据调度策略为node分配节点

[kube-scheduler](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/components/scheduler.md)

### namespace

虚拟隔离。node，pv，namespace等资源不属于任何namespace

```
kubectl get namespaces
kubectl create namespace <name>
kubectl delete namespace <name>
//删除一个namespace会删除所有该namespace的资源
//event是否属于该namespace取决于产生event的对象
```

### Resource Quota

限制用户资源用量

* 资源配额应用在namespace上，每个namespace最多只有一个 `ResourceQuota`
* 开启计算资源配额后，创建容器时必须配置计算资源请求或限制
* 用户超额后禁止创建新的资源

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/quota.md#%E5%BC%80%E5%90%AF%E8%B5%84%E6%BA%90%E9%85%8D%E9%A2%9D%E5%8A%9F%E8%83%BD)

### deployment

为pod和rs提供了一个声明式定义方法

* 定义deployment创建pod和rs
* 滚动升级和回滚应用
* 扩容和缩容
* 暂停和继续deployment

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/deployment.md#api-%E7%89%88%E6%9C%AC%E5%AF%B9%E7%85%A7%E8%A1%A8)

deployment更新只会发生在 `.spec.template`中label或者镜像更改时触发

[回退](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/deployment.md#%E5%9B%9E%E9%80%80-deployment)

[spec](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/deployment.md#%E7%BC%96%E5%86%99-deployment-spec)

### pod

特征

* 包含多个共享IPC（Inter-Process Communication）和network namespace的容器，可直接通过localhost通信
* 所有Pod可访问共享的Volume，访问共享数据
* 无容错性：pod一旦被调度后就跟node绑定，即使node挂掉也不会被重新调度（而是被自动删除），因此推荐用deploy/daemonset等控制器容错
* 优雅终止：pod删除的时候先给其内的进程发送SIGTERM，等待一段时间（grace period）后才强制停止
* 特权容器（通过SecurityContext配置）具有改变系统配置的权限

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/pod.md#pod-%E5%AE%9A%E4%B9%89)

生命周期

* pending：API Server已经创建了该pod，但一个或多个容器还没被创建
* running：pod所有容器都已经被创建且已经调度到Node上，但至少一个容器还在运行或者正在启动
* succeeded：pod调度到node上后均成功运行结束，且不会重启
* failed：所有容器都被终止，但至少有一个退出失败（退出码不为0或被系统终止）
* unknown：通常是apiserver无法与kubelet通信导致

#### PodPreset

给指定标签的pod注入额外信息，使pod模版不需要为每个pod显式设置重复信息

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/podpreset.md#podpreset-%E7%A4%BA%E4%BE%8B)

### ReplicaSet

取代RC（确保容器应用副本📖始终符合预期，确保pod数量/弹性伸缩/滚动升级以及应用多版本发布跟踪），支持集合式selector

建议使用deployment管理RS

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/replicaset.md#replicaset-%E7%A4%BA%E4%BE%8B)

### StatefulSet

应用场景

* 稳定的持久化存储。基于PVC来实现
* 稳定网络标志。pod重新调度后podname和hostname不变，基于headless service（没有cluster IP的service）实现
* 有序部署，有序扩展。pod是有顺序的，在部署或者扩展的时候要依据定义的顺序依次依序进行（在下一个pod运行前所有pod必须都是running和ready状态），基于init containers实现
* 有序收缩，有序删除

SS组成部分

* 定义DNS domain的headless service
* 创建PV的volumeClaimTemplate
* 定义具体应用的SS

SS每个pod的DNS格式为 `statefulSetName-{0..N-1}.serviceName.namespace.svc.cluster.local`

* `serviceName`为headless service的名字
* `.cluster.local`为cluster domain

[示例](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/statefulset.md#%E7%AE%80%E5%8D%95%E7%A4%BA%E4%BE%8B)

SS可通过 `spec.updateStrategy`设置自动更新策略，目前支持两种策略

* OnDelete：当 `.spec.template`更新时，并不立即删除旧的pod，而是等待用户手动删除这些旧pod后自动创建新pod。默认值
* RollingUpdate：当 `.spec.template`更新时，自动删除旧的pod并创建新pod替换。在更新时，这些pod时按逆序方式进行，依次删除/创建并等待pod变成ready状态才进行下个pod的更新

设置 `.spec.updateStrategy.rollingUpdate.partition`后，之后序号大于等于partition的pod会滚动更新

pod管理策略

* OrderedReady：默认策略，按照pod次序依次创建每个pod并等待ready后才创建后面pod
* [Parrallel](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/statefulset.md#parallel-%E7%A4%BA%E4%BE%8B)：并行创建或删除pod

注意事项

* 所有pod的volume必须使用PV或者管理用事先创建好
* 删除SS时不会删除Volume
* SS需要一个headless service定义DNS domain，需要在SS前创建好

### service

kubernetes的负载均衡

* service：cluster内部的负载均衡，借助cloud provider提供的LB提供外部访问
* ingress controller：用service提供cluster内部的负载均衡，通过自定义的ingress controller提供外部访问
* service load balance：把load balancer跑在容器中，实现bare metal的service load balancer
* custom load balancer：自定义负载均衡，替代kube-proxy，一般在物理部署kubernetes时使用，方便接入公司已有的外部服务

分类

* ClusterIP：默认类型，自动分配一个仅cluster内部可以访问的虚拟IP
* NodePort：在ClusterIP的基础上为每台机器上绑定一个端口，这样可以通过 `<NodeIP>:NodePort`访问服务。
  * 如果kube-proxy设置了 `--nodeport-addresses=10.240.0.0/16`，那么仅该NodePort仅对设置在范围内的IP有效
* LoadBalancer：在NodePort基础上，借助cloud provider创建一个外部的负载均衡器，将请求转发到 `<NodeIP>:NodePort`
* ExternalName：将服务通过DNS CNAME记录方式转发到指定域名（`spec.externalName`设置）

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/service.md#service-%E5%AE%9A%E4%B9%89)

service/endpoints/pod支持tcp/udp/sctp

### service account

* 每个pod在创建后都会自动设置 `spec.serviceAccount`为default（除非设置了其他ServiceAccount）
* 验证pod引用的service account已经存在，否则拒绝创建
* 如果pod没有指定ImagePullSecrets，则把service account的ImagePullSecrets加到pod中
* 每个container启动后都会挂载该service account的token到 `ca.crt`到 `/var/run/secrets/kubernetes.io/serviceaccount`

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/serviceaccount.md#%E5%88%9B%E5%BB%BA-service-account)

### configmap

实现应用和配置分离，避免因为修改配置而重新构建镜像

可以保存单个属性或配置文件

[创建configmap](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/configmap.md#configmap-%E5%88%9B%E5%BB%BA)

[yaml使用configmap](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/configmap.md#configmap-%E4%BD%BF%E7%94%A8)

大量的ConfigMap和Secret会使大量的watch事件急剧增加kube-apiserver的负载，并会导致错误配置过快传播到整个集群

yaml中设置 `immutable: true`

* 保护应用，使之免受意外更新带来的负面影响
* kubenetes会关闭不可变ConfigMap的监视操作

### Secret

通过Volume或环境变量使用

类型

* Opaque：base64编码，存储密码/密钥等，加密性很弱
* `kubernetes.io/dockerconfigjson`：存储私有docker registry的认证信息
* `kubernetes.io/service-account-token`：用于被serviceaccount饮用。pod如果使用了service account，对应的secret会自动挂在到 `/run/secrets/kubernetes.io/serviceaccount`目录中

[使用和yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/secret.md#opaque-secret-%E7%9A%84%E4%BD%BF%E7%94%A8)

### Volume

Kubernetes Volume生命周期与pod绑定。pod删除时，volume才会被清理，只有PV数据不会丢失

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/volume.md#emptydir)

### PersistentVolume

提供网络存储资源

Volume生命周期

* provisioning。PV创建，可直接创建（静态方式），也可以使用StorageClass动态创建
  * available状态
* Binding，将PV分配给PVC
  * bound状态
* Using，pod通过PVC使用该Volume，可以组织删除正在使用的PVC
* releasing，pod释放volume并删除PVC
  * released状态
* reclaiming，回收PV，保留PV以便下次使用，或直接从云存储删除
* deleting，删除pv
  * failed状态

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/persistent-volume.md#pv)

### LocalVolume

[local volume](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/local-volume.md)

代表本地存储设备，如磁盘/分区或目录

主要场景包括分布式存储和数据库等

不支持动态创建

### CRD

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/customresourcedefinition.md#crd-%E7%A4%BA%E4%BE%8B)

finalizer用于实现控制器的异步预删除。finalizer只当后，客户端删除对象只会设置 `metadata.deletionTimestamp`而不是直接操作

会触发正在监听CRD的控制器，控制器执行一些删除前的清理操作，从列表中删除自己的finalizer，然后再重新发起一个删除操作

yaml `validatoin`可以提前验证用户提交的资源是否符合规范

[validation](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/customresourcedefinition.md#validation)

[subresources](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/customresourcedefinition.md#subresources)支持/status和/scale两个子资源

[categories](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/customresourcedefinition.md#categories)给CRD分组

### DaemonSet

在每个容器上运行一个容器副本，用来部署一些集群日志、监控或者其他系统管理应用

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/daemonset.md#api-%E7%89%88%E6%9C%AC%E5%AF%B9%E7%85%A7%E8%A1%A8)

通过 `.spec.updateStrategy.type`设置更新策略

* OnDelete：默认，更新模板后，只有手动删除旧的pod才会创建新pod
* RollingUpdate：自动删除旧的并创建新的
  * `.spec.updateStrategy.rollingUpdate.maxUnavailable`默认1
  * `spec.minReadySeconds`默认0

[回滚](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/daemonset.md#%E5%9B%9E%E6%BB%9A)

### Job

批量处理短暂的一次性任务

* 非并行job：创建一个pod直到其成功结束
* 固定结束次数的job：设置 `.spec.completions`，创建多个pod，直到 `.spec.completions`个pod成功结束
* 带有工作队列的并行job：设置 `.spec.Parallelism`，当所有pod结束并至少一个成功，job被认为成功

job controller负责根据job spec创建pod，持续监控pod状态，直到结束。如果失败，根据 `restartPolicy`决定是否创建新的pod再次重试任务（只支持 `OnFailure`和 `Never`）

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/job.md#job-spec-%E6%A0%BC%E5%BC%8F)

* `spec.template`同pod
* `RestartPolicy`只支持 `Never`和 `OnFailure`
* `spec.activeDeadlineSeconds`标志失败pod的重试最大时间，超过这个时间不会继续重试

[Indexed Job](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/job.md#indexed-job)

TTL控制器用来自动清理已经结束的pod（Complete和Failed）`.spec.ttlSecondsAfterFinished`

`.sepc.suspend`暂停和重启job

### CronJob

定时任务，在指定时间周期运行指定的任务

[spec](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/cronjob.md#cronjob-spec)

### SecurityContext

[security context](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/security-context.md)

### NetworkPolicy

[NetworkPolicy](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/network-policy.md)

### Ingress

通常，service和pod的IP仅可在集群内部访问。集群外部请求需要通过负载均衡转发到service和Node上暴露的NodePort，然后再由kube-proxy通过edge router将其转发给相关的pod或者丢弃

ingress是为进入集群的请求提供路由规则的集合

#### ingress controller

为配置Ingress规则，需要部署一个[ingress controller](https://github.com/feiskyer/kubernetes-handbook/tree/master/extension/ingress)

[yaml](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/ingress.md#ingress-%E6%A0%BC%E5%BC%8F)

更新ingress `kubectl edit ing <ing_name>`

# todo

组件通讯

heapster

[HPA最佳实践](https://github.com/feiskyer/kubernetes-handbook/blob/master/concepts/objects/autoscaling.md)

[cluster autoscaler部署](https://github.com/feiskyer/kubernetes-handbook/blob/master/setup/addon-list/cluster-autoscaler.md#%E9%83%A8%E7%BD%B2)

ingress controller
