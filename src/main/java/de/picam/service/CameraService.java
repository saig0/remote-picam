package de.picam.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CameraService {

	@Value("${video.directory:}")
	private String directory;

	private String fileName = "video%04d.h264";

	@Value("${video.framerate:25}")
	private String frameRate;

	@Value("${video.height:100}")
	private int height;

	@Value("${video.interval:10}")
	private int intervalInSeconds;

	private boolean isActive = false;

	@Value("${video.maxVideos:10}")
	private int maxVideos;

	@Value("${video.width:100}")
	private int width;

	public boolean isActive() {
		return isActive;
	}

	public InputStream openStream() {
		try {
			Process process = startProcess();
			BufferedInputStream in = new BufferedInputStream(
					process.getInputStream());
			return in;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void record(File output) {
		try {

			Process process = startProcess();
			BufferedInputStream bis = new BufferedInputStream(
					process.getInputStream());
			// Direct methode p.getInputStream().read() also possible, but
			// BufferedInputStream gives 0,5-1s better performance
			FileOutputStream fos = new FileOutputStream(output);

			int read = bis.read();
			fos.write(read);

			while (read != -1) {
				read = bis.read();
				fos.write(read);
			}

			bis.close();
			fos.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void recordWithInterval() {
		assertNotActive();
		isActive = true;
		System.out.println("start pi-cam");

		try {
			new ProcessBuilder("/bin/sh", "-c", "raspivid -n --width " + width
					+ " --height " + height + " --framerate " + frameRate
					+ " --segment " + (intervalInSeconds * 1000) + " --wrap "
					+ maxVideos + " --timeout 0 -o " + getVideoPath(fileName))
					.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		try {
			stopProcess();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void assertNotActive() {
		if (isActive) {
			throw new IllegalStateException(
					"can not start pi-cam because it already on!");
		}
	}

	private String getVideoPath(String fileName) {
		if (!directory.endsWith("/")) {
			directory = directory + "/";
		}
		return directory + fileName;
	}

	private Process startProcess() throws IOException {
		assertNotActive();
		isActive = true;

		System.out.println("start pi-cam");
		Process process = new ProcessBuilder("/bin/sh", "-c", "raspivid -w "
				+ width + " -h " + height + " -n -ih -t 0 -o -")
				.redirectOutput(Redirect.PIPE).start();
		return process;
	}

	private Process stopProcess() throws IOException {
		isActive = false;
		System.out.println("stop pi-cam");
		Process process = new ProcessBuilder("/bin/sh", "-c",
				"killall raspivid").start();
		return process;
	}
}
