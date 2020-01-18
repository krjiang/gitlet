package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/** Commit class representing the contents of a commit.
 * Includes blobs and tree.
 * @author Kevin Ren */

public class Commit implements Serializable {

    /** Constructor for Commit containing the data
     * LOG, PID, MAP, BRANCH.*/
    public Commit(String log, String pid, HashMap<String, Blob> map,
                  String branch) {
        _log = log;
        _pid = pid;
        _map = map;
        _branch = branch;
        _contents = new ArrayList<>();
        for (Map.Entry<String, Blob> entry : map.entrySet()) {
            _contents.add(entry.getValue().contents());
        }
        _timeStamp = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z")
                .format(new Date());
        _sha1 = calcSHA();
    }

    /** Constructor for Commit containing
     * LOG, BRANCH.
     */
    public Commit(String log, String branch) {
        _log = log;
        _branch = branch;
        _pid = null;
        _timeStamp = "Wed Dec 31 16:00:00 1969 -0800";
        _sha1 = uid();
    }

    /** Returns the SHA-1 calculated for the commit. */
    private String calcSHA() {
        String type = "Commit";
        ArrayList<Object> metadata = new ArrayList<>();
        metadata.add(_log);
        metadata.add(_timeStamp);
        metadata.add(_map.toString());
        metadata.add(_pid);
        metadata.add(type);
        return Utils.sha1(metadata);
    }

    /** Returns the universal SHA-1. */
    private String uid() {
        List<Object> initial = new ArrayList<>();
        initial.add("commit");
        initial.add(_timeStamp);
        initial.add(_log);
        return Utils.sha1(initial);
    }

    /** Returns the contents of the commit. */
    public ArrayList<byte[]> contents() {
        return _contents;
    }

    /** Returns the SHA-1. */
    public String id() {
        return _sha1;
    }

    /** Returns parent ID. */
    public String pid() {
        return _pid;
    }

    /** Returns timestamp. */
    public String timeStamp() {
        return _timeStamp;
    }

    /** Returns log. */
    public String log() {
        return _log;
    }

    /** Returns branch. */
    public String branch() {
        return _branch;
    }

    /** Returns map. */
    public HashMap<String, Blob> map() {
        return _map;
    }


    /** Log message for the commit. */
    private String _log;
    /** The timestamp of the commit. */
    private String _timeStamp;
    /** Mapping of files to blobs. */
    private HashMap<String, Blob> _map;
    /** SHA-1 of the commit. */
    private String _sha1;
    /** Parent identifier. */
    private String _pid;
    /** Branch location. */
    private String _branch;
    /** Contents of the blobs in the commit. */
    private ArrayList<byte[]> _contents;
}
