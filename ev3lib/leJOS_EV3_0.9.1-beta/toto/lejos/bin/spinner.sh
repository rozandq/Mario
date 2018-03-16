#!/bin/busybox ash

logfile=/tmp/logfile
logsize=0
spinpause=0.10
linelen=0


# Output last line from log file.
lastout()
{
    local line=$(tail -n 1 $logfile 2>/dev/null)
    if [[ "$line" ]]; then
        line=${line:0:21}
        echo -n "$line"

        # Erase any extra from last line.
        local len
        let len=$linelen-${#line}
        while [[ $len -gt 0 ]]
        do
            echo -n " "
            let len--
        done
        linelen=${#line}
    fi
}

# Output a spin character.
spinout()
{
    local spinchar="$1"
    local sz
    local ll
    local count=1
    while [ $count -gt 0 ]; do
        let count=count-1 
        if [[ -f $logfile ]]; then
            # Check for new message.
            sz=$logsize
            sz=$(ls -l $logfile 2> /dev/null | awk '{print $5;}' 2>/dev/null)
            if [[ $sz -gt $logsize ]]; then
                echo -n -e "\r "
                lastout
                logsize=$sz
                count=10
            fi
            sleep $spinpause
        else
            return
        fi
    done
    if [[ -f $logfile ]]; then
        echo -n -e "\r$spinchar"
    fi
}

echo
echo
echo 
echo
echo

if [[ -f $logfile ]]; then
    logsize=$(ls -l $logfile 2> /dev/null | awk '{print $5;}' 2>/dev/null)
    if [[ $logsize -gt 0 ]]; then
        echo -n " "
        lastout
    fi
    while [[ -f $logfile ]]
    do
        spinout "/"
        spinout "-"
        spinout "\\"
        spinout "|"
        spinout "/"
        spinout "-"
        spinout "\\"
        spinout "|"
    done
    echo -n -e "\r "
    echo
fi
