# Ansible Playbook

## 1. Setup local Environment

- Install ansible using your package manager like `sudo apt install ansible` or follow this guide https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html
- Install the `gcloud` CLI using this guide https://cloud.google.com/sdk/docs/install
- Activate the current project and make sure `gcloud` is running

## 1.1 Configure GCO
- Store your **public** key into the folder `files/ssh/` and make sure that is has the ending `.pub`
- Run the command
```
ansible-playbook playbook.yml -t init
```
This command will:
- Upload all SSH keys to the project
- Configure all firewall rules
- Create all VMs and automatically started them

**Attention**: Remeber to stop them if not needed


## 3. Starting all VMs
Run this command to start all VMs.

```
ansible-playbook playbook.yml -t start
```

**Attention**
You must run this command even if you just created the VMs, because it will update the IP addresse in the inventory.

## 4. Stoping all VMs
Run this command to stop all VMs.

```
ansible-playbook playbook.yml -t stop
```

## 5. Configure the whole Setup
Run this command to install Docker and Kubernetes, Helm, HelmFile (and all dependencies) on the virtual machines

```
ansible-playbook playbook.yml -t setup
```

There are the following ansible roles:
- **general**: for all packages and tasks that will be run the same on all machines
- **kubernetes_controller**: all tasks to be runned on the kubernetes controller, like initializing the cluster and creating the join command
- **kubernetes_node**: all to be runned on the nodes, like joining the cluster
- **application**: cloning the application and running the helmfile. (has to be a seperate role, because it needs to be executed after the nodes joining the cluster)


## Add more VMs
To add more virtual machine just add the google name in the inventory category.