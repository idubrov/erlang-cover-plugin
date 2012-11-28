package hudson.plugins.erlangcover.renderers;

import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.erlangcover.targets.CoveragePaint;
import hudson.remoting.VirtualChannel;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.*;

/**
 * TODO javadoc.
 * 
 * @author Stephen Connolly
 * @since 31-Aug-2007 16:52:25
 */
public class SourceCodePainter implements FilePath.FileCallable<Boolean>, Serializable {
	private final Set<String> sourcePaths;
	private final Map<String, CoveragePaint> paint;
	private final FilePath destination;
	private final BuildListener listener;
	private final SourceEncoding sourceEncoding;

	public SourceCodePainter(FilePath destination, Set<String> sourcePaths, Map<String, CoveragePaint> paint, BuildListener listener,
			SourceEncoding sourceEncoding) {
		this.destination = destination;
		this.sourcePaths = sourcePaths;
		this.paint = paint;
		this.listener = listener;
		this.sourceEncoding = sourceEncoding;
	}

	public void paintSourceCode(File source, CoveragePaint paint, FilePath canvas) throws IOException, InterruptedException {
		OutputStream os = null;

		FileInputStream is = null;
		InputStreamReader reader = null;
		BufferedReader input = null;
		OutputStreamWriter bos = null;

		BufferedWriter output = null;
		int line = 0;
		try {
			canvas.getParent().mkdirs();
			os = canvas.write();
			is = new FileInputStream(source);
			reader = new InputStreamReader(is, getSourceEncoding().getEncodingName());
			input = new BufferedReader(reader);
			bos = new OutputStreamWriter(os, "UTF-8");
			output = new BufferedWriter(bos);
			String content;
			while ((content = input.readLine()) != null) {
				line++;

				if (paint.isPainted(line)) {
					final int hits = paint.getHits(line);
					if (paint.getHits(line) > 0) {
    					output.write("<tr class=\"coverFull\">\n");
					} else {
						output.write("<tr class=\"coverNone\">\n");
					}
					output.write("<td class=\"line\"><a name='" + line + "'/>" + line + "</td>\n");
					output.write("<td class=\"hits\">" + hits + "</td>\n");
				} else {
					output.write("<tr class=\"noCover\">\n");
					output.write("<td class=\"line\"><a name='" + line + "'/>" + line + "</td>\n");
					output.write("<td class=\"hits\"/>\n");
				}
				output.write("<td class=\"code\">"
						+ content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "").replace("\r", "").replace(" ",
								"&nbsp;").replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;") + "</td>\n");
				output.write("</tr>\n");
			}

		} finally {
			if (output != null) {
				output.close();
			}
			if (bos != null) {
				bos.close();
			}
			if (input != null) {
				input.close();
			}
			if (is != null) {
				is.close();
			}

			
			if (os != null) {
				os.close();
			}
			if (reader != null) {
				reader.close();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Boolean invoke(File workspaceDir, VirtualChannel channel) throws IOException {
		final List<File> trialPaths = new ArrayList<File>(sourcePaths.size());
        trialPaths.add(workspaceDir);
		for (String sourcePath : sourcePaths) {
			final File trialPath = new File(sourcePath);
			if (trialPath.exists()) {
				trialPaths.add(trialPath);
			}
			final File trialPath2 = new File(workspaceDir, sourcePath);
			if (trialPath2.exists() && !trialPath2.equals(trialPath)) {
				trialPaths.add(trialPath2);
			}
		}
		for (Map.Entry<String, CoveragePaint> entry : paint.entrySet()) {
			// first see if we can find the file directly
			File source = lookupSource(trialPaths, entry.getKey());
			if (source.isFile()) {
				try {
					paintSourceCode(source, entry.getValue(), destination.child(entry.getKey()));
				} catch (IOException e) {
					// We made our best shot at generating painted source code,
					// but alas, we failed. Log the error and continue. We
					// should not fail the build just because we cannot paint
					// one file.
					e.printStackTrace(listener.error("ERROR: Failure to paint " + source + " to " + destination));
				} catch (InterruptedException e) {
					return Boolean.FALSE;
				}
			}
		}
		return Boolean.TRUE;
	}

    private File lookupSource(Collection<File> trialPaths, String relativePath) throws IOException {
        for (File path : trialPaths) {
            File source = new File(path, relativePath);
            if (source.exists()) {
                return source;
            }
        }

        // Try searching files
        for (File dir : trialPaths) {
            FileSet fs = Util.createFileSet(dir, "**/" + relativePath, null);
            DirectoryScanner ds = fs.getDirectoryScanner(new Project());
            String[] files = ds.getIncludedFiles();
            if (files.length > 0) {
                return new File(dir, files[0]);
            }
        }
        return null;
    }

	public SourceEncoding getSourceEncoding() {
		if (sourceEncoding == null) {
			return SourceEncoding.UTF_8;
		}
		return sourceEncoding;
	}

    public static File paintedSourcesDirectory(AbstractBuild<?, ?> build) {
        return new File(build.getProject().getRootDir(), "cover/");
    }
}
