#!/bin/bash

wget http://nlp.stanford.edu/software/stanford-corenlp-full-2014-01-04.zip
unzip stanford-corenlp-full-2014-01-04.zip -d lib/


wget https://google-gson.googlecode.com/files/google-gson-2.2.4-release.zip
unzip google-gson-2.2.4-release.zip -d lib/

rm stanford-corenlp-full-2014-01-04.zip  google-gson-2.2.4-release.zip
