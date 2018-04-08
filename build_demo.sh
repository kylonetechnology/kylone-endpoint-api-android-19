#!/bin/bash
#
# build_demo.sh
# Authors: Gokhan Poyraz <gokhan@kylone.com>
#
# Kylone Client API for Android
# Copyright (c) 2018, Kylone Technology International Ltd.
# API Version 2.0.81
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
#

jarsigner="/usr/bin/jarsigner"
if [ "${JARSIGNER}" != "" ]; then
   jarsigner="${JARSIGNER}"
fi
zipalign="/opt/devel/local/api/android/sdk/build-tools/19.1.0/zipalign"
if [ "${ZIPALIGN}" != "" ]; then
   zipalign="${ZIPALIGN}"
fi

function do_clean() {
   if [ -d "${1}/bin" -a -d "${1}/src" ]; then
      rm -rf "${1}/bin"
   fi
   if [ -d "${1}/src/gen" ]; then
      rm -rf "${1}/src/gen"
   fi
   if [ -d "${1}/src/bin" ]; then
      rm -rf "${1}/src/bin"
   fi
}

function err_check() {
  if test $? -ne 0; then
    exit
  fi
}

function do_compile() {
   cd "${1}"
   ant release
   err_check
}

function do_sign() {
   "${jarsigner}" \
      -sigalg MD5withRSA \
      -digestalg SHA1 \
      -keystore "${1}/src/demo.keystore" \
      -storepass 12345678 \
      -keypass 12345678 \
      -signedjar "${1}/src/bin/Kylone API Sample-release-unaligned.apk" "${1}/src/bin/Kylone API Sample-release-unsigned.apk" \
      com.user.test
   err_check
   "${jarsigner}" -verify "${1}/src/bin/Kylone API Sample-release-unaligned.apk"
   err_check
   mkdir -p "${1}/bin"
   "${zipalign}" \
      -v 4 \
      "${1}/src/bin/Kylone API Sample-release-unaligned.apk" "${1}/bin/Kylone API Sample-release.apk"
   err_check
   echo
   echo "> ${1}/bin/Kylone API Sample-release.apk ready"
   ls -l "${1}/bin/Kylone API Sample-release.apk"
}

pw="`pwd`"
if [ "${1}" == "clean" ]; then
   do_clean "${pw}"
elif [ "${1}" == "make" ]; then
   if [ ! -e "${jarsigner}" ]; then
      echo
      echo "> jarsigner not found, please run script like below;"
      echo "  JARSIGNER=/path/to/jarsigner ${0} ${@}"
      echo
      exit
   else
      echo "Using jarsigner ${jarsigner}"
   fi
   if [ ! -e "${zipalign}" ]; then
      echo
      echo "> zipalign not found, please run script like below;"
      echo "  ZIPALIGN=/path/to/android/sdk/build-tools/19.1.0/zipalign ${0} ${@}"
      echo
      exit
   else
      echo "Using zipalign ${zipalign}"
   fi
   if [ -e "${pw}/src/local.properties" ]; then
      sdkdir=`cat ${pw}/src/local.properties | grep "^sdk.dir=" | awk -F"=" {'print $2'}`
      if [ ! -d "${sdkdir}" ]; then
         echo
         echo "> Android SDK DIR not found"
         echo "  Please change sdk.dir at src/local.properties file"
         echo
         exit
      else
         echo "Using SDK DIR ${sdkdir}"
      fi
   fi
   do_clean "${pw}"
   do_compile "${pw}/src"
   do_sign "${pw}"
else
   echo "> Usage ${0} <make|clean>"
   echo
fi

