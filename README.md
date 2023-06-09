
### Basic Setup & Requirements

```shell
# install Kubernetes CLI (see https://kubernetes.io/de/docs/tasks/tools/install-kubectl/)
brew install kubernetes-cli # for Mac
choco install kubernetes-cli # for Windows

# install K9s CLI tool (for administrating a Kubernetes cluster, see https://k9scli.io/topics/install/)
brew install derailed/k9s/k9s # for Mac
choco install k9s # for Windows
```

For the cluster setup including 
- Kafka, 
- Redpanda,
- Prometheus and
- Grafana <br>

see **[SETUP.md](docs/SETUP.md)**.

# Kafka setup

```shell
# create our test topic from within any broker pod (e.g. k8kafka-cp-kafka-0)
kafka-topics --bootstrap-server k8kafka-cp-kafka-headless.monitoring:9092 \
--topic topic1 \
--create \
--partitions 12 \
--replication-factor 1
```

### producerapp
```shell
# Build producer app and push to registry (assuming you are in the projects root dir)
docker build -t localhost:12345/producerapp:latest -f ./producerapp/Dockerfile ./producerapp 
docker push localhost:12345/producerapp:latest

# Run producer app in cluster under namespace monitoring
kubectl apply -f deployment/producerapp.yaml -n monitoring
```

### consumerapp
```shell
# Build producer app and push to registry (assuming you are in the projects root dir)
docker build -t localhost:12345/consumerapp:latest -f ./consumerapp/Dockerfile ./consumerapp 
docker push localhost:12345/consumerapp:latest

# Run producer app in cluster under namespace monitoring
kubectl apply -f deployment/consumerapp.yaml -n monitoring
```

# Grafana
To view Grafana under `localhost:3000`, just port-forward the `grafana-0` pod via `k9s`. <br>
Login credentials default to `admin` for username and password.
