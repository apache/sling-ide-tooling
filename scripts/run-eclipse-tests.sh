#!/bin/bash

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
base_dir="${script_dir}/.."
count=${1:-10}
pass=0
additional_args=""
fail_fast=0

while getopts ":c:t:dfh" opt; do
    case $opt in
        c)
            count=${OPTARG}
            ;;
        d)
            additional_args="${additional_args} --debug"
            ;;
        t)
            # don't fail for no tests since we execute multiple test projects
            additional_args="${additional_args} -Dtest=${OPTARG} -DfailIfNoTests=false"
            ;;
        f)
            fail_fast=1
            ;;
        h)
            echo "Usage: $0 [-c count] [-t test] [-d] [-f]"
            echo
            echo -e "-c\tCount of test iterations to run, defaults to 10"
            echo -e "-t\tSingle test name to run"
            echo -e "-d\tEnable debug mode for Maven, also enables platform tracing for Eclipse tests"
            echo -e "-f\tEnable fail-fast mode - execution stops after first failure"
            exit 0
            ;;
    esac
done

if [ -d ${base_dir}/target ]; then
    rm -rf ${base_dir}/target/*
else
    mkdir ${base_dir}/target
fi


for step in $(seq 1 ${count}); do
    echo "----------------------------"
    echo "Step/Pass/Total: ${step}/${pass}/${count}"
    echo "----------------------------"

    mkdir ${base_dir}/target/${step}

    mvn -f ${base_dir}/eclipse clean verify ${additional_args} | tee ${base_dir}/target/${step}/build.log
    exec_result=${PIPESTATUS[0]}
    if [ ${exec_result} -eq 0 ]; then
        pass=$((pass+1))
    fi
    mv $(find eclipse/eclipse-test/target -maxdepth 2 -type d -name sling) ${base_dir}/target/${step}/sling
    if [ ${exec_result} -ne 0 ] && [ ${fail_fast} -eq 1 ]; then
        echo "----------------------------"
        echo "FAIL-FAST, stopping execution"
        echo "----------------------------"
        break
    fi
done

echo "----------------------------"
echo "FINAL STATS: Pass/Total: ${pass}/${count}"
echo "----------------------------"

