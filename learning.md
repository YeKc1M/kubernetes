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
