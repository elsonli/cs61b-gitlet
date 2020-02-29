package gitlet;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Commit implements Serializable {
    private String ownHash;
    private String parentHash;
    private String message;
    private String datetime;
    private HashMap<String, String> blobs; // <fileName, SHA1>

    public Commit(String msg, HashMap<String, String> blobMap, String parent) {
        LocalDateTime current = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        message = msg;
        datetime = current.format(formatter);
        blobs = blobMap;
        parentHash = parent;
        ownHash = calcHash();
        SerializeUtils.writeStringToFile(globalLog(), ".gitlet/global-log/gl.txt", true);
    }

    public String calcHash() {
        byte[] commitObj = SerializeUtils.toByteArray(this);
        return Utils.sha1(commitObj);
    }

    public String getOwnHash() {
        return ownHash;
    }

    public String getParentHash() {
        return parentHash;
    }

    public String getMessage() {
        return message;
    }

    public String getDatetime() {
        return datetime;
    }

    public HashMap<String, String> getBlobs() {
        return blobs;
    }

    public String globalLog() {
        String firstLine = "===\n";
        String secondLine = "Commit " + ownHash + "\n";
        String thirdLine = datetime + "\n";
        String fourthLine = message + "\n";
        String fifthLine = "\n";
        String allLines = firstLine + secondLine + thirdLine + fourthLine + fifthLine;
        return allLines;
    }
}
