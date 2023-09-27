#!/bin/bash

# Function to print usage instructions
usage() {
  echo "Usage: $0 [--] <command-to-run-in-container>"
  echo "Example: $0 -- ping 192.168.1.1"
  exit 1
}

# Check if there are at least two arguments
if [ $# -lt 2 ]; then
  usage
fi

# Find the first container ID based on the image name
container_id=$(docker container ls --filter "ancestor=opennms/lokahi-minion" -q | head -n 1)

if [ -z "$container_id" ]; then
  echo "No container found matching the specified image name."
  exit 1
fi

# Extract the command to run in the container
shift # Move past the "--" argument
command_to_run_in_container="$@"

# Run the command in the specified container
docker exec -it "$container_id" /bin/bash ./bin/client -- "$command_to_run_in_container"

