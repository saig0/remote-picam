package de.picam.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Paths;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.H264TrackImpl;

import de.picam.service.CameraService;

@RestController
public class CameraController {

	private static final String VIDEO_FILE = "/test.h264";

	@Autowired
	private CameraService service;

	@RequestMapping("/")
	String home() {
		return "Hello World!";
	}

	@RequestMapping("/start")
	public ResponseEntity<InputStreamResource> start() {
		InputStreamResource inputStreamResource = new InputStreamResource(
				service.start());
		// httpHeaders.setContentLength(contentLengthOfStream);
		return new ResponseEntity<InputStreamResource>(inputStreamResource,
				HttpStatus.OK);
	}

	@RequestMapping(value = "/live2", method = RequestMethod.GET)
	@ResponseBody
	public void live2(HttpServletResponse response) {
		try {
			final ServletOutputStream outputStream = response.getOutputStream();

			File file = Paths.get(getClass().getResource(VIDEO_FILE).toURI())
					.toFile();
			System.out.println("read from " + file.getPath());
			FileDataSourceImpl dataSource = new FileDataSourceImpl(file);
			H264TrackImpl h264Track = new H264TrackImpl(dataSource);
			Movie movie = new Movie();
			movie.addTrack(h264Track);

			Container mp4file = new DefaultMp4Builder().build(movie);
			WritableByteChannel channel = Channels.newChannel(outputStream);
			mp4file.writeContainer(channel);

			outputStream.close();
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (java.io.FileNotFoundException e) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
		} catch (Exception e) {
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}

	@RequestMapping("/live1")
	@ResponseBody
	public Resource live() throws FileNotFoundException, IOException,
			URISyntaxException {
		// InputStream inputStream = getClass().getResourceAsStream(VIDEO_FILE);

		File file = Paths.get(getClass().getResource(VIDEO_FILE).toURI())
				.toFile();
		System.out.println("read from " + file.getPath());

		H264TrackImpl h264Track = new H264TrackImpl(
				new FileDataSourceImpl(file));
		Movie movie = new Movie();
		movie.addTrack(h264Track);

		final Container mp4file = new DefaultMp4Builder().build(movie);

		File out = new File(file.getParent(), "output.mp4");
		FileChannel fc = new FileOutputStream(out).getChannel();
		mp4file.writeContainer(fc);
		fc.close();

		System.out.println("write output to " + out.getPath());

		return new InputStreamResource(new FileInputStream(out.getPath()));

	}

	@RequestMapping("/test")
	@ResponseBody
	public ResponseEntity<InputStreamResource> test() {
		HttpHeaders httpHeaders = new HttpHeaders();

		InputStreamResource inputStreamResource = new InputStreamResource(
				getClass().getResourceAsStream(VIDEO_FILE));
		return new ResponseEntity<InputStreamResource>(inputStreamResource,
				httpHeaders, HttpStatus.OK);
	}

	@RequestMapping("/test2")
	@ResponseBody
	public Resource getStream() {
		return new InputStreamResource(getClass().getResourceAsStream(
				VIDEO_FILE));

	}

	@RequestMapping(value = "/test3", method = RequestMethod.GET)
	@ResponseBody
	public void test4(HttpServletResponse response) {
		try {
			InputStream inputStream = getClass()
					.getResourceAsStream(VIDEO_FILE);
			ServletOutputStream outputStream = response.getOutputStream();
			IOUtils.copy(inputStream, outputStream);
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(outputStream);
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (java.io.FileNotFoundException e) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
		} catch (Exception e) {
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}
}
