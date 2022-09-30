#!/bin/sh -e

build_all()  {
    mvn -e -f shared clean install
    mvn -e -f cli clean install
    mvn -e -f eclipse clean verify

}

if [ $# -eq 1 ]; then
    case "$1" in
        eclipse)
            mvn -e -f shared clean install
            mvn -e -f eclipse clean verify
            ;;
        cli)
            mvn -e -f shared clean install
            mvn -e -f cli clean install
            ;;
        *)
            build_all
            ;;
    esac
else
    build_all
fi
