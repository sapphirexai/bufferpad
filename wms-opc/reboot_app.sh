#!/bin/bash
if [ $# -lt 1 ] || [ $# -gt 2 ]; then
  echo "usage: $0 <app-name-without-.jar> [profile]"
  exit 1
else
  app=$1
  profile=${2:-${SPRING_PROFILES_ACTIVE:-prod}}
  app_dir=/home/gitlab-runner/application/wms-opc
  echo "Finding $app PID"
  pid=$(pgrep -f "$app.jar")
  if [ -n "$pid" ]; then
    echo "Killing $app"
    kill -9 $pid
    echo "$app is dead"
  else
    echo "$app is not running"
  fi
  mkdir -p "$app_dir/logs"
  cd "$app_dir" || exit 1
  sudo nohup java -jar "$app.jar" --spring.profiles.active="$profile" >/dev/null 2>&1 &
  echo "### START SUCCESS ###"
fi
