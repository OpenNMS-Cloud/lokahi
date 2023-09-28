#!/bin/bash

# Function to print usage instructions
usage() {
  echo "Usage: $0 <command-to-run-in-container>"
  echo "Example: $0 ping 192.168.1.1"
  exit 1
}

# Check if there are at least one arguments
if [ $# -lt 1 ]; then
  usage
fi

# Find the containers based on the image name
container_list=$(docker container ls --filter "ancestor=opennms/lokahi-minion" -q)

# Count the number of containers in the list
container_count=$(echo "$container_list" | wc -l)

# Display a warning if there are multiple containers with the same ancestor
if [ "$container_count" -ge 2 ]; then
  echo "Warning: Multiple containers found with ancestor 'opennms/lokahi-minion':"
  docker container ls --filter "ancestor=opennms/lokahi-minion"
fi

# Find the first container ID based on the image name
container_id=$(echo "$container_list" | head -n 1)

if [ -z "$container_id" ]; then
  echo "No container found matching the specified image name."
  exit 1
fi


# Run the command in the specified container
docker exec -it "$container_id" /bin/bash ./bin/client -- "$@"

