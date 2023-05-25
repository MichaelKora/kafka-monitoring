# Setup a k3d cluster

## install k3d

wget -q -O - https://raw.githubusercontent.com/k3d-io/k3d/v5.4.6/install.sh | bash

## create k3d docker registry

k3d registry create monitoring-registry.localhost --port 12345

## Create cluster

k3d cluster create monitoring --registry-use k3d-monitoring-registry.localhost:12345 --image rancher/k3s:v1.20.15-k3s1

kubectl create namespace monitoring

## Create docker image

docker build . --tag localhost:12345/kafka-connect-jdbc:7.2.2 && \
docker push localhost:12345/kafka-connect-jdbc:7.2.2

## Deploymwnt

### kafka stack

```yaml
helm repo add confluentinc https://confluentinc.github.io/cp-helm-charts/ &&
helm repo update

helm upgrade \
--install \
--version 0.6.1 \
--values ./kafka-stack.yaml \
--namespace monitoring \
--create-namespace \
--wait \
k8kafka confluentinc/cp-helm-chart
```

### PostgreSQL

```yaml
helm repo add bitnami https://charts.bitnami.com/bitnami && \
helm repo update

helm upgrade --install -f ./postgresql.yaml \
--namespace monitoring \
postgresql bitnami/postgresql
```

### Streams Explorer

```yaml
helm repo add streams-explorer https://bakdata.github.io/streams-explorer
helm repo update

helm install -f streams-explorer.yaml --namespace monitoring streams-explorer streams-explorer/streams-explorer
```

### Prometheus

```yaml
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install -f prometheus.yaml --namespace monitoring prometheus prometheus-community/prometheus
```

### Grafana

```yaml
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
helm install -f grafana.yaml --namespace monitoring  grafana grafana/grafana
```
