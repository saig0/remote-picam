package de.picam.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
public class CameraService {

	private boolean isActive = false;

	public boolean isActive() {
		return isActive;
	}

	private Process startProcess() throws IOException {
		System.out.println("start pi-cam");
		Process process = new ProcessBuilder("/bin/sh", "-c",
				"raspivid -w 100 -h 100 -n -ih -t 0 -o -").redirectOutput(
				Redirect.PIPE).start();
		return process;
	}

	private Process stopProcess() throws IOException {
		System.out.println("stop pi-cam");
		Process process = new ProcessBuilder("/bin/sh", "-c",
				"killall raspivid").start();
		return process;
	}

	public InputStream openStream() {
		try {
			Process process = startProcess();

			System.out.println("start reading");

			BufferedInputStream in = new BufferedInputStream(
					process.getInputStream());
			return in;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void stop() {
		try {
			stopProcess();
			// reset
			last = 0;
			if (executor != null) {
				executor.shutdown();
			}
		} catch (IOException e) {
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

			System.out.println("start writing");
			int read = bis.read();
			fos.write(read);

			while (read != -1) {
				read = bis.read();
				fos.write(read);
			}
			System.out.println("end writing");
			bis.close();
			fos.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int last = 0;
	private final String fileName = "video000";
	private ScheduledExecutorService executor;

	public void recordToActiveFile() {
		System.out.println("start pi-cam");

		// new ProcessBuilder(
		// "/bin/sh",
		// "-c",
		// "raspivid -n --width 1920 --height 1080 --framerate 25 --segment 1000 --wrap 4 --timeout 0 -o video%04d.h264")
		// .start();

		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(new Runnable() {

			public void run() {
				last = 1 + (last % 4);
			}
		}, 1, 1, TimeUnit.SECONDS);

		isActive = true;
	}

	public File getLastVideo() {
		File lastVideo = Paths.get(fileName + last + ".h264").toFile();
		System.out.println("last file " + lastVideo.getAbsolutePath());
		return lastVideo;
	}
}
