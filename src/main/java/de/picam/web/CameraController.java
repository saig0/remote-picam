package de.picam.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.H264TrackImpl;

import de.picam.service.CameraService;

@RestController
public class CameraController {

	private final class StreamDataSource implements DataSource {
		private final long fixSize = 1000;
		private final ReadableByteChannel inputChannel;
		private long pos = 0;
		private final long streamSize = 20757463;

		private StreamDataSource(ReadableByteChannel inputChannel) {
			this.inputChannel = inputChannel;
		}

		public void close() throws IOException {
			System.out.println("close");
			inputChannel.close();
		}

		public ByteBuffer map(long startPosition, long size) throws IOException {
			// TODO Auto-generated method stub
			System.out.println("map > pos:" + startPosition + " size:" + size);
			// ByteBuffer buffer = ByteBuffer.allocate((int) fixSize);
			ByteBuffer buffer = ByteBuffer.allocate((int) size);
			int read = inputChannel.read(buffer);
			System.out.println("map - read bytes: " + read);
			return buffer;
		}

		public long position() throws IOException {
			// TODO Auto-generated method stub
			System.out.println("position?");
			return pos;
		}

		public void position(long nuPos) throws IOException {
			// TODO Auto-generated method stub
			System.out.println("position");
		}

		public int read(ByteBuffer byteBuffer) throws IOException {
			System.out.println("read");
			int read = inputChannel.read(byteBuffer);
			pos += read;
			return read;
		}

		public long size() throws IOException {
			// TODO Auto-generated method stub
			System.out.println("size");
			// return Long.MAX_VALUE;
			return streamSize;
		}

		public long transferTo(long position, long count,
				WritableByteChannel target) throws IOException {
			// TODO Auto-generated method stub
			System.out.println("transfer");
			return 0;
		}
	}

	private static final String VIDEO_FILE = "live.h264";

	@Autowired
	private CameraService service;

	@RequestMapping(value = "/live/start", method = RequestMethod.POST)
	public void liveStart() {
		if (!service.isActive()) {
			service.recordWithInterval();
		}
	}

	@RequestMapping("/live/stream")
	@ResponseBody
	public void liveStream(HttpServletResponse response)
			throws FileNotFoundException, IOException, URISyntaxException {
		if (service.isActive()) {
			try {
				File file = service.getLastVideo();
				System.out.println("stream from " + file.getAbsolutePath());

				final ServletOutputStream outputStream = response
						.getOutputStream();

				H264TrackImpl h264Track = new H264TrackImpl(
						new FileDataSourceImpl(file), "eng", 25, 1);

				Movie movie = new Movie();
				movie.addTrack(h264Track);

				Container out = new DefaultMp4Builder().build(movie);
				WritableByteChannel channel = Channels.newChannel(outputStream);
				out.writeContainer(channel);

				outputStream.close();
				response.setStatus(HttpServletResponse.SC_OK);
			} catch (java.io.FileNotFoundException e) {
				response.setStatus(HttpStatus.NOT_FOUND.value());
				e.printStackTrace();
			} catch (Exception e) {
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				e.printStackTrace();
			}
		}
	}

	/**
	 * stream data source don't read input stream correctly
	 */
	@RequestMapping(value = "/live", method = RequestMethod.GET)
	@ResponseBody
	public void liveTest(HttpServletResponse response) {
		try {
			final ServletOutputStream outputStream = response.getOutputStream();
			System.out.println("open input stream");
			// InputStream inputStream = service.openStream();

			InputStream inputStream = getClass().getResourceAsStream(
					"/test2.h264");
			System.out.println("create trask");

			final ReadableByteChannel inputChannel = Channels
					.newChannel(inputStream);
			DataSource dataSource = new StreamDataSource(inputChannel);
			H264TrackImpl h264Track = new H264TrackImpl(dataSource, "eng", 25,
					1);

			Movie movie = new Movie();
			System.out.println("add to movie");
			movie.addTrack(h264Track);

			System.out.println("build mp4");
			Container out = new DefaultMp4Builder().build(movie);
			System.out.println("create channel for output");
			WritableByteChannel writeChannel = Channels
					.newChannel(outputStream);
			System.out.println("write container");
			out.writeContainer(writeChannel);

			System.out.println("close output stream");
			outputStream.close();
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (java.io.FileNotFoundException e) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			e.printStackTrace();
		} catch (Exception e) {
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			e.printStackTrace();
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
	public ResponseEntity<InputStreamResource> stream() {
		File file = Paths.get(VIDEO_FILE).toFile();
		System.out.println("stream from " + file.getPath());
		HttpHeaders httpHeaders = new HttpHeaders();
		InputStreamResource inputStreamResource = new InputStreamResource(
				getClass().getResourceAsStream(file.getPath()));
		return new ResponseEntity<InputStreamResource>(inputStreamResource,
				httpHeaders, HttpStatus.OK);
	}

	@RequestMapping("/stream2")
	@ResponseBody
	public Resource stream2() {
		File file = Paths.get(VIDEO_FILE).toFile();
		System.out.println("stream from " + file.getPath());
		return new InputStreamResource(getClass().getResourceAsStream(
				file.getPath()));

	}

	@RequestMapping(value = "/stream3")
	@ResponseBody
	public void stream3(HttpServletResponse response) {
		try {
			File file = Paths.get(VIDEO_FILE).toFile();
			System.out.println("stream from " + file.getPath());

			InputStream inputStream = getClass().getResourceAsStream(
					file.getPath());

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

	@RequestMapping("/stream/mp4")
	@ResponseBody
	public void streamMp4(HttpServletResponse response)
			throws FileNotFoundException, IOException, URISyntaxException {
		System.out.println("read h264, covert to mp4 and stream");
		try {
			File file = Paths.get(VIDEO_FILE).toFile();
			System.out.println("read from " + file.getPath());

			final ServletOutputStream outputStream = response.getOutputStream();

			H264TrackImpl h264Track = new H264TrackImpl(new FileDataSourceImpl(
					file));

			Movie movie = new Movie();
			System.out.println("add to movie");
			movie.addTrack(h264Track);

			System.out.println("covert to mp4");
			Container out = new DefaultMp4Builder().build(movie);
			WritableByteChannel channel = Channels.newChannel(outputStream);
			System.out.println("write to mp4");
			out.writeContainer(channel);

			System.out.println("close output stream");
			outputStream.close();
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (java.io.FileNotFoundException e) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			e.printStackTrace();
		} catch (Exception e) {
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			e.printStackTrace();
		}
	}

}
