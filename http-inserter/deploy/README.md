# Running

## Multiple instances

Set `.spec.parallelism` of the job to the number of instances you want to have.

## Monitor rate

    echo "Pod;Time;Total;Created;Rate;ErrorsR;ErrorsC;AvgR;AvgC"; for i in $(oc get pods -l job-name=http-inserter --no-headers | awk '{ print $1}' ); do echo -n "\"${i}\";"; oc logs --since 10s $i | tail -1; done

