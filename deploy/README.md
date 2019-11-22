# Running

## Multiple instances

Set `.spec.parallelism` of the job to the number of instances you want to have.

## Monitor rate

    for i in $(oc get pods -l job-name=http-inserter --no-headers | awk '{ print $1}' ); do echo -n "${i}: "; oc logs --since 1s $i; done