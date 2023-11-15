#!/bin/sh

# $1 Path of latexmk and xelatex
# $2 Name of the tex file which should be compiled
export PATH=$PATH:$1
latexmk -xelatex -halt-on-error $2