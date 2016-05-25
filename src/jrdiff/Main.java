package jrdiff;

import java.io.File;

/**
 * @author aj-code
 */
public class Main {

    private static void printUsage() {
        System.out.println("Usage:\tjava -jar jrdiff.jar signature <basis file> <signature output file>\n" +
                "\tjava -jar jrdiff.jar delta <signature file> <new file> <delta output file>\n" +
                "\tjava -jar jrdiff.jar patch <basis file> <delta file> <new output file>");
    }

    private static void run(String[] args) throws Exception {

        if (args.length == 0) {
            printUsage();
            return;
        }

        String mode = args[0];

        long start = System.currentTimeMillis();

        if (mode.equalsIgnoreCase("signature") && args.length == 3) {

            File baseFile = new File(args[1]);
            File sigFile = new File(args[2]);

            if (!sigFile.exists() && !sigFile.createNewFile()) {
                System.err.println("Cannot create file: " + sigFile);
                return;
            }

            if (!baseFile.canRead()) {
                System.err.println("Cannot read file: " + baseFile);
                return;
            }

            if (baseFile.length() < FingerPrinter.MIN_BLOCK_SIZE) {
                System.err.println("File too small, must be at least " + FingerPrinter.MIN_BLOCK_SIZE + " bytes.");
                return;
            }

            FingerPrinter fp = new FingerPrinter(sigFile);
            fp.create(baseFile);

            System.out.println("Signature generation complete in " + (System.currentTimeMillis() - start) + "ms.");

        } else if (mode.equalsIgnoreCase("delta") && args.length == 4) {

            File sigFile = new File(args[1]);
            File newFile = new File(args[2]);
            File deltaFile = new File(args[3]);

            if (!sigFile.canRead()) {
                System.err.println("Cannot read file: " + sigFile);
                return;
            }

            if (!newFile.canRead()) {
                System.err.println("Cannot read file: " + newFile);
                return;
            }

            if (!deltaFile.exists() && !deltaFile.createNewFile()) {
                System.err.println("Cannot create file: " + deltaFile);
                return;
            }


            PatchFactory factory = new PatchFactory(newFile, sigFile, deltaFile);
            factory.createPatch();

            System.out.println("Delta generation complete in " + (System.currentTimeMillis() - start) + "ms.");


        } else if (mode.equalsIgnoreCase("patch") && args.length == 4) {

            File oldFile = new File(args[1]);
            File deltaFile = new File(args[2]);
            File newFile = new File(args[3]);

            if (!deltaFile.canRead()) {
                System.err.println("Cannot read file: " + deltaFile);
                return;
            }

            if (!oldFile.canRead()) {
                System.err.println("Cannot read file: " + oldFile);
                return;
            }

            if (!newFile.exists() && !newFile.createNewFile()) {
                System.err.println("Cannot create file: " + newFile);
                return;
            }

            FilePatcher patcher = new FilePatcher(oldFile, newFile, deltaFile);
            patcher.startPatch();


            System.out.println("File patch complete in " + (System.currentTimeMillis() - start) + "ms.");

        } else {
            System.err.println("Invalid command.");
            printUsage();
        }


    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            run(args);
        } catch (Exception e) {
            System.err.println("There has been a fatal error: ");
            e.printStackTrace();
        }

    }
}