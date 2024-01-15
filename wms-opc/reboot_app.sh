#!/bin/bash
if [ $# -ne 1 ]; then
  echo "arguments error!"
  exit 1
else
  app=$1
  echo "Finding $app PID"
  pid=$(pgrep -f "$app.jar")
  if [ -n "$pid" ]; then
    echo "Killing $app"
    pgrep -f "$app.jar" | xargs kill
    echo "$app is dead"
  else
    echo "$app is not running"
  fi
  sudo nohup java -jar /home/gitlab-runner/application/wms-opc/$app.jar >/dev/null 2>&1 &
  echo "### START SUCCESS ###"
fi
