# Basic setup of local k3d cluster

```shell
# navigate to deployment dir
cd deployment

# install k3d
wget -q -O - https://raw.githubusercontent.com/k3d-io/k3d/v5.4.6/install.sh | bash

# create k3d docker registry
k3d registry create monitoring-registry.localhost --port 12345

# Create cluster
k3d cluster create monitoring --registry-use k3d-monitoring-registry.localhost:12345 --image rancher/k3s
kubectl create namespace monitoring

# in case you want to stop/delete the cluster
k3d cluster stop monitoring
k3d cluster delete monitoring
```

# CLI tools
```shell
# install helm and helmfile
brew install helm # for Mac
brew install helmfile
choco install kubernetes-helm # for Windows
choco install helmfile

# install Kubernetes CLI (see https://kubernetes.io/de/docs/tasks/tools/install-kubectl/)
brew install kubernetes-cli # for Mac
choco install kubernetes-cli # for Windows

# install K9s CLI tool (for administrating a Kubernetes cluster, see https://k9scli.io/topics/install/)
brew install derailed/k9s/k9s # for Mac
choco install k9s # for Windows
```

# Deployment of whole stack

##### Kafka, Strimzi-Operator, Prometheus, Prometheus JMX Exporter, Grafana, Redpanda console
```shell
# install whole stack via helmfile
helmfile apply
```

# Kafka setup

### producerapp
```shell
# Build producer app and push to registry (assuming you are in the projects root dir)
docker build -t localhost:12345/producerapp:latest -f ./producerapp/Dockerfile ./producerapp 
docker push localhost:12345/producerapp:latest

# Run producer app in cluster under namespace monitoring
kubectl apply -f deployment/producerapp.yaml -n kafka 
```

### consumerapp
```shell
# Build producer app and push to registry (assuming you are in the projects root dir)
docker build -t localhost:12345/consumerapp:latest -f ./consumerapp/Dockerfile ./consumerapp 
docker push localhost:12345/consumerapp:latest

# Run producer app in cluster under namespace monitoring
kubectl apply -f deployment/consumerapp.yaml -n kafka 
```

# Grafana
To view Grafana under `localhost:3000`, just port-forward the `kube-prometheus-stack-grafana` pod via `k9s`. <br>
Login credentials default to `admin` for username and `prom-operator` for password.

# Redpanda concole 
To view Redpanda console under `localhost:8081`, just port-forward the `redpanda-console` pod via `k9s`.
