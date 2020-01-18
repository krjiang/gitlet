package gitlet;

import java.io.File;
import java.io.Serializable;

/** Blob class representing the contents of a file.
 * @author Kevin Ren */

public class Blob implements Serializable {

    /** Constructor for a blob with
     * file has NAME. */
    public Blob(String name) {
        File file = new File(name);
        _name = name;
        _contents = Utils.readContents(file);
        _string = Utils.readContentsAsString(file);
        _sha1 = calcSHA();
    }

    /** Returns the SHA-1 for the blob. */
    private String calcSHA() {
        String type = "Blob";
        return Utils.sha1(_name, _contents, type);
    }

    /** Returns the contents. */
    public byte[] contents() {
        return _contents;
    }

    /** Returns the contents as a string. */
    public String string() {
        return _string;
    }

    /** Returns the name. */
    public String name() {
        return _name;
    }

    /** Returns the SHA-1 id for the blob. */
    public String id() {
        return _sha1;
    }

    /** SHA-1 of the blob. */
    private String _sha1;
    /** Name of the blob. */
    private String _name;
    /** Contents of File. */
    private byte[] _contents;
    /** Contents of File as a string. */
    private String _string;

}
