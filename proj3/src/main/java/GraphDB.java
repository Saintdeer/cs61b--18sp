import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /**
     * Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc.
     */

    /* all nodes with lat and lon */
    Map<Long, Node> rowNodes = new HashMap<>();

    /* all nodes with relationships */
    Map<Long, Node> validNodes = new HashMap<>();

    /* all nodes with name in rowNodes, maybe not valid */
    Map<Long, Node> nameNodes = new HashMap<>();
    Set<Long> ids = new HashSet<>();

    /* names in nameNodes*/
    Tries nodeNames = new Tries();
    Map<String, List<Long>> cleanNameToId = new HashMap<>();

    static class Node {
        long id;
        double lon, lat;
        Map<String, String> info = new HashMap<>();
        Set<Long> adjacent = new HashSet<>();
        Set<String> way = new HashSet<>();

        Node(long id, double lon, double lat) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
        }

        void addAdjacent(Node nd) {
            if (nd == null || nd.equals(this)) {
                return;
            }
            this.adjacent.add(nd.id);
            nd.adjacent.add(this.id);
        }
    }

    /* add nodes of the way to graphdb when way is valid,
        and add adjacent relationships on this way*/
    void addValidNodesOfWay(Way way) {
        /* if number of nodes of the way less than 2, there's no edges */
        if (way.nodesIdOfWay.size() < 2) {
            return;
        }

        Iterable<Long> nodeIdIterable = way.nodesIdOfWay;
        String wayName = way.info.get("name");

        /* only nodes with relationships considered valid */
        Node oldNode = null;
        for (Long nodeId : nodeIdIterable) {
            Node node = rowNodes.get(nodeId);
            ids.add(nodeId);
            validNodes.put(nodeId, node);
            node.addAdjacent(oldNode);

            if (wayName != null) {
                node.way.add(wayName);
            }
            oldNode = node;
        }
    }

    static class Way {
        long id;
        boolean valid;
        Map<String, String> info = new HashMap<>();
        List<Long> nodesIdOfWay = new ArrayList<>();

        Way(long id) {
            this.id = id;
        }
    }

    Node getNode(long id) {
        return validNodes.get(id);
    }

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     *
     * @param dbPath Path to the XML file to be parsed.
     */
    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     *
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     * Remove nodes with no connections from the graph.
     * While this does not guarantee that any two nodes in the remaining graph are connected,
     * we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        //Your code here.
        for (Long id : rowNodes.keySet()) {
            Node nd = rowNodes.get(id);
            if (nd.info.get("name") != null) {
                nameNodes.put(id, nd);
            }
        }
        rowNodes.clear();
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     *
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        //YOUR CODE HERE, this currently returns only an empty list.
        return ids;
    }

    /**
     * Returns ids of all vertices adjacent to v.
     *
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        return validNodes.get(v).adjacent;
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     *
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     *
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     *
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        double minDist = Double.MAX_VALUE;
        long closeId = 0;
        for (long id : ids) {
            double longitude = lon(id), latitude = lat(id),
                    currentDist = distance(lon, lat, longitude, latitude);
            if (currentDist < minDist) {
                minDist = currentDist;
                closeId = id;
            }
        }
        return closeId;
    }

    /**
     * Gets the longitude of a vertex.
     *
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        return validNodes.get(v).lon;
    }

    /**
     * Gets the latitude of a vertex.
     *
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        return validNodes.get(v).lat;
    }

    public List<String> getLocationsByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return new ArrayList<>();
        }

        String clean = GraphDB.cleanString(prefix);

        return nodeNames.getStrWithPrefix(clean);
    }

    public List<Map<String, Object>> getLocations(String locationName) {
        List<Map<String, Object>> result = new LinkedList<>();
        if (locationName == null) {
            return result;
        }

        String cleanName = GraphDB.cleanString(locationName);
        if (!cleanNameToId.containsKey(cleanName)) {
            return result;
        }

        for (Long id : cleanNameToId.get(cleanName)) {
            Node nd = nameNodes.get(id);
            Map<String, Object> info = new HashMap<>();
            info.put("lat", nd.lat);
            info.put("lon", nd.lon);
            info.put("name", nd.info.get("name"));
            info.put("id", id);
            result.add(info);
        }
        return result;
    }
}
