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
    ps -ef | grep java | grep "$app.jar" | awk '{print $2}' | xargs kill
    echo "$app is dead"
  else
    echo "$app is not running"
  fi
  sudo nohup java -jar /home/gitlab-runner/application/wms-opc/$app.jar >/home/gitlab-runner/application/wms-opc/$app-running.log 2>&1 &
  echo "### RESULT ###"
  result=$(pgrep -fa "$app.jar")
  echo $result
fi
