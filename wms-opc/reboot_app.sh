#!/bin/bash
if [ $# -ne 1 ];then
       	echo "arguments error!"
       	exit 1
else 
		app=$1
       	echo "Finding $app PID"
       	pid=`pgrep -f "$app.jar"`
       	if [ -n "$pid" ]; then 
		echo "Killing $app"
	     kill -9 $pid
	     echo "$app is dead"
	     else 
	     echo "$app is not running" 
	     fi
		sudo nohup java -jar /home/gitlab-runner/application/wms-opc/$app.jar > /home/gitlab-runner/application/wms-opc/$app-running.log 2>&1 &
		newPid=`pgrep -f "$app.jar"`
		if [ -n "$newPid" ]; then 
		echo "$app is running again, its new pid is $newPid"
		else
		echo "$app reboot failed"
		fi
fi