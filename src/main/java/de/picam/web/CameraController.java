package de.picam.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Paths;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import de.picam.service.CameraService;
import de.picam.service.Mp4VideoConverter;
import de.picam.service.VideoRepository;

@RestController
public class CameraController {

	private static final String VIDEO_FILE = "live.h264";

	@Autowired
	private CameraService service;

	@Autowired
	private Mp4VideoConverter videoConverter;

	@Autowired
	private VideoRepository videoRepository;

	@RequestMapping("/live/stream")
	@ResponseBody
	public void liveStream(HttpServletResponse response)
			throws FileNotFoundException, IOException, URISyntaxException {
		if (service.isActive()) {
			File video = videoRepository.getLastVideo();
			tryToWriteVideoToResponse(video, response);
		}
	}

	/**
	 * record camera and write to file
	 */
	@RequestMapping(value = "/record", method = RequestMethod.POST)
	public void startCam() throws FileNotFoundException, IOException,
			URISyntaxException {
		System.out.println("record camera");
		service.record(Paths.get(VIDEO_FILE).toFile());
	}

	@RequestMapping(value = "/live/start", method = RequestMethod.POST)
	public void startLiveStream() {
		if (!service.isActive()) {
			videoRepository.reset();
			service.recordWithInterval();
		}
	}

	/**
	 * stop camera
	 */
	@RequestMapping(value = "/stop", method = RequestMethod.POST)
	public void stopCam() {
		System.out.println("stop camera");
		service.stop();
	}

	@RequestMapping("/stream")
	@ResponseBody
	public Resource stream() {
		File file = Paths.get(VIDEO_FILE).toFile();
		System.out.println("stream from " + file.getPath());
		return new InputStreamResource(getClass().getResourceAsStream(
				file.getPath()));

	}

	@RequestMapping("/stream/mp4")
	@ResponseBody
	public void streamMp4(HttpServletResponse response)
			throws FileNotFoundException, IOException, URISyntaxException {
		System.out.println("read h264, covert to mp4 and stream");

		File video = Paths.get(VIDEO_FILE).toFile();
		tryToWriteVideoToResponse(video, response);
	}

	private void tryToWriteVideoToResponse(File video,
			HttpServletResponse response) {
		try {
			writeVideoToResponse(video, response);
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (java.io.FileNotFoundException e) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			e.printStackTrace();
		} catch (Exception e) {
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			e.printStackTrace();
		}
	}

	private void writeVideoToResponse(File video, HttpServletResponse response)
			throws IOException, FileNotFoundException {
		System.out.println("stream from " + video.getAbsolutePath());

		final ServletOutputStream outputStream = response.getOutputStream();
		WritableByteChannel channel = Channels.newChannel(outputStream);

		videoConverter.createMp4(video, channel);

		outputStream.close();
	}

}
