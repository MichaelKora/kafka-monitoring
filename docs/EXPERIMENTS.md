# Experiments
This guide will show to run a series of experiments in the Google Cloud. It is required to setup the `gcloud` environment and running the `init` and `setup` Ansible Playbook tags in advance. Also make sure to have Ansible installed.

## Variables
All variables are definied in the Ansible `group_vars` file of the group `kubernetes_controllers`.
The file is located at:
```
gcp/group_vars/kubernetes_controllers/main.yml
```

At top of the file you find the default values for the consumer, the producer and the resources.
Ansible will render the file located at `deplyment/charts/{consumerapp,producerapp}/values.yaml`.

At the end of `group_vars` file you find the list of dicts `experiments`. Each entry defines an experiment round.
It has to consist of:
- A string `name` identifying the round
- A int or float `duration` in hours
- A dict `values` defining, which values should be changed. **Note**: You can only set values relatively to the last round. Make sure to add all values to prevent unexpected parameter sets.

### Consumer
|Configuration|Meaning|
|---|---|
|autoscaling_enabled|Enables the HPA autoscaler (Default: true)|
|autoscaling_min_replicas|How many replicas should be there at least. (Default: 1)|
|autoscaling_max_replicas|How many replicas should be there at most. Keep in mind to adjust the number of partitions accordingly. (Default: 24)|
|autoscaling_scale_up|Configures scaling up behaviour. Dict values are `window` and `policies`, consisting of `type`, `value` and `period_seconds`|
|autoscaling_scale_down|Configures scaling down behaviour. Dict values are `window` and `policies`, consisting of `type`, `value` and `period_seconds`|
|autoscaling_hpa|Necessary to set the CPU target utilization. Dict values are `enabled` and `target_cpu_util`.(Default: 80)|
|autoscaling_keda|Configures behaviour of keda. Dict values are `enabled`, `polling_interval`. `cooldown_period`, `trigger_cpu_value` and `trigger_kafka_lag_threshold`|

### Producer
|Configuration|Meaning|
|---|---|
|producer_workload_pattern|Choose the applied workload pattern. Valid strings are: **Static**, **Pattern**, **Random**, **Stair**|
|producer_pattern_window|Controls the duraction of one pattern cycle. (Default: 30)|
|producer_messages_per_minute|Controls the amount of messages per minute. (Default: 1500)|
|producer_sleep_time|Controls the timeout between sending messages to Kafka. (Default: 5)|
|producer_replica_count|Triggers to start sending messages. Setting to 0 to disable the producer. (Default: 1)|

### Resources
|Configuration|Meaning|
|---|---|
|autoscaling_resources|Controls the pod's resources. Consists of dict `limits` and `requets`. Each contain of `cpu` (ending with `m`, Default:`250m`) and `memory` (ending with `Mi`, Default: `512Mi`)|

For detailed informartion have a look at the kubernetes documentation: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/

## Ansible hint:
A list always starts with a `-`, while each element of a dict is just added intended.
Example:
```
# Lists
- List elem 1
- List elem 2
- List elem 3

# Dict
Dict elem 1: value
Dict elem 2: value
Dict elem 3: value

# List of dicts
- Dict 1 elem 1: value
  Dict 1 elem 2: value
  Dict 1 elem 3: value
- Dict 2 elem 1: value
  Dict 2 elem 2: value
  Dict 2 elem 3: value
```

## Starting the experiment
Assume that the experiment series is setup in `group_vars/kubernetes_controllers/main.yml` wanted.
1. Open a terminal go into the directory `gcp`.
2. Make sure the gcloud cluster is running by using the command: 
```
ansible-playbook playbook.yml -t start
```
4. Init the experiment with the command: 
```
ansible-playbook playbook.yml -t experiments
```
5. The playbook will make the Grafana instance available by using Port-forwarding and show you the link. Confirm to start the experiment by pressing any button
```
TASK [experiments : Show link to Grafana] *************************************************
ok: [controller-1] => {
    "msg": [
        "--------------------------------------",
        "ATTENTION",
        "--------------------------------------",
        "This playbook will not terminate. To",
        "quit hit CTRL + C",
        "",
        "Open Grafana via http://localhost:3000",
        "",
        "or enable port-forwarding yourself with",
        "blocking command",
        "gcloud compute ssh controller-1 --zone=europe-west1-b -- -NL 3000: grafana_ip :80"
    ]
}
```
6. The configuration is rendered, uploaded and applied. Afterwards the playbook prints when it is ready and waits for the experiment round to finish.
7. You should be able to see and export the values in the Grafana Dashboard.
