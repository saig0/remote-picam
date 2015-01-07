package de.picam.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.picam.service.CameraService;

@RestController
public class CameraController {

	@Autowired
	private CameraService service;

	@RequestMapping("/")
	String home() {
		return "Hello World!";
	}

}
