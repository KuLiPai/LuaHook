#!/system/bin/sh
MP=/data/adb/ap/bin/magiskpolicy
RULES=/data/local/tmp/policy-rules.txt
while IFS= read -r line; do
  [ -z "$line" ] && continue
  "$MP" --live "$line"
  echo "RC $? :: $line"
done < "$RULES"
