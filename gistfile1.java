
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Finder {

    public static void main(String[] args) throws IOException {
        List<String> list = new ArrayList<String>();

        File dir = new File(args[0]);
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
