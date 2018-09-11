@echo off
IF "%1"=="cosmogrid" (
  java -cp ../KITE-Engine/target/kite-jar-with-dependencies.jar;target/callstats-test-1.0-SNAPSHOT.jar org.webrtc.kite.Engine configs/cosmogrid.config.json
) ELSE IF "%1"=="win" (
  java -cp ../KITE-Engine/target/kite-jar-with-dependencies.jar;target/callstats-test-1.0-SNAPSHOT.jar org.webrtc.kite.Engine configs/win.config.json
) ELSE (
  echo "Usage: run win|cosmogrid"
)


