kpe
===

This project is the successor of the keyphrase extractor system SZTERGAK, which participated at the [SemEval shared task on keyphrase extraction](http://semeval2.fbk.eu/semeval2.php?location=tasks&taskid=6) from scientific publications.

#####How to make the code running:

**The fast lane**: enter the subsequent commands in the project directory (assuming a UNIX-like environment and the accessability of zip and ant commands):  
1. `./getLibs.sh`  
2. `ant`  
3. `ant KpeMainNoTraining`  

**The (not so) fast lane**
1. Running the script `getLibs` downloads dependant libs necessary for building the project. Note that this script assumes a UNIX-like envorinment (and the accessability of the zip command). This step takes a some minutes depending on your Internet connection (as it downloads 210Mb of data approximately). Open a terminal and in the project directory type:   
`./getLibs.sh`   
Having done this, all the necessary libs can be found in the directory *lib/*.
2. You are now ready to compile the project in your favorite IDE or by using *ant* for that purpose. In case *ant* is available on your machine, compilation can be performed by executing `ant` in the project directory.   
3. When running the project, you can decide to create a new model or not. The two options are accessible by `ant KpeMain` and `ant KpeMainNoTraining` configurations.   
Details of the behavior of the main class is influenced by the contents of the *config.txt* and the *config_no_training.txt* files.
The former file is decorated with comments (text following `//`), which are intended to explain the various parameters that can be set within the config files.

###The structure of the config file
See comments included in *config.txt*.

###Writing custom readers
This code was primarily written for dealing with the SemEval shared task dataset. However, arbitrary readers can be added to the project by implementing the `hu.u_szeged.kpe.readers.KpeReader` interface in a similar manner as `hu.u_szeged.kpe.readers.SemEvalReader` does it so.
