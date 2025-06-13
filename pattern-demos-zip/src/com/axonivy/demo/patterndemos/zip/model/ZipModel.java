package com.axonivy.demo.patterndemos.zip.model;

import java.util.Optional;

import org.apache.commons.io.FileUtils;

public class ZipModel {
	private String fileName;
	private Long size;
	private Long compressedSize;

	public ZipModel(String fileName, Long size, Long compressedSize) {
		super();
		this.fileName = fileName;
		this.size = size;
		this.compressedSize = compressedSize;
	}

	public String getFileName() {
		return fileName;
	}
	
	public Long getSize() {
		return size;
	}
	
	public Long getCompressedSize() {
		return compressedSize;
	}
	
	public String getSizeDisplay() {
		return FileUtils.byteCountToDisplaySize(Optional.ofNullable(this.size).orElse(0L));
	}
	
	public String getCompressedSizeDisplay() {
		return FileUtils.byteCountToDisplaySize(Optional.ofNullable(this.compressedSize).orElse(0L));
	}
}
