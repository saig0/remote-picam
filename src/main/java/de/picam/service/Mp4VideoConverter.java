package de.picam.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.springframework.stereotype.Component;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.H264TrackImpl;

@Component
public class Mp4VideoConverter {

	public void createMp4(File file, WritableByteChannel channel)
			throws IOException, FileNotFoundException {
		FileDataSourceImpl dataSource = new FileDataSourceImpl(file);
		write(dataSource, channel);
	}

	public void createMp4(InputStream stream, WritableByteChannel channel)
			throws IOException, FileNotFoundException {
		DataSource dataSource = new StreamDataSource(stream);
		write(dataSource, channel);
	}

	private void write(DataSource dataSource, WritableByteChannel channel)
			throws IOException {
		final H264TrackImpl h264Track = new H264TrackImpl(dataSource, "eng",
				25, 1);

		final Movie movie = new Movie();
		movie.addTrack(h264Track);

		final Container out = new DefaultMp4Builder().build(movie);
		out.writeContainer(channel);
	}

	private final class StreamDataSource implements DataSource {
		private final long fixSize = 1000;
		private final ReadableByteChannel inputChannel;
		private long pos = 0;
		private final long streamSize = 20757463;

		private StreamDataSource(InputStream inputStream) {
			this.inputChannel = Channels.newChannel(inputStream);
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
}
