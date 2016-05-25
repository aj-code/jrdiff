package jrdiff;


public class RollingChecksum {


    //sums
    private int firstHalf;
    private int secondHalf;

    //positions
    private int start;
    private int end;

    //data block
    private byte[] block;


    public RollingChecksum() {
        firstHalf = secondHalf = 0;
        start = 0;
    }

    /**
     * Return the value of the currently computed checksum.
     */
    public int getChecksum() {
        return (firstHalf & 0xffff) | (secondHalf << 16);
    }

    /**
     * Reset the checksum.
     */
    public void reset() {
        start = 0;
        firstHalf = secondHalf = 0;
        end = 0;
    }

    /**
     * Update checksum with new byte
     */
    public void update(byte data) {
        firstHalf -= block[start];
        secondHalf -= end * block[start];
        firstHalf += data;
        secondHalf += firstHalf;
        block[start] = data;
        start++;
        if (start == end)
            start = 0;
    }

    /**
     * Update the checksum by trimming off a byte only, not adding anything. For end of file.
     */
    public void update() {
        firstHalf -= block[start % block.length];
        secondHalf -= end * (block[start % block.length]);
        start++;
        end--;
    }

    /**
     * Add initial data, copying array.
     */
    public void addInitialData(byte[] buf, int off, int len) {
        block = new byte[len];
        System.arraycopy(buf, off, block, 0, len);

        addInitialData(buf);
    }

    /**
     * Adds initial data working directly on provided array.
     */
    public void addInitialData(byte[] block) {
        reset();
        end = block.length;
        int i;

        for (i = 0; i < block.length - 4; i += 4) {
            secondHalf += 4 * (firstHalf + block[i]) + 3 * block[i + 1] + 2 * block[i + 2]
                    + block[i + 3];
            firstHalf += block[i] + block[i + 1] + block[i + 2] + block[i + 3];
        }
        for (; i < block.length; i++) {
            firstHalf += block[i];
            secondHalf += firstHalf;
        }
    }

}
