#!/bin/bash
set -e

OUT_DIR="out"

javac -d $OUT_DIR Main.java
java -cp $OUT_DIR Main
printf "\n"
