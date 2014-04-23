kpe
===

This project is the successor of the keyphrase extractor system SZTERGAK, which participated at the [SemEval shared task on keyphrase extraction](http://semeval2.fbk.eu/semeval2.php?location=tasks&taskid=6) from scientific publications.

#####How to make the code running:

1. Run the script `getLibs` in order to have all the necessary dependant libs. Note that this script assumes a UNIX-like envorinment (and the accessability of the zip command). This step takes a some minutes depending on your Internet connection (as it downloads 210Mb of data approximately). Open a terminal and in the project directory type:   
`./getLibs.sh`   
Having done this, all the necessary libs can be found in the directory *lib/*.
2. You are now ready to compile the project in your favorite IDE or by using *ant* for that purpose. In the case *ant* is available on your computer, compilation can be performed as easy as executing command :   
`ant`   
3. Before `ant KpeMain` or `ant KpeMainNoTraining`   
The behavior of the main class is defined by the contents of the *config.txt* file which is described next.

###The structure of the config file




###Writing custom readers

This code was primarily written for dealing with the SemEval shared task dataset. However, arbitrary readers can be added to the project by implementing the `hu.u_szeged.kpe.readers.KpeReader` interface in a similar manner as `hu.u_szeged.kpe.readers.SemEvalReader` does it so.
