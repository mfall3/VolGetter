import gov.loc.repository.pairtree.Pairtree;
import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

public class VolGetter {

    /**
     * Reads volume ids from given list in file,
     * gets the aggregate zip files for the volumes,
     * and writes them in a HathiTrust-style pairtree structure
     * for compatibility with related systems.
     * 
     * Designed specifically to work with the client for HathiTrust's Data API
     * at https://github.com/mfall3/htdapiclient-getvol
     *
     * @param args arg[0] -> volumeListFile - the file containing a list of volume ids, one on each line
     *             arg[1] -> destinationRoot - the target root directory, must exist or be valid to create
     *             arg[2] -> serviceEndpoint - the url for the api client service
     */

    public static void main(String[] args) throws IOException {

        //check arguments
        if (args.length != 3)
            exitWithMessage("Proper Usage is: java -jar volgetter.jar volumeListFile destinationRoot serviceEndpoint");

        String volumeFilename = args[0];  // List of HT volume IDs
        String destRoot = args[1];    // Root of destination pairtree, does not need to exist
        String serviceEndpoint = args[2]; // URL for the API client service

        File volumeFile = new File(volumeFilename);
        if (!volumeFile.exists())
            exitWithMessage("Cannot open " + volumeFilename + ". Proper Usage is: java -jar volgetter.jar volumeListFile destinationRoot serviceEndpoint.");

        File destRootDir = new File(destRoot);
        if (!destRootDir.exists()) {
            System.out.println("creating directory: " + destRoot);
            destRootDir.mkdir();
            if (!destRootDir.exists())
                exitWithMessage("Unable to find or create directory " + destRoot + ". Proper Usage is: java -jar volgetter.jar volumeListFile destinationRoot serviceEndpoint.");
        }

        String exceptionFilename = destRoot + File.separator + "exceptions.txt";
        File exceptionFile = new File(exceptionFilename);
        FileWriter fw = null;

        // Read the volume list
        List<String> volumeList = FileUtils.readLines(volumeFile);

        try {

            fw = new FileWriter(exceptionFile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);

            for (String volumeId : volumeList) {
                volumeId = URLDecoder.decode(volumeId, "UTF-8");
                Pairtree pt = new Pairtree();

                // Parse the volume ID
                String sourcePart = volumeId.substring(0, volumeId.indexOf("."));
                String volumePart = volumeId.substring(volumeId.indexOf(".") + 1, volumeId.length());
                String uncleanId = pt.uncleanId(volumePart);
                String path = pt.mapToPPath(uncleanId);
                String cleanId = pt.cleanId(volumePart);

                // Path to the destination volume directory
                String destDir = destRoot + File.separator + sourcePart
                        + File.separator + "pairtree_root"
                        + File.separator + path;

                String destVolume = destDir + File.separator + cleanId;

                System.out.println("Downloading " + volumeId + " to " + destDir);
                try {

                    File downloadedFile = new File(destVolume);
                    writeHtAggregate(volumeId, destDir, destVolume, serviceEndpoint);


                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(volumeId + " not found.");
                    bw.write(volumeId + " not found.\n");
                }
            }
            bw.close();
            System.out.println("Done");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void exitWithMessage(String m) {
        System.out.println(m);
        System.exit(0);
    }

    /**
     * Call HathiTrust Data API to get aggregate zip and save response body to a file.
     *
     * @throws IOException
     */
    private static boolean writeHtAggregate(String volumeId, String destDir, String destVolume, String serviceEndpoint) throws IOException {

        new File(destDir).mkdirs();
        File to = new File(destVolume);

        String url = serviceEndpoint + "?id=" + volumeId;
        URL from = new URL(url);

        BufferedInputStream urlin = null;
        BufferedOutputStream fout = null;

        HttpURLConnection con = (HttpURLConnection) from.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        try {
            int bufSize = 8 * 1024;
            urlin = new BufferedInputStream(
                    from.openConnection().getInputStream(),
                    bufSize);
            fout = new BufferedOutputStream(new FileOutputStream(to), bufSize);
            copyPipe(urlin, fout, bufSize);
        } catch (IOException ioex) {
            ioex.printStackTrace();
            return false;
        } catch (SecurityException sx) {
            sx.printStackTrace();
            return false;
        } finally {
            if (urlin != null) {
                try {
                    urlin.close();
                } catch (IOException cioex) {
                    cioex.printStackTrace();
                }
            }
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException cioex) {
                    cioex.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
     * Reads data from the input and writes it to the output, until the end of the input
     * stream.
     *
     * @param in
     * @param out
     * @param bufSizeHint
     * @throws IOException
     */
    public static void copyPipe(InputStream in, OutputStream out, int bufSizeHint)
            throws IOException {
        int read = -1;
        byte[] buf = new byte[bufSizeHint];
        while ((read = in.read(buf, 0, bufSizeHint)) >= 0) {
            out.write(buf, 0, read);
        }
        out.flush();
    }

}
