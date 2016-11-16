#!/bin/bash

mvn install:install-file -Dfile=LibBlinkInput.aar -DpomFile=pom.xml -DcreateChecksum=true -Djavadoc=LibBlinkInput-javadoc.jar