
# Basic setup

```shell
# install minikube  (see https://kubernetes.io/de/docs/tasks/tools/install-minikube/)
brew install minikube # for Mac
choco install minikube kubernetes-cli # for Windows 

# install Kubernetes CLI (see https://kubernetes.io/de/docs/tasks/tools/install-kubectl/)
brew install kubernetes-cli # for Mac
choco install kubernetes-cli # for Windows

# install K9s CLI tool (for administrating a Kubernetes cluster, see https://k9scli.io/topics/install/)
brew install derailed/k9s/k9s # for Mac
choco install k9s # for Windows
```

## Deploy Kafka with [Strimzi](https://strimzi.io/) 

```shell
# install Strimzi operator in namespace 'kafka'
kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka

# deploy Kafka
cd deployment
kubectl apply -f kafka.yaml -n kafka

# Grab bootstrap services node port and node address 
# ! our applications (consumer/producer) need this address nodeAddress:nodePort as their bootstrap server
kubectl get service kafka-cluster-kafka-external-bootstrap -o=jsonpath='{.spec.ports[0].nodePort}{"\n"}' -n=kafka
kubectl get node minikube -o=jsonpath='{range .status.addresses[*]}{.type}{"\t"}{.address}{"\n"}'
```

##### Route docker commands to minikube
```shell
# Needs to be done every time you open a new terminal! 
eval $(minikube -p minikube docker-env)

# Show available images in docker registry
minikube image ls --format table
```

### Deploy Producerapp

```shell
# Build producer app (assuming you are in the projects root dir)
docker build -t producerapp:latest -f ./producerapp/Dockerfile ./producerapp 

# Run producer app in cluster
kubectl run producerapp --image=producerapp --image-pull-policy=Never --restart=Never -n=kafka
```

### Deploy Consumerapp

```shell
# Build consumer app (assuming you are in the projects root dir)
docker build -t consumerapp:latest -f ./consumerapp/Dockerfile ./consumerapp

# Run consumer app in cluster
kubectl run consumerapp --image=consumerapp --image-pull-policy=Never --restart=Never -n=kafka
```
