package ca.uhn.fhir.jpa.starter.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RedirectController {

	/*
	@GetMapping("/fhir/.well-known/smart-configuration")
	public String redirectOldUrl() {
		return "redirect:/custom1/info";
	}
	@GetMapping("/fhir/docs")
	public String redirectFhirDocs() {
		return "redirect:/fhir/metadata";
	}

	@GetMapping("/docs")
	public String redirectDocs() {
		return "redirect:/fhir/metadata";
	}
	*/
}
