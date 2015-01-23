package de.picam.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CameraService {

	@Value("${video.count:10}")
	private int count;

	@Value("${video.directory:}")
	private String directory;

	private ScheduledExecutorService executor;

	private final String fileName = "video%04d.h264";

	@Value("${video.interval:10}")
	private int intervalInSeconds;

	private boolean isActive = false;
	private int lastVideo = 0;

	public File getLastVideo() {
		String path = directory + String.format(fileName, lastVideo);
		File video = Paths.get(path).toFile();
		return video;
	}

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
			new ProcessBuilder("/bin/sh", "-c",
					"raspivid -n --width 1920 --height 1080 --framerate 25 --segment "
							+ (intervalInSeconds * 1000) + " --wrap " + count
							+ " --timeout 0 -o " + fileName).start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		final CountDownLatch countDownLatch = new CountDownLatch(1);

		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(new Runnable() {

			public void run() {
				lastVideo = 1 + (lastVideo % count);

				countDownLatch.countDown();
			}
		}, intervalInSeconds, intervalInSeconds, TimeUnit.SECONDS);

		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		try {
			stopProcess();
			reset();
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

	private void reset() {
		lastVideo = 0;
		if (executor != null) {
			executor.shutdown();
		}
	}

	private Process startProcess() throws IOException {
		assertNotActive();
		isActive = true;

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
}
