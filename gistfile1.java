
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Finder {

    public static final String MODE = "-mode"
    public static final String DIRECTORY = "-dir";
    public static final String DATE_MIN = "-dm";
    public static final String DATE_MAX = "-dM";
    public static final String TEXT = "-t";
    public static final String OUTPUT = "-o";

    public static class FileInfo {

        private final String name;
        private final Date time;

        private FileInfo(String name, long time) {
            this.name = name;
            this.time = new Date(time);
        }

        @Override
        public String toString() {
            return "" + time + "\t" + name;
        }
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> am = new HashMap<String, String>();
        for (int i = 0; i < args.length; i += 2) {
            am.put(args[i], args[i + 1]);
        }

        List<FileInfo> list = new ArrayList<FileInfo>();

        File dir = new File(am.get(DIRECTORY));
        for (File f : dir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".zip")) {
                list.addAll(listZip(f));
            }
        }

        for (FileInfo s : list) {
            System.out.println(s);
        }
    }

    public static List<FileInfo> listZip(File f) {
        try {
            ZipFile zip = new ZipFile(f.getAbsolutePath());
            Enumeration<? extends ZipEntry> enu = zip.entries();
            List<FileInfo> ret = new ArrayList<FileInfo>();
            while (enu.hasMoreElements()) {
                ret.addAll(listFile((ZipEntry) enu.nextElement()));
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<FileInfo> listFile(ZipEntry entry) throws IOException {
        List<FileInfo> ret = new ArrayList<FileInfo>();
        if (!entry.isDirectory()) {
            ret.add(new FileInfo(entry.getName(), entry.getTime()));
        }
        return ret;
    }
}
