package jrdiff;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class PatchFactory {

    private final static boolean DEBUG = false;
    private static final int STRONG_HASH_LENGTH = 16;
    private static final String STRONG_HASH_TYPE = "MD5";
    private File fingerPrintFile;
    private File updatedFile;
    private int blockSize;
    private HashMap<Integer, FingerPrintBlock[]> hashes;
    private DataOutputStream patchOutput;
    private Checksum strongHasher;
    private ArrayList<OffsetPair> matchedList;
    private ArrayList<FileBlock> unmatchedList;
    private RandomAccessFile baseFile;
    private RollingChecksum weakHasher;
    private Checksum fileHasher;
    private File patchFile;

    public PatchFactory(File updatedFile, File fingerPrintFile, File patchFile) {

        this.updatedFile = updatedFile;
        this.fingerPrintFile = fingerPrintFile;
        this.patchFile = patchFile;

        hashes = new HashMap<Integer, FingerPrintBlock[]>();
        matchedList = new ArrayList<OffsetPair>();
        unmatchedList = new ArrayList<FileBlock>();

    }

    public void createPatch() throws IOException, NoSuchAlgorithmException {

        baseFile = new RandomAccessFile(updatedFile, "r");

        parseFingerPrintFile();

        //create patch file
        patchOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(patchFile)));

        //create hashers
        strongHasher = new Checksum(STRONG_HASH_TYPE);
        weakHasher = new RollingChecksum();

        if (DEBUG)
            System.out.println("PatchFactory - Finding Matching blocks");
        findMatches();

        if (DEBUG)
            System.out.println("PatchFactory - Creating new data blocks");
        createNewBlocks();

        if (DEBUG)
            System.out.println("PatchFactory - Writing Patch File");
        writePatchFile();

    }

    public File getPatchFile() {
        return patchFile;
    }

    public String getNewFileHash() {
        return fileHasher.getBase64Hash();
    }

    @SuppressWarnings("empty-statement")
    private void parseFingerPrintFile() throws IOException {

        DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(fingerPrintFile)));

        blockSize = input.readInt();

        int numBlocks = input.readInt();

        for (int i = 0; i < numBlocks; i++) {

            int weakHash = input.readInt();

            byte[] strongHash = new byte[STRONG_HASH_LENGTH];
            ByteUtils.fillRead(input, strongHash, strongHash.length);

            if (DEBUG)
                visualiseData("Read weak: " + weakHash + ", strong", strongHash);

            FingerPrintBlock bh = new FingerPrintBlock(blockSize * i, strongHash);

            FingerPrintBlock[] blockArr = hashes.get(weakHash);

            FingerPrintBlock[] newBlockArr;
            if (blockArr == null) {
                newBlockArr = new FingerPrintBlock[1];
                newBlockArr[0] = bh;
            } else {
                newBlockArr = new FingerPrintBlock[blockArr.length + 1];
                for (int iter = 0; iter < blockArr.length; newBlockArr[iter] = blockArr[iter++]) ;
                newBlockArr[blockArr.length] = bh;
            }

            hashes.put(weakHash, newBlockArr);


        }

        if (DEBUG) {
            System.out.println();
            System.out.println("Got " + numBlocks + " block checksums. MapSize: " + hashes.size() + ", weak collisions: " + (numBlocks - hashes.size()));
            System.out.println();
        }

        input.close();
    }

    private void findMatches() throws IOException, NoSuchAlgorithmException {

        //setup file hasher
        fileHasher = new Checksum(STRONG_HASH_TYPE);

        //initial bytes
        byte[] buffer = new byte[blockSize];
        int initalRead = ByteUtils.fillRead(baseFile, buffer, blockSize);

        if (initalRead != blockSize)
            throw new IOException("Cannot read inital " + blockSize + " bytes from base file");

        CircularByteList byteList = new CircularByteList(buffer, 0, blockSize);

        fileHasher.addData(buffer, 0, blockSize);
        weakHasher.addInitialData(buffer, 0, blockSize);

        //visualiseData("\tWeak processed: ", buffer);

        //check for initial match
        buffer = new byte[4096];//set standard buffersize
        int skipBytes = 0;
        int weakHash = weakHasher.getChecksum();
        if (hashes.containsKey(weakHash)) {


            if (DEBUG)
                System.out.println("Got weak match: " + weakHash);

            processWeakMatch(weakHash, blockSize, byteList);
            skipBytes = blockSize; //skip next block
        }

        //roll though file looking for matches
        int numRead = 0;
        int count = blockSize;
        while ((numRead = baseFile.read(buffer)) > 0)
            for (int i = 0; i < numRead; i++) {
                count++;

                fileHasher.addData(buffer[i]);
                weakHasher.update(buffer[i]);
                byteList.update(buffer[i]);

                //if there are bytes to skip then skip one and continue with loop
                if (skipBytes > 0)
                    skipBytes--;
                else {
                    //check hash
                    weakHash = weakHasher.getChecksum();
                    if (hashes.containsKey(weakHash)) {


                        if (DEBUG)
                            System.out.println("Got weak match: " + weakHash);

                        processWeakMatch(weakHash, count, byteList);
                        skipBytes = blockSize - 1; //skip next block

                    }
                }
            }

        //sort list of matched offsets
        Collections.sort(matchedList);

        if (DEBUG) {
            System.out.println();
            for (OffsetPair pair : matchedList)
                System.out.println("Matched: " + pair);
        }

    }

    private void processWeakMatch(int weakHash, int filePointer, CircularByteList byteList) throws IOException {

        int offset = filePointer - blockSize;

        //get block data
        byte[] data = byteList.getBytes();


        //strong hash block to make sure it matches
        strongHasher.addData(data);
        byte[] strongHash = strongHasher.getHash();
        strongHasher.reset();

        FingerPrintBlock[] blocks = hashes.get(weakHash);

        //compare all blocks
        for (FingerPrintBlock block : blocks) {

            if (ByteUtils.areArraysEqual(block.getHash(), strongHash)) {

                //check if adjacent to last matched block
                OffsetPair prevPair;
                if ((prevPair = isAdjacentBlock(offset, block.getOffset())) != null)
                    prevPair.incrementBlocks();
                else { //not adjacent to last block

                    OffsetPair pair = new OffsetPair(block.getOffset(), offset, 1);
                    matchedList.add(pair);

                }

                return; //match found, no need to check other hashes
            }
        }
        if (DEBUG)
            System.out.println("Got weak match but not strong match: " + offset);

    }

    private OffsetPair isAdjacentBlock(int newOffset, int oldOffset) {

        if (matchedList.size() == 0)
            return null;

        OffsetPair prevPair = matchedList.get(matchedList.size() - 1);
        int newEndOffset = prevPair.getNewOffset() + (blockSize * prevPair.getNumBlocks());
        int oldEndOffset = prevPair.getOldOffset() + (blockSize * prevPair.getNumBlocks());

        if (DEBUG)
            System.out.println("Check for adjacent block - PrevPair: " + prevPair + ", CurrentOffset: " + newOffset + ", Old Offset: " + oldOffset + ", IsAdjacent: " + (newEndOffset == newOffset && oldEndOffset == oldOffset));
        if (newEndOffset == newOffset && oldEndOffset == oldOffset)
            return prevPair;
        else
            return null;
    }

    private void createNewBlocks() {

        int startOffset = 0;

        //do bits between matched blocks
        for (OffsetPair match : matchedList) {

            //calc blocksize of new data block
            int unmatchedBlockSize = match.getNewOffset() - startOffset;

            //if blocksize 0 then no new block here so skip
            if (unmatchedBlockSize != 0) {
                if (DEBUG)
                    System.out.println("Unmatched Block: Offset " + startOffset + ", size: " + unmatchedBlockSize + ", Matched Offset: " + match.getNewOffset());

                //record block
                FileBlock block = new FileBlock(startOffset, unmatchedBlockSize);
                unmatchedList.add(block);
            }

            //calc next start
            startOffset = match.getNewOffset() + (blockSize * match.getNumBlocks());
        }

        //do bit of file after last matched block
        int unmatchedBlockSize = (int) updatedFile.length() - startOffset;
        if (unmatchedBlockSize > 0) {
            FileBlock block = new FileBlock(startOffset, unmatchedBlockSize);
            unmatchedList.add(block);

            if (DEBUG)
                System.out.println("END Unmatched Block: Offset " + startOffset + ", size: " + unmatchedBlockSize);

        } else if (DEBUG)
            System.out.println("No END block. End block size: " + unmatchedBlockSize);

    }

    private void writePatchFile() throws IOException {

        patchOutput.writeInt(matchedList.size());
        patchOutput.writeInt(unmatchedList.size());
        patchOutput.writeInt(blockSize);

        for (OffsetPair pair : matchedList) {

            patchOutput.writeInt(pair.getOldOffset());
            patchOutput.writeInt(pair.getNewOffset());
            patchOutput.writeInt(pair.getNumBlocks());

            if (DEBUG)
                System.out.println("Wrote: " + pair);

        }

        byte[] buffer = new byte[2048];
        for (FileBlock block : unmatchedList) {

            int offset = block.getOffset();
            int size = block.getSize();

            patchOutput.writeInt(offset);
            patchOutput.writeInt(size);

            //position file pointer
            baseFile.seek(offset);

            int totalRead = 0;
            while (totalRead < size) {

                //calculate number of bytes to read
                int amountToRead = (size - totalRead) > buffer.length ? buffer.length : (size - totalRead);
                //read
                int numRead = baseFile.read(buffer, 0, amountToRead);
                //write data
                patchOutput.write(buffer, 0, numRead);
                //update number of bytes read total
                totalRead += numRead;
            }

            if (DEBUG)
                System.out.println("Wrote: Offset - " + offset + ", Size: " + size);


        }

        patchOutput.flush();
        patchOutput.close();

        if (DEBUG) {
            System.out.println("Matched Blocks: " + matchedList.size() + ", Unmatched blocks: " + unmatchedList.size());

            int matchedSize = 0;
            for (OffsetPair pair : matchedList)
                matchedSize += pair.getNumBlocks() * blockSize;

            int unmatchedSize = 0;
            for (FileBlock block : unmatchedList)
                unmatchedSize += block.getSize();

            System.out.println("Matched Bytes: " + matchedSize + ", Unmatched Bytes: " + unmatchedSize);
        }


    }

    private void visualiseData(String prepend, byte[] data) {

        System.out.print(prepend + ": ");
        for (byte crumb : data)
            System.out.print(crumb + ",");
        System.out.println();

    }

    private class FileBlock {

        private int offset;
        private int size;

        public FileBlock(int offset, int size) {
            this.offset = offset;
            this.size = size;
        }

        public int getOffset() {
            return offset;
        }

        public int getSize() {
            return size;
        }
    }

    private class FingerPrintBlock {

        private int offset;
        private byte[] hash;

        public FingerPrintBlock(int offset, byte[] hash) {
            this.offset = offset;
            this.hash = hash;
        }

        public byte[] getHash() {
            return hash;
        }

        public int getOffset() {
            return offset;
        }
    }

    private class OffsetPair implements Comparable<OffsetPair> {

        private int oldOffset, newOffset;
        private int numBlocks;

        public OffsetPair(int oldOffset, int newOffset, int numBlocks) {
            this.oldOffset = oldOffset;
            this.newOffset = newOffset;
            this.numBlocks = numBlocks;
        }

        public int getNewOffset() {
            return newOffset;
        }

        public int getOldOffset() {
            return oldOffset;
        }

        public int getNumBlocks() {
            return numBlocks;
        }

        private void incrementBlocks() {
            numBlocks++;
        }

        @Override
        public int compareTo(OffsetPair compInt) {
            return ((Integer) newOffset).compareTo(compInt.getNewOffset());
        }

        @Override
        public String toString() {
            return "Old Offset: " + oldOffset + ", New Offset: " + newOffset + ", Blocks: " + numBlocks;
        }
    }
}
