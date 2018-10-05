@echo off
setlocal
  title NW Instrumentation KITE Test
  java  -Dkite.firefox.profile=../third_party/ -cp ../KITE-Engine/target/kite-jar-with-dependencies.jar;target/apprtc-test-jar-with-dependencies.jar org.webrtc.kite.Engine configs/interop.apprtc.config.json
endlocal
