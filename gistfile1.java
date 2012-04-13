
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Search {

    // 共通オプション
    public static final String MODE = "-mode";
    public static final String DIRECTORY = "-dir";
    // 以下indexモード用オプション
    public static final String DATE = "-date";
    //以下searchモード用オプション
    public static final String DATE_MIN = "-dm";
    public static final String DATE_MAX = "-dM";
    public static final String TEXT = "-t";
    public static final String OUTPUT = "-o";

    public static void main(String[] args) throws IOException, ParseException {
        Map<String, String> am = new HashMap<String, String>();
        for (int i = 0; i < args.length; i += 2) {
            am.put(args[i], args[i + 1]);
        }

        final String mode = am.get(MODE);
        if ("index".equals(mode)) {
            final Date date = DateFormat.getDateInstance().parse(am.get(DATE));
            new Indexer().index(new File(am.get(DIRECTORY)), date);
        } else if ("search".equals(mode)) {
            new Finder().find();
        }
    }

    public static class Indexer {

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

        public void index(final File dir, final Date date) {
            final Map<String, List<FileInfo>> results = new TreeMap<String, List<FileInfo>>();
            final Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            final String name = String.format("%4d_%02d_%02d", cal.get(YEAR), cal.get(MONTH) + 1, cal.get(Calendar.DATE));

            for (File f : dir.listFiles()) {
                if (f.isFile() && f.getName().endsWith(".zip") && f.getName().contains(name)) {
                    List<FileInfo> list = listZip(f);
                    results.put(f.getName(), list);
                }
            }

            for (String k : results.keySet()) {
                System.out.println("==" + k + "==");
                for (FileInfo fi : results.get(k)) {
                    System.out.println(fi);
                }
            }
        }

        public List<FileInfo> listZip(File f) {
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

        public List<FileInfo> listFile(ZipEntry entry) throws IOException {
            List<FileInfo> ret = new ArrayList<FileInfo>();
            if (!entry.isDirectory()) {
                ret.add(new FileInfo(entry.getName(), entry.getTime()));
            }
            return ret;
        }
    }

    public static class Finder {

        private void find() {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }
}
