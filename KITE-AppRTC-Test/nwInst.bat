@echo off
setlocal
  title NW Instrumentation KITE Test
  java -cp ../KITE-Engine/target/kite-jar-with-dependencies.jar;target/apprtc-test-1.0-SNAPSHOT.jar org.webrtc.kite.Engine configs/interop.apprtc.config.json
endlocal
