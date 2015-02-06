VolGetter
=========
HathiTrust Data API Custom Client Consumer
==========================================

    /**
     * Reads volume ids from given list in file,
     * gets the aggregate zip files for the volumes,
     * and writes them in a HathiTrust-style pairtree structure
     * for compatibility with related systems.
     *
     * Designed specifically to work with the client for HathiTrust's Data API
     * at https://github.com/mfall3/htdapiclient-getvol
     *
	 * Usage is: java -jar volgetter.jar volumeListFile destinationRoot serviceEndpoint
     * 
	 * @param args
     *   arg[0] -> volumeListFile - the file containing a list of volume ids, one on each line
     *   arg[1] -> destinationRoot - the target root directory, must exist or be valid to create
     *   arg[2] -> serviceEndpoint - the url for the api client service
     */

