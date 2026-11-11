#!/bin/bash
sudo -n nft add table ip podman_forward 2>&1
sudo -n nft add chain ip podman_forward forward { type filter hook forward priority 0 ; } 2>&1
sudo -n nft add rule ip podman_forward forward ct state established,related accept 2>&1
sudo -n nft add rule ip podman_forward forward iifname eth0 oifname podman1 accept 2>&1
echo '---AFTER---'
sudo -n nft list ruleset 2>&1 | head -50
