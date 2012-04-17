
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class Search {

    public static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
    // 共通オプション
    public static final String MODE = "-mode";
    public static final String DIRECTORY = "-dir";
    // 以下indexモードとfindモードで用途が異なるオプション
    // indexではzipファイル名パターン, findではテキストファイル名パターン
    public static final String FILE_NAME_PATTERN = "-pattern";
    //以下findモード用オプション
    public static final String INDEX = "-index";
    public static final String DATE_MIN = "-dm";
    public static final String DATE_MAX = "-dM";
    public static final String TEXT = "-text";
    public static final String OUTPUT_DIRECTORY = "-o";

    public static void main(String[] args) throws IOException, ParseException {
        Map<String, String> am = new HashMap<String, String>();
        for (int i = 0; i < args.length; i += 2) {
            am.put(args[i], args[i + 1]);
        }

        final String mode = am.get(MODE);
        if ("index".equals(mode)) {
            final Pattern p = Pattern.compile(am.get(FILE_NAME_PATTERN));
            new Indexer().index(new File(am.get(DIRECTORY)), p);
        } else if ("find".equals(mode)) {
            final File index = new File(am.get(INDEX));
            final File inDir = new File(am.get(DIRECTORY));
            final File outDir = new File(am.get(OUTPUT_DIRECTORY));
            final String fileName = (am.get(FILE_NAME_PATTERN) != null) ? am.get(FILE_NAME_PATTERN) : ".*";
            final Pattern fnPattern = Pattern.compile(fileName);
            final String text = (am.get(TEXT) != null) ? am.get(TEXT) : ".*";
            final DateFormat df = DateFormat.getInstance();
            final Date dateMin = (am.get(DATE_MIN) != null) ? df.parse(am.get(DATE_MIN)) : null;
            final Date dateMax = (am.get(DATE_MAX) != null) ? df.parse(am.get(DATE_MAX)) : null;

            new Finder().find(text, index, fnPattern, dateMin, dateMax, inDir, outDir);
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
                final SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
                return "" + sdf.format(time) + "\t" + name;
            }
        }

        public void index(final File dir, final Pattern pattern) {
            final Map<String, List<FileInfo>> results = new TreeMap<String, List<FileInfo>>();

            for (File f : dir.listFiles()) {
                if (f.isFile() && f.getName().endsWith(".zip")) {
                    final Matcher m = pattern.matcher(f.getName());
                    if (m.find()) {
                        List<FileInfo> list = listZip(f);
                        results.put(f.getName(), list);
                    }
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

        public static final Pattern ZIP_FILE_PATTERN = Pattern.compile("^==(.*)==$");

        public void find(String text, File index, Pattern fileName, Date dateMin, Date dateMax, File inDir, File outDir)
                throws IOException, ParseException {
            final Pattern pattern = Pattern.compile(text);
            final BufferedReader reader = new BufferedReader(new FileReader(index));

            String line;
            String zipFile = null;
            while ((line = reader.readLine()) != null) {
                final Matcher zipMatcher = ZIP_FILE_PATTERN.matcher(line);
                if (zipMatcher.matches()) {
                    zipFile = zipMatcher.group(1);
                } else {
                    final String[] info = line.split("\t");
                    final SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
                    final Date timeStamp = sdf.parse(info[0]);
                    final String entryName = info[1];

                    if ((dateMin == null || timeStamp.after(dateMin)) && (dateMax == null || timeStamp.before(dateMax))) {
                        final Matcher m = fileName.matcher(entryName);
                        if (m.find()) {
                            final ZipInputStream zis = new ZipInputStream(new FileInputStream(new File(inDir, zipFile)));
                            for (ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry()) {
                                if (entryName.equals(ze.getName())) {
                                    System.out.println("CANDIDATE: " + entryName);
                                    File outFile = extract(outDir, entryName, zis, timeStamp);

                                    checkText(outFile, pattern);
                                }
                            }
                            zis.close();
                        }
                    }
                }


            }
        }

        public File extract(final File outDir, final String entryName, final ZipInputStream zis, final Date timeStamp)
                throws
                IOException {
            final String[] splittedEntryName = entryName.split("/");
            final String baseName = splittedEntryName[splittedEntryName.length - 1];
            final File outFile = new File(outDir, baseName);

            final BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(outFile));
            final byte[] b = new byte[1024 * 1024];
            int len;
            while ((len = zis.read(b, 0, b.length)) != -1) {
                bos.write(b, 0, len);
            }
            bos.close();
            outFile.setLastModified(timeStamp.getTime());
            return outFile;
        }

        public void checkText(final File outFile, final Pattern pattern) throws IOException {
            final BufferedReader br = new BufferedReader(new FileReader(outFile));
            String line;
            boolean matched = false;
            while ((line = br.readLine()) != null) {
                final Matcher m = pattern.matcher(line);
                if (m.find()) {
                    matched = true;
                    break;
                }
            }
            br.close();

            if (!matched) {
                System.out.println("NO MACH,DELETE: " + outFile);
                outFile.delete();
            } else {
                System.out.println("HIT: " + outFile);
            }
        }
    }
}
