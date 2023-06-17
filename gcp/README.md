# Ansible Playbook

## 1. Setup local Environment

- Install ansible using your package manager like `sudo apt install ansible` or follow this guide https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html
- Install the `gcloud` CLI using this guide https://cloud.google.com/sdk/docs/install
- Activate the current project and make sure `gcloud` is running

## 2. Creating all VMs
If there isn't any VM in the project or you delete all, create them using

```
ansible-playbook playbook.yml -t init
```

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

## 5. Install Docker and Kubernetes
Run this command to install Docker and Kubernetes (and all dependencies) on the virtual machines

```
ansible-playbook playbook.yml -t setup
```

## Add more VMs
To add more virtual machine just add the google name in the inventory category.