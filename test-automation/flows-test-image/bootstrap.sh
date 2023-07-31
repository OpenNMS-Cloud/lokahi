#!/bin/sh

set -u
UDPGEN=""
SNMP=""

# User params

USER_PARAMS=$@
if [ ! -z "USER_PARAMS" ]; then
  while getopts "u:,s:" o; do
      case "${o}" in
          s)
              SNMP=${OPTARG}
              ;;
          u)
              UDPGEN=${OPTARG}
              ;;
          *)
              echo "-u 'udpgen args' -s 'snmp args'"
              ;;
      esac
  done
fi

# Launch
if [ ! -z "$UDPGEN" ]; then
  echo "Start udpgen with args ${UDPGEN}"
  tmux new -s udpgen -d "/udpgen ${UDPGEN}"
fi
snmpd -f ${SNMP}

# Exit immidiately in case of any errors or when we have interactive terminal
if [[ $? != 0 ]] || test -t 0; then exit $?; fi
