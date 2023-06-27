# Prerequisites

This workflow description assumes you've configured your `gcloud` cli like described [here](./gcp/README.md).
```bash
# don't forged to connect gcloud to the project
gcloud config set project pj-distributed-systems-sose23
```

### Port-forward Grafana and Prometheus
```bash
# Log into the controller node as root via ssh
gcloud compute ssh root@controller-1 --zone=europe-west1-b

# open k9s
k9s
```
Then, port-forward prometheus-kube-prometheus-stack-prometheus and kube-prometheus-stack-grafana via pressing `shift-f`.

### After cluster or VMs have been killed, you need to create the test topic again
```bash
bin/kafka-topics.sh --create --topic topic1 --bootstrap-server cluster-kafka-bootstrap.kafka:9092 --partitions 12 --replication-factor 1
```

### To have a look into the grafana dashboard or prometheus, you have to create a ssh-tunnel to the vm
```bash
# for grafana
gcloud compute ssh controller-1 --zone=europe-west1-b -- -NL 3000:localhost:3000
# for grafana and prometheus 
gcloud compute ssh controller-1 --zone=europe-west1-b -- -NL 3000:localhost:3000 -NL 9090:localhost:9090
```
