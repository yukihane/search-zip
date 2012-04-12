
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Finder {

    public static final String DIRECTORY = "-dir";
    public static final String DATE_MIN = "-dm";
    public static final String DATE_MAX = "-dM";
    public static final String TEXT = "-t";
    public static final String OUTPUT = "-o";

    public static void main(String[] args) throws IOException {
        Map<String, String> am = new HashMap<String, String>();
        for (int i = 0; i < args.length; i += 2) {
            am.put(args[i], args[i + 1]);
        }

        List<String> list = new ArrayList<String>();

        File dir = new File(am.get(DIRECTORY));
        for (File f : dir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".zip")) {
                list.addAll(listZip(f));
            }
        }

        for (String s : list) {
            System.out.println(s);
        }
    }

    private static List<String> listZip(File f) {
        try {
            ZipFile zip = new ZipFile(f.getAbsolutePath());
            Enumeration<? extends ZipEntry> enu = zip.entries();
            List<String> ret = new ArrayList<String>();
            while (enu.hasMoreElements()) {
                ret.addAll(listFile((ZipEntry) enu.nextElement()));
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<String> listFile(ZipEntry entry) throws IOException {
        List<String> ret = new ArrayList<String>();
        String name = entry.getName();
        if (entry.isDirectory()) {
            ret.add("Dir :" + name);
        } else {
            ret.add("File:" + name);
        }
        return ret;
    }
}
