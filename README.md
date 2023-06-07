
##### Grab bootstrap services node port
`kubectl get service kafka-cluster-kafka-external-bootstrap -o=jsonpath='{.spec.ports[0].nodePort}{"\n"}' -n=kafka`

##### Grab node address
`kubectl get node minikube -o=jsonpath='{range .status.addresses[*]}{.type}{"\t"}{.address}{"\n"}'`

##### Route docker commands to minikube
`eval $(minikube -p minikube docker-env)`

##### Show available images in docker registry
`minikube image ls --format table`

## Producer

##### Build producer app
`docker build -t producerapp:latest -f ./producerapp/Dockerfile ./producerapp `

##### Run producer app in cluster
`kubectl run producerapp --image=producerapp --image-pull-policy=Never --restart=Never -n=kafka`

## Consumer

##### Build consumer app
`docker build -t consumerapp:latest -f ./consumerapp/Dockerfile ./consumerapp`

##### Run consumer app in cluster
`kubectl run consumerapp --image=consumerapp --image-pull-policy=Never --restart=Never -n=kafka`
