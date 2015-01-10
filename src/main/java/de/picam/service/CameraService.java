package de.picam.service;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;

import org.springframework.stereotype.Service;

@Service
public class CameraService {

	public InputStream openStream() {
		System.out.println("START PROGRAM");

		try {
			Process process = new ProcessBuilder("/bin/sh", "-c",
					"raspivid -w 100 -h 100 -n -t 0 -o -").redirectOutput(
					Redirect.PIPE).start();

			System.out.println("start reading");

			// Process process = Runtime.getRuntime().exec(
			// "raspivid -w 100 -h 100 -n -t 0 -o -");
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
			Runtime.getRuntime().exec("killall raspivid");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void test() {
		System.out.println("START PROGRAM");
		long start = System.currentTimeMillis();
		try {

			Process p = Runtime.getRuntime().exec(
					"raspivid -w 100 -h 100 -n -t 10000 -o -");
			BufferedInputStream bis = new BufferedInputStream(
					p.getInputStream());
			// Direct methode p.getInputStream().read() also possible, but
			// BufferedInputStream gives 0,5-1s better performance
			FileOutputStream fos = new FileOutputStream("testvid.h264");

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
		System.out.println("END PROGRAM");
		System.out.println("Duration in ms: "
				+ (System.currentTimeMillis() - start));
	}
}
