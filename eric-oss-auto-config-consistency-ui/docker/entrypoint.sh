#
# COPYRIGHT Ericsson 2023
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

#!/bin/bash
rm -rf /etc/apache2/*
rm -rf /app/*
rm -rf /var/log/apache/*

mkdir ${APP_DIR}/log; mkdir ${APP_DIR}/log/apache2; > ${APP_DIR}/log/apache2/error_log
cp /tmp/apache2/httpd.conf /etc/apache2/httpd.conf
/usr/sbin/apachectl -D FOREGROUND