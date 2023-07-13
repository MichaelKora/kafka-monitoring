# Local setup 

In case you want to test locally, follow this guide to set up a local k3d cluster and image registry.

```shell
# navigate to deployment dir
cd deployment

# install k3d
wget -q -O - https://raw.githubusercontent.com/k3d-io/k3d/v5.4.6/install.sh | bash
# or 
brew install k3d # for Mac
choco install k3d # for Windows

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
![Running cluster should look somehow like this](media/pods.png)
