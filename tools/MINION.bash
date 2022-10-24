#!/bin/bash

USER=admin

kubectl exec -it deployment.apps/my-horizon-stream-minion -- /bin/bash
