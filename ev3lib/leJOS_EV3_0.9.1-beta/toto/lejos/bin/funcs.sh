waitDisplay()
{
  if [[ -f /var/run/logtime ]]; then
    local now=$(date +%s)
    prevTime=$(date -r /var/run/logtime +%s)
    prevTime=$(( $prevTime + 1 ))
    if [ "$prevTime" -ge "$now" ]; then
      sleep 1
    fi
  fi
  touch /var/run/logtime
}

log()
{
  waitDisplay
  if [[ -f /tmp/logfile ]]; then
    echo $1 >> /tmp/logfile
  fi
  echo $1
}

warning()
{
  log "$1"
  sleep 5
}

startLog()
{
  export prevTime=0
  setterm -clear > /dev/tty1
  # make sure terminal is enabled
  echo 1 > /sys/class/vtconsole/vtcon1/bind
  # configure terminal
  setterm -cursor off > /dev/tty1
  setterm -blank 0 > /dev/tty1
  setterm -linewrap off > /dev/tty1
  setterm -clear > /dev/tty1
  # load the logo
  cp ${LEJOS_HOME}/images/lejoslogo.ev3i /dev/fb0
  rm /tmp/logfile 2> /dev/null
  touch /tmp/logfile
  ${LEJOS_HOME}/bin/spinner.sh > /dev/tty1 &
}

stopLog()
{
  waitDisplay
  # stop spinner
  rm /tmp/logfile 2> /dev/null
  sleep 1
  # disable console
  echo 0 > /sys/class/vtconsole/vtcon1/bind
}


error()
{
  # some sort of error, log it
  log "Install error"
  echo $1 >> /tmp/logfile
  # try and save the error info.
  mount /dev/mmcblk0p1 /media/bootfs
  cp /tmp/logfile /media/bootfs
  umount /media/bootfs
  rm /tmp/logfile
  sleep 2
  setterm -clear > /dev/tty1
  echo "Install error:" > /dev/tty1
  echo $1 > /dev/tty1
  echo
  sleep 20
  echo "Power off" > /dev/tty1
  sleep 10
  poweroff -f
  exit 1
}


