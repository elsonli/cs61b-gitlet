package gitlet;

import java.util.*;
import java.io.Serializable;

public class StagingArea implements Serializable {

    private HashMap<String, String> addedFiles;
    private ArrayList<String> removedFiles;

    public StagingArea() {
        addedFiles = new HashMap<>();
        removedFiles = new ArrayList<>();
    }

    public void add(String fileName, String sha1) {
        addedFiles.put(fileName, sha1);
    }

    public void addToRemovedFiles(String fileName) {
        removedFiles.add(fileName);
    }

    public void clear() {
        addedFiles = new HashMap<>();
        removedFiles = new ArrayList<>();
    }

    public HashMap<String, String> getAddedFiles() {
        return addedFiles;
    }

    public ArrayList<String> getRemovedFiles() {
        return removedFiles;
    }
}
