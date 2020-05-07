#!/bin/bash

pushd `dirname $0` > /dev/null
SCRIPTPATH=`pwd -P`
popd > /dev/null

$SCRIPTPATH/script/size_report.sh "BlinkInput" $SCRIPTPATH/../BlinkInputSample BlinkInputFieldByFieldSample $SCRIPTPATH/sdk_size_report.md
