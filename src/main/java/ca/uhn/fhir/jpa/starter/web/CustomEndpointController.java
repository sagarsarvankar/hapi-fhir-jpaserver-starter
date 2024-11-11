package ca.uhn.fhir.jpa.starter.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/custom1")
public class CustomEndpointController {
	@GetMapping("/info")
	public ResponseEntity<String> getCustomInfo() {
		String info = "{\n" +
			"  \"message\": \"This is a custom endpoint in HAPI FHIR server\",\n" +
			"  \"status\": \"success\"\n" +
			"}";
		return ResponseEntity.ok()
			.header("Content-Type", "application/json")
			.body(info);
	}
}