
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

public class OSMParser {

    private static TreeSet<Long> neededNodesIds = new TreeSet<Long>();
    private static TreeSet<Long> neededWaysIds = new TreeSet<Long>();

    private static TreeMap<Long, Long> allIds = new TreeMap<Long, Long>();

    private static TreeMap<Double, TreeMap<Double, Double>> nodeGps = new TreeMap<Double, TreeMap<Double, Double>>();

    private static TreeMap<Long, TreeMap<Long, Long>> allWayParts = new TreeMap<Long, TreeMap<Long, Long>>();

    private static String[] highwayTypes = {"motorway", "motorway_link", "motorway_junction", "trunk", "trunk_link",
        "primary", "primary_link", "primary_trunk", "secondary", "secondary_link",
        "tertiary", "tertiary_link", "unclassified", "unsurfaced", "track",
        "residential", "living_street", "service", "road", "raceway",
        "xxx", "xxx", "xxx", "xxx", "xxx",
        "xxx", "xxx", "xxx", "xxx", "xxx", //intentionally left blank
        "steps", "bridleway", "cycleway", "footway", "pedestrian",
        "bus_guideway", "path", "xxx", "xxx", "xxx",
        "xxx", "xxx", "xxx", "xxx", "xxx",
        "xxx", "xxx", "xxx", "xxx", "xxx",};

    private static final int DEFAULT = 0x00;
    private static final int CAR = 0x01;

    private static int meansOfTransport = OSMParser.DEFAULT;

    private static int carPermission; //0 = notallowed, 1 = restricted, 2 = allowed

    private static String highway = "";
    private static String motorcar = "";

    private static double minlat = Double.MAX_VALUE;
    private static double maxlat = -Double.MAX_VALUE;
    private static double minlon = Double.MAX_VALUE;
    private static double maxlon = -Double.MAX_VALUE;

    public static Long newID = 1000000L;

    public static void main(String[] args) {

        // check args
        Vector<String> gpsFiles = new Vector<String>();
        String osmInFile = "";
        String osmOutFile = "";

        Vector<Double> lats = new Vector<Double>();
        Vector<Double> lons = new Vector<Double>();

        double disOffset = 0.01;

        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-g")) {
                    i++;
                    gpsFiles.add(args[i]);
                } else if (args[i].equals("-oi")) {
                    i++;
                    osmInFile = args[i];
                } else if (args[i].equals("-oo")) {
                    i++;
                    osmOutFile = args[i];
                } else if (args[i].equals("-d")) {
                    i++;
                    disOffset = Double.parseDouble(args[i]);
                } else if (args[i].equals("-e")) {
                    i++;
                    lats.add(Double.parseDouble(args[i]));
                    i++;
                    lons.add(Double.parseDouble(args[i]));
                    i++;
                    lats.add(Double.parseDouble(args[i]));
                    i++;
                    lons.add(Double.parseDouble(args[i]));
                }
            }
        } catch (Exception e) {
            printParameterInfo();
            System.exit(-1);
        }

        if (osmInFile == "" || osmOutFile == "") {
            printParameterInfo();
            System.exit(-1);
        }

        if (lats.size() != 0 && lats.size() != 2) {
            printParameterInfo();
            System.exit(-1);
        }
		// args are OK!

        // read GPS-Traces
        for (int i = 0; i < gpsFiles.size(); i++) {
            scanGPSFile(gpsFiles.get(i));
        }

        // set map boundaries for size filter
        if (gpsFiles.size() == 0 && lats.size() == 0) {
            OSMParser.minlat = -Double.MAX_VALUE;
            OSMParser.maxlat = Double.MAX_VALUE;
            OSMParser.minlon = -Double.MAX_VALUE;
            OSMParser.maxlon = Double.MAX_VALUE;
        } else if (lats.size() != 0) {
            for (Double lat : lats) {
                if (lat < OSMParser.minlat) {
                    OSMParser.minlat = lat;
                }
                if (OSMParser.maxlat < lat) {
                    OSMParser.maxlat = lat;
                }
            }
            for (Double lon : lons) {
                if (lon < OSMParser.minlon) {
                    OSMParser.minlon = lon;
                }
                if (OSMParser.maxlon < lon) {
                    OSMParser.maxlon = lon;
                }
            }
        } else {
            OSMParser.minlat = OSMParser.minlat - disOffset;
            OSMParser.maxlat = OSMParser.maxlat + disOffset;
            OSMParser.minlon = OSMParser.minlon - disOffset;
            OSMParser.maxlon = OSMParser.maxlon + disOffset;
        }

        // start parsing
        OSMParser.doWork(osmInFile, osmOutFile);

    }

    /**
     * print description of args for user
     */
    public static void printParameterInfo() {
        System.out.println("");
        System.out.println("Parameter: ");
        System.out.println(" -g = Path of gps-file (multi)");
        System.out.println(" -oi = Path of Osm-Input-file (required)");
        System.out.println(" -oo = Path of Osm-Output-file (required)");
        System.out.println(" -d = gps offset in (gps) degrees (default: 0.01)");
        System.out.println(" -e = Lat Lon Lat Lon  (GPS corners (degree) for map-filter)");

        System.out.println("");
    }

    /**
     * read the GPS Trace file and check the max / min GPS coordinates for map
     * boundaries for size filter
     *
     * @param FilePathGPS path of GPS Trace file
     */
    public static void scanGPSFile(String FilePathGPS) {

        try {
            BufferedReader bReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(FilePathGPS)), "UTF-8"));

            if (FilePathGPS.endsWith(".txt")) {
                String line = bReader.readLine();
                while (line != null) {

                    if (line.startsWith("#") == false) {
                        String[] lines = line.split(",");

                        double d = Double.parseDouble(lines[1]);

                        if (d < minlat) {
                            minlat = d;
                        }

                        if (maxlat < d) {
                            maxlat = d;
                        }

                        d = Double.parseDouble(lines[2]);

                        if (d < minlon) {
                            minlon = d;
                        }

                        if (maxlon < d) {
                            maxlon = d;
                        }

                    }

                    line = bReader.readLine();
                }
            }

            if (FilePathGPS.endsWith(".log")) {
                String line = bReader.readLine();
                while (line != null) {
                    if (line.contains("\"lat\":") && line.contains("\"lon\":")) {
                        String[] lines = line.split("\"lat\":", 2);

                        lines = lines[1].split(",\"lon\":", 2);

                        double lat = Double.parseDouble(lines[0]);

                        lines = lines[1].split(",", 2);

                        double lon = Double.parseDouble(lines[0]);

                        if (lat < minlat) {
                            minlat = lat;
                        }

                        if (maxlat < lat) {
                            maxlat = lat;
                        }

                        if (lon < minlon) {
                            minlon = lon;
                        }

                        if (maxlon < lon) {
                            maxlon = lon;
                        }
                    }

                    line = bReader.readLine();
                }
            }

            bReader.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }
    }

    /**
     * search after "used" id and save them in allIds
     *
     * @param line XML line from OSM file
     */
    public static void scanIds(String line) {

        if (line != null) {
            String lines[] = line.split("id=\"");

            for (int i = 0; i < lines.length; i++) {

                line = lines[i];
                line = line.split("\"")[0];

                try {
                    Long l = Long.parseLong(line);
                    allIds.put(l, l);
                } catch (Exception e) {
                    // no id
                }
            }
        }
    }

    /**
     * create new unique id
     *
     * @return new unique id
     */
    public static Long getNewId() {

        int i;
        for (i = 0; i < 100000000; i++) {
            if (allIds.containsKey(newID)) {
                newID++;
            } else {
                allIds.put(newID, newID);
                return newID;
            }
        }

        System.out.println("Error: getNewId: no new ID after " + i);
        System.exit(-1);

        return -1L;
    }

    /**
     * check if GPS coordinates of nGps are unique if not, create new node with
     * unique GPS coordinates
     *
     * @param nGps node to be checked
     * @return node with unique GPS coordinates
     */
    public static nodeGps node_CheckCreateUniqueGps(nodeGps nGps) {

        TreeMap<Double, Double> latMap = nodeGps.get(nGps.lat);

        if (latMap == null) {
            latMap = new TreeMap<Double, Double>();
            latMap.put(nGps.lon, nGps.lon);

            nodeGps.put(nGps.lat, latMap);
        } else {
            Double lonD = latMap.get(nGps.lon);

            if (lonD == null) {
                latMap.put(nGps.lon, nGps.lon);
            } else {
                nodeGps newnGps = new nodeGps();
                newnGps.lon = nGps.lon;
                newnGps.lat = nGps.lat + 0.00000001;
                return node_CheckCreateUniqueGps(newnGps);
            }
        }

        return nGps;
    }

    /**
     * @param from node id
     * @param to node id
     * @return true if wayPart beetwen from and to is new, else false
     */
    public static boolean isWayPartNew(Long from, Long to) {
        TreeMap<Long, Long> fromMap = allWayParts.get(from);

        if (fromMap == null) {
            fromMap = new TreeMap<Long, Long>();
            fromMap.put(to, to);

            allWayParts.put(from, fromMap);
            return true;
        } else {
            Long toL = fromMap.get(to);

            if (toL == null) {
                fromMap.put(to, to);
                return true;
            }
        }

        return false;
    }

    /**
     * parse the OSM file read OSM file 3 times
     *
     * @param FilePathIn path of osm file for reading
     * @param FilePathOutpath of new osm file for creating
     */
    public static void doWork(String FilePathIn, String FilePathOut) {

        BufferedReader bReader;

        FileReader fReader;
		//FileWriter fWriter;;

        long lineNR = 0;

        long lineCount;

        try {
            String line = "";

            lineNR = 0;

			// ##############################################
            // ##############################################
            // 1. Reading of osm file
            // check the number of lines
            fReader = new FileReader(FilePathIn);
            bReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(FilePathIn)), "UTF-8"));
            line = bReader.readLine();
            while (line != null) {
                lineNR++;
                line = bReader.readLine();
            }
            bReader.close();
            fReader.close();

            lineCount = lineNR;

            System.out.println("File has " + lineCount + " lines");

			// ##############################################
            // ##############################################
            // 2. Reading of osm file
            // search all already used ids
            // search and check ways, if they may be used for cars (save the way id)
            // save needed node ids for ways for cars
            fReader = new FileReader(FilePathIn);
            bReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(FilePathIn)), "UTF-8"));

            lineNR = 0;
            line = bReader.readLine();
            scanIds(line);
            lineNR++;

            while (line != null) {

                // search all already used ids
                scanIds(line);

                line = line.trim();

				// start of reading way tag
                // search and check way, if it may be used for cars
                // read all required properties of way 
                if (line.startsWith("<way id=\"")) {

                    meansOfTransport = OSMParser.DEFAULT;

					//line = line.replace("<way id=\"", "").split("\"")[0];
                    Long wayID = Long.parseLong(line.replace("<way id=\"", "").split("\"")[0]);

                    TreeMap<Integer, Long> neededNodesIds_temp = new TreeMap<Integer, Long>();
                    highway = "";
                    motorcar = "";
                    boolean building_yes = false;

                    line = bReader.readLine();
                    scanIds(line);
                    lineNR++;
                    if (lineNR % 1000000 == 0) {
                        System.out.println("scanning file: " + ((lineNR * 100) / lineCount) + " % ");
                    }
                    line = line.trim();

                    while (line.equals("</way>") == false) {

                        if (line.startsWith("<nd ref=\"") && line.endsWith("\"/>")) {
                            line = line.replace("<nd ref=\"", "").replace("\"/>", "");

                            long l = Long.parseLong(line);

                            neededNodesIds_temp.put(neededNodesIds_temp.size(), l);
                        } else if (line.startsWith("<tag k=\"highway\" v=\"") && line.endsWith("\"/>")) {

                            highway = line.replace("<tag k=\"highway\" v=\"", "").replace("\"/>", "");

                        } else if (line.startsWith("<tag k=\"motorcar\" v=\"") && line.endsWith("\"/>")) {

                            motorcar = line.replace("<tag k=\"motorcar\" v=\"", "").replace("\"/>", "");

                        } else if (line.equals("<tag k=\"building\" v=\"yes\"/>")) {

                            building_yes = true;

                        }

                        line = bReader.readLine();
                        scanIds(line);
                        lineNR++;
                        if (lineNR % 1000000 == 0) {
                            System.out.println("scanning file: " + ((lineNR * 100) / lineCount) + " % ");
                        }
                        line = line.trim();
                    }

                    if (building_yes == false) {

                        set_meansOfTransport(wayID);

                        // check ways, if they may be used for cars
                        if (getMeansOfTransportPermission(OSMParser.CAR)) {

                            // save the way id of way for cars
                            neededWaysIds.add(wayID);

                            // save needed node ids for ways for cars
                            for (int i = 0; i < neededNodesIds_temp.size(); i++) {

                                long l = neededNodesIds_temp.get(i);

                                neededNodesIds.add(l);

                            }

                        }

                    }

                }

                line = bReader.readLine();
                lineNR++;
                if (lineNR % 1000000 == 0) {
                    System.out.println("scanning file: " + ((lineNR * 100) / lineCount) + " % ");
                }
            }

            bReader.close();
            fReader.close();

			// ##############################################
            // ##############################################
            // 3. Reading of osm file
            // create new OSM file:
            // check and make GPS coordinates of nodes unique
            // split/copy way with n wayParts to n new ways
            // check ony_way property : if oneway is "-1" turn over the way
            fReader = new FileReader(FilePathIn);

            bReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(FilePathIn)), "UTF-8"));

            BufferedWriter bWriter;
            bWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(FilePathOut)), "UTF-8"));

            lineNR = 0;

            line = bReader.readLine();

            String tline;

            while (line != null) {

                tline = line.trim();

				// start of read node tag
                // write this node tag only if it is needed (info of 2. reading: neededNodesIds)
                if (tline.startsWith("<node id=\"")) {

                    String s = tline.replace("<node id=\"", "").split("\"")[0];

                    long id = Long.parseLong(s);

                    boolean write = neededNodesIds.contains(id);

                    if (write) {
                        nodeGps nGps = new nodeGps();

                        String oldLat = tline.split(" lat=\"")[1].split("\"")[0];
                        nGps.lat = Double.parseDouble(oldLat);
                        String oldLon = tline.split(" lon=\"")[1].split("\"")[0];
                        nGps.lon = Double.parseDouble(oldLon);

                        nodeGps nGpsNew = nGps;

                        if (minlat <= nGps.lat && nGps.lat <= maxlat && minlon <= nGps.lon && nGps.lon <= maxlon) {
                            // check and make GPS coordinates of nodes unique
                            nGpsNew = node_CheckCreateUniqueGps(nGps);
                        } else {
                            neededNodesIds.remove(id);
                            write = false;
                        }

                        if (nGpsNew.lat != nGps.lat || nGpsNew.lon != nGps.lon) {
                            System.out.println("Info: Change gps of node: " + id + " : " + nGps.lat + " " + nGps.lon + " -> " + nGpsNew.lat + " " + nGpsNew.lon);

                            oldLat = " lat=\"" + oldLat;
                            String newlat = " lat=\"" + nGpsNew.lat;

                            oldLon = " lon=\"" + oldLon;
                            String newlon = " lon=\"" + nGpsNew.lon;

                            line = line.replace(oldLat, newlat);
                            line = line.replace(oldLon, newlon);

                            tline = line.trim();
                        }
                    }

                    if (tline.endsWith("/>")) {
                        if (write) {
                            bWriter.write(line + System.lineSeparator());
                        }
                    } else if (tline.endsWith(">")) {

                        if (write) {
                            bWriter.write(line + System.lineSeparator());
                        }

                        line = bReader.readLine();
                        lineNR++;
                        if (lineNR % 1000000 == 0) {
                            System.out.println("writing new file: " + ((lineNR * 100) / lineCount) + " % ");
                        }
                        tline = line.trim();

                        while (tline.equals("</node>") == false) {
                            if (write) {
                                bWriter.write(line + System.lineSeparator());
                            }
                            line = bReader.readLine();
                            lineNR++;
                            if (lineNR % 1000000 == 0) {
                                System.out.println("writing new file: " + ((lineNR * 100) / lineCount) + " % ");
                            }
                            tline = line.trim();
                        }

                        if (write) {
                            bWriter.write(line + System.lineSeparator());
                        }

                    } else {
                        System.out.println("Error: Ende von Note: " + id);
                    }

                } // start of read way tag
                // write this way tag only if it is needed (info of 2. reading: neededWaysIds)
                else if (tline.startsWith("<way id=\"")) {

                    // way1List: save all lines from "start way tag" until ref-Tag
                    LinkedList<String> way1List = new LinkedList<String>();

                    way1List.add(line);

                    String s = tline.replace("<way id=\"", "").split("\"", 2)[0];
                    long id = Long.parseLong(s);
                    boolean write = neededWaysIds.contains(id);

                    line = bReader.readLine();
                    lineNR++;
                    if (lineNR % 1000000 == 0) {
                        System.out.println("writing new file: " + ((lineNR * 100) / lineCount) + " % ");
                    }
                    tline = line.trim();

                    while (tline.startsWith("<nd ref=\"") == false) {
                        way1List.add(line);

                        line = bReader.readLine();
                        lineNR++;
                        if (lineNR % 1000000 == 0) {
                            System.out.println("writing new file: " + ((lineNR * 100) / lineCount) + " % ");
                        }
                        tline = line.trim();
                    }

                    // way2List: save all lines with ref-Tag (nodes for wayParts)
                    LinkedList<String> way2List = new LinkedList<String>();

                    while (tline.startsWith("<nd ref=\"") == true) {
                        way2List.add(line);

                        line = bReader.readLine();
                        lineNR++;
                        if (lineNR % 1000000 == 0) {
                            System.out.println("writing new file: " + ((lineNR * 100) / lineCount) + " % ");
                        }
                        tline = line.trim();
                    }

                    // way3List: save all lines with k-Tag (properties of way (e.g. one_way, speed, ...))
                    LinkedList<String> way3List = new LinkedList<String>();

                    while (tline.startsWith("<tag k=\"") == true) {
                        way3List.add(line);

                        line = bReader.readLine();
                        lineNR++;
                        if (lineNR % 1000000 == 0) {
                            System.out.println("writing new file: " + ((lineNR * 100) / lineCount) + " % ");
                        }
                        tline = line.trim();
                    }

                    // way4List: save all lines after "k-tag" to end of way tag
                    LinkedList<String> way4List = new LinkedList<String>();

                    while (tline.startsWith("</way>") == false) {
                        way4List.add(line);

                        line = bReader.readLine();
                        lineNR++;
                        if (lineNR % 1000000 == 0) {
                            System.out.println("writing new file: " + ((lineNR * 100) / lineCount) + " % ");
                        }
                        tline = line.trim();
                    }

                    way4List.add(line);

					// create for every wayPart one new way and save it to the new osm
                    // if oneway is "-1" turn over the way / list of nodes (wayParts)
                    if (write) {

                        // check oneway property
                        boolean oneway_nurRueckrichtung = false;
                        //boolean has_oneway = false;
                        boolean is_oneway = false;
                        boolean is_oneway_implies = false;

                        String oldOneWay = "";

                        for (int i = way3List.size() - 1; i >= 0; i--) {
                            s = way3List.get(i);

                            if (s.contains("k=\"oneway\" v=\"")) {
								//has_oneway = true;

                                oldOneWay = s.split("\"oneway\" v=\"")[1].split("\"")[0];

                                if (s.contains("k=\"oneway\" v=\"-1\"")) {
                                    oneway_nurRueckrichtung = true;
                                    is_oneway = true;
                                    s = s.replace("k=\"oneway\" v=\"-1\"", "k=\"oneway\" v=\"yes\"");
                                    way3List.remove(i);
                                    way3List.add(s);
                                    i = way3List.size() - 1;
                                } else if (s.contains("k=\"oneway\" v=\"yes\"")) {
                                    is_oneway = true;
                                }

                                way3List.remove(i);
                            } else if (s.contains("k=\"junction\" v=\"roundabout") || s.contains("k=\"highway\" v=\"motorway")) {
                                is_oneway_implies = true;
                            }
                        }

						// split/copy way with n wayParts to n new ways
                        // create und save way/ways to new file (note way direction (oneway))
                        for (int i = 0; i < way2List.size() - 1; i++) {

                            s = way2List.get(i).split("<nd ref=\"", 2)[1].split("\"", 2)[0];
                            long id1 = Long.parseLong(s);

                            s = way2List.get(i + 1).split("<nd ref=\"", 2)[1].split("\"", 2)[0];
                            long id2 = Long.parseLong(s);

                            if (id1 == id2) {

                                System.out.println("Info: skipping way: " + id + " : " + id1 + " -> " + id2);

                            } else if (neededNodesIds.contains(id1) && neededNodesIds.contains(id2)) {

                                if (is_oneway == false) {

                                    boolean newWayPartHin = isWayPartNew(id1, id2);

                                    boolean newWayPartRueck = isWayPartNew(id2, id1);

                                    if (newWayPartHin == true && newWayPartRueck == true) {

                                        String wlines[] = way1List.get(0).split("\"", 3);
                                        String wline = wlines[0] + "\"" + getNewId() + "\"" + wlines[2];

                                        bWriter.write(wline + System.lineSeparator());

                                        for (int j = 1; j < way1List.size(); j++) {
                                            bWriter.write(way1List.get(j) + System.lineSeparator());
                                        }

                                        bWriter.write(way2List.get(i) + System.lineSeparator());
                                        bWriter.write(way2List.get(i + 1) + System.lineSeparator());

                                        for (int j = 0; j < way3List.size(); j++) {
                                            bWriter.write(way3List.get(j) + System.lineSeparator());
                                        }

                                        bWriter.write("  <tag k=\"old_way_id\" v=\"" + wlines[1] + "\"/>\n");

                                        if (is_oneway_implies) {
                                            bWriter.write("  <tag k=\"oneway\" v=\"yes\"/>\n");
                                        } else {
                                            bWriter.write("  <tag k=\"oneway\" v=\"no\"/>\n");
                                        }
                                        bWriter.write("  <tag k=\"old_oneway\" v=\"" + oldOneWay + "\"/>\n");

                                        for (int j = 0; j < way4List.size(); j++) {
                                            bWriter.write(way4List.get(j) + System.lineSeparator());
                                        }

                                    } else if (newWayPartHin == true && newWayPartRueck == false) {

                                        System.out.println("Info: skipping way (duplication): " + id + " : " + id2 + " -> " + id1);

                                        String wlines[] = way1List.get(0).split("\"", 3);
                                        String wline = wlines[0] + "\"" + getNewId() + "\"" + wlines[2];

                                        bWriter.write(wline + System.lineSeparator());

                                        for (int j = 1; j < way1List.size(); j++) {
                                            bWriter.write(way1List.get(j) + System.lineSeparator());
                                        }

                                        bWriter.write(way2List.get(i) + System.lineSeparator());
                                        bWriter.write(way2List.get(i + 1) + System.lineSeparator());

                                        for (int j = 0; j < way3List.size(); j++) {
                                            bWriter.write(way3List.get(j) + System.lineSeparator());
                                        }

                                        bWriter.write("  <tag k=\"old_way_id\" v=\"" + wlines[1] + "\"/>\n");

                                        bWriter.write("  <tag k=\"oneway\" v=\"yes\"/>\n");
                                        bWriter.write("  <tag k=\"old_oneway\" v=\"" + oldOneWay + "\"/>\n");

                                        for (int j = 0; j < way4List.size(); j++) {
                                            bWriter.write(way4List.get(j) + System.lineSeparator());
                                        }

                                    } else if (newWayPartHin == false && newWayPartRueck == true) {

                                        System.out.println("Info: skipping way (duplication): " + id + " : " + id1 + " -> " + id2);

                                        String wlines[] = way1List.get(0).split("\"", 3);
                                        String wline = wlines[0] + "\"" + getNewId() + "\"" + wlines[2];

                                        bWriter.write(wline + System.lineSeparator());

                                        for (int j = 1; j < way1List.size(); j++) {
                                            bWriter.write(way1List.get(j) + System.lineSeparator());
                                        }

                                        bWriter.write(way2List.get(i + 1) + System.lineSeparator());
                                        bWriter.write(way2List.get(i) + System.lineSeparator());

                                        for (int j = 0; j < way3List.size(); j++) {
                                            bWriter.write(way3List.get(j) + System.lineSeparator());
                                        }

                                        bWriter.write("  <tag k=\"old_way_id\" v=\"" + wlines[1] + "\"/>\n");

                                        bWriter.write("  <tag k=\"oneway\" v=\"yes\"/>\n");
                                        bWriter.write("  <tag k=\"old_oneway\" v=\"" + oldOneWay + "\"/>\n");

                                        for (int j = 0; j < way4List.size(); j++) {
                                            bWriter.write(way4List.get(j) + System.lineSeparator());
                                        }

                                    } else {

                                        System.out.println("Info: skipping way (duplication): " + id + " : " + id2 + " -> " + id1);
                                        System.out.println("Info: skipping way (duplication): " + id + " : " + id1 + " -> " + id2);

                                    }

                                } else {

                                    if (oneway_nurRueckrichtung == false) {

                                        // Hinrichtung
                                        boolean newWayPart = isWayPartNew(id1, id2);

                                        if (newWayPart == false) {

                                            System.out.println("Info: skipping way (duplication): " + id + " : " + id1 + " -> " + id2);

                                        } else {

                                            String wlines[] = way1List.get(0).split("\"", 3);
                                            String wline = wlines[0] + "\"" + getNewId() + "\"" + wlines[2];

                                            bWriter.write(wline + System.lineSeparator());

                                            for (int j = 1; j < way1List.size(); j++) {
                                                bWriter.write(way1List.get(j) + System.lineSeparator());
                                            }

                                            bWriter.write(way2List.get(i) + System.lineSeparator());
                                            bWriter.write(way2List.get(i + 1) + System.lineSeparator());

                                            for (int j = 0; j < way3List.size(); j++) {
                                                bWriter.write(way3List.get(j) + System.lineSeparator());
                                            }

                                            bWriter.write("  <tag k=\"old_way_id\" v=\"" + wlines[1] + "\"/>\n");

                                            bWriter.write("  <tag k=\"oneway\" v=\"yes\"/>\n");
                                            bWriter.write("  <tag k=\"old_oneway\" v=\"" + oldOneWay + "\"/>\n");

                                            for (int j = 0; j < way4List.size(); j++) {
                                                bWriter.write(way4List.get(j) + System.lineSeparator());
                                            }

                                        }

                                    } else {

                                        // Rueckrichtung
                                        boolean newWayPart = isWayPartNew(id2, id1);

                                        if (newWayPart == false) {

                                            System.out.println("Info: skipping way (duplication): " + id + " : " + id1 + " -> " + id2);

                                        } else {

                                            String wlines[] = way1List.get(0).split("\"", 3);
                                            String wline = wlines[0] + "\"" + getNewId() + "\"" + wlines[2];

                                            bWriter.write(wline + System.lineSeparator());

                                            for (int j = 1; j < way1List.size(); j++) {
                                                bWriter.write(way1List.get(j) + System.lineSeparator());
                                            }

                                            bWriter.write(way2List.get(i + 1) + System.lineSeparator());
                                            bWriter.write(way2List.get(i) + System.lineSeparator());

                                            for (int j = 0; j < way3List.size(); j++) {
                                                bWriter.write(way3List.get(j) + System.lineSeparator());
                                            }

                                            bWriter.write("  <tag k=\"old_way_id\" v=\"" + wlines[1] + "\"/>\n");

                                            bWriter.write("  <tag k=\"oneway\" v=\"yes\"/>\n");
                                            bWriter.write("  <tag k=\"old_oneway\" v=\"" + oldOneWay + "\"/>\n");

                                            for (int j = 0; j < way4List.size(); j++) {
                                                bWriter.write(way4List.get(j) + System.lineSeparator());
                                            }

                                        }

                                    }

                                }

                            }
                        }

                    }

                    // read relation-Tag, but to not copy/save it to the new file
                } else if (tline.startsWith("<relation id=\"")) {

                    String s = tline.replace("<relation id=\"", "").split("\"")[0];
                    long id = Long.parseLong(s);

                    if (tline.endsWith("/>")) {

                    } else if (tline.endsWith(">")) {

                        line = bReader.readLine();
                        lineNR++;
                        if (lineNR % 1000000 == 0) {
                            System.out.println("writing new file: " + ((lineNR * 100) / lineCount) + " % ");
                        }
                        tline = line.trim();

                        while (tline.equals("</relation>") == false) {
                            line = bReader.readLine();
                            lineNR++;
                            if (lineNR % 1000000 == 0) {
                                System.out.println("writing new file: " + ((lineNR * 100) / lineCount) + " % ");
                            }
                            tline = line.trim();
                        }

                    } else {
                        System.out.println("Error: Ende von relation: " + id);
                    }

                    // copy/save bounds-Tag
                } else if (tline.startsWith("<bounds minlat=")) {
                    bWriter.write("	<bounds minlat=\"" + minlat + "\" minlon=\"" + minlon + "\" maxlat=\"" + maxlat + "\" maxlon=\"" + maxlon + "\"/>" + System.lineSeparator());

                    // copy/save unknown-Tag 
                } else {
                    bWriter.write(line + System.lineSeparator());
                }

                line = bReader.readLine();
                lineNR++;
                if (lineNR % 1000000 == 0) {
                    System.out.println("writing new file: " + ((lineNR * 100) / lineCount) + " % ");
                }
            }

            bWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());
        }

        System.out.println(" end :-) ");

    }

    /**
     * check if bit for this means of transport is set
     */
    public static boolean getMeansOfTransportPermission(final int transportFlag) {
        return ((meansOfTransport & transportFlag) != 0);
    }

    public static void set_meansOfTransport(long id) {
        //transform Highway string to integer
        int highwayType = highwayType(highway);

        //check carPermission
        carPermission = carPermission(highwayType, motorcar, id);

        //check car permission to set flag
        if (carPermission != 0) {
            meansOfTransport |= OSMParser.CAR;
        }
    }

    /**
     * return highwayType as int from highway
     */
    public static int highwayType(String highway) {
        for (int i = 0; i < highwayTypes.length; i++) {
            if (highwayTypes[i].equals(highway)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns a value that shows if a car may drive on this way.
     *
     * @param highwayType
     * @param motorcar
     * @param id
     * @return 0 = not allowed, 1 = restricted, 2 = allowed
     */
    public static int carPermission(int highwayType, String motorcar, long id) {

        // highway types usually intended for car
        if (highwayType >= 0 && highwayType <= 29) {
            if (motorcar.equals("") || motorcar.equals("yes") || motorcar.equals("designated") || motorcar.equals("official")) {
                return 2;
            } else if (motorcar.equals("private") || motorcar.equals("permissive") || motorcar.equals("unknown")
                    || motorcar.equals("restricted") || motorcar.equals("destination") || motorcar.equals("customer")
                    || motorcar.equals("delivery") || motorcar.equals("agricultural") || motorcar.equals("forestry")
                    || motorcar.equals("destination; no") || motorcar.equals("agricultural;forestry") || motorcar.equals("access")
                    || motorcar.equals("delivery;destination") || motorcar.equals("customers")) {

                return 1;
            } else if (motorcar.equals("no")) {
                return 0;
            } else {
                System.out.println("Illegal motorcar/highway combination in way-id:" + id
                        + " highway=" + highwayTypes[highwayType] + " motorcar=" + motorcar);
                return 0;
            }
        } // motor-driven vehicles can't drive on steps!
        else if (highwayType == 30) {
            if (motorcar.equals("") || motorcar.equals("no")) {
                return 0;
            } else {
                System.out.println("Illegal motorcar/highway combination in way-id:" + id
                        + " highway=" + highwayTypes[highwayType] + " motorcar=" + motorcar);
                return 0;
            }
        } // highway types not designed for cars primarily
        else if (highwayType >= 31 && highwayType <= 49) {
            if (motorcar.equals("") || motorcar.equals("no")) {
                return 0;
            } else if (motorcar.equals("yes") || motorcar.equals("designated") || motorcar.equals("official")) {
                return 2;
            } else if (motorcar.equals("private") || motorcar.equals("permissive") || motorcar.equals("unknown")
                    || motorcar.equals("restricted") || motorcar.equals("destination") || motorcar.equals("customer")
                    || motorcar.equals("delivery") || motorcar.equals("agricultural") || motorcar.equals("forestry")) {
                return 1;
            } else {
                System.out.println("Unhandled motorcar/highway combination in way-id:" + id
                        + " highway=" + highwayTypes[highwayType] + " motorcar=" + motorcar);
                return 0;
            }
        }
        //else
        return 0;
    }

}

// helper class with GPS coordinates
class nodeGps {

    public double lat;
    public double lon;
}
