# Basic setup of local k3d cluster

```shell
# navigate to deployment dir
cd deployment

# install k3d
wget -q -O - https://raw.githubusercontent.com/k3d-io/k3d/v5.4.6/install.sh | bash
# or 
brew install k3d # for Mac
choco install k3d # for Windows

# install k3d
wget -q -O - https://raw.githubusercontent.com/k3d-io/k3d/v5.4.6/install.sh | bash

# create k3d docker registry
k3d registry create monitoring-registry.localhost --port 12345

# Create cluster
k3d cluster create monitoring --registry-use k3d-monitoring-registry.localhost:12345 --image rancher/k3s

# in case you want to stop/delete the cluster
k3d cluster stop monitoring
k3d cluster delete monitoring

# create k3d docker registry
k3d registry create monitoring-registry.localhost --port 12345

# Create cluster
k3d cluster create monitoring --registry-use k3d-monitoring-registry.localhost:12345 --image rancher/k3s

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
# install whole stack via helmfile (from within `deployment` dir)
helmfile apply
```
![Running cluster should look somehow like this](docs/media/pods.png)

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
Login credentials default to `admin` for username and `prom-operator` for password. <br>
Lots of dashboards already exist. Feel free to change and save under `deployment/charts/kafka-cluster/dashboards`.

![Dashboards overview](docs/media/dashboards.png)

# Redpanda concole
Kafka is configured to auto-create topics. For managing and gaining insight of the kafka deployment, you can use the
Redpanda console.
To view the console, just port-forward the `redpanda-console` pod via `k9s` and visit `localhost:8081`.

![Redpanda console topic overview](docs/media/redpanda_console.png)
