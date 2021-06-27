# Tutorial

[kubernetes official document](https://kubernetes.io/docs/tutorials/)

## hello minikube

## basics

### cluster

a kubernetes cluster consists of two type of  resources:

* the *Control Plane* coordinates the cluster
* *Nodes* are the workers that run the applications
  * a node is a VM or a computer that serve as a worker machine in the cluster
  * each node has a *Kubelet*, which is an agent for managing the node and communicating with the Kubernetes control plane

`kubectl get nodes` to see the nodes in the cluster

## deployment & pod

A *deployment* is responsible for creating and updating instances of your application

once the application instances are created, a Kubernetes Deployment Controller continuously monitors those intances.

self-healing mechanism to address machine failure or maintenance

> if the nodehosting an instance goes down or deleted, the Deployment controller replaces the instance with an instance on another Node in the cluster

*Pod* is a Kubernetes abstraction that represents a group of or one or more application containers (like Docker), and some shared resources for those containers:

* shares storage, as Volumes
* networking, as a unique cluster IP address
* information about how to run each container, such as image version or port

a *Pod* models an application-specific "logical host" and can contain different application containers which are relatively tightly coupled

*Pod* is the atomic unit on Kubernetes platform

when we create a Deployment in K8S, the Deployment creates Pods with containers inside them

in case of a Node failure, indentical Pods are schedules on other available Nodes in the cluster
