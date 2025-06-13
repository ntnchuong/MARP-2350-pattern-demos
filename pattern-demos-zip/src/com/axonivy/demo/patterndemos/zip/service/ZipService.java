package com.axonivy.demo.patterndemos.zip.service;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.hibernate.engine.jdbc.StreamUtils;

import ch.ivyteam.ivy.scripting.objects.File;

public class ZipService {

	private static final String DUPLICATE_FILE_NAME_FORMAT = "%s(%d).%s";

	private static final ZipService INSTANCE = new ZipService();

	public static ZipService get() {
		return INSTANCE;
	}

	/**
	 * Zip files into a exist zip file
	 * 
	 * @param zipFile
	 * @param dataFiles
	 * @throws IOException
	 */
	public void addAllToZipFile(File zipFile, List<Entry<String, byte[]>> dataFiles) throws IOException {
		if (isEmpty(dataFiles)) {
			return;
		}

		final var dataFileWithDifferentFileName = buildFileNameWithIndex(dataFiles);

		for (final Map.Entry<String, byte[]> file : dataFileWithDifferentFileName.entrySet()) {
			addToZipFile(zipFile, file.getKey(), file.getValue());
		}
	}

	/**
	 * Zip all file to a zip input stream
	 */
	public InputStream zipAllFiles(List<Entry<String, byte[]>> dataFiles) throws Exception {
		final var dataFileWithDifferentFileName = buildFileNameWithIndex(dataFiles);
		return compress(dataFileWithDifferentFileName);
	}

	/**
	 * Unzip file to a folder
	 */
	public void unzip(File zipFile, String destDirectory) throws IOException {
		java.io.File destDir = new java.io.File(destDirectory);
		if (!destDir.exists()) {
			destDir.mkdir();
		}
		ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile.getAbsolutePath()));
		ZipEntry entry = zipIn.getNextEntry();
		// iterates over entries in the zip file
		while (entry != null) {
			String filePath = destDirectory + java.io.File.separator + entry.getName();
			if (!entry.isDirectory()) {
				// if the entry is a file, extracts it
				extractFile(zipIn, filePath);
			} else {
				// if the entry is a directory, make the directory
				java.io.File dir = new java.io.File(filePath);
				dir.mkdir();
			}
			zipIn.closeEntry();
			entry = zipIn.getNextEntry();
		}
		zipIn.close();
	}

	public List<Triple<String, Long, Long>> getFilesInZip(File file) throws IOException {
		List<Triple<String, Long, Long>> result = new ArrayList<>();

		try (ZipFile zipFile = new ZipFile(file.getAbsolutePath())) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				// Check if entry is a directory
				if (!entry.isDirectory()) {
					var fileInfor = ImmutableTriple.of(entry.getName(), entry.getSize(), entry.getCompressedSize());
					result.add(fileInfor);
				}
			}
		}
		return result;
	}

	private InputStream compress(final Map<String, byte[]> data) throws Exception {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeToOutputStream(out, data);
		out.close();
		return new ByteArrayInputStream(out.toByteArray());
	}

	private void writeToOutputStream(OutputStream out, Map<String, byte[]> data) throws IOException {
		ZipOutputStream zos = new ZipOutputStream(out);
		for (final Map.Entry<String, byte[]> file : data.entrySet()) {

			final var inputStream = new ByteArrayInputStream(file.getValue());
			final var zipEntry = new ZipEntry(file.getKey());
			zipEntry.setSize(inputStream.available());

			zos.putNextEntry(zipEntry);
			StreamUtils.copy(inputStream, zos);

			zos.closeEntry();
		}
		zos.finish();
		zos.close();
	}

	private void addToZipFile(File zipFile, String fileName, byte[] data) throws IOException {
		Map<String, String> env = new HashMap<>();
		env.put("create", "true");

		Path zipPath = Paths.get(zipFile.getAbsolutePath());
		URI uri = URI.create("jar:" + zipPath.toUri());

		try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
			Path nf = fs.getPath(fileName);
			Files.write(nf, data, StandardOpenOption.CREATE);
		}
	}

	private Map<String, byte[]> buildFileNameWithIndex(List<Entry<String, byte[]>> dataFiles) {
		var files = dataFiles.stream().collect(
				Collectors.groupingBy(it -> it.getKey(), Collectors.mapping(it -> it.getValue(), Collectors.toList())));

		final var dataFileWithDifferentFileName = new HashMap<String, byte[]>();
		for (Map.Entry<String, List<byte[]>> file : files.entrySet()) {
			for (int i = 0; i < file.getValue().size(); i++) {

				final var contentFilename = buildFileNameWithIndex(file.getKey(), i);
				dataFileWithDifferentFileName.put(contentFilename, file.getValue().get(i));
			}
		}
		return dataFileWithDifferentFileName;
	}

	private String buildFileNameWithIndex(final String filename, final int index) {
		if (index > 0) {
			final var extension = FilenameUtils.getExtension(filename);
			final var name = FilenameUtils.getBaseName(filename);
			return String.format(DUPLICATE_FILE_NAME_FORMAT, name, index, extension);
		}
		return filename;
	}

	/**
	 * Extracts a zip entry (file entry)
	 * 
	 * @param zipIn
	 * @param filePath
	 * @throws IOException
	 */
	private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
		byte[] bytesIn = new byte[2048];
		int read = 0;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
	}
}
