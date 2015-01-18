package de.picam.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;

import org.springframework.stereotype.Service;

@Service
public class CameraService {

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
}
