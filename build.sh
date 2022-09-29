#!/bin/sh -e

build_all()  {
    mvn -f shared clean install -e
#   mvn -f cli clean install -e
    mvn -f eclipse clean verify -e

}



if [ $# -eq 1 ]; then
    case "$1" in
        eclipse)
            mvn -f shared clean install
            mvn -f eclipse clean verify
            ;;
        cli)
            mvn -f shared clean install
            mvn -f cli clean install
            ;;
        *)
            build_all
            ;;
    esac
else
    build_all
fi
