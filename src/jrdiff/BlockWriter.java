package jrdiff;

import java.io.*;
import java.security.NoSuchAlgorithmException;


public class BlockWriter {

    private File newFile;
    private RandomAccessFile newRandFile, baseRandFile;
    byte[] dataBuffer;
    private InputStream patchInput;

    public BlockWriter(File newFile, File baseFile, InputStream patchInput) throws FileNotFoundException {

        this.newFile = newFile;
        this.newRandFile = new RandomAccessFile(newFile, "rw");
        this.baseRandFile = new RandomAccessFile(baseFile, "r");
        this.patchInput = patchInput;
        this.dataBuffer = new byte[2048];

    }


    public void writeBlock(FileBlock block) throws IOException {

        if (block.hasNewData()) {
            writeBlock(block.getNewOffset(), block.getLength());
        } else {
            copyBlock(block.getOldOffset(), block.getNewOffset(), block.getLength());
        }

    }

    private void copyBlock(int baseOffset, int newOffset, int length) throws IOException {

        //place pointers
        baseRandFile.seek(baseOffset);
        newRandFile.seek(newOffset);

        int leftToRead = length;
        while (leftToRead > 0) {

            //read bytes remaining up to buffer length
            int numToRead = leftToRead < dataBuffer.length ? leftToRead : dataBuffer.length;
            //read
            int numRead = baseRandFile.read(dataBuffer, 0, numToRead);
            //write
            newRandFile.write(dataBuffer, 0, numRead);

            //update number of bytes left to read
            leftToRead -= numRead;

        }

    }

    private void writeBlock(int newOffset, int length) throws IOException {

        newRandFile.seek(newOffset);

        int leftToRead = length;
        while (leftToRead > 0) {

            //read bytes remaining up to buffer length
            int numToRead = leftToRead < dataBuffer.length ? leftToRead : dataBuffer.length;
            //read
            int numRead = patchInput.read(dataBuffer, 0, numToRead);
            //write
            newRandFile.write(dataBuffer, 0, numRead);

            //update number of bytes left to read
            leftToRead -= numRead;

        }

    }

    public boolean verifyFile(String md5Hash) throws IOException {

        //reset file pointer
        newRandFile.seek(0);

        //create hasher
        Checksum hasher;
        try {
            hasher = new Checksum("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Cannot verify file.", e);
        }

        //feed data to hasher
        int numRead = 0;
        byte[] dataBuff = new byte[1024];
        while ((numRead = newRandFile.read(dataBuff)) > 0) {
            hasher.addData(dataBuff, 0, numRead);
        }

        String realHash = hasher.getBase64Hash();//.getHexHash();
        //System.out.println("BlockWriter: Given Hash: " + md5Hash + ", calc hash: " + realHash);

        //check sums match
        if (realHash.equals(md5Hash)) {
            return true;
        } else {
            return false;
        }

    }

    public File getPatchedFile() {
        return newFile;
    }

    public void close() throws IOException {

        newRandFile.close();
        baseRandFile.close();

    }
    /*
    public static void main(String[] args) throws FileNotFoundException, IOException {
		
		
		
		File baseFile = new File("base.txt");
		File newFile = new File("final.txt");
		
		DiffFileCreator creator = new DiffFileCreator(newFile, baseFile);
		
		
		byte[] data = new byte[10];
		new FileInputStream("data.txt").read(data);
		
		
		FileBlock newBlock = new FileBlock(10, 10, data);
		
		creator.writeBlock(newBlock);
		
		
		
		FileBlock oldBlock = new FileBlock(0, 0, 10);
		
		creator.writeBlock(oldBlock);
		
		
		creator.cleanUp();
		
		
		System.out.println("Printing file contents: " + newFile.length());
		data = new byte[(int)newFile.length()];
		int read = new FileInputStream(newFile).read(data);
		
		System.out.println("Num Read: " + read);
		
		for (byte chunk : data) {
			System.out.print(((char)chunk) + ",");
		}
		
	}*/
}
