::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
::      Dev environment startup script for Alfresco Community.    ::
::                                                                ::
::      Downloads the spring-loaded lib if not existing and       ::
::      runs the Share AMP applied to Share WAR.                  ::
::      Note. requires Alfresco.war to be running in another      ::
::      Tomcat on port 8080.                                      ::
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
@echo off

set springloadedfile=C:\Users\bphelps\AlfWorkspace\SpringLoaded\springloaded-1.2.1.RELEASE.jar

if not exist %springloadedfile% (
  mvn validate -Psetup
)

set MAVEN_OPTS=-javaagent:"%springloadedfile%" -noverify

mvn integration-test -Pamp-to-war -nsu
:: mvn integration-test -Pamp-to-war 
