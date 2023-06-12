#!/bin/bash

###################
# SSH
###################

#Generate local SSH key
echo -e "\Generating local SSH key...\n"

# -t	(to set Type)
# -f	(Output File)
# -c	(Comment)
# -b	(Bits)
# -q -N	(for no passphrase)
ssh-keygen		\
    -t rsa		\
    -f id_rsa	\
    -C monitoring		\
    -b 2048		\
    -q -N ""


# Modify File Permission (400 -> Only read by User)
chmod 400 id_rsa


# Define a local variable with public key
publickey=$(cat id_rsa.pub)


# Modify the public key for GCP
echo "monitoring:$publickey" > id_rsa_copy.pub


# Upload refomatted SSH Key into project metadata
echo -e "\nUploading Public SSH key into project metadata...\n"

gcloud compute project-info add-metadata	\
    --metadata-from-file=ssh-keys=id_rsa_copy.pub


# Create a Firewall rule (Allow Incoming ICMP and SSH traffic)
# The rule applies only for VMs with the tag cloud-computing
echo -e "\nCreating a Firewall rule for allowing incoming ICMP and SSH traffic...\n"

# gcloud compute firewall-rules [RULE NAME]
# --direction	(to set rule direction)
# --target-tags	(to apply for instances using same tag)
# --rules	(to set firewall protocols)
# --action	(to set the action)
gcloud compute firewall-rules create ssh-icmp-ingress-rule	\
    --direction ingress						\
    --target-tags cloud-computing				\
    --rules tcp:22,icmp						\
    --action allow


###################
# INSTANCE
###################

# Get Project Name and set it to a local variable
projectname=$(gcloud compute project-info describe --format="value(name)")


# Launch an instance
echo -e "\nLaunching Instance...\n"

echo -e "\nLaunching VM for Broker, Producer, Grafana and Prometheus [BACKEND]...\n"
# gcloud compute instances create [INSTANCE NAME]
# --project		(Project Name)
# --image		(to set the instance image)
# --zone		(to set the Zone)
# --machine-type	(to set the machine type)
# --tags		(to set network tags -> firewall rules)
gcloud compute instances create backend-producer	\
    --project=$projectname			\
    --image=projects/ubuntu-os-cloud/global/images/ubuntu-2304-lunar-amd64-v20230530\
    --zone=europe-west1-b			\
    --machine-type=e2-standard-2		\
    --tags=cloud-computing


# Resize Disk Volume to 100GB
echo -e "\nResizing volume to 100GB\n"

# --size	(to set Volume Size)
# --zone	(to set the Zone)
#  -q		(quiet to avoid user input prompt)
gcloud compute disks resize backend-producer	\
    --size=100GB			\
    --zone=europe-west1-b		\
    -q


# Add SSH Identity
ssh-add id_rsa

echo -e "\nLaunching VM for Consumer...\n"
# gcloud compute instances create [INSTANCE NAME]
# --project		(Project Name)
# --image		(to set the instance image)
# --zone		(to set the Zone)
# --machine-type	(to set the machine type)
# --tags		(to set network tags -> firewall rules)
gcloud compute instances create consumer	\
    --project=$projectname			\
    --image=projects/ubuntu-os-cloud/global/images/ubuntu-2304-lunar-amd64-v20230530\
    --zone=europe-west1-b			\
    --machine-type=e2-standard-2		\
    --tags=cloud-computing


# Resize Disk Volume to 100GB
echo -e "\nResizing volume to 100GB\n"

# --size	(to set Volume Size)
# --zone	(to set the Zone)
#  -q		(quiet to avoid user input prompt)
gcloud compute disks resize consumer	\
    --size=100GB			\
    --zone=europe-west1-b		\
    -q

# Upload bench.sh to Instance

# --ssh-key-file	(to set SSH key)
# --zone		(to set the Zone)
# gcloud compute scp bench.sh monitoring@assignment1:/home/monitoring	\
#     --ssh-key-file=./.ssh/id_rsa				\
#     --zone=europe-west1-b

# Stopping Instances

read -n 1 -r -s -p $'Press enter to to Shutdown all VMs...\n'

glcoud compute instances stop backend-producer \
    -q

glcoud compute instances stop consumer \
    -q
