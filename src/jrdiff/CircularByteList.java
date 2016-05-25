package jrdiff;


/**
 * @author aj-code
 */
public class CircularByteList {

    private byte[] data, newData;
    private int marker;

    public CircularByteList(byte[] data, int offset, int length) {

        this.data = new byte[length];
        this.newData = new byte[length];
        this.marker = 0;

        System.arraycopy(data, offset, this.data, 0, length);

    }

    public CircularByteList(byte[] data) {
        this(data, 0, data.length);
    }

    public void update(byte data) {

        this.data[marker] = data;

        marker++;
        if (marker > this.data.length - 1) {
            marker = 0;
        }

    }

    public byte[] getBytes() {

        int marker = this.marker;
        for (int i = 0; i < data.length; i++) {
            newData[i] = data[marker];

            marker++;
            if (marker > this.data.length - 1) {
                marker = 0;
            }
        }

        return newData;

    }

}
