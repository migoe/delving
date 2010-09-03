package eu.europeana.sip.model;

import com.thoughtworks.xstream.XStream;
import eu.europeana.sip.core.DataSetDetails;
import org.apache.log4j.Logger;

import javax.swing.SwingUtilities;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Implementing FileSet, handling all the files related to the original xml file.]
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class FileSetImpl implements FileSet {
    private final Logger LOG = Logger.getLogger(getClass());
    private File inputFile, statisticsFile, mappingFile, outputFile, discardedFile, reportFile, dataSetDetailsFile;
    private UserNotifier userNotifier;

    public FileSetImpl(File inputFile) {
        this.inputFile = inputFile;
        this.statisticsFile = new File(inputFile.getParentFile(), inputFile.getName() + ".statistics");
        this.mappingFile = new File(inputFile.getParentFile(), inputFile.getName() + ".mapping");
        this.outputFile = new File(inputFile.getParentFile(), inputFile.getName() + ".normalized");
        this.discardedFile = new File(inputFile.getParentFile(), inputFile.getName() + ".discarded");
        this.reportFile = new File(inputFile.getParentFile(), inputFile.getName() + ".report");
        this.dataSetDetailsFile = new File(inputFile.getParentFile(), inputFile.getName() + ".details");
    }

    @Override
    public void setExceptionHandler(UserNotifier handler) {
        this.userNotifier = handler;
    }

    @Override
    public String getName() {
        return inputFile.getName();
    }

    @Override
    public String getAbsolutePath() {
        return inputFile.getAbsolutePath();
    }

    @Override
    public boolean isValid() {
        return inputFile.exists();
    }

    @Override
    public File getDirectory() {
        return inputFile.getParentFile();
    }

    @Override
    public InputStream getInputStream() {
        checkWorkerThread();
        try {
            return new FileInputStream(inputFile);
        }
        catch (FileNotFoundException e) {
            userNotifier.tellUser("Unable to open input file", e);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Statistics> getStatistics() {
        checkWorkerThread();
        if (statisticsFile.exists()) {
            try {
                ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(statisticsFile)));
                List<Statistics> statisticsList = (List<Statistics>) in.readObject();
                in.close();
                return statisticsList;
            }
            catch (Exception e) {
                userNotifier.tellUser("Unable to read statistics, please re-analyze", e);
                if (statisticsFile.delete()) {
                    LOG.warn("Cannot delete statistics file");
                }
            }
        }
        return null;
    }

    @Override
    public void setStatistics(List<Statistics> statisticsList) {
        checkWorkerThread();
        try {
            ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(statisticsFile)));
            out.writeObject(statisticsList);
            out.close();
        }
        catch (IOException e) {
            userNotifier.tellUser("Unable to save statistics file", e);
        }
    }

    @Override
    public String getMapping() {
        checkWorkerThread();
        if (mappingFile.exists()) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(mappingFile));
                StringBuilder mapping = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    mapping.append(line).append('\n');
                }
                in.close();
                return mapping.toString();
            }
            catch (IOException e) {
                userNotifier.tellUser("Unable to read mapping file", e);
            }
        }
        return "";
    }

    @Override
    public void setMapping(String mapping) {
        checkWorkerThread();
        try {
            FileWriter out = new FileWriter(mappingFile);
            out.write(mapping);
            out.close();
        }
        catch (IOException e) {
            userNotifier.tellUser("Unable to save mapping file", e);
        }
    }

    @Override
    public DataSetDetails getDataSetDetails() {
        checkWorkerThread();
        DataSetDetails details = null;
        if (dataSetDetailsFile.exists()) {
            XStream stream = new XStream();
            stream.processAnnotations(DataSetDetails.class);
            try {
                FileInputStream fis = new FileInputStream(dataSetDetailsFile);
                details = (DataSetDetails) stream.fromXML(fis);
                fis.close();
            }
            catch (IOException e) {
                userNotifier.tellUser("Unable to load dataset details file", e);
            }
        }
        if (details == null) {
            details = new DataSetDetails();
        }
        return details;
    }

    @Override
    public void setDataSetDetails(DataSetDetails details) {
        checkWorkerThread();
        try {
            XStream stream = new XStream();
            stream.processAnnotations(DataSetDetails.class);
            FileOutputStream fos = new FileOutputStream(dataSetDetailsFile);
            stream.toXML(details, fos);
            fos.close();
        }
        catch (IOException e) {
            userNotifier.tellUser("Unable to save dataset details file", e);
        }
    }

    @Override
    public File createZipFile(String zipFileName) {
        checkWorkerThread();
        File zipFile = new File(inputFile.getParentFile(), zipFileName + ".zip");
        if (zipFile.exists()) {
            if (!zipFile.delete()) {
                userNotifier.tellUser("Unable to delete zip file");
            }
        }
        try {
            buildZipFile(zipFile);
            return zipFile;
        }
        catch (IOException e) {
            userNotifier.tellUser("Unable to build zip file", e);
            LOG.warn("Unable to build zip file " + zipFile.getAbsolutePath(), e);
        }
        return null;
    }

    @Override
    public Report getReport() {
        if (reportFile.exists()) {
            try {
                return new ReportImpl();
            }
            catch (Exception e) {
                removeOutput();
                return null;
            }
        }
        else {
            return null;
        }
    }

    @Override
    public Output prepareOutput() {
        return new OutputImpl();
    }

    public String toString() {
        return getName();
    }

    private class OutputImpl implements Output {
        private Writer outputWriter, discardedWriter, reportWriter;
        private int recordsNormalized, recordsDiscarded;

        private OutputImpl() {
            checkWorkerThread();
            removeOutput();
            try {
                this.outputWriter = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8");
                this.discardedWriter = new OutputStreamWriter(new FileOutputStream(discardedFile), "UTF-8");
                this.reportWriter = new OutputStreamWriter(new FileOutputStream(reportFile), "UTF-8");
            }
            catch (FileNotFoundException e) {
                userNotifier.tellUser("Unable to open output file " + outputFile.getAbsolutePath(), e);
            }
            catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Writer getOutputWriter() {
            return outputWriter;
        }

        @Override
        public Writer getDiscardedWriter() {
            return discardedWriter;
        }

        @Override
        public void recordNormalized() {
            recordsNormalized++;
        }

        @Override
        public void recordDiscarded() {
            recordsDiscarded++;
        }

        @Override
        public void close(boolean abort) {
            if (abort) {
                removeOutput();
            }
            else {
                try {
                    outputWriter.close();
                    discardedWriter.close();
                    Properties properties = new Properties();
                    properties.put("normalizationDate", String.valueOf(System.currentTimeMillis()));
                    properties.put("recordsNormalized", String.valueOf(recordsNormalized));
                    properties.put("recordsDiscarded", String.valueOf(recordsDiscarded));
                    properties.store(reportWriter, "Normalization Report");
                    reportWriter.close();
                }
                catch (IOException e) {
                    userNotifier.tellUser("Unable to close output files", e);
                }
            }
        }
    }

    private class ReportImpl implements Report {
        private Properties properties = new Properties();

        private ReportImpl() throws Exception {
            try {
                InputStream reportStream = new FileInputStream(reportFile);
                properties.load(reportStream);
            }
            catch (Exception e) {
                userNotifier.tellUser("Unable to load report file", e);
                throw e;
            }
        }

        @Override
        public Date getNormalizationDate() {
            String s = (String) properties.get("normalizationDate");
            if (s != null) {
                return new Date(Long.parseLong(s));
            }
            else {
                return new Date();
            }
        }

        @Override
        public int getRecordsNormalized() {
            String s = (String) properties.get("recordsNormalized");
            if (s != null) {
                return Integer.parseInt(s);
            }
            else {
                return 0;
            }
        }

        @Override
        public int getRecordsDiscarded() {
            String s = (String) properties.get("recordsDiscarded");
            if (s != null) {
                return Integer.parseInt(s);
            }
            else {
                return 0;
            }
        }

        @Override
        public void clear() {
            checkWorkerThread();
            removeOutput();
        }

    }

    private void buildZipFile(File zipFile) throws IOException {
        OutputStream outputStream = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        stream(dataSetDetailsFile, zos);
        stream(inputFile, zos);
        stream(mappingFile, zos);
        zos.close();
    }

    private void stream(File file, ZipOutputStream zos) throws IOException {
        InputStream in = new FileInputStream(file);
        zos.putNextEntry(new ZipEntry(file.getName()));
        byte[] buffer = new byte[4096];
        int length;
        while ((length = in.read(buffer)) > 0) {
            zos.write(buffer, 0, length);
        }
        zos.closeEntry();
    }

    private void removeOutput() {
        if (outputFile.exists() && !outputFile.delete()) {
            LOG.warn("Unable to delete " + outputFile);
        }
        if (discardedFile.exists() && !discardedFile.delete()) {
            LOG.warn("Unable to delete " + discardedFile);
        }
        if (reportFile.exists() && !reportFile.delete()) {
            LOG.warn("Unable to delete " + reportFile);
        }
    }

    private static void checkWorkerThread() {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new RuntimeException("Expected Worker thread");
        }
    }
}
