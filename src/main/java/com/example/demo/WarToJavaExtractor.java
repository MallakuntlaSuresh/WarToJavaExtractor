package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WarToJavaExtractor {

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.out.println("Usage: mvn exec:java -Dexec.args=\"<input.war> <output-dir>\"");
			return;
		}

		String warFilePath = args[0];
		String outputDir = args[1];

		Path tempDir = Files.createTempDirectory("war_extract");
		unzip(warFilePath, tempDir.toFile());

		Files.walk(tempDir).filter(path -> path.toString().endsWith(".class")).forEach(classFile -> {
			try {
				Path relativePath = tempDir.relativize(classFile);
				String javaFileName = relativePath.toString().replace(".class", ".java");
				Path outputFile = Paths.get(outputDir, javaFileName);

				Files.createDirectories(outputFile.getParent());
				ProcessBuilder pb = new ProcessBuilder("java", "-jar", "C:/Users/sures/Downloads/cfr-0.152.jar",
						classFile.toString(), "--outputdir", outputFile.getParent().toString());
				pb.inheritIO();
				Process p = pb.start();
				p.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		System.out.println("Decompiled sources are in: " + outputDir);
	}

	private static void unzip(String zipFilePath, File destDir) throws IOException {
		byte[] buffer = new byte[1024];
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				File newFile = newFile(destDir, zipEntry);
				if (zipEntry.isDirectory()) {
					if (!newFile.isDirectory() && !newFile.mkdirs()) {
						throw new IOException("Failed to create dir " + newFile);
					}
				} else {
					File parent = newFile.getParentFile();
					if (!parent.isDirectory() && !parent.mkdirs()) {
						throw new IOException("Failed to create dir " + parent);
					}

					try (FileOutputStream fos = new FileOutputStream(newFile)) {
						int len;
						while ((len = zis.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
					}
				}
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
		}
	}

	private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
		File destFile = new File(destinationDir, zipEntry.getName());
		String destDirPath = destinationDir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry outside target dir: " + zipEntry.getName());
		}

		return destFile;
	}
}