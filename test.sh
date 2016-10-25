class=$1
thread_numbers="2 4 8 16 32 64 128 256"
for N in $thread_numbers; do
  java -cp build/libs/PingsPongs-1.0-SNAPSHOT.jar com.blumental.pingspongs.$class <<< "$N 1000"
done
