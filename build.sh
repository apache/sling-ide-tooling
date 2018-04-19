#!/bin/sh -e

build_all()  {
    mvn -f shared/modules clean install
    mvn -f cli clean install
    mvn -f shared/p2 clean package
    mvn -f eclipse clean verify

}



if [ $# -eq 1 ]; then
    case "$1" in
        eclipse)
            mvn -f shared/modules clean install
            mvn -f shared/p2 clean package
            mvn -f eclipse clean verify
            ;;
        cli)
            mvn -f shared/modules clean install
            mvn -f cli clean install
            ;;
        *)
            build_all
            ;;
    esac
else
    build_all
fi
