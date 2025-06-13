package com.axonivy.demo.patterndemos.zip.ui;

import static java.util.Optional.ofNullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.primefaces.event.FilesUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.demo.patterndemos.zip.model.ZipModel;
import com.axonivy.demo.patterndemos.zip.service.ZipService;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.scripting.objects.Binary;
import ch.ivyteam.ivy.scripting.objects.File;

public class ZipFileCtrl {
	private List<ZipModel> zipModels;
	private File zipFile;
	private String unzipFolder;

	public List<ZipModel> getZipModels() {
		return zipModels;
	}

	public File getZipFile() {
		return zipFile;
	}

	public String getUnzipFolder() {
		return unzipFolder;
	}

	public void init() {
		this.zipModels = new ArrayList<>();
		this.unzipFolder = null;
	}

	public void onFilesUpload(FilesUploadEvent event) {
		List<Entry<String, byte[]>> data = event.getFiles().getFiles().stream()
				.map(it -> Map.entry(it.getFileName(), it.getContent())).toList();

		if (this.zipFile == null) {
			this.zipFile = createZipFile(data);
		} else {
			addFileToZip(this.zipFile, data);
		}

		this.zipModels = getAllFileInZip(zipFile);
	}

	public StreamedContent downloadZipFile() {
		try {
			InputStream inputStream = new FileInputStream(this.zipFile.getJavaFile());
			StreamedContent streamedContent = DefaultStreamedContent.builder().name(this.zipFile.getName())
					.stream(() -> inputStream).build();
			return streamedContent;
		} catch (Exception e) {
			Ivy.log().error("Error when streaming file {0} is error {1}", e, this.zipFile.getName(), e.getMessage());
		}

		return null;
	}

	public void onUnzipFile() {
		String folderName = RandomStringUtils.secure().nextAlphanumeric(10);
		try {
			File outFolder = new File(folderName);
			this.unzipFolder = outFolder.getAbsolutePath();

			ZipService.get().unzip(zipFile, this.unzipFolder);
		} catch (IOException e) {
			Ivy.log().error("Error when un-zip file {0} is error {1}", e, this.zipFile.getName(), e.getMessage());
		}
	}

	public String getTotalSize() {
		Long size = ListUtils.emptyIfNull(this.zipModels).stream().map(ZipModel::getSize).reduce(0L, Long::sum);
		return FileUtils.byteCountToDisplaySize(Optional.ofNullable(size).orElse(0L));
	}

	public String getTotalCompressedSize() {
		Long size = ListUtils.emptyIfNull(this.zipModels).stream().map(ZipModel::getCompressedSize).reduce(0L,
				Long::sum);
		return FileUtils.byteCountToDisplaySize(Optional.ofNullable(size).orElse(0L));
	}

	public String getFileSizeDisplay() {
		return FileUtils.byteCountToDisplaySize(ofNullable(this.zipFile.getJavaFile().length()).orElse(0L));
	}
	
	private List<ZipModel> getAllFileInZip(File zipFile) {
		try {
			List<Triple<String, Long, Long>> filesInZip = ZipService.get().getFilesInZip(zipFile);
			return filesInZip.stream().map(it -> new ZipModel(it.getLeft(), it.getMiddle(), it.getRight())).toList();

		} catch (IOException e) {
			Ivy.log().error("Error when get all files", e);
		}
		return Collections.emptyList();
	}

	private void addFileToZip(File zipFile, List<Entry<String, byte[]>> data) {
		try {
			ZipService.get().addAllToZipFile(zipFile, data);
		} catch (IOException e) {
			Ivy.log().error("Error when add file to zip", e);
		}
	}

	private File createZipFile(List<Entry<String, byte[]>> data) {
		try {
			InputStream zipInputStream = ZipService.get().zipAllFiles(data);
			File file = new File(UUID.randomUUID().toString() + ".zip");
			file.writeBinary(new Binary(zipInputStream.readAllBytes()));
			return file;
		} catch (Exception e) {
			Ivy.log().error("Error when create a zip file", e);
		}
		return null;
	}

}
