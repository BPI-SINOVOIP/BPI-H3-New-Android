#!/bin/sh

source send_cmd_pipe.sh
source script_parser.sh

module_count=`script_fetch "tv" "module_count"`
if [ $module_count -gt 0 ]; then
    for i in $(seq $module_count); do
        key_name="module"$i"_path"
        module_path=`script_fetch "tv" "$key_name"`
        if [ -n "$module_path" ]; then
            echo "insmod $module_path"
            insmod "$module_path"
            if [ $? -ne 0 ]; then
                echo "insmod $module_path failed"
            fi
        fi
    done
fi

sleep 3

source config_ac100.sh

device_name=`script_fetch "tv" "device_name"`
	tvtester $* "$device_name" &
	exit 0
SEND_CMD_PIPE_FAIL $3
