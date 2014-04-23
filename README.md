kpe
===

The successor of the keyphrase extractor system SZTERGAK participating at the SemEval shared task on keyphrase extraction from scientific publications.

How to make the code running:
1. Run the script `getLibs` in order to have all the necessary dependant libs. Note that this batch script assumes a UNIX-like envorinment and the accessability of the zip command. This step migth take a couple of minutes depending on your Internet connection. In the project directory type:

``./getLibs``

2. Compile the source code being located in the *src* folder. Provided *ant* is available on your computer you can also use the *build.xml* file for that. Alternatively, you might run the shell script `compileProject.sh`. Note that this step assumes that the PATH environment variable contains the path for your JDK (or JRE).

``./compileProject``
