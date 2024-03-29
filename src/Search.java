/*
 * 使い方:
 * 1. 一覧出力(index生成)
 * java -mode index -dir [zipがあるディレクトリ] -pattern [対象zipファイル名パターン] > [出力ファイル]
 *
 * 2. 検索
 * java -mode find dir [zipがあるディレクトリ] \
 * -index [一覧ファイル(上記コマンドで生成したファイル)] \
 * -pattern [検索対象ファイル名パターン] \
 * -dm [検索対象ファイルの更新日時(最古)] \
 * -dM [検索対象ファイルの更新日時(最新)] \
 * -text [検索対象ファイル中に含まれる文字列]
 * -o [検索したファイルの出力ディレクトリ]
 * ※ -pattern, -dm, -dMは省略可. ただし検索範囲が広くなるため速度は低下する.
 */

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

    private static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
    // 共通オプション
    private static final String MODE = "-mode";
    private static final String DIRECTORY = "-dir";
    // 以下indexモードとfindモードで用途が異なるオプション
    // indexではzipファイル名パターン, findではテキストファイル名パターン
    private static final String FILE_NAME_PATTERN = "-pattern";
    //以下findモード用オプション
    private static final String INDEX = "-index";
    private static final String DATE_MIN = "-dm";
    private static final String DATE_MAX = "-dM";
    private static final String TEXT = "-text";
    private static final String OUTPUT_DIRECTORY = "-o";

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
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static class Indexer {

        private static final SimpleDateFormat SDF = new SimpleDateFormat(DATE_PATTERN);

        private void index(final File dir, final Pattern pattern) {

            for (File f : dir.listFiles()) {
                if (f.isFile() && f.getName().endsWith(".zip")) {
                    final Matcher m = pattern.matcher(f.getName());
                    if (m.find()) {
                        listZip(f);
                    }
                }
            }
        }

        private void listZip(File f) {
            System.out.println("==" + f.getName() + "==");
            try {
                ZipFile zip = new ZipFile(f.getAbsolutePath());
                Enumeration<? extends ZipEntry> enu = zip.entries();
                while (enu.hasMoreElements()) {
                    listFile((ZipEntry) enu.nextElement());
                }
            } catch (IOException e) {
                e.printStackTrace(); // TODO: 例外はそのままthrowする
            }
        }

        private void listFile(ZipEntry entry) throws IOException {
            if (!entry.isDirectory()) {
                System.out.println(SDF.format(new Date(entry.getTime())) + "\t" + entry.getName());
            }
        }
    }

    private static class Finder {

        private static final Pattern ZIP_FILE_PATTERN = Pattern.compile("^==(.*)==$");

        private void find(String text, File index, Pattern fileName, Date dateMin, Date dateMax, File inDir, File outDir)
                throws IOException, ParseException {
            final Pattern pattern = Pattern.compile(text);
            final BufferedReader reader = new BufferedReader(new FileReader(index));

            String line;
            String zipFile = null;
            List<String> files = new ArrayList<String>();
            while ((line = reader.readLine()) != null) {
                final Matcher zipMatcher = ZIP_FILE_PATTERN.matcher(line);
                if (zipMatcher.matches()) {
                    if (zipFile != null && files.size() > 0) {
                        pickUp(zipFile, files, pattern, inDir, outDir);
                    }
                    files = new ArrayList<String>();
                    zipFile = zipMatcher.group(1);
                } else {
                    final String[] info = line.split("\t");
                    final SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
                    final Date timeStamp = sdf.parse(info[0]);
                    final String entryName = info[1];

                    if ((dateMin == null || timeStamp.after(dateMin)) && (dateMax == null || timeStamp.before(dateMax))) {
                        final Matcher m = fileName.matcher(entryName);
                        if (m.find()) {
                            files.add(entryName);
                        }
                    }
                }
            }
            if (zipFile != null && files.size() > 0) {
                pickUp(zipFile, files, pattern, inDir, outDir);
            }
        }

        private void pickUp(String zipFile, List<String> files, Pattern pattern, File inDir, File outDir) throws
                IOException {
            System.out.println("SEARCHING IN ZIP: " + zipFile);
            final ZipInputStream zis = new ZipInputStream(new FileInputStream(new File(inDir, zipFile)));
            for (ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry()) {
                if (files.contains(ze.getName())) {
                    // System.out.println("CANDIDATE: " + ze.getName());
                    final File outFile = extract(outDir, ze.getName(), zis, ze.getTime());

                    checkText(outFile, pattern);
                }
            }
            zis.close();
            System.out.println("END SEARCHING IN ZIP: " + zipFile);
        }

        private File extract(final File outDir, final String entryName, final ZipInputStream zis,
                final long timeStamp) throws IOException {
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
            outFile.setLastModified(timeStamp);
            return outFile;
        }

        private void checkText(final File outFile, final Pattern pattern)
                throws IOException {
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
                // System.out.println("NO MACH,DELETE: " + outFile);
                outFile.delete();
            } else {
                System.out.println("HIT: " + outFile);
            }
        }
    }
}
