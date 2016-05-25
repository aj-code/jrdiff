package jrdiff;

public class FileBlock {


    private boolean newData;
    private int oldOffset;
    private int newOffset;
    private int length;


    /**
     * Constructor to be used with new data from client.
     *
     * @param offset
     * @param length
     * @param data
     */
    public FileBlock(int newOffset, int length) {

        newData = true;

        this.newOffset = newOffset;
        this.length = length;


    }


    public FileBlock(int oldOffset, int newOffset, int length) {

        this.oldOffset = oldOffset;
        this.newOffset = newOffset;
        this.length = length;

    }


    public int getLength() {
        return length;
    }


    public boolean hasNewData() {
        return newData;
    }


    public int getNewOffset() {
        return newOffset;
    }


    public int getOldOffset() {
        return oldOffset;
    }

    @Override
    public String toString() {

        String str = "FileBlock: NewData: " + newData + ", OldOffset: " + oldOffset + ", NewOffset: " + newOffset + ", Length: " + length;
        return str;
    }


}
