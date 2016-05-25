package jrdiff;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class FingerPrinter {

    private static final int STRONG_HASH_LENGTH = 16;
    private static final String STRONG_HASH_TYPE = "MD5";

    public static final int MIN_BLOCK_SIZE = 704;
    public static final int MAX_BLOCK_SIZE = 16384;


    private final static boolean DEBUG = false;


    private int blockSize = -1;
    private File fpFile;

    public FingerPrinter(File fpFile) {
        this.fpFile = fpFile;
    }

    public FingerPrinter(File fpFile, int blockSize) {
        this.fpFile = fpFile;
        this.blockSize = blockSize;
    }

    private void calculateBlockSize(File baseFile) {

        long fileSize = baseFile.length();

        if (fileSize < MIN_BLOCK_SIZE)
            throw new RuntimeException("File too small, must be at least " + MIN_BLOCK_SIZE + " bytes.");

        int goodBlockSize = (int) fileSize / 10000;
        goodBlockSize = goodBlockSize < MIN_BLOCK_SIZE ? MIN_BLOCK_SIZE : goodBlockSize;
        goodBlockSize = goodBlockSize > MAX_BLOCK_SIZE ? MAX_BLOCK_SIZE : goodBlockSize;

        int remainderBytes = (int) fileSize % goodBlockSize;
        int numGoodBlocks = (int) fileSize / goodBlockSize;

        int extraBytes = (numGoodBlocks == 0) ? remainderBytes : remainderBytes / numGoodBlocks;

        blockSize = goodBlockSize + extraBytes;

        if (DEBUG)
            System.out.println("Using blocksize: " + blockSize);
    }

    public void create(File baseFile) throws IOException, NoSuchAlgorithmException {

        if (blockSize == -1)
            calculateBlockSize(baseFile);

        int numBlocks = (int) Math.ceil(baseFile.length() / blockSize);

        Checksum strongSum = new Checksum(STRONG_HASH_TYPE);
        RollingChecksum weakSum = new RollingChecksum();

        InputStream input = new BufferedInputStream(new FileInputStream(baseFile));
        DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fpFile)));

        output.writeInt(blockSize);
        output.writeInt(numBlocks);

        int numRead = 0;
        byte[] buf = new byte[blockSize];
        while ((numRead = ByteUtils.fillRead(input, buf)) > 0) {

            if (DEBUG && numRead < blockSize)
                System.out.println("FingerPrinter - Got block smaller than blocksize: " + numRead + " bytes.");

            strongSum.addData(buf, 0, numRead);
            weakSum.addInitialData(buf);

            byte[] strong = strongSum.getHash();
            int weak = weakSum.getChecksum();

            strongSum.reset();

            output.writeInt(weak);
            output.write(strong);

        }

        input.close();
        output.close();
    }

    public HashMap<Integer, ArrayList<FingerPrintBlock>> parse() throws IOException {

        HashMap<Integer, ArrayList<FingerPrintBlock>> hashes = new HashMap<Integer, ArrayList<FingerPrintBlock>>();
        DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(fpFile)));

        blockSize = input.readInt();

        int numBlocks = input.readInt();

        for (int i = 0; i < numBlocks; i++) {

            int weakHash = input.readInt();

            byte[] strongHash = new byte[STRONG_HASH_LENGTH];
            ByteUtils.fillRead(input, strongHash);

            FingerPrintBlock bh = new FingerPrintBlock(blockSize * i, strongHash);

            ArrayList<FingerPrintBlock> blockArr = hashes.get(weakHash);


            if (blockArr == null) {
                ArrayList<FingerPrintBlock> newBlockArr = new ArrayList<FingerPrintBlock>(3);
                newBlockArr.add(bh);
                hashes.put(weakHash, newBlockArr);
            } else
                blockArr.add(bh);

        }

        if (DEBUG)
            System.out.println("FingerPrinter.create() - Number of hashes: " + numBlocks + ", Weak Collisions Detected: " + (numBlocks - hashes.size()));

        return hashes;
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
}
