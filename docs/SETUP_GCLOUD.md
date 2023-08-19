# Ansible Playbook

## 1. Setup local Environment

- Install ansible using your package manager like `sudo apt install ansible` or follow this guide https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html
- Install the `gcloud` CLI using this guide https://cloud.google.com/sdk/docs/install
- Activate the current project via `gcloud config set project $PROJECT_ID` and make sure `gcloud` is running 
- Create local `.vault_pass` file: `echo ds2023 > .vault_pass`

## 2 Configure GCP
- Store your **public** ssh key into the folder `files/ssh/` and make sure that is has the ending `.pub`
- Run the command
```
ansible-playbook playbook.yml -t init
```
This command will:
- Upload all SSH keys to the project
- Configure all firewall rules
- Create all VMs and automatically started them

**Attention**: Remember to stop them if not needed

For just updating the ssh keys run
```
ansible-playbook playbook.yml -t ssh
```

## 3. Starting all VMs
Run this command to start all VMs.

```
ansible-playbook playbook.yml -t start
```

## 4. Stopping all VMs
Run this command to stop all VMs.

```
ansible-playbook playbook.yml -t stop
```

## 5. Configure the whole Setup
Run this command to install Docker and Kubernetes, Helm, HelmFile (and all dependencies) on the virtual machines

```
# right after spinning up the VM's via the start command, the following command can fail because they need some time 
# to be fully ready and to enable ssh-connection 
ansible-playbook playbook.yml -t setup
```

For debugging you can run steps during the setup separately. Available tags are:
- **setup**: all dependencies are installed
- **docker**: only install and setup docker
- **kubernetes**: only install and setup kubernetes
- **repo**: for cloning the application repository and running the helmfile

There are the following ansible roles:
- **general**: for all packages and tasks that will be run the same on all machines
- **kubernetes_controller**: all tasks to be run on the kubernetes controller, like initializing the cluster and creating the join command
- **kubernetes_node**: all to be run on the nodes, like joining the cluster
- **application**: cloning the application and running the helmfile. (has to be a separate role, because it needs to be executed after the nodes joining the cluster)


## Add more VMs
To add more virtual machine just add the google name in the inventory category.
