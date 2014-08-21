#!/bin/bash

wget http://nlp.stanford.edu/software/stanford-corenlp-full-2014-06-16.zip
unzip stanford-corenlp-full-2014-06-16.zip -d lib/


wget https://google-gson.googlecode.com/files/google-gson-2.2.4-release.zip
unzip google-gson-2.2.4-release.zip -d lib/

rm stanford-corenlp-full-2014-06-16.zip  google-gson-2.2.4-release.zip
