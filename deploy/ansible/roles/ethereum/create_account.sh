#!/usr/bin/env bash

KEY_NAME=$1
PASSWORD=$2

if [ -z $KEY_NAME ] || [ -z $PASSWORD ]; then
    echo "No keyname or password provided"
    exit 1
fi

FILE=$(expect -c "

spawn web3j wallet create

expect \"password:\"
send \"${PASSWORD}\n\"
expect \"password:\"
send \"${PASSWORD}\n\"
expect \"]:\"
send \"files\n\"
expect eof
" | grep UTC |awk -F'file ' '{print $2}' | awk -F' success' '{print $1}' )

mv files/$FILE files/${KEY_NAME}.key
