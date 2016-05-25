package jrdiff;

import java.io.*;


public class FilePatcher {

    private final static boolean DEBUG = false;

    private File patchFile, newFile;
    private File baseFile;


    public FilePatcher(File baseFile, File newFile, File patchFile) {

        this.patchFile = patchFile;
        this.baseFile = baseFile;
        this.newFile = newFile;
    }

    public File startPatch() throws PatchException {


        DataInputStream patchInput;
        try {


            patchInput = new DataInputStream(new BufferedInputStream(new FileInputStream(patchFile)));

            BlockWriter blockWriter = new BlockWriter(newFile, baseFile, patchInput);


            int numOldBlocks = patchInput.readInt();
            int numNewBlocks = patchInput.readInt();
            int matchedBlockSize = patchInput.readInt();

            if (DEBUG)
                System.out.println("Got new bocks: " + numNewBlocks + ", old blocks: " + numOldBlocks + ", blocksize: " + matchedBlockSize);

            //write matching blocks
            for (int i = 0; i < numOldBlocks; i++) {

                int oldOffset = patchInput.readInt();
                int newOffset = patchInput.readInt();
                int blocks = patchInput.readInt();

                //create block object
                FileBlock fileBlock = new FileBlock(oldOffset, newOffset, matchedBlockSize * blocks);

                blockWriter.writeBlock(fileBlock);

                if (DEBUG) {
                    System.out.println("Wrote block: " + fileBlock);
                }

            }

            if (DEBUG)
                System.out.println("\n");

            //write new blocks
            for (int i = 0; i < numNewBlocks; i++) {
                int newOffset = patchInput.readInt();
                int length = patchInput.readInt();

                //create block object
                FileBlock fileBlock = new FileBlock(newOffset, length);

                //write
                blockWriter.writeBlock(fileBlock);

                if (DEBUG)
                    System.out.println("Wrote block: " + fileBlock);

            }

            patchInput.close();
            blockWriter.close();

            return blockWriter.getPatchedFile();


        } catch (FileNotFoundException e) {
            throw new PatchException("File not found when patching.", e);
        } catch (IOException e) {
            throw new PatchException("IO Error when patching", e);
        }

    }

    public File getPatchedFile() {
        return newFile;
    }
}
