package de.picam.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CameraService {

	private File currentVideo;

	@Value("${video.directory:}")
	private String directory;

	private ExecutorService executor = Executors.newSingleThreadExecutor();

	@Value("${video.filename:video%04d.h264}")
	private String fileName;

	@Value("${video.framerate:25}")
	private String frameRate;

	@Value("${video.height:100}")
	private int height;

	@Value("${video.interval:10}")
	private int intervalInSeconds;

	private boolean isActive = false;

	private File lastVideo;
	@Value("${video.maxVideos:10}")
	private int maxVideos;

	@Value("${video.width:100}")
	private int width;

	public File getLastVideo() {
		File absolutePathToVideo = new File(getVideoPath(lastVideo.toString()));
		return absolutePathToVideo;
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
			new ProcessBuilder("/bin/sh", "-c", "raspivid -n --width " + width
					+ " --height " + height + " --framerate " + frameRate
					+ " --segment " + (intervalInSeconds * 1000) + " --wrap "
					+ maxVideos + " --timeout 0 -o " + getVideoPath(fileName))
					.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		startWatcherService();
	}

	public void stop() {
		try {
			stopProcess();
			currentVideo = null;
			lastVideo = null;
			executor.shutdown();
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

	private void startWatcherService() {
		final CountDownLatch counter = new CountDownLatch(1);

		executor = Executors.newSingleThreadExecutor();
		executor.execute(new Runnable() {

			public void run() {
				try {
					WatchService watcher = FileSystems.getDefault()
							.newWatchService();
					Paths.get(directory).register(watcher,
							StandardWatchEventKinds.ENTRY_MODIFY);
					for (;;) {
						WatchKey watchKey = watcher.take();
						for (WatchEvent<?> e : watchKey.pollEvents()) {
							WatchEvent<Path> event = (WatchEvent<Path>) e;
							File modifiedFile = event.context().toFile();

							// FIXME check the name of modified file
							if (modifiedFile.getName().startsWith("video")
									&& !modifiedFile.equals(currentVideo)
									&& !modifiedFile.equals(lastVideo)) {
								System.out.println("new video: "
										+ modifiedFile.getAbsolutePath());

								lastVideo = currentVideo;
								currentVideo = modifiedFile;

								if (lastVideo != null) {
									counter.countDown();
								}
							}

						}
						watchKey.reset();
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		System.out.println("wait for video");
		try {
			counter.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("video is ready!");
	}

	private Process stopProcess() throws IOException {
		isActive = false;
		System.out.println("stop pi-cam");
		Process process = new ProcessBuilder("/bin/sh", "-c",
				"killall raspivid").start();
		return process;
	}
}
