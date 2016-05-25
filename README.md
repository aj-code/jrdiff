jrdiff
=======

A non-compatible implementation of rdiff in Java.

This allows you to generate a signature of a file. Then once that file changes you can generate a delta file using the old signature. This delta can then be used to patch an old copy of the file to bring it up to date with the new file. This type of scheme is used under the hood by rsync and can be good for a few things like creating a backup system, efficiently syncing changes between servers, etc.

### Requirements ###
There are no external dependencies outside of a JVM. jrdiff should run anywhere Java 5+ runs: Windows, mac, linux, probably even Android. Only linux and Windows have been tested.

### Usage ###
Grab the jrdiff.jar file from this repo, then:
```
java -jar jrdiff.jar signature <basis file> <signature output file>
java -jar jrdiff.jar delta <signature file> <new file> <delta output file>
java -jar jrdiff.jar patch <basis file> <delta file> <new output file>
```





*This software is licenced under LGPLv3.*
