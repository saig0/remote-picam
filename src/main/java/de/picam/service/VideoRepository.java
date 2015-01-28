package de.picam.service;

import java.io.File;
import java.io.IOException;
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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VideoRepository {

	private File currentVideo;

	@Value("${video.directory:}")
	private String directory;

	private ExecutorService executor = Executors.newSingleThreadExecutor();

	private File lastVideo;

	private CountDownLatch lastVideoCounter;

	private String VIDEO_FILE_PREFIX = "video";

	public File getLastVideo() {
		if (!hasLastVideo()) {
			waitForLastVideo();
		}
		File absolutePathToVideo = new File(directory, lastVideo.toString());
		return absolutePathToVideo;
	}

	public boolean hasLastVideo() {
		return lastVideo != null;
	}

	public void reset() {
		currentVideo = null;
		lastVideo = null;

		lastVideoCounter = new CountDownLatch(1);
	}

	@PostConstruct
	public void startWatching() {
		lastVideoCounter = new CountDownLatch(1);

		executor.execute(new DirectoryWatcher());
	}

	private void waitForLastVideo() {
		System.out.println("wait for video");
		try {
			lastVideoCounter.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private final class DirectoryWatcher implements Runnable {

		public void run() {
			try {
				watchDirectory();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void processEvent(WatchEvent<Path> event) {
			File modifiedFile = event.context().toFile();

			if (modifiedFile.getName().startsWith(VIDEO_FILE_PREFIX)
					&& !modifiedFile.equals(currentVideo)
					&& !modifiedFile.equals(lastVideo)) {

				System.out.println("new video: " + modifiedFile.toString());

				lastVideo = currentVideo;
				currentVideo = modifiedFile;

				if (lastVideo != null) {
					lastVideoCounter.countDown();
				}
			}
		}

		private void processEvents(WatchService watchService)
				throws InterruptedException {
			WatchKey watchKey = watchService.take();
			for (final WatchEvent<?> event : watchKey.pollEvents()) {
				processEvent((WatchEvent<Path>) event);
			}
			watchKey.reset();
		}

		private void watchDirectory() throws IOException {
			WatchService watchService = FileSystems.getDefault()
					.newWatchService();
			Paths.get(directory).register(watchService,
					StandardWatchEventKinds.ENTRY_MODIFY);

			while (true) {
				try {
					processEvents(watchService);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
