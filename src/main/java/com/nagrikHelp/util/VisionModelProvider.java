package com.nagrikHelp.util;

public interface VisionModelProvider {
	VisionResult analyze(String imageBase64) throws Exception;
	String name();
}
