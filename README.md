kpe
===

The successor of the keyphrase extractor system SZTERGAK participating at the SemEval shared task on keyphrase extraction from scientific publications.

How to make the code running:

1. Run the script `getLibs` in order to have all the necessary dependant libs. Note that this batch script assumes a UNIX-like envorinment (and the accessability of the zip command). This step takes a some minutes depending on your Internet connection (as it downloads 210Mb of data approximately). In the project directory type:

`./getLibs`

Having done this, all the necessary libs can be found in the directory *lib/*.

2. You are now ready to compile and run the project in your favorite IDE or by using *ant* for that purpose. In the case *ant* is available on your computer, compiling and running the project can be obtained via executing the following two commands:

`ant`

`ant KpeMain`

The behavior of the main class is defined by the contents of the *config.txt* file which is described next.

The structure of the config file
---



Writing custom readers
---
