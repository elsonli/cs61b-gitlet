package gitlet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.File;
import java.io.IOException;

public class Repo {
    private String HEAD = "master";
    private StagingArea stage;
    private File workingDir;

    public Repo() {
        workingDir = new File(System.getProperty("user.dir"));
        String pathToHead = ".gitlet/branches/HEAD.txt";
        if (new File(pathToHead).exists()) {
            HEAD = SerializeUtils.readStringFromFile(pathToHead);
        }
        String pathToStage = ".gitlet/staging/stage.txt";
        if (new File(pathToStage).exists()) {
            stage = SerializeUtils.deserialize(pathToStage, StagingArea.class);
        }
    }

    public void init() {
        File git = new File(".gitlet");
        if (git.exists()) {
            System.out.println("A gitlet version-control system"
                    + " already exists in the current directory.");
        } else {
            new File(".gitlet").mkdirs();
            new File(".gitlet/blobs").mkdirs();
            new File(".gitlet/branches").mkdirs();
            new File(".gitlet/commits").mkdirs();
            new File(".gitlet/staging").mkdirs();
            new File(".gitlet/global-log").mkdirs();

            // Initializes default commit saved to /commits directory with SHA1 as name.
            Commit initialCommit = new Commit("initial commit", new HashMap<>(), null);
            SerializeUtils.storeObjectToFile(initialCommit,
                    ".gitlet/commits/" + initialCommit.getOwnHash() + ".txt");

            // Makes a master branch file in /branches with initial commit SHA1 String as contents.
            String pathToMaster = ".gitlet/branches/master.txt";
            new File(pathToMaster);
            SerializeUtils.writeStringToFile(initialCommit.getOwnHash(), pathToMaster, false);

            // Makes a HEAD text file in /branches, with the name of branch as contents.
            String pathToHead = ".gitlet/branches/HEAD.txt";
            new File(pathToHead);
            SerializeUtils.writeStringToFile("master", pathToHead, false);

            // Makes a StagingArea Object with an empty HashMap of added and changed files,
            // as well as an empty ArrayList of removed files.
            stage = new StagingArea();
            new File(".gitlet/staging/stage.txt");
            SerializeUtils.storeObjectToFile(stage, ".gitlet/staging/stage.txt");
        }
    }

    public String getHEAD() {
        return HEAD;
    }

    public StagingArea getStage() {
        return stage;
    }

    public void add(String fileName) {
        File toAdd = new File(fileName);
        File findFile = findFile(fileName, workingDir);
        if (toAdd.exists()) {
            byte[] blob = Utils.readContents(toAdd);
            String blobHash = Utils.sha1(blob);
            if (getCurrentCommit().getBlobs().get(fileName) != null
                    && getCurrentCommit().getBlobs().get(fileName).equals(blobHash)) {
                if (stage.getRemovedFiles().contains(fileName)) {
                    stage.getRemovedFiles().remove(fileName);
                    SerializeUtils.storeObjectToFile(stage, ".gitlet/staging/stage.txt");
                }
                return;
            }
            if (stage.getRemovedFiles().contains(fileName)) {
                stage.getRemovedFiles().remove(fileName);
            }
            Utils.writeContents(new File(".gitlet/blobs/" + blobHash + ".txt"), blob);
            stage.add(fileName, blobHash);
            SerializeUtils.storeObjectToFile(stage, ".gitlet/staging/stage.txt");
        } else {
            System.out.print("File does not exist.");
        }
    }

    public void commitment(String msg) {
        if (stage.getAddedFiles().isEmpty() && stage.getRemovedFiles().isEmpty()) {
            System.out.print("No changes added to the commit.");
            return;
        } else if (msg.equals("")) {
            System.out.print("Please enter a commit message.");
            return;
        }
        Commit curr = getCurrentCommit();
        HashMap<String, String> copiedBlobs = (HashMap) curr.getBlobs().clone();
        ArrayList<String> filesToAdd = new ArrayList<>(stage.getAddedFiles().keySet());
        for (String fileName : filesToAdd) {
            copiedBlobs.put(fileName, stage.getAddedFiles().get(fileName));
        }
        for (String fileToRemove : stage.getRemovedFiles()) {
            copiedBlobs.remove(fileToRemove);
        }
        Commit newC = new Commit(msg, copiedBlobs, curr.getOwnHash());
        SerializeUtils.writeStringToFile(newC.getOwnHash(),
                ".gitlet/branches/" + HEAD + ".txt", false);
        SerializeUtils.storeObjectToFile(newC,
                ".gitlet/commits/" + newC.getOwnHash() + ".txt");
        stage.clear();
        SerializeUtils.storeObjectToFile(stage, ".gitlet/staging/stage.txt");
    }

    public void rm(String fileName) {
        boolean isStaged = stage.getAddedFiles().containsKey(fileName);
        Commit curr = getCurrentCommit();
        boolean isTracked = false;
        ArrayList<String> committedFiles = new ArrayList<>(curr.getBlobs().keySet());
        for (String f : committedFiles) {
            if (f.equals(fileName)) {
                isTracked = true;
            }
        }
        if (isTracked) {
            Utils.restrictedDelete(fileName);
            stage.addToRemovedFiles(fileName);
            if (isStaged) {
                stage.getAddedFiles().remove(fileName);
            }
            SerializeUtils.storeObjectToFile(stage, ".gitlet/staging/stage.txt");
        } else if (isStaged) {
            stage.getAddedFiles().remove(fileName);
            SerializeUtils.storeObjectToFile(stage, ".gitlet/staging/stage.txt");
        } else {
            System.out.print("No reason to remove the file.");
        }
    }

    public void log() {
        Commit curr = getCurrentCommit();
        while (curr != null) {
            System.out.println("===");
            System.out.println("Commit " + curr.getOwnHash());
            System.out.println(curr.getDatetime());
            System.out.println(curr.getMessage());
            System.out.println();
            if (curr.getParentHash() != null) {
                curr = SerializeUtils.deserialize(".gitlet/commits/"
                        + curr.getParentHash() + ".txt", Commit.class);
            } else {
                break;
            }
        }
    }

    public void global() {
        File gl = new File(".gitlet/global-log/gl.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(gl))) {
            String line = null;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException excp) {
            return;
        }
    }

    public void find(String msg) {
        File commitFolder = new File(workingDir, ".gitlet/commits");
        String[] fileNameArray = commitFolder.list();
        int printCount = 0;
        for (String aFile : fileNameArray) {
            Commit currFile = SerializeUtils.deserialize(".gitlet/commits/" + aFile, Commit.class);
            if (currFile.getMessage().equals(msg)) {
                System.out.println(currFile.getOwnHash());
                printCount += 1;
            }
        }
        if (printCount == 0) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    public void status() {
        List<String> branches = new ArrayList<String>();
        File[] files1 = new File(".gitlet/branches").listFiles();
        for (File file : files1) {
            branches.add(file.getName().substring(0, file.getName().length() - 4));
        }
        branches.remove("HEAD");
        branches.remove(HEAD);
        branches.add("*" + HEAD);
        Collections.sort(branches);
        List<String> stagedFiles = new ArrayList<String>();
        for (Map.Entry<String, String> entry : stage.getAddedFiles().entrySet()) {
            stagedFiles.add(entry.getKey());
        }
        Collections.sort(stagedFiles);
        List<String> remFiles = new ArrayList<String>();
        for (String file : stage.getRemovedFiles()) {
            remFiles.add(file);
        }
        Collections.sort(remFiles);

        System.out.println("=== Branches ===");
        for (String branch : branches) {
            System.out.println(branch);
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String staged : stagedFiles) {
            System.out.println(staged);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String removed : remFiles) {
            System.out.println(removed);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
    }

    public void checkout(String... args) {
        if (args.length == 2) {
            String branchName = args[1];
            if (!(new File(".gitlet/branches/" + branchName + ".txt")).exists()) {
                System.out.println("No such branch exists.");
                return;
            }
            String branchPath = ".gitlet/branches/" + branchName + ".txt";
            String headPath = ".gitlet/branches/" + HEAD + ".txt";
            String newCommitID = SerializeUtils.readStringFromFile(branchPath);
            Commit newCommit = SerializeUtils.deserialize(".gitlet/commits/"
                    + newCommitID + ".txt", Commit.class);
            Commit curr = getCurrentCommit();
            HashMap<String, String> newBlobs = newCommit.getBlobs();
            HashMap<String, String> headBlobs = curr.getBlobs();
            ArrayList<File> fileList = new ArrayList<>();
            for (File f : workingDir.listFiles()) {
                if (f.getName().endsWith(".txt")) {
                    fileList.add(f);
                }
            }
            Commit commitToCheckout = newCommit;
            Commit currCommit = getCurrentCommit();
            for (File f : fileList) {
                if (!currCommit.getBlobs().containsKey(f.getName())
                        && commitToCheckout.getBlobs().containsKey(f.getName())) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it or add it first.");
                    return;
                }
            }
            ArrayList<String> fileNames = new ArrayList<>(commitToCheckout.getBlobs().keySet());
            for (File f : fileList) {
                if (!commitToCheckout.getBlobs().containsKey(f.getName())
                        && currCommit.getBlobs().containsKey(f.getName())) {
                    Utils.restrictedDelete(f);
                }
            }
            for (String f : fileNames) {
                String blobHash = commitToCheckout.getBlobs().get(f);
                String blobPath = workingDir.getPath() + "/.gitlet/blobs/" + blobHash + ".txt";
                File newFile = new File(blobPath);
                byte[] blobBytes = Utils.readContents(newFile);
                Utils.writeContents(new File(f), blobBytes);
            }
            stage.clear();
            SerializeUtils.storeObjectToFile(stage, workingDir.getPath()
                    + "/.gitlet/staging/stage.txt");
            SerializeUtils.writeStringToFile(branchName, ".gitlet/branches/HEAD.txt", false);
        } else if (args.length == 3) {
            String fileName = args[2];
            Commit headCommit = getCurrentCommit();
            HashMap<String, String> commitMap = headCommit.getBlobs();
            if (!commitMap.containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                return;
            }
            if ((new File(workingDir.getPath() + fileName)).exists()) {
                Utils.restrictedDelete(workingDir.getPath() + fileName);
            }
            File blob = new File(workingDir.getPath() + "/.gitlet/blobs/"
                    + headCommit.getBlobs().get(fileName) + ".txt");
            byte[] storeRFile = Utils.readContents(blob);
            File newFile = new File(workingDir.getPath(), fileName);
            Utils.writeContents(newFile, storeRFile);
        } else if (args.length == 4) {
            String commitID = args[1];
            String fileName = args[3];
            String[] possibleCommit = new File(".gitlet/commits").list();
            for (String identifier : possibleCommit) {
                if (identifier.contains(commitID)) {
                    commitID = identifier;
                    commitID = commitID.substring(0, commitID.length() - 4);
                    break;
                }
            }
            Commit currCommit = SerializeUtils.deserialize(".gitlet/commits/"
                    + commitID + ".txt", Commit.class);
            if (currCommit == null) {
                System.out.println("No commit with that id exists.");
            } else if (!currCommit.getBlobs().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
            } else {
                if ((new File(workingDir.getPath() + fileName)).exists()) {
                    Utils.restrictedDelete(workingDir.getPath() + fileName);
                }
                File newFile = new File(workingDir.getPath(), fileName);
                File blob = new File(workingDir.getPath() + "/.gitlet/blobs/"
                        + currCommit.getBlobs().get(fileName) + ".txt");
                byte[] storeRFile = Utils.readContents(blob);
                Utils.writeContents(newFile, storeRFile);
            }
        }
    }

    public void branch(String branchName) {
        File branchFile = new File(".gitlet/branches/" + branchName + ".txt");
        if (branchFile.exists()) {
            System.out.print("A branch with that name already exists.");
            return;
        }
        String sha1 = SerializeUtils.readStringFromFile(".gitlet/branches/" + HEAD + ".txt");
        SerializeUtils.writeStringToFile(sha1,
                ".gitlet/branches/" + branchName + ".txt",
                false);
    }

    public void rmb(String branchName) {
        if (branchName.equals(SerializeUtils.readStringFromFile(".gitlet/branches/HEAD.txt"))) {
            System.out.print("Cannot remove the current branch.");
            return;
        }
        File branchFile = new File(".gitlet/branches/" + branchName + ".txt");
        if (!branchFile.delete()) {
            System.out.print("A branch with that name does not exist.");
        }
    }

    public void reset(String commitID) {
        Commit commitToCheckout = SerializeUtils.deserialize(".gitlet/commits/"
                + commitID + ".txt", Commit.class);
        if (commitToCheckout == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        ArrayList<File> fileList = new ArrayList<>();
        for (File f : workingDir.listFiles()) {
            if (f.getName().endsWith(".txt")) {
                fileList.add(f);
            }
        }
        Commit currCommit = getCurrentCommit();
        for (File f : fileList) {
            if (!currCommit.getBlobs().containsKey(f.getName())
                    && commitToCheckout.getBlobs().containsKey(f.getName())) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it or add it first.");
                return;
            }
        }
        ArrayList<String> fileNames = new ArrayList<>(commitToCheckout.getBlobs().keySet());
        for (File f : fileList) {
            if (!commitToCheckout.getBlobs().containsKey(f.getName())
                    && currCommit.getBlobs().containsKey(f.getName())) {
                Utils.restrictedDelete(f);
            }
        }
        for (String f : fileNames) {
            String blobHash = commitToCheckout.getBlobs().get(f);
            String blobPath = workingDir.getPath() + "/.gitlet/blobs/" + blobHash + ".txt";
            File newFile = new File(blobPath);
            byte[] blobBytes = Utils.readContents(newFile);
            Utils.writeContents(new File(f), blobBytes);
        }
        stage.clear();
        SerializeUtils.storeObjectToFile(stage, workingDir.getPath()
                + "/.gitlet/staging/stage.txt");
        SerializeUtils.writeStringToFile(commitID, ".gitlet/branches/" + HEAD + ".txt", false);
    }

    public boolean mergeHelper1(String bName) {
        boolean toReturn = false;
        if (!stage.getAddedFiles().isEmpty() || !stage.getRemovedFiles().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            toReturn = true;
            return toReturn;
        } else if (!(new File(".gitlet/branches/" + bName + ".txt")).exists()) {
            System.out.println("A branch with that name does not exist.");
            toReturn = true;
            return toReturn;
        } else if (bName.equals(HEAD)) {
            System.out.println("Cannot merge a branch with itself.");
            toReturn = true;
            return toReturn;
        }
        return toReturn;
    }

    public void mergeHelper2(String cBH, String fName, String bBH) {
        File merge = new File(workingDir.getPath() + "/" + fName);
        byte[] everything = addStuff("<<<<<<< HEAD\n".getBytes(StandardCharsets.UTF_8),
                Utils.readContents(new File(".gitlet/blobs/" + cBH + ".txt")));
        everything = addStuff(everything, "=======\n".getBytes(StandardCharsets.UTF_8));
        everything = addStuff(everything, ">>>>>>>\n".getBytes(StandardCharsets.UTF_8));
        Utils.writeContents(merge, everything);
    }

    public void mergeHelper3(String cBH, String fName, String bBH) {
        File merge = new File(workingDir.getPath() + "/" + fName);
        byte[] everything = addStuff("<<<<<<< HEAD\n".getBytes(StandardCharsets.UTF_8),
                Utils.readContents(new File(".gitlet/blobs/" + cBH + ".txt")));
        everything = addStuff(everything, "=======\n".getBytes(StandardCharsets.UTF_8));
        everything = addStuff(everything, Utils.readContents(
                new File(".gitlet/blobs/" + bBH + ".txt")));
        everything = addStuff(everything, ">>>>>>>\n".getBytes(StandardCharsets.UTF_8));
        Utils.writeContents(merge, everything);
    }

    public void merge(String bName) {
        boolean conflict = false;
        if (mergeHelper1(bName)) {
            return;
        }
        ArrayList<File> fileList = new ArrayList<>();
        for (File f : workingDir.listFiles()) {
            if (f.getName().endsWith(".txt")) {
                fileList.add(f);
            }
        }
        String bCommitID = SerializeUtils.readStringFromFile(".gitlet/branches/" + bName + ".txt");
        String pathToBranchCommit = ".gitlet/commits/" + bCommitID + ".txt";
        Commit cCom = getCurrentCommit();
        Commit bCom = SerializeUtils.deserialize(pathToBranchCommit, Commit.class);
        HashMap<String, Commit> cCommitTree = new HashMap<>();
        Commit cComPtr = cCom;
        Commit bComPtr = bCom;
        Commit sPnt = null;
        for (File f : fileList) {
            if (!cCom.getBlobs().containsKey(f.getName())
                    && bCom.getBlobs().containsKey(f.getName())) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it or add it first.");
                return;
            }
        }
        while (cComPtr != null && SerializeUtils.deserialize(".gitlet/commits/"
                + cComPtr.getOwnHash() + ".txt", Commit.class) != null) {
            cCommitTree.put(cComPtr.getOwnHash(), SerializeUtils.deserialize
                    (".gitlet/commits/" + cComPtr.getOwnHash() + ".txt", Commit.class));
            cComPtr = SerializeUtils.deserialize(".gitlet/commits/"
                    + cComPtr.getParentHash() + ".txt", Commit.class);
        }
        while (bComPtr != null && SerializeUtils.deserialize
                (".gitlet/commits/" + bComPtr.getOwnHash() + ".txt",
                        Commit.class) != null) {
            if (cCommitTree.containsKey(bComPtr.getOwnHash())) {
                sPnt = cCommitTree.get(bComPtr.getOwnHash());
                break;
            }
            bComPtr = SerializeUtils.deserialize
                    (".gitlet/commits/" + bComPtr.getParentHash() + ".txt", Commit.class);
        }
        if (sPnt == null) {
            System.out.println("There was an error finding the split point.");
        }
        if (sPnt.getOwnHash().equals(cCom.getOwnHash())) {
            String currBranch = ".gitlet/branches/" + HEAD + ".txt";
            SerializeUtils.writeStringToFile(bCommitID, currBranch, false);
            System.out.println("Current branch fast-forwarded.");
            return;
        } else if (cCommitTree.containsKey(bCommitID)) {
            System.out.print("Given branch is an ancestor of the current branch.");
            return;
        }
        for (String fName : cCom.getBlobs().keySet()) {
            String bBH = bCom.getBlobs().get(fName);
            String cBH = cCom.getBlobs().get(fName);
            if (sPnt.getBlobs().containsKey(fName) && bCom.getBlobs().containsKey(fName)) {
                String sPBH = sPnt.getBlobs().get(fName);
                if (!sPBH.equals(bBH) && sPBH.equals(cBH)) {
                    checkout("checkout", bCommitID, "--", fName);
                    add(fName);
                    SerializeUtils.storeObjectToFile(stage, ".gitlet/staging/stage.txt");
                }
                if (!sPBH.equals(bBH) || !sPBH.equals(cBH) || !bBH.equals(cBH)) {
                    mergeHelper3(cBH, fName, bBH);
                    conflict = true;
                }
            } else if (!bCom.getBlobs().containsKey(fName) && sPnt.getBlobs().containsKey(fName)
                    && !sPnt.getBlobs().get(fName).equals(cCom.getBlobs().get(fName))) {
                mergeHelper2(cBH, fName, bBH);
                conflict = true;
            }
        }
        ArrayList<String> splitFiles = new ArrayList<>(sPnt.getBlobs().keySet());
        ArrayList<String> givenBranchFiles = new ArrayList<>(bCom.getBlobs().keySet());
        for (String theFile : givenBranchFiles) {
            if (!splitFiles.contains(theFile)) {
                checkout("checkout", bCommitID, "--", theFile);
                add(theFile);
            }
        }
        for (String theFile : splitFiles) {
            if (cCom.getBlobs().containsKey(theFile)) {
                if (sPnt.getBlobs().get(theFile).equals(cCom.getBlobs().get(theFile))
                        && !givenBranchFiles.contains(theFile)) {
                    rm(theFile);
                }
            }
        }
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
            return;
        } else {
            commitment("Merged " + HEAD + " with " + bName + ".");
        }
    }

    public File findFile(String fileName, File dir) throws IllegalArgumentException {
        File[] fileList = dir.listFiles();
        for (File f : fileList) {
            if (f.getName().equals(fileName)) {
                return f;
            }
        }
        return null;
    }

    public Commit getCurrentCommit() {
        String hash = SerializeUtils.readStringFromFile(".gitlet/branches/" + HEAD + ".txt");
        return SerializeUtils.deserialize(".gitlet/commits/" + hash + ".txt", Commit.class);
    }

    private byte[] addStuff(byte[] addThisStuff, byte[] newStuffs) {
        byte[] endResult = new byte[addThisStuff.length + newStuffs.length];
        System.arraycopy(addThisStuff, 0, endResult, 0, addThisStuff.length);
        System.arraycopy(newStuffs, 0, endResult, addThisStuff.length, newStuffs.length);
        return endResult;
    }
}
