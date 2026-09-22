#!/system/bin/sh
# Late-start service: enable wireless ADB on port 5555
MODDIR=${0%/*}
LOG="$MODDIR/start.log"

log() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') $*" >> "$LOG"
}

log "start-adb-tcp: begin"
settings put global adb_enabled 1
setprop persist.adb.tcp.port 5555
setprop service.adb.tcp.port 5555
stop adbd
start adbd
log "start-adb-tcp: adbd restarted on tcp 5555"
